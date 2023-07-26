package com.vitorpamplona.amethyst.ui.screen.loggedIn

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.vitorpamplona.amethyst.R
import com.vitorpamplona.amethyst.model.Account
import com.vitorpamplona.amethyst.model.LocalCache
import com.vitorpamplona.amethyst.model.Note
import com.vitorpamplona.amethyst.model.User
import com.vitorpamplona.amethyst.service.NostrUserProfileDataSource
import com.vitorpamplona.amethyst.service.model.AppDefinitionEvent
import com.vitorpamplona.amethyst.service.model.IdentityClaim
import com.vitorpamplona.amethyst.service.model.PayInvoiceErrorResponse
import com.vitorpamplona.amethyst.service.model.PayInvoiceSuccessResponse
import com.vitorpamplona.amethyst.service.model.ReportEvent
import com.vitorpamplona.amethyst.ui.actions.ImmutableListOfLists
import com.vitorpamplona.amethyst.ui.actions.NewUserMetadataView
import com.vitorpamplona.amethyst.ui.actions.toImmutableListOfLists
import com.vitorpamplona.amethyst.ui.components.CreateTextWithEmoji
import com.vitorpamplona.amethyst.ui.components.DisplayNip05ProfileStatus
import com.vitorpamplona.amethyst.ui.components.InvoiceRequestCard
import com.vitorpamplona.amethyst.ui.components.TranslatableRichTextViewer
import com.vitorpamplona.amethyst.ui.components.ZoomableImageDialog
import com.vitorpamplona.amethyst.ui.components.figureOutMimeType
import com.vitorpamplona.amethyst.ui.dal.UserProfileReportsFeedFilter
import com.vitorpamplona.amethyst.ui.navigation.ShowQRDialog
import com.vitorpamplona.amethyst.ui.note.ClickableUserPicture
import com.vitorpamplona.amethyst.ui.note.LightningAddressIcon
import com.vitorpamplona.amethyst.ui.screen.FeedState
import com.vitorpamplona.amethyst.ui.screen.NostrUserAppRecommendationsFeedViewModel
import com.vitorpamplona.amethyst.ui.screen.NostrUserProfileBookmarksFeedViewModel
import com.vitorpamplona.amethyst.ui.screen.NostrUserProfileConversationsFeedViewModel
import com.vitorpamplona.amethyst.ui.screen.NostrUserProfileFollowsUserFeedViewModel
import com.vitorpamplona.amethyst.ui.screen.NostrUserProfileNewThreadsFeedViewModel
import com.vitorpamplona.amethyst.ui.screen.NostrUserProfileReportFeedViewModel
import com.vitorpamplona.amethyst.ui.screen.RefresheableFeedView
import com.vitorpamplona.amethyst.ui.screen.RefreshingFeedUserFeedView
import com.vitorpamplona.amethyst.ui.screen.RelayFeedView
import com.vitorpamplona.amethyst.ui.screen.RelayFeedViewModel
import com.vitorpamplona.amethyst.ui.screen.UserFeedViewModel
import com.vitorpamplona.amethyst.ui.theme.BitcoinOrange
import com.vitorpamplona.amethyst.ui.theme.ButtonBorder
import com.vitorpamplona.amethyst.ui.theme.Size16Modifier
import com.vitorpamplona.amethyst.ui.theme.Size35dp
import com.vitorpamplona.amethyst.ui.theme.placeholderText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(userId: String?, accountViewModel: AccountViewModel, nav: (String) -> Unit) {
    if (userId == null) return

    var userBase by remember { mutableStateOf<User?>(LocalCache.getUserIfExists(userId)) }

    if (userBase == null) {
        LaunchedEffect(userId) {
            // waits to resolve.
            launch(Dispatchers.IO) {
                val newUserBase = LocalCache.checkGetOrCreateUser(userId)
                if (newUserBase != userBase) {
                    userBase = newUserBase
                }
            }
        }
    }

    userBase?.let {
        PrepareViewModels(
            baseUser = it,
            accountViewModel = accountViewModel,
            nav = nav
        )
    }
}

