# TodayDDL

一款面向高校学生与个人日常的轻量级 Android 截止日期（DDL）管理应用。基于 **Kotlin + Jetpack Compose (Material 3)** 构建，支持**公历与学期周数双向换算**、**状态栏今日 DDL 常驻通知**、**自定义时间精准弹窗提醒**，并针对**低功耗与低内存占用**做了深度优化。

---

## 核心功能

### 1. 首页智能分组与紧迫度分级
- **顶部大字日期栏**：直观展示当前公历日期、学期周数、星期与未完成 DDL 总数。
- **自动分组与排序**：按「已逾期 / 今天 / 即将到来 / 已完成（可折叠）」自动归类，同一分组内按截止时间先后排序。
- **多级紧迫度视觉区分**：根据剩余时间（已逾期、今日/24 小时内、3 天内、常规、已完成）自动匹配边框与倒计时徽章配色，支持亮色 / 暗色主题跟随系统。
- **分类标签筛选**：顶部横向标签栏支持按课程或任务类型一键筛选。

### 2. 公历 / 学期周数双模式录入
- **双模式切换**：新建或编辑 DDL 时，可在「公历日期」与「学期周数（第 1–30 周 + 周一至周日）」之间自由切换，并实时双向预览对应日期。
- **全天 / 精确时间模式**：
  - 全天任务在截止日结束（次日 `00:00`）后才判定为逾期。
  - 精确时间任务支持 24 小时制设定到分钟。
- **学期起始日自动对齐**：在设置页配置「第 1 周周一日期」时，选择任意日期都会自动对齐到该周周一。

### 3. 定时弹窗提醒（支持自定义时间与实时预览）
- **丰富的提醒策略**：
  - 精确时间模式：不提醒、截止准点、提前 15 分钟、提前 30 分钟、提前 1 小时、提前 3 小时、提前 1 天、**自定义时间**。
  - 全天模式：不提醒、当天 08:00、当天 12:00、前一天 20:00、**自定义时间**。
- **自定义提醒时间**：支持自由选择任意提醒日期与 24 小时制时间；首次选择时自动推荐合理的未来提醒时间。
- **预计提醒时间实时预览**：编辑页底部实时展示准确触发时间（含学期周数）与相对倒计时（如「约 30 分钟后弹出提醒」）；若所选提醒时间已过或系统通知权限未开启，将即时标红提示。
- **高可靠准点触发**：优先使用免受 Doze 休眠限制的 `AlarmManager.setAlarmClock()` 调度提醒，并监听重启、更新、时间/时区及权限变化事件自动恢复所有未完成任务的闹钟。

### 4. 状态栏今日 DDL 常驻通知
- 在通知栏静默常驻展示当日待完成的 DDL 列表（支持多行 `InboxStyle` 展开预览），点击通知直达首页。
- 支持配置「今日无任务时隐藏常驻通知」。
- 设置页内置「权限与常驻状态自检」（通知权限、精确闹钟权限、电池优化白名单），支持一键刷新常驻通知与发送测试提醒。

### 5. 分类 / 课程标签管理
- 首次启动自动预置常用标签（作业、实验报告、考试、项目）。
- 支持自定义新增、重命名、配色与删除标签（删除标签仅解除关联，不删除已创建的 DDL）。

---

## 省电与内存优化设计

- **按需精确前台刷新**：首页倒计时不再每分钟无条件轮询，而是精确计算下一次文案/日期跳变时刻（分钟级 / 小时级 / 次日零点）再唤醒刷新，大幅减少前台 CPU 唤醒与 Compose 重组。
- **非唤醒零点刷新**：跨日刷新使用非唤醒 `AlarmManager.RTC` 闹钟并做当日去重，夜间不打断系统深度休眠（Doze）。
- **低 I/O 与签名去重**：
  - Room 按当天条件精准查询常驻通知任务，避免全表加载。
  - 常驻通知维护内容签名与在线状态检测，内容未变化时跳过重复 Binder IPC；通知渠道与冷启动初始化均为幂等缓存。
  - 广播接收器统一复用应用协程作用域并设置超时兜底，及时释放 `goAsync()` 系统锁。
- **极致体积与内存瘦身**：
  - 移除体积庞大的 `material-icons-extended` 依赖，仅使用 Material 3 内置核心图标与 4 个懒加载自定义矢量图标（`AppIcons`）。
  - 核心数据类与 UI 状态均标注 `@Immutable`，复用排序比较器单例并缓存卡片日期格式化结果。
  - Release 包开启 R8 代码压缩、资源裁剪与 `zh`/`en` 语言过滤。

