package com.vitorpamplona.amethyst.service

import com.vitorpamplona.amethyst.model.Account
import com.vitorpamplona.amethyst.service.model.PrivateDmEvent
import com.vitorpamplona.amethyst.service.relays.EOSEAccount
import com.vitorpamplona.amethyst.service.relays.FeedType
import com.vitorpamplona.amethyst.service.relays.JsonFilter
import com.vitorpamplona.amethyst.service.relays.TypedFilter

object NostrChatroomListDataSource : NostrDataSource("MailBoxFeed") {
    lateinit var account: Account

    val latestEOSEs = EOSEAccount()
    val chatRoomList = "ChatroomList"

    fun createMessagesToMeFilter() = TypedFilter(
        types = setOf(FeedType.PRIVATE_DMS),
        filter = JsonFilter(
            kinds = listOf(PrivateDmEvent.kind),
            tags = mapOf("p" to listOf(account.userProfile().pubkeyHex)),
            since = latestEOSEs.users[account.userProfile()]?.followList?.get(chatRoomList)?.relayList
        )
    )

    fun createMessagesFromMeFilter() = TypedFilter(
        types = setOf(FeedType.PRIVATE_DMS),
        filter = JsonFilter(
            kinds = listOf(PrivateDmEvent.kind),
            authors = listOf(account.userProfile().pubkeyHex),
            since = latestEOSEs.users[account.userProfile()]?.followList?.get(chatRoomList)?.relayList
        )
    )

    val chatroomListChannel = requestNewChannel { time, relayUrl ->
        latestEOSEs.addOrUpdate(account.userProfile(), chatRoomList, relayUrl, time)
    }

    override fun updateChannelFilters() {
        val list = listOf(
            createMessagesToMeFilter(),
            createMessagesFromMeFilter()
        )

        chatroomListChannel.typedFilters = listOfNotNull(
            list
        ).flatten().ifEmpty { null }
    }
}
