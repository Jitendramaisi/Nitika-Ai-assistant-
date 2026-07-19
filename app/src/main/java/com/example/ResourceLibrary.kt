package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.ResourceEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceLibrarySheet(
    viewModel: ChatViewModel,
    onDismiss: () -> Unit
) {
    val resources by viewModel.resources.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DeepAbyss,
        contentColor = LightIvory,
        dragHandle = { BottomSheetDefaults.DragHandle(color = WarmGrey) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Academic Library",
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = ScholasticGold
                )
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Resource",
                        tint = ScholasticGold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (resources.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No resources found in the library.", color = WarmGrey, fontSize = 14.sp)
                }
            } else {
                val categories = resources.map { it.category }.distinct()

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    categories.forEach { category ->
                        item {
                            Text(
                                text = category.uppercase(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarmGrey,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(bottom = 4.dp, top = 8.dp)
                            )
                        }
                        
                        items(resources.filter { it.category == category }) { resource ->
                            ResourceItemCard(
                                resource = resource,
                                onDelete = { viewModel.deleteResource(resource.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddResourceDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { title, url, category, description ->
                viewModel.addResource(title, url, category, description)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ResourceItemCard(resource: ResourceEntity, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardSlate),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, ScholasticGold.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(OxfordBlue)
                    .border(1.dp, ScholasticGold.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = "Link",
                    tint = ScholasticGold,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = resource.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = LightIvory
                )
                if (resource.description.isNotBlank()) {
                    Text(
                        text = resource.description,
                        fontSize = 12.sp,
                        color = WarmGrey,
                        maxLines = 2
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = resource.url,
                    fontSize = 11.sp,
                    color = SageGreen,
                    maxLines = 1
                )
            }
            
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = CrimsonRed.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddResourceDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardSlate,
        titleContentColor = ScholasticGold,
        textContentColor = LightIvory,
        title = {
            Text("Add Academic Resource", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title", color = WarmGrey) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = LightIvory,
                        unfocusedTextColor = LightIvory,
                        focusedBorderColor = ScholasticGold,
                        unfocusedBorderColor = WarmGrey
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL / Link", color = WarmGrey) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = LightIvory,
                        unfocusedTextColor = LightIvory,
                        focusedBorderColor = ScholasticGold,
                        unfocusedBorderColor = WarmGrey
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (e.g. Physics)", color = WarmGrey) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = LightIvory,
                        unfocusedTextColor = LightIvory,
                        focusedBorderColor = ScholasticGold,
                        unfocusedBorderColor = WarmGrey
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)", color = WarmGrey) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = LightIvory,
                        unfocusedTextColor = LightIvory,
                        focusedBorderColor = ScholasticGold,
                        unfocusedBorderColor = WarmGrey
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                colors = ButtonDefaults.buttonColors(containerColor = ScholasticGold, contentColor = OnGold),
                onClick = {
                    if (title.isNotBlank() && category.isNotBlank()) {
                        onAdd(title, url, category, description)
                    }
                },
                enabled = title.isNotBlank() && category.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = LightIvory)
            }
        }
    )
}
