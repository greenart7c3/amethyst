package com.vitorpamplona.amethyst.ui.screen.loggedIn

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextFieldColors
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TextFieldDefaults.indicatorLine
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vitorpamplona.amethyst.R
import com.vitorpamplona.amethyst.model.AddressableNote
import com.vitorpamplona.amethyst.model.Channel
import com.vitorpamplona.amethyst.model.Note
import com.vitorpamplona.amethyst.model.PublicChatChannel
import com.vitorpamplona.amethyst.service.NostrChannelDataSource
import com.vitorpamplona.amethyst.ui.actions.ImmutableListOfLists
import com.vitorpamplona.amethyst.ui.actions.NewChannelView
import com.vitorpamplona.amethyst.ui.actions.NewMessageTagger
import com.vitorpamplona.amethyst.ui.actions.NewPostViewModel
import com.vitorpamplona.amethyst.ui.actions.PostButton
import com.vitorpamplona.amethyst.ui.actions.ServersAvailable
import com.vitorpamplona.amethyst.ui.actions.UploadFromGallery
import com.vitorpamplona.amethyst.ui.components.LoadNote
import com.vitorpamplona.amethyst.ui.components.RobohashAsyncImageProxy
import com.vitorpamplona.amethyst.ui.components.TranslatableRichTextViewer
import com.vitorpamplona.amethyst.ui.note.ChatroomMessageCompose
import com.vitorpamplona.amethyst.ui.note.LikeReaction
import com.vitorpamplona.amethyst.ui.note.LoadChannel
import com.vitorpamplona.amethyst.ui.note.MoreOptionsButton
import com.vitorpamplona.amethyst.ui.note.NoteAuthorPicture
import com.vitorpamplona.amethyst.ui.note.NoteUsernameDisplay
import com.vitorpamplona.amethyst.ui.note.TimeAgo
import com.vitorpamplona.amethyst.ui.note.ZapReaction
import com.vitorpamplona.amethyst.ui.note.routeFor
import com.vitorpamplona.amethyst.ui.note.timeAgo
import com.vitorpamplona.amethyst.ui.screen.NostrChannelFeedViewModel
import com.vitorpamplona.amethyst.ui.screen.RefreshingChatroomFeedView
import com.vitorpamplona.amethyst.ui.theme.ButtonBorder
import com.vitorpamplona.amethyst.ui.theme.DoubleHorzSpacer
import com.vitorpamplona.amethyst.ui.theme.DoubleVertSpacer
import com.vitorpamplona.amethyst.ui.theme.EditFieldBorder
import com.vitorpamplona.amethyst.ui.theme.EditFieldLeadingIconModifier
import com.vitorpamplona.amethyst.ui.theme.EditFieldModifier
import com.vitorpamplona.amethyst.ui.theme.EditFieldTrailingIconModifier
import com.vitorpamplona.amethyst.ui.theme.Size25dp
import com.vitorpamplona.amethyst.ui.theme.Size35dp
import com.vitorpamplona.amethyst.ui.theme.SmallBorder
import com.vitorpamplona.amethyst.ui.theme.StdHorzSpacer
import com.vitorpamplona.amethyst.ui.theme.StdPadding
import com.vitorpamplona.amethyst.ui.theme.placeholderText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ChannelScreen(
    channelId: String?,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    if (channelId == null) return

    LoadChannel(channelId) {
        PrepareChannelViewModels(
            baseChannel = it,
            accountViewModel = accountViewModel,
            nav = nav
        )
    }
}

@Composable
fun PrepareChannelViewModels(baseChannel: Channel, accountViewModel: AccountViewModel, nav: (String) -> Unit) {
    val feedViewModel: NostrChannelFeedViewModel = viewModel(
        key = baseChannel.idHex + "ChannelFeedViewModel",
        factory = NostrChannelFeedViewModel.Factory(
            baseChannel,
            accountViewModel.account
        )
    )

    val channelScreenModel: NewPostViewModel = viewModel()
    channelScreenModel.account = accountViewModel.account

    ChannelScreen(
        channel = baseChannel,
        feedViewModel = feedViewModel,
        newPostModel = channelScreenModel,
        accountViewModel = accountViewModel,
        nav = nav
    )
}

