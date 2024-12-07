package net.prizowo.filejs.kubejs;

import dev.latvian.mods.kubejs.event.EventJS;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class FileEventJS extends EventJS {
    private final String path;
    private final String content;
    private final ServerPlayer player;
    private final MinecraftServer server;
    private final ServerLevel level;

    public FileEventJS(String path, String content, ServerPlayer player, MinecraftServer server, ServerLevel level) {
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