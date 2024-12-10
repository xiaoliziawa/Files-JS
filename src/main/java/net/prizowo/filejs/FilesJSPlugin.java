package net.prizowo.filejs;

import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingsEvent;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import net.prizowo.filejs.kubejs.FileEventJS;
import net.prizowo.filejs.kubejs.FilesWrapper;

public class FilesJSPlugin extends KubeJSPlugin {
    public static final EventGroup GROUP = EventGroup.of("FilesJS");
    public static final EventHandler FILE_CREATED = GROUP.server("file_created", () -> FileEventJS.class);
    public static final EventHandler FILE_CHANGED = GROUP.server("file_changed", () -> FileEventJS.class);
    public static final EventHandler FILE_DELETED = GROUP.server("file_deleted", () -> FileEventJS.class);
    public static final EventHandler FILE_COPIED = GROUP.server("file_copied", () -> FileEventJS.class);
    public static final EventHandler FILE_MOVED = GROUP.server("file_moved", () -> FileEventJS.class);
    public static final EventHandler FILE_RENAMED = GROUP.server("file_renamed", () -> FileEventJS.class);
    public static final EventHandler FILE_BACKUP_CREATED = GROUP.server("file_backup_created", () -> FileEventJS.class);
    public static final EventHandler FILES_MERGED = GROUP.server("files_merged", () -> FileEventJS.class);
    public static final EventHandler FILE_WATCH_STOPPED = GROUP.server("file_watch_stopped", () -> FileEventJS.class);
    public static final EventHandler DIRECTORY_CREATED = GROUP.server("directory_created", () -> FileEventJS.class);
    public static final EventHandler DIRECTORY_DELETED = GROUP.server("directory_deleted", () -> FileEventJS.class);
    public static final EventHandler FILE_ACCESS_DENIED = GROUP.server("file_access_denied", () -> FileEventJS.class);
    public static final EventHandler FILE_SIZE_THRESHOLD = GROUP.server("file_size_threshold", () -> FileEventJS.class);
    public static final EventHandler FILE_PATTERN_MATCHED = GROUP.server("file_pattern_matched", () -> FileEventJS.class);
    public static final EventHandler FILE_CONTENT_CHANGED_SIGNIFICANTLY = GROUP.server("file_content_changed_significantly", () -> FileEventJS.class);

    @Override
    public void registerBindings(BindingsEvent event) {
        if (event.getType() == ScriptType.SERVER) {
            event.add("FilesJS", new FilesWrapper());
        }
    }

    @Override
    public void registerEvents() {
        Filesjs.LOGGER.info("Registering FilesJS events");
        GROUP.register();
    }
}