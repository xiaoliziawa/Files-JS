package net.prizowo.filejs.kubejs;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.prizowo.filejs.FilesJSPlugin;
import net.prizowo.filejs.Filesjs;
import net.prizowo.filejs.security.FileAccessManager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Consumer;
import java.security.MessageDigest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.Timer;
import java.util.TimerTask;

public class FilesWrapper {
    private void validateFileOperation(String path) throws IOException {
        try {
            FileAccessManager.validateFileAccess(path);
            FileAccessManager.validateFileSize(Paths.get(path));
        } catch (SecurityException e) {
            FileAccessManager.logSecurityViolation(e.getMessage(), path);
            throw e;
        }
    }

    public String readFile(String path) {
        try {
            FileAccessManager.validateFileAccess(path);
            validateFileOperation(path);
            return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Filesjs.LOGGER.error("Read file error: " + path, e);
            throw new RuntimeException("Fail to read file: " + path, e);
        }
    }

    public List<String> readLines(String path) throws IOException {
        return Files.readAllLines(Paths.get(path), StandardCharsets.UTF_8);
    }

    public void writeFile(String path, String content) {
        try {
            FileAccessManager.validateFileAccess(path);
            
            if (content.length() > 1024 * 1024 * 5) { // 5MB
                throw new SecurityException("Content size exceeds limit (max 5MB)");
            }
            
            Path filePath = Paths.get(path);
            boolean isNewFile = !Files.exists(filePath);
            
            Files.write(filePath, content.getBytes(StandardCharsets.UTF_8));
            
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            ServerLevel level = server.overworld();
            
            if (isNewFile) {
                FilesJSPlugin.FILE_CREATED.post(new FileEventJS(path, content, "created", null, server, level));
            } else {
                FilesJSPlugin.FILE_CHANGED.post(new FileEventJS(path, content, "changed", null, server, level));
            }
        } catch (IOException e) {
            Filesjs.LOGGER.error("Error writing file: " + path, e);
            throw new RuntimeException("Failed to write file: " + path, e);
        }
    }

