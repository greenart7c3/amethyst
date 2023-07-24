package com.vitorpamplona.amethyst.ui.navigation

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.vitorpamplona.amethyst.BuildConfig
import com.vitorpamplona.amethyst.LocalPreferences
import com.vitorpamplona.amethyst.R
import com.vitorpamplona.amethyst.ServiceManager
import com.vitorpamplona.amethyst.model.Account
import com.vitorpamplona.amethyst.model.User
import com.vitorpamplona.amethyst.service.HttpClient
import com.vitorpamplona.amethyst.ui.actions.NewRelayListView
import com.vitorpamplona.amethyst.ui.actions.toImmutableListOfLists
import com.vitorpamplona.amethyst.ui.components.CreateTextWithEmoji
import com.vitorpamplona.amethyst.ui.components.RobohashAsyncImageProxy
import com.vitorpamplona.amethyst.ui.screen.RelayPoolViewModel
import com.vitorpamplona.amethyst.ui.screen.loggedIn.AccountBackupDialog
import com.vitorpamplona.amethyst.ui.screen.loggedIn.AccountViewModel
import com.vitorpamplona.amethyst.ui.screen.loggedIn.ConnectOrbotDialog
import com.vitorpamplona.amethyst.ui.theme.Size16dp
import com.vitorpamplona.amethyst.ui.theme.placeholderText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DrawerContent(
    nav: (String) -> Unit,
    scaffoldState: ScaffoldState,
    sheetState: ModalBottomSheetState,
    accountViewModel: AccountViewModel
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colors.background
    ) {
        Column() {
            ProfileContent(
                accountViewModel.account.userProfile(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 25.dp)
                    .padding(top = 100.dp),
                scaffoldState,
                nav
            )
            Divider(
                thickness = 0.25.dp,
                modifier = Modifier.padding(top = 20.dp)
            )
            ListContent(
                nav,
                scaffoldState,
                sheetState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                accountViewModel
            )

            BottomContent(accountViewModel.account.userProfile(), scaffoldState, nav)
        }
    }
}