@Composable
fun ChannelScreen(
    channel: Channel,
    feedViewModel: NostrChannelFeedViewModel,
    newPostModel: NewPostViewModel,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    val context = LocalContext.current

    NostrChannelDataSource.loadMessagesBetween(accountViewModel.account, channel)

    val lifeCycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        NostrChannelDataSource.start()
        feedViewModel.invalidateData()

        launch(Dispatchers.IO) {
            newPostModel.imageUploadingError.collect { error ->
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    DisposableEffect(accountViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                println("Channel Start")
                NostrChannelDataSource.start()
                feedViewModel.invalidateData()
            }
            if (event == Lifecycle.Event.ON_PAUSE) {
                println("Channel Stop")

                NostrChannelDataSource.clear()
                NostrChannelDataSource.stop()
            }
        }

        lifeCycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifeCycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(Modifier.fillMaxHeight()) {
        ChannelHeader(
            baseChannel = channel,
            showBottomDiviser = true,
            accountViewModel = accountViewModel,
            nav = nav
        )

        val replyTo = remember { mutableStateOf<Note?>(null) }

        Column(
            modifier = remember {
                Modifier
                    .fillMaxHeight()
                    .padding(vertical = 0.dp)
                    .weight(1f, true)
            }
        ) {
            RefreshingChatroomFeedView(
                viewModel = feedViewModel,
                accountViewModel = accountViewModel,
                nav = nav,
                routeForLastRead = "Channel/${channel.idHex}",
                onWantsToReply = {
                    replyTo.value = it
                }
            )
        }

        Spacer(modifier = DoubleVertSpacer)

        replyTo.value?.let {
            DisplayReplyingToNote(it, accountViewModel, nav) {
                replyTo.value = null
            }
        }

        val scope = rememberCoroutineScope()

        // LAST ROW
        EditFieldRow(newPostModel, isPrivate = false, accountViewModel = accountViewModel) {
            scope.launch(Dispatchers.IO) {
                val tagger = NewMessageTagger(
                    message = newPostModel.message.text,
                    mentions = listOfNotNull(replyTo.value?.author),
                    replyTos = listOfNotNull(replyTo.value),
                    channelHex = channel.idHex
                )
                tagger.run()
                if (channel is PublicChatChannel) {
                    accountViewModel.account.sendChannelMessage(
                        message = tagger.message,
                        toChannel = channel.idHex,
                        replyTo = tagger.replyTos,
                        mentions = tagger.mentions,
                        wantsToMarkAsSensitive = false
                    )
                }
                newPostModel.message = TextFieldValue("")
                replyTo.value = null
                feedViewModel.sendToTop()
            }
        }
    }
}

@Composable
fun DisplayReplyingToNote(
    replyingNote: Note?,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit,
    onCancel: () -> Unit
) {
    Row(
        Modifier
            .padding(horizontal = 10.dp)
            .animateContentSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (replyingNote != null) {
            Column(remember { Modifier.weight(1f) }) {
                ChatroomMessageCompose(
                    baseNote = replyingNote,
                    null,
                    innerQuote = true,
                    accountViewModel = accountViewModel,
                    nav = nav,
                    onWantsToReply = {}
                )
            }

            Column(Modifier.padding(end = 10.dp)) {
                IconButton(
                    modifier = Modifier.size(30.dp),
                    onClick = onCancel
                ) {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        null,
                        modifier = Modifier
                            .padding(end = 5.dp)
                            .size(30.dp),
                        tint = MaterialTheme.colors.placeholderText
                    )
                }
            }
        }
    }
}

