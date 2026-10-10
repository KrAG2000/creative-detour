package com.project.creativedetour.ai

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import androidx.core.content.edit
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.security.MessageDigest

/**
 * Downloads a model straight from Hugging Face with Android's DownloadManager:
 * Wi-Fi only, resumes after drops, survives the app being closed, shows its own progress notification.
 * The file lands as "<name>.part" and is only renamed after its SHA-256 matches.
 */
class ModelDownloader(private val context: Context) {

    /** A `sealed interface` = a closed set of states; `when` over it must handle every one. */
    sealed interface State {
        data object NotDownloaded : State
        data class Downloading(val bytes: Long, val total: Long, val waitingForWifi: Boolean) : State
        data object Verifying : State
        data class Ready(val file: File) : State
        data class Failed(val reason: String) : State
    }

    private val manager = context.getSystemService(DownloadManager::class.java)
    private val prefs = context.getSharedPreferences("model_download", Context.MODE_PRIVATE)
    private val verifyLock = Mutex()

    fun start(spec: ModelSpec): State {
        val dir = ModelFiles.downloadDir(context).apply { mkdirs() }
        if (dir.usableSpace < spec.sizeBytes + SPACE_MARGIN_BYTES) {
            return State.Failed("Not enough free space. Needs about ${spec.sizeBytes / 1_000_000_000.0} GB.")
        }
        partFile(spec).delete() // DownloadManager renames instead of overwriting, so clear leftovers
        val request = DownloadManager.Request(spec.url.toUri())
            .setTitle("Creative Detour AI model")
            .setDescription(spec.displayName)
            .setDestinationInExternalFilesDir(context, "models", partFile(spec).name)
            .setAllowedOverMetered(false) // Wi-Fi only: 2.5 GB on mobile data would hurt
            .setAllowedOverRoaming(false)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        prefs.edit { putLong(spec.id, manager.enqueue(request)) }
        return State.Downloading(0, spec.sizeBytes, waitingForWifi = false)
    }

    fun cancel(spec: ModelSpec) {
        downloadId(spec)?.let { manager.remove(it) }
        prefs.edit { remove(spec.id) }
        partFile(spec).delete()
    }

    /** Polls once a second while collected (i.e. while the screen showing it is visible). */
    fun states(spec: ModelSpec): Flow<State> = flow {
        while (true) {
            emit(currentState(spec) { emit(it) }) // `report` lets it show "Verifying…" mid-check
            delay(1_000)
        }
    }.distinctUntilChanged().flowOn(Dispatchers.IO)

    private suspend fun currentState(spec: ModelSpec, report: suspend (State) -> Unit): State {
        ModelFiles.find(context, spec)?.let { return State.Ready(it) }
        val id = downloadId(spec) ?: return State.NotDownloaded

        val (status, reason, bytes) = manager.query(DownloadManager.Query().setFilterById(id)).use { c ->
            if (!c.moveToFirst()) {
                prefs.edit { remove(spec.id) } // the user removed it from the system Downloads list
                return State.NotDownloaded
            }
            Triple(c.int(DownloadManager.COLUMN_STATUS), c.int(DownloadManager.COLUMN_REASON), c.long(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
        }

        return when (status) {
            DownloadManager.STATUS_SUCCESSFUL -> {
                report(State.Verifying)
                verifyAndInstall(spec)
            }
            DownloadManager.STATUS_FAILED -> {
                cancel(spec)
                State.Failed("Download failed (code $reason). Check your connection and retry.")
            }
            else -> State.Downloading(
                bytes = bytes,
                total = spec.sizeBytes,
                waitingForWifi = status == DownloadManager.STATUS_PAUSED &&
                    (reason == DownloadManager.PAUSED_QUEUED_FOR_WIFI || reason == DownloadManager.PAUSED_WAITING_FOR_NETWORK),
            )
        }
    }

    /** SHA-256 of 2.5 GB takes ~10–20 s on a phone. Worth it: a corrupt model would crash the AI. */
    private suspend fun verifyAndInstall(spec: ModelSpec): State = verifyLock.withLock {
        ModelFiles.find(context, spec)?.let { return State.Ready(it) } // another check already finished
        val part = partFile(spec)
        val ok = part.length() == spec.sizeBytes && sha256(part) == spec.sha256
        prefs.edit { remove(spec.id) }
        if (ok && part.renameTo(ModelFiles.target(context, spec))) {
            State.Ready(ModelFiles.target(context, spec))
        } else {
            part.delete()
            State.Failed("The downloaded file was corrupted. Please retry.")
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered(1 shl 20).use { input ->
            val buffer = ByteArray(1 shl 20)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun partFile(spec: ModelSpec) = File(ModelFiles.downloadDir(context), "${spec.fileName}.part")
    private fun downloadId(spec: ModelSpec) = prefs.getLong(spec.id, -1).takeIf { it >= 0 }
    private fun Cursor.int(column: String) = getInt(getColumnIndexOrThrow(column))
    private fun Cursor.long(column: String) = getLong(getColumnIndexOrThrow(column))

    private companion object {
        const val SPACE_MARGIN_BYTES = 300_000_000L
    }
}