@Composable
fun ProfileContent(
    baseAccountUser: User,
    modifier: Modifier = Modifier,
    scaffoldState: ScaffoldState,
    nav: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    val accountUserState by baseAccountUser.live().metadata.observeAsState()

    val profilePubHex = remember(accountUserState) { accountUserState?.user?.pubkeyHex } ?: return

    val profileBanner = remember(accountUserState) { accountUserState?.user?.info?.banner?.ifBlank { null } }
    val profilePicture = remember(accountUserState) { accountUserState?.user?.profilePicture() }
    val bestUserName = remember(accountUserState) { accountUserState?.user?.bestUsername() }
    val bestDisplayName = remember(accountUserState) { accountUserState?.user?.bestDisplayName() }
    val tags = remember(accountUserState) { accountUserState?.user?.info?.latestMetadata?.tags?.toImmutableListOfLists() }
    val route = remember(accountUserState) { "User/${accountUserState?.user?.pubkeyHex}" }

    Box {
        if (profileBanner != null) {
            AsyncImage(
                model = profileBanner,
                contentDescription = stringResource(id = R.string.profile_image),
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            )
        } else {
            Image(
                painter = painterResource(R.drawable.profile_banner),
                contentDescription = stringResource(R.string.profile_banner),
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            )
        }

        Column(modifier = modifier) {
            RobohashAsyncImageProxy(
                robot = profilePubHex,
                model = profilePicture,
                contentDescription = stringResource(id = R.string.profile_image),
                modifier = Modifier
                    .width(100.dp)
                    .height(100.dp)
                    .clip(shape = CircleShape)
                    .border(3.dp, MaterialTheme.colors.background, CircleShape)
                    .background(MaterialTheme.colors.background)
                    .clickable(onClick = {
                        nav(route)
                        coroutineScope.launch {
                            scaffoldState.drawerState.close()
                        }
                    })
            )

            if (bestDisplayName != null) {
                CreateTextWithEmoji(
                    text = bestDisplayName,
                    tags = tags,
                    modifier = Modifier
                        .padding(top = 7.dp)
                        .clickable(onClick = {
                            nav(route)
                            coroutineScope.launch {
                                scaffoldState.drawerState.close()
                            }
                        }),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (bestUserName != null) {
                CreateTextWithEmoji(
                    text = remember { " @$bestUserName" },
                    tags = tags,
                    color = Color.LightGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(top = 15.dp)
                        .clickable(
                            onClick = {
                                nav(route)
                                coroutineScope.launch {
                                    scaffoldState.drawerState.close()
                                }
                            }
                        )
                )
            }
            Row(
                modifier = Modifier
                    .padding(top = 15.dp)
                    .clickable(onClick = {
                        nav(route)
                        coroutineScope.launch {
                            scaffoldState.drawerState.close()
                        }
                    })
            ) {
                FollowingAndFollowerCounts(baseAccountUser)
            }
        }
    }
}

@Composable
private fun FollowingAndFollowerCounts(baseAccountUser: User) {
    var followingCount by remember { mutableStateOf("--") }

    WatchFollow(baseAccountUser = baseAccountUser) { newFollowing ->
        if (followingCount != newFollowing) {
            followingCount = newFollowing
        }
    }

    Text(
        text = followingCount,
        fontWeight = FontWeight.Bold
    )

    Text(stringResource(R.string.following))
}

@Composable
fun WatchFollow(baseAccountUser: User, onReady: (String) -> Unit) {
    val accountUserFollowsState by baseAccountUser.live().follows.observeAsState()

    LaunchedEffect(key1 = accountUserFollowsState) {
        launch(Dispatchers.IO) {
            onReady(accountUserFollowsState?.user?.cachedFollowCount()?.toString() ?: "--")
        }
    }
}

@Composable
fun WatchFollower(baseAccountUser: User, onReady: (String) -> Unit) {
    val accountUserFollowersState by baseAccountUser.live().followers.observeAsState()

    LaunchedEffect(key1 = accountUserFollowersState) {
        launch(Dispatchers.IO) {
            onReady(accountUserFollowersState?.user?.cachedFollowerCount()?.toString() ?: "--")
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ListContent(
    nav: (String) -> Unit,
    scaffoldState: ScaffoldState,
    sheetState: ModalBottomSheetState,
    modifier: Modifier,
    accountViewModel: AccountViewModel
) {
    val route = remember(accountViewModel) {
        "User/${accountViewModel.userProfile().pubkeyHex}"
    }

    val coroutineScope = rememberCoroutineScope()
    val relayViewModel: RelayPoolViewModel = viewModel { RelayPoolViewModel() }
    var wantsToEditRelays by remember {
        mutableStateOf(false)
    }

    var backupDialogOpen by remember { mutableStateOf(false) }
    var checked by remember { mutableStateOf(accountViewModel.account.proxy != null) }
    var disconnectTorDialog by remember { mutableStateOf(false) }
    var conectOrbotDialogOpen by remember { mutableStateOf(false) }
    val proxyPort = remember { mutableStateOf(accountViewModel.account.proxyPort.toString()) }
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
    ) {
        NavigationRow(
            title = stringResource(R.string.profile),
            icon = Route.Profile.icon,
            tint = MaterialTheme.colors.primary,
            nav = nav,
            scaffoldState = scaffoldState,
            route = route
        )

        NavigationRow(
            title = stringResource(R.string.bookmarks),
            icon = Route.Bookmarks.icon,
            tint = MaterialTheme.colors.onBackground,
            nav = nav,
            scaffoldState = scaffoldState,
            route = Route.Bookmarks.route
        )

        IconRowRelays(
            relayViewModel = relayViewModel,
            onClick = {
                coroutineScope.launch {
                    scaffoldState.drawerState.close()
                }
                wantsToEditRelays = true
            }
        )

        NavigationRow(
            title = stringResource(R.string.security_filters),
            icon = Route.BlockedUsers.icon,
            tint = MaterialTheme.colors.onBackground,
            nav = nav,
            scaffoldState = scaffoldState,
            route = Route.BlockedUsers.route
        )

        IconRow(
            title = stringResource(R.string.backup_keys),
            icon = R.drawable.ic_key,
            tint = MaterialTheme.colors.onBackground,
            onClick = {
                coroutineScope.launch {
                    scaffoldState.drawerState.close()
                }
                backupDialogOpen = true
            }
        )

        val textTorProxy = if (checked) stringResource(R.string.disconnect_from_your_orbot_setup) else stringResource(R.string.connect_via_tor_short)
        IconRow(
            title = textTorProxy,
            icon = R.drawable.ic_tor,
            tint = MaterialTheme.colors.onBackground,
            onLongClick = {
                coroutineScope.launch {
                    scaffoldState.drawerState.close()
                }
                conectOrbotDialogOpen = true
            },
            onClick = {
                if (checked) {
                    disconnectTorDialog = true
                } else {
                    coroutineScope.launch {
                        scaffoldState.drawerState.close()
                    }
                    conectOrbotDialogOpen = true
                }
            }
        )

        NavigationRow(
            title = stringResource(R.string.settings),
            icon = Route.Settings.icon,
            tint = MaterialTheme.colors.onBackground,
            nav = nav,
            scaffoldState = scaffoldState,
            route = Route.Settings.route
        )

        Spacer(modifier = Modifier.weight(1f))

        IconRow(
            title = stringResource(R.string.drawer_accounts),
            icon = R.drawable.manage_accounts,
            tint = MaterialTheme.colors.onBackground,
            onClick = { coroutineScope.launch { sheetState.show() } }
        )
    }

    if (wantsToEditRelays) {
        NewRelayListView({ wantsToEditRelays = false }, accountViewModel, nav = nav)
    }
    if (backupDialogOpen) {
        AccountBackupDialog(accountViewModel.account, onClose = { backupDialogOpen = false })
    }
    if (conectOrbotDialogOpen) {
        ConnectOrbotDialog(
            onClose = { conectOrbotDialogOpen = false },
            onPost = {
                conectOrbotDialogOpen = false
                disconnectTorDialog = false
                checked = true
                enableTor(accountViewModel.account, true, proxyPort, context, coroutineScope)
            },
            proxyPort
        )
    }

    if (disconnectTorDialog) {
        AlertDialog(
            title = {
                Text(text = stringResource(R.string.do_you_really_want_to_disable_tor_title))
            },
            text = {
                Text(text = stringResource(R.string.do_you_really_want_to_disable_tor_text))
            },
            onDismissRequest = {
                disconnectTorDialog = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        disconnectTorDialog = false
                        checked = false
                        enableTor(accountViewModel.account, false, proxyPort, context, coroutineScope)
                    }
                ) {
                    Text(text = stringResource(R.string.yes))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        disconnectTorDialog = false
                    }
                ) {
                    Text(text = stringResource(R.string.no))
                }
            }
        )
    }
}

private fun enableTor(
    account: Account,
    checked: Boolean,
    portNumber: MutableState<String>,
    context: Context,
    scope: CoroutineScope
) {
    account.proxyPort = portNumber.value.toInt()
    account.proxy = HttpClient.initProxy(checked, "127.0.0.1", account.proxyPort)
    scope.launch(Dispatchers.IO) {
        LocalPreferences.saveToEncryptedStorage(account)
        ServiceManager.pause()
        ServiceManager.start(context)
    }
}

@Composable
private fun RelayStatus(
    relayViewModel: RelayPoolViewModel
) {
    val connectedRelaysText by relayViewModel.connectionStatus.observeAsState("--/--")
    val isConnected by relayViewModel.isConnected.distinctUntilChanged().observeAsState(false)

    RenderRelayStatus(connectedRelaysText, isConnected)
}

@Composable
private fun RenderRelayStatus(
    connectedRelaysText: String,
    isConnected: Boolean
) {
    Text(
        text = connectedRelaysText,
        color = if (isConnected) {
            MaterialTheme.colors.placeholderText
        } else {
            Color.Red
        },
        style = MaterialTheme.typography.subtitle1
    )
}

@Composable
fun NavigationRow(
    title: String,
    icon: Int,
    tint: Color,
    nav: (String) -> Unit,
    scaffoldState: ScaffoldState,
    route: String
) {
    val coroutineScope = rememberCoroutineScope()
    IconRow(title, icon, tint, onClick = {
        nav(route)
        coroutineScope.launch {
            scaffoldState.drawerState.close()
        }
    })
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IconRow(title: String, icon: Int, tint: Color, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 15.dp, horizontal = 25.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(icon),
                null,
                modifier = Modifier.size(22.dp),
                tint = tint
            )
            Text(
                modifier = Modifier.padding(start = 16.dp),
                text = title,
                fontSize = 18.sp
            )
        }
    }
}

