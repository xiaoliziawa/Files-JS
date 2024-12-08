package net.prizowo.filejs;

import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingsEvent;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import net.prizowo.filejs.kubejs.FileEventJS;
import net.prizowo.filejs.kubejs.FilesWrapper;

public class FilesJSPlugin extends KubeJSPlugin {
    public static final EventGroup GROUP = EventGroup.of("Files");
    public static final EventHandler FILE_CHANGED = GROUP.server("fileChanged", () -> FileEventJS.class);
    public static final EventHandler FILE_CREATED = GROUP.server("fileCreated", () -> FileEventJS.class);
    public static final EventHandler FILE_DELETED = GROUP.server("fileDeleted", () -> FileEventJS.class);
    public static final EventHandler FILE_RENAMED = GROUP.server("fileRenamed", () -> FileEventJS.class);
    public static final EventHandler FILE_MOVED = GROUP.server("fileMoved", () -> FileEventJS.class);
    public static final EventHandler FILE_COPIED = GROUP.server("fileCopied", () -> FileEventJS.class);
    public static final EventHandler DIRECTORY_CREATED = GROUP.server("directoryCreated", () -> FileEventJS.class);
    public static final EventHandler DIRECTORY_DELETED = GROUP.server("directoryDeleted", () -> FileEventJS.class);
    public static final EventHandler FILE_ACCESS_DENIED = GROUP.server("fileAccessDenied", () -> FileEventJS.class);
    public static final EventHandler FILE_SIZE_THRESHOLD = GROUP.server("fileSizeThreshold", () -> FileEventJS.class);
    public static final EventHandler FILE_PATTERN_MATCHED = GROUP.server("filePatternMatched", () -> FileEventJS.class);
    public static final EventHandler FILE_CONTENT_CHANGED_SIGNIFICANTLY = GROUP.server("fileContentChangedSignificantly", () -> FileEventJS.class);
    public static final EventHandler FILE_BACKUP_SCHEDULED = GROUP.server("fileBackupScheduled", () -> FileEventJS.class);

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