package com.vitorpamplona.amethyst.ui.screen.loggedIn

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Divider
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vitorpamplona.amethyst.R
import com.vitorpamplona.amethyst.model.Account
import com.vitorpamplona.amethyst.model.LocalCache
import com.vitorpamplona.amethyst.model.Note
import com.vitorpamplona.amethyst.model.User
import com.vitorpamplona.amethyst.service.NostrSearchEventOrUserDataSource
import com.vitorpamplona.amethyst.service.checkNotInMainThread
import com.vitorpamplona.amethyst.ui.components.BundledUpdate
import com.vitorpamplona.amethyst.ui.note.AboutDisplay
import com.vitorpamplona.amethyst.ui.note.ClearTextIcon
import com.vitorpamplona.amethyst.ui.note.ClickableUserPicture
import com.vitorpamplona.amethyst.ui.note.NoteCompose
import com.vitorpamplona.amethyst.ui.note.SearchIcon
import com.vitorpamplona.amethyst.ui.note.UserCompose
import com.vitorpamplona.amethyst.ui.note.UsernameDisplay
import com.vitorpamplona.amethyst.ui.theme.Size20Modifier
import com.vitorpamplona.amethyst.ui.theme.placeholderText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.regex.Pattern
import kotlinx.coroutines.channels.Channel as CoroutineChannel

@Composable
fun SearchScreen(
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    val searchBarViewModel: SearchBarViewModel = viewModel(
        key = accountViewModel.account.userProfile().pubkeyHex + "SearchBarViewModel",
        factory = SearchBarViewModel.Factory(
            accountViewModel.account
        )
    )

    SearchScreen(searchBarViewModel, accountViewModel, nav)
}

@Composable
fun SearchScreen(
    searchBarViewModel: SearchBarViewModel,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    val lifeCycleOwner = LocalLifecycleOwner.current

    WatchAccountForSearchScreen(accountViewModel)

    DisposableEffect(accountViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                println("Search Start")
                NostrSearchEventOrUserDataSource.start()
            }
            if (event == Lifecycle.Event.ON_PAUSE) {
                println("Search Stop")
                NostrSearchEventOrUserDataSource.clear()
                NostrSearchEventOrUserDataSource.stop()
            }
        }

        lifeCycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifeCycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val listState = rememberLazyListState()

    Column(Modifier.fillMaxSize()) {
        SearchBar(searchBarViewModel, listState)
        DisplaySearchResults(searchBarViewModel, listState, nav, accountViewModel)
    }
}

@Composable
fun WatchAccountForSearchScreen(accountViewModel: AccountViewModel) {
    LaunchedEffect(accountViewModel) {
        launch(Dispatchers.IO) {
            NostrSearchEventOrUserDataSource.start()
        }
    }
}

@Stable
class SearchBarViewModel(val account: Account) : ViewModel() {
    var searchValue by mutableStateOf("")

    private var _searchResultsUsers = MutableStateFlow<List<User>>(emptyList())
    private var _searchResultsNotes = MutableStateFlow<List<Note>>(emptyList())
    private var _hashtagResults = MutableStateFlow<List<String>>(emptyList())

    val searchResultsUsers = _searchResultsUsers.asStateFlow()
    val searchResultsNotes = _searchResultsNotes.asStateFlow()
    val hashtagResults = _hashtagResults.asStateFlow()

    val isSearching by derivedStateOf {
        searchValue.isNotBlank()
    }

    fun updateSearchValue(newValue: String) {
        searchValue = newValue
    }

    private suspend fun runSearch() {
        if (searchValue.isBlank()) {
            _hashtagResults.value = emptyList()
            _searchResultsUsers.value = emptyList()
            _searchResultsNotes.value = emptyList()
            return
        }

        _hashtagResults.emit(findHashtags(searchValue))
        _searchResultsUsers.emit(LocalCache.findUsersStartingWith(searchValue).sortedWith(compareBy({ account.isFollowing(it) }, { it.toBestDisplayName() })).reversed())
        _searchResultsNotes.emit(LocalCache.findNotesStartingWith(searchValue).sortedWith(compareBy({ it.createdAt() }, { it.idHex })).reversed())
    }

    fun clear() {
        searchValue = ""
        _searchResultsUsers.value = emptyList()
        _searchResultsNotes.value = emptyList()
    }

    private val bundler = BundledUpdate(250, Dispatchers.IO)

    fun invalidateData() {
        bundler.invalidate() {
            // adds the time to perform the refresh into this delay
            // holding off new updates in case of heavy refresh routines.
            runSearch()
        }
    }

    fun isSearchingFun() = searchValue.isNotBlank()

    class Factory(val account: Account) : ViewModelProvider.Factory {
        override fun <SearchBarViewModel : ViewModel> create(modelClass: Class<SearchBarViewModel>): SearchBarViewModel {
            return SearchBarViewModel(account) as SearchBarViewModel
        }
    }
}

