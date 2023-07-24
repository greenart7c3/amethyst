package com.vitorpamplona.amethyst

import android.content.Context
import android.os.Build
import android.util.Log
import coil.Coil
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import com.vitorpamplona.amethyst.model.Account
import com.vitorpamplona.amethyst.model.LocalCache
import com.vitorpamplona.amethyst.service.HttpClient
import com.vitorpamplona.amethyst.service.NostrAccountDataSource
import com.vitorpamplona.amethyst.service.NostrChatroomDataSource
import com.vitorpamplona.amethyst.service.NostrChatroomListDataSource
import com.vitorpamplona.amethyst.service.NostrHashtagDataSource
import com.vitorpamplona.amethyst.service.NostrHomeDataSource
import com.vitorpamplona.amethyst.service.NostrSearchEventOrUserDataSource
import com.vitorpamplona.amethyst.service.NostrSingleEventDataSource
import com.vitorpamplona.amethyst.service.NostrSingleUserDataSource
import com.vitorpamplona.amethyst.service.NostrThreadDataSource
import com.vitorpamplona.amethyst.service.NostrUserProfileDataSource
import com.vitorpamplona.amethyst.service.relays.Client
import com.vitorpamplona.amethyst.ui.actions.ImageUploader

object ServiceManager {
    private var account: Account? = null

    fun start(account: Account, context: Context) {
        this.account = account
        start(context)
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    @Synchronized
    fun start(context: Context) {
        Log.d("ServiceManager", "Starting Relay Services")

        val myAccount = account

        // Resets Proxy Use
        HttpClient.start(account)
        OptOutFromFilters.start(account?.warnAboutPostsWithReports ?: true, account?.filterSpamFromStrangers ?: true)
        Coil.setImageLoader {
            ImageLoader.Builder(context).components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                add(SvgDecoder.Factory())
            } // .logger(DebugLogger())
                .okHttpClient { HttpClient.getHttpClient() }
                .respectCacheHeaders(false)
                .build()
        }

        if (myAccount != null) {
            Client.connect(myAccount.activeRelays() ?: myAccount.convertLocalRelays())

            // start services
            NostrAccountDataSource.account = myAccount
            NostrHomeDataSource.account = myAccount
            NostrChatroomListDataSource.account = myAccount
            ImageUploader.account = myAccount

            // Notification Elements
            NostrHomeDataSource.start()
            NostrAccountDataSource.start()
            NostrChatroomListDataSource.start()

            // More Info Data Sources
            NostrSingleEventDataSource.start()
            NostrSingleUserDataSource.start()
        }
    }

    fun pause() {
        Log.d("ServiceManager", "Pausing Relay Services")

        NostrAccountDataSource.stop()
        NostrHomeDataSource.stop()
        NostrChatroomDataSource.stop()
        NostrChatroomListDataSource.stop()

        NostrHashtagDataSource.stop()
        NostrSearchEventOrUserDataSource.stop()
        NostrSingleEventDataSource.stop()
        NostrSingleUserDataSource.stop()
        NostrThreadDataSource.stop()
        NostrUserProfileDataSource.stop()

        Client.disconnect()
    }

    fun cleanUp() {
        LocalCache.cleanObservers()

        account?.let {
            LocalCache.pruneHiddenMessages(it)
            LocalCache.pruneContactLists(it)
            // LocalCache.pruneNonFollows(it)
        }
    }
}
