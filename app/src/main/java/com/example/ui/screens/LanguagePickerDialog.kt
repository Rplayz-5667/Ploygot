package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.Language
import com.example.ui.theme.*
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.MainViewModel

@Composable
fun LanguagePickerDialog(
    mainViewModel: MainViewModel,
    authViewModel: AuthViewModel,
    onDismiss: () -> Unit
) {
    val selectedLang by mainViewModel.selectedLanguage.collectAsState()
    var searchPrompt by remember { mutableStateOf("") }
    var activeRegionFilter by remember { mutableStateOf("All") }

    val regions = listOf("All", "Europe", "Asia", "Africa", "Americas", "Middle East", "Pacific", "Ancient/Con")

    val filteredLanguages = remember(searchPrompt, activeRegionFilter) {
        mainViewModel.languages.filter { lang ->
            val matchesSearch = lang.name.contains(searchPrompt, ignoreCase = true) ||
                    lang.nativeName.contains(searchPrompt, ignoreCase = true) ||
                    lang.code.contains(searchPrompt, ignoreCase = true)
            val matchesRegion = activeRegionFilter == "All" || lang.region == activeRegionFilter
            matchesSearch && matchesRegion
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(32.dp),
            color = BgWarm
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "GLOBAL REGISTRY",
                            style = MaterialTheme.typography.labelLarge,
                            color = DeepGreen
                        )
                        Text(
                            text = "${mainViewModel.languages.size} Languages",
                            style = MaterialTheme.typography.displayLarge,
                            color = DarkOlive,
                            fontSize = 28.sp
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(Color.White, RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = DarkOlive)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Search field
                OutlinedTextField(
                    value = searchPrompt,
                    onValueChange = { searchPrompt = it },
                    placeholder = { Text("Search by name, native, or code...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = DeepGreen) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeepGreen,
                        unfocusedBorderColor = SandyBorder,
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Region Tabs
                ScrollableTabRow(
                    selectedTabIndex = regions.indexOf(activeRegionFilter).coerceAtLeast(0),
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent,
                    divider = {},
                    indicator = {}
                ) {
                    regions.forEach { region ->
                        val isSelected = activeRegionFilter == region
                        Tab(
                            selected = isSelected,
                            onClick = { activeRegionFilter = region },
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) DeepGreen else Color.White)
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = region,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else DarkOlive,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Language List counts display
                Text(
                    text = "Showing ${filteredLanguages.size} matching items",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Lazy Column of Languages
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredLanguages) { lang ->
                        val isSelected = lang.code == selectedLang.code
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) LimeGreen else Color.White)
                                .clickable {
                                    mainViewModel.selectLanguage(lang, authViewModel)
                                    onDismiss()
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Flag container
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(BgWarm, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(lang.flag, fontSize = 24.sp)
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = lang.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkOlive
                                )
                                Text(
                                    text = lang.nativeName,
                                    fontSize = 13.sp,
                                    color = TextGray
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                // Category / Region tag
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = when (lang.difficulty) {
                                                "Easy" -> Color(0xFFE8F5E9)
                                                "Medium" -> Color(0xFFFFF3E0)
                                                else -> Color(0xFFFFEBEE)
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = lang.difficulty,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (lang.difficulty) {
                                            "Easy" -> Color(0xFF2E7D32)
                                            "Medium" -> Color(0xFFEF6C00)
                                            else -> Color(0xFFC62828)
                                        }
                                    )
                                }
                                Text(
                                    text = "Code: " + lang.code.uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextGray,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