@Composable
fun PrepareViewModels(baseUser: User, accountViewModel: AccountViewModel, nav: (String) -> Unit) {
    val followsFeedViewModel: NostrUserProfileFollowsUserFeedViewModel = viewModel(
        key = baseUser.pubkeyHex + "UserProfileFollowsUserFeedViewModel",
        factory = NostrUserProfileFollowsUserFeedViewModel.Factory(
            baseUser,
            accountViewModel.account
        )
    )

    val appRecommendations: NostrUserAppRecommendationsFeedViewModel = viewModel(
        key = baseUser.pubkeyHex + "UserAppRecommendationsFeedViewModel",
        factory = NostrUserAppRecommendationsFeedViewModel.Factory(
            baseUser
        )
    )

    val threadsViewModel: NostrUserProfileNewThreadsFeedViewModel = viewModel(
        key = baseUser.pubkeyHex + "UserProfileNewThreadsFeedViewModel",
        factory = NostrUserProfileNewThreadsFeedViewModel.Factory(
            baseUser,
            accountViewModel.account
        )
    )

    val repliesViewModel: NostrUserProfileConversationsFeedViewModel = viewModel(
        key = baseUser.pubkeyHex + "UserProfileConversationsFeedViewModel",
        factory = NostrUserProfileConversationsFeedViewModel.Factory(
            baseUser,
            accountViewModel.account
        )
    )

    val bookmarksFeedViewModel: NostrUserProfileBookmarksFeedViewModel = viewModel(
        key = baseUser.pubkeyHex + "UserProfileBookmarksFeedViewModel",
        factory = NostrUserProfileBookmarksFeedViewModel.Factory(
            baseUser,
            accountViewModel.account
        )
    )

    val reportsFeedViewModel: NostrUserProfileReportFeedViewModel = viewModel(
        key = baseUser.pubkeyHex + "UserProfileReportFeedViewModel",
        factory = NostrUserProfileReportFeedViewModel.Factory(
            baseUser
        )
    )

    ProfileScreen(
        baseUser = baseUser,
        threadsViewModel,
        repliesViewModel,
        followsFeedViewModel,
        appRecommendations,
        bookmarksFeedViewModel,
        reportsFeedViewModel,
        accountViewModel = accountViewModel,
        nav = nav
    )
}