    public void writeLines(String path, List<String> lines) {
        try {
            FileAccessManager.validateFileAccess(path);
            Files.write(Paths.get(path), lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            Filesjs.LOGGER.error("Error writing lines to file: " + path, e);
            throw new RuntimeException("Failed to write lines to file: " + path, e);
        }
    }

    public void appendFile(String path, String content) {
        try {
            FileAccessManager.validateFileAccess(path);
            Files.write(Paths.get(path), content.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        } catch (IOException e) {
            Filesjs.LOGGER.error("Error appending to file: " + path, e);
            throw new RuntimeException("Failed to append to file: " + path, e);
        }
    }

    public boolean exists(String path) {
        try {
            FileAccessManager.validateFileAccess(path);
            return Files.exists(Paths.get(path));
        } catch (SecurityException e) {
            return false;
        }
    }

    public void createDirectory(String path) {
        try {
            FileAccessManager.validateFileAccess(path);
            Files.createDirectories(Paths.get(path));
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            ServerLevel level = server.overworld();
            FilesJSPlugin.DIRECTORY_CREATED.post(new FileEventJS(path, null, "directory_created", null, server, level));
        } catch (IOException e) {
            Filesjs.LOGGER.error("Error creating directory: " + path, e);
            throw new RuntimeException("Failed to create directory: " + path, e);
        }
    }

    public void delete(String path) {
        try {
            FileAccessManager.validateFileAccess(path);
            boolean isDirectory = Files.isDirectory(Paths.get(path));
            Files.delete(Paths.get(path));
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            ServerLevel level = server.overworld();
            if (isDirectory) {
                FilesJSPlugin.DIRECTORY_DELETED.post(new FileEventJS(path, null, "directory_deleted", null, server, level));
            } else {
                FilesJSPlugin.FILE_DELETED.post(new FileEventJS(path, null, "deleted", null, server, level));
            }
        } catch (IOException e) {
            Filesjs.LOGGER.error("Error deleting file: " + path, e);
            throw new RuntimeException("Failed to delete file: " + path, e);
        }
    }

    public List<String> listFiles(String path) {
        try {
            FileAccessManager.validateFileAccess(path);
            return Files.list(Paths.get(path))
                    .map(Path::toString)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            Filesjs.LOGGER.error("Error listing files: " + path, e);
            throw new RuntimeException("Failed to list files: " + path, e);
        }
    }

    public void copy(String source, String target) {
        try {
            FileAccessManager.validateFileAccess(source);
            FileAccessManager.validateFileAccess(target);
            Files.copy(Paths.get(source), Paths.get(target), StandardCopyOption.REPLACE_EXISTING);
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            ServerLevel level = server.overworld();
            String content = new String(Files.readAllBytes(Paths.get(target)), StandardCharsets.UTF_8);
            FilesJSPlugin.FILE_COPIED.post(new FileEventJS(target, content, "copied", null, server, level));
        } catch (IOException e) {
            Filesjs.LOGGER.error("Error copying file: " + source + " -> " + target, e);
            throw new RuntimeException("Failed to copy file: " + source + " -> " + target, e);
        }
    }

    public void move(String source, String target) {
        try {
            FileAccessManager.validateFileAccess(source);
            FileAccessManager.validateFileAccess(target);
            Files.move(Paths.get(source), Paths.get(target), StandardCopyOption.REPLACE_EXISTING);
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            ServerLevel level = server.overworld();
            String content = new String(Files.readAllBytes(Paths.get(target)), StandardCharsets.UTF_8);
            FilesJSPlugin.FILE_MOVED.post(new FileEventJS(target, content, "moved", null, server, level));
        } catch (IOException e) {
            Filesjs.LOGGER.error("Error moving file: " + source + " -> " + target, e);
            throw new RuntimeException("Failed to move file: " + source + " -> " + target, e);
        }
    }

    private void validatePath(String path) {
    }


    public void appendLine(String path, String line) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(line);
        Files.write(Paths.get(path), lines, StandardCharsets.UTF_8,
                StandardOpenOption.APPEND, StandardOpenOption.CREATE);
    }


    public void ensureDirectoryExists(String path) throws IOException {
        Path dirPath = Paths.get(path).getParent();
        if (dirPath != null && !Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }
    }

    public void saveJson(String path, String jsonContent) {
        try {
            Path filePath = Paths.get(path);
            ensureDirectoryExists(path);
            writeFile(path, jsonContent);
        } catch (IOException e) {
            Filesjs.LOGGER.error("Error saving JSON file: " + path, e);
            throw new RuntimeException("Failed to save JSON file: " + path, e);
        }
    }

    public void saveScript(String path, String scriptContent) {
        try {
            if (!path.endsWith(".js")) {
                path += ".js";
            }

            String formattedScript = String.format(
                    "// Generated by FilesJS\n" +
                            "// Created at: %s\n\n" +
                            "%s",
                    java.time.LocalDateTime.now(),
                    scriptContent
            );

            Path filePath = Paths.get(path);
            ensureDirectoryExists(path);
            writeFile(path, formattedScript);

        } catch (IOException e) {
            Filesjs.LOGGER.error("Error saving script file: " + path, e);
            throw new RuntimeException("Failed to save script file: " + path, e);
        }
    }

    public List<String> readLastLines(String path, int n) {
        try {
            FileAccessManager.validateFileAccess(path);
            List<String> allLines = Files.readAllLines(Paths.get(path), StandardCharsets.UTF_8);
            int start = Math.max(0, allLines.size() - n);
            return allLines.subList(start, allLines.size());
        } catch (IOException e) {
            Filesjs.LOGGER.error("Error reading last lines: " + path, e);
            throw new RuntimeException("Failed to read last lines: " + path, e);
        }
    }

    public List<String> searchInFile(String path, String searchTerm) {
        try {
            FileAccessManager.validateFileAccess(path);
            return Files.lines(Paths.get(path))
                    .filter(line -> line.contains(searchTerm))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            Filesjs.LOGGER.error("Error searching in file: " + path, e);
            throw new RuntimeException("Failed to search in file: " + path, e);
        }
    }

    public Map<String, Object> getFileInfo(String path) {
        try {
            FileAccessManager.validateFileAccess(path);
            Path filePath = Paths.get(path);
            Map<String, Object> info = new HashMap<>();

            info.put("exists", Files.exists(filePath));
            if (Files.exists(filePath)) {
                info.put("size", Files.size(filePath));
                info.put("lastModified", Files.getLastModifiedTime(filePath).toMillis());
                info.put("isDirectory", Files.isDirectory(filePath));
                info.put("isFile", Files.isRegularFile(filePath));
                info.put("isReadable", Files.isReadable(filePath));
                info.put("isWritable", Files.isWritable(filePath));
            }

            return info;
        } catch (IOException e) {
            Filesjs.LOGGER.error("Error getting file info: " + path, e);
            throw new RuntimeException("Failed to get file info: " + path, e);
        }
    }

    public List<String> listFilesRecursively(String path) {
        try {
            FileAccessManager.validateFileAccess(path);
            List<String> files = new ArrayList<>();
            Files.walk(Paths.get(path))
                    .filter(Files::isRegularFile)
                    .map(Path::toString)
                    .forEach(files::add);
            return files;
        } catch (IOException e) {
            Filesjs.LOGGER.error("Error listing files recursively: " + path, e);
            throw new RuntimeException("Failed to list files recursively: " + path, e);
        }
    }

    public void copyFiles(String sourceDir, String targetDir, String pattern) {
        try {
            FileAccessManager.validateFileAccess(sourceDir);
            FileAccessManager.validateFileAccess(targetDir);

            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
            Files.walk(Paths.get(sourceDir))
                    .filter(Files::isRegularFile)
                    .filter(path -> matcher.matches(path.getFileName()))
                    .forEach(source -> {
                        try {
                            Path target = Paths.get(targetDir, source.getFileName().toString());
                            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            Filesjs.LOGGER.error("Error copying file: " + source, e);
                        }
                    });
        } catch (IOException e) {
            Filesjs.LOGGER.error("Error in batch copy operation", e);
            throw new RuntimeException("Failed in batch copy operation", e);
        }
    }

    public void backupFile(String path) {
        try {
            FileAccessManager.validateFileAccess(path);
            Path source = Paths.get(path);
            if (!Files.exists(source)) {
                throw new IOException("Source file does not exist: " + path);
            }

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String backupName = source.getFileName().toString() + "." + timestamp + ".backup";
            Path backup = source.resolveSibling(backupName);

            Files.copy(source, backup, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Filesjs.LOGGER.error("Error creating backup: " + path, e);
            throw new RuntimeException("Failed to create backup: " + path, e);
        }
    }

    public boolean isFileEmpty(String path) {
        try {
            FileAccessManager.validateFileAccess(path);
            return Files.size(Paths.get(path)) == 0;
        } catch (IOException e) {
            Filesjs.LOGGER.error("Error checking if file is empty: " + path, e);
            throw new RuntimeException("Failed to check if file is empty: " + path, e);
        }
    }

    public void mergeFiles(List<String> sourcePaths, String targetPath) {
        try {
            for (String path : sourcePaths) {
                FileAccessManager.validateFileAccess(path);
            }
            FileAccessManager.validateFileAccess(targetPath);

            // 合并文件内容
            List<String> mergedContent = new ArrayList<>();
            for (String path : sourcePaths) {
                mergedContent.addAll(Files.readAllLines(Paths.get(path), StandardCharsets.UTF_8));
                mergedContent.add(""); // 添加空行分隔
            }

            Files.write(Paths.get(targetPath), mergedContent, StandardCharsets.UTF_8);
        } catch (IOException e) {
            Filesjs.LOGGER.error("Error merging files to: " + targetPath, e);
            throw new RuntimeException("Failed to merge files: " + targetPath, e);
        }
    }

    public void replaceInFile(String path, String search, String replace) {
        try {
            FileAccessManager.validateFileAccess(path);
            String content = new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
            String newContent = content.replace(search, replace);
            Files.write(Paths.get(path), newContent.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Filesjs.LOGGER.error("Error replacing content in file: " + path, e);
            throw new RuntimeException("Failed to replace content in file: " + path, e);
        }
    }

    public void processLargeFile(String path, Consumer<String> lineProcessor) {
        try {
            FileAccessManager.validateFileAccess(path);
            try (BufferedReader reader = Files.newBufferedReader(Paths.get(path))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lineProcessor.accept(line);
                }
            }
        } catch (IOException e) {
            Filesjs.LOGGER.error("Error processing large file: " + path, e);
            throw new RuntimeException("Failed to process large file: " + path, e);
        }
    }

    // 计算文件的MD5哈希值
    public String getFileMD5(String path) {
        try {
            FileAccessManager.validateFileAccess(path);
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(Files.readAllBytes(Paths.get(path)));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (IOException | NoSuchAlgorithmException e) {
            Filesjs.LOGGER.error("Error calculating MD5 for file: " + path, e);
            throw new RuntimeException("Failed to calculate MD5: " + path, e);
        }
    }

    public boolean compareFiles(String path1, String path2) {
        try {
            FileAccessManager.validateFileAccess(path1);
            FileAccessManager.validateFileAccess(path2);
            return Arrays.equals(
                    Files.readAllBytes(Paths.get(path1)),
                    Files.readAllBytes(Paths.get(path2))
            );
        } catch (IOException e) {
            Filesjs.LOGGER.error("Error comparing files", e);
            throw new RuntimeException("Failed to compare files", e);
        }
    }

    public void createZip(String sourcePath, String zipPath) {
        try {
            FileAccessManager.validateFileAccess(sourcePath);
            FileAccessManager.validateFileAccess(zipPath);

            Path source = Paths.get(sourcePath);
            Path zip = Paths.get(zipPath);

            try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zip))) {
                Files.walk(source)
                        .filter(path -> !Files.isDirectory(path))
                        .forEach(path -> {
                            try {
                                String relativePath = source.relativize(path).toString();
                                zos.putNextEntry(new ZipEntry(relativePath));
                                Files.copy(path, zos);
                                zos.closeEntry();
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        });
            }
        } catch (IOException e) {
            Filesjs.LOGGER.error("Error creating ZIP file: " + zipPath, e);
            throw new RuntimeException("Failed to create ZIP file: " + zipPath, e);
        }
    }

    private final Map<String, WatchService> watchServices = new HashMap<>();

    public void watchDirectory(String path, Consumer<Path> changeCallback) {
        try {
            FileAccessManager.validateFileAccess(path);
            Path dir = Paths.get(path);
            WatchService watchService = FileSystems.getDefault().newWatchService();
            dir.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

            watchServices.put(path, watchService);

            Thread watchThread = new Thread(() -> {
                try {
                    while (true) {
                        WatchKey key = watchService.take();
                        for (WatchEvent<?> event : key.pollEvents()) {
                            @SuppressWarnings("unchecked")
                            WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                            Path changed = dir.resolve(pathEvent.context());
                            changeCallback.accept(changed);
                        }
                        key.reset();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            watchThread.setDaemon(true);
            watchThread.start();
        } catch (IOException e) {
            Filesjs.LOGGER.error("Error setting up file watcher: " + path, e);
            throw new RuntimeException("Failed to set up file watcher: " + path, e);
        }
    }

    public void stopWatching(String path) {
        WatchService watchService = watchServices.remove(path);
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                Filesjs.LOGGER.error("Error closing file watcher: " + path, e);
            }
        }
    }

    private void triggerAccessDenied(String path, String operation, ServerPlayer player) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        ServerLevel level = server.overworld();
        FilesJSPlugin.FILE_ACCESS_DENIED.post(new FileEventJS(path, null, "access_denied", player, server, level));
    }

    public void renameFile(String oldPath, String newPath) {
        try {
            FileAccessManager.validateFileAccess(oldPath);
            FileAccessManager.validateFileAccess(newPath);
            
            Path source = Paths.get(oldPath);
            Path target = Paths.get(newPath);
            
            String content = new String(Files.readAllBytes(source), StandardCharsets.UTF_8);
            
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            ServerLevel level = server.overworld();
            FilesJSPlugin.FILE_RENAMED.post(new FileEventJS(newPath, content, "renamed", null, server, level));
        } catch (IOException e) {
            Filesjs.LOGGER.error("Error renaming file: " + oldPath + " -> " + newPath, e);
            throw new RuntimeException("Failed to rename file: " + oldPath + " -> " + newPath, e);
        }
    }

    public void watchFileSize(String path, long threshold) {
        try {
            FileAccessManager.validateFileAccess(path);
            Path filePath = Paths.get(path);
            
            watchDirectory(path, changedPath -> {
                try {
                    if (Files.size(changedPath) > threshold) {
                        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                        ServerLevel level = server.overworld();
                        FilesJSPlugin.FILE_SIZE_THRESHOLD.post(new FileEventJS(
                            path,
                            String.valueOf(Files.size(changedPath)),
                            "size_threshold",
                            null,
                            server,
                            level
                        ));
                    }
                } catch (IOException e) {
                    Filesjs.LOGGER.error("Error checking file size: " + path, e);
                }
            });
        } catch (SecurityException e) {
            Filesjs.LOGGER.error("Security error setting up file size watch: " + path, e);
            throw e;
        }
    }

    private double calculateSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) {
            return 0.0;
        }
        
        int[][] dp = new int[text1.length() + 1][text2.length() + 1];
        
        for (int i = 1; i <= text1.length(); i++) {
            for (int j = 1; j <= text2.length(); j++) {
                if (text1.charAt(i - 1) == text2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }
        
        int lcsLength = dp[text1.length()][text2.length()];
        int maxLength = Math.max(text1.length(), text2.length());
        
        return maxLength > 0 ? (double) lcsLength / maxLength : 1.0;
    }

    public void watchFilePattern(String directory, String pattern) {
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
        watchDirectory(directory, path -> {
            if (matcher.matches(path.getFileName())) {
                try {
                    String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                    MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                    ServerLevel level = server.overworld();
                    FilesJSPlugin.FILE_PATTERN_MATCHED.post(new FileEventJS(
                        path.toString(),
                        content,
                        "pattern_matched",
                        null,
                        server,
                        level
                    ));
                } catch (IOException e) {
                    Filesjs.LOGGER.error("Error reading matched file: " + path, e);
                }
            }
        });
    }

    private Map<String, String> fileContentCache = new HashMap<>();
    
    public void watchContentChanges(String path, double threshold) {
        try {
            FileAccessManager.validateFileAccess(path);
            String originalContent = new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
            fileContentCache.put(path, originalContent);
            
            watchDirectory(path, changedPath -> {
                try {
                    String newContent = new String(Files.readAllBytes(changedPath), StandardCharsets.UTF_8);
                    String oldContent = fileContentCache.get(path);
                    
                    double similarity = calculateSimilarity(oldContent, newContent);
                    if (1.0 - similarity > threshold) {
                        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                        ServerLevel level = server.overworld();
                        FilesJSPlugin.FILE_CONTENT_CHANGED_SIGNIFICANTLY.post(new FileEventJS(
                            path,
                            newContent,
                            "content_changed_significantly",
                            null,
                            server,
                            level
                        ));
                    }
                    
                    fileContentCache.put(path, newContent);
                } catch (IOException e) {
                    Filesjs.LOGGER.error("Error checking content changes: " + path, e);
                }
            });
        } catch (IOException e) {
            Filesjs.LOGGER.error("Error setting up content watch: " + path, e);
        }
    }

    // 存储事件监听器的引用
    private Object currentTickListener = null;

    public void scheduleBackup(String path, int ticks) {
        // 如果ticks为0，执行即时备份
        if (ticks == 0) {
            doBackup(path);
            return;
        }

        // 使用MinecraftServer的tick来计时
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            // 如果存在旧的监听器，先移除它
            if (currentTickListener != null) {
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(currentTickListener);
            }

            // 创建一个计数器
            final int[] tickCounter = {0};
            
            // 创建新的监听器对象
            Object listener = new Object() {
                @net.minecraftforge.eventbus.api.SubscribeEvent
                public void onServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
                    if (event.phase == net.minecraftforge.event.TickEvent.Phase.END) {
                        tickCounter[0]++;
                        if (tickCounter[0] >= ticks) {
                            // 执行备份
                            doBackup(path);
                            // 移除监听器
                            net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(this);
                            currentTickListener = null;
                        }
                    }
                }
            };

            // 注册新的监听器
            currentTickListener = listener;
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(listener);
        }
    }

