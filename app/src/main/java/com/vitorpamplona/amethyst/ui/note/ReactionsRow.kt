package com.vitorpamplona.amethyst.ui.note

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProgressIndicatorDefaults
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterStart
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import com.vitorpamplona.amethyst.R
import com.vitorpamplona.amethyst.model.Note
import com.vitorpamplona.amethyst.ui.actions.NewPostView
import com.vitorpamplona.amethyst.ui.components.ImageUrlType
import com.vitorpamplona.amethyst.ui.components.InLineIconRenderer
import com.vitorpamplona.amethyst.ui.components.TextType
import com.vitorpamplona.amethyst.ui.screen.CombinedZap
import com.vitorpamplona.amethyst.ui.screen.loggedIn.AccountViewModel
import com.vitorpamplona.amethyst.ui.theme.ButtonBorder
import com.vitorpamplona.amethyst.ui.theme.DarkerGreen
import com.vitorpamplona.amethyst.ui.theme.Font14SP
import com.vitorpamplona.amethyst.ui.theme.HalfDoubleVertSpacer
import com.vitorpamplona.amethyst.ui.theme.HalfStartPadding
import com.vitorpamplona.amethyst.ui.theme.Height4dpModifier
import com.vitorpamplona.amethyst.ui.theme.ModifierWidth3dp
import com.vitorpamplona.amethyst.ui.theme.NoSoTinyBorders
import com.vitorpamplona.amethyst.ui.theme.ReactionRowExpandButton
import com.vitorpamplona.amethyst.ui.theme.ReactionRowHeight
import com.vitorpamplona.amethyst.ui.theme.ReactionRowZapraiserSize
import com.vitorpamplona.amethyst.ui.theme.Size0dp
import com.vitorpamplona.amethyst.ui.theme.Size17dp
import com.vitorpamplona.amethyst.ui.theme.Size20Modifier
import com.vitorpamplona.amethyst.ui.theme.Size20dp
import com.vitorpamplona.amethyst.ui.theme.Size22Modifier
import com.vitorpamplona.amethyst.ui.theme.Size24dp
import com.vitorpamplona.amethyst.ui.theme.Size75dp
import com.vitorpamplona.amethyst.ui.theme.TinyBorders
import com.vitorpamplona.amethyst.ui.theme.mediumImportanceLink
import com.vitorpamplona.amethyst.ui.theme.placeholderText
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.roundToInt
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

@Composable
fun ReactionsRow(
    baseNote: Note,
    showReactionDetail: Boolean,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    val wantsToSeeReactions = remember {
        mutableStateOf(false)
    }

    Spacer(modifier = HalfDoubleVertSpacer)

    InnerReactionRow(baseNote, showReactionDetail, wantsToSeeReactions, accountViewModel, nav)

    LoadAndDisplayZapraiser(baseNote, showReactionDetail, wantsToSeeReactions, accountViewModel)

    if (showReactionDetail && wantsToSeeReactions.value) {
        ReactionDetailGallery(baseNote, nav, accountViewModel)
    }

    Spacer(modifier = HalfDoubleVertSpacer)
}

@OptIn(ExperimentalTime::class)
@Composable
private fun InnerReactionRow(
    baseNote: Note,
    showReactionDetail: Boolean,
    wantsToSeeReactions: MutableState<Boolean>,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    Row(verticalAlignment = CenterVertically, modifier = ReactionRowHeight) {
        if (showReactionDetail) {
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = ReactionRowExpandButton
            ) {
                Row(verticalAlignment = CenterVertically) {
                    WatchReactionsZapsBoostsAndDisplayIfExists(baseNote) {
                        RenderShowIndividualReactionsButton(wantsToSeeReactions)
                    }
                }
            }
        }

        Column(
            verticalArrangement = Arrangement.Center,
            modifier = remember { Modifier.weight(1f) }
        ) {
            val (value, elapsed) = measureTimedValue {
                Row(verticalAlignment = CenterVertically) {
                    ReplyReactionWithDialog(baseNote, MaterialTheme.colors.placeholderText, accountViewModel, nav)
                }
            }
            Log.d("Rendering Metrics", "Reaction Reply: ${baseNote.event?.content()?.split("\n")?.getOrNull(0)?.take(15)}.. $elapsed")
        }

        Column(
            verticalArrangement = Arrangement.Center,
            modifier = remember { Modifier.weight(1f) }
        ) {
            val (value, elapsed) = measureTimedValue {
                Row(verticalAlignment = CenterVertically) {
                    BoostWithDialog(
                        baseNote,
                        MaterialTheme.colors.placeholderText,
                        accountViewModel,
                        nav
                    )
                }
            }
            Log.d("Rendering Metrics", "Reaction Boost: ${baseNote.event?.content()?.split("\n")?.getOrNull(0)?.take(15)}.. $elapsed")
        }

        Column(
            verticalArrangement = Arrangement.Center,
            modifier = remember { Modifier.weight(1f) }
        ) {
            val (value, elapsed) = measureTimedValue {
                Row(verticalAlignment = CenterVertically) {
                    LikeReaction(baseNote, MaterialTheme.colors.placeholderText, accountViewModel, nav)
                }
            }
            Log.d("Rendering Metrics", "Reaction Likes: ${baseNote.event?.content()?.split("\n")?.getOrNull(0)?.take(15)}.. $elapsed")
        }

        Column(
            verticalArrangement = Arrangement.Center,
            modifier = remember { Modifier.weight(1f) }
        ) {
            val (value, elapsed) = measureTimedValue {
                Row(verticalAlignment = CenterVertically) {
                    ZapReaction(baseNote, MaterialTheme.colors.placeholderText, accountViewModel)
                }
            }
            Log.d("Rendering Metrics", "Reaction Zaps:  ${baseNote.event?.content()?.split("\n")?.getOrNull(0)?.take(15)}.. $elapsed")
        }
    }
}

