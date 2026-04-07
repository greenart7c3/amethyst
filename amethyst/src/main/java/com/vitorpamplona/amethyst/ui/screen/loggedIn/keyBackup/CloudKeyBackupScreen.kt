/*
 * Copyright (c) 2025 Vitor Pamplona
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the
 * Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.vitorpamplona.amethyst.ui.screen.loggedIn.keyBackup

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitorpamplona.amethyst.R
import com.vitorpamplona.amethyst.ui.navigation.navs.INav
import com.vitorpamplona.amethyst.ui.navigation.topbars.TopBarWithBackButton
import com.vitorpamplona.amethyst.ui.note.authenticate
import com.vitorpamplona.amethyst.ui.screen.loggedIn.AccountViewModel
import com.vitorpamplona.amethyst.ui.stringRes
import com.vitorpamplona.amethyst.ui.theme.ButtonBorder
import com.vitorpamplona.amethyst.ui.theme.ButtonPadding
import com.vitorpamplona.amethyst.ui.theme.placeholderText
import com.vitorpamplona.quartz.nip01Core.core.toHexKey
import com.vitorpamplona.quartz.nip49PrivKeyEnc.Nip49
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val BACKUP_FILENAME = "amethyst_key_backup.ncryptsec"
private const val BACKUP_MIME_TYPE = "text/plain"

@Composable
fun CloudKeyBackupScreen(
    accountViewModel: AccountViewModel,
    nav: INav,
) {
    CloudKeyBackupScreenContent(accountViewModel, nav::popBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CloudKeyBackupScreenContent(
    accountViewModel: AccountViewModel,
    onClose: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopBarWithBackButton(
                stringRes(R.string.cloud_key_backup),
                popBack = onClose,
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringRes(R.string.cloud_key_backup_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Shared encryption password state
            var encPassword by remember { mutableStateOf(TextFieldValue("")) }
            var showEncPassword by remember { mutableStateOf(false) }
            var encPasswordError by remember { mutableStateOf("") }

            Text(
                text = stringRes(R.string.cloud_backup_encryption_password_label),
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .semantics { contentType = ContentType.Password },
                value = encPassword,
                onValueChange = {
                    encPassword = it
                    encPasswordError = ""
                },
                keyboardOptions =
                    KeyboardOptions(
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next,
                    ),
                placeholder = {
                    Text(
                        text = stringRes(R.string.cloud_backup_encryption_password_hint),
                        color = MaterialTheme.colorScheme.placeholderText,
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { showEncPassword = !showEncPassword }) {
                        Icon(
                            imageVector = if (showEncPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = null,
                        )
                    }
                },
                visualTransformation = if (showEncPassword) VisualTransformation.None else PasswordVisualTransformation(),
                isError = encPasswordError.isNotBlank(),
                supportingText =
                    if (encPasswordError.isNotBlank()) {
                        { Text(encPasswordError) }
                    } else {
                        null
                    },
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(thickness = 2.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // --- File / Cloud Storage Section (SAF) ---
            SectionHeader(
                icon = { Icon(Icons.Outlined.Folder, contentDescription = null, modifier = Modifier.size(20.dp)) },
                title = stringRes(R.string.cloud_backup_file_section_title),
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringRes(R.string.cloud_backup_file_section_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))

            SafBackupButtons(
                accountViewModel = accountViewModel,
                encPassword = encPassword,
                onPasswordError = { encPasswordError = it },
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(thickness = 2.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // --- WebDAV Section ---
            SectionHeader(
                icon = { Icon(Icons.Outlined.Cloud, contentDescription = null, modifier = Modifier.size(20.dp)) },
                title = stringRes(R.string.cloud_backup_webdav_section_title),
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringRes(R.string.cloud_backup_webdav_section_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))

            WebDavBackupSection(
                accountViewModel = accountViewModel,
                encPassword = encPassword,
                onPasswordError = { encPasswordError = it },
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SectionHeader(
    icon: @Composable () -> Unit,
    title: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        icon()
        Text(
            text = title,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
        )
    }
}

@Composable
private fun SafBackupButtons(
    accountViewModel: AccountViewModel,
    encPassword: TextFieldValue,
    onPasswordError: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Save: launch system file creator (shows Google Drive, local, etc.)
    val createFileLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(BACKUP_MIME_TYPE)) { uri ->
            uri ?: return@rememberLauncherForActivityResult
            scope.launch {
                val ncryptsec = encryptKey(accountViewModel, encPassword.text) ?: return@launch
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(ncryptsec.toByteArray(Charsets.UTF_8))
                    }
                }
                Toast.makeText(context, stringRes(context, R.string.cloud_backup_saved_to_file), Toast.LENGTH_SHORT).show()
            }
        }

    // Restore: launch system file picker
    val openFileLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri ?: return@rememberLauncherForActivityResult
            scope.launch {
                val content =
                    withContext(Dispatchers.IO) {
                        runCatching {
                            context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
                        }.getOrNull()
                    }
                if (content.isNullOrBlank()) {
                    Toast.makeText(context, stringRes(context, R.string.cloud_backup_restore_failed), Toast.LENGTH_SHORT).show()
                    return@launch
                }
                showRestoredKey(context, scope, content.trim())
            }
        }

    val keyguardSaveLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                if (encPassword.text.isBlank()) {
                    onPasswordError(stringRes(context, R.string.cloud_backup_password_required))
                } else {
                    createFileLauncher.launch(BACKUP_FILENAME)
                }
            }
        }

    val keyguardRestoreLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                openFileLauncher.launch(arrayOf(BACKUP_MIME_TYPE, "*/*"))
            }
        }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            modifier = Modifier.weight(1f),
            onClick = {
                if (encPassword.text.isBlank()) {
                    onPasswordError(stringRes(context, R.string.cloud_backup_password_required))
                } else {
                    authenticate(
                        title = stringRes(context, R.string.cloud_backup_save_title),
                        context = context,
                        keyguardLauncher = keyguardSaveLauncher,
                        onApproved = { createFileLauncher.launch(BACKUP_FILENAME) },
                        onError = { title, message -> accountViewModel.toastManager.toast(title, message) },
                    )
                }
            },
            shape = ButtonBorder,
            contentPadding = ButtonPadding,
        ) {
            Icon(
                imageVector = Icons.Outlined.CloudUpload,
                contentDescription = null,
                modifier = Modifier.padding(end = 6.dp).size(18.dp),
            )
            Text(stringRes(R.string.cloud_backup_save_to_file))
        }

        OutlinedButton(
            modifier = Modifier.weight(1f),
            onClick = {
                authenticate(
                    title = stringRes(context, R.string.cloud_backup_restore_title),
                    context = context,
                    keyguardLauncher = keyguardRestoreLauncher,
                    onApproved = { openFileLauncher.launch(arrayOf(BACKUP_MIME_TYPE, "*/*")) },
                    onError = { title, message -> accountViewModel.toastManager.toast(title, message) },
                )
            },
            shape = ButtonBorder,
            contentPadding = ButtonPadding,
        ) {
            Icon(
                imageVector = Icons.Outlined.FolderOpen,
                contentDescription = null,
                modifier = Modifier.padding(end = 6.dp).size(18.dp),
            )
            Text(stringRes(R.string.cloud_backup_restore_from_file))
        }
    }
}

