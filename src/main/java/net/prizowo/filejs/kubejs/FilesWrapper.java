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

public class FilesWrapper {
    private Path validateAndNormalizePath(String path) throws SecurityException {
        if (path == null || path.trim().isEmpty()) {
            throw new SecurityException("Access denied: Empty path");
        }
        
        Path minecraftDir = FileAccessManager.getMinecraftDir().normalize().toAbsolutePath();
        
        path = path.replace('\\', '/').trim();
        
        if (path.contains(":") || path.startsWith("/") || path.startsWith("\\") ||
            path.contains("../") || path.contains("..\\") || 
            path.contains("./") || path.contains(".\\") ||
            path.contains("|") || path.contains("*") || path.contains("?") ||
            path.contains("<") || path.contains(">") || path.contains("\"")) {
            throw new SecurityException("Access denied: Invalid path characters or absolute path detected: " + path);
        }
        
        Path resolvedPath;
        try {
            resolvedPath = minecraftDir.resolve(path).normalize().toAbsolutePath();
            if (!resolvedPath.startsWith(minecraftDir)) {
                throw new SecurityException("Access denied: Path escapes Minecraft directory: " + path);
            }
        } catch (Exception e) {
            throw new SecurityException("Access denied: Invalid path format: " + path);
        }
        
        String relativePathStr;
        try {
            relativePathStr = minecraftDir.relativize(resolvedPath).toString().replace('\\', '/');
        } catch (Exception e) {
            throw new SecurityException("Access denied: Cannot relativize path: " + path);
        }
        
        String firstDir = relativePathStr.split("/")[0];
        if (!FileAccessManager.ALLOWED_SUBDIRS.contains(firstDir)) {
            throw new SecurityException("Access denied: Directory not allowed: " + firstDir);
        }
        
        if (!Files.isDirectory(resolvedPath)) {
            if (!relativePathStr.startsWith("kubejs/backups/")) {
                String extension = getFileExtension(relativePathStr);
                if (!FileAccessManager.ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
                    throw new SecurityException("Access denied: File type not allowed: " + extension);
                }
            }
        }
        
        try {
            if (Files.exists(resolvedPath) && Files.isSymbolicLink(resolvedPath)) {
                throw new SecurityException("Access denied: Symbolic links not allowed: " + path);
            }
        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            throw new SecurityException("Access denied: Error checking path: " + path);
        }
        
        return resolvedPath;
    }

    private static String getFileExtension(String path) {
        int lastDot = path.lastIndexOf('.');
        return lastDot > 0 ? path.substring(lastDot).toLowerCase() : "";
    }

    public String readFile(String path) {
        try {
            Path normalizedPath = validateAndNormalizePath(path);
            return new String(Files.readAllBytes(normalizedPath), StandardCharsets.UTF_8);
        } catch (SecurityException e) {
            Filesjs.LOGGER.error("Security violation in read operation: " + path, e);
            throw e;
        } catch (IOException e) {
            Filesjs.LOGGER.error("Error reading file: " + path, e);
            throw new RuntimeException("Failed to read file: " + path, e);
        }
    }

    public List<String> readLines(String path) throws IOException {
        try {
            Path normalizedPath = validateAndNormalizePath(path);
            return Files.readAllLines(normalizedPath, StandardCharsets.UTF_8);
        } catch (SecurityException e) {
            Filesjs.LOGGER.error("Security violation in readLines operation: " + path, e);
            throw e;
        } catch (IOException e) {
            Filesjs.LOGGER.error("Error reading lines from file: " + path, e);
            throw new RuntimeException("Failed to read lines from file: " + path, e);
        }
    }

    public void writeFile(String path, String content) {
        try {
            Path normalizedPath = validateAndNormalizePath(path);
            
            if (content.length() > 1024 * 1024 * 5) { // 5MB
                throw new SecurityException("Content size exceeds limit (max 5MB)");
            }
            
            boolean isNewFile = !Files.exists(normalizedPath);
            Files.write(normalizedPath, content.getBytes(StandardCharsets.UTF_8));
            
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            ServerLevel level = server.overworld();
            
            if (isNewFile) {
                FilesJSPlugin.FILE_CREATED.post(new FileEventJS(path, content, "created", null, server, level));
            } else {
                FilesJSPlugin.FILE_CHANGED.post(new FileEventJS(path, content, "changed", null, server, level));
            }
        } catch (SecurityException e) {
            Filesjs.LOGGER.error("Security violation in write operation: " + path, e);
            throw e;
        } catch (IOException e) {
            Filesjs.LOGGER.error("Error writing file: " + path, e);
            throw new RuntimeException("Failed to write file: " + path, e);
        }
    }

