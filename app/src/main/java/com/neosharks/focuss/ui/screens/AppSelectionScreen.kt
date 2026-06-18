package com.neosharks.focuss.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import com.neosharks.focuss.R
import com.neosharks.focuss.data.InstalledApp
import com.neosharks.focuss.ui.UiState

@Composable
fun AppSelectionScreen(
    state: UiState,
    initiallySelected: Set<String>,
    loadIcon: suspend (String) -> Bitmap?,
    onBack: () -> Unit,
    onSave: (Set<String>) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(initiallySelected) }

    // Selected apps float to the top; the rest follow, filtered by the query.
    val ordered = remember(state.installedApps, query, selected) {
        val q = query.trim().lowercase()
        fun matches(a: InstalledApp) =
            q.isEmpty() || a.appName.lowercase().contains(q) || a.packageName.lowercase().contains(q)
        val chosen = state.installedApps.filter { it.packageName in selected }
        val rest = state.installedApps.filter { it.packageName !in selected && matches(it) }
        chosen + rest
    }

    Column(modifier = Modifier.fillMaxSize().background(cs.background)) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = cs.onSurface)
            }
            Text(
                stringResource(R.string.select_apps_to_block),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }

        // Search
        TextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(stringResource(R.string.search_apps)) },
            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = cs.onSurfaceVariant) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Rounded.Close, "Clear", tint = cs.onSurfaceVariant)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = cs.surface,
                unfocusedContainerColor = cs.surface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )

        // Count bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Text(
                stringResource(R.string.selected_shown, selected.size, ordered.size),
                fontSize = 12.sp,
                color = cs.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (selected.isNotEmpty()) {
                Text(
                    stringResource(R.string.clear_all),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.error,
                    modifier = Modifier.clickable { selected = emptySet() },
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            if (state.appsLoading) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    CircularProgressIndicator(color = cs.primary)
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.loading_apps), color = cs.onSurfaceVariant)
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                    items(ordered, key = { it.packageName }) { app ->
                        AppRow(
                            app = app,
                            checked = app.packageName in selected,
                            loadIcon = loadIcon,
                            onToggle = {
                                selected = if (app.packageName in selected) {
                                    selected - app.packageName
                                } else {
                                    selected + app.packageName
                                }
                            },
                        )
                    }
                }
            }

            // Save button (anchored)
            Surface(
                color = cs.surface,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
            ) {
                Button(
                    onClick = { onSave(selected) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = cs.primary),
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
                ) {
                    Icon(Icons.Rounded.Check, null, tint = cs.onPrimary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (selected.isEmpty()) stringResource(R.string.save) else stringResource(R.string.save_count, selected.size),
                        color = cs.onPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun AppRow(
    app: InstalledApp,
    checked: Boolean,
    loadIcon: suspend (String) -> Bitmap?,
    onToggle: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    // Decode this row's icon lazily and off the main thread; keyed on the package
    // so recycled rows in the LazyColumn re-fetch for their new app.
    val icon by produceState<Bitmap?>(initialValue = null, app.packageName) {
        value = loadIcon(app.packageName)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(cs.primary.copy(alpha = 0.12f)),
        ) {
            if (icon != null) {
                Image(
                    bitmap = icon!!.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)),
                )
            } else {
                Text(
                    app.appName.firstOrNull()?.uppercase() ?: "?",
                    color = cs.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(app.appName, style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
            Text(app.packageName, fontSize = 11.sp, color = cs.onSurfaceVariant, maxLines = 1)
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(7.dp))
                .then(
                    if (checked) Modifier.background(cs.primary)
                    else Modifier.border(2.dp, cs.outline, RoundedCornerShape(7.dp)),
                ),
        ) {
            if (checked) Icon(Icons.Rounded.Check, null, tint = cs.onPrimary, modifier = Modifier.size(14.dp))
        }
    }
}
