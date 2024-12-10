package net.prizowo.filejs.kubejs;

import dev.latvian.mods.kubejs.event.EventJS;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.prizowo.filejs.security.FileAccessManager;

public class FileEventJS extends EventJS {
    private final String path;
    private final String content;
    private final ServerPlayer player;
    private final MinecraftServer server;
    private final ServerLevel level;

    private static final String[] ALLOWED_DIRECTORIES = {
            "kubejs",
            "config",
            "scripts"
    };

    private static boolean isPathSafe(String path) {
        try {
            FileAccessManager.validateFileAccess(path);
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }

    public FileEventJS(String path, String content, String type, ServerPlayer player, MinecraftServer server, ServerLevel level) {
        if (!isPathSafe(path)) {
            throw new SecurityException("Access denied: Unsafe path: " + path);
        }
        this.path = path;
        this.content = content;
        this.player = player;
        this.server = server;
        this.level = level;
    }

    public String getPath() {
        return path;
    }

    public String getContent() {
        return content;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public MinecraftServer getServer() {
        return server;
    }

    public ServerLevel getLevel() {
        return level;
    }
} 