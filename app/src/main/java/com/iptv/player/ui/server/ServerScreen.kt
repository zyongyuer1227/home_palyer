package com.iptv.player.ui.server

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iptv.player.IptvApplication
import com.iptv.player.data.api.ApiClient
import com.iptv.player.data.repo.ServerRepo
import com.iptv.player.ui.common.CollectErrors
import com.iptv.player.ui.common.rememberToastMessage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as IptvApplication
    val serverRepo = app.serverRepo
    val state by serverRepo.state.collectAsState()
    val scope = rememberCoroutineScope()
    val toast = rememberToastMessage()

    var address by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var testing by remember { mutableStateOf(false) }
    var testingServerId by remember { mutableStateOf<String?>(null) }
    var showSavedAddresses by remember { mutableStateOf(false) }
    var deletingId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("服务器设置") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("添加服务器", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("服务器地址") },
                            placeholder = {
                                Text("192.168.1.100:8081 或 https://x.com/iptv")
                            },
                            supportingText = {
                                Text("支持 http/https、公网 IP、反向代理子路径", style = MaterialTheme.typography.bodySmall)
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        )
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("名称（可选）") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        )
                        Button(
                            onClick = {
                                if (address.isBlank()) {
                                    toast("请输入服务器地址")
                                    return@Button
                                }
                                testing = true
                                scope.launch {
                                    val ok = ApiClient.checkHealth(address.trim())
                                    testing = false
                                    if (ok) {
                                        serverRepo.add(address.trim(), name.trim())
                                        toast("连接成功")
                                        address = ""
                                        name = ""
                                    } else {
                                        toast("连接失败：无法访问 /api/health")
                                    }
                                }
                            },
                            enabled = !testing,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        ) {
                            if (testing) {
                                CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp), strokeWidth = 2.dp)
                                Text("正在测试连接...")
                            } else {
                                Text("测试连接并添加")
                            }
                        }
                    }
                }
            }

            if (state.servers.isNotEmpty()) {
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "已保存的服务器",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { showSavedAddresses = !showSavedAddresses }) {
                            Text(if (showSavedAddresses) "隐藏地址" else "显示地址")
                        }
                    }
                }
                items(state.servers, key = { it.id }) { server ->
                    val active = server.id == state.activeId
                    val testingThis = testingServerId == server.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch { serverRepo.select(server.id) }
                            },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                if (active) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                contentDescription = if (active) "当前使用" else "选择",
                                tint = if (active) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                            )
                            Column(
                                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                            ) {
                                Text(server.name, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    if (showSavedAddresses) server.address else maskServerAddress(server.address),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (active) {
                                    Text(
                                        "当前使用中",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.padding(top = 2.dp),
                                    )
                                }
                            }
                            TextButton(
                                onClick = {
                                    testingServerId = server.id
                                    scope.launch {
                                        val ok = ApiClient.checkHealth(server.address)
                                        testingServerId = null
                                        toast(if (ok) "连接正常" else "连接失败")
                                    }
                                },
                                enabled = !testingThis,
                            ) {
                                if (testingThis) {
                                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.padding(end = 6.dp).size(16.dp))
                                    Text("测试中")
                                } else {
                                    Text("测试")
                                }
                            }
                            IconButton(onClick = { deletingId = server.id }) {
                                Icon(Icons.Filled.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            if (state.servers.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "尚未配置服务器。\n请填写后端地址，点击“测试连接并添加”。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "运行日志（dev.log）",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "内部存储（完整）：\n${com.iptv.player.data.local.FileLogger.internalPath() ?: "未初始化"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                        Text(
                            "共享存储（无需 root）：\n${com.iptv.player.data.local.FileLogger.externalPath() ?: "未初始化"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Text(
                            "电脑上双击项目根目录的 get-devlog.cmd，可一键把 dev.log 拉取到项目根目录。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                        Text(
                            "日志含启动、服务器切换、播放候选链、断流重连与错误。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
    }

    deletingId?.let { id ->
        AlertDialog(
            onDismissRequest = { deletingId = null },
            title = { Text("删除服务器") },
            text = { Text("确定删除该服务器配置吗？") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { serverRepo.remove(id) }
                    deletingId = null
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { deletingId = null }) { Text("取消") }
            },
        )
    }
}

private fun maskServerAddress(address: String): String {
    val trimmed = address.trim()
    if (trimmed.isBlank()) return "地址已隐藏"
    val scheme = when {
        trimmed.startsWith("https://", ignoreCase = true) -> "https://"
        trimmed.startsWith("http://", ignoreCase = true) -> "http://"
        else -> ""
    }
    return "${scheme}••••••"
}
