package net.prizowo.filejs.security;

import net.minecraftforge.fml.loading.FMLPaths;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.*;

public class FileAccessManager {
    public static final Set<String> ALLOWED_SUBDIRS = new HashSet<>(Arrays.asList(
            "config",
            "saves",
            "logs",
            "mods",
            "resourcepacks",
            "kubejs",
            "scripts",
            "defaultconfigs",
            "schematics",
            "crash-reports",
            "screenshots"
    ));

    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".json",     // JSON
            ".toml",     // TOML
            ".properties", // Properties
            ".cfg",      // Cfg
            ".nbt",      // NBT
            ".mcfunction", // Minecraft Function
            ".mcmeta",   // mcmeta
            ".lang",     // lang
            ".dat",      // data
            
            ".js",       // JavaScript
            ".zs",       // ZenScript
            
            ".txt",      // TXT
            ".md",       // Markdown
            ".log",      // log
            ".backup",   // backup
            ".bak",      // backup 2
            ".zip"       // ZIP
    ));

    public static Path getMinecraftDir() {
        return FMLPaths.GAMEDIR.get();
    }

    public static void validateFileAccess(String path) throws SecurityException {
        Path minecraftDir = getMinecraftDir().normalize().toAbsolutePath();
        
        // 1. 预处理路径
        if (path == null || path.trim().isEmpty()) {
            throw new SecurityException("Access denied: Empty path");
        }
        path = path.replace('\\', '/').trim();
        
        // 2. 检查危险字符和模式
        if (path.contains("../") || path.contains("..\\") || 
            path.contains(":") || path.startsWith("/") || path.startsWith("\\") ||
            path.contains("./") || path.contains(".\\") ||
            path.contains("|") || path.contains("*") || path.contains("?") ||
            path.contains("<") || path.contains(">") || path.contains("\"")) {
            throw new SecurityException("Access denied: Invalid path characters detected: " + path);
        }
        
        // 3. 规范化路径并进行严格检查
        Path normalizedPath;
        try {
            normalizedPath = minecraftDir.resolve(path).normalize().toAbsolutePath();
        } catch (Exception e) {
            throw new SecurityException("Access denied: Invalid path format: " + path);
        }

        // 4. 确保路径在 Minecraft 目录内
        if (!normalizedPath.startsWith(minecraftDir)) {
            throw new SecurityException("Access denied: Path escapes Minecraft directory: " + path);
        }
        
        // 5. 获取并检查相对路径
        String relativePathStr;
        try {
            relativePathStr = minecraftDir.relativize(normalizedPath).toString().replace('\\', '/');
        } catch (Exception e) {
            throw new SecurityException("Access denied: Cannot relativize path: " + path);
        }

        // 6. 检查目录访问权限
        String firstDir = relativePathStr.split("/")[0];
        if (!ALLOWED_SUBDIRS.contains(firstDir)) {
            throw new SecurityException("Access denied: Directory not allowed: " + firstDir);
        }

        // 7. 检查文件扩展名（对非目录文件）
        if (!Files.isDirectory(normalizedPath)) {
            // 特殊处理 kubejs/backups 目录
            if (!relativePathStr.startsWith("kubejs/backups/")) {
                String extension = getFileExtension(relativePathStr);
                if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
                    throw new SecurityException("Access denied: File type not allowed: " + extension);
                }
            }
        }

        // 8. 检查符号链接
        try {
            if (Files.exists(normalizedPath) && Files.isSymbolicLink(normalizedPath)) {
                throw new SecurityException("Access denied: Symbolic links not allowed: " + path);
            }
        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            throw new SecurityException("Access denied: Error checking path: " + path);
        }
    }

    private static String getFileExtension(String path) {
        int lastDot = path.lastIndexOf('.');
        return lastDot > 0 ? path.substring(lastDot).toLowerCase() : "";
    }
} 