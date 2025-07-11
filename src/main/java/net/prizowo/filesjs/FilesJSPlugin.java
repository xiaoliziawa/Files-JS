package net.prizowo.filesjs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;
import net.prizowo.filesjs.kubejs.FileEventJS;
import net.prizowo.filesjs.kubejs.FilesWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilesJSPlugin implements KubeJSPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger("FilesJS");
    
    public static final EventGroup GROUP = EventGroup.of("Files");
    public static final EventHandler FILE_CREATED = GROUP.server("fileCreated", () -> FileEventJS.class);
    public static final EventHandler FILE_CHANGED = GROUP.server("fileChanged", () -> FileEventJS.class);
    public static final EventHandler FILE_DELETED = GROUP.server("fileDeleted", () -> FileEventJS.class);
    public static final EventHandler FILE_COPIED = GROUP.server("fileCopied", () -> FileEventJS.class);
    public static final EventHandler FILE_MOVED = GROUP.server("fileMoved", () -> FileEventJS.class);
    public static final EventHandler FILE_RENAMED = GROUP.server("fileRenamed", () -> FileEventJS.class);
    public static final EventHandler FILE_BACKUP_CREATED = GROUP.server("fileBackupCreated", () -> FileEventJS.class);
    public static final EventHandler FILES_MERGED = GROUP.server("filesMerged", () -> FileEventJS.class);
    public static final EventHandler FILE_WATCH_STOPPED = GROUP.server("fileWatchStopped", () -> FileEventJS.class);
    public static final EventHandler DIRECTORY_CREATED = GROUP.server("directoryCreated", () -> FileEventJS.class);
    public static final EventHandler DIRECTORY_DELETED = GROUP.server("directoryDeleted", () -> FileEventJS.class);
    public static final EventHandler FILE_CONTENT_CHANGED_SIGNIFICANTLY = GROUP.server("fileContentChangedSignificantly", () -> FileEventJS.class);

    @Override
    public void registerBindings(BindingRegistry bindings) {
        if (bindings.type().isServer()) {
            bindings.add("FilesJS", new FilesWrapper());
        }
    }

    @Override
    public void registerEvents(EventGroupRegistry registry) {
        LOGGER.info("Registering FilesJS events");
        registry.register(GROUP);
    }
}