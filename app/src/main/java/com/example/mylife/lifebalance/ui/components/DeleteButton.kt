package com.example.mylife.lifebalance.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.lifebalance.R

//ФУНКЦИЯ ДЛЯ КНОПКИ УДАЛИТЬ(мусорное ведро)
@Composable
fun DeleteButton(
    isActive: Boolean,
    onDelete: () -> Unit
) {
    IconButton(onClick = onDelete) {
        Icon(
            painter = painterResource(R.drawable.ic_bin),
            contentDescription = "Удалить",
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSecondary
        )
    }
}
