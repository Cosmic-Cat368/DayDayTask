# DayDayTask

一款**纯本地**的个人任务管理 App：既管理今天要做的事，也长期归档每天、每周完成过的记录。

- 不联网，不申请网络权限
- 数据保存在手机本地
- 通过手动导出 / 导入 zip 完成备份与换机
- 包名：`com.example.taskrecall`
- 最低支持 Android 8.0（API 26），目标 Android 14（API 34）
- 当前版本：`1.1.0`

---

## 直接安装 App（普通用户）

已经编译好可直接安装的 APK，**不需要 Android Studio，也不需要懂代码**：

**[下载 DayDayTask-debug.apk](dist/DayDayTask-debug.apk)**（约 18 MB，Debug 签名）

### 在安卓手机上安装（4 步）

1. **把 APK 传到手机**
   - 用数据线把 `dist/DayDayTask-debug.apk` 复制到手机“下载”目录；
   - 或者通过微信“文件传输助手”、QQ、网盘发送到手机。
2. **在手机上点击 APK**
   - 打开“文件管理” → “下载”，找到 `DayDayTask-debug.apk` 并点击。
3. **允许安装未知应用**
   - 如果系统提示“禁止安装未知应用”，点击“设置”，允许当前文件管理器 / 微信安装应用，然后返回继续。
4. **安装并打开**
   - 点击“安装” → “打开”。桌面上出现 **DayDayTask** 图标即安装成功。

### 用电脑 + ADB 安装

前提：手机已开启“开发者选项”和“USB 调试”，电脑已安装 ADB。

```powershell
adb devices
adb install -r dist\DayDayTask-debug.apk
```

`-r` 表示覆盖安装。若签名不一致，需要先卸载旧版本；**卸载会清除 App 本地数据**。

### 安装要求与提示

- 手机系统：Android 8.0（API 26）或更高；
- 当前是 Debug 签名版本，部分手机可能提示“未知来源”或安全风险，允许安装即可；
- 没有正式上架应用商店，只能通过 APK 安装；
- 换用正式签名版本前需要卸载 Debug 版，本地数据会丢失，卸载前请先在 App 内导出备份。

---

## 功能一览

### 五种任务类型

| 类型 | 含义 | 时间字段 | 进入“今日区”规则 |
|---|---|---|---|
| 单次任务 `SINGLE` | 一次性任务 | 到期时间 `dueAt`，可精确到分钟 | 到期日在今天 |
| 时限型长期 `LONGTERM_A` | 有明确截止时间的长期任务 | 截止时间 `dueAt` | 仅在截止日当天进入一次，同时常驻长期区 |
| 目标型长期 `LONGTERM_B` | 没有截止时间的目标，如“瘦到 60kg” | 无 | 不进入今日区，只显示在长期区 |
| 日常 `DAILY` | 每天重复 | 起始日 `startAt` | 每天显示当天这一次 |
| 周常 `WEEKLY` | 每周完成一次，周一为周首 | 起始周 `startAt` | 每周显示这一周的一次 |

### 三态状态

点击任务卡片左侧的状态圆点，按以下顺序循环：

```text
PENDING（未完成）
   ↓ 点击
DONE（完成，绿色圆点，卡片变灰）
   ↓ 点击
FAILED（失败，红色圆点，卡片变灰）
   ↓ 点击
PENDING（恢复正常）
```

- 单次任务、时限型长期任务超过截止时间仍未完成时，会**动态显示为红色失败态**；
- 该失败态不会写入数据库，而是每次都根据当前时间重新计算；
- 日常任务次日自动回到未完成；
- 周常任务下周一自动回到未完成；
- 日常 / 周常的历史完成记录会保留。

### 主页

自上而下依次为：

1. 日历：默认今天，可向前、向后翻月，未来日期也可以点击；
2. 标签页（类似网页标签）：
   - **今日任务**：默认标签；
   - **日常**：只显示日常任务；
   - **周常**：只显示周常任务；
   - **长期任务**：只显示长期任务，内部再分时限型和目标型。

“今日任务”标签内包含：

