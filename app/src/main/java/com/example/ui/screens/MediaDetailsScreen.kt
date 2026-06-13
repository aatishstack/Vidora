package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Star
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
import com.example.data.model.MediaFormat
import com.example.data.repository.VidoraRepository
import kotlinx.coroutines.launch

@Composable
fun MediaDetailsScreen(
    url: String,
    providerId: String,
    repository: VidoraRepository,
    onBack: () -> Unit,
    onDownloadEnqueued: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var isLoading by remember { mutableStateOf(true) }
    var titleState by remember { mutableStateOf("") }
    var thumbnailState by remember { mutableStateOf("") }
    var channelState by remember { mutableStateOf("") }
    var durationSecsState by remember { mutableIntStateOf(180) }
    var formatsState by remember { mutableStateOf<List<MediaFormat>>(emptyList()) }

    LaunchedEffect(url) {
        isLoading = true
        val response = repository.getRealMediaInfo(url)
        if (response != null) {
            titleState = response.title
            thumbnailState = response.thumbnail ?: ""
            channelState = response.channel ?: "Unknown Creator"
            durationSecsState = response.duration?.toInt() ?: 180
            
            val mappedFormats = response.formats?.map { apiFormat ->
                val sizeSec = apiFormat.approxBytes
                val label = if (sizeSec != null) {
                    val sizeMB = sizeSec / (1024 * 1024)
                    "${apiFormat.qualityLabel ?: "Video"} · ${apiFormat.ext.uppercase()} · ~$sizeMB MB"
                } else {
                    "${apiFormat.qualityLabel ?: "Video"} · ${apiFormat.ext.uppercase()}"
                }
                MediaFormat(
                    formatId = apiFormat.formatId,
                    label = label,
                    ext = apiFormat.ext,
                    isAudioOnly = apiFormat.height == null && apiFormat.width == null,
                    width = apiFormat.width,
                    height = apiFormat.height,
                    fileSizeBytes = apiFormat.approxBytes ?: (25 * 1024 * 1024L),
                    isRecommended = apiFormat.qualityLabel?.contains("1080") == true || apiFormat.qualityLabel?.contains("720") == true,
                    url = apiFormat.url
                )
            } ?: emptyList()

            formatsState = mappedFormats
        } else {
            titleState = repository.getTitleForMockUrl(url, providerId)
            thumbnailState = repository.getThumbnailForMockUrl(url, providerId)
            channelState = repository.getChannelForMockUrl(providerId)
            durationSecsState = 184
            formatsState = repository.fetchFormatsForProvider(providerId)
        }
        isLoading = false
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0 = Video, 1 = Audio
    val filteredFormats = remember(selectedTabIndex, formatsState) {
        if (selectedTabIndex == 0) formatsState.filter { !it.isAudioOnly }
        else formatsState.filter { it.isAudioOnly }
    }

    var selectedFormatIndex by remember { mutableIntStateOf(0) }
    val activeFormat = remember(selectedFormatIndex, filteredFormats) {
        filteredFormats.getOrNull(selectedFormatIndex)
    }

    var includeSubtitles by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App top header bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back to home dashboard button", tint = MaterialTheme.colorScheme.onBackground)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Format Selection",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
            ) {
                // Video Preview card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    AsyncImage(
                        model = thumbnailState,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        val minutes = durationSecsState / 60
                        val seconds = durationSecsState % 60
                        Text(
                            text = "%d:%02d".format(minutes, seconds),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Metadata info block
                Text(
                    text = titleState,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    lineHeight = 24.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Channel: $channelState · ${providerId.uppercase()}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Segment Tabs
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { 
                            selectedTabIndex = 0
                            selectedFormatIndex = 0
                        },
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text("Video", fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))
                    }
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { 
                            selectedTabIndex = 1
                            selectedFormatIndex = 0
                        },
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text("Audio Only", fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Spec formats list
                filteredFormats.forEachIndexed { index, format ->
                    val isSelected = index == selectedFormatIndex
                    Card(
                        onClick = { selectedFormatIndex = index },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) 
                                             else MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(14.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                  Text(
                                      text = format.label,
                                      fontWeight = FontWeight.Bold,
                                      color = MaterialTheme.colorScheme.onBackground,
                                      fontSize = 14.sp
                                  )
                                  if (format.isRecommended) {
                                      Spacer(modifier = Modifier.width(8.dp))
                                      Box(
                                          modifier = Modifier
                                              .clip(RoundedCornerShape(4.dp))
                                              .background(MaterialTheme.colorScheme.primary)
                                              .padding(horizontal = 4.dp, vertical = 2.dp)
                                      ) {
                                          Text(
                                              text = "Best Ratio",
                                              color = Color.White,
                                              fontSize = 9.sp,
                                              fontWeight = FontWeight.ExtraBold
                                          )
                                      }
                                  }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Extension format: .${format.ext} · Codecs: hardware direct",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Active selection format specification indicator",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Subtitle Option Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Subtitles integration",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Auto-extract system subtitles in native language (.vtt)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = includeSubtitles,
                        onCheckedChange = { includeSubtitles = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Bottom CTA Action
            Card(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                val targetFormat = activeFormat ?: return@launch
                                repository.enqueueDownload(
                                    url = url,
                                    providerId = providerId,
                                    title = titleState,
                                    thumbnailUri = thumbnailState,
                                    durationSecs = durationSecsState,
                                    videoQuality = targetFormat.label,
                                    isAudioOnly = targetFormat.isAudioOnly,
                                    fileBytes = targetFormat.fileSizeBytes ?: (25 * 1024 * 1024L),
                                    channelName = channelState,
                                    directUrl = targetFormat.url
                                )
                                onDownloadEnqueued()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Initiate Sync: ${activeFormat?.label?.split("·")?.get(0)?.trim() ?: "Download"}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}
