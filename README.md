# SPW Fixed Shuffle Queue Plugin

Fixed Shuffle Queue 是一款为 [Salt Player for Windows (SPW)](https://github.com/Moriafly/SaltPlayerForWindows) 开发的第三方插件。

## 安装与使用

1. 前往本仓库的 **[Releases](https://github.com/AugustuXue/spw-fixed-shuffle-plugin/releases)** 页面。
2. 下载最新版本的 `FixedShuffleQueue-x.x.x-fixed-shuffle.zip` 文件。
3. 打开SPW，选择 `设置`-`创意工坊`-`模组管理`，右上角选择导入模组，并选择模组zip文件进行导入
4. 点击模组，选择启用

---

## 功能介绍

本插件旨在优化和“劫持”原生的随机播放体验。当你在 SPW 主界面点击【随机播放】时：
- 插件会瞬间拦截该操作，将当前的正常播放队列进行**完全打乱**和重组。
- 将打乱后的固定顺序重新写入播放队列，并从第一首开始播放。
- 自动将播放模式切换回【列表循环/顺序播放】，让你可以直接看到完整的随机后顺序，且可以继续使用原生的拖拽排序功能。

---

## 本地手动构建方式

本项目已脱离官方 `spw-workshop-api` 模板独立出来，可直接使用 Gradle 进行构建。

**环境要求**：本机必须安装 JDK 21，并配置好 `JAVA_HOME`。

```bash
cd spw-fixed-shuffle-plugin

# 执行插件打包任务 (Windows)
./gradlew.bat plugin
```

编译完成后，插件 `.zip` 包会自动生成并复制到你的 `%APPDATA%\Salt Player for Windows\workshop\plugins\` 目录下，重启播放器即可生效。

---

## 技术原理

由于 SPW 目前的 Workshop API 尚未暴露播放队列的操作接口，本插件采用了 **反射与状态流劫持** 的方式：
1. 订阅 `PlaybackQueueState` 的 `StateFlow`，仅在队列状态变化时处理。
2. 当检测到播放模式枚举切换为 `Random` 时触发劫持。
3. 使用当前 SPW 的 `com.xuncorp.spc.core.queue` 队列类型读取正常队列和当前索引。
4. 切回顺序模式后，通过宿主 `PlaybackController.moveMediaItem()` 原地重排当前歌曲前后的队列项；不替换当前媒体、不重播，也不重置播放进度。