@Composable
private fun WebDavBackupSection(
    accountViewModel: AccountViewModel,
    encPassword: TextFieldValue,
    onPasswordError: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var serverUrl by remember { mutableStateOf(TextFieldValue("")) }
    var webDavUsername by remember { mutableStateOf(TextFieldValue("")) }
    var webDavPassword by remember { mutableStateOf(TextFieldValue("")) }
    var showWebDavPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = serverUrl,
        onValueChange = { serverUrl = it },
        label = { Text(stringRes(R.string.cloud_backup_webdav_server_url)) },
        placeholder = {
            Text(
                text = "https://cloud.example.com/remote.php/dav/files/user/",
                color = MaterialTheme.colorScheme.placeholderText,
            )
        },
        keyboardOptions =
            KeyboardOptions(
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Next,
            ),
        singleLine = true,
    )

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = webDavUsername,
        onValueChange = { webDavUsername = it },
        label = { Text(stringRes(R.string.cloud_backup_webdav_username)) },
        keyboardOptions =
            KeyboardOptions(
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
        singleLine = true,
    )

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics { contentType = ContentType.Password },
        value = webDavPassword,
        onValueChange = { webDavPassword = it },
        label = { Text(stringRes(R.string.cloud_backup_webdav_password)) },
        keyboardOptions =
            KeyboardOptions(
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
        trailingIcon = {
            IconButton(onClick = { showWebDavPassword = !showWebDavPassword }) {
                Icon(
                    imageVector = if (showWebDavPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = null,
                )
            }
        },
        visualTransformation = if (showWebDavPassword) VisualTransformation.None else PasswordVisualTransformation(),
        singleLine = true,
    )

    Spacer(modifier = Modifier.height(12.dp))

    val keyguardSaveLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                if (encPassword.text.isBlank()) {
                    onPasswordError(stringRes(context, R.string.cloud_backup_password_required))
                    return@rememberLauncherForActivityResult
                }
                scope.launch {
                    isLoading = true
                    val ncryptsec = encryptKey(accountViewModel, encPassword.text)
                    if (ncryptsec == null) {
                        Toast.makeText(context, stringRes(context, R.string.failed_to_encrypt_key), Toast.LENGTH_SHORT).show()
                        isLoading = false
                        return@launch
                    }
                    val result =
                        WebDavKeyBackupService.save(
                            serverUrl = serverUrl.text,
                            username = webDavUsername.text,
                            password = webDavPassword.text,
                            filename = BACKUP_FILENAME,
                            encryptedKey = ncryptsec,
                        )
                    isLoading = false
                    if (result.isSuccess) {
                        Toast.makeText(context, stringRes(context, R.string.cloud_backup_saved_to_webdav), Toast.LENGTH_SHORT).show()
                    } else {
                        val msg = result.exceptionOrNull()?.message ?: stringRes(context, R.string.cloud_backup_webdav_failed)
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

    val keyguardRestoreLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                scope.launch {
                    isLoading = true
                    val loadResult =
                        WebDavKeyBackupService.load(
                            serverUrl = serverUrl.text,
                            username = webDavUsername.text,
                            password = webDavPassword.text,
                            filename = BACKUP_FILENAME,
                        )
                    isLoading = false
                    if (loadResult.isSuccess) {
                        showRestoredKey(context, scope, loadResult.getOrThrow().trim())
                    } else {
                        val msg = loadResult.exceptionOrNull()?.message ?: stringRes(context, R.string.cloud_backup_webdav_failed)
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            modifier = Modifier.weight(1f),
            enabled = !isLoading && serverUrl.text.isNotBlank() && webDavUsername.text.isNotBlank() && webDavPassword.text.isNotBlank(),
            onClick = {
                if (encPassword.text.isBlank()) {
                    onPasswordError(stringRes(context, R.string.cloud_backup_password_required))
                } else {
                    authenticate(
                        title = stringRes(context, R.string.cloud_backup_save_title),
                        context = context,
                        keyguardLauncher = keyguardSaveLauncher,
                        onApproved = {
                            scope.launch {
                                isLoading = true
                                val ncryptsec = encryptKey(accountViewModel, encPassword.text)
                                if (ncryptsec == null) {
                                    Toast.makeText(context, stringRes(context, R.string.failed_to_encrypt_key), Toast.LENGTH_SHORT).show()
                                    isLoading = false
                                    return@launch
                                }
                                val result =
                                    WebDavKeyBackupService.save(
                                        serverUrl = serverUrl.text,
                                        username = webDavUsername.text,
                                        password = webDavPassword.text,
                                        filename = BACKUP_FILENAME,
                                        encryptedKey = ncryptsec,
                                    )
                                isLoading = false
                                if (result.isSuccess) {
                                    Toast.makeText(context, stringRes(context, R.string.cloud_backup_saved_to_webdav), Toast.LENGTH_SHORT).show()
                                } else {
                                    val msg = result.exceptionOrNull()?.message ?: stringRes(context, R.string.cloud_backup_webdav_failed)
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        onError = { title, message -> accountViewModel.toastManager.toast(title, message) },
                    )
                }
            },
            shape = ButtonBorder,
            contentPadding = ButtonPadding,
        ) {
            Icon(
                imageVector = Icons.Outlined.CloudUpload,
                contentDescription = null,
                modifier = Modifier.padding(end = 6.dp).size(18.dp),
            )
            Text(
                if (isLoading) stringRes(R.string.cloud_backup_saving) else stringRes(R.string.cloud_backup_save_to_webdav),
            )
        }

        OutlinedButton(
            modifier = Modifier.weight(1f),
            enabled = !isLoading && serverUrl.text.isNotBlank() && webDavUsername.text.isNotBlank() && webDavPassword.text.isNotBlank(),
            onClick = {
                authenticate(
                    title = stringRes(context, R.string.cloud_backup_restore_title),
                    context = context,
                    keyguardLauncher = keyguardRestoreLauncher,
                    onApproved = {
                        scope.launch {
                            isLoading = true
                            val loadResult =
                                WebDavKeyBackupService.load(
                                    serverUrl = serverUrl.text,
                                    username = webDavUsername.text,
                                    password = webDavPassword.text,
                                    filename = BACKUP_FILENAME,
                                )
                            isLoading = false
                            if (loadResult.isSuccess) {
                                showRestoredKey(context, scope, loadResult.getOrThrow().trim())
                            } else {
                                val msg = loadResult.exceptionOrNull()?.message ?: stringRes(context, R.string.cloud_backup_webdav_failed)
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    onError = { title, message -> accountViewModel.toastManager.toast(title, message) },
                )
            },
            shape = ButtonBorder,
            contentPadding = ButtonPadding,
        ) {
            Icon(
                imageVector = Icons.Outlined.CloudDownload,
                contentDescription = null,
                modifier = Modifier.padding(end = 6.dp).size(18.dp),
            )
            Text(
                if (isLoading) stringRes(R.string.cloud_backup_loading) else stringRes(R.string.cloud_backup_restore_from_webdav),
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = stringRes(R.string.cloud_backup_webdav_restore_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun encryptKey(
    accountViewModel: AccountViewModel,
    password: String,
): String? {
    val privKey = accountViewModel.account.settings.keyPair.privKey ?: return null
    return runCatching { Nip49().encrypt(privKey.toHexKey(), password) }.getOrNull()
}

private fun showRestoredKey(
    context: Context,
    scope: kotlinx.coroutines.CoroutineScope,
    ncryptsec: String,
) {
    scope.launch {
        // Copy the restored ncryptsec to clipboard and notify the user
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("ncryptsec", ncryptsec)
        clipboard.setPrimaryClip(clip)
        Toast
            .makeText(
                context,
                stringRes(context, R.string.cloud_backup_restored_key_copied),
                Toast.LENGTH_LONG,
            ).show()
    }
}
