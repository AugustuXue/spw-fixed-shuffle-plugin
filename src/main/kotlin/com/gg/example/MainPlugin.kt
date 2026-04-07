@file:OptIn(UnstableSpwWorkshopApi::class)
@file:Suppress("unused")

package com.gg.example

import com.xuncorp.spw.workshop.api.PluginContext
import com.xuncorp.spw.workshop.api.SpwPlugin
import com.xuncorp.spw.workshop.api.UnstableSpwWorkshopApi
import com.xuncorp.spw.workshop.api.WorkshopApi
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.random.Random

class MainPlugin(
    pluginContext: PluginContext
) : SpwPlugin(pluginContext) {

    private var monitorThread: Thread? = null
    @Volatile private var isRunning = false

    override fun start() {
        isRunning = true
        monitorThread = Thread {
            runCatching {
                val bridge = InternalPlaybackBridge()
                var lastModeWasRandom = false
                while (isRunning) {
                    Thread.sleep(500)
                    try {
                        val stateValue = bridge.requirePlaybackQueueStateValue()
                        val modeObj = bridge.getModeMethod.invoke(stateValue)
                        val modeStr = modeObj?.toString() ?: ""
                        val isRandom = modeStr.contains("f8363") || modeStr.contains("Random", ignoreCase = true)
                        
                        if (isRandom && !lastModeWasRandom) {
                            // User switched to Random mode, hijack it!
                            val normalQueue = bridge.unwrapPlaybackQueue(bridge.getNormalQueueMethod.invoke(stateValue))
                            if (normalQueue.size >= 2) {
                                val shuffledQueue = normalQueue.toMutableList().apply {
                                    shuffle(Random.Default)
                                }
                                bridge.setPlaybackQueue(shuffledQueue, 0)
                                bridge.playMusicAt(0, true)
                                
                                // Change mode back to linear (Sequence/LoopAll)
                                val enums = modeObj!!.javaClass.enumConstants
                                val seqEnum = enums.firstOrNull { 
                                    val s = it.toString()
                                    s.contains("f8361") || s.contains("Sequence", ignoreCase = true) || s.contains("LoopAll", ignoreCase = true)
                                } ?: enums[0]
                                
                                bridge.changePlaybackMode(seqEnum)
                                
                                WorkshopApi.ui.toast(
                                    "已触发固定随机（劫持随机播放），当前队列共 ${shuffledQueue.size} 首",
                                    WorkshopApi.Ui.ToastType.Success
                                )
                            }
                        }
                        lastModeWasRandom = isRandom
                    } catch (e: Exception) {
                        // Ignore transient reflection errors during rapid state changes
                    }
                }
            }
        }
        monitorThread?.start()
    }

    override fun stop() {
        isRunning = false
        monitorThread?.interrupt()
    }

    override fun delete() = Unit

    override fun update() = Unit

    companion object {
        @JvmStatic
        @JvmName("onDeterministicShuffleButtonClick")
        fun onDeterministicShuffleButtonClick() {
            runCatching {
                val playbackBridge = InternalPlaybackBridge()
                val currentQueue = playbackBridge.getPlaybackQueueItems()

                if (currentQueue.size < 2) {
                    WorkshopApi.ui.toast(
                        "当前播放队列少于 2 首歌曲，无需固定随机",
                        WorkshopApi.Ui.ToastType.Warning
                    )
                    return
                }

                val shuffledQueue = currentQueue.toMutableList().apply {
                    shuffle(Random.Default)
                }

                playbackBridge.setPlaybackQueue(shuffledQueue, 0)
                playbackBridge.playMusicAt(0, true)

                WorkshopApi.ui.toast(
                    "已成功将当前播放队列重置为固定随机顺序，共 ${shuffledQueue.size} 首",
                    WorkshopApi.Ui.ToastType.Success
                )
            }.onFailure { error ->
                WorkshopApi.ui.toast(
                    error.message ?: "固定随机队列失败，请查看日志",
                    WorkshopApi.Ui.ToastType.Error
                )
                error.printStackTrace()
            }
        }
    }
}

private class InternalPlaybackBridge {
    private val controllerClass: Class<*> = Class.forName("com.xuncorp.voxzen.service.PlaybackController")
    private val controller: Any = controllerClass.getField("INSTANCE").get(null)
    private val piscesMediaItemClass: Class<*> = Class.forName("com.xuncorp.pisces.PiscesMediaItem")
    private val playbackQueueStateClass: Class<*> = Class.forName("androidx.compose.ui.ne")
    private val playbackQueueItemClass: Class<*> = Class.forName("androidx.compose.ui.nd")

