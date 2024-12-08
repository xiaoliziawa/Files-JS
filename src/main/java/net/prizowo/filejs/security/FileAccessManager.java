package net.prizowo.filejs.security;

import net.minecraftforge.fml.loading.FMLPaths;
import net.prizowo.filejs.Filesjs;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.IOException;
import java.util.*;

public class FileAccessManager {
    private static final Set<String> ALLOWED_SUBDIRS = new HashSet<>(Arrays.asList(
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
            ".js",       // JavaScript
            ".mjs",      // ES Module
            ".cjs",      // CommonJS
            ".jsx",      // React

            ".py",       // Python
            ".pyw",      // Python Windows
            ".pyi",      // Python Interface

            ".java",     // Java Source
            ".class",    // Java Class
            ".jar",      // Java Archive

            ".html",     // HTML
            ".htm",      // HTML
            ".css",      // CSS
            ".scss",     // SASS
            ".less",     // LESS
            ".vue",

            ".json",     // JSON
            ".json5",    // JSON5
            ".yaml",     // YAML
            ".yml",      // YAML
            ".toml",     // TOML
            ".xml",      // XML
            ".csv",      // CSV
            ".tsv",      // TSV

            ".properties", // Properties
            ".conf",     // Config
            ".config",   // Config
            ".cfg",      // Config
            ".ini",      // INI

            ".lua",      // Lua
            ".rb",       // Ruby
            ".php",      // PHP
            ".pl",       // Perl
            ".sh",       // Shell
            ".bat",      // Batch
            ".ps1",      // PowerShell
            ".zs",       // ZenScript
            ".groovy",   // Groovy
            ".gradle",   // Gradle
            ".kt",       // Kotlin
            ".kts",      // Kotlin Script

            ".nbt",      // NBT
            ".mcfunction", // Minecraft Function
            ".mcmeta",   // Minecraft Metadata
            ".lang",     // Language
            ".dat",      // Data

            ".txt",      // Text
            ".md",       // Markdown
            ".log",      // Log
            ".template", // Template
            ".list",     // List

            ".sql",      // SQL
            ".r",        // R
            ".c",        // C
            ".cpp",      // C++
            ".h",        // Header
            ".hpp",      // C++ Header
            ".cs",       // C#
            ".go",       // Go
            ".rs",       // Rust
            ".ts",       // TypeScript
            ".tsx",      // TypeScript React

            // 备份文件
            ".backup",    // 备份文件
            ".bak"       // 备份文件替代扩展名
    ));

    private static final long MAX_FILE_SIZE = 1024 * 1024 * 10; // 10MB

    private static Path getMinecraftDir() {
        return FMLPaths.GAMEDIR.get();
    }

    public static void validateFileAccess(String path) throws SecurityException {
        if (path.startsWith("kubejs/backups/")) {
            return;
        }

        Path minecraftDir = getMinecraftDir();
        Path normalizedPath = Paths.get(path).normalize();
        Path absolutePath = normalizedPath.isAbsolute() ? normalizedPath : minecraftDir.resolve(normalizedPath);

        if (!absolutePath.startsWith(minecraftDir)) {
            throw new SecurityException("Access denied: Cannot access files outside Minecraft instance directory: " + path);
        }

        Path relativePath = minecraftDir.relativize(absolutePath);
        String relativePathStr = relativePath.toString().replace('\\', '/');

        if (relativePathStr.contains("..")) {
            throw new SecurityException("Access denied: Parent directory traversal not allowed");
        }

        boolean allowed = ALLOWED_SUBDIRS.stream()
                .anyMatch(dir -> relativePathStr.startsWith(dir + "/") || relativePathStr.equals(dir));

        if (!allowed) {
            throw new SecurityException("Access denied: Directory not allowed: " + path + "\nAllowed directories: " + String.join(", ", ALLOWED_SUBDIRS));
        }

        if (!relativePathStr.startsWith("kubejs/backups/")) {
            boolean validExtension = ALLOWED_EXTENSIONS.stream()
                    .anyMatch(ext -> relativePathStr.toLowerCase().endsWith(ext));
            if (!validExtension && !Files.isDirectory(absolutePath)) {
                throw new SecurityException("Access denied: File type not allowed: " + path + "\nAllowed extensions: " + String.join(", ", ALLOWED_EXTENSIONS));
            }
        }
    }

    public static void validateFileSize(Path path) throws SecurityException, IOException {
        if (Files.exists(path) && !Files.isDirectory(path) && Files.size(path) > MAX_FILE_SIZE) {
            throw new SecurityException(String.format("File size exceeds limit (max %.2fMB): %s", MAX_FILE_SIZE / 1024.0 / 1024.0, path));
        }
    }

    public static void validateContentSize(String content) throws SecurityException {
        if (content != null && content.length() > MAX_FILE_SIZE) {
            throw new SecurityException(String.format("Content size exceeds limit (max %.2fMB)", MAX_FILE_SIZE / 1024.0 / 1024.0));
        }
    }

    public static boolean isScriptThread() {
        return Arrays.stream(Thread.currentThread().getStackTrace())
                .anyMatch(element -> element.getClassName().contains("rhino") ||
                        element.getClassName().contains("script"));
    }

    public static void logSecurityViolation(String message, String path) {
        Filesjs.LOGGER.warn("Security violation: {} (Path: {})", message, path);
        if (isScriptThread()) {
            Filesjs.LOGGER.warn("Violation from script execution");
        }
    }
} 