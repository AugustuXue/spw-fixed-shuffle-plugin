# SPW Fixed Shuffle Queue Plugin

Fixed Shuffle Queue 是一款为 [Salt Player for Windows (SPW)](https://github.com/Moriafly/SaltPlayerForWindows) 开发的插件。

## 功能介绍
本插件旨在优化和“劫持”原生的随机播放体验。当你在 SPW 主界面点击【随机播放】时：
- 插件会瞬间拦截该操作，将当前的正常播放队列进行**完全洗牌**和重组。
- 将洗牌后的固定顺序重新写入播放队列，并从第一首开始播放。
- 自动将播放模式切换回【列表循环/顺序播放】，让你可以直接看到完整的随机后顺序，且可以继续使用原生的拖拽排序功能。

**无需任何设置，界面无感交互！**

## 构建方法
本项目已脱离 `spw-workshop-api` 模板独立出来，可直接使用 Gradle 构建。

**环境要求**：本机必须安装 JDK 21。

```bash
# 执行插件打包任务
./gradlew plugin
```

编译完成后，插件 `.zip` 包会自动复制到 `%APPDATA%\Salt Player for Windows\workshop\plugins\` 目录下。

## 技术原理
由于 SPW 目前的 Workshop API 尚未暴露播放队列的操作接口，本插件采用了 **反射** 的方式：
1. 启动后台线程，轮询监控 `PlaybackQueueState` (混淆为 `androidx.compose.ui.ne`) 的 StateFlow。
2. 解析混淆的队列属性 (`Ԩ()`, `Ϳ()`) 和底层的 `PiscesMediaItem`。
3. 通过反射 `PlaybackController.INSTANCE.setPlaybackQueue()` 进行队列替换和模式切换。

## 致谢
感谢 SPW 开发者提供的强大播放器及 Workshop API。