@Composable
fun ProfileScreen(
    baseUser: User,
    threadsViewModel: NostrUserProfileNewThreadsFeedViewModel,
    repliesViewModel: NostrUserProfileConversationsFeedViewModel,
    followsFeedViewModel: NostrUserProfileFollowsUserFeedViewModel,
    appRecommendations: NostrUserAppRecommendationsFeedViewModel,
    bookmarksFeedViewModel: NostrUserProfileBookmarksFeedViewModel,
    reportsFeedViewModel: NostrUserProfileReportFeedViewModel,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    NostrUserProfileDataSource.loadUserProfile(baseUser)

    val lifeCycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        NostrUserProfileDataSource.start()
    }

    DisposableEffect(accountViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                println("Profidle Start")
                NostrUserProfileDataSource.loadUserProfile(baseUser)
                NostrUserProfileDataSource.start()
            }
            if (event == Lifecycle.Event.ON_PAUSE) {
                println("Profile Stop")
                NostrUserProfileDataSource.loadUserProfile(null)
                NostrUserProfileDataSource.stop()
            }
        }

        lifeCycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifeCycleOwner.lifecycle.removeObserver(observer)
            println("Profile Dispose")
            NostrUserProfileDataSource.loadUserProfile(null)
            NostrUserProfileDataSource.stop()
        }
    }

    RenderSurface(
        baseUser,
        threadsViewModel,
        repliesViewModel,
        appRecommendations,
        followsFeedViewModel,
        bookmarksFeedViewModel,
        reportsFeedViewModel,
        accountViewModel,
        nav
    )
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun RenderSurface(
    baseUser: User,
    threadsViewModel: NostrUserProfileNewThreadsFeedViewModel,
    repliesViewModel: NostrUserProfileConversationsFeedViewModel,
    appRecommendations: NostrUserAppRecommendationsFeedViewModel,
    followsFeedViewModel: NostrUserProfileFollowsUserFeedViewModel,
    bookmarksFeedViewModel: NostrUserProfileBookmarksFeedViewModel,
    reportsFeedViewModel: NostrUserProfileReportFeedViewModel,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colors.background
    ) {
        var columnSize by remember { mutableStateOf(IntSize.Zero) }
        var tabsSize by remember { mutableStateOf(IntSize.Zero) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged {
                    columnSize = it
                }
        ) {
            val pagerState = rememberPagerState()
            val coroutineScope = rememberCoroutineScope()
            val scrollState = rememberScrollState()

            val tabRowModifier = remember {
                Modifier.onSizeChanged {
                    tabsSize = it
                }
            }

            val pagerModifier = with(LocalDensity.current) {
                Modifier.height((columnSize.height - tabsSize.height).toDp())
            }

            Box(
                modifier = remember {
                    Modifier
                        .verticalScroll(scrollState)
                        .nestedScroll(object : NestedScrollConnection {
                            override fun onPreScroll(
                                available: Offset,
                                source: NestedScrollSource
                            ): Offset {
                                // When scrolling vertically, scroll the container first.
                                return if (available.y < 0 && scrollState.canScrollForward) {
                                    coroutineScope.launch {
                                        scrollState.scrollBy(-available.y)
                                    }
                                    Offset(0f, available.y)
                                } else {
                                    Offset.Zero
                                }
                            }
                        })
                        .fillMaxHeight()
                }
            ) {
                RenderScreen(
                    baseUser,
                    pagerState,
                    tabRowModifier,
                    pagerModifier,
                    threadsViewModel,
                    repliesViewModel,
                    appRecommendations,
                    followsFeedViewModel,
                    bookmarksFeedViewModel,
                    reportsFeedViewModel,
                    accountViewModel,
                    nav
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun RenderScreen(
    baseUser: User,
    pagerState: PagerState,
    tabRowModifier: Modifier,
    pagerModifier: Modifier,
    threadsViewModel: NostrUserProfileNewThreadsFeedViewModel,
    repliesViewModel: NostrUserProfileConversationsFeedViewModel,
    appRecommendations: NostrUserAppRecommendationsFeedViewModel,
    followsFeedViewModel: NostrUserProfileFollowsUserFeedViewModel,
    bookmarksFeedViewModel: NostrUserProfileBookmarksFeedViewModel,
    reportsFeedViewModel: NostrUserProfileReportFeedViewModel,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    Column() {
        ProfileHeader(baseUser, appRecommendations, nav, accountViewModel)
        ScrollableTabRow(
            backgroundColor = MaterialTheme.colors.background,
            selectedTabIndex = pagerState.currentPage,
            edgePadding = 8.dp,
            modifier = tabRowModifier
        ) {
            CreateAndRenderTabs(baseUser, pagerState)
        }
        HorizontalPager(
            pageCount = 7,
            state = pagerState,
            modifier = pagerModifier
        ) { page ->
            CreateAndRenderPages(
                page,
                baseUser,
                threadsViewModel,
                repliesViewModel,
                followsFeedViewModel,
                bookmarksFeedViewModel,
                reportsFeedViewModel,
                accountViewModel,
                nav
            )
        }
    }
}

@Composable
private fun CreateAndRenderPages(
    page: Int,
    baseUser: User,
    threadsViewModel: NostrUserProfileNewThreadsFeedViewModel,
    repliesViewModel: NostrUserProfileConversationsFeedViewModel,
    followsFeedViewModel: NostrUserProfileFollowsUserFeedViewModel,
    bookmarksFeedViewModel: NostrUserProfileBookmarksFeedViewModel,
    reportsFeedViewModel: NostrUserProfileReportFeedViewModel,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    when (page) {
        0 -> TabNotesNewThreads(threadsViewModel, accountViewModel, nav)
        1 -> TabNotesConversations(repliesViewModel, accountViewModel, nav)
        2 -> TabFollows(baseUser, followsFeedViewModel, accountViewModel, nav)
        3 -> TabBookmarks(bookmarksFeedViewModel, accountViewModel, nav)
        4 -> TabFollowedTags(baseUser, accountViewModel, nav)
        5 -> TabReports(baseUser, reportsFeedViewModel, accountViewModel, nav)
        6 -> TabRelays(baseUser, accountViewModel, nav)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CreateAndRenderTabs(
    baseUser: User,
    pagerState: PagerState
) {
    val coroutineScope = rememberCoroutineScope()

    val tabs = listOf<@Composable() (() -> Unit)?>(
        { Text(text = stringResource(R.string.notes)) },
        { Text(text = stringResource(R.string.replies)) },
        { FollowTabHeader(baseUser) },
        { BookmarkTabHeader(baseUser) },
        { FollowedTagsTabHeader(baseUser) },
        { ReportsTabHeader(baseUser) },
        { RelaysTabHeader(baseUser) }
    )

    tabs.forEachIndexed { index, function ->
        Tab(
            selected = pagerState.currentPage == index,
            onClick = {
                coroutineScope.launch { pagerState.animateScrollToPage(index) }
            },
            text = function
        )
    }
}

@Composable
private fun RelaysTabHeader(baseUser: User) {
    val userState by baseUser.live().relays.observeAsState()
    val userRelaysBeingUsed = remember(userState) { userState?.user?.relaysBeingUsed?.size ?: "--" }

    val userStateRelayInfo by baseUser.live().relayInfo.observeAsState()
    val userRelays = remember(userStateRelayInfo) { userStateRelayInfo?.user?.latestContactList?.relays()?.size ?: "--" }

    Text(text = "$userRelaysBeingUsed / $userRelays ${stringResource(R.string.relays)}")
}

@Composable
private fun ReportsTabHeader(baseUser: User) {
    val userState by baseUser.live().reports.observeAsState()
    var userReports by remember { mutableStateOf(0) }

    LaunchedEffect(key1 = userState) {
        launch(Dispatchers.IO) {
            val newSize = UserProfileReportsFeedFilter(baseUser).feed().size

            if (newSize != userReports) {
                userReports = newSize
            }
        }
    }

    Text(text = "$userReports ${stringResource(R.string.reports)}")
}

@Composable
private fun FollowedTagsTabHeader(baseUser: User) {
    var usertags by remember { mutableStateOf(0) }

    LaunchedEffect(key1 = baseUser) {
        launch(Dispatchers.IO) {
            val contactList = baseUser?.latestContactList

            val newTags = (contactList?.verifiedFollowTagSet?.count() ?: 0)

            if (newTags != usertags) {
                usertags = newTags
            }
        }
    }

    Text(text = "$usertags ${stringResource(R.string.followed_tags)}")
}

@Composable
private fun BookmarkTabHeader(baseUser: User) {
    val userState by baseUser.live().bookmarks.observeAsState()

    var userBookmarks by remember { mutableStateOf(0) }

    LaunchedEffect(key1 = userState) {
        launch(Dispatchers.IO) {
            val bookmarkList = userState?.user?.latestBookmarkList

            val newBookmarks = (bookmarkList?.taggedEvents()?.count() ?: 0) + (bookmarkList?.taggedAddresses()?.count() ?: 0)

            if (newBookmarks != userBookmarks) {
                userBookmarks = newBookmarks
            }
        }
    }

    Text(text = "$userBookmarks ${stringResource(R.string.bookmarks)}")
}

@Composable
private fun FollowTabHeader(baseUser: User) {
    val userState by baseUser.live().follows.observeAsState()
    var followCount by remember { mutableStateOf("--") }

    val text = stringResource(R.string.follows)

    LaunchedEffect(key1 = userState) {
        launch(Dispatchers.IO) {
            val newFollow = (userState?.user?.transientFollowCount()?.toString() ?: "--") + " " + text

            if (followCount != newFollow) {
                followCount = newFollow
            }
        }
    }

    Text(text = followCount)
}

@Composable
private fun ProfileHeader(
    baseUser: User,
    appRecommendations: NostrUserAppRecommendationsFeedViewModel,
    nav: (String) -> Unit,
    accountViewModel: AccountViewModel
) {
    var popupExpanded by remember { mutableStateOf(false) }
    var zoomImageDialogOpen by remember { mutableStateOf(false) }

    Box {
        DrawBanner(baseUser, accountViewModel)

        Box(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .size(40.dp)
                .align(Alignment.TopEnd)
        ) {
            Button(
                modifier = Modifier
                    .size(30.dp)
                    .align(Alignment.Center),
                onClick = { popupExpanded = true },
                shape = ButtonBorder,
                colors = ButtonDefaults
                    .buttonColors(
                        backgroundColor = MaterialTheme.colors.background
                    ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    tint = MaterialTheme.colors.placeholderText,
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.more_options)
                )

                UserProfileDropDownMenu(baseUser, popupExpanded, { popupExpanded = false }, accountViewModel)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
                .padding(top = 75.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                val clipboardManager = LocalClipboardManager.current

                ClickableUserPicture(
                    baseUser = baseUser,
                    accountViewModel = accountViewModel,
                    size = 100.dp,
                    modifier = Modifier.border(
                        3.dp,
                        MaterialTheme.colors.background,
                        CircleShape
                    ),
                    onClick = {
                        if (baseUser.profilePicture() != null) {
                            zoomImageDialogOpen = true
                        }
                    },
                    onLongClick = {
                        it.info?.picture?.let { it1 ->
                            clipboardManager.setText(
                                AnnotatedString(it1)
                            )
                        }
                    }
                )

                Spacer(Modifier.weight(1f))

                Row(
                    modifier = Modifier
                        .height(Size35dp)
                        .padding(bottom = 3.dp)
                ) {
                    MessageButton(baseUser, nav)

                    // No need for this button anymore
                    // NPubCopyButton(baseUser)

                    ProfileActions(baseUser, accountViewModel)
                }
            }

            DrawAdditionalInfo(baseUser, appRecommendations, accountViewModel, nav)

            Divider(modifier = Modifier.padding(top = 6.dp))
        }
    }

    val profilePic = baseUser.profilePicture()
    if (zoomImageDialogOpen && profilePic != null) {
        ZoomableImageDialog(figureOutMimeType(profilePic), onDismiss = { zoomImageDialogOpen = false }, accountViewModel = accountViewModel)
    }
}

@Composable
private fun ProfileActions(
    baseUser: User,
    accountViewModel: AccountViewModel
) {
    val isMe by remember(accountViewModel) {
        derivedStateOf {
            accountViewModel.userProfile() == baseUser
        }
    }

    if (isMe) {
        EditButton(accountViewModel.account)
    }

    WatchIsHiddenUser(baseUser, accountViewModel) { isHidden ->
        if (isHidden) {
            val scope = rememberCoroutineScope()
            ShowUserButton {
                scope.launch(Dispatchers.IO) {
                    accountViewModel.account.showUser(baseUser.pubkeyHex)
                }
            }
        } else {
            DisplayFollowUnfollowButton(baseUser, accountViewModel)
        }
    }
}

@Composable
private fun DisplayFollowUnfollowButton(
    baseUser: User,
    accountViewModel: AccountViewModel
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val isLoggedInFollowingUser by accountViewModel.account.userProfile().live().follows.map {
        it.user.isFollowing(baseUser)
    }.distinctUntilChanged().observeAsState(initial = accountViewModel.account.isFollowing(baseUser))

    val isUserFollowingLoggedIn by baseUser.live().follows.map {
        it.user.isFollowing(accountViewModel.account.userProfile())
    }.distinctUntilChanged().observeAsState(initial = baseUser.isFollowing(accountViewModel.account.userProfile()))

    if (isLoggedInFollowingUser) {
        UnfollowButton {
            if (!accountViewModel.isWriteable()) {
                scope.launch {
                    Toast
                        .makeText(
                            context,
                            context.getString(R.string.login_with_a_private_key_to_be_able_to_unfollow),
                            Toast.LENGTH_SHORT
                        )
                        .show()
                }
            } else {
                scope.launch(Dispatchers.IO) {
                    accountViewModel.account.unfollow(baseUser)
                }
            }
        }
    } else {
        if (isUserFollowingLoggedIn) {
            FollowButton(R.string.follow_back) {
                if (!accountViewModel.isWriteable()) {
                    scope.launch {
                        Toast
                            .makeText(
                                context,
                                context.getString(R.string.login_with_a_private_key_to_be_able_to_follow),
                                Toast.LENGTH_SHORT
                            )
                            .show()
                    }
                } else {
                    scope.launch(Dispatchers.IO) {
                        accountViewModel.account.follow(baseUser)
                    }
                }
            }
        } else {
            FollowButton(R.string.follow) {
                if (!accountViewModel.isWriteable()) {
                    scope.launch {
                        Toast
                            .makeText(
                                context,
                                context.getString(R.string.login_with_a_private_key_to_be_able_to_follow),
                                Toast.LENGTH_SHORT
                            )
                            .show()
                    }
                } else {
                    scope.launch(Dispatchers.IO) {
                        accountViewModel.account.follow(baseUser)
                    }
                }
            }
        }
    }
}

@Composable
private fun WatchIsHiddenUser(baseUser: User, accountViewModel: AccountViewModel, content: @Composable (Boolean) -> Unit) {
    val isHidden by accountViewModel.account.liveHiddenUsers.map {
        it.hiddenUsers.contains(baseUser.pubkeyHex) || it.spammers.contains(baseUser.pubkeyHex)
    }.observeAsState(accountViewModel.account.isHidden(baseUser))

    content(isHidden)
}

@Composable
private fun DrawAdditionalInfo(
    baseUser: User,
    appRecommendations: NostrUserAppRecommendationsFeedViewModel,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    val userState by baseUser.live().metadata.observeAsState()
    val user = remember(userState) { userState?.user } ?: return
    val tags = remember(userState) { userState?.user?.info?.latestMetadata?.tags?.toImmutableListOfLists() }

    val uri = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current

    (user.bestDisplayName() ?: user.bestUsername())?.let {
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 7.dp)) {
            CreateTextWithEmoji(
                text = it,
                tags = tags,
                fontWeight = FontWeight.Bold,
                fontSize = 25.sp
            )
        }
    }

    if (user.bestDisplayName() != null) {
        user.bestUsername()?.let {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 1.dp, bottom = 1.dp)
            ) {
                CreateTextWithEmoji(
                    text = "@$it",
                    tags = tags,
                    color = MaterialTheme.colors.placeholderText
                )
            }
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = user.pubkeyDisplayHex(),
            modifier = Modifier.padding(top = 1.dp, bottom = 1.dp),
            color = MaterialTheme.colors.placeholderText
        )

        IconButton(
            modifier = Modifier
                .size(25.dp)
                .padding(start = 5.dp),
            onClick = { clipboardManager.setText(AnnotatedString(user.pubkeyNpub())); }
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                null,
                modifier = Modifier.size(15.dp),
                tint = MaterialTheme.colors.placeholderText
            )
        }

        var dialogOpen by remember {
            mutableStateOf(false)
        }

        if (dialogOpen) {
            ShowQRDialog(
                user,
                onScan = {
                    dialogOpen = false
                    nav(it)
                },
                onClose = { dialogOpen = false }
            )
        }

        IconButton(
            modifier = Modifier.size(25.dp),
            onClick = { dialogOpen = true }
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_qrcode),
                null,
                modifier = Modifier.size(15.dp),
                tint = MaterialTheme.colors.placeholderText
            )
        }
    }

    DisplayNip05ProfileStatus(user)

    val website = user.info?.website
    if (!website.isNullOrEmpty()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                tint = MaterialTheme.colors.placeholderText,
                imageVector = Icons.Default.Link,
                contentDescription = stringResource(R.string.website),
                modifier = Modifier.size(16.dp)
            )

            ClickableText(
                text = AnnotatedString(website.removePrefix("https://")),
                onClick = { website.let { runCatching { uri.openUri(it) } } },
                style = LocalTextStyle.current.copy(color = MaterialTheme.colors.primary),
                modifier = Modifier.padding(top = 1.dp, bottom = 1.dp, start = 5.dp)
            )
        }
    }

    val lud16 = remember(userState) { user.info?.lud16?.trim() ?: user.info?.lud06?.trim() }
    val pubkeyHex = remember { baseUser.pubkeyHex }
    DisplayLNAddress(lud16, pubkeyHex, accountViewModel.account)

    val identities = user.info?.latestMetadata?.identityClaims()
    if (!identities.isNullOrEmpty()) {
        identities.forEach { identity: IdentityClaim ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    tint = Color.Unspecified,
                    painter = painterResource(id = identity.toIcon()),
                    contentDescription = stringResource(identity.toDescriptor()),
                    modifier = Modifier.size(16.dp)
                )

                ClickableText(
                    text = AnnotatedString(identity.identity),
                    onClick = { runCatching { uri.openUri(identity.toProofUrl()) } },
                    style = LocalTextStyle.current.copy(color = MaterialTheme.colors.primary),
                    modifier = Modifier
                        .padding(top = 1.dp, bottom = 1.dp, start = 5.dp)
                        .weight(1f)
                )
            }
        }
    }

    user.info?.about?.let {
        Row(
            modifier = Modifier.padding(top = 5.dp, bottom = 5.dp)
        ) {
            val defaultBackground = MaterialTheme.colors.background
            val background = remember {
                mutableStateOf(defaultBackground)
            }

            TranslatableRichTextViewer(
                content = it,
                canPreview = false,
                tags = remember { ImmutableListOfLists(emptyList()) },
                backgroundColor = background,
                accountViewModel = accountViewModel,
                nav = nav
            )
        }
    }

    DisplayAppRecommendations(appRecommendations, nav)
}

