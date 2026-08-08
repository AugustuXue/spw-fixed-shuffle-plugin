@file:Suppress("UNCHECKED_CAST")

package com.gg.example

import java.lang.reflect.Method
import kotlinx.coroutines.flow.StateFlow

/**
 * SPW internal queue bridge.
 *
 * Names below are the original binary names from JADX's `loaded from` and
 * `renamed from` records, never Kotlin-metadata display names.
 */
class InternalPlaybackBridge {
    private val controllerClass = Class.forName("com.xuncorp.voxzen.service.PlaybackController")
    private val controllerInstance = controllerClass.getField("INSTANCE").get(null)
        ?: error("PlaybackController.INSTANCE is null")
    private val playbackQueueStateMethod = controllerClass.getMethod("getPlaybackQueueState")
    private val playbackQueueStateFlow = playbackQueueStateMethod.invoke(controllerInstance) as? StateFlow<*>
        ?: error("PlaybackController.getPlaybackQueueState() did not return StateFlow")
    private val changePlaybackModeMethod = controllerClass.methods.firstOrNull {
        it.name == "changePlaybackMode" && it.parameterCount == 1
    } ?: error("PlaybackController.changePlaybackMode(Mode) not found")
    private val moveMediaItemMethod = controllerClass.getMethod(
        "moveMediaItem", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType
    )

    // Original class file names:
    // PlaybackQueueState.class and PlaybackQueueItem's raw class file Ϳ.class.
    private val playbackQueueStateClass = Class.forName("com.xuncorp.spc.core.queue.PlaybackQueueState")
    private val playbackQueueItemClass = Class.forName("com.xuncorp.spc.core.queue.Ϳ")

    // Original Unicode method names, in the exact order reported by JADX.
    val getModeMethod: Method = playbackQueueStateClass.getDeclaredMethod("Ϳ").apply { isAccessible = true }
    val getNormalQueueMethod: Method = playbackQueueStateClass.getDeclaredMethod("Ԩ").apply { isAccessible = true }
    val getNormalIndexMethod: Method = playbackQueueStateClass.getDeclaredMethod("ԩ").apply { isAccessible = true }
    val getRandomQueueMethod: Method = playbackQueueStateClass.getDeclaredMethod("Ԫ").apply { isAccessible = true }
    val getRandomIndexMethod: Method = playbackQueueStateClass.getDeclaredMethod("ԫ").apply { isAccessible = true }
    val getReadyToSaveMethod: Method = playbackQueueStateClass.getDeclaredMethod("Ԭ").apply { isAccessible = true }
    private val getIdMethod: Method = playbackQueueItemClass.getDeclaredMethod("Ϳ").apply { isAccessible = true }

    fun requirePlaybackQueueStateValue(): Any {
        return playbackQueueStateFlow.value ?: error("PlaybackQueueState.value is null")
    }

    /** The host exposes this as the public kotlinx.coroutines StateFlow interface. */
    fun playbackQueueStates(): StateFlow<*> = playbackQueueStateFlow

    fun isRandomMode(modeObj: Any?): Boolean = modeObj?.toString()?.lowercase()?.let {
        it.contains("random") || it.contains("shuffle")
    } ?: false

    fun unwrapPlaybackQueue(queueValue: Any?): List<Any?> = (queueValue as? List<*>)?.toList() ?: emptyList()
    fun describeMode(modeObj: Any?): String = modeObj?.toString() ?: "<null>"
    fun describeValue(value: Any?): String = value?.let { "${it::class.java.name}=$it" } ?: "<null>"
    fun describeItems(items: List<*>): String = items.take(3).joinToString(
        prefix = "[", postfix = if (items.size > 3) ", ...]" else "]",
    ) { it?.toString() ?: "null" }
    fun describeControllerMethods(): String = listOf(
        moveMediaItemMethod, changePlaybackModeMethod, playbackQueueStateMethod,
    ).joinToString(" | ") { it.toGenericString() }

