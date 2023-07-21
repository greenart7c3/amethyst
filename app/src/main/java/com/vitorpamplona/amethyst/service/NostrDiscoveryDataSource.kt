package com.vitorpamplona.amethyst.service

import com.vitorpamplona.amethyst.model.Account
import com.vitorpamplona.amethyst.service.model.ChannelCreateEvent
import com.vitorpamplona.amethyst.service.model.ChannelMessageEvent
import com.vitorpamplona.amethyst.service.model.ChannelMetadataEvent
import com.vitorpamplona.amethyst.service.relays.EOSEAccount
import com.vitorpamplona.amethyst.service.relays.FeedType
import com.vitorpamplona.amethyst.service.relays.JsonFilter
import com.vitorpamplona.amethyst.service.relays.TypedFilter

object NostrDiscoveryDataSource : NostrDataSource("DiscoveryFeed") {
    lateinit var account: Account

    val latestEOSEs = EOSEAccount()

    fun createPublicChatFilter(): TypedFilter {
        val follows = account.selectedUsersFollowList(account.defaultDiscoveryFollowList)

        val followKeys = follows?.map {
            it.substring(0, 8)
        }

        return TypedFilter(
            types = setOf(FeedType.PUBLIC_CHATS),
            filter = JsonFilter(
                authors = followKeys,
                kinds = listOf(ChannelCreateEvent.kind, ChannelMetadataEvent.kind, ChannelMessageEvent.kind),
                limit = 300,
                since = latestEOSEs.users[account.userProfile()]?.followList?.get(account.defaultDiscoveryFollowList)?.relayList
            )
        )
    }

    fun createPublicChatsTagsFilter(): TypedFilter? {
        val hashToLoad = account.selectedTagsFollowList(account.defaultDiscoveryFollowList)

        if (hashToLoad.isNullOrEmpty()) return null

        return TypedFilter(
            types = setOf(FeedType.PUBLIC_CHATS),
            filter = JsonFilter(
                kinds = listOf(ChannelCreateEvent.kind, ChannelMetadataEvent.kind, ChannelMessageEvent.kind),
                tags = mapOf(
                    "t" to hashToLoad.map {
                        listOf(it, it.lowercase(), it.uppercase(), it.capitalize())
                    }.flatten()
                ),
                limit = 300,
                since = latestEOSEs.users[account.userProfile()]?.followList?.get(account.defaultDiscoveryFollowList)?.relayList
            )
        )
    }

    val discoveryFeedChannel = requestNewChannel() { time, relayUrl ->
        latestEOSEs.addOrUpdate(account.userProfile(), account.defaultDiscoveryFollowList, relayUrl, time)
    }

    override fun updateChannelFilters() {
        discoveryFeedChannel.typedFilters = listOfNotNull(
            createPublicChatFilter(),
            createPublicChatsTagsFilter()
        ).ifEmpty { null }
    }
}