@Composable
fun EditFieldRow(
    channelScreenModel: NewPostViewModel,
    isPrivate: Boolean,
    accountViewModel: AccountViewModel,
    onSendNewMessage: () -> Unit
) {
    Row(
        modifier = EditFieldModifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val context = LocalContext.current

        MyTextField(
            value = channelScreenModel.message,
            onValueChange = {
                channelScreenModel.updateMessage(it)
            },
            keyboardOptions = KeyboardOptions.Default.copy(
                capitalization = KeyboardCapitalization.Sentences
            ),
            shape = EditFieldBorder,
            modifier = Modifier.weight(1f, true),
            placeholder = {
                Text(
                    text = stringResource(R.string.reply_here),
                    color = MaterialTheme.colors.placeholderText
                )
            },
            textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Content),
            trailingIcon = {
                PostButton(
                    onPost = {
                        onSendNewMessage()
                    },
                    isActive = channelScreenModel.message.text.isNotBlank() && !channelScreenModel.isUploadingImage,
                    modifier = EditFieldTrailingIconModifier
                )
            },
            leadingIcon = {
                UploadFromGallery(
                    isUploading = channelScreenModel.isUploadingImage,
                    tint = MaterialTheme.colors.placeholderText,
                    modifier = EditFieldLeadingIconModifier
                ) {
                    val fileServer = if (isPrivate) {
                        // TODO: Make private servers
                        when (accountViewModel.account.defaultFileServer) {
                            ServersAvailable.NOSTR_BUILD -> ServersAvailable.NOSTR_BUILD
                            ServersAvailable.NOSTRIMG -> ServersAvailable.NOSTRIMG
                            ServersAvailable.NOSTRFILES_DEV -> ServersAvailable.NOSTRFILES_DEV
                            ServersAvailable.NOSTRCHECK_ME -> ServersAvailable.NOSTRCHECK_ME
                            ServersAvailable.NOSTR_BUILD_NIP_94 -> ServersAvailable.NOSTR_BUILD
                            ServersAvailable.NOSTRIMG_NIP_94 -> ServersAvailable.NOSTRIMG
                            ServersAvailable.NOSTRFILES_DEV_NIP_94 -> ServersAvailable.NOSTRFILES_DEV
                            ServersAvailable.NOSTRCHECK_ME_NIP_94 -> ServersAvailable.NOSTRCHECK_ME
                            ServersAvailable.NIP95 -> ServersAvailable.NOSTR_BUILD
                        }
                    } else {
                        accountViewModel.account.defaultFileServer
                    }

                    channelScreenModel.upload(it, "", false, fileServer, context)
                }
            },
            colors = TextFieldDefaults.textFieldColors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
    }
}

@Composable
fun MyTextField(
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
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = TextFieldDefaults.TextFieldShape,
    colors: TextFieldColors = TextFieldDefaults.textFieldColors()
) {
    // If color is not provided via the text style, use content color as a default
    val textColor = textStyle.color.takeOrElse {
        colors.textColor(enabled).value
    }
    val mergedTextStyle = textStyle.merge(TextStyle(color = textColor))

    @OptIn(ExperimentalMaterialApi::class)
    (
        BasicTextField(
            value = value,
            modifier = modifier
                .background(colors.backgroundColor(enabled).value, shape)
                .indicatorLine(enabled, isError, interactionSource, colors)
                .defaultMinSize(
                    minWidth = TextFieldDefaults.MinWidth,
                    minHeight = 36.dp
                ),
            onValueChange = onValueChange,
            enabled = enabled,
            readOnly = readOnly,
            textStyle = mergedTextStyle,
            cursorBrush = SolidColor(colors.cursorColor(isError).value),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interactionSource,
            singleLine = singleLine,
            maxLines = maxLines,
            minLines = minLines,
            decorationBox = @Composable { innerTextField ->
                // places leading icon, text field with label and placeholder, trailing icon
                TextFieldDefaults.TextFieldDecorationBox(
                    value = value.text,
                    visualTransformation = visualTransformation,
                    innerTextField = innerTextField,
                    placeholder = placeholder,
                    label = label,
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon,
                    singleLine = singleLine,
                    enabled = enabled,
                    isError = isError,
                    interactionSource = interactionSource,
                    colors = colors,
                    contentPadding = TextFieldDefaults.textFieldWithoutLabelPadding(
                        top = 12.dp,
                        bottom = 12.dp,
                        start = 10.dp,
                        end = 10.dp
                    )
                )
            }
        )
        )
}

