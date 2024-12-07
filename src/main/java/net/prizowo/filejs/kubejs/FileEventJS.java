package net.prizowo.filejs.kubejs;

import dev.latvian.mods.kubejs.event.EventJS;

public class FileEventJS extends EventJS {
    private final String path;
    private final String content;

    public FileEventJS(String path, String content) {
        this.path = path;
        this.content = content;
    }

    public String getPath() {
        return path;
    }

    public String getContent() {
        return content;
    }
} 