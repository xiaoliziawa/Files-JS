package net.prizowo.filejs.security;

import net.minecraftforge.fml.loading.FMLPaths;
import net.prizowo.filejs.Filesjs;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.IOException;
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

    private static final long MAX_FILE_SIZE = 1024 * 1024 * 10; // 10MB

    public static Path getMinecraftDir() {
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