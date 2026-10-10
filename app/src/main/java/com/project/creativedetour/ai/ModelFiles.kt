package com.project.creativedetour.ai

import android.content.Context
import java.io.File

/** Where on-device models live: private internal storage, excluded from backup. */
object ModelFiles {
    const val E2B_FILE = "gemma-4-E2B-it.litertlm"

    fun dir(context: Context) = File(context.filesDir, "models")

    fun e2b(context: Context) = File(dir(context), E2B_FILE)

    fun totalBytes(context: Context): Long =
        dir(context).listFiles()?.sumOf { it.length() } ?: 0L

    fun deleteAll(context: Context): Boolean = dir(context).deleteRecursively()
}
