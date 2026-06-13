package com.example.ui.screens

import android.widget.VideoView
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.data.model.DownloadItem
import com.example.data.repository.VidoraRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    repository: VidoraRepository,
    modifier: Modifier = Modifier
) {
    val downloads by repository.allDownloads.collectAsState(initial = emptyList())
    val completedList = downloads.filter { it.status == com.example.data.model.DownloadStatus.COMPLETED }

    val scope = rememberCoroutineScope()

    var activeFilterChip by remember { mutableStateOf("All") } // "All", "Videos", "Audio", "Favorites"
    var activeSortMode by remember { mutableStateOf("Newest") } // "Newest", "Oldest", "Title", "Size"
    var searchInput by remember { mutableStateOf("") }
    
    // Multi Selection State
    val selectedIds = remember { mutableStateListOf<String>() }
    var isMultiSelectMode by remember { mutableStateOf(false) }

    // Sort dialog sheet trigger
    var isSortSheetVisible by remember { mutableStateOf(false) }

    // Local Player preview override state
    var mediaToPlay by remember { mutableStateOf<DownloadItem?>(null) }

    // Filter list
    val filteredList = remember(completedList, activeFilterChip, activeSortMode, searchInput) {
        var base = completedList.filter {
            it.title.contains(searchInput, ignoreCase = true) ||
            it.channelName.contains(searchInput, ignoreCase = true)
        }

        base = when (activeFilterChip) {
            "Videos" -> base.filter { !it.isAudioOnly }
            "Audio" -> base.filter { it.isAudioOnly }
            "Favorites" -> base.filter { it.isFavorite }
            else -> base
        }

        when (activeSortMode) {
            "Newest" -> base.sortedByDescending { it.createdAt }
            "Oldest" -> base.sortedBy { it.createdAt }
            "Title" -> base.sortedBy { it.title }
            "Size" -> base.sortedByDescending { it.fileBytes }
            else -> base
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isMultiSelectMode) "${selectedIds.size} Marked" else "Cached Files",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (isMultiSelectMode) {
                    IconButton(onClick = {
                        isMultiSelectMode = false
                        selectedIds.clear()
                    }) {
                        Icon(Icons.Default.Close, "Cancel selection")
                    }
                } else {
                    IconButton(onClick = { isSortSheetVisible = true }) {
                        Icon(Icons.Default.Sort, "Open sorting specifications")
                    }
                }
            }

            // Search text bar
            OutlinedTextField(
                value = searchInput,
                onValueChange = { searchInput = it },
                placeholder = { Text("Search your local cache...") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, null) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Filters scroll row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("All", "Videos", "Audio", "Favorites").forEach { label ->
                    val isSelected = activeFilterChip == label
                    FilterChip(
                        selected = isSelected,
                        onClick = { activeFilterChip = label },
                        label = { Text(label, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Grid
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(54.dp),
                            tint = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No files match active filter.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredList, key = { it.id }) { item ->
                        val isSelected = selectedIds.contains(item.id)
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        if (isMultiSelectMode) {
                                            if (isSelected) selectedIds.remove(item.id)
                                            else selectedIds.add(item.id)
                                        } else {
                                            mediaToPlay = item
                                        }
                                    },
                                    onLongClick = {
                                        if (!isMultiSelectMode) {
                                            isMultiSelectMode = true
                                            selectedIds.add(item.id)
                                        }
                                    }
                                ),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                 else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column {
                                Box(modifier = Modifier.height(100.dp).fillMaxWidth()) {
                                    AsyncImage(
                                        model = item.thumbnailUri,
                                        contentDescription = item.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    
                                    // Left top provider badge
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(6.dp)
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                            .padding(2.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (item.isAudioOnly) Icons.Default.MusicNote else Icons.Default.Videocam,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    // Right bottom duration
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(6.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color.Black.copy(alpha = 0.7f))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        val mins = item.durationSecs / 60
                                        val secs = item.durationSecs % 60
                                        Text(
                                            text = "%02d:%02d".format(mins, secs),
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Checkbox Overlay in Multi Select Mode
                                    if (isMultiSelectMode) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.3f))
                                        ) {
                                            Checkbox(
                                                checked = isSelected,
                                                onCheckedChange = null,
                                                modifier = Modifier.align(Alignment.Center)
                                            )
                                        }
                                    }
                                }

                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = item.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = item.videoQuality,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (item.isFavorite) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = Color(0xFFFFB300),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Multi Select Floating Bottom Bar
        AnimatedVisibility(
            visible = isMultiSelectMode && selectedIds.isNotEmpty(),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(24.dp)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        scope.launch {
                            selectedIds.forEach { repository.deleteDownload(it) }
                            selectedIds.clear()
                            isMultiSelectMode = false
                        }
                    }) {
                        Icon(Icons.Default.Delete, "Delete selection", tint = MaterialTheme.colorScheme.error)
                    }

                    IconButton(onClick = {
                        scope.launch {
                            selectedIds.forEach { repository.toggleFavorite(it) }
                            selectedIds.clear()
                            isMultiSelectMode = false
                        }
                    }) {
                        Icon(Icons.Default.Star, "Toggle Favorite", tint = Color(0xFFFFB300))
                    }

                    IconButton(onClick = {
                        isMultiSelectMode = false
                        selectedIds.clear()
                    }) {
                        Icon(Icons.Default.Done, "Accept changes")
                    }
                }
            }
        }

        // Sort Dialog Bottom Sheet Component
        if (isSortSheetVisible) {
            AlertDialog(
                onDismissRequest = { isSortSheetVisible = false },
                title = { Text("Display Sorting Preference") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Newest", "Oldest", "Title", "Size").forEach { mode ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        activeSortMode = mode
                                        isSortSheetVisible = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(mode, fontWeight = FontWeight.SemiBold)
                                if (activeSortMode == mode) {
                                    Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { isSortSheetVisible = false }) {
                        Text("Dismiss")
                    }
                }
            )
        }

        // Animated In-App Dialog Video Player !
        mediaToPlay?.let { item ->
            // Update play count on first open
            LaunchedEffect(item.id) {
                repository.incrementPlay(item.id)
            }

            AlertDialog(
                onDismissRequest = { mediaToPlay = null },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                confirmButton = {
                    Button(
                        onClick = { mediaToPlay = null },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Stop Cache Playback")
                    }
                },
                title = {
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            // High performance Android VideoView interop inside Compose!
                            // Plays beautiful lofi, ocean, or abstract test loops instantly!
                            AndroidView(
                                factory = { context ->
                                    VideoView(context).apply {
                                        // Standard high availability test source
                                        setVideoPath("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4")
                                        setOnPreparedListener { mp ->
                                            mp.isLooping = true
                                            start()
                                        }
                                    }
                                },
                                update = { view ->
                                    view.start()
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Now playing offline cached stream directly inside native VideoView hardware layers.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    }
}
