package com.jonipharju.less

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.jonipharju.less.launcher.LauncherConfiguration
import com.jonipharju.less.launcher.LauncherRepository
import com.jonipharju.less.launcher.configuration
import com.jonipharju.less.launcher.configurationFrom
import com.jonipharju.less.launcher.encoded
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * The name the file picker starts from. The user renames it or files it wherever they like;
 * Less never goes looking for it again.
 */
private val ConfigurationFileName = "less-configuration"

/**
 * The exported Configuration is Less's own record, so it claims no type the system knows. The
 * import asks for any type in return: a picker that filtered on this one would grey out the very
 * file it just wrote, since the provider holding it is free to type it however it likes.
 */
private val ConfigurationMimeType = "application/octet-stream"

/**
 * A Configuration is a few dozen short records. Anything larger is a file the user picked by
 * mistake, and it is refused without being read into memory.
 */
private val ConfigurationSizeLimit = 1 shl 20

/**
 * The user's own copy of their Configuration: written to a file they keep, and read back on a
 * phone that has just been reset. The system's file picker owns where it goes and what it is
 * called — Less only hands over the bytes.
 */
@Composable
internal fun ConfigurationFile(repository: LauncherRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var notice by remember { mutableStateOf<Int?>(null) }

    val export =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(ConfigurationMimeType)) { destination ->
            // A user who backed out of the picker asked for nothing and is told nothing.
            destination ?: return@rememberLauncherForActivityResult
            scope.launch {
                val written = writeConfiguration(context, destination, repository.configuration().encoded())
                notice = if (written) R.string.configuration_exported else R.string.configuration_export_failed
            }
        }

    val import =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { source ->
            source ?: return@rememberLauncherForActivityResult
            scope.launch {
                val read = readConfiguration(context, source)
                // A file that reads as nothing is imported as nothing: what the device holds stands.
                // The write itself is uncancellable — a user who leaves Settings as the picker
                // returns still gets the Configuration they asked for, notice or no notice.
                read?.let { withContext(NonCancellable) { repository.restoreConfiguration(it) } }
                notice = if (read == null) R.string.configuration_import_failed else R.string.configuration_imported
            }
        }

    GroupTitle(stringResource(R.string.settings_configuration))
    TextControl(
        label = stringResource(R.string.settings_configuration_export),
        onClick = { export.launch(ConfigurationFileName) },
    )
    TextControl(
        label = stringResource(R.string.settings_configuration_import),
        onClick = { import.launch(arrayOf("*/*")) },
    )

    notice?.let { message ->
        Notice(message = stringResource(message), onDismiss = { notice = null })
    }
}

/**
 * Whether [bytes] reached [destination]. A picker can hand back a place that will not take them.
 *
 * The write finishes even if the user leaves Settings while it runs: a half-written file would
 * read as no Configuration at all, and it would be the copy the user thinks they have.
 */
private suspend fun writeConfiguration(
    context: Context,
    destination: Uri,
    bytes: ByteArray,
): Boolean =
    withContext(Dispatchers.IO + NonCancellable) {
        try {
            // "wt" truncates: writing a shorter Configuration over a longer one leaves no tail.
            val file = context.contentResolver.openOutputStream(destination, "wt") ?: return@withContext false
            file.use { it.write(bytes) }
            true
        } catch (_: IOException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

/** The Configuration held in [source], or null where the file holds none and none is imported. */
private suspend fun readConfiguration(
    context: Context,
    source: Uri,
): LauncherConfiguration? =
    withContext(Dispatchers.IO + NonCancellable) {
        val bytes =
            try {
                context.contentResolver.openInputStream(source)?.use { it.readNBytes(ConfigurationSizeLimit + 1) }
            } catch (_: IOException) {
                null
            } catch (_: SecurityException) {
                null
            }

        bytes?.takeIf { it.size <= ConfigurationSizeLimit }?.let(::configurationFrom)
    }