@Composable
fun ChannelHeader(
    channelNote: Note,
    showBottomDiviser: Boolean,
    sendToChannel: Boolean,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    val channelHex by remember {
        derivedStateOf {
            channelNote.channelHex()
        }
    }
    channelHex?.let {
        ChannelHeader(
            channelHex = it,
            showBottomDiviser = showBottomDiviser,
            sendToChannel = sendToChannel,
            accountViewModel = accountViewModel,
            nav = nav
        )
    }
}

@Composable
fun ChannelHeader(
    channelHex: String,
    showBottomDiviser: Boolean,
    showFlag: Boolean = true,
    sendToChannel: Boolean = false,
    modifier: Modifier = StdPadding,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    LoadChannel(channelHex) {
        ChannelHeader(
            it,
            showBottomDiviser,
            showFlag,
            sendToChannel,
            modifier,
            accountViewModel,
            nav
        )
    }
}

@Composable
fun ChannelHeader(
    baseChannel: Channel,
    showBottomDiviser: Boolean,
    showFlag: Boolean = true,
    sendToChannel: Boolean = false,
    modifier: Modifier = StdPadding,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        val expanded = remember { mutableStateOf(false) }

        Column(
            modifier = modifier.clickable {
                if (sendToChannel) {
                    nav(routeFor(baseChannel))
                } else {
                    expanded.value = !expanded.value
                }
            }
        ) {
            ShortChannelHeader(baseChannel, expanded, accountViewModel, nav, showFlag)

            if (expanded.value) {
                LongChannelHeader(baseChannel, accountViewModel, nav)
            }
        }

        if (showBottomDiviser) {
            Divider(
                thickness = 0.25.dp
            )
        }
    }
}

@Composable
private fun ShortChannelHeader(
    baseChannel: Channel,
    expanded: MutableState<Boolean>,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit,
    showFlag: Boolean
) {
    val channelState = baseChannel.live.observeAsState()
    val channel = remember(channelState) {
        channelState.value?.channel
    } ?: return

    Row(verticalAlignment = Alignment.CenterVertically) {
        channel.profilePicture()?.let {
            RobohashAsyncImageProxy(
                robot = channel.idHex,
                model = it,
                contentDescription = stringResource(R.string.profile_image),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .padding(start = 10.dp)
                    .width(Size35dp)
                    .height(Size35dp)
                    .clip(shape = CircleShape)
            )
        }

        Column(
            modifier = Modifier
                .padding(start = 10.dp)
                .height(35.dp)
                .weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = remember(channelState) { channel.toBestDisplayName() },
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            val summary = remember(channelState) {
                channel.summary()?.ifBlank { null }
            }

            if (summary != null && !expanded.value) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = summary,
                        color = MaterialTheme.colors.placeholderText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .height(Size35dp)
                .padding(start = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (channel is PublicChatChannel) {
                ShortChannelActionOptions(channel, accountViewModel, nav)
            }
        }
    }
}

@Composable
private fun LongChannelHeader(
    baseChannel: Channel,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    val channelState = baseChannel.live.observeAsState()
    val channel = remember(channelState) {
        channelState.value?.channel
    } ?: return

    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
    ) {
        val summary = remember(channelState) {
            channel.summary()?.ifBlank { null }
        }

        Column(
            Modifier
                .weight(1f)
                .padding(start = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val defaultBackground = MaterialTheme.colors.background
                val background = remember {
                    mutableStateOf(defaultBackground)
                }

                TranslatableRichTextViewer(
                    content = summary ?: stringResource(id = R.string.groups_no_descriptor),
                    canPreview = false,
                    tags = ImmutableListOfLists(),
                    backgroundColor = background,
                    accountViewModel = accountViewModel,
                    nav = nav
                )
            }
        }

        Column() {
            if (channel is PublicChatChannel) {
                Row() {
                    Spacer(DoubleHorzSpacer)
                    LongChannelActionOptions(channel, accountViewModel, nav)
                }
            }
        }
    }

    Spacer(DoubleVertSpacer)

    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LoadNote(baseNoteHex = channel.idHex) {
            it?.let {
                Text(
                    text = stringResource(id = R.string.owner),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(55.dp)
                )
                Spacer(DoubleHorzSpacer)
                NoteAuthorPicture(it, nav, accountViewModel, Size25dp)
                Spacer(DoubleHorzSpacer)
                NoteUsernameDisplay(it, remember { Modifier.weight(1f) })
                TimeAgo(it)
                MoreOptionsButton(it, accountViewModel)
            }
        }
    }
}

