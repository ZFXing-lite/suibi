富文本渲染核心 —— 验证记录
================================

时间：2026-05-16
状态：逻辑层全部通过（49/49），UI 接线未完成

## 做了什么

新建 `ui/common/RichText.kt`（376 行）：
- `RichTextTransform : VisualTransformation` —— 把 markdown 标记从显示里拿掉，
  换成真样式（粗体/斜体/等宽/标题放大/引用变色/列表点）
- `parse()` 按行拆分成 `Piece` 片段，每段记录 (原文区间, 显示文本, 样式)
- 正向映射 `rawToDisplay` + 反向映射 `displayToRaw`（左逆，保证单调）
- 光标所在行不隐藏标记（Typora 式 live preview），否则没法编辑标记本身
- `lineRangeOf(text, offset)` 求光标所在行

## 怎么验证的

Android 的 `testReleaseUnitTest` 在这个环境跑不起来：
- 项目路径含中文（`D:\DSH\DIY程序`），aapt2 处理 `android.jar` 时报
  `Failed to stat file`，debug 变体的 `checkDebugAarMetadata` 直接失败
- release 变体勉强能编译，但测试 worker 报
  `ClassNotFoundException: RichTextTransformTest`，class 文件明明在
  `build/tmp/kotlin-classes/releaseUnitTest/` 里

绕开 Android 测试框架，用纯 JVM 跑：
1. 从 Gradle 缓存抽 `ui-text` / `ui-graphics` / `ui-unit` / `ui-geometry` /
   `ui-util` / `runtime` / `runtime-saveable` 的 classes.jar
2. 用 `kotlin-compiler-embeddable-2.0.20.jar` 直接编译
   `RichText.kt` + 手写的 `Main.kt`（49 条断言，覆盖同样的用例）
3. `java -cp out;jars MainKt` 运行

编译器 classpath 需要 56 个 jar（含 trove4j、coroutines、reflect），
不然 `K2JVMCompiler` 起不来。

## 结果

```
通过 49，失败 0
```

覆盖：
- 基本渲染：粗体/斜体/行内代码/标题/无序列表/引用/分隔线/有序列表
- 光标行显露标记，其他行照旧隐藏
- 显露时映射恒等
- 隐藏标记后长度差正确
- 正向映射单调不减、反向映射单调不减
- 两端对齐（原文 0↔显示 0，原文末↔显示末）
- 空文本、半截标记、未闭合标记、下划线不误判、三星号无残留
- 越界偏移被夹住不抛异常
- 样式真的贴上了（Bold 覆盖正确区间、搜索底色贴到正确显示位置）
- `lineRangeOf` 各种边界

## 修掉的两个 bug

1. **反向映射在整段隐藏时是死代码**
   `for (j in 0 until m)` 在 `m == 0` 时不执行，导致显示位置 0 映射到
   原文偏移 2（「粗」的位置）而不是 0。光标放文本开头会漂进标记里。
   改成取正向映射的左逆。

2. **标题行里的行内标记没被处理**
   标题分支直接把整行塞成一个纯文本片段，没调 `inline()`，
   所以 `# **粗体**` 里的 `**` 原样留着。
   改成标题也走 `inline()`，并把 `fontSize` 加进 `buildSpan` 的继承链
   （否则标题里的粗体会掉回正文大小）。

## 还没做的

- `EditorScreen.kt` 还没接上 `RichTextTransform`（仍用旧的 `HighlightTransform`）
- 正文编辑区还没加高加宽
- `MarkdownToolbar` 还没有选中态
- `Md.wrap` 还不是 toggle 语义
- 版本号没升到 1.5
- 没构建 APK、没推送、没发 release