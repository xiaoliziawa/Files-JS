# FilesJS - KubeJS 文件管理附属模组

FilesJS（文件吉斯）是一个功能强大的 KubeJS 6 附属模组，用来偷走你的失谐账号和钱包。

## 功能

- 文件操作：读取、写入、追加、复制、移动和删除文件
- 目录管理：列出、创建和监视目录
- 文件监控：监视文件变更和内容修改
- 文件分析：比较文件、计算MD5哈希值、搜索文件内容
- 备份管理：创建和管理文件备份
- 归档操作：创建ZIP压缩包

## API 部分参考（不全面）

### 文件读写操作

```javascript

// 读取文件内容
let content = FilesJS.readFile('kubejs/config/myconfig.txt');

// 写入文件
FilesJS.writeFile('kubejs/data/output.txt', '你好，世界！');

// 追加到文件
FilesJS.appendFile('kubejs/logs/mylog.txt', '新的日志条目');

// 追加单行
FilesJS.appendLine('kubejs/logs/mylog.txt', '新的一行');

// 写入多行
FilesJS.writeLines('kubejs/data/lines.txt', ['第一行', '第二行', '第三行']);

// 读取所有行
let lines = FilesJS.readLines('kubejs/data/lines.txt');

// 读取最后N行
let lastLines = FilesJS.readLastLines('kubejs/logs/latest.log', 10);

// 保存脚本文件（自动添加.js扩展名和时间戳注释）
FilesJS.saveScript('kubejs/scripts/newscript', 'console.log("Hello");');
```

### 文件管理操作

```javascript
// 检查文件是否存在
if (FilesJS.exists('kubejs/scripts/myscript.js')) {
    // 执行操作
}

// 删除文件
FilesJS.delete('kubejs/temp/oldfile.txt');

// 复制文件
FilesJS.copy('source.txt', 'target.txt');

// 移动文件
FilesJS.move('old/path.txt', 'new/path.txt');

// 重命名文件
FilesJS.renameFile('oldname.txt', 'newname.txt');

// 创建目录
FilesJS.createDirectory('kubejs/newdir');

// 检查文件是否为空
let isEmpty = FilesJS.isFileEmpty('kubejs/data/file.txt');

// 获取文件MD5哈希值
let hash = FilesJS.getFileMD5('kubejs/data/important.dat');

// 比较两个文件
let areEqual = FilesJS.compareFiles('file1.txt', 'file2.txt');
```

### 批量文件操作

```javascript
// 合并多个文件
FilesJS.mergeFiles(['file1.txt', 'file2.txt'], 'merged.txt');

// 批量复制文件（使用通配符）
FilesJS.copyFiles('source/dir', 'target/dir', '*.json');

// 创建ZIP压缩包
FilesJS.createZip('kubejs/data', 'kubejs/backups/data.zip');

// 替换文件中的内容
FilesJS.replaceInFile('config.txt', '旧值', '新值');
```

### 目录操作

```javascript
// 列出目录中的文件
let files = FilesJS.listFiles('kubejs/data');

// 递归列出所有文件
let allFiles = FilesJS.listFilesRecursively('kubejs/scripts');

// 获取文件信息
let fileInfo = FilesJS.getFileInfo('kubejs/config/settings.json');
// fileInfo包含：exists, size, lastModified, isDirectory, isFile, isReadable, isWritable
```

### 文件监控

```javascript
// 监视目录变更
FilesJS.watchDirectory('kubejs/data', (changedPath) => {
    console.log('文件已更改:', changedPath);
});

// 监视文件内容变更（带相似度阈值）
FilesJS.watchContentChanges('kubejs/config/dynamic.json', 0.1);

// 监视匹配特定模式的文件
FilesJS.watchFilePattern('kubejs/scripts', '*.js');

// 监视文件大小
FilesJS.watchFileSize('kubejs/data/growing.log', 1024 * 1024); // 1MB阈值
```

### 备份系统

