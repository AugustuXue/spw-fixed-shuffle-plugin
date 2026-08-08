@file:OptIn(UnstableSpwWorkshopApi::class)
@file:Suppress("unused")

package com.gg.example

import com.xuncorp.spw.workshop.api.PluginContext
import com.xuncorp.spw.workshop.api.SpwPlugin
import com.xuncorp.spw.workshop.api.UnstableSpwWorkshopApi
import com.xuncorp.spw.workshop.api.WorkshopApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MainPlugin(
    pluginContext: PluginContext
) : SpwPlugin(pluginContext) {

    private var monitorScope: CoroutineScope? = null
    private var monitorJob: Job? = null

    override fun start() {
        val configManager = WorkshopApi.manager.createConfigManager(pluginContext.pluginId)
        val config = configManager.getConfig()
        val pluginDataDir = config.getConfigPath().toFile().parentFile
        val configLogEnabled = (config.get("debug.enable_file_log", false) as? Boolean) ?: false
        val logEnabled = FORCE_DEBUG_LOG || configLogEnabled
        val logFile = File(pluginDataDir, "logs/plugin-debug.log")

        configureDebugLogging(logEnabled, logFile)
        logDebug(
            "start(): pluginPath=${pluginContext.pluginPath}, pluginDataDir=$pluginDataDir, " +
                "debugLogEnabled=$logEnabled, configLogEnabled=$configLogEnabled, forceDebugLog=$FORCE_DEBUG_LOG"
        )

        runCatching {
            Class.forName("com.xuncorp.voxzen.service.PlaybackController")
            logDebug("start(): PlaybackController class loaded")
        }.onFailure { logThrowable("start(): PlaybackController load failed", it) }

        runCatching {
            Class.forName("com.xuncorp.spc.core.queue.PlaybackQueueState")
            Class.forName("com.xuncorp.spc.core.queue.Ϳ")
            logDebug("start(): raw queue classes loaded")
        }.onFailure { logThrowable("start(): raw queue class load failed", it) }

        monitorScope?.cancel()
        monitorScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        monitorJob = monitorScope!!.launch {
            runCatching {
                val bridge = InternalPlaybackBridge()
                logDebug("monitor(): event listener initialized")
                var lastModeWasRandom = false
                bridge.playbackQueueStates().collect { stateValue ->
                    try {
                        val modeObj = bridge.getModeMethod.invoke(stateValue)
                        val isRandom = bridge.isRandomMode(modeObj)
                        if (isRandom && !lastModeWasRandom) {
                            val normalQueue = bridge.unwrapPlaybackQueue(bridge.getNormalQueueMethod.invoke(stateValue))
                            if (normalQueue.size >= 2) {
                                val seqEnum = bridge.selectLinearMode(modeObj)
                                bridge.changePlaybackMode(seqEnum)
                                delay(150) // host mode switch is dispatched asynchronously
                                val latestState = bridge.requirePlaybackQueueStateValue()
                                val movedItemCount = bridge.shuffleNormalQueueKeepingCurrent(latestState)
                                logDebug("monitor(): fixed shuffle reordered $movedItemCount items without replacing current media")
                            } else {
                                logDebug("monitor(): random mode entered with fewer than two items")
                            }
                        }
                        lastModeWasRandom = isRandom
                    } catch (t: Throwable) {
                        logThrowable("monitor(): state handling failed", t)
                    }
                }
            }.onFailure { logThrowable("monitor(): listener terminated", it) }
        }
        logDebug("start(): StateFlow listener started")
    }

    override fun stop() {
        monitorJob?.cancel()
        monitorJob = null
        monitorScope?.cancel()
        monitorScope = null
        closeDebugLogging()
        logDebug("stop(): plugin stopped")
    }

    override fun delete() = Unit

    override fun update() = Unit

    companion object {
        @JvmStatic
        @JvmName("onDeterministicShuffleButtonClick")
        fun onDeterministicShuffleButtonClick() {
            Thread {
                runCatching {
                    logDebug("button(): deterministic shuffle clicked")
                    val playbackBridge = InternalPlaybackBridge()
                    var state = playbackBridge.requirePlaybackQueueStateValue()
                    val mode = playbackBridge.getModeMethod.invoke(state)
                    if (playbackBridge.isRandomMode(mode)) {
                        playbackBridge.changePlaybackMode(playbackBridge.selectLinearMode(mode))
                        Thread.sleep(150)
                        state = playbackBridge.requirePlaybackQueueStateValue()
                    }
                    val movedItemCount = playbackBridge.shuffleNormalQueueKeepingCurrent(state)
                    if (movedItemCount < 2) {
                        WorkshopApi.ui.toast("当前播放队列少于 2 首歌曲，无需固定随机", WorkshopApi.Ui.ToastType.Warning)
                    } else {
                        logDebug("button(): reordered $movedItemCount items without replacing current media")
                    }
                }.onFailure { error ->
                    logThrowable("button(): deterministic shuffle failed", error)
                    WorkshopApi.ui.toast(error.message ?: "固定随机队列失败，请查看日志", WorkshopApi.Ui.ToastType.Error)
                }
            }.apply {
                name = "spw-fixed-shuffle-button"
                isDaemon = true
                start()
            }
        }

        private const val FORCE_DEBUG_LOG = false
        private val logLock = Any()
        @Volatile private var debugLogEnabled = false
        @Volatile private var debugLogFile: File? = null
        @Volatile private var debugLogWriter: BufferedWriter? = null
        private val timestampFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")

        private fun configureDebugLogging(enabled: Boolean, file: File) {
            DebugLogger.configure(enabled, file)
        }

        private fun closeDebugLogging() {
            DebugLogger.close()
        }

        private fun logDebug(message: String) {
            DebugLogger.logDebug(message)
        }

        private fun logDebug(message: () -> String) {
            DebugLogger.logDebug(message)
        }

        private fun logThrowable(message: String, t: Throwable) {
            DebugLogger.logThrowable(message, t)
        }
    }
}

private object DebugLogger {
    private const val MAX_DEBUG_LOG_BYTES = 10L * 1024L * 1024L
    private val logLock = Any()
    @Volatile private var debugLogEnabled = false
    @Volatile private var debugLogFile: File? = null
    @Volatile private var debugLogWriter: BufferedWriter? = null
    private val timestampFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")

    fun configure(enabled: Boolean, file: File) {
        synchronized(logLock) {
            debugLogEnabled = enabled
            debugLogFile = file
            closeLocked()
            if (!enabled) return
            file.parentFile?.mkdirs()
            pruneIfNeeded(file)
            debugLogWriter = BufferedWriter(FileWriter(file, true))
        }
        logDebug("logging(): file=${file.absolutePath}, enabled=$enabled")
    }

    fun close() {
        synchronized(logLock) {
            closeLocked()
        }
    }

    fun logDebug(message: String) {
        if (!debugLogEnabled) return
        val line = "[${LocalDateTime.now().format(timestampFormatter)}] $message"
        synchronized(logLock) {
            try {
                val writer = debugLogWriter ?: return
                writer.write(line)
                writer.newLine()
                writer.flush()
            } catch (_: Throwable) {
            }
        }
    }

    fun logDebug(message: () -> String) {
        if (!debugLogEnabled) return
        logDebug(message())
    }

    fun logThrowable(message: String, t: Throwable) {
        logDebug("$message: ${t::class.java.name}: ${t.message}")
        val writer = StringWriter()
        t.printStackTrace(PrintWriter(writer))
        writer.toString().lineSequence().forEach { line ->
            if (line.isNotBlank()) {
                logDebug(line)
            }
        }
    }

    private fun closeLocked() {
        runCatching {
            debugLogWriter?.flush()
            debugLogWriter?.close()
        }
        debugLogWriter = null
    }

    private fun pruneIfNeeded(file: File) {
        if (!file.exists()) return
        if (file.length() <= MAX_DEBUG_LOG_BYTES) return
        runCatching { file.delete() }
    }
}

private fun logDebug(message: String) {
    DebugLogger.logDebug(message)
}

private fun logThrowable(message: String, t: Throwable) {
    DebugLogger.logThrowable(message, t)
}
