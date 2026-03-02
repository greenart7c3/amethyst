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
package com.vitorpamplona.amethyst.ui.components

import android.net.Uri
import androidx.compose.foundation.content.MediaType
import androidx.compose.foundation.content.ReceiveContentListener
import androidx.compose.foundation.content.TransferableContent
import androidx.compose.foundation.content.consume
import androidx.compose.foundation.content.contentReceiver
import androidx.compose.foundation.content.hasMediaType
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.vitorpamplona.amethyst.ui.theme.placeholderText
import kotlinx.coroutines.flow.distinctUntilChanged

// COPIED FROM TEXT FIELD
// The only change is the contentPadding below

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThinPaddingTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    onMediaReceived: ((Uri, String?) -> Unit)? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = TextFieldDefaults.shape,
    colors: TextFieldColors = TextFieldDefaults.colors(),
    // new fields
    contentPadding: PaddingValues =
        if (label == null) {
            TextFieldDefaults.contentPaddingWithoutLabel(
                start = 10.dp,
                top = 12.dp,
                end = 10.dp,
                bottom = 12.dp,
            )
        } else {
            TextFieldDefaults.contentPaddingWithLabel(
                start = 10.dp,
                top = 12.dp,
                end = 10.dp,
                bottom = 12.dp,
            )
        },
) {
    if (onMediaReceived != null) {
        RichContentThinPaddingTextField(
            value = value,
            onValueChange = onValueChange,
            onMediaReceived = onMediaReceived,
            modifier = modifier,
            enabled = enabled,
            readOnly = readOnly,
            textStyle = textStyle,
            label = label,
            placeholder = placeholder,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            prefix = prefix,
            suffix = suffix,
            supportingText = supportingText,
            isError = isError,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            maxLines = maxLines,
            minLines = minLines,
            interactionSource = interactionSource,
            shape = shape,
            colors = colors,
            contentPadding = contentPadding,
        )
    } else {
        ClassicThinPaddingTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            enabled = enabled,
            readOnly = readOnly,
            textStyle = textStyle,
            label = label,
            placeholder = placeholder,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            prefix = prefix,
            suffix = suffix,
            supportingText = supportingText,
            isError = isError,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            maxLines = maxLines,
            minLines = minLines,
            interactionSource = interactionSource,
            shape = shape,
            colors = colors,
            contentPadding = contentPadding,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClassicThinPaddingTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = TextFieldDefaults.shape,
    colors: TextFieldColors = TextFieldDefaults.colors(),
    contentPadding: PaddingValues =
        if (label == null) {
            TextFieldDefaults.contentPaddingWithoutLabel(
                start = 10.dp,
                top = 12.dp,
                end = 10.dp,
                bottom = 12.dp,
            )
        } else {
            TextFieldDefaults.contentPaddingWithLabel(
                start = 10.dp,
                top = 12.dp,
                end = 10.dp,
                bottom = 12.dp,
            )
        },
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }

    // If color is not provided via the text style, use content color as a default
    val textColor =
        textStyle.color.takeOrElse {
            val focused by interactionSource.collectIsFocusedAsState()

            // this has changed, but only because of private access on the original
            when {
                !enabled -> MaterialTheme.colorScheme.placeholderText
                isError -> MaterialTheme.colorScheme.onSurface
                focused -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.onSurface
            }
        }
    val mergedTextStyle = textStyle.merge(TextStyle(color = textColor))

    CompositionLocalProvider(LocalTextSelectionColors provides colors.textSelectionColors) {
        BasicTextField(
            value = value,
            modifier =
                modifier
                    .defaultMinSize(
                        minWidth = TextFieldDefaults.MinWidth,
                        // this has changed
                        minHeight = 36.dp,
                    ),
            onValueChange = onValueChange,
            enabled = enabled,
            readOnly = readOnly,
            textStyle = mergedTextStyle,
            // this has changed
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interactionSource,
            singleLine = singleLine,
            maxLines = maxLines,
            minLines = minLines,
            decorationBox =
                @Composable { innerTextField ->
                    TextFieldDefaults.DecorationBox(
                        value = value.text,
                        visualTransformation = visualTransformation,
                        innerTextField = innerTextField,
                        placeholder = placeholder,
                        label = label,
                        leadingIcon = leadingIcon,
                        trailingIcon = trailingIcon,
                        prefix = prefix,
                        suffix = suffix,
                        supportingText = supportingText,
                        shape = shape,
                        singleLine = singleLine,
                        enabled = enabled,
                        isError = isError,
                        interactionSource = interactionSource,
                        colors = colors,
                        // this has changed
                        contentPadding = contentPadding,
                    )
                },
        )
    }
}

/**
 * Uses the new state-based BasicTextField to support receiving rich content
 * (GIFs, images) from the keyboard via the contentReceiver modifier.
 *
 * The contentReceiver API requires the state-based BasicTextField (TextFieldState).
 * This function bridges between the value-based API (TextFieldValue) used by
 * the app's ViewModels and the state-based API needed for content receiving.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RichContentThinPaddingTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onMediaReceived: (Uri, String?) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = TextFieldDefaults.shape,
    colors: TextFieldColors = TextFieldDefaults.colors(),
    contentPadding: PaddingValues =
        if (label == null) {
            TextFieldDefaults.contentPaddingWithoutLabel(
                start = 10.dp,
                top = 12.dp,
                end = 10.dp,
                bottom = 12.dp,
            )
        } else {
            TextFieldDefaults.contentPaddingWithLabel(
                start = 10.dp,
                top = 12.dp,
                end = 10.dp,
                bottom = 12.dp,
            )
        },
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }

    val textColor =
        textStyle.color.takeOrElse {
            val focused by interactionSource.collectIsFocusedAsState()

            when {
                !enabled -> MaterialTheme.colorScheme.placeholderText
                isError -> MaterialTheme.colorScheme.onSurface
                focused -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.onSurface
            }
        }
    val mergedTextStyle = textStyle.merge(TextStyle(color = textColor))

    // Bridge between TextFieldValue (ViewModel) and TextFieldState (new BasicTextField)
    val textFieldState = remember { TextFieldState(value.text) }

    // Sync external TextFieldValue -> internal TextFieldState
    SideEffect {
        val currentText = textFieldState.text.toString()
        if (value.text != currentText) {
            textFieldState.edit {
                replace(0, length, value.text)
                // Clamp selection to valid range after text replacement
                val newLength = value.text.length
                val clampedStart = value.selection.start.coerceIn(0, newLength)
                val clampedEnd = value.selection.end.coerceIn(0, newLength)
                selection = TextRange(clampedStart, clampedEnd)
            }
        } else if (value.selection != textFieldState.selection) {
            textFieldState.edit {
                val clampedStart = value.selection.start.coerceIn(0, length)
                val clampedEnd = value.selection.end.coerceIn(0, length)
                selection = TextRange(clampedStart, clampedEnd)
            }
        }
    }

    // Sync internal TextFieldState -> external TextFieldValue
    LaunchedEffect(textFieldState) {
        snapshotFlow {
            TextFieldValue(
                text = textFieldState.text.toString(),
                selection = textFieldState.selection,
            )
        }.distinctUntilChanged().collect { newValue ->
            onValueChange(newValue)
        }
    }

    val context = LocalContext.current
    val receiveContentListener =
        remember(onMediaReceived) {
            object : ReceiveContentListener {
                override fun onReceive(transferableContent: TransferableContent): TransferableContent? {
                    if (!transferableContent.hasMediaType(MediaType.Image)) {
                        return transferableContent
                    }
                    return transferableContent.consume { item ->
                        val uri = item.uri
                        if (uri != null) {
                            val mimeType = context.contentResolver.getType(uri)
                            onMediaReceived(uri, mimeType)
                            true
                        } else {
                            false
                        }
                    }
                }
            }
        }

    val lineLimits =
        if (singleLine) {
            TextFieldLineLimits.SingleLine
        } else {
            TextFieldLineLimits.MultiLine(minHeightInLines = minLines, maxHeightInLines = maxLines)
        }

    // Adapt VisualTransformation to OutputTransformation for the state-based BasicTextField.
    // Applies text replacements (e.g. @npub -> @DisplayName) and style spans (URL coloring)
    // by running the VisualTransformation and applying its result to the TextFieldBuffer.
    val outputTransformation =
        if (visualTransformation != VisualTransformation.None) {
            androidx.compose.foundation.text.input.OutputTransformation {
                val original = AnnotatedString(asCharSequence().toString())
                val (transformed, _) = visualTransformation.filter(original)
                if (original.text != transformed.text || original.spanStyles != transformed.spanStyles) {
                    replace(0, length, transformed)
                }
            }
        } else {
            null
        }

    CompositionLocalProvider(LocalTextSelectionColors provides colors.textSelectionColors) {
        BasicTextField(
            state = textFieldState,
            modifier =
                modifier
                    .defaultMinSize(
                        minWidth = TextFieldDefaults.MinWidth,
                        minHeight = 36.dp,
                    ).contentReceiver(receiveContentListener),
            enabled = enabled,
            readOnly = readOnly,
            textStyle = mergedTextStyle,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = keyboardOptions,
            onKeyboardAction = { performDefaultAction ->
                // Map old-style KeyboardActions to the new onKeyboardAction handler.
                // Check which IME action was configured and invoke the corresponding handler.
                val handler =
                    when (keyboardOptions.imeAction) {
                        androidx.compose.ui.text.input.ImeAction.Done -> keyboardActions.onDone
                        androidx.compose.ui.text.input.ImeAction.Go -> keyboardActions.onGo
                        androidx.compose.ui.text.input.ImeAction.Next -> keyboardActions.onNext
                        androidx.compose.ui.text.input.ImeAction.Previous -> keyboardActions.onPrevious
                        androidx.compose.ui.text.input.ImeAction.Search -> keyboardActions.onSearch
                        androidx.compose.ui.text.input.ImeAction.Send -> keyboardActions.onSend
                        else -> null
                    }
                if (handler != null) {
                    handler.invoke(
                        object : androidx.compose.foundation.text.KeyboardActionScope {
                            override fun defaultKeyboardAction(imeAction: androidx.compose.ui.text.input.ImeAction) {
                                performDefaultAction()
                            }
                        },
                    )
                } else {
                    performDefaultAction()
                }
            },
            lineLimits = lineLimits,
            outputTransformation = outputTransformation,
            interactionSource = interactionSource,
            decorator = { innerTextField ->
                TextFieldDefaults.DecorationBox(
                    value = textFieldState.text.toString(),
                    visualTransformation = visualTransformation,
                    innerTextField = innerTextField,
                    placeholder = placeholder,
                    label = label,
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon,
                    prefix = prefix,
                    suffix = suffix,
                    supportingText = supportingText,
                    shape = shape,
                    singleLine = singleLine,
                    enabled = enabled,
                    isError = isError,
                    interactionSource = interactionSource,
                    colors = colors,
                    contentPadding = contentPadding,
                )
            },
        )
    }
}