- 今天到期的单次任务（显示小时和分钟）；
- 截止日为今天的时限型长期任务；
- 分界线“近一周要结束的任务”；
- 未来 7 天到期的单次任务（只显示日期，不显示分钟）。

在日历中选中非今天的日期后，标签会自动回到“当天任务”，显示该日期的：

- 当天到期的单次任务 / 时限型长期任务；
- 当天 / 当周的日常、周常记录；
- 该日期视图只读，右侧有“回到今天”按钮。

近一周区可以在设置中永久隐藏；重新打开后，可以点击标题展开 / 收起。

### 拖拽排序

- 今日任务、近一周、日常、周常、长期任务各分区支持手动排序；
- **长按任务卡片**后上下拖动即可调整位置；
- 也可以按住卡片右侧的**三条杠手柄**拖动；
- 只能在任务自己所属的区内移动，不能拖到别的分区；
- 同一任务同时出现在两个区时，两个区的顺序互相独立；
- 排序结果会保存到数据库，重启 App 后仍然有效；
- 导入导出会一并保留排序。

### 日历与历史

- 日历只区分两种颜色：当天有单次任务为正常色，没有为灰色；
- 完成与否、日常、周常都不影响日历着色；
- 未来日期和过去日期都可以点击；
- 点击“2026年10月”这样的月份标题，会打开日期选择器，可快速选择年、月、日；
- 选中日期后显示当天任务和记录；
- 历史记录只读，不会修改过去的状态。

### Markdown 与图片

- 详细说明支持 Markdown；
- 添加页可以调用系统相册插入图片；
- 图片会被复制到 App 私有目录，Markdown 中只保存相对路径，例如：

```markdown
![图片](images/img_xxx.jpg)
```

- 详情页使用 Markwon 渲染，图片由 Coil 从私有目录加载；
- 导出时只把被 Markdown 引用的图片放进 zip，并保持相对路径；
- 换机导入后，图片路径不变，图片仍然可以显示。

### 导入导出

- 导出：生成 `DayDayTask-backup-时间.zip`，通过系统分享保存或发送；
- zip 包含 `tasks.json` 和 `images/`；
- 导入：在设置中选择 zip，或直接在文件管理器 / 微信中点击 zip，选择用 DayDayTask 打开；
- 导入前必须确认“将覆盖当前数据”；
- 确认后覆盖任务、完成记录，并整体替换私有图片目录。

### 设置

- 近一周区显示开关（DataStore 持久化）；
- 导出数据 / 导入数据；
- 关于与版本信息；
- 不申请网络权限，不做云同步。

---

## 使用指南

### 1. 添加任务

- 点击主页右下角“添加任务”；
- 或者先在日历选择日期，再点击添加，日期会自动带入；
- 选择任务类型；
- 填写简写（必填）；
- 按需填写 Markdown 详细说明；
- 选择日期；单次任务和时限型长期任务还可以设置具体时间；
- 点击右上角“保存”。

不设置具体时间时，按照“当天结束前完成”处理，任务不会在当天一开始就显示逾期。

### 2. 切换状态

- 点一下圆点：变成已完成（绿色），卡片变灰；
- 再点一下：变成失败（红色），卡片变灰；
- 再点一下：回到未完成；
- 对逾期的未完成任务，第一次点击会直接标记完成，避免卡在动态红色状态。

### 3. 调整任务顺序

- 长按任务卡片，等卡片轻微放大后上下拖动；
- 或者按住卡片右侧的三条杠手柄拖动；
- 松手后任务会落到新位置；
- 只能在本分区内调整，不能跨区；
- 顺序会保存，重新打开 App 后仍保持。

### 4. 查看详情

- 点击任务卡片进入详情；
- 查看类型、状态、日期、创建时间和 Markdown 内容；
- 右上角可以删除任务；
- 删除是软删除：任务从主页隐藏，但完成记录仍然保留在数据库中。

### 5. 日常与周常

- 日常每天有一个独立状态，次日自动变成未完成；
- 周常每周有一个独立状态，周一自动变成未完成；
- 历史记录会保留，但不参与日历着色。

### 6. 长期任务

