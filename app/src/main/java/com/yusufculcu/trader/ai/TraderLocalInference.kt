package com.yusufculcu.trader.ai

import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.LogSeverity
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext

/** Real on-device inference. This adapter never substitutes canned output or silently calls cloud AI. */
class TraderLocalInference {
    suspend fun generate(
        model: TraderModelSpec,
        verifiedModelFile: File,
        systemInstruction: String,
        prompt: String,
    ): String = withContext(Dispatchers.IO) {
        require(model.isValidCatalogEntry()) { "Invalid model catalog entry: ${model.id}" }
        require(verifiedModelFile.isFile && verifiedModelFile.length() == model.sizeBytes) {
            "Model is missing or has an unexpected size. Install and verify it in Model Hub first."
        }
        require(systemInstruction.isNotBlank()) { "System instruction cannot be empty." }
        require(prompt.isNotBlank()) { "Prompt cannot be empty." }
        require(systemInstruction.length <= MAX_SYSTEM_CHARS) { "System instruction exceeds context input limit." }
        require(prompt.length <= MAX_PROMPT_CHARS) { "Prompt exceeds context input limit." }

        Engine.setNativeMinLogSeverity(LogSeverity.ERROR)
        val startedAt = System.nanoTime()
        val engine = Engine(EngineConfig(modelPath = verifiedModelFile.absolutePath, backend = Backend.CPU()))
        try {
            engine.initialize()
            val conversation = engine.createConversation(
                ConversationConfig(systemInstruction = Contents.of(systemInstruction))
            )
            try {
                val response = StringBuilder()
                conversation.sendMessageAsync(prompt, maxOutputToken = model.maxOutputTokens).collect { message ->
                    response.append(message.toString())
                    require(response.length <= MAX_RESPONSE_CHARS) { "Model response exceeded output safety limit." }
                }
                val output = response.toString().trim()
                check(output.isNotEmpty()) { "Local model returned an empty response." }
                Log.i(TAG, "Local inference complete: ${model.id}, ${((System.nanoTime() - startedAt) / 1_000_000)} ms")
                output
            } finally {
                conversation.close()
            }
        } finally {
            if (engine.isInitialized()) engine.close()
        }
    }

    companion object {
        private const val TAG = "TraderLocalAI"
        const val MAX_SYSTEM_CHARS = 12_000
        const val MAX_PROMPT_CHARS = 24_000
        const val MAX_RESPONSE_CHARS = 32_000
    }
}