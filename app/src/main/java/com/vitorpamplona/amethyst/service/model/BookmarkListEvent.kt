package com.vitorpamplona.amethyst.service.model

import androidx.compose.runtime.Immutable
import com.vitorpamplona.amethyst.model.HexKey
import com.vitorpamplona.amethyst.model.toHexKey
import com.vitorpamplona.amethyst.service.Nip26
import nostr.postr.Utils
import java.util.Date

@Immutable
class BookmarkListEvent(
    id: HexKey,
    pubKey: HexKey,
    createdAt: Long,
    tags: List<List<String>>,
    content: String,
    sig: HexKey
) : GeneralListEvent(id, pubKey, createdAt, kind, tags, content, sig) {
    companion object {
        const val kind = 30001

        fun create(
            name: String = "",
            events: List<String>? = null,
            users: List<String>? = null,
            addresses: List<ATag>? = null,
            privEvents: List<String>? = null,
            privUsers: List<String>? = null,
            privAddresses: List<ATag>? = null,
            privateKey: ByteArray,
            createdAt: Long = Date().time / 1000,
            delegationToken: String,
            delegationHexKey: String,
            delegationSignature: String
        ): BookmarkListEvent {
            val pubKey = Utils.pubkeyCreate(privateKey)
            val content = createPrivateTags(privEvents, privUsers, privAddresses, privateKey, pubKey)

            val tags = mutableListOf<List<String>>()
            tags.add(listOf("d", name))

            events?.forEach {
                tags.add(listOf("e", it))
            }
            users?.forEach {
                tags.add(listOf("p", it))
            }
            addresses?.forEach {
                tags.add(listOf("a", it.toTag()))
            }
            if (delegationToken.isNotBlank()) {
                tags.add(Nip26.toTags(delegationToken, delegationSignature, delegationHexKey))
            }

            val id = generateId(pubKey.toHexKey(), createdAt, kind, tags, content)
            val sig = Utils.sign(id, privateKey)
            return BookmarkListEvent(id.toHexKey(), pubKey.toHexKey(), createdAt, tags, content, sig.toHexKey())
        }
    }
}