    public void writeLines(String path, List<String> lines) {
        try {
            Path normalizedPath = validateAndNormalizePath(path);
            Files.write(normalizedPath, lines, StandardCharsets.UTF_8);
        } catch (SecurityException e) {
            Filesjs.LOGGER.error("Security violation in writeLines operation: " + path, e);
            throw e;
        } catch (IOException e) {
            Filesjs.LOGGER.error("Error writing lines to file: " + path, e);
            throw new RuntimeException("Failed to write lines to file: " + path, e);
        }
    }

    public void appendFile(String path, String content) {
        try {
            Path normalizedPath = validateAndNormalizePath(path);
            Files.write(normalizedPath, content.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        } catch (SecurityException e) {
            Filesjs.LOGGER.error("Security violation in append operation: " + path, e);
            throw e;
        } catch (IOException e) {
            Filesjs.LOGGER.error("Error appending to file: " + path, e);
            throw new RuntimeException("Failed to append to file: " + path, e);
        }
    }

    public boolean exists(String path) {
        try {
            Path normalizedPath = validateAndNormalizePath(path);
            return Files.exists(normalizedPath);
        } catch (SecurityException e) {
            Filesjs.LOGGER.error("Security violation in exists check: " + path, e);
            throw e;
        }
    }

    public void createDirectory(String path) {
        try {
            Path normalizedPath = validateAndNormalizePath(path);
            Files.createDirectories(normalizedPath);
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            ServerLevel level = server.overworld();
            FilesJSPlugin.DIRECTORY_CREATED.post(new FileEventJS(path, null, "directory_created", null, server, level));
        } catch (SecurityException e) {
            Filesjs.LOGGER.error("Security violation in directory creation: " + path, e);
            throw e;
        } catch (IOException e) {
            Filesjs.LOGGER.error("Error creating directory: " + path, e);
            throw new RuntimeException("Failed to create directory: " + path, e);
        }
    }

    public void delete(String path) {
        try {
            Path normalizedPath = validateAndNormalizePath(path);
            
            if (!Files.exists(normalizedPath)) {
                throw new IOException("File does not exist: " + path);
            }
            
            boolean isDirectory = Files.isDirectory(normalizedPath);
            
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            ServerLevel level = server.overworld();
            FileEventJS event = new FileEventJS(path, null, isDirectory ? "directory_deleted" : "deleted", null, server, level);
            
            Files.delete(normalizedPath);
            
            if (isDirectory) {
                FilesJSPlugin.DIRECTORY_DELETED.post(event);
            } else {
                FilesJSPlugin.FILE_DELETED.post(event);
            }
        } catch (SecurityException e) {
            Filesjs.LOGGER.error("Security violation in delete operation: " + path, e);
            throw e;
        } catch (IOException e) {
            Filesjs.LOGGER.error("Error deleting file: " + path, e);
            throw new RuntimeException("Failed to delete file: " + path, e);
        }
    }

    public List<String> listFiles(String path) {
        try {
            Path normalizedPath = validateAndNormalizePath(path);
            if (!Files.isDirectory(normalizedPath)) {
                throw new SecurityException("Access denied: Path is not a directory: " + path);
            }
            
            return Files.list(normalizedPath)
                .filter(p -> {
                    try {
                        validateAndNormalizePath(p.toString());
                        return Files.isRegularFile(p);
                    } catch (SecurityException e) {
                        Filesjs.LOGGER.warn("Skipping unsafe file in listing: " + p);
                        return false;
                    }
                })
                .map(Path::toString)
                .collect(Collectors.toList());
        } catch (SecurityException e) {
            Filesjs.LOGGER.error("Security violation in list operation: " + path, e);
            throw e;
        } catch (IOException e) {
            Filesjs.LOGGER.error("Error listing files: " + path, e);
            throw new RuntimeException("Failed to list files: " + path, e);
        }
    }

    public List<String> listDirectories(String path) {
        try {
            Path normalizedPath = validateAndNormalizePath(path);
            if (!Files.isDirectory(normalizedPath)) {
                throw new SecurityException("Access denied: Path is not a directory: " + path);
            }
            
            return Files.list(normalizedPath)
                .filter(p -> {
                    try {
                        validateAndNormalizePath(p.toString());
                        return Files.isDirectory(p);
                    } catch (SecurityException e) {
                        Filesjs.LOGGER.warn("Skipping unsafe directory in listing: " + p);
                        return false;
                    }
                })
                .map(Path::toString)
                .collect(Collectors.toList());
        } catch (SecurityException e) {
            Filesjs.LOGGER.error("Security violation in list operation: " + path, e);
            throw e;
        } catch (IOException e) {
            Filesjs.LOGGER.error("Error listing directories: " + path, e);
            throw new RuntimeException("Failed to list directories: " + path, e);
        }
    }

    public void copy(String source, String target) {
        try {
            Path sourcePath = validateAndNormalizePath(source);
            Path targetPath = validateAndNormalizePath(target);
            
            FileEventJS event = new FileEventJS(target, null, "copied", null,
                ServerLifecycleHooks.getCurrentServer(), 
                ServerLifecycleHooks.getCurrentServer().overworld());
            
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            
            FilesJSPlugin.FILE_COPIED.post(event);
        } catch (SecurityException e) {
            Filesjs.LOGGER.error("Security violation in copy operation: " + source + " -> " + target, e);
            throw e;
        } catch (IOException e) {
            Filesjs.LOGGER.error("Error copying file: " + source + " -> " + target, e);
            throw new RuntimeException("Failed to copy file: " + source + " -> " + target, e);
        }
    }

    public void move(String source, String target) {
        try {
            Path sourcePath = validateAndNormalizePath(source);
            Path targetPath = validateAndNormalizePath(target);
            
            Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            ServerLevel level = server.overworld();
            String content = new String(Files.readAllBytes(targetPath), StandardCharsets.UTF_8);
            FilesJSPlugin.FILE_MOVED.post(new FileEventJS(target, content, "moved", null, server, level));
        } catch (SecurityException e) {
            Filesjs.LOGGER.error("Security violation in move operation: " + source + " -> " + target, e);
            throw e;
        } catch (IOException e) {
            Filesjs.LOGGER.error("Error moving file: " + source + " -> " + target, e);
            throw new RuntimeException("Failed to move file: " + source + " -> " + target, e);
        }
    }


    public void appendLine(String path, String line) {
        try {
            Path normalizedPath = validateAndNormalizePath(path);
            List<String> lines = new ArrayList<>();
            lines.add(line);
            Files.write(normalizedPath, lines, StandardCharsets.UTF_8,
                    StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        } catch (SecurityException e) {
            Filesjs.LOGGER.error("Security violation in appendLine operation: " + path, e);
            throw e;
        } catch (IOException e) {
            Filesjs.LOGGER.error("Error appending line to file: " + path, e);
            throw new RuntimeException("Failed to append line to file: " + path, e);
        }
    }


    public void ensureDirectoryExists(String path) {
        try {
            Path normalizedPath = validateAndNormalizePath(path);
            if (!Files.exists(normalizedPath)) {
                Files.createDirectories(normalizedPath);
            }
        } catch (SecurityException e) {
            Filesjs.LOGGER.error("Security violation in directory creation: " + path, e);
            throw e;
        } catch (IOException e) {
            Filesjs.LOGGER.error("Error creating directory: " + path, e);
            throw new RuntimeException("Failed to create directory: " + path, e);
        }
    }

    public void saveJson(String path, String jsonContent) {
        try {
            Path normalizedPath = validateAndNormalizePath(path);
            
            Path parent = normalizedPath.getParent();
            if (parent != null) {
                String parentPath = FileAccessManager.getMinecraftDir().relativize(parent).toString().replace('\\', '/');
                ensureDirectoryExists(parentPath);
            }
            
            writeFile(path, jsonContent);
        } catch (SecurityException e) {
            Filesjs.LOGGER.error("Security violation in JSON save: " + path, e);
            throw e;
        } catch (RuntimeException e) {
            Filesjs.LOGGER.error("Error saving JSON file: " + path, e);
            throw new RuntimeException("Failed to save JSON file: " + path, e);
        }
    }

    public void saveScript(String path, String scriptContent) {
        try {
            if (!path.endsWith(".js")) {
                path += ".js";
            }

            Path normalizedPath = validateAndNormalizePath(path);
            
            Path parent = normalizedPath.getParent();
            if (parent != null) {
                String parentPath = FileAccessManager.getMinecraftDir().relativize(parent).toString().replace('\\', '/');
                ensureDirectoryExists(parentPath);
            }

            String formattedScript = String.format(
                    "// Generated by FilesJS\n" +
                    "// Created at: %s\n\n" +
                    "%s",
                    java.time.LocalDateTime.now(),
                    scriptContent
            );

            // 写入脚本内容
            writeFile(path, formattedScript);
        } catch (SecurityException e) {
            Filesjs.LOGGER.error("Security violation in script save: " + path, e);
            throw e;
        } catch (RuntimeException e) {
            Filesjs.LOGGER.error("Error saving script file: " + path, e);
            throw new RuntimeException("Failed to save script file: " + path, e);
        }
    }

    public List<String> readLastLines(String path, int n) {
        try {
            Path normalizedPath = validateAndNormalizePath(path);
            List<String> allLines = Files.readAllLines(normalizedPath, StandardCharsets.UTF_8);
            int start = Math.max(0, allLines.size() - n);
            return allLines.subList(start, allLines.size());
        } catch (SecurityException e) {
            Filesjs.LOGGER.error("Security violation in reading last lines: " + path, e);
            throw e;
        } catch (IOException e) {
            Filesjs.LOGGER.error("Error reading last lines: " + path, e);
            throw new RuntimeException("Failed to read last lines: " + path, e);
        }
    }

    public List<String> searchInFile(String path, String searchTerm) {
        try {
            Path normalizedPath = validateAndNormalizePath(path);
            return Files.lines(normalizedPath)
                    .filter(line -> line.contains(searchTerm))
                    .collect(Collectors.toList());
        } catch (SecurityException e) {
            Filesjs.LOGGER.error("Security violation in file search: " + path, e);
            throw e;
        } catch (IOException e) {
            Filesjs.LOGGER.error("Error searching in file: " + path, e);
            throw new RuntimeException("Failed to search in file: " + path, e);
        }
    }

    public Map<String, Object> getFileInfo(String path) {
        try {
            Path normalizedPath = validateAndNormalizePath(path);
            Map<String, Object> info = new HashMap<>();

            info.put("exists", Files.exists(normalizedPath));
            if (Files.exists(normalizedPath)) {
                info.put("size", Files.size(normalizedPath));
                info.put("lastModified", Files.getLastModifiedTime(normalizedPath).toMillis());
                info.put("isDirectory", Files.isDirectory(normalizedPath));
                info.put("isFile", Files.isRegularFile(normalizedPath));
                info.put("isReadable", Files.isReadable(normalizedPath));
                info.put("isWritable", Files.isWritable(normalizedPath));
            }

            return info;
        } catch (SecurityException e) {
            Filesjs.LOGGER.error("Security violation in getting file info: " + path, e);
            throw e;
        } catch (IOException e) {
            Filesjs.LOGGER.error("Error getting file info: " + path, e);
            throw new RuntimeException("Failed to get file info: " + path, e);
        }
    }

    public List<String> listFilesRecursively(String path) {
        try {
            Path normalizedPath = validateAndNormalizePath(path);
            List<String> files = new ArrayList<>();
            
            Files.walk(normalizedPath)
                .filter(p -> {
                    try {
                        validateAndNormalizePath(p.toString());
                        return Files.isRegularFile(p);
                    } catch (SecurityException e) {
                        Filesjs.LOGGER.warn("Skipping unsafe path in recursive listing: " + p);
                        return false;
                    }
                })
                .map(Path::toString)
                .forEach(files::add);
                
            return files;
        } catch (SecurityException e) {
            Filesjs.LOGGER.error("Security violation in recursive listing: " + path, e);
            throw e;
        } catch (IOException e) {
            Filesjs.LOGGER.error("Error listing files recursively: " + path, e);
            throw new RuntimeException("Failed to list files recursively: " + path, e);
        }
    }

    public void copyFiles(String sourceDir, String targetDir, String pattern) {
        try {
            Path sourcePath = validateAndNormalizePath(sourceDir);
            Path targetPath = validateAndNormalizePath(targetDir);

            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
            Files.walk(sourcePath)
                    .filter(path -> {
                        try {
                            validateAndNormalizePath(path.toString());
                            return Files.isRegularFile(path) && matcher.matches(path.getFileName());
                        } catch (SecurityException e) {
                            Filesjs.LOGGER.warn("Skipping unsafe path in batch copy: " + path);
                            return false;
                        }
                    })
                    .forEach(source -> {
                        try {
                            Path target = targetPath.resolve(sourcePath.relativize(source));
                            validateAndNormalizePath(target.toString());
                            Files.createDirectories(target.getParent());
                            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                        } catch (SecurityException | IOException e) {
                            Filesjs.LOGGER.error("Error copying file: " + source, e);
                        }
                    });
        } catch (SecurityException e) {
            Filesjs.LOGGER.error("Security violation in batch copy operation: " + sourceDir + " -> " + targetDir, e);
            throw e;
        } catch (IOException e) {
            Filesjs.LOGGER.error("Error in batch copy operation", e);
            throw new RuntimeException("Failed in batch copy operation", e);
        }
    }

    public void backupFile(String path) {
        try {
            Path sourcePath = validateAndNormalizePath(path);
            if (!Files.exists(sourcePath)) {
                throw new IOException("Source file does not exist: " + path);
            }

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String backupName = sourcePath.getFileName().toString() + "." + timestamp + ".backup";
            
            Path backupDir = Paths.get("kubejs/backups");
            Path backupPath = backupDir.resolve(backupName);
            validateAndNormalizePath(backupPath.toString());
            
            Files.createDirectories(backupDir);
            Files.copy(sourcePath, backupPath, StandardCopyOption.REPLACE_EXISTING);
            
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            ServerLevel level = server.overworld();
            FilesJSPlugin.FILE_BACKUP_CREATED.post(new FileEventJS(backupPath.toString(), null, "backup_created", null, server, level));
        } catch (SecurityException e) {
            Filesjs.LOGGER.error("Security violation in backup creation: " + path, e);
            throw e;
        } catch (IOException e) {
            Filesjs.LOGGER.error("Error creating backup: " + path, e);
            throw new RuntimeException("Failed to create backup: " + path, e);
        }
    }

    public boolean isFileEmpty(String path) {
        try {
            Path normalizedPath = validateAndNormalizePath(path);
            return Files.size(normalizedPath) == 0;
        } catch (SecurityException e) {
            Filesjs.LOGGER.error("Security violation in checking empty file: " + path, e);
            throw e;
        } catch (IOException e) {
            Filesjs.LOGGER.error("Error checking if file is empty: " + path, e);
            throw new RuntimeException("Failed to check if file is empty: " + path, e);
        }
    }

    public void mergeFiles(List<String> sourcePaths, String targetPath) {
        try {
            List<Path> normalizedSourcePaths = new ArrayList<>();
            for (String path : sourcePaths) {
                normalizedSourcePaths.add(validateAndNormalizePath(path));
            }
            
            Path normalizedTargetPath = validateAndNormalizePath(targetPath);
            
            List<String> mergedContent = new ArrayList<>();
            for (Path path : normalizedSourcePaths) {
                mergedContent.addAll(Files.readAllLines(path, StandardCharsets.UTF_8));
                mergedContent.add("");
            }

            Files.write(normalizedTargetPath, mergedContent, StandardCharsets.UTF_8);
            
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            ServerLevel level = server.overworld();
            FilesJSPlugin.FILES_MERGED.post(new FileEventJS(targetPath, String.join("\n", mergedContent), "merged", null, server, level));
        } catch (SecurityException e) {
            Filesjs.LOGGER.error("Security violation in merging files: " + targetPath, e);
            throw e;
        } catch (IOException e) {
            Filesjs.LOGGER.error("Error merging files to: " + targetPath, e);
            throw new RuntimeException("Failed to merge files: " + targetPath, e);
        }
    }

    public void replaceInFile(String path, String search, String replace) {
        try {
            Path normalizedPath = validateAndNormalizePath(path);
            String content = new String(Files.readAllBytes(normalizedPath), StandardCharsets.UTF_8);
            String newContent = content.replace(search, replace);
            Files.write(normalizedPath, newContent.getBytes(StandardCharsets.UTF_8));
        } catch (SecurityException e) {
            Filesjs.LOGGER.error("Security violation in file content replacement: " + path, e);
            throw e;
        } catch (IOException e) {
            Filesjs.LOGGER.error("Error replacing content in file: " + path, e);
            throw new RuntimeException("Failed to replace content in file: " + path, e);
        }
    }

    public void processLargeFile(String path, Consumer<String> lineProcessor) {
        try {
            Path normalizedPath = validateAndNormalizePath(path);
            try (BufferedReader reader = Files.newBufferedReader(normalizedPath)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lineProcessor.accept(line);
                }
            }
        } catch (SecurityException e) {
            Filesjs.LOGGER.error("Security violation in processing large file: " + path, e);
            throw e;
        } catch (IOException e) {
            Filesjs.LOGGER.error("Error processing large file: " + path, e);
            throw new RuntimeException("Failed to process large file: " + path, e);
        }
    }

    public String getFileMD5(String path) {
        try {
            Path normalizedPath = validateAndNormalizePath(path);
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(Files.readAllBytes(normalizedPath));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (SecurityException e) {
            Filesjs.LOGGER.error("Security violation in MD5 calculation: " + path, e);
            throw e;
        } catch (IOException | NoSuchAlgorithmException e) {
            Filesjs.LOGGER.error("Error calculating MD5 for file: " + path, e);
            throw new RuntimeException("Failed to calculate MD5: " + path, e);
        }
    }

    public boolean compareFiles(String path1, String path2) {
        try {
            Path normalizedPath1 = validateAndNormalizePath(path1);
            Path normalizedPath2 = validateAndNormalizePath(path2);

            // 先检查文件是否存在
            if (!Files.exists(normalizedPath1)) {
                throw new IOException("First file does not exist: " + path1);
            }
            if (!Files.exists(normalizedPath2)) {
                throw new IOException("Second file does not exist: " + path2);
            }

            return Arrays.equals(
                Files.readAllBytes(normalizedPath1),
                Files.readAllBytes(normalizedPath2)
            );
        } catch (SecurityException e) {
            Filesjs.LOGGER.error("Security violation in file comparison: " + path1 + " vs " + path2, e);
            throw e;
        } catch (IOException e) {
            Filesjs.LOGGER.error("Error comparing files: " + path1 + " vs " + path2, e);
            throw new RuntimeException("Failed to compare files", e);
        }
    }

    public void createZip(String sourcePath, String zipPath) {
        try {
            Path source = validateZipPath(sourcePath);
            Path zip = validateAndNormalizePath(zipPath);

            if (!Files.exists(source)) {
                throw new IOException("Source directory does not exist: " + sourcePath);
            }

            Path zipParent = zip.getParent();
            if (zipParent != null) {
                Files.createDirectories(zipParent);
            }

            try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zip))) {
                Files.walk(source)
                    .filter(path -> {
                        try {
                            FileAccessManager.validateZipAccess(path.toString());
                            return true;
                        } catch (SecurityException e) {
                            Filesjs.LOGGER.warn("Skipping unsafe path in zip: " + path);
                            return false;
                        }
                    })
                    .forEach(path -> {
                        try {
                            String relativePath = source.relativize(path).toString().replace('\\', '/');
                            if (Files.isDirectory(path)) {
                                relativePath += "/";
                            }
                            zos.putNextEntry(new ZipEntry(relativePath));
                            if (!Files.isDirectory(path)) {
                                Files.copy(path, zos);
                            }
                            zos.closeEntry();
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
            }
        } catch (SecurityException e) {
            Filesjs.LOGGER.error("Security violation in zip creation: " + sourcePath + " -> " + zipPath, e);
            throw e;
        } catch (IOException | UncheckedIOException e) {
            Filesjs.LOGGER.error("Error creating zip file: " + zipPath, e);
            throw new RuntimeException("Failed to create zip file: " + zipPath, e);
        }
    }

    private Path validateZipPath(String path) throws SecurityException {
        try {
            return Paths.get(FileAccessManager.getMinecraftDir().toString(), path).normalize();
        } catch (Exception e) {
            throw new SecurityException("Invalid path: " + path);
        }
    }

    private final Map<String, WatchService> watchServices = new HashMap<>();

    public void watchDirectory(String path, Consumer<Path> changeCallback) {
        try {
            Path normalizedPath = validateAndNormalizePath(path);
            
            FileEventJS watchEvent = new FileEventJS(path, null, "watch_started", null,
                ServerLifecycleHooks.getCurrentServer(),
                ServerLifecycleHooks.getCurrentServer().overworld());
            
            WatchService watchService;
            try {
                watchService = FileSystems.getDefault().newWatchService();
                normalizedPath.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
            } catch (IOException e) {
                Filesjs.LOGGER.error("Error creating watch service: " + path, e);
                throw new RuntimeException("Failed to create watch service: " + path, e);
            }

            watchServices.put(path, watchService);

            Thread watchThread = new Thread(() -> {
                try {
                    while (true) {
                        WatchKey key = watchService.take();
                        for (WatchEvent<?> watchedEvent : key.pollEvents()) {
                            @SuppressWarnings("unchecked")
                            WatchEvent<Path> pathEvent = (WatchEvent<Path>) watchedEvent;
                            Path changed = normalizedPath.resolve(pathEvent.context());
                            try {
                                validateAndNormalizePath(changed.toString());
                                changeCallback.accept(changed);
                            } catch (SecurityException e) {
                                Filesjs.LOGGER.warn("Skipping unsafe path in watch event: " + changed);
                            }
                        }
                        key.reset();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            watchThread.setDaemon(true);
            watchThread.start();
        } catch (SecurityException e) {
            Filesjs.LOGGER.error("Security violation in watch operation: " + path, e);
            throw e;
        }
    }

    public void stopWatching(String path) {
        try {
            Path normalizedPath = validateAndNormalizePath(path);
            WatchService watchService = watchServices.remove(normalizedPath.toString());
            if (watchService != null) {
                try {
                    watchService.close();
                    MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                    ServerLevel level = server.overworld();
                    FilesJSPlugin.FILE_WATCH_STOPPED.post(new FileEventJS(path, null, "watch_stopped", null, server, level));
                } catch (IOException e) {
                    Filesjs.LOGGER.error("Error closing file watcher: " + path, e);
                    throw new RuntimeException("Failed to close file watcher: " + path, e);
                }
            }
        } catch (SecurityException e) {
            Filesjs.LOGGER.error("Security violation in stopping file watcher: " + path, e);
            throw e;
        }
    }

    private void triggerAccessDenied(String path, String operation, ServerPlayer player) {
        try {
            Path normalizedPath = validateAndNormalizePath(path);
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            ServerLevel level = server.overworld();
            FilesJSPlugin.FILE_ACCESS_DENIED.post(new FileEventJS(
                normalizedPath.toString(), 
                null, 
                "access_denied", 
                player, 
                server, 
                level
            ));
            
            Filesjs.LOGGER.warn("Access denied: Player {} attempted {} operation on {}", 
                player != null ? player.getName().getString() : "Unknown",
                operation,
                normalizedPath
            );
        } catch (Exception e) {
            Filesjs.LOGGER.error("Error handling access denied event: " + path, e);
        }
    }

    public void renameFile(String oldPath, String newPath) {
        try {
            Path sourcePath = validateAndNormalizePath(oldPath);
            Path targetPath = validateAndNormalizePath(newPath);
            
            String content = new String(Files.readAllBytes(sourcePath), StandardCharsets.UTF_8);
            
            Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            ServerLevel level = server.overworld();
            FilesJSPlugin.FILE_RENAMED.post(new FileEventJS(newPath, content, "renamed", null, server, level));
        } catch (SecurityException e) {
            Filesjs.LOGGER.error("Security violation in rename operation: " + oldPath + " -> " + newPath, e);
            throw e;
        } catch (IOException e) {
            Filesjs.LOGGER.error("Error renaming file: " + oldPath + " -> " + newPath, e);
            throw new RuntimeException("Failed to rename file: " + oldPath + " -> " + newPath, e);
        }
    }

    public void watchFileSize(String path, long threshold) {
        try {
            Path normalizedPath = validateAndNormalizePath(path);
            
            Path parentDir = normalizedPath.getParent();
            if (parentDir == null) {
                throw new SecurityException("Invalid file path: " + path);
            }
            
            Path fileName = normalizedPath.getFileName();
            
            String relativeParentDir = FileAccessManager.getMinecraftDir()
                .relativize(parentDir)
                .toString()
                .replace('\\', '/');
            
            watchDirectory(relativeParentDir, changedPath -> {
                try {
                    if (changedPath.getFileName().equals(fileName) && Files.exists(changedPath)) {
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
                    }
                } catch (IOException e) {
                    Filesjs.LOGGER.error("Error checking file size: " + path, e);
                }
            });
        } catch (SecurityException e) {
            Filesjs.LOGGER.error("Security violation in setting up file size watch: " + path, e);
            throw e;
        }
    }

    public void watchFilePattern(String directory, String pattern) {
        try {
            Path normalizedPath = validateAndNormalizePath(directory);
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
            
            watchDirectory(directory, path -> {
                try {
                    Path normalizedChangedPath = validateAndNormalizePath(path.toString());
                    if (matcher.matches(normalizedChangedPath.getFileName())) {
                        String content = new String(Files.readAllBytes(normalizedChangedPath), StandardCharsets.UTF_8);
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
                    }
                } catch (SecurityException e) {
                    Filesjs.LOGGER.warn("Security violation in pattern watch: " + path);
                } catch (IOException e) {
                    Filesjs.LOGGER.error("Error reading matched file: " + path, e);
                }
            });
        } catch (SecurityException e) {
            Filesjs.LOGGER.error("Security violation in setting up pattern watch: " + directory, e);
            throw e;
        }
    }

    public void watchContentChanges(String path, double threshold) {
        try {
            Path normalizedPath = validateAndNormalizePath(path);
            
            Path parentDir = normalizedPath.getParent();
            if (parentDir == null) {
                throw new SecurityException("Invalid file path: " + path);
            }
            Path fileName = normalizedPath.getFileName();
            
            String originalContent = new String(Files.readAllBytes(normalizedPath), StandardCharsets.UTF_8);
            
            String relativeParentDir = FileAccessManager.getMinecraftDir()
                .relativize(parentDir)
                .toString()
                .replace('\\', '/');
            
            watchDirectory(relativeParentDir, changedPath -> {
                try {
                    if (changedPath.getFileName().equals(fileName) && Files.exists(changedPath)) {
                        String newContent = new String(Files.readAllBytes(changedPath), StandardCharsets.UTF_8);
                        double similarity = calculateSimilarity(originalContent, newContent);
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
                    }
                } catch (IOException e) {
                    Filesjs.LOGGER.error("Error checking content changes: " + path, e);
                }
            });
        } catch (SecurityException e) {
            Filesjs.LOGGER.error("Security violation in setting up content watch: " + path, e);
            throw e;
        } catch (IOException e) {
            Filesjs.LOGGER.error("Error setting up content watch: " + path, e);
            throw new RuntimeException("Failed to set up content watch: " + path, e);
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

    private Object currentTickListener = null;

    public void scheduleBackup(String path, int ticks) {
        try {
            Path normalizedPath = validateAndNormalizePath(path);
            
            if (ticks == 0) {
                doBackup(normalizedPath.toString());
                return;
            }

            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                if (currentTickListener != null) {
                    net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(currentTickListener);
                }

                final int[] tickCounter = {0};
                
                Object listener = new Object() {
                    @net.minecraftforge.eventbus.api.SubscribeEvent
                    public void onServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
                        if (event.phase == net.minecraftforge.event.TickEvent.Phase.END) {
                            tickCounter[0]++;
                            if (tickCounter[0] >= ticks) {
                                try {
                                    doBackup(normalizedPath.toString());
                                } catch (Exception e) {
                                    Filesjs.LOGGER.error("Error during scheduled backup: " + path, e);
                                }
                                net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(this);
                                currentTickListener = null;
                            }
                        }
                    }
                };

                currentTickListener = listener;
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(listener);
            }
        } catch (SecurityException e) {
            Filesjs.LOGGER.error("Security violation in scheduling backup: " + path, e);
            throw e;
        }
    }

    private void doBackup(String path) {
        try {
            Path sourcePath = validateAndNormalizePath(path);
            if (!Files.exists(sourcePath)) {
                throw new IOException("Source file does not exist: " + path);
            }

            Path backupDir = Paths.get("kubejs/backups");
            Files.createDirectories(backupDir);

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = sourcePath.getFileName().toString();
            String backupName = fileName + "." + timestamp + ".backup";
            
            Path backupPath = backupDir.resolve(backupName);
            validateAndNormalizePath(backupPath.toString());

            Files.copy(sourcePath, backupPath, StandardCopyOption.REPLACE_EXISTING);

            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            ServerLevel level = server.overworld();
            FilesJSPlugin.FILE_BACKUP_CREATED.post(new FileEventJS(
                backupPath.toString(),
                null,
                "backup_created",
                null,
                server,
                level
            ));

            cleanupOldBackups(backupDir, 5);
        } catch (SecurityException e) {
            Filesjs.LOGGER.error("Security violation in backup operation: " + path, e);
            throw e;
        } catch (IOException e) {
            Filesjs.LOGGER.error("Error creating backup: " + path, e);
            throw new RuntimeException("Failed to create backup: " + path, e);
        }
    }

    private void cleanupOldBackups(Path backupDir, int keepCount) throws IOException {
        try {
            if (!Files.exists(backupDir)) return;

            List<Path> backups = Files.list(backupDir)
                .filter(path -> {
                    try {
                        validateAndNormalizePath(path.toString());
                        return path.toString().endsWith(".backup");
                    } catch (SecurityException e) {
                        Filesjs.LOGGER.warn("Skipping unsafe backup path: " + path);
                        return false;
                    }
                })
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
                    try {
                        validateAndNormalizePath(backup.toString());
                        Files.delete(backup);
                    } catch (SecurityException e) {
                        Filesjs.LOGGER.warn("Security violation when deleting old backup: " + backup);
                    }
                }
            }
        } catch (SecurityException e) {
            Filesjs.LOGGER.error("Security violation in cleanup operation", e);
            throw e;
        }
    }

    private FileEventJS createFileEvent(String path, String content, String type) {
        try {
            Path normalizedPath = validateAndNormalizePath(path);
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            ServerLevel level = server.overworld();
            return new FileEventJS(normalizedPath.toString(), content, type, null, server, level);
        } catch (SecurityException e) {
            Filesjs.LOGGER.error("Security violation creating event: " + path, e);
            throw e;
        }
    }
} 