@Composable
fun DisplayLNAddress(
    lud16: String?,
    userHex: String,
    account: Account
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var zapExpanded by remember { mutableStateOf(false) }

    if (!lud16.isNullOrEmpty()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LightningAddressIcon(modifier = Size16Modifier, tint = BitcoinOrange)

            ClickableText(
                text = AnnotatedString(lud16),
                onClick = { zapExpanded = !zapExpanded },
                style = LocalTextStyle.current.copy(color = MaterialTheme.colors.primary),
                modifier = Modifier
                    .padding(top = 1.dp, bottom = 1.dp, start = 5.dp)
                    .weight(1f)
            )
        }

        if (zapExpanded) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 5.dp)
            ) {
                InvoiceRequestCard(
                    lud16,
                    userHex,
                    account,
                    onSuccess = {
                        zapExpanded = false
                        // pay directly
                        if (account.hasWalletConnectSetup()) {
                            account.sendZapPaymentRequestFor(it, null) { response ->
                                if (response is PayInvoiceSuccessResponse) {
                                    scope.launch {
                                        Toast.makeText(
                                            context,
                                            "Payment Successful", // Turn this into a UI animation
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                } else if (response is PayInvoiceErrorResponse) {
                                    scope.launch {
                                        Toast.makeText(
                                            context,
                                            response.error?.message
                                                ?: response.error?.code?.toString()
                                                ?: "Error parsing error message",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        } else {
                            runCatching {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("lightning:$it"))
                                ContextCompat.startActivity(context, intent, null)
                            }
                        }
                    },
                    onClose = {
                        zapExpanded = false
                    }
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun DisplayAppRecommendations(
    appRecommendations: NostrUserAppRecommendationsFeedViewModel,
    nav: (String) -> Unit
) {
    val feedState by appRecommendations.feedContent.collectAsState()

    LaunchedEffect(key1 = Unit) {
        appRecommendations.invalidateData()
    }

    Crossfade(
        targetState = feedState,
        animationSpec = tween(durationMillis = 100)
    ) { state ->
        when (state) {
            is FeedState.Loaded -> {
                Column() {
                    Text(stringResource(id = R.string.recommended_apps))

                    FlowRow(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 5.dp)
                    ) {
                        state.feed.value.forEach { app ->
                            WatchApp(app, nav)
                        }
                    }
                }
            }

            else -> {}
        }
    }
}

@Composable
private fun WatchApp(baseApp: Note, nav: (String) -> Unit) {
    val appState by baseApp.live().metadata.observeAsState()

    var appLogo by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(key1 = appState) {
        launch(Dispatchers.Default) {
            val newAppLogo = (appState?.note?.event as? AppDefinitionEvent)?.appMetaData()?.picture?.ifBlank { null }
            if (newAppLogo != appLogo) {
                appLogo = newAppLogo
            }
        }
    }

    appLogo?.let {
        Box(
            remember {
                Modifier
                    .size(Size35dp)
                    .clickable {
                        nav("Note/${baseApp.idHex}")
                    }
            }
        ) {
            AsyncImage(
                model = appLogo,
                contentDescription = null,
                modifier = remember {
                    Modifier
                        .size(Size35dp)
                        .clip(shape = CircleShape)
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DrawBanner(baseUser: User, accountViewModel: AccountViewModel) {
    val userState by baseUser.live().metadata.observeAsState()
    val banner = remember(userState) { userState?.user?.info?.banner }

    val clipboardManager = LocalClipboardManager.current
    var zoomImageDialogOpen by remember { mutableStateOf(false) }

    if (!banner.isNullOrBlank()) {
        AsyncImage(
            model = banner,
            contentDescription = stringResource(id = R.string.profile_image),
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
                .height(125.dp)
                .combinedClickable(
                    onClick = { zoomImageDialogOpen = true },
                    onLongClick = {
                        clipboardManager.setText(AnnotatedString(banner))
                    }
                )
        )

        if (zoomImageDialogOpen) {
            ZoomableImageDialog(imageUrl = figureOutMimeType(banner), onDismiss = { zoomImageDialogOpen = false }, accountViewModel = accountViewModel)
        }
    } else {
        Image(
            painter = painterResource(R.drawable.profile_banner),
            contentDescription = stringResource(id = R.string.profile_banner),
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
                .height(125.dp)
        )
    }
}

@Composable
fun TabNotesNewThreads(feedViewModel: NostrUserProfileNewThreadsFeedViewModel, accountViewModel: AccountViewModel, nav: (String) -> Unit) {
    LaunchedEffect(Unit) {
        feedViewModel.invalidateData()
    }

    Column(Modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier.padding(vertical = 0.dp)
        ) {
            RefresheableFeedView(
                feedViewModel,
                null,
                enablePullRefresh = false,
                accountViewModel = accountViewModel,
                nav = nav
            )
        }
    }
}

@Composable
fun TabNotesConversations(feedViewModel: NostrUserProfileConversationsFeedViewModel, accountViewModel: AccountViewModel, nav: (String) -> Unit) {
    LaunchedEffect(Unit) {
        feedViewModel.invalidateData()
    }

    Column(Modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier.padding(vertical = 0.dp)
        ) {
            RefresheableFeedView(
                feedViewModel,
                null,
                enablePullRefresh = false,
                accountViewModel = accountViewModel,
                nav = nav
            )
        }
    }
}

@Composable
fun TabFollowedTags(baseUser: User, account: AccountViewModel, nav: (String) -> Unit) {
    Column(Modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier.padding(vertical = 0.dp)
        ) {
            baseUser.latestContactList?.let {
                it.unverifiedFollowTagSet().forEach { hashtag ->
                    HashtagHeader(
                        tag = hashtag,
                        account = account,
                        onClick = {
                            nav("Hashtag/$hashtag")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TabBookmarks(feedViewModel: NostrUserProfileBookmarksFeedViewModel, accountViewModel: AccountViewModel, nav: (String) -> Unit) {
    LaunchedEffect(Unit) {
        feedViewModel.invalidateData()
    }

    Column(Modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier.padding(vertical = 0.dp)
        ) {
            RefresheableFeedView(
                feedViewModel,
                null,
                enablePullRefresh = false,
                accountViewModel = accountViewModel,
                nav = nav
            )
        }
    }
}

@Composable
fun TabFollows(baseUser: User, feedViewModel: UserFeedViewModel, accountViewModel: AccountViewModel, nav: (String) -> Unit) {
    WatchFollowChanges(baseUser, feedViewModel)

    Column(Modifier.fillMaxHeight()) {
        Column() {
            RefreshingFeedUserFeedView(feedViewModel, accountViewModel, nav, enablePullRefresh = false)
        }
    }
}

@Composable
private fun WatchFollowChanges(
    baseUser: User,
    feedViewModel: UserFeedViewModel
) {
    val userState by baseUser.live().follows.observeAsState()

    LaunchedEffect(userState) {
        feedViewModel.invalidateData()
    }
}

@Composable
fun TabReports(baseUser: User, feedViewModel: NostrUserProfileReportFeedViewModel, accountViewModel: AccountViewModel, nav: (String) -> Unit) {
    WatchReportsAndUpdateFeed(baseUser, feedViewModel)

    Column(Modifier.fillMaxHeight()) {
        Column() {
            RefresheableFeedView(
                feedViewModel,
                null,
                enablePullRefresh = false,
                accountViewModel = accountViewModel,
                nav = nav
            )
        }
    }
}

@Composable
private fun WatchReportsAndUpdateFeed(
    baseUser: User,
    feedViewModel: NostrUserProfileReportFeedViewModel
) {
    val userState by baseUser.live().reports.observeAsState()
    LaunchedEffect(userState) {
        feedViewModel.invalidateData()
    }
}

@Composable
fun TabRelays(user: User, accountViewModel: AccountViewModel, nav: (String) -> Unit) {
    val feedViewModel: RelayFeedViewModel = viewModel()

    val lifeCycleOwner = LocalLifecycleOwner.current

    DisposableEffect(user) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                println("Profile Relay Start")
                feedViewModel.subscribeTo(user)
            }
            if (event == Lifecycle.Event.ON_PAUSE) {
                println("Profile Relay Stop")
                feedViewModel.unsubscribeTo(user)
            }
        }

        lifeCycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifeCycleOwner.lifecycle.removeObserver(observer)
            println("Profile Relay Dispose")
            feedViewModel.unsubscribeTo(user)
        }
    }

    Column(Modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier.padding(vertical = 0.dp)
        ) {
            RelayFeedView(feedViewModel, accountViewModel, enablePullRefresh = false, nav = nav)
        }
    }
}

@Composable
private fun MessageButton(user: User, nav: (String) -> Unit) {
    Button(
        modifier = Modifier
            .padding(horizontal = 3.dp)
            .width(50.dp),
        onClick = { nav("Room/${user.pubkeyHex}") },
        shape = ButtonBorder,
        colors = ButtonDefaults
            .buttonColors(
                backgroundColor = MaterialTheme.colors.placeholderText
            )
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_dm),
            stringResource(R.string.send_a_direct_message),
            modifier = Modifier.size(20.dp),
            tint = Color.White
        )
    }
}

@Composable
private fun EditButton(account: Account) {
    var wantsToEdit by remember {
        mutableStateOf(false)
    }

    if (wantsToEdit) {
        NewUserMetadataView({ wantsToEdit = false }, account)
    }

    Button(
        modifier = Modifier
            .padding(horizontal = 3.dp)
            .width(50.dp),
        onClick = { wantsToEdit = true },
        shape = ButtonBorder,
        colors = ButtonDefaults
            .buttonColors(
                backgroundColor = MaterialTheme.colors.primary
            )
    ) {
        Icon(
            tint = Color.White,
            imageVector = Icons.Default.EditNote,
            contentDescription = stringResource(R.string.edits_the_user_s_metadata)
        )
    }
}

@Composable
fun UnfollowButton(onClick: () -> Unit) {
    Button(
        modifier = Modifier.padding(horizontal = 3.dp),
        onClick = onClick,
        shape = ButtonBorder,
        colors = ButtonDefaults
            .buttonColors(
                backgroundColor = MaterialTheme.colors.primary
            ),
        contentPadding = PaddingValues(vertical = 6.dp, horizontal = 16.dp)
    ) {
        Text(text = stringResource(R.string.unfollow), color = Color.White)
    }
}

@Composable
fun FollowButton(text: Int = R.string.follow, onClick: () -> Unit) {
    Button(
        modifier = Modifier.padding(start = 3.dp),
        onClick = onClick,
        shape = ButtonBorder,
        colors = ButtonDefaults
            .buttonColors(
                backgroundColor = MaterialTheme.colors.primary
            ),
        contentPadding = PaddingValues(vertical = 6.dp, horizontal = 16.dp)
    ) {
        Text(text = stringResource(text), color = Color.White, textAlign = TextAlign.Center)
    }
}

@Composable
fun ShowUserButton(onClick: () -> Unit) {
    Button(
        modifier = Modifier.padding(start = 3.dp),
        onClick = onClick,
        shape = ButtonBorder,
        colors = ButtonDefaults
            .buttonColors(
                backgroundColor = MaterialTheme.colors.primary
            ),
        contentPadding = PaddingValues(vertical = 6.dp, horizontal = 16.dp)
    ) {
        Text(text = stringResource(R.string.unblock), color = Color.White)
    }
}

@Composable
fun UserProfileDropDownMenu(user: User, popupExpanded: Boolean, onDismiss: () -> Unit, accountViewModel: AccountViewModel) {
    DropdownMenu(
        expanded = popupExpanded,
        onDismissRequest = onDismiss
    ) {
        val clipboardManager = LocalClipboardManager.current
        val accountState by accountViewModel.accountLiveData.observeAsState()
        val account = accountState?.account!!

        val scope = rememberCoroutineScope()

        DropdownMenuItem(onClick = { clipboardManager.setText(AnnotatedString(user.pubkeyNpub())); onDismiss() }) {
            Text(stringResource(R.string.copy_user_id))
        }

        if (account.userProfile() != user) {
            Divider()
            if (account.isHidden(user)) {
                DropdownMenuItem(onClick = {
                    scope.launch(Dispatchers.IO) {
                        accountViewModel.show(user)
                        onDismiss()
                    }
                }) {
                    Text(stringResource(R.string.unblock_user))
                }
            } else {
                DropdownMenuItem(onClick = {
                    scope.launch(Dispatchers.IO) {
                        accountViewModel.hide(user)
                        onDismiss()
                    }
                }) {
                    Text(stringResource(id = R.string.block_hide_user))
                }
            }
            Divider()
            DropdownMenuItem(onClick = {
                scope.launch(Dispatchers.IO) {
                    accountViewModel.report(user, ReportEvent.ReportType.SPAM)
                    accountViewModel.hide(user)
                }
                onDismiss()
            }) {
                Text(stringResource(id = R.string.report_spam_scam))
            }
            DropdownMenuItem(onClick = {
                scope.launch(Dispatchers.IO) {
                    accountViewModel.report(user, ReportEvent.ReportType.PROFANITY)
                    accountViewModel.hide(user)
                }
                onDismiss()
            }) {
                Text(stringResource(R.string.report_hateful_speech))
            }
            DropdownMenuItem(onClick = {
                scope.launch(Dispatchers.IO) {
                    accountViewModel.report(user, ReportEvent.ReportType.IMPERSONATION)
                    accountViewModel.hide(user)
                }
                onDismiss()
            }) {
                Text(stringResource(id = R.string.report_impersonation))
            }
            DropdownMenuItem(onClick = {
                scope.launch(Dispatchers.IO) {
                    accountViewModel.report(user, ReportEvent.ReportType.NUDITY)
                    accountViewModel.hide(user)
                }
                onDismiss()
            }) {
                Text(stringResource(R.string.report_nudity_porn))
            }
            DropdownMenuItem(onClick = {
                scope.launch(Dispatchers.IO) {
                    accountViewModel.report(user, ReportEvent.ReportType.ILLEGAL)
                    accountViewModel.hide(user)
                }
                onDismiss()
            }) {
                Text(stringResource(id = R.string.report_illegal_behaviour))
            }
        }
    }
}
