package com.example.timeprogress

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.*
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TimeProgressScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun TimeProgressScreen() {

    var startTime by remember { mutableStateOf<LocalTime?>(null) }
    var endTime by remember { mutableStateOf<LocalTime?>(null) }

    val now by produceState(initialValue = LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            delay(1000L)
        }
    }

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    val startPickerState = rememberTimePickerState()
    val endPickerState = rememberTimePickerState()

    val rawProgress by remember(startTime, endTime, now) {
        derivedStateOf {
            if (startTime == null || endTime == null) return@derivedStateOf 0f

            val today = LocalDate.now()
            var start = today.atTime(startTime)
            var end = today.atTime(endTime)

            if (!end.isAfter(start)) {
                end = end.plusDays(1)
            }

            when {
                now < start -> 0f
                now >= end   -> 1f
                else -> {
                    val total = Duration.between(start, end).toMillis().toFloat()
                    val passed = Duration.between(start, now).toMillis().toFloat()
                    (passed / total).coerceIn(0f, 1f)
                }
            }
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(
            durationMillis = 800,
            easing = FastOutSlowInEasing
        ),
        label = "progress animation"
    )

    // درصد را بر اساس مقدار خام (بدون انیمیشن) محاسبه می‌کنیم تا عدد پرش نداشته باشد
    val percentRemaining = ((1 - rawProgress) * 100).toInt().coerceIn(0, 100)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Time Progress") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {

            TimeSelectorRow(
                label = "Start time",
                time = startTime,
                onClick = { showStartPicker = true }
            )

            TimeSelectorRow(
                label = "End time",
                time = endTime,
                onClick = { showEndPicker = true }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Now: ${now.format(DateTimeFormatter.ofPattern("HH:mm:ss"))}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(18.dp))

            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(260.dp)) {
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.size(260.dp),
                    strokeWidth = 24.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$percentRemaining%",
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "remaining",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedButton(onClick = {
                startTime = null
                endTime = null
            }) {
                Text("Reset")
            }

            Spacer(modifier = Modifier.height(45.dp))
        }
    }

    if (showStartPicker) {
        TimePickerDialog(
            title = "Select start time",
            state = startPickerState,
            onDismiss = { showStartPicker = false },
            onConfirm = {
                startTime = LocalTime.of(startPickerState.hour, startPickerState.minute)
                showStartPicker = false
            }
        )
    }

    if (showEndPicker) {
        TimePickerDialog(
            title = "Select end time",
            state = endPickerState,
            onDismiss = { showEndPicker = false },
            onConfirm = {
                endTime = LocalTime.of(endPickerState.hour, endPickerState.minute)
                showEndPicker = false
            }
        )
    }
}

@Composable
private fun TimeSelectorRow(
    label: String,
    time: LocalTime?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium
        )

        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Text(
                text = time?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "Pick time",
                fontSize = 16.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    title: String,
    state: TimePickerState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}