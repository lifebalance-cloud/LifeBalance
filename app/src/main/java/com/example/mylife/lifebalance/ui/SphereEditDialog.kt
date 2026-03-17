package com.example.mylife.lifebalance.ui
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lifebalance.R
import com.example.mylife.lifebalance.data.LifeSphere
import java.util.Locale

@Composable
fun SphereEditDialog(
    sphere: LifeSphere?,
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf(sphere?.name ?: "") }
    var selectedColorIndex by remember { mutableStateOf(sphere?.colorIndex ?: 0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (sphere == null) stringResource(R.string.add_area) else stringResource(R.string.edit_the_area),
                color = MaterialTheme.colorScheme.onSecondary, // здесь задаём цвет
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Поле ввода и цветовая палитра
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(text=stringResource(R.string.name)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.tertiary,   // фон когда поле в фокусе
                        unfocusedContainerColor = MaterialTheme.colorScheme.tertiary, // фон когда поле не в фокусе
                        focusedBorderColor = MaterialTheme.colorScheme.primary,   // цвет рамки в фокусе
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary, // цвет рамки без фокуса
                        cursorColor = MaterialTheme.colorScheme.primary,          // цвет курсора
                        focusedLabelColor = MaterialTheme.colorScheme.primary,    // цвет label в фокусе
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSecondary // цвет label без фокуса
                    )
                )

                Text(
                    text = stringResource(R.string.select_color),
                    style = MaterialTheme.typography.bodyMedium
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(colorPalette) { index, color ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedColorIndex = index }
                                .background(color = color)
                                .then(
                                    if (selectedColorIndex == index) {
                                        Modifier
                                            .padding(2.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .padding(2.dp)
                                    } else Modifier
                                )
                        )
                    }
                }

                // Кастомный Row для кнопок по краям
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(R.string.cancel).uppercase(Locale.getDefault()))
                    }
                    TextButton(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(name, selectedColorIndex)
                                onDismiss()
                            }
                        }
                    ) {
                        Text(text = stringResource(R.string.save).uppercase(Locale.getDefault()))
                    }
                }
            }
        },
        // Отключаем стандартные кнопки
        confirmButton = {},
        dismissButton = {}
    )

}
