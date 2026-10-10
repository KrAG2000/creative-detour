package com.project.creativedetour.ai

import android.content.Context
import android.os.SystemClock
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import kotlin.random.Random

enum class AiBackend { GPU, CPU }

class GenerationTimeoutException : Exception("generation timed out")

/**
 * The only class that touches LiteRT-LM. Load → generate → unload, one at a time.
 * Keeping ~2 GB resident in a background service would get the app killed, so we never cache the engine.
 */
class GemmaRuntime(private val context: Context) {
    data class Output(val text: String, val loadMs: Long, val generateMs: Long) {
        val totalMs get() = loadMs + generateMs
    }

    private val lock = Mutex() // two loaded copies of the model would exhaust memory

    suspend fun generate(model: File, backend: AiBackend, system: String, prompt: String, timeoutMs: Long): Output =
        lock.withLock {
            withContext(Dispatchers.Default) {
                val start = SystemClock.elapsedRealtime()
                val engine = Engine(
                    EngineConfig(
                        modelPath = model.absolutePath,
                        backend = if (backend == AiBackend.GPU) Backend.GPU() else Backend.CPU(),
                        maxNumTokens = MAX_TOKENS,
                        cacheDir = context.cacheDir.path, // speeds up every load after the first
                    )
                )
                try {
                    engine.initialize()
                    val loaded = SystemClock.elapsedRealtime()
                    val config = ConversationConfig(
                        systemInstruction = Contents.of(system),
                        initialMessages = emptyList(),
                        samplerConfig = SamplerConfig(topK = 40, topP = 0.95, temperature = 0.9, seed = Random.nextInt()),
                        maxOutputToken = MAX_OUTPUT_TOKENS,
                    )
                    val text = engine.createConversation(config).use { conversation ->
                        // sendMessage is a blocking native call: run it on IO, and on timeout ask the
                        // native side to stop (cancelProcess) instead of waiting forever.
                        val reply = coroutineScope {
                            val call = async(Dispatchers.IO) { conversation.sendMessage(prompt) }
                            withTimeoutOrNull(timeoutMs) { call.await() }
                                ?: run { conversation.cancelProcess(); call.cancel(); null }
                        } ?: throw GenerationTimeoutException()
                        reply.contents.contents.filterIsInstance<Content.Text>().joinToString("") { it.text }
                    }
                    Output(text, loadMs = loaded - start, generateMs = SystemClock.elapsedRealtime() - loaded)
                } finally {
                    engine.close()
                }
            }
        }

    private companion object {
        const val MAX_TOKENS = 1024 // prompt + reply; small context = less memory
        const val MAX_OUTPUT_TOKENS = 80
    }
}
