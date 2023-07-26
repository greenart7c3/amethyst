package com.vitorpamplona.amethyst.ui.navigation

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.vitorpamplona.amethyst.R
import com.vitorpamplona.amethyst.model.Account
import com.vitorpamplona.amethyst.model.Note
import com.vitorpamplona.amethyst.service.checkNotInMainThread
import com.vitorpamplona.amethyst.service.model.PrivateDmEvent
import com.vitorpamplona.amethyst.ui.dal.AdditiveFeedFilter
import com.vitorpamplona.amethyst.ui.dal.ChatroomListKnownFeedFilter
import com.vitorpamplona.amethyst.ui.dal.HomeNewThreadFeedFilter
import com.vitorpamplona.amethyst.ui.dal.NotificationFeedFilter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Immutable
sealed class Route(
    val route: String,
    val icon: Int,
    val hasNewItems: (Account, Set<com.vitorpamplona.amethyst.model.Note>) -> Boolean = { _, _ -> false },
    val arguments: ImmutableList<NamedNavArgument> = persistentListOf()
) {
    val base: String
        get() = route.substringBefore("?")

    object Home : Route(
        route = "Home?nip47={nip47}",
        icon = R.drawable.ic_home,
        arguments = listOf(
            navArgument("nip47") { type = NavType.StringType; nullable = true; defaultValue = null }
        ).toImmutableList(),
        hasNewItems = { accountViewModel, newNotes -> HomeLatestItem.hasNewItems(accountViewModel, newNotes) }
    )

    object Global : Route(
        route = "Global",
        icon = R.drawable.ic_globe
    )

    object Search : Route(
        route = "Search",
        icon = R.drawable.ic_search
    )

    object Discover : Route(
        route = "Discover",
        icon = R.drawable.ic_sensors,
        hasNewItems = { accountViewModel, newNotes -> false }
    )

    object Notification : Route(
        route = "Notification",
        icon = R.drawable.ic_notifications,
        hasNewItems = { accountViewModel, newNotes -> NotificationLatestItem.hasNewItems(accountViewModel, newNotes) }
    )

    object Message : Route(
        route = "Message",
        icon = R.drawable.ic_dm,
        hasNewItems = { accountViewModel, newNotes -> MessagesLatestItem.hasNewItems(accountViewModel, newNotes) }
    )

    object BlockedUsers : Route(
        route = "BlockedUsers",
        icon = R.drawable.ic_security
    )

    object Bookmarks : Route(
        route = "Bookmarks",
        icon = R.drawable.ic_bookmarks
    )

    object Profile : Route(
        route = "User/{id}",
        icon = R.drawable.ic_profile,
        arguments = listOf(navArgument("id") { type = NavType.StringType }).toImmutableList()
    )

    object Note : Route(
        route = "Note/{id}",
        icon = R.drawable.ic_moments,
        arguments = listOf(navArgument("id") { type = NavType.StringType }).toImmutableList()
    )

    object Hashtag : Route(
        route = "Hashtag/{id}",
        icon = R.drawable.ic_moments,
        arguments = listOf(navArgument("id") { type = NavType.StringType }).toImmutableList()
    )

    object Geohash : Route(
        route = "Geohash/{id}",
        icon = R.drawable.ic_moments,
        arguments = listOf(navArgument("id") { type = NavType.StringType }).toImmutableList()
    )

    object Community : Route(
        route = "Community/{id}",
        icon = R.drawable.ic_moments,
        arguments = listOf(navArgument("id") { type = NavType.StringType }).toImmutableList()
    )

    object Room : Route(
        route = "Room/{id}",
        icon = R.drawable.ic_moments,
        arguments = listOf(navArgument("id") { type = NavType.StringType }).toImmutableList()
    )

    object Channel : Route(
        route = "Channel/{id}",
        icon = R.drawable.ic_moments,
        arguments = listOf(navArgument("id") { type = NavType.StringType }).toImmutableList()
    )

    object Event : Route(
        route = "Event/{id}",
        icon = R.drawable.ic_moments,
        arguments = listOf(navArgument("id") { type = NavType.StringType }).toImmutableList()
    )

    object Settings : Route(
        route = "Settings",
        icon = R.drawable.ic_settings
    )
}

// **
// *  Functions below only exist because we have not broken the datasource classes into backend and frontend.
// **
@Composable
fun currentRoute(navController: NavHostController): String? {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    return navBackStackEntry?.destination?.route
}

open class LatestItem {
    var newestItemPerAccount: Map<String, Note?> = mapOf()

    fun getNewestItem(account: Account): Note? {
        return newestItemPerAccount[account.userProfile().pubkeyHex]
    }

    fun clearNewestItem(account: Account) {
        val userHex = account.userProfile().pubkeyHex
        if (newestItemPerAccount.contains(userHex)) {
            newestItemPerAccount = newestItemPerAccount - userHex
        }
    }

