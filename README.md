# FilesJS

为 KubeJS 服务端脚本提供文件读写、目录操作、备份、压缩和文件变化事件。

## 26.1.2 版本

| 组件 | 版本 |
| --- | --- |
| Minecraft | 26.1.2 |
| NeoForge | 26.1.2.109 或更高的 26.1.2 版本 |
| FilesJS | 1.1.0 |
| KubeJS | 26.1.2-8.0.6 |
| Rhino | 2101.2.8-build.91 |
| Better Advanced Tooltips | 2601.1.0-build.10 |
| Java 工具链 | 25 |
| Gradle | 9.2.1 |
| ModDevGradle | 2.0.147 |

Rhino 的版本号沿用上游的 `2101` 系列；上表版本是 KubeJS 26.1.2-8.0.6 声明的依赖。

## 构建

```powershell
.\gradlew.bat --no-daemon compileJava build
```

Gradle 会从官方 Maven 仓库下载依赖，并使用 Java 25 工具链。构建产物位于 `build/libs/filesjs-1.1.0.jar`。

KubeJS 已内嵌 Tiny Java Server 和 GIF 库，由 NeoForge 的 Jar-in-Jar 加载机制处理，因此开发依赖关闭传递解析，单独声明 KubeJS、Rhino 与运行时所需的 Better Advanced Tooltips。

KubeJS 8.0.6 的公共初始化代码直接使用 Better Advanced Tooltips 的 `BATIcons`，缺少该模组会导致加载失败，因此本版本将其声明为必需依赖。

## 使用

在 Minecraft 26.1.2 的 NeoForge 实例中安装 FilesJS、KubeJS、Rhino 和 Better Advanced Tooltips，将脚本放在 `kubejs/server_scripts`。现有的 `FilesJS` 全局对象及 `Files.*` 事件接口保持不变。

```js
ServerEvents.loaded(event => {
    FilesJS.createFiles('kubejs/filesjs-example.txt', 'Hello from FilesJS!')
    console.info(FilesJS.readFile('kubejs/filesjs-example.txt'))
})
```
