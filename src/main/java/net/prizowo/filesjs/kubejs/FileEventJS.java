package net.prizowo.filesjs.kubejs;

import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class FileEventJS implements KubeEvent {
    private final String path;
    private final String content;
    private final String type;
    private final ServerPlayer server;
    private final ServerLevel level;

    public FileEventJS(String path, String content, String type, ServerPlayer server, MinecraftServer minecraftServer, ServerLevel level) {
        this.path = path;
        this.content = content;
        this.type = type;
        this.server = server;
        this.level = level;
    }

    public String getPath() {
        return path;
    }

    public String getContent() {
        return content;
    }

    public String getType() {
        return type;
    }

    public ServerPlayer getServer() {
        return server;
    }

    public ServerLevel getLevel() {
        return level;
    }
} 