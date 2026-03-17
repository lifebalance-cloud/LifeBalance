package com.example.mylife.lifebalance.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.lifebalance.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarWithIcons(onNavigate: (Screen) -> Unit) {
    TopAppBar(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onNavigate(Screen.Home) }) {
                    Icon(painter = painterResource(R.drawable.ic_home), contentDescription = "Дела", modifier = Modifier.size(32.dp))
                }
                IconButton(onClick = { onNavigate(Screen.Goals) }) {
                    Icon(painter = painterResource(R.drawable.ic_goals), contentDescription = "Главные цели", modifier = Modifier.size(32.dp))
                }
                IconButton(onClick = { onNavigate(Screen.Balance) }) {
                    Icon(painter = painterResource(R.drawable.ic_balance), contentDescription = "Колесо баланса", modifier = Modifier.size(32.dp))
                }
                IconButton(onClick = { onNavigate(Screen.Ideas) }) {
                    Icon(painter = painterResource(R.drawable.ic_ideas), contentDescription = "Идеи", modifier = Modifier.size(32.dp))
                }
                IconButton(onClick = { onNavigate(Screen.Calendar) }) {
                    Icon(painter = painterResource(R.drawable.ic_calendar), contentDescription = "Календарь", modifier = Modifier.size(32.dp))
                }
                IconButton(onClick = { onNavigate(Screen.Settings) }) {
                    Icon(painter = painterResource(R.drawable.ic_settings), contentDescription = "Настройки", modifier = Modifier.size(32.dp))
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}