@Composable
private fun ShortChannelActionOptions(
    channel: PublicChatChannel,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    LoadNote(baseNoteHex = channel.idHex) {
        it?.let {
            var popupExpanded by remember { mutableStateOf(false) }

            Spacer(modifier = StdHorzSpacer)
            LikeReaction(baseNote = it, grayTint = MaterialTheme.colors.onSurface, accountViewModel = accountViewModel, nav)
            Spacer(modifier = StdHorzSpacer)
            ZapReaction(baseNote = it, grayTint = MaterialTheme.colors.onSurface, accountViewModel = accountViewModel)
            Spacer(modifier = StdHorzSpacer)
        }
    }

    WatchChannelFollows(channel, accountViewModel) { isFollowing ->
        if (!isFollowing) {
            JoinChatButton(accountViewModel, channel, nav)
        }
    }
}

@Composable
private fun WatchChannelFollows(
    channel: PublicChatChannel,
    accountViewModel: AccountViewModel,
    content: @Composable (Boolean) -> Unit
) {
    val isFollowing by accountViewModel.userProfile().live().follows.map {
        it.user.latestContactList?.isTaggedEvent(channel.idHex) ?: false
    }.distinctUntilChanged().observeAsState(
        accountViewModel.userProfile().latestContactList?.isTaggedEvent(channel.idHex) ?: false
    )

    content(isFollowing)
}

@Composable
private fun LongChannelActionOptions(
    channel: PublicChatChannel,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    val isMe by remember(accountViewModel) {
        derivedStateOf {
            channel.creator == accountViewModel.account.userProfile()
        }
    }

    if (isMe) {
        EditButton(accountViewModel, channel)
    }

    WatchChannelFollows(channel, accountViewModel) { isFollowing ->
        if (isFollowing) {
            LeaveChatButton(accountViewModel, channel, nav)
        }
    }
}

@Composable
fun LiveFlag() {
    Text(
        text = stringResource(id = R.string.live_stream_live_tag),
        color = Color.White,
        fontWeight = FontWeight.Bold,
        modifier = remember {
            Modifier
                .clip(SmallBorder)
                .background(Color.Red)
                .padding(horizontal = 5.dp)
        }
    )
}

@Composable
fun EndedFlag() {
    Text(
        text = stringResource(id = R.string.live_stream_ended_tag),
        color = Color.White,
        fontWeight = FontWeight.Bold,
        modifier = remember {
            Modifier
                .clip(SmallBorder)
                .background(Color.Black)
                .padding(horizontal = 5.dp)
        }
    )
}

@Composable
fun OfflineFlag() {
    Text(
        text = stringResource(id = R.string.live_stream_offline_tag),
        color = Color.White,
        fontWeight = FontWeight.Bold,
        modifier = remember {
            Modifier
                .clip(SmallBorder)
                .background(Color.Black)
                .padding(horizontal = 5.dp)
        }
    )
}

