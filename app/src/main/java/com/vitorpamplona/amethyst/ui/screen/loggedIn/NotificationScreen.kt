package com.vitorpamplona.amethyst.ui.screen.loggedIn

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.vitorpamplona.amethyst.service.NostrAccountDataSource
import com.vitorpamplona.amethyst.ui.navigation.Route
import com.vitorpamplona.amethyst.ui.note.OneGiga
import com.vitorpamplona.amethyst.ui.note.OneKilo
import com.vitorpamplona.amethyst.ui.note.OneMega
import com.vitorpamplona.amethyst.ui.screen.NotificationViewModel
import com.vitorpamplona.amethyst.ui.screen.RefresheableCardView
import com.vitorpamplona.amethyst.ui.screen.ScrollStateKeys
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun NotificationScreen(
    notifFeedViewModel: NotificationViewModel,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    WatchAccountForNotifications(notifFeedViewModel, accountViewModel)

    val lifeCycleOwner = LocalLifecycleOwner.current
    DisposableEffect(accountViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                NostrAccountDataSource.invalidateFilters()
            }
        }

        lifeCycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifeCycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(Modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier.padding(vertical = 0.dp)
        ) {
            RefresheableCardView(
                viewModel = notifFeedViewModel,
                accountViewModel = accountViewModel,
                nav = nav,
                routeForLastRead = Route.Notification.base,
                scrollStateKey = ScrollStateKeys.NOTIFICATION_SCREEN
            )
        }
    }
}

@Composable
fun WatchAccountForNotifications(
    notifFeedViewModel: NotificationViewModel,
    accountViewModel: AccountViewModel
) {
    val accountState by accountViewModel.accountLiveData.observeAsState()

    LaunchedEffect(accountViewModel, accountState?.account?.defaultNotificationFollowList) {
        NostrAccountDataSource.invalidateFilters()
        notifFeedViewModel.checkKeysInvalidateDataAndSendToTop()
    }
}

fun showAmountAxis(amount: BigDecimal?): String {
    if (amount == null) return ""
    if (amount.abs() < BigDecimal(0.01)) return ""

    return when {
        amount >= OneGiga -> "%.0fG".format(amount.div(OneGiga).setScale(0, RoundingMode.HALF_UP))
        amount >= OneMega -> "%.0fM".format(amount.div(OneMega).setScale(0, RoundingMode.HALF_UP))
        amount >= OneKilo -> "%.0fk".format(amount.div(OneKilo).setScale(0, RoundingMode.HALF_UP))
        else -> "%.0f".format(amount)
    }
}