@OptIn(FlowPreview::class)
@Composable
private fun SearchBar(
    searchBarViewModel: SearchBarViewModel,
    listState: LazyListState
) {
    val scope = rememberCoroutineScope()

    // Create a channel for processing search queries.
    val searchTextChanges = remember {
        CoroutineChannel<String>(CoroutineChannel.CONFLATED)
    }

    LaunchedEffect(Unit) {
        launch(Dispatchers.IO) {
            LocalCache.live.newEventBundles.collect {
                checkNotInMainThread()

                if (searchBarViewModel.isSearchingFun()) {
                    searchBarViewModel.invalidateData()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        // Wait for text changes to stop for 300 ms before firing off search.
        withContext(Dispatchers.IO) {
            searchTextChanges.receiveAsFlow()
                .filter { it.isNotBlank() }
                .distinctUntilChanged()
                .debounce(300)
                .collectLatest {
                    if (it.length >= 2) {
                        NostrSearchEventOrUserDataSource.search(it.trim())
                    }

                    searchBarViewModel.invalidateData()

                    // makes sure to show the top of the search
                    launch(Dispatchers.Main) { listState.animateScrollToItem(0) }
                }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            NostrSearchEventOrUserDataSource.clear()
        }
    }

    // LAST ROW
    SearchTextField(searchBarViewModel) {
        scope.launch(Dispatchers.IO) {
            searchTextChanges.trySend(it)
        }
    }
}

@Composable
private fun SearchTextField(
    searchBarViewModel: SearchBarViewModel,
    onTextChanges: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .padding(10.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = searchBarViewModel.searchValue,
            onValueChange = {
                searchBarViewModel.updateSearchValue(it)
                onTextChanges(it)
            },
            shape = RoundedCornerShape(25.dp),
            keyboardOptions = KeyboardOptions.Default.copy(
                capitalization = KeyboardCapitalization.Sentences
            ),
            leadingIcon = {
                SearchIcon(modifier = Size20Modifier, Color.Unspecified)
            },
            modifier = Modifier
                .weight(1f, true)
                .defaultMinSize(minHeight = 20.dp),
            placeholder = {
                Text(
                    text = stringResource(R.string.npub_hex_username),
                    color = MaterialTheme.colors.placeholderText
                )
            },
            trailingIcon = {
                if (searchBarViewModel.isSearching) {
                    IconButton(
                        onClick = {
                            searchBarViewModel.clear()
                            NostrSearchEventOrUserDataSource.clear()
                        }
                    ) {
                        ClearTextIcon()
                    }
                }
            },
            singleLine = true,
            colors = TextFieldDefaults.textFieldColors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun DisplaySearchResults(
    searchBarViewModel: SearchBarViewModel,
    listState: LazyListState,
    nav: (String) -> Unit,
    accountViewModel: AccountViewModel
) {
    if (!searchBarViewModel.isSearching) {
        return
    }

    val hashTags by searchBarViewModel.hashtagResults.collectAsState()
    val users by searchBarViewModel.searchResultsUsers.collectAsState()
    val notes by searchBarViewModel.searchResultsNotes.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxHeight(),
        contentPadding = PaddingValues(
            top = 10.dp,
            bottom = 10.dp
        ),
        state = listState
    ) {
        itemsIndexed(
            hashTags,
            key = { _, item -> "#$item" }
        ) { _, item ->
            HashtagLine(item) {
                nav("Hashtag/$item")
            }
        }

        itemsIndexed(
            users,
            key = { _, item -> "u" + item.pubkeyHex }
        ) { _, item ->
            UserCompose(item, accountViewModel = accountViewModel, nav = nav)
        }

        itemsIndexed(
            notes,
            key = { _, item -> "n" + item.idHex }
        ) { _, item ->
            NoteCompose(
                item,
                accountViewModel = accountViewModel,
                nav = nav
            )
        }
    }
}

val hashtagSearch = Pattern.compile("(?:\\s|\\A)#([^\\s!@#\$%^&*()=+./,\\[{\\]};:'\"?><]+)")

fun findHashtags(content: String): List<String> {
    val matcher = hashtagSearch.matcher(content)
    val returningList = mutableSetOf<String>()
    while (matcher.find()) {
        try {
            val tag = matcher.group(1)
            if (tag != null && tag.isNotBlank()) {
                returningList.add(tag)
            }
        } catch (e: Exception) {
        }
    }
    return returningList.toList()
}

@Composable
fun HashtagLine(tag: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(
                    start = 12.dp,
                    end = 12.dp,
                    top = 10.dp
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Search hashtag: #$tag",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Divider(
            modifier = Modifier.padding(top = 10.dp),
            thickness = 0.25.dp
        )
    }
}

@Composable
fun UserLine(
    baseUser: User,
    accountViewModel: AccountViewModel,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(
                    start = 12.dp,
                    end = 12.dp,
                    top = 10.dp
                )
        ) {
            ClickableUserPicture(baseUser, 55.dp, accountViewModel, Modifier, null)

            Column(
                modifier = Modifier
                    .padding(start = 10.dp)
                    .weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UsernameDisplay(baseUser)
                }

                AboutDisplay(baseUser)
            }
        }

        Divider(
            modifier = Modifier.padding(top = 10.dp),
            thickness = 0.25.dp
        )
    }
}