    fun updateNewestItem(newNotes: Set<Note>, account: Account, filter: AdditiveFeedFilter<Note>): Note? {
        val newestItem = newestItemPerAccount[account.userProfile().pubkeyHex]

        if (newestItem == null) {
            newestItemPerAccount = newestItemPerAccount + Pair(
                account.userProfile().pubkeyHex,
                filterMore(filter.feed(), account).firstOrNull { it.createdAt() != null }
            )
        } else {
            newestItemPerAccount = newestItemPerAccount + Pair(
                account.userProfile().pubkeyHex,
                filter.sort(filterMore(filter.applyFilter(newNotes), account) + newestItem).first()
            )
        }

        return newestItemPerAccount[account.userProfile().pubkeyHex]
    }

    open fun filterMore(newItems: Set<Note>, account: Account): Set<Note> {
        return newItems
    }

    open fun filterMore(newItems: List<Note>, account: Account): List<Note> {
        return newItems
    }
}

object HomeLatestItem : LatestItem() {
    fun hasNewItems(
        account: Account,
        newNotes: Set<Note>
    ): Boolean {
        checkNotInMainThread()

        val lastTime = account.loadLastRead("HomeFollows")

        val newestItem = updateNewestItem(newNotes, account, HomeNewThreadFeedFilter(account))

        return (newestItem?.createdAt() ?: 0) > lastTime
    }
}

object NotificationLatestItem : LatestItem() {
    fun hasNewItems(
        account: Account,
        newNotes: Set<Note>
    ): Boolean {
        checkNotInMainThread()

        val lastTime = account.loadLastRead("Notification")

        val newestItem = updateNewestItem(newNotes, account, NotificationFeedFilter(account))

        return (newestItem?.createdAt() ?: 0) > lastTime
    }
}

object MessagesLatestItem : LatestItem() {
    fun hasNewItems(
        account: Account,
        newNotes: Set<Note>
    ): Boolean {
        checkNotInMainThread()

        // Checks if the current newest item is still unread.
        // If so, there is no need to check anything else
        if (isNew(getNewestItem(account), account)) {
            return true
        }

        clearNewestItem(account)

        // gets the newest of the unread items
        val newestItem = updateNewestItem(newNotes, account, ChatroomListKnownFeedFilter(account))

        return isNew(newestItem, account)
    }

    fun isNew(it: Note?, account: Account): Boolean {
        if (it == null) return false

        val currentUser = account.userProfile().pubkeyHex
        val room = (it.event as? PrivateDmEvent)?.talkingWith(currentUser)
        return if (room != null) {
            val lastRead = account.loadLastRead("Room/$room")
            (it.createdAt() ?: 0) > lastRead
        } else {
            false
        }
    }

    override fun filterMore(newItems: Set<Note>, account: Account): Set<Note> {
        return newItems.filter {
            isNew(it, account)
        }.toSet()
    }

    override fun filterMore(newItems: List<Note>, account: Account): List<Note> {
        return newItems.filter {
            isNew(it, account)
        }
    }
}

fun getRouteWithArguments(navController: NavHostController): String? {
    val currentEntry = navController.currentBackStackEntry ?: return null
    return getRouteWithArguments(currentEntry.destination, currentEntry.arguments)
}

fun getRouteWithArguments(navState: State<NavBackStackEntry?>): String? {
    return navState.value?.let {
        getRouteWithArguments(it.destination, it.arguments)
    }
}

private fun getRouteWithArguments(
    destination: NavDestination,
    arguments: Bundle?
): String? {
    var route = destination.route ?: return null
    arguments?.let { bundle ->
        destination.arguments.keys.forEach { key ->
            val value = destination.arguments[key]?.type?.get(bundle, key)?.toString()
            if (value == null) {
                val keyStart = route.indexOf("{$key}")
                // if it is a parameter, removes the complete segment `var={key}` and adjust connectors `#`, `&` or `&`
                if (keyStart > 0 && route[keyStart - 1] == '=') {
                    val end = keyStart + "{$key}".length
                    var start = keyStart
                    for (i in keyStart downTo 0) {
                        if (route[i] == '#' || route[i] == '?' || route[i] == '&') {
                            start = i + 1
                            break
                        }
                    }
                    if (end < route.length && route[end] == '&') {
                        route = route.removeRange(start, end + 1)
                    } else if (end < route.length && route[end] == '#') {
                        route = route.removeRange(start - 1, end)
                    } else if (end == route.length) {
                        route = route.removeRange(start - 1, end)
                    } else {
                        route = route.removeRange(start, end)
                    }
                } else {
                    route = route.replaceFirst("{$key}", "")
                }
            } else {
                route = route.replaceFirst("{$key}", value)
            }
        }
    }
    return route
}