@Composable
fun IconRowRelays(relayViewModel: RelayPoolViewModel, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 15.dp, horizontal = 25.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.relays),
                null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colors.onSurface
            )

            Text(
                modifier = Modifier.padding(start = 16.dp),
                text = stringResource(id = R.string.relay_setup),
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.width(Size16dp))

            RelayStatus(relayViewModel = relayViewModel)
        }
    }
}

@Composable
fun BottomContent(user: User, scaffoldState: ScaffoldState, nav: (String) -> Unit) {
    val coroutineScope = rememberCoroutineScope()

    // store the dialog open or close state
    var dialogOpen by remember {
        mutableStateOf(false)
    }

    Column(modifier = Modifier) {
        Divider(
            modifier = Modifier.padding(top = 15.dp),
            thickness = 0.25.dp
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.padding(start = 16.dp),
                text = "v" + BuildConfig.VERSION_NAME + "-" + BuildConfig.FLAVOR.uppercase(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            /*
            IconButton(
                onClick = {
                    when (AppCompatDelegate.getDefaultNightMode()) {
                        AppCompatDelegate.MODE_NIGHT_NO -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                        AppCompatDelegate.MODE_NIGHT_YES -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                        else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    }
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_theme),
                    null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colors.primary
                )
            }*/
            Box(modifier = Modifier.weight(1F))
            IconButton(onClick = {
                dialogOpen = true
                coroutineScope.launch {
                    scaffoldState.drawerState.close()
                }
            }) {
                Icon(
                    painter = painterResource(R.drawable.ic_qrcode),
                    null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colors.primary
                )
            }
        }
    }

    if (dialogOpen) {
        ShowQRDialog(
            user,
            onScan = {
                dialogOpen = false
                coroutineScope.launch {
                    scaffoldState.drawerState.close()
                }
                nav(it)
            },
            onClose = { dialogOpen = false }
        )
    }
}
