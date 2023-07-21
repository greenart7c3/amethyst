package com.vitorpamplona.amethyst.ui.buttons

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vitorpamplona.amethyst.R
import com.vitorpamplona.amethyst.ui.actions.JoinUserOrChannelView
import com.vitorpamplona.amethyst.ui.screen.loggedIn.AccountViewModel

@Composable
fun ChannelFabColumn(accountViewModel: AccountViewModel, nav: (String) -> Unit) {
    var wantsToJoinChannelOrUser by remember {
        mutableStateOf(false)
    }

    if (wantsToJoinChannelOrUser) {
        JoinUserOrChannelView({ wantsToJoinChannelOrUser = false }, accountViewModel = accountViewModel, nav = nav)
    }

    Column {
        OutlinedButton(
            onClick = { wantsToJoinChannelOrUser = true },
            modifier = Modifier.size(55.dp),
            shape = CircleShape,
            colors = ButtonDefaults.outlinedButtonColors(backgroundColor = MaterialTheme.colors.primary),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = stringResource(R.string.new_channel),
                modifier = Modifier.size(26.dp),
                tint = Color.White
            )
        }
    }
}
