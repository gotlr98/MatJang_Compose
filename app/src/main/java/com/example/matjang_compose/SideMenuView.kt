package com.example.matjang_compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// 👇 사이드 메뉴 전체 컨텐츠
@Composable
fun SideMenuContent(
    viewModel: MainMapViewModel,
    onMatjipClick: (Matjip) -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val bookmarkFolders by viewModel.bookmarkFolders.collectAsState()
    val folderMatjips by viewModel.folderMatjips.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // 🧑‍💻 프로필 영역
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = "프로필",
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = userProfile?.nickname ?: "Guest",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = userProfile?.email ?: "로그인 정보 없음",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 📚 폴더 리스트
        Text(
            "나의 북마크 폴더",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Divider()

        LazyColumn {
            items(bookmarkFolders) { folder ->
                FolderItem(
                    folder = folder,
                    viewModel = viewModel,
                    onMatjipClick = onMatjipClick,
                    savedMatjips = folderMatjips[folder.id] ?: emptyList()
                )
            }
        }
    }
}

// 📂 폴더 아이템 컴포넌트
@Composable
fun FolderItem(
    folder: BookmarkFolder,
    viewModel: MainMapViewModel,
    onMatjipClick: (Matjip) -> Unit,
    savedMatjips: List<Matjip>
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "rotation")

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    expanded = !expanded
                    if (expanded) {
                        viewModel.fetchMatjipsInFolder(folder.id)
                    }
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Folder, contentDescription = "폴더", tint = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.width(16.dp))
            Text(folder.name, modifier = Modifier.weight(1f))
            Text("(${savedMatjips.size})", style = MaterialTheme.typography.bodySmall)
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = "확장",
                modifier = Modifier.rotate(rotation)
            )
        }

        // 📌 폴더 내용 (맛집 리스트)
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(start = 24.dp)) {
                if (savedMatjips.isEmpty()) {
                    Text(
                        "저장된 맛집이 없습니다.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
                    )
                } else {
                    savedMatjips.forEach { matjip ->
                        MatjipItem(matjip = matjip, onMatjipClick = onMatjipClick)
                        Divider(Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
        Divider()
    }
}

// 🍽️ 맛집 아이템 컴포넌트
@Composable
fun MatjipItem(matjip: Matjip, onMatjipClick: (Matjip) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onMatjipClick(matjip) }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Place, contentDescription = "장소", modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(matjip.place_name, style = MaterialTheme.typography.bodyMedium)
            Text(matjip.category_name.split(" > ").last(), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}