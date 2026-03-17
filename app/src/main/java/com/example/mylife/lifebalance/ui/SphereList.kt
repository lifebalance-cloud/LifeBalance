package com.example.mylife.lifebalance.ui
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifebalance.R
import com.example.mylife.lifebalance.data.LifeSphere
import com.example.mylife.lifebalance.ui.components.DeleteButton
import java.util.Locale


@Composable
fun SphereList(
    spheres: List<LifeSphere>,
    onSphereClick: (LifeSphere) -> Unit,
    onScoreChange: (LifeSphere, Int) -> Unit,
    onDeleteSphere: (LifeSphere) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(spheres, key = { it.id }) { sphere ->
            SphereItem(
                sphere = sphere,
                onClick = { onSphereClick(sphere) },
                onScoreChange = { score -> onScoreChange(sphere, score) },
                onDelete = { onDeleteSphere(sphere) }
            )
        }
    }
}
@Composable
fun SphereItem(
    sphere: LifeSphere,
    onClick: () -> Unit,
    onScoreChange: (Int) -> Unit,
    onDelete: () -> Unit
) {
    var showScoreDialog by remember { mutableStateOf(false) }

    if (showScoreDialog) {
        ScoreDialog(
            sphereName = sphere.name,
            onDismiss = { showScoreDialog = false },
            onSelect = { score ->
                onScoreChange(score)
                showScoreDialog = false
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = MaterialTheme.shapes.small
            )
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            // Левая часть: цвет + название + текущий балл
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            color = colorPalette[sphere.colorIndex % colorPalette.size],
                            shape = MaterialTheme.shapes.small
                        )
                )

                Text(
                    text = sphere.name,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "${sphere.score}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSecondary
                )
            }


            // Правая часть: кнопка ОЦЕНИТЬ + удалить
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showScoreDialog = true },
                    modifier = Modifier
                        .height(40.dp)
                        .padding(start = 8.dp),   // ⬅️ внешний отступ слева
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.assess).uppercase(Locale.getDefault()),
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp) // размер области клика
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Удалить",
                        tint = MaterialTheme.colorScheme.error, // красная иконка
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
@Composable
fun ScoreDialog(
    sphereName: String,   // имя сферы
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    val items = listOf(
        1 to stringResource(R.string.score_1),
        2 to stringResource(R.string.score_2),
        3 to stringResource(R.string.score_3),
        4 to stringResource(R.string.score_4),
        5 to stringResource(R.string.score_5),
        6 to stringResource(R.string.score_6),
        7 to stringResource(R.string.score_7),
        8 to stringResource(R.string.score_8),
        9 to stringResource(R.string.score_9),
        10 to stringResource(R.string.score_10)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = stringResource(R.string.assess_life_area),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    sphereName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp), // ограничивает высоту, появляется прокрутка
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items) { (value, description) ->
                    Button(
                        onClick = { onSelect(value) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Text("$value — $description", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel).uppercase(Locale.getDefault()))
            }
        }
    )
}