---

## 技术栈

| 类别 | 技术选型 |
| --- | --- |
| 语言 | Kotlin 2.4.0（JDK 17） |
| UI 框架 | Jetpack Compose + Material 3（Compose BOM `2025.05.00`） |
| 导航 | Navigation Compose `2.9.8` |
| 本地存储 | Room `2.8.5`（含无损迁移 `MIGRATION_1_2`）+ DataStore Preferences `1.1.7` |
| 异步与状态 | Kotlin Coroutines & Flow + Lifecycle ViewModel |
| 构建工具 | Android Gradle Plugin `9.5.2` / Gradle `9.6.0` / KSP `2.3.14` |
| SDK 版本 | `minSdk = 26`（Android 8.0+） / `targetSdk = 35` / `compileSdk = 37` |

---

## 项目结构

```text
app/src/main/java/com/todayddl/app/
├── MainActivity.kt                     # 单 Activity 入口与边缘到边缘适配
├── TodayDdlApplication.kt              # 懒加载 Application 与共享协程域
├── di/
│   └── AppContainer.kt                 # 轻量级手动依赖注入容器
├── data/
│   ├── local/                          # Room 数据库、DAO、Task/Tag 实体与关联模型
│   ├── preferences/                    # DataStore 偏好设置仓库
│   └── repository/                     # DdlRepository 统一数据与通知调度入口
├── domain/
│   ├── ReminderOption.kt               # 提醒策略枚举（含自定义时间）
│   ├── ReminderCalculator.kt           # 提醒时间计算、预览与默认时间推荐
│   ├── SemesterDateCalculator.kt       # 公历与学期周数双向换算与格式化
│   └── TaskStatusCalculator.kt         # 分组排序、紧迫度判定与按需刷新延时计算
├── notification/
│   ├── NotificationChannels.kt         # 常驻与高优先级提醒渠道管理
│   ├── OngoingNotificationHelper.kt    # 状态栏常驻通知构建与签名去重
│   ├── ReminderScheduler.kt            # 定时弹窗提醒调度与发送
│   ├── MidnightRefreshScheduler.kt     # 跨日非唤醒刷新调度
│   └── *Receiver.kt                    # 闹钟触发、通知划掉与系统事件恢复广播
└── ui/
    ├── home/                           # 首页列表与筛选界面
    ├── editor/                         # 任务新建 / 编辑、自定义提醒与预览界面
    ├── settings/                       # 学期配置、标签管理与权限自检界面
    ├── navigation/                     # Compose 导航图
    ├── theme/                          # Material 3 主题、色彩、字号与轻量图标
    └── viewmodel/                      # Home / TaskEditor / Settings ViewModel
```

---

## 快速构建与测试

### 环境要求
- Android Studio（支持 AGP 9.5+）或命令行环境
- JDK 17+（项目默认通过 `gradle.properties` 配置 `org.gradle.java.home`，如在本机不同路径请先按需调整）

### 常用命令

```bash
# 编译 Debug APK
./gradlew :app:assembleDebug

# 编译 Release APK（开启 R8 混淆与资源裁剪）
./gradlew :app:assembleRelease

# 运行本地单元测试（覆盖日期换算、状态分组、按需刷新延时与提醒计算）
./gradlew :app:testDebugUnitTest
```

> Windows 环境下请将 `./gradlew` 替换为 `gradlew.bat`。

---

## 权限与手机端设置建议

应用仅申请完成核心提醒功能所需的最小权限集合：
- `POST_NOTIFICATIONS`：展示今日常驻通知与 DDL 截止提醒。
- `USE_EXACT_ALARM` / `SCHEDULE_EXACT_ALARM`：确保定时弹窗提醒在设定时间准点触发。
- `RECEIVE_BOOT_COMPLETED`：设备重启后自动恢复尚未到点的提醒闹钟。
- `VIBRATE`：提醒到达时提供震动反馈。

**国产定制系统（澎湃 OS / MIUI、ColorOS、OriginOS、HarmonyOS 等）使用建议：**
1. 如提醒到达时仅有声音未见顶部横幅，请在系统通知管理中开启「**DDL 截止提醒**」渠道的**悬浮 / 横幅通知**。
2. 如希望从最近任务清理后台后仍能稳定收到提醒与常驻通知，建议在系统应用设置中开启**允许自启动 / 后台运行**，并在 App 设置页中将应用加入**电池优化白名单**。