    // 将备份逻辑抽取到单独的方法
    private void doBackup(String path) {
        try {
            Path backupDir = Paths.get("kubejs/backups");
            if (!Files.exists(backupDir)) {
                Files.createDirectories(backupDir);
            }
            
            Path sourcePath = Paths.get(path);
            String fileName = sourcePath.getFileName().toString();
            
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String backupName = fileName + "." + timestamp + ".backup";
            
            String relativePath = sourcePath.getParent().toString().replace('\\', '/');
            Path backupPath = backupDir.resolve(relativePath).resolve(backupName);
            
            Files.createDirectories(backupPath.getParent());
            
            copy(path, backupPath.toString());
            
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            ServerLevel level = server.overworld();
            FilesJSPlugin.FILE_BACKUP_SCHEDULED.post(new FileEventJS(
                backupPath.toString(),
                null,
                "backup_scheduled",
                null,
                server,
                level
            ));
            
            cleanupOldBackups(backupDir.resolve(relativePath), 5);
            
        } catch (Exception e) {
            Filesjs.LOGGER.error("Error during backup: " + path, e);
        }
    }

    private void cleanupOldBackups(Path backupDir, int keepCount) throws IOException {
        if (!Files.exists(backupDir)) return;
        
        List<Path> backups = Files.list(backupDir)
            .filter(path -> path.toString().endsWith(".backup"))
            .sorted((a, b) -> {
                try {
                    return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
                } catch (IOException e) {
                    return 0;
                }
            })
            .collect(Collectors.toList());
        
        if (backups.size() > keepCount) {
            for (Path backup : backups.subList(keepCount, backups.size())) {
                Files.delete(backup);
            }
        }
    }
} 