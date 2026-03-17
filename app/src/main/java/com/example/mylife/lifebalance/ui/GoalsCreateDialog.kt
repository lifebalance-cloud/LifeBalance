package com.example.mylife.lifebalance.ui
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.lifebalance.R
import com.example.mylife.lifebalance.data.LifeSphere
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import java.util.Locale

@Composable
fun GoalsCreateDialog(
    sphere: LifeSphere,
    onDismiss: () -> Unit,
    onSaveGoal: (String, LocalDate) -> Unit,
    initialGoalText: String = ""
) {
    var goalText by remember { mutableStateOf(initialGoalText) }
    var deadline by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Заголовок
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.improve_area),
                        color = MaterialTheme.colorScheme.onSecondary, // цвет первой строки
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = sphere.name,
                        color = MaterialTheme.colorScheme.primary, // цвет второй строки
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Контент
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = goalText,
                        onValueChange = { goalText = it },
                        label = { Text(text = stringResource(R.string.create_goal)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
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
                    val appLocale = getAppLocale()
                    val dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", appLocale)
                    val formattedDeadline = deadline.format(dateFormatter).replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(appLocale) else it.toString()
                    }
                    
                    Button(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(text = stringResource(R.string.select_date).uppercase(Locale.getDefault()),
                            color= MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        text = stringResource(R.string.deadline_text, formattedDeadline),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                // Кнопки
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(R.string.cancel).uppercase(Locale.getDefault()))
                    }
                    TextButton(onClick = {
                        if (goalText.isNotBlank()) {
                            onSaveGoal(goalText, deadline)
                            onDismiss()
                        }
                    }) {
                        Text(text = stringResource(R.string.save).uppercase(Locale.getDefault()))
                    }
                }
            }
        }
    }
    
    // Диалог выбора даты
    if (showDatePicker) {
        var selectedDate by remember { mutableStateOf(deadline) }
        
        LaunchedEffect(showDatePicker) {
            if (showDatePicker) {
                selectedDate = deadline
            }
        }
        
        Dialog(onDismissRequest = { showDatePicker = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Заголовок
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val appLocale = getAppLocale()
                            val dayOfWeek = selectedDate.format(
                                DateTimeFormatter.ofPattern("EEEE", appLocale)
                            ).replaceFirstChar { it.titlecase(appLocale) }

                            Text(
                                text = dayOfWeek,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            val dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", appLocale)
                            val formattedDate = selectedDate.format(dateFormatter).replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase(appLocale) else it.toString()
                            }

                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Календарь
                    DatePickerCalendar(
                        initialDate = selectedDate,
                        onDateSelected = { newDate ->
                            selectedDate = newDate
                        }
                    )

                    // Кнопки
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text(text = stringResource(R.string.cancel).uppercase(Locale.getDefault()))
                        }
                        TextButton(onClick = { 
                            deadline = selectedDate
                            showDatePicker = false 
                        }) {
                            Text(text = stringResource(R.string.save).uppercase(Locale.getDefault()))
                        }
                    }
                }
            }
        }
    }
}