- 时限型长期任务在截止日当天会出现在今日任务区，同时一直保留在长期任务区；
- 目标型长期任务只出现在长期任务区，需要手动点击完成。

### 7. 查看历史 / 未来

- 在日历中向前或向后翻月；
- 也可以点击顶部的“2026年10月”直接选择年、月、日；
- 点击任意日期（包括未来日期），查看当天任务和记录；
- 点击“回到今天”恢复今天视图，并回到“今日任务”标签。

---

## 数据导入与导出

### 导出

1. 打开右上角设置；
2. 点击“导出数据”；
3. 等待生成 zip；
4. 系统分享面板会打开，可保存到文件管理器、网盘，或发送到电脑/微信。

压缩包结构：

```text
DayDayTask-backup-yyyyMMdd-HHmmss.zip
├── tasks.json        # 任务 + 完成记录 + 分区排序 + 当前显示状态
└── images/           # 被 Markdown 引用的图片
    ├── img_xxx.jpg
    └── img_yyy.png
```

`tasks.json` 中的图片路径始终是 `images/xxx` 相对路径。

### 导入

#### 方式 A：直接打开 zip

1. 在文件管理器或微信中找到备份 zip；
2. 选择“用其他软件打开”；
3. 选择 **DayDayTask**；
4. App 弹出覆盖确认；
5. 确认后开始导入。

#### 方式 B：设置页导入

1. 打开设置；
2. 点击“导入数据”；
3. 系统文件选择器中选择 zip；
4. 确认覆盖；
5. 等待导入完成。

### 覆盖规则

- 导入是覆盖，不是合并；
- 确认前不会修改任何数据；
- 任务、完成记录、分区排序会被备份内容替换；
- 图片目录会整体替换为本 zip 的 `images/`；
- 导入完成后，旧手机上的 Markdown 图片引用仍然有效。

### 注意事项

- 导入前建议先导出一次当前数据；
- 不要手动修改 zip 内的目录结构；
- 如果文件管理器没有把 zip 关联到 DayDayTask，请使用设置页导入；
- 卸载 App 会清除本地数据库和私有目录中的图片。

---

## 数据存储与隐私

- 所有任务、完成记录和图片都保存于手机本地；
- 不连接服务器，不做云同步；
- 不申请 `INTERNET` 权限；
- 禁止系统云备份和换机自动传输，备份只由用户手动导出；
- 不收集、不上传任何使用数据；
- 卸载 App 会清除本地数据，换机前请先导出 zip。

---

## 编译与安装（开发者）

### 环境要求

| 项目 | 要求 |
|---|---|
| JDK | 17 |
| Android Studio | Ladybug 或更新版本 |
| Android SDK | Platform 34 |
| Build-Tools | 35.0.0（项目已固定） |
| Gradle | 8.9（项目自带 Wrapper） |

### Android Studio

1. 打开项目根目录；
2. 等待 Gradle Sync；
3. 连接 API 26+ 设备或模拟器；
4. 点击 Run ▶。

### 命令行

```powershell
# Windows
$env:JAVA_HOME = "C:\Program Files\Android\OpenJDK\jdk-17"
.\gradlew.bat :app:assembleDebug
```

```bash
# macOS / Linux
export JAVA_HOME=/path/to/jdk-17
./gradlew :app:assembleDebug
```

APK 输出：

```text
app/build/outputs/apk/debug/app-debug.apk
```

开发构建后可把 APK 复制到：

```text
dist/DayDayTask-debug.apk
```