@Composable
private fun LoadAndDisplayZapraiser(
    baseNote: Note,
    showReactionDetail: Boolean,
    wantsToSeeReactions: MutableState<Boolean>,
    accountViewModel: AccountViewModel
) {
    val zapraiserAmount by remember {
        derivedStateOf {
            baseNote.event?.zapraiserAmount() ?: 0
        }
    }

    if (zapraiserAmount > 0) {
        Spacer(modifier = Height4dpModifier)
        Box(
            modifier = remember {
                ReactionRowZapraiserSize
                    .padding(start = if (showReactionDetail) Size75dp else Size0dp)
            },
            contentAlignment = CenterStart
        ) {
            RenderZapRaiser(baseNote, zapraiserAmount, wantsToSeeReactions.value, accountViewModel)
        }
    }
}

@Composable
fun RenderZapRaiser(baseNote: Note, zapraiserAmount: Long, details: Boolean, accountViewModel: AccountViewModel) {
    val zapsState by baseNote.live().zaps.observeAsState()

    var zapraiserProgress by remember { mutableStateOf(0F) }
    var zapraiserLeft by remember { mutableStateOf("$zapraiserAmount") }

    LaunchedEffect(key1 = zapsState) {
        launch(Dispatchers.Default) {
            zapsState?.note?.let {
                val newZapAmount = accountViewModel.calculateZapAmount(it)
                var percentage = newZapAmount.div(zapraiserAmount.toBigDecimal()).toFloat()

                if (percentage > 1) {
                    percentage = 1f
                }

                if (Math.abs(zapraiserProgress - percentage) > 0.001) {
                    val newZapraiserProgress = percentage
                    val newZapraiserLeft = if (percentage > 0.99) {
                        "0"
                    } else {
                        showAmount((zapraiserAmount * (1 - percentage)).toBigDecimal())
                    }
                    if (zapraiserLeft != newZapraiserLeft) {
                        zapraiserLeft = newZapraiserLeft
                    }
                    if (zapraiserProgress != newZapraiserProgress) {
                        zapraiserProgress = newZapraiserProgress
                    }
                }
            }
        }
    }

    val color = if (zapraiserProgress > 0.99) {
        DarkerGreen
    } else {
        MaterialTheme.colors.mediumImportanceLink
    }

    LinearProgressIndicator(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (details) 24.dp else 4.dp),
        color = color,
        progress = zapraiserProgress
    )

    if (details) {
        Box(
            contentAlignment = Center,
            modifier = TinyBorders
        ) {
            val totalPercentage by remember(zapraiserProgress) {
                derivedStateOf {
                    "${(zapraiserProgress * 100).roundToInt()}%"
                }
            }

            Text(
                text = stringResource(id = R.string.sats_to_complete, totalPercentage, zapraiserLeft),
                modifier = NoSoTinyBorders,
                color = MaterialTheme.colors.placeholderText,
                fontSize = Font14SP,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun WatchReactionsZapsBoostsAndDisplayIfExists(baseNote: Note, content: @Composable () -> Unit) {
    val hasReactions by baseNote.live().zaps.combineWith(
        liveData1 = baseNote.live().boosts,
        liveData2 = baseNote.live().reactions,
        block = { zapsState, boostsState, reactionsState ->
            zapsState?.note?.zaps?.isNotEmpty() == true ||
                boostsState?.note?.boosts?.isNotEmpty() == true ||
                reactionsState?.note?.reactions?.isNotEmpty() == true
        }
    ).observeAsState(
        baseNote.zaps.isNotEmpty() ||
            baseNote.boosts.isNotEmpty() ||
            baseNote.reactions.isNotEmpty()
    )

    Crossfade(targetState = hasReactions) {
        if (it) {
            content()
        }
    }
}

fun <T, K, R> LiveData<T>.combineWith(
    liveData1: LiveData<K>,
    block: (T?, K?) -> R
): LiveData<R> {
    val result = MediatorLiveData<R>()
    result.addSource(this) {
        result.value = block(this.value, liveData1.value)
    }
    result.addSource(liveData1) {
        result.value = block(this.value, liveData1.value)
    }
    return result
}

fun <T, K, P, R> LiveData<T>.combineWith(
    liveData1: LiveData<K>,
    liveData2: LiveData<P>,
    block: (T?, K?, P?) -> R
): LiveData<R> {
    val result = MediatorLiveData<R>()
    result.addSource(this) {
        result.value = block(this.value, liveData1.value, liveData2.value)
    }
    result.addSource(liveData1) {
        result.value = block(this.value, liveData1.value, liveData2.value)
    }
    result.addSource(liveData2) {
        result.value = block(this.value, liveData1.value, liveData2.value)
    }
    return result
}

@Composable
private fun RenderShowIndividualReactionsButton(wantsToSeeReactions: MutableState<Boolean>) {
    IconButton(
        onClick = {
            wantsToSeeReactions.value = !wantsToSeeReactions.value
        },
        modifier = Size20Modifier
    ) {
        Crossfade(targetState = wantsToSeeReactions.value) {
            if (it) {
                ExpandLessIcon(modifier = Size22Modifier)
            } else {
                ExpandMoreIcon(modifier = Size22Modifier)
            }
        }
    }
}

@Composable
private fun ReactionDetailGallery(
    baseNote: Note,
    nav: (String) -> Unit,
    accountViewModel: AccountViewModel
) {
    val zapsState by baseNote.live().zaps.observeAsState()
    val boostsState by baseNote.live().boosts.observeAsState()
    val reactionsState by baseNote.live().reactions.observeAsState()

    val defaultBackgroundColor = MaterialTheme.colors.background
    val backgroundColor = remember { mutableStateOf<Color>(defaultBackgroundColor) }

    val hasReactions by remember(zapsState, boostsState, reactionsState) {
        derivedStateOf {
            baseNote.zaps.isNotEmpty() ||
                baseNote.boosts.isNotEmpty() ||
                baseNote.reactions.isNotEmpty()
        }
    }

    if (hasReactions) {
        Row(verticalAlignment = CenterVertically, modifier = Modifier.padding(start = 10.dp, top = 5.dp)) {
            Column() {
                val zapEvents by remember(zapsState) { derivedStateOf { baseNote.zaps.mapNotNull { it.value?.let { zapEvent -> CombinedZap(it.key, zapEvent) } }.toImmutableList() } }
                val boostEvents by remember(boostsState) { derivedStateOf { baseNote.boosts.toImmutableList() } }
                val likeEvents by remember(reactionsState) { derivedStateOf { baseNote.reactions.toImmutableMap() } }

                val hasZapEvents by remember(zapsState) { derivedStateOf { baseNote.zaps.isNotEmpty() } }
                val hasBoostEvents by remember(boostsState) { derivedStateOf { baseNote.boosts.isNotEmpty() } }
                val hasLikeEvents by remember(reactionsState) { derivedStateOf { baseNote.reactions.isNotEmpty() } }

                if (hasZapEvents) {
                    RenderZapGallery(
                        zapEvents,
                        backgroundColor,
                        nav,
                        accountViewModel
                    )
                }

                if (hasBoostEvents) {
                    RenderBoostGallery(
                        boostEvents,
                        nav,
                        accountViewModel
                    )
                }

                if (hasLikeEvents) {
                    likeEvents.forEach {
                        val reactions = remember(it.value) { it.value.toImmutableList() }
                        RenderLikeGallery(
                            it.key,
                            reactions,
                            nav,
                            accountViewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BoostWithDialog(
    baseNote: Note,
    grayTint: Color,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    var wantsToQuote by remember {
        mutableStateOf<Note?>(null)
    }

    if (wantsToQuote != null) {
        NewPostView({ wantsToQuote = null }, null, wantsToQuote, accountViewModel, nav)
    }

    BoostReaction(baseNote, grayTint, accountViewModel) {
        wantsToQuote = baseNote
    }
}

@Composable
private fun ReplyReactionWithDialog(
    baseNote: Note,
    grayTint: Color,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    var wantsToReplyTo by remember {
        mutableStateOf<Note?>(null)
    }

    if (wantsToReplyTo != null) {
        NewPostView({ wantsToReplyTo = null }, wantsToReplyTo, null, accountViewModel, nav)
    }

    ReplyReaction(baseNote, grayTint, accountViewModel) {
        wantsToReplyTo = baseNote
    }
}

@Composable
fun ReplyReaction(
    baseNote: Note,
    grayTint: Color,
    accountViewModel: AccountViewModel,
    showCounter: Boolean = true,
    iconSize: Dp = Size17dp,
    onPress: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    IconButton(
        modifier = remember {
            Modifier.size(iconSize)
        },
        onClick = {
            if (accountViewModel.isWriteable()) {
                onPress()
            } else {
                scope.launch {
                    Toast.makeText(
                        context,
                        context.getString(R.string.login_with_a_private_key_to_be_able_to_reply),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    ) {
        CommentIcon(iconSize, grayTint)
    }

    if (showCounter) {
        ReplyCounter(baseNote, grayTint)
    }
}

@Composable
fun ReplyCounter(baseNote: Note, textColor: Color) {
    val repliesState by baseNote.live().replies.map {
        it.note.replies.size
    }.observeAsState(baseNote.replies.size)

    SlidingAnimationCount(repliesState, textColor)
}

@Composable
private fun SlidingAnimationCount(baseCount: MutableState<Int>, textColor: Color) {
    SlidingAnimationCount(baseCount.value, textColor)
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun SlidingAnimationCount(baseCount: Int, textColor: Color) {
    AnimatedContent<Int>(
        targetState = baseCount,
        transitionSpec = AnimatedContentScope<Int>::transitionSpec
    ) { count ->
        TextCount(count, textColor)
    }
}

@OptIn(ExperimentalAnimationApi::class)
private fun <S> AnimatedContentScope<S>.transitionSpec(): ContentTransform {
    return slideAnimation
}

@ExperimentalAnimationApi
val slideAnimation: ContentTransform = slideInVertically(animationSpec = tween(durationMillis = 100)) { height -> height } + fadeIn(
    animationSpec = tween(durationMillis = 100)
) with slideOutVertically(animationSpec = tween(durationMillis = 100)) { height -> -height } + fadeOut(
    animationSpec = tween(durationMillis = 100)
)

@Composable
private fun TextCount(count: Int, textColor: Color) {
    Text(
        text = remember(count) { showCount(count) },
        fontSize = Font14SP,
        color = textColor,
        modifier = HalfStartPadding,
        maxLines = 1
    )
}

@Composable
@OptIn(ExperimentalAnimationApi::class)
private fun SlidingAnimationAmount(amount: MutableState<String>, textColor: Color) {
    AnimatedContent(
        targetState = amount.value,
        transitionSpec = AnimatedContentScope<String>::transitionSpec
    ) { count ->
        Text(
            text = count,
            fontSize = Font14SP,
            color = textColor,
            maxLines = 1
        )
    }
}

@Composable
fun BoostReaction(
    baseNote: Note,
    grayTint: Color,
    accountViewModel: AccountViewModel,
    iconSize: Dp = 20.dp,
    onQuotePress: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var wantsToBoost by remember { mutableStateOf(false) }

    val iconButtonModifier = remember {
        Modifier.size(iconSize)
    }

    IconButton(
        modifier = iconButtonModifier,
        onClick = {
            if (accountViewModel.isWriteable()) {
                if (accountViewModel.hasBoosted(baseNote)) {
                    scope.launch(Dispatchers.IO) {
                        accountViewModel.deleteBoostsTo(baseNote)
                    }
                } else {
                    wantsToBoost = true
                }
            } else {
                scope.launch {
                    Toast.makeText(
                        context,
                        context.getString(R.string.login_with_a_private_key_to_be_able_to_boost_posts),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    ) {
        BoostIcon(baseNote, iconSize, grayTint, accountViewModel)
    }

    if (wantsToBoost) {
        BoostTypeChoicePopup(
            baseNote,
            accountViewModel,
            onDismiss = {
                wantsToBoost = false
            },
            onQuote = {
                wantsToBoost = false
                onQuotePress()
            }
        )
    }

    BoostText(baseNote, grayTint)
}

@Composable
fun BoostIcon(baseNote: Note, iconSize: Dp = Size20dp, grayTint: Color, accountViewModel: AccountViewModel) {
    val iconTint by baseNote.live().boosts.map {
        if (it.note.isBoostedBy(accountViewModel.userProfile())) Color.Unspecified else grayTint
    }.distinctUntilChanged().observeAsState(grayTint)

    val iconModifier = remember {
        Modifier.size(iconSize)
    }

    RepostedIcon(iconModifier, iconTint)
}

@Composable
fun BoostText(baseNote: Note, grayTint: Color) {
    val boostState by baseNote.live().boosts.map {
        it.note.boosts.size
    }.distinctUntilChanged().observeAsState(baseNote.boosts.size)

    SlidingAnimationCount(boostState, grayTint)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LikeReaction(
    baseNote: Note,
    grayTint: Color,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit,
    iconSize: Dp = 20.dp,
    heartSize: Dp = 16.dp,
    iconFontSize: TextUnit = Font14SP
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val iconButtonModifier = remember {
        Modifier.size(iconSize)
    }

    var wantsToChangeReactionSymbol by remember { mutableStateOf(false) }
    var wantsToReact by remember { mutableStateOf(false) }

    Box(
        contentAlignment = Center,
        modifier = iconButtonModifier.combinedClickable(
            role = Role.Button,
            interactionSource = remember { MutableInteractionSource() },
            indication = rememberRipple(bounded = false, radius = Size24dp),
            onClick = {
                likeClick(
                    baseNote,
                    accountViewModel,
                    scope,
                    context,
                    onMultipleChoices = {
                        wantsToReact = true
                    }
                )
            },
            onLongClick = {
                wantsToChangeReactionSymbol = true
            }
        )
    ) {
        LikeIcon(baseNote, iconFontSize, heartSize, grayTint, accountViewModel)
    }

    LikeText(baseNote, grayTint)

    if (wantsToChangeReactionSymbol) {
        UpdateReactionTypeDialog(
            { wantsToChangeReactionSymbol = false },
            accountViewModel = accountViewModel,
            nav
        )
    }

    if (wantsToReact) {
        ReactionChoicePopup(
            baseNote,
            accountViewModel,
            onDismiss = {
                wantsToReact = false
            },
            onChangeAmount = {
                wantsToReact = false
                wantsToChangeReactionSymbol = true
            }
        )
    }
}

@Composable
fun LikeIcon(
    baseNote: Note,
    iconFontSize: TextUnit = Font14SP,
    iconSize: Dp = Size20dp,
    grayTint: Color,
    accountViewModel: AccountViewModel
) {
    val reactionType = remember(baseNote) {
        mutableStateOf<String?>(null)
    }

    val scope = rememberCoroutineScope()

    WatchReactionTypeForNote(baseNote, accountViewModel) { newReactionType ->
        if (reactionType.value != newReactionType) {
            scope.launch(Dispatchers.Main) {
                reactionType.value = newReactionType
            }
        }
    }

    Crossfade(targetState = reactionType) {
        val value = it.value
        if (value != null) {
            RenderReactionType(value, iconSize, iconFontSize)
        } else {
            LikeIcon(iconSize, grayTint)
        }
    }
}

@Composable
private fun WatchReactionTypeForNote(baseNote: Note, accountViewModel: AccountViewModel, onNewReactionType: (String?) -> Unit) {
    val reactionsState by baseNote.live().reactions.observeAsState()

    LaunchedEffect(key1 = reactionsState) {
        launch(Dispatchers.Default) {
            val reactionNote = reactionsState?.note?.getReactionBy(accountViewModel.userProfile())
            onNewReactionType(reactionNote)
        }
    }
}

@Composable
private fun RenderReactionType(
    reactionType: String,
    iconSize: Dp = Size20dp,
    iconFontSize: TextUnit
) {
    if (reactionType.startsWith(":")) {
        val noStartColon = reactionType.removePrefix(":")
        val url = noStartColon.substringAfter(":")

        val renderable = listOf(
            ImageUrlType(url)
        ).toImmutableList()

        InLineIconRenderer(
            renderable,
            style = SpanStyle(color = Color.White),
            fontSize = iconFontSize,
            maxLines = 1
        )
    } else {
        when (reactionType) {
            "+" -> LikedIcon(iconSize)
            "-" -> Text(text = "\uD83D\uDC4E", fontSize = iconFontSize)
            else -> Text(text = reactionType, fontSize = iconFontSize)
        }
    }
}

@Composable
fun LikeText(baseNote: Note, grayTint: Color) {
    val reactionsCount = remember(baseNote) {
        mutableStateOf(baseNote.reactions.size)
    }

    val scope = rememberCoroutineScope()

    WatchReactionCountForNote(baseNote) { newReactionsCount ->
        if (reactionsCount.value != newReactionsCount) {
            scope.launch(Dispatchers.Main) {
                reactionsCount.value = newReactionsCount
            }
        }
    }

    SlidingAnimationCount(reactionsCount, grayTint)
}

@Composable
private fun WatchReactionCountForNote(baseNote: Note, onNewReactionCount: (Int) -> Unit) {
    val reactionsState by baseNote.live().reactions.observeAsState()

    LaunchedEffect(key1 = reactionsState) {
        launch(Dispatchers.Default) {
            onNewReactionCount(reactionsState?.note?.countReactions() ?: 0)
        }
    }
}

private fun likeClick(
    baseNote: Note,
    accountViewModel: AccountViewModel,
    scope: CoroutineScope,
    context: Context,
    onMultipleChoices: () -> Unit
) {
    if (accountViewModel.account.reactionChoices.isEmpty()) {
        scope.launch {
            Toast
                .makeText(
                    context,
                    context.getString(R.string.no_reaction_type_setup_long_press_to_change),
                    Toast.LENGTH_SHORT
                )
                .show()
        }
    } else if (!accountViewModel.isWriteable()) {
        scope.launch {
            Toast
                .makeText(
                    context,
                    context.getString(R.string.login_with_a_private_key_to_like_posts),
                    Toast.LENGTH_SHORT
                )
                .show()
        }
    } else if (accountViewModel.account.reactionChoices.size == 1) {
        scope.launch(Dispatchers.IO) {
            val reaction = accountViewModel.account.reactionChoices.first()
            if (accountViewModel.hasReactedTo(baseNote, reaction)) {
                accountViewModel.deleteReactionTo(baseNote, reaction)
            } else {
                accountViewModel.reactTo(baseNote, reaction)
            }
        }
    } else if (accountViewModel.account.reactionChoices.size > 1) {
        onMultipleChoices()
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun ZapReaction(
    baseNote: Note,
    grayTint: Color,
    accountViewModel: AccountViewModel,
    iconSize: Dp = 20.dp,
    animationSize: Dp = 14.dp
) {
    var wantsToZap by remember { mutableStateOf(false) }
    var wantsToChangeZapAmount by remember { mutableStateOf(false) }
    var wantsToSetCustomZap by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var zappingProgress by remember { mutableStateOf(0f) }

    Row(
        verticalAlignment = CenterVertically,
        modifier = Modifier
            .size(iconSize)
            .combinedClickable(
                role = Role.Button,
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = false, radius = Size24dp),
                onClick = {
                    zapClick(
                        baseNote,
                        accountViewModel,
                        scope,
                        context,
                        onZappingProgress = { progress: Float ->
                            zappingProgress = progress
                        },
                        onMultipleChoices = {
                            wantsToZap = true
                        }
                    )
                },
                onLongClick = {
                    wantsToChangeZapAmount = true
                },
                onDoubleClick = {
                    wantsToSetCustomZap = true
                }
            )
    ) {
        if (wantsToZap) {
            ZapAmountChoicePopup(
                baseNote,
                accountViewModel,
                onDismiss = {
                    wantsToZap = false
                    zappingProgress = 0f
                },
                onChangeAmount = {
                    wantsToZap = false
                    wantsToChangeZapAmount = true
                },
                onError = {
                    scope.launch {
                        zappingProgress = 0f
                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                    }
                },
                onProgress = {
                    scope.launch(Dispatchers.Main) {
                        zappingProgress = it
                    }
                }
            )
        }

        if (wantsToChangeZapAmount) {
            UpdateZapAmountDialog({ wantsToChangeZapAmount = false }, accountViewModel = accountViewModel)
        }

        if (wantsToSetCustomZap) {
            ZapCustomDialog({ wantsToSetCustomZap = false }, accountViewModel, baseNote)
        }

        if (zappingProgress > 0.00001 && zappingProgress < 0.99999) {
            Spacer(ModifierWidth3dp)

            CircularProgressIndicator(
                progress = animateFloatAsState(
                    targetValue = zappingProgress,
                    animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
                ).value,
                modifier = remember { Modifier.size(animationSize) },
                strokeWidth = 2.dp
            )
        } else {
            ZapIcon(
                baseNote,
                iconSize,
                grayTint,
                accountViewModel
            )
        }
    }

    ZapAmountText(baseNote, grayTint, accountViewModel)
}

private fun zapClick(
    baseNote: Note,
    accountViewModel: AccountViewModel,
    scope: CoroutineScope,
    context: Context,
    onZappingProgress: (Float) -> Unit,
    onMultipleChoices: () -> Unit
) {
    if (accountViewModel.account.zapAmountChoices.isEmpty()) {
        scope.launch {
            Toast
                .makeText(
                    context,
                    context.getString(R.string.no_zap_amount_setup_long_press_to_change),
                    Toast.LENGTH_SHORT
                )
                .show()
        }
    } else if (!accountViewModel.isWriteable()) {
        scope.launch {
            Toast
                .makeText(
                    context,
                    context.getString(R.string.login_with_a_private_key_to_be_able_to_send_zaps),
                    Toast.LENGTH_SHORT
                )
                .show()
        }
    } else if (accountViewModel.account.zapAmountChoices.size == 1) {
        scope.launch(Dispatchers.IO) {
            accountViewModel.zap(
                baseNote,
                accountViewModel.account.zapAmountChoices.first() * 1000,
                null,
                "",
                context,
                onError = {
                    scope.launch {
                        onZappingProgress(0f)
                        Toast
                            .makeText(context, it, Toast.LENGTH_SHORT)
                            .show()
                    }
                },
                onProgress = {
                    scope.launch(Dispatchers.Main) {
                        onZappingProgress(it)
                    }
                },
                zapType = accountViewModel.account.defaultZapType
            )
        }
    } else if (accountViewModel.account.zapAmountChoices.size > 1) {
        onMultipleChoices()
    }
}

@Composable
private fun ZapIcon(
    baseNote: Note,
    iconSize: Dp,
    grayTint: Color,
    accountViewModel: AccountViewModel
) {
    val wasZappedByLoggedInUser = remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    if (!wasZappedByLoggedInUser.value) {
        WatchZapsForNote(baseNote, accountViewModel) { newWasZapped ->
            if (wasZappedByLoggedInUser.value != newWasZapped) {
                scope.launch(Dispatchers.Main) {
                    wasZappedByLoggedInUser.value = newWasZapped
                }
            }
        }
    }

    Crossfade(targetState = wasZappedByLoggedInUser) {
        if (it.value) {
            ZappedIcon(iconSize)
        } else {
            ZapIcon(iconSize, grayTint)
        }
    }
}

@Composable
private fun WatchZapsForNote(baseNote: Note, accountViewModel: AccountViewModel, onWasZapped: (Boolean) -> Unit) {
    val zapsState by baseNote.live().zaps.observeAsState()

    LaunchedEffect(key1 = zapsState) {
        launch(Dispatchers.Default) {
            onWasZapped(accountViewModel.calculateIfNoteWasZappedByAccount(baseNote))
        }
    }
}

@Composable
private fun ZapAmountText(
    baseNote: Note,
    grayTint: Color,
    accountViewModel: AccountViewModel
) {
    val zapAmountTxt = remember(baseNote) { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    WatchZapAmountsForNote(baseNote, accountViewModel) { newZapAmount ->
        if (zapAmountTxt.value != newZapAmount) {
            scope.launch(Dispatchers.Main) {
                zapAmountTxt.value = newZapAmount
            }
        }
    }

    SlidingAnimationAmount(zapAmountTxt, grayTint)
}

@Composable
fun WatchZapAmountsForNote(baseNote: Note, accountViewModel: AccountViewModel, onZapAmount: (String) -> Unit) {
    val zapsState by baseNote.live().zaps.observeAsState()

    LaunchedEffect(key1 = zapsState) {
        launch(Dispatchers.Default) {
            onZapAmount(showAmount(accountViewModel.calculateZapAmount(baseNote)))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoostTypeChoicePopup(baseNote: Note, accountViewModel: AccountViewModel, onDismiss: () -> Unit, onQuote: () -> Unit) {
    Popup(
        alignment = Alignment.BottomCenter,
        offset = IntOffset(0, -50),
        onDismissRequest = { onDismiss() }
    ) {
        FlowRow {
            val scope = rememberCoroutineScope()
            Button(
                modifier = Modifier.padding(horizontal = 3.dp),
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        accountViewModel.boost(baseNote)
                        onDismiss()
                    }
                },
                shape = ButtonBorder,
                colors = ButtonDefaults
                    .buttonColors(
                        backgroundColor = MaterialTheme.colors.primary
                    )
            ) {
                Text(stringResource(R.string.boost), color = Color.White, textAlign = TextAlign.Center)
            }

            Button(
                modifier = Modifier.padding(horizontal = 3.dp),
                onClick = onQuote,
                shape = ButtonBorder,
                colors = ButtonDefaults
                    .buttonColors(
                        backgroundColor = MaterialTheme.colors.primary
                    )
            ) {
                Text(stringResource(R.string.quote), color = Color.White, textAlign = TextAlign.Center)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReactionChoicePopup(
    baseNote: Note,
    accountViewModel: AccountViewModel,
    onDismiss: () -> Unit,
    onChangeAmount: () -> Unit
) {
    val accountState by accountViewModel.accountLiveData.observeAsState()
    val account = accountState?.account ?: return

    val toRemove = remember {
        baseNote.reactedBy(account.userProfile()).toSet()
    }

    Popup(
        alignment = Alignment.BottomCenter,
        offset = IntOffset(0, -50),
        onDismissRequest = { onDismiss() }
    ) {
        FlowRow(horizontalArrangement = Arrangement.Center) {
            account.reactionChoices.forEach { reactionType ->
                ActionableReactionButton(
                    baseNote,
                    reactionType,
                    accountViewModel,
                    onDismiss,
                    onChangeAmount,
                    toRemove
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun ActionableReactionButton(
    baseNote: Note,
    reactionType: String,
    accountViewModel: AccountViewModel,
    onDismiss: () -> Unit,
    onChangeAmount: () -> Unit,
    toRemove: Set<String>
) {
    val scope = rememberCoroutineScope()

    Button(
        modifier = Modifier.padding(horizontal = 3.dp),
        onClick = {
            scope.launch(Dispatchers.IO) {
                accountViewModel.reactToOrDelete(
                    baseNote,
                    reactionType
                )
                onDismiss()
            }
        },
        shape = ButtonBorder,
        colors = ButtonDefaults
            .buttonColors(
                backgroundColor = MaterialTheme.colors.primary
            )
    ) {
        val thisModifier = remember(reactionType) {
            Modifier.combinedClickable(
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        accountViewModel.reactToOrDelete(
                            baseNote,
                            reactionType
                        )
                        onDismiss()
                    }
                },
                onLongClick = {
                    onChangeAmount()
                }
            )
        }

        val removeSymbol = remember(reactionType) {
            if (reactionType in toRemove) {
                " ✖"
            } else {
                ""
            }
        }

        if (reactionType.startsWith(":")) {
            val noStartColon = reactionType.removePrefix(":")
            val url = noStartColon.substringAfter(":")

            val renderable = listOf(
                ImageUrlType(url),
                TextType(removeSymbol)
            ).toImmutableList()

            InLineIconRenderer(
                renderable,
                style = SpanStyle(color = Color.White),
                maxLines = 1
            )
        } else {
            when (reactionType) {
                "+" -> {
                    Icon(
                        painter = painterResource(R.drawable.ic_liked),
                        null,
                        modifier = remember { thisModifier.size(16.dp) },
                        tint = Color.White
                    )
                    Text(
                        text = removeSymbol,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = thisModifier
                    )
                }

                "-" -> Text(
                    text = "\uD83D\uDC4E$removeSymbol",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = thisModifier
                )

                else -> Text(
                    "$reactionType$removeSymbol",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = thisModifier
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun ZapAmountChoicePopup(
    baseNote: Note,
    accountViewModel: AccountViewModel,
    onDismiss: () -> Unit,
    onChangeAmount: () -> Unit,
    onError: (text: String) -> Unit,
    onProgress: (percent: Float) -> Unit
) {
    val context = LocalContext.current

    val accountState by accountViewModel.accountLiveData.observeAsState()
    val account = accountState?.account ?: return
    val zapMessage = ""
    val scope = rememberCoroutineScope()

    Popup(
        alignment = Alignment.BottomCenter,
        offset = IntOffset(0, -50),
        onDismissRequest = { onDismiss() }
    ) {
        FlowRow(horizontalArrangement = Arrangement.Center) {
            account.zapAmountChoices.forEach { amountInSats ->
                Button(
                    modifier = Modifier.padding(horizontal = 3.dp),
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            accountViewModel.zap(
                                baseNote,
                                amountInSats * 1000,
                                null,
                                zapMessage,
                                context,
                                onError,
                                onProgress,
                                account.defaultZapType
                            )
                            onDismiss()
                        }
                    },
                    shape = ButtonBorder,
                    colors = ButtonDefaults
                        .buttonColors(
                            backgroundColor = MaterialTheme.colors.primary
                        )
                ) {
                    Text(
                        "⚡ ${showAmount(amountInSats.toBigDecimal().setScale(1))}",
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.combinedClickable(
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    accountViewModel.zap(
                                        baseNote,
                                        amountInSats * 1000,
                                        null,
                                        zapMessage,
                                        context,
                                        onError,
                                        onProgress,
                                        account.defaultZapType
                                    )
                                    onDismiss()
                                }
                            },
                            onLongClick = {
                                onChangeAmount()
                            }
                        )
                    )
                }
            }
        }
    }
}

fun showCount(count: Int?): String {
    if (count == null) return ""
    if (count == 0) return ""

    return when {
        count >= 1000000000 -> "${(count / 1000000000f).roundToInt()}G"
        count >= 1000000 -> "${(count / 1000000f).roundToInt()}M"
        count >= 1000 -> "${(count / 1000f).roundToInt()}k"
        else -> "$count"
    }
}

val OneGiga = BigDecimal(1000000000)
val OneMega = BigDecimal(1000000)
val OneKilo = BigDecimal(1000)

fun showAmount(amount: BigDecimal?): String {
    if (amount == null) return ""
    if (amount.abs() < BigDecimal(0.01)) return ""

    return when {
        amount >= OneGiga -> "%.1fG".format(amount.div(OneGiga).setScale(1, RoundingMode.HALF_UP))
        amount >= OneMega -> "%.1fM".format(amount.div(OneMega).setScale(1, RoundingMode.HALF_UP))
        amount >= OneKilo -> "%.1fk".format(amount.div(OneKilo).setScale(1, RoundingMode.HALF_UP))
        else -> "%.0f".format(amount)
    }
}