@Composable
fun ScheduledFlag(starts: Long?) {
    val context = LocalContext.current
    val startsIn = starts?.let { timeAgo(it, context) }

    Text(
        text = startsIn ?: stringResource(id = R.string.live_stream_planned_tag),
        color = Color.White,
        fontWeight = FontWeight.Bold,
        modifier = remember {
            Modifier
                .clip(SmallBorder)
                .background(Color.Black)
                .padding(horizontal = 5.dp)
        }
    )
}

@Composable
private fun EditButton(accountViewModel: AccountViewModel, channel: PublicChatChannel) {
    var wantsToPost by remember {
        mutableStateOf(false)
    }

    if (wantsToPost) {
        NewChannelView({ wantsToPost = false }, accountViewModel, channel)
    }

    Button(
        modifier = Modifier
            .padding(horizontal = 3.dp)
            .width(50.dp),
        onClick = { wantsToPost = true },
        shape = ButtonBorder,
        colors = ButtonDefaults
            .buttonColors(
                backgroundColor = MaterialTheme.colors.primary
            )
    ) {
        Icon(
            tint = Color.White,
            imageVector = Icons.Default.EditNote,
            contentDescription = stringResource(R.string.edits_the_channel_metadata)
        )
    }
}

@Composable
fun JoinChatButton(accountViewModel: AccountViewModel, channel: Channel, nav: (String) -> Unit) {
    val scope = rememberCoroutineScope()

    Button(
        modifier = Modifier.padding(horizontal = 3.dp),
        onClick = {
            scope.launch(Dispatchers.IO) {
                accountViewModel.account.follow(channel)
            }
        },
        shape = ButtonBorder,
        colors = ButtonDefaults
            .buttonColors(
                backgroundColor = MaterialTheme.colors.primary
            ),
        contentPadding = PaddingValues(vertical = 6.dp, horizontal = 16.dp)
    ) {
        Text(text = stringResource(R.string.join), color = Color.White)
    }
}

@Composable
fun LeaveChatButton(accountViewModel: AccountViewModel, channel: Channel, nav: (String) -> Unit) {
    val scope = rememberCoroutineScope()

    Button(
        modifier = Modifier.padding(horizontal = 3.dp),
        onClick = {
            scope.launch(Dispatchers.IO) {
                accountViewModel.account.unfollow(channel)
            }
        },
        shape = ButtonBorder,
        colors = ButtonDefaults
            .buttonColors(
                backgroundColor = MaterialTheme.colors.primary
            ),
        contentPadding = PaddingValues(vertical = 6.dp, horizontal = 16.dp)
    ) {
        Text(text = stringResource(R.string.leave), color = Color.White)
    }
}

@Composable
fun JoinCommunityButton(accountViewModel: AccountViewModel, note: AddressableNote, nav: (String) -> Unit) {
    val scope = rememberCoroutineScope()

    Button(
        modifier = Modifier.padding(horizontal = 3.dp),
        onClick = {
            scope.launch(Dispatchers.IO) {
                accountViewModel.account.follow(note)
            }
        },
        shape = ButtonBorder,
        colors = ButtonDefaults
            .buttonColors(
                backgroundColor = MaterialTheme.colors.primary
            ),
        contentPadding = PaddingValues(vertical = 6.dp, horizontal = 16.dp)
    ) {
        Text(text = stringResource(R.string.join), color = Color.White)
    }
}

@Composable
fun LeaveCommunityButton(accountViewModel: AccountViewModel, note: AddressableNote, nav: (String) -> Unit) {
    val scope = rememberCoroutineScope()

    Button(
        modifier = Modifier.padding(horizontal = 3.dp),
        onClick = {
            scope.launch(Dispatchers.IO) {
                accountViewModel.account.unfollow(note)
            }
        },
        shape = ButtonBorder,
        colors = ButtonDefaults
            .buttonColors(
                backgroundColor = MaterialTheme.colors.primary
            ),
        contentPadding = PaddingValues(vertical = 6.dp, horizontal = 16.dp)
    ) {
        Text(text = stringResource(R.string.leave), color = Color.White)
    }
}
