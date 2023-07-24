package com.vitorpamplona.amethyst.ui

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.vitorpamplona.amethyst.BuildConfig
import com.vitorpamplona.amethyst.LocalPreferences
import com.vitorpamplona.amethyst.ServiceManager
import com.vitorpamplona.amethyst.service.connectivitystatus.ConnectivityStatus
import com.vitorpamplona.amethyst.service.model.PrivateDmEvent
import com.vitorpamplona.amethyst.service.nip19.Nip19
import com.vitorpamplona.amethyst.service.relays.Client
import com.vitorpamplona.amethyst.ui.components.DefaultMutedSetting
import com.vitorpamplona.amethyst.ui.components.keepPlayingMutex
import com.vitorpamplona.amethyst.ui.navigation.Route
import com.vitorpamplona.amethyst.ui.navigation.debugState
import com.vitorpamplona.amethyst.ui.navigation.getRouteWithArguments
import com.vitorpamplona.amethyst.ui.note.Nip47
import com.vitorpamplona.amethyst.ui.screen.AccountScreen
import com.vitorpamplona.amethyst.ui.screen.AccountStateViewModel
import com.vitorpamplona.amethyst.ui.screen.ThemeViewModel
import com.vitorpamplona.amethyst.ui.theme.AmethystTheme
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : AppCompatActivity() {
    lateinit var navController: NavHostController

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent?.data?.toString()
        val startingPage = uriToRoute(uri)

        LocalPreferences.migrateSingleUserPrefs()
        val language = LocalPreferences.getPreferredLanguage()
        if (language.isNotBlank()) {
            val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(language)
            AppCompatDelegate.setApplicationLocales(appLocale)
        }

        setContent {
            navController = rememberNavController()
            val themeViewModel: ThemeViewModel = viewModel()

            themeViewModel.onChange(LocalPreferences.getTheme())
            AmethystTheme(themeViewModel) {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                    val accountStateViewModel: AccountStateViewModel = viewModel {
                        AccountStateViewModel(this@MainActivity)
                    }

                    AccountScreen(accountStateViewModel, themeViewModel, navController)
                }
            }

            var actionableNextPage by remember { mutableStateOf(startingPage) }
            actionableNextPage?.let {
                LaunchedEffect(it) {
                    navController.navigate(it)
                }
                actionableNextPage = null
            }
        }

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()

        val connectivityManager = getSystemService(ConnectivityManager::class.java) as ConnectivityManager
        connectivityManager.requestNetwork(networkRequest, networkCallback)

        Client.lenient = true
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onResume() {
        super.onResume()

        // starts muted every time
        DefaultMutedSetting.value = true

        // Only starts after login
        GlobalScope.launch(Dispatchers.IO) {
            ServiceManager.start(this@MainActivity)
        }
    }

    override fun onPause() {
        if (BuildConfig.DEBUG) {
            debugState(this)
        }

        ServiceManager.pause()
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        keepPlayingMutex?.stop()
        keepPlayingMutex?.release()
        keepPlayingMutex = null
    }

    /**
     * Release memory when the UI becomes hidden or when system resources become low.
     * @param level the memory-related event that was raised.
     */
    @OptIn(DelicateCoroutinesApi::class)
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        println("Trim Memory $level")
        GlobalScope.launch(Dispatchers.Default) {
            ServiceManager.cleanUp()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        val uri = intent?.data?.toString()
        val startingPage = uriToRoute(uri)

        startingPage?.let { route ->
            val currentRoute = getRouteWithArguments(navController)
            if (!isSameRoute(currentRoute, route)) {
                navController.navigate(route) {
                    popUpTo(Route.Home.route)
                    launchSingleTop = true
                }
            }
        }
    }

    private fun isSameRoute(currentRoute: String?, newRoute: String): Boolean {
        if (currentRoute == null) return false

        if (currentRoute == newRoute) {
            return true
        }

        if (newRoute.startsWith("Event/") && currentRoute.contains("/")) {
            if (newRoute.split("/")[1] == currentRoute.split("/")[1]) {
                return true
            }
        }

        return false
    }

    @OptIn(DelicateCoroutinesApi::class)
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        // network is available for use
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            Log.d("NETWORKCALLBACK", "onAvailable: Disconnecting and connecting again")
            // Only starts after login
            GlobalScope.launch(Dispatchers.IO) {
                ServiceManager.pause()
                ServiceManager.start(this@MainActivity)
            }
        }

        // Network capabilities have changed for the network
        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            super.onCapabilitiesChanged(network, networkCapabilities)

            GlobalScope.launch(Dispatchers.IO) {
                val hasMobileData = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                val hasWifi = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                Log.d("NETWORKCALLBACK", "onCapabilitiesChanged: hasMobileData $hasMobileData")
                Log.d("NETWORKCALLBACK", "onCapabilitiesChanged: hasWifi $hasWifi")
                ConnectivityStatus.updateConnectivityStatus(
                    hasMobileData,
                    hasWifi
                )
            }
        }

        // lost network connection
        override fun onLost(network: Network) {
            super.onLost(network)
            Log.d("NETWORKCALLBACK", "onLost: Disconnecting and pausing relay's connection")
            // Only starts after login
            GlobalScope.launch(Dispatchers.IO) {
                ServiceManager.pause()
            }
        }
    }
}

class GetMediaActivityResultContract : ActivityResultContracts.GetContent() {

    override fun createIntent(context: Context, input: String): Intent {
        return super.createIntent(context, input).apply {
            // Force only images and videos to be selectable
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
        }
    }
}

fun uriToRoute(uri: String?): String? {
    return if (uri.equals("nostr:Notifications", true)) {
        Route.Notification.route.replace("{scrollToTop}", "true")
    } else {
        if (uri?.startsWith("nostr:Hashtag?id=") == true) {
            Route.Hashtag.route.replace("{id}", uri.removePrefix("nostr:Hashtag?id="))
        } else {
            val nip19 = Nip19.uriToRoute(uri)
            when (nip19?.type) {
                Nip19.Type.USER -> "User/${nip19.hex}"
                Nip19.Type.NOTE -> "Note/${nip19.hex}"
                Nip19.Type.EVENT -> {
                    if (nip19.kind == PrivateDmEvent.kind) {
                        "Room/${nip19.author}"
                    } else {
                        "Event/${nip19.hex}"
                    }
                }

                Nip19.Type.ADDRESS -> "Event/${nip19.hex}"
                else -> null
            }
        } ?: try {
            uri?.let {
                Nip47.parse(it)
                val encodedUri = URLEncoder.encode(it, StandardCharsets.UTF_8.toString())
                Route.Home.base + "?nip47=" + encodedUri
            }
        } catch (e: Exception) {
            null
        }
    }
}
