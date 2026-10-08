# 随笔 · Suibi

打开就能写、按话题归档、笔记下面能挂讨论的本地记事本。数据在你自己手里，云盘只是备份。

**纯本地存储** · **无账号** · **无联网请求**（除非你自己配 WebDAV）

---

## 它做什么

三层结构：**话题 → 笔记 → 讨论**。

话题是分类，笔记是正文，讨论是挂在笔记下面的一条条补充。讨论独立存储，所以能被单独搜索、计数、删除。

```
工作
├── 周一例会
│   ├── 讨论：补充一点
│   └── 讨论：再补充
└── 项目排期
灵感
└── 一个念头
```

## 功能

**记录**
- 标题 + 正文，边写边自动保存（700ms 防抖），没有保存按钮
- Markdown 工具栏：粗体、斜体、标题、无序/有序列表、引用、行内代码、分隔线
- 正文源码存储，导图时渲染

**附件**
- 笔记和每一条讨论都能挂图片和文件
- 从相册选图或走系统文件选择器，复制进应用私有目录（`filesDir/attachments/`）
- 图片显示为缩略图，点开用系统看图器；其它文件交给能处理它的应用
- 随机文件名存盘，数据库只记相对路径 —— 原文件删了也不影响
- 删笔记/删讨论时附件行随外键级联删除，磁盘文件在下次启动时对账回收
- WebDAV 备份会把附件以 base64 内嵌，换手机恢复不丢图

**组织**
- 话题增删改，笔记归属话题
- 首页「新建」可直接建话题，也可直接建笔记（会先让你选话题）
- 左滑：置顶 / 标记颜色 / 删除
- 标记色 13 个预设 + 自定义 HSV 调色盘，选中后该条以该色淡染底色
- 长按快捷菜单
- 话题前用讨论图标，笔记前用记事本图标

**查找**
- 全库搜索：笔记标题、正文、讨论、话题名一起搜（220ms 防抖）
- 「全部笔记」跨话题视图，按更新时间汇总，可再搜
- 笔记内查找：输入即高亮全部命中，`n/总数` 计数，上下箭头逐处跳转并自动滚到该行

**导出**
- 单篇笔记导出成 1080px 或 1440px 宽的长图，含话题名、标题、时间署名、正文、全部讨论
- 长图里 Markdown 会渲染：标题、列表、引用、粗体、斜体、行内代码、分隔线
- 存相册（`Pictures/随笔`）或直接分享

**外观**
- 8 套内置主题，实时切换：

| 主题 | 来源 |
|---|---|
| 宣纸 / 墨夜 | 内置 |
| Catppuccin Latte / Mocha | [catppuccin](https://github.com/catppuccin/catppuccin) · MIT |
| Nord | [nordtheme](https://github.com/nordtheme/nord) · MIT |
| Gruvbox Dark | [morhetz/gruvbox](https://github.com/morhetz/gruvbox) · MIT |
| Rosé Pine / Dawn | [rose-pine](https://github.com/rose-pine/rose-pine-theme) · MIT |

- 状态栏图标跟随主题（不跟随系统）

**设置**
- 「通用」页：
  - **删除前二次确认** —— 开启后删话题/笔记/讨论要连点两次，两次按钮文案不同，避免连点误删。可关
  - 正文字号（小 / 标准 / 大）
  - 讨论默认展开 / 收起
  - 时间显示用相对（`3 分钟前`）还是绝对（`2026/10/09 14:22`）
  - 导出图片宽度（1080 / 1440）
- 主题页：8 套配色实时预览
- WebDAV 页：地址、账号、应用密码、远程目录、自动备份间隔

**备份**
- WebDAV 全量备份 / 恢复，手动或按间隔自动
- 备份含话题、笔记、讨论、附件文件，附件的二进制以 base64 内嵌，文件是自包含的
- 密码用 Android Keystore AES/GCM 加密后存本地
- 备份格式：gzip 压缩的 JSON（`suibi-backup.json.gz`）

## 技术栈

| | |
|---|---|
| 语言 | Kotlin 2.0.20 |
| UI | Jetpack Compose（BOM 2024.09.00）+ Material 3 |
| 导航 | Navigation Compose 2.8.0 |
| 存储 | Room 2.6.1（SQLite）+ DataStore Preferences |
| 图片 | Coil 2.7.0 |
| 网络 | OkHttp 4.12.0（手写 WebDAV） |
| 构建 | AGP 8.5.2 / Gradle 8.7 / JDK 17 |
| 系统 | minSdk 26（Android 8.0）· targetSdk 34 |

搜索用 `LIKE` + `ESCAPE`，没有用 FTS5 —— 中文在 `unicode61` 分词下不可靠。

## 构建

需要 JDK 17 和 Android SDK（platform 34 + build-tools 34.0.0）。

```bash
# 1. 建 local.properties，指向你的 SDK
echo "sdk.dir=/path/to/android-sdk" > local.properties

# 2. 构建
./gradlew :app:assembleRelease

# 产物
# app/build/outputs/apk/release/app-release.apk
```

Windows 上用 `build.cmd`（会读 `JAVA_HOME` / `ANDROID_HOME` 环境变量）。

### 签名

仓库**不含**发布密钥。`app/build.gradle.kts` 从根目录的 `keystore.properties` 读签名信息：

```properties
storeFile=keystore/suibi.jks
storePassword=你的密码
keyAlias=suibi
keyPassword=你的密码
```

没有这个文件时，release 自动退回 debug 签名 —— 克隆下来照样能构建出可安装的包，只是不能用来覆盖官方版本。

生成自己的密钥：

```bash
keytool -genkeypair -v -keystore keystore/suibi.jks \
  -alias suibi -keyalg RSA -keysize 2048 -validity 10000
```

模板见 `keystore.properties.example`。

## 目录结构

```
app/src/main/java/com/yq/suibi/
├── data/               Room 实体、DAO、数据库、WebDAV、加密、附件存储
├── export/             笔记导出长图（StaticLayout 排版 + 轻量 Markdown 渲染）
├── ui/
│   ├── allnotes/       全部笔记视图
│   ├── common/         通用组件：左滑、调色盘、查找高亮、附件选择与展示
│   ├── editor/         编辑页 + 查找栏 + Markdown 工具栏 + 附件
│   ├── notes/          话题内笔记列表
│   ├── search/         全库搜索
│   ├── settings/       设置 / 通用 / WebDAV / 主题
│   ├── theme/          8 套配色
│   └── topics/         话题列表
└── MainActivity.kt

DESIGN.md               完整设计文档
```

## 版本

| 版本 | 内容 |
|---|---|
| **v1.3** | 笔记与讨论挂附件 · 「通用」设置页 · 删除二次确认 · 直接新建笔记 · 话题/笔记图标 · 备份含附件 |
| **v1.2** | 全部笔记视图 · 笔记内查找高亮 · 导出单篇为图片 |
| **v1.1** | 修内容不加载 · 时间署名 · 左滑菜单 · 标记颜色 · 8 套主题 · 设置分页 · 加速过渡 · 统一圆角 · 线条图标 |
| **v1.0** | 首个可用版本 |

详见 [Releases](../../releases) 与 [DESIGN.md](DESIGN.md)。

## 许可

MIT