    fun selectLinearMode(modeObj: Any?): Any {
        val constants = modeObj?.javaClass?.enumConstants as? Array<*>
            ?: error("Playback mode enum is null")
        return constants.firstOrNull {
            val text = it?.toString()?.lowercase().orEmpty()
            text.contains("linear") || text.contains("sequence") || text.contains("normal") || text.contains("list")
        } ?: constants.firstOrNull { !it?.toString().orEmpty().lowercase().contains("random") }
            ?: error("No non-random playback mode found")
    }

    fun changePlaybackMode(mode: Any) {
        changePlaybackModeMethod.invoke(controllerInstance, mode)
    }

    fun moveMediaItem(from: Int, to: Int) {
        moveMediaItemMethod.invoke(controllerInstance, from, to)
    }

    /**
     * Reorders the normal queue through the host's Move command. The current
     * item remains at the same index, so the player keeps its active media and
     * progress; only the history and upcoming portions are shuffled.
     */
    fun shuffleNormalQueueKeepingCurrent(state: Any): Int {
        val normalQueue = unwrapPlaybackQueue(getNormalQueueMethod.invoke(state))
        if (normalQueue.size < 2) return normalQueue.size

        val currentIndex = (getNormalIndexMethod.invoke(state) as? Number)?.toInt() ?: -1
        val target = normalQueue.toMutableList()
        if (currentIndex in normalQueue.indices) {
            val before = normalQueue.subList(0, currentIndex).shuffled()
            val after = normalQueue.subList(currentIndex + 1, normalQueue.size).shuffled()
            before.forEachIndexed { index, item -> target[index] = item }
            after.forEachIndexed { offset, item -> target[currentIndex + 1 + offset] = item }
            // Start over from the real source list: the first range has been
            // moved already, so mirror those moves before processing the tail.
            val simulated = normalQueue.toMutableList()
            applyMoves(simulated, target, 0, currentIndex)
            applyMoves(simulated, target, currentIndex + 1, normalQueue.size)
        } else {
            target.shuffle()
            applyMoves(normalQueue.toMutableList(), target, 0, normalQueue.size)
        }
        return normalQueue.size
    }

    private fun applyMoves(current: MutableList<Any?>, target: List<Any?>, start: Int, endExclusive: Int) {
        for (targetIndex in start until endExclusive) {
            val wantedId = queueItemId(target[targetIndex])
            if (queueItemId(current[targetIndex]) == wantedId) continue
            val sourceIndex = (targetIndex + 1 until endExclusive).firstOrNull {
                queueItemId(current[it]) == wantedId
            } ?: error("Queue item $wantedId disappeared while preparing shuffle")
            moveMediaItem(sourceIndex, targetIndex)
            current.add(targetIndex, current.removeAt(sourceIndex))
        }
    }

    private fun queueItemId(queueItem: Any?): Int? = queueItem?.let { getIdMethod.invoke(it) as? Number }?.toInt()

    fun getPlaybackQueueItems(): List<Any?> {
        val state = requirePlaybackQueueStateValue()
        val method = if (isRandomMode(getModeMethod.invoke(state))) getRandomQueueMethod else getNormalQueueMethod
        return unwrapPlaybackQueue(method.invoke(state))
    }

    fun snapshotState(): String = runCatching {
        val state = requirePlaybackQueueStateValue()
        val normalQueue = unwrapPlaybackQueue(getNormalQueueMethod.invoke(state))
        val randomQueue = unwrapPlaybackQueue(getRandomQueueMethod.invoke(state))
        "state=${describeValue(state)}, mode=${describeValue(getModeMethod.invoke(state))}, " +
            "normalIndex=${describeValue(getNormalIndexMethod.invoke(state))}, " +
            "randomIndex=${describeValue(getRandomIndexMethod.invoke(state))}, " +
            "readyToSave=${describeValue(getReadyToSaveMethod.invoke(state))}, " +
            "normalSize=${normalQueue.size}, randomSize=${randomQueue.size}"
    }.getOrElse { "snapshot failed: ${it::class.java.name}: ${it.message}" }
}
