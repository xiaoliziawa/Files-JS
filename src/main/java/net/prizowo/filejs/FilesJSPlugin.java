package net.prizowo.filejs;

import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingsEvent;
import net.prizowo.filejs.kubejs.FileEventJS;
import net.prizowo.filejs.kubejs.FilesWrapper;
import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;

public class FilesJSPlugin extends KubeJSPlugin {
    public static final EventGroup GROUP = EventGroup.of("Files");
    public static final EventHandler FILE_CHANGED = GROUP.server("fileChanged", () -> FileEventJS.class);

    @Override
    public void registerBindings(BindingsEvent event) {
        event.add("FilesJS", new FilesWrapper());
    }

    @Override
    public void registerEvents() {
        Filesjs.LOGGER.info("Registering FilesJS events");
        GROUP.register();
    }
}