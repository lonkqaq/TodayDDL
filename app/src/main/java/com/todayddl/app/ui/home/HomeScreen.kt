package com.todayddl.app.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.todayddl.app.data.local.entity.TagEntity
import com.todayddl.app.data.local.entity.TaskEntity
import com.todayddl.app.domain.HomeTaskItem
import com.todayddl.app.domain.SemesterDateCalculator
import com.todayddl.app.domain.UrgencyLevel
import com.todayddl.app.ui.theme.LocalUrgencyPalette
import com.todayddl.app.ui.viewmodel.AppViewModelProvider
import com.todayddl.app.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.delay
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddTaskClick: () -> Unit,
    onEditTaskClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }
    var permissionRequestedOnce by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
        if (granted) {
            viewModel.refreshOngoingNotification()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !hasNotificationPermission &&
            !permissionRequestedOnce
        ) {
            permissionRequestedOnce = true
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Refresh clock at the start of every minute while visible in foreground
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                viewModel.refreshClock()
                val nowMillis = System.currentTimeMillis()
                val millisUntilNextMinute = 60_000L - (nowMillis % 60_000L) + 100L
                delay(millisUntilNextMinute)
            }
        }
    }

    // Refresh clock, ongoing notification, and permission state on every ON_RESUME
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    hasNotificationPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                }
                viewModel.refreshClock()
                viewModel.refreshOngoingNotification()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = {
                    Column {
                        Text(
                            text = uiState.headerDateText,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val activeCount = uiState.groupedTasks.overdue.size +
                            uiState.groupedTasks.today.size +
                            uiState.groupedTasks.upcoming.size
                        Text(
                            text = if (activeCount == 0) "暂无待办 DDL" else "共 $activeCount 项未完成 DDL",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "设置"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTaskClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "新建 DDL"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!hasNotificationPermission) {
                NotificationPermissionHintBanner(
                    onGoToSettings = onSettingsClick
                )
            }

            TagFilterRow(
                tags = uiState.tags,
                selectedTagId = uiState.selectedTagId,
                onSelectTag = viewModel::selectTag
            )

            if (uiState.groupedTasks.isAllEmpty) {
                EmptyTasksPlaceholder(
                    hasFilter = uiState.selectedTagId != null,
                    modifier = Modifier.weight(1f)
                )
            } else {
                val overdueHeaderColor = MaterialTheme.colorScheme.error
                val todayHeaderColor = MaterialTheme.colorScheme.primary
                val upcomingHeaderColor = MaterialTheme.colorScheme.onSurfaceVariant
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    taskGroupSection(
                        title = "已逾期",
                        items = uiState.groupedTasks.overdue,
                        headerColor = overdueHeaderColor,
                        semesterStartMonday = uiState.semesterStartMonday,
                        referenceYear = uiState.referenceYear,
                        onToggleCompletion = viewModel::toggleTaskCompletion,
                        onEditClick = onEditTaskClick
                    )

                    taskGroupSection(
                        title = "今天",
                        items = uiState.groupedTasks.today,
                        headerColor = todayHeaderColor,
                        semesterStartMonday = uiState.semesterStartMonday,
                        referenceYear = uiState.referenceYear,
                        onToggleCompletion = viewModel::toggleTaskCompletion,
                        onEditClick = onEditTaskClick
                    )

                    taskGroupSection(
                        title = "即将到来",
                        items = uiState.groupedTasks.upcoming,
                        headerColor = upcomingHeaderColor,
                        semesterStartMonday = uiState.semesterStartMonday,
                        referenceYear = uiState.referenceYear,
                        onToggleCompletion = viewModel::toggleTaskCompletion,
                        onEditClick = onEditTaskClick
                    )

                    val completedItems = uiState.groupedTasks.completed
                    if (completedItems.isNotEmpty()) {
                        item(key = "completed_header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { viewModel.toggleCompletedSection() }
                                    .padding(horizontal = 4.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "已完成 (${completedItems.size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Icon(
                                    imageVector = if (uiState.isCompletedExpanded) {
                                        Icons.Outlined.ExpandLess
                                    } else {
                                        Icons.Outlined.ExpandMore
                                    },
                                    contentDescription = if (uiState.isCompletedExpanded) "收起已完成" else "展开已完成",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (uiState.isCompletedExpanded) {
                            items(
                                items = completedItems,
                                key = { "task_${it.taskWithTag.task.id}" }
                            ) { item ->
                                val task = item.taskWithTag.task
                                AnimatedVisibility(visible = true) {
                                    TaskCard(
                                        item = item,
                                        semesterStartMonday = uiState.semesterStartMonday,
                                        referenceYear = uiState.referenceYear,
                                        onToggleCompletion = { viewModel.toggleTaskCompletion(task) },
                                        onClick = { onEditTaskClick(task.id) }
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

private fun LazyListScope.taskGroupSection(
    title: String,
    items: List<HomeTaskItem>,
    headerColor: Color,
    semesterStartMonday: LocalDate,
    referenceYear: Int,
    onToggleCompletion: (TaskEntity) -> Unit,
    onEditClick: (Long) -> Unit
) {
    if (items.isEmpty()) return

    item(key = "section_$title") {
        Text(
            text = "$title (${items.size})",
            style = MaterialTheme.typography.labelMedium,
            color = headerColor,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp, start = 4.dp)
        )
    }

    items(
        items = items,
        key = { "task_${it.taskWithTag.task.id}" }
    ) { item ->
        val task = item.taskWithTag.task
        TaskCard(
            item = item,
            semesterStartMonday = semesterStartMonday,
            referenceYear = referenceYear,
            onToggleCompletion = { onToggleCompletion(task) },
            onClick = { onEditClick(task.id) }
        )
    }
}

@Composable
private fun NotificationPermissionHintBanner(
    onGoToSettings: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.NotificationsOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "未开启通知权限，无法在状态栏常驻显示今日 DDL",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onGoToSettings) {
                Text("去开启")
            }
        }
    }
}

@Composable
private fun TagFilterRow(
    tags: List<TagEntity>,
    selectedTagId: Long?,
    onSelectTag: (Long?) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "tag_all") {
            FilterChip(
                selected = selectedTagId == null,
                onClick = { onSelectTag(null) },
                label = { Text("全部") }
            )
        }
        items(items = tags, key = { "filter_tag_${it.id}" }) { tag ->
            val selected = selectedTagId == tag.id
            FilterChip(
                selected = selected,
                onClick = {
                    onSelectTag(if (selected) null else tag.id)
                },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(tag.colorHex))
                    )
                },
                label = { Text(tag.name) }
            )
        }
    }
}

@Composable
private fun TaskCard(
    item: HomeTaskItem,
    semesterStartMonday: LocalDate,
    referenceYear: Int,
    onToggleCompletion: () -> Unit,
    onClick: () -> Unit
) {
    val task = item.taskWithTag.task
    val tag = item.taskWithTag.tag
    val status = item.status
    val urgencyPalette = LocalUrgencyPalette.current

    val cardBorder: BorderStroke? = when (status.urgencyLevel) {
        UrgencyLevel.OVERDUE_RED -> BorderStroke(1.2.dp, urgencyPalette.overdueBorder)
        UrgencyLevel.URGENT_ORANGE -> BorderStroke(1.dp, urgencyPalette.urgentBorder)
        else -> BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    }

    val cardContainerColor = when (status.urgencyLevel) {
        UrgencyLevel.OVERDUE_RED -> urgencyPalette.overdueContainer.copy(alpha = 0.35f)
        UrgencyLevel.URGENT_ORANGE -> urgencyPalette.urgentContainer.copy(alpha = 0.25f)
        else -> MaterialTheme.colorScheme.surface
    }

    val badgeContainerColor = when (status.urgencyLevel) {
        UrgencyLevel.OVERDUE_RED -> urgencyPalette.overdueContainer
        UrgencyLevel.URGENT_ORANGE -> urgencyPalette.urgentContainer
        UrgencyLevel.SOON_AMBER -> urgencyPalette.soonContainer
        UrgencyLevel.NORMAL_NEUTRAL, UrgencyLevel.COMPLETED_MUTED -> urgencyPalette.neutralContainer
    }

    val badgeContentColor = when (status.urgencyLevel) {
        UrgencyLevel.OVERDUE_RED -> urgencyPalette.overdueContent
        UrgencyLevel.URGENT_ORANGE -> urgencyPalette.urgentContent
        UrgencyLevel.SOON_AMBER -> urgencyPalette.soonContent
        UrgencyLevel.NORMAL_NEUTRAL, UrgencyLevel.COMPLETED_MUTED -> urgencyPalette.neutralContent
    }

    val dueDate = LocalDate.ofEpochDay(task.dueDateEpochDay)
    val scheduleText = SemesterDateCalculator.formatTaskDueSchedule(
        dueDate = dueDate,
        dueTimeMinutes = task.dueTimeMinutes,
        semesterStartMonday = semesterStartMonday,
        referenceYear = referenceYear
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (task.isCompleted) 0.65f else 1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor),
        border = cardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggleCompletion() }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (tag != null) {
                        val tagColor = Color(tag.colorHex)
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = tagColor.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = tag.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = tagColor,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = scheduleText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = badgeContainerColor
            ) {
                Text(
                    text = status.countdownText,
                    style = MaterialTheme.typography.labelMedium,
                    color = badgeContentColor,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyTasksPlaceholder(
    hasFilter: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircleOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (hasFilter) "当前标签下暂无任务" else "暂无 DDL 任务",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = if (hasFilter) "尝试切换「全部」或新增任务" else "点击右下角 + 添加一条截止日期",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
