package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.DownloadItem
import com.example.data.model.DownloadStatus
import com.example.data.repository.VidoraRepository
import kotlinx.coroutines.launch

@Composable
fun DownloadsScreen(
    repository: VidoraRepository,
    modifier: Modifier = Modifier
) {
    val downloads by repository.allDownloads.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    val activeList = downloads.filter { 
        it.status == DownloadStatus.QUEUED || 
        it.status == DownloadStatus.RESOLVING || 
        it.status == DownloadStatus.DOWNLOADING 
    }
    val pausedList = downloads.filter { it.status == DownloadStatus.PAUSED }
    val failedList = downloads.filter { it.status == DownloadStatus.FAILED || it.status == DownloadStatus.CANCELLED }
    val completedList = downloads.filter { it.status == DownloadStatus.COMPLETED }

    var isCompletedExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Downloads Manager",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (completedList.isNotEmpty() || failedList.isNotEmpty()) {
                TextButton(
                    onClick = {
                        scope.launch {
                            repository.clearDownloads()
                        }
                    }
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear List")
                }
            }
        }

        if (downloads.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Download list empty.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "All sync runs across Vidora will locate here.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Active Section
                if (activeList.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Processing Tasks (${activeList.size})")
                    }
                    items(activeList, key = { it.id }) { item ->
                        DownloadManagerCard(
                            item = item,
                            onPause = { repository.pauseDownload(item.id) },
                            onResume = { repository.resumeDownload(item.id) },
                            onCancel = { repository.cancelDownload(item.id) },
                            onDelete = { scope.launch { repository.deleteDownload(item.id) } }
                        )
                    }
                }

                // Paused Section
                if (pausedList.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Paused Pools (${pausedList.size})")
                    }
                    items(pausedList, key = { it.id }) { item ->
                        DownloadManagerCard(
                            item = item,
                            onPause = { repository.pauseDownload(item.id) },
                            onResume = { repository.resumeDownload(item.id) },
                            onCancel = { repository.cancelDownload(item.id) },
                            onDelete = { scope.launch { repository.deleteDownload(item.id) } }
                        )
                    }
                }

                // Failed Section
                if (failedList.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Failed Attempts (${failedList.size})")
                    }
                    items(failedList, key = { it.id }) { item ->
                        DownloadManagerCard(
                            item = item,
                            onPause = {},
                            onResume = {},
                            onCancel = {},
                            onDelete = { scope.launch { repository.deleteDownload(item.id) } },
                            onRetry = { repository.retryDownload(item.id) }
                        )
                    }
                }

                // Completed Section Dropdown
                if (completedList.isNotEmpty()) {
                    item {
                        Card(
                            onClick = { isCompletedExpanded = !isCompletedExpanded },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Completed Caches (${completedList.size})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Icon(
                                    imageVector = if (isCompletedExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Expand list"
                                )
                            }
                        }
                    }

                    if (isCompletedExpanded) {
                        items(completedList, key = { it.id }) { item ->
                            DownloadManagerCard(
                                item = item,
                                onPause = {},
                                onResume = {},
                                onCancel = {},
                                onDelete = { scope.launch { repository.deleteDownload(item.id) } }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
fun DownloadManagerCard(
    item: DownloadItem,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onRetry: (() -> Unit)? = null
) {
    val progress = if (item.fileBytes > 0) item.bytesDownloaded.toFloat() / item.fileBytes else 0f
    val percentage = (progress * 100).toInt()
    val downloadedMB = item.bytesDownloaded / (1024 * 1024)
    val totalMB = item.fileBytes / (1024 * 1024)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Media details block
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        AsyncImage(
                            model = item.thumbnailUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${item.videoQuality} · ${item.providerId.uppercase()}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Task Controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when (item.status) {
                        DownloadStatus.QUEUED, DownloadStatus.RESOLVING -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = onCancel) {
                                Icon(Icons.Default.Close, "Cancel task")
                            }
                        }
                        DownloadStatus.DOWNLOADING -> {
                            IconButton(onClick = onPause) {
                                Icon(Icons.Default.Pause, "Pause download")
                            }
                            IconButton(onClick = onCancel) {
                                Icon(Icons.Default.Close, "Cancel task")
                            }
                        }
                        DownloadStatus.PAUSED -> {
                            IconButton(onClick = onResume) {
                                Icon(Icons.Default.PlayArrow, "Resume task")
                            }
                            IconButton(onClick = onDelete) {
                                Icon(Icons.Default.Delete, "Delete description")
                            }
                        }
                        DownloadStatus.FAILED, DownloadStatus.CANCELLED -> {
                            onRetry?.let {
                                IconButton(onClick = it) {
                                    Icon(Icons.Default.Refresh, "Retry attempt")
                                }
                            }
                            IconButton(onClick = onDelete) {
                                Icon(Icons.Default.Delete, "Delete descriptions")
                            }
                        }
                        DownloadStatus.COMPLETED -> {
                            IconButton(onClick = onDelete) {
                                Icon(Icons.Default.Delete, "Remove from list")
                            }
                        }
                    }
                }
            }

            // Progress bar and descriptive labels
            if (item.status == DownloadStatus.DOWNLOADING || item.status == DownloadStatus.QUEUED || item.status == DownloadStatus.RESOLVING || item.status == DownloadStatus.PAUSED) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = if (item.status == DownloadStatus.PAUSED) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val progressLabel = when (item.status) {
                        DownloadStatus.QUEUED -> "Pending in queue..."
                        DownloadStatus.RESOLVING -> "Resolving stream URLs via cloud..."
                        DownloadStatus.PAUSED -> "Paused ($percentage%)"
                        else -> "Downloading: $percentage% ($downloadedMB MB / $totalMB MB)"
                    }
                    Text(
                        text = progressLabel,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (item.status == DownloadStatus.DOWNLOADING) {
                        Text(
                            text = "Active Sync",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else if (item.status == DownloadStatus.FAILED) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Failure Reason: ${item.errorMessage ?: "Network Connection Timeout"}",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