    private val setPlaybackQueueMethod: Method = controllerClass.getMethod(
        "setPlaybackQueue",
        List::class.java,
        Int::class.javaPrimitiveType
    )

    private val playMusicAtMethod: Method = controllerClass.getMethod(
        "playMusicAt",
        Int::class.javaPrimitiveType,
        Boolean::class.javaPrimitiveType
    )

    private val moveMediaItemMethod: Method = controllerClass.getMethod(
        "moveMediaItem",
        Int::class.javaPrimitiveType,
        Int::class.javaPrimitiveType
    )

    private val getPlaybackQueueStateMethod: Method = controllerClass.getMethod("getPlaybackQueueState")
    private val stateFlowClass: Class<*> = Class.forName("kotlinx.coroutines.flow.StateFlow")
    private val stateFlowGetValueMethod: Method = stateFlowClass.getMethod("getValue")
    
    // Obfuscated methods in PlaybackQueueState
    val getModeMethod: Method = playbackQueueStateClass.getMethod("Ϳ")
    val getNormalQueueMethod: Method = playbackQueueStateClass.getMethod("Ԩ")
    private val getNormalIndexMethod: Method = playbackQueueStateClass.getMethod("ԩ")
    private val getRandomQueueMethod: Method = playbackQueueStateClass.getMethod("Ԫ")
    private val getRandomIndexMethod: Method = playbackQueueStateClass.getMethod("ԫ")
    
    // Obfuscated methods in PlaybackQueueItem
    private val getQueueItemIdMethod: Method = playbackQueueItemClass.getMethod("Ϳ")
    private val getQueueItemDataMethod: Method = playbackQueueItemClass.getMethod("Ԩ")

    private val changePlaybackModeMethod: Method = controllerClass.methods.first {
        it.name == "changePlaybackMode" && it.parameterCount == 1
    }

    fun changePlaybackMode(modeEnum: Any) {
        changePlaybackModeMethod.invoke(controller, modeEnum)
    }

    fun getPlaybackQueueItems(): List<Any> {
        val stateValue = requirePlaybackQueueStateValue()
        val mode = getModeMethod.invoke(stateValue)
        val normalQueue = unwrapPlaybackQueue(getNormalQueueMethod.invoke(stateValue))
        val randomQueue = unwrapPlaybackQueue(getRandomQueueMethod.invoke(stateValue))

        return if (mode?.toString()?.contains("f8363") == true || mode?.toString()?.contains("Random", ignoreCase = true) == true) {
            randomQueue
        } else {
            normalQueue
        }.takeIf { it.isNotEmpty() }
            ?: throw IllegalStateException("未能从宿主播放队列中解析出 PiscesMediaItem 列表。")
    }

    fun setPlaybackQueue(items: List<Any>, startIndex: Int) {
        setPlaybackQueueMethod.invoke(controller, items, startIndex)
    }

    fun playMusicAt(index: Int, playWhenReady: Boolean) {
        playMusicAtMethod.invoke(controller, index, playWhenReady)
    }

    fun moveMediaItem(fromIndex: Int, toIndex: Int) {
        moveMediaItemMethod.invoke(controller, fromIndex, toIndex)
    }

    fun requirePlaybackQueueStateValue(): Any {
        val flow = getPlaybackQueueStateMethod.invoke(controller)
            ?: throw IllegalStateException("无法获取宿主播放队列 StateFlow。")

        val stateValue = readStateFlowValue(flow)
            ?: throw IllegalStateException("无法读取宿主播放队列状态值。")

        if (!playbackQueueStateClass.isInstance(stateValue)) {
            throw IllegalStateException("播放队列状态类型不匹配: ${stateValue.javaClass.name}")
        }
        return stateValue
    }

    fun unwrapPlaybackQueue(rawQueue: Any?): List<Any> {
        val queue = rawQueue as? Iterable<*> ?: return emptyList()
        return queue.mapNotNull { item ->
            if (item == null || !playbackQueueItemClass.isInstance(item)) {
                null
            } else {
                getQueueItemDataMethod.invoke(item)
                    ?.takeIf { piscesMediaItemClass.isInstance(it) }
            }
        }
    }

    private fun readStateFlowValue(flow: Any): Any? {
        if (!stateFlowClass.isInstance(flow)) {
            return null
        }
        return stateFlowGetValueMethod.invoke(flow)
    }
}