### 测试

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
```

当前共 18 项测试，覆盖：

- 今日任务查询与排序；
- 近一周查询；
- `LONGTERM_A` 今日区 + 长期区双区出现；
- DAILY / WEEKLY 周期键与 ISO 周跨年；
- 逾期动态判定；
- 三态循环；
- 完成记录 upsert；
- 软删除保留完成记录；
- 分区拖拽顺序独立保存；
- 数据库 v1 → v2 迁移并保留旧数据；
- Markdown 相对图片路径；
- 备份 JSON 序列化（含排序）；
- 导出/导入 zip、图片与排序恢复的端到端流程。

---

## 项目结构

```text
app/src/main/java/com/example/taskrecall/
├── data/
│   ├── db/           # TaskEntity、CompletionEntity、DAO、AppDatabase
│   ├── repository/   # TaskRepository、SettingsRepository、ImageStorage
│   └── export/       # ExportManager、ImportManager、BackupModels
├── domain/           # TaskType、TaskStatus、PeriodKey、StatusResolver 等
├── ui/
│   ├── home/         # 主页、日历、今日/近一周/长期、历史记录
│   ├── addtask/      # 添加任务
│   ├── detail/       # Markwon 详情与 Coil 图片
│   ├── settings/     # 设置、导出、导入
│   ├── components/   # 任务卡、状态圆点、日历
│   ├── theme/        # Material 3 主题
│   ├── MainViewModel.kt
│   └── TaskRecallApp.kt
├── di/               # Hilt 模块
├── MainActivity.kt
└── TaskRecallApplication.kt
```

---

## 技术栈

- Kotlin
- Jetpack Compose + Material 3
- Room（SQLite）
- Navigation Compose
- Markwon（Markdown 渲染）
- Coil（图片加载）
- kotlinx-serialization（JSON）
- DataStore（设置持久化）
- `java.util.zip`（导入导出）
- Hilt（依赖注入）
- 单 Activity + MVVM + Repository

> Markwon 官方 `image-coil` 4.6.2 依赖 Coil 0.x，与项目的 Coil 2.x 不兼容。
> 因此详情页使用 Markwon 的 `SchemeHandler` + 自定义 `MediaDecoder`，底层仍由 Coil 2 解码图片。

---

## 截图占位

当前构建环境没有连接真机或模拟器，先保留截图位置；安装到手机后可替换为真实截图：

| 页面 | 截图文件 |
|---|---|
| 主页 | `docs/screenshots/home.png` |
| 添加任务 | `docs/screenshots/add-task.png` |
| 任务详情（Markdown） | `docs/screenshots/detail.png` |
| 历史日历 | `docs/screenshots/calendar.png` |
| 设置 / 导入导出 | `docs/screenshots/settings.png` |

---

## 常见问题

### Q1：安装时提示“应用未安装”或签名冲突？

先卸载手机上的旧版本，再安装新 APK。卸载会删除本地数据，请先导出备份。

### Q2：Gradle Sync 找不到 Build-Tools 35.0.0？

在 Android Studio 的 **SDK Manager → SDK Tools** 中安装 **Android SDK Build-Tools 35.0.0**，或修改 [app/build.gradle.kts](app/build.gradle.kts) 中的 `buildToolsVersion`。

### Q3：提示 `SDK location not found`？

在项目根目录创建 `local.properties`：

```properties
sdk.dir=C:/Users/你的用户名/AppData/Local/Android/Sdk
```

Android Studio 通常会自动生成。

### Q4：App 会联网吗？

不会。没有声明 `INTERNET` 权限，所有数据仅保存在本地。

### Q5：为什么日常/周常第二天/下周变成未完成了？

这是规格设计：完成记录按天 / 按 ISO 周保存，新周期没有记录，所以显示为未完成。历史记录仍然保留在数据库中。

### Q6：换手机怎么迁移？

1. 旧手机打开设置 → 导出数据；
2. 把 zip 传到新手机；
3. 新手机安装 App → 设置 → 导入数据；
4. 选中 zip，确认覆盖；
5. 等待恢复完成，图片会一起恢复。

### Q7：图片为什么没有显示？

- 确认 Markdown 中使用的是 `images/xxx.jpg` 相对路径，而不是绝对路径；
- 确认图片确实随备份 zip 一起导入；
- 手动修改过 zip 目录结构可能导致图片丢失，请重新导出/导入。

---

## 版本

| 项目 | 值 |
|---|---|
| 版本 | 1.1.0 |
| versionCode | 6 |
| 包名 | com.example.taskrecall |
| 最低系统 | Android 8.0 / API 26 |
| 目标系统 | Android 14 / API 34 |

README 会随版本继续更新。