```javascript
// 立即创建备份
FilesJS.backupFile('kubejs/important/data.json');

// 计划备份（延迟执行tick）
FilesJS.scheduleBackup('kubejs/config/settings.json', 100); // 100 tick后开始备份这个文件

// 搜索文件内容
let matches = FilesJS.searchInFile('kubejs/logs/latest.log', 'ERROR');
```

### 文件事件系统

```javascript
// 文件创建事件
Files.fileCreated(event => {
    console.log('文件已创建:', event.getPath());
    console.log('文件内容:', event.getContent());
    console.log('服务器:', event.getServer());
    console.log('世界:', event.getLevel());
    console.log('玩家:', event.getPlayer());
});

// 文件修改事件
Files.fileChanged(event => {
    console.log('文件已更改:', event.getPath());
});

// 文件删除事件
Files.fileDeleted(event => {
    console.log('文件已删除:', event.getPath());
});

// 文件移动事件
Files.fileMoved(event => {
    console.log('文件已移动:', event.getPath());
});

// 文件复制事件
Files.fileCopied(event => {
    console.log('文件已复制:', event.getPath());
});

// 文件重命名事件
Files.fileRenamed(event => {
    console.log('文件已重命名:', event.getPath());
});

// 目录事件
Files.directoryCreated(event => {
    console.log('目录已创建:', event.getPath());
});

Files.directoryDeleted(event => {
    console.log('目录已删除:', event.getPath());
});

// 文件访问事件
Files.fileAccessDenied(event => {
    console.log('访问被拒绝:', event.getPath());
});

// 文件备份事件
Files.fileBackupScheduled(event => {
    console.log('备份已计划:', event.getPath());
});

// 文件模式匹配事件
Files.filePatternMatched(event => {
    console.log('文件匹配模式:', event.getPath());
});

// 文件大小阈值事件
Files.fileSizeThreshold(event => {
    console.log('文件超过大小阈值:', event.getPath());
});

// 文件内容显著变化事件
Files.fileContentChangedSignificantly(event => {
    console.log('文件内容显著变化:', event.getPath());
});
```

## 安全性和限制

### 允许访问的目录
只能访问以下目录中的文件：
- kubejs/
- config/
- logs/
- backups/
- scripts/

### 文件大小限制
- 最大文件大小：5MB
- 写入操作的内容大小限制：5MB

### 文件类型限制
允许的文件扩展名：
- .txt
- .json
- .js
- .log
- .cfg
- .toml
- .properties
- .backup
- 。。。还有很多

### 安全措施
- 禁止访问Minecraft实例目录之外的文件
- 禁止父目录遍历 (..)
- 关键操作自动创建备份
- 所有操作都进行文件访问验证
- 文件大小限制检查
- 文件类型验证
- 路径安全性检查
- 文件拓展名验证

## 错误处理

使用`try` `catch`进行异常处理

```javascript
try {
    Files.writeFile('kubejs/data/test.txt', '内容');
} catch (e) {
    console.error('写入文件失败:', e.message);
}
```

## 最佳实践

1. 始终在脚本中处理异常
2. 使用适当的文件扩展名
3. 在操作前检查文件大小
4. 及时清理临时文件
5. 对重要文件使用备份功能
6. 谨慎监控文件变更
7. 写入前验证文件内容
8. 使用事件系统进行日志记录
9. 实现适当的错误恢复机制
10. 定期清理旧的备份文件

## 性能考虑

1. 大文件操作时注意内存使用
2. 避免频繁的文件监控
3. 合理使用递归文件操作
4. 适当设置监控阈值
5. 使用批量操作代替单个操作

## 支持

如果遇到问题或有疑问：
1. 检查日志中的详细错误信息
2. 验证文件权限和路径
3. 确保在允许的目录中操作
4. 检查文件大小限制
5. 查看事件监听器的日志输出

## 许可证

本项目采用 MIT 许可证 - 详见 LICENSE 文件

## 问题

1. 如果遇到无法访问路径或文件的，说明是做了限制。
2. 文件路径只能在Minecraft的实例路径开始访问，超过实例路径的不得访问。
3. URI无法访问。
4. 文件拓展名做了限制，可能导致有些拓展名的文件无法访问。
