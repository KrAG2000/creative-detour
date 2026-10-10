package com.project.creativedetour.ai

/**
 * Every model the app knows how to download. Adding a model = adding an entry here.
 * Later this list can come from a JSON file (e.g. in the GitHub repo) instead of being hard-coded.
 */
data class ModelSpec(
    val id: String,
    val displayName: String,
    val fileName: String,
    val url: String,
    val sizeBytes: Long,
    /** From Hugging Face's x-linked-etag header; checked after download so a corrupt file never loads. */
    val sha256: String,
)

object ModelCatalog {
    val GEMMA_4_E2B = ModelSpec(
        id = "gemma-4-e2b",
        displayName = "Gemma 4 E2B",
        fileName = "gemma-4-E2B-it.litertlm",
        url = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm",
        sizeBytes = 2_588_147_712,
        sha256 = "181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c",
    )

    val all = listOf(GEMMA_4_E2B)
    val default = GEMMA_4_E2B
}
