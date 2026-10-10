package com.project.creativedetour.ai

import android.content.Context
import java.io.File

/**
 * Where on-device models live. Downloads go to app-specific external storage
 * (Android/data/<app>/files/models): no permission needed, DownloadManager can write there,
 * and it's still wiped on uninstall. The old internal folder is still checked, so the copy
 * pushed with adb keeps working.
 */
object ModelFiles {
    fun downloadDir(context: Context) = File(context.getExternalFilesDir(null) ?: context.filesDir, "models")

    private fun legacyDir(context: Context) = File(context.filesDir, "models")

    fun target(context: Context, spec: ModelSpec) = File(downloadDir(context), spec.fileName)

    /** The model file if it's present and complete, otherwise null. */
    fun find(context: Context, spec: ModelSpec): File? =
        listOf(target(context, spec), File(legacyDir(context), spec.fileName))
            .firstOrNull { it.length() == spec.sizeBytes }

    fun totalBytes(context: Context): Long =
        listOf(downloadDir(context), legacyDir(context)).distinct()
            .sumOf { dir -> dir.listFiles()?.sumOf { it.length() } ?: 0L }

    fun deleteAll(context: Context) {
        listOf(downloadDir(context), legacyDir(context)).forEach { it.deleteRecursively() }
    }
}
