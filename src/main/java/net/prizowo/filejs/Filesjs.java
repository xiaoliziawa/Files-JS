package net.prizowo.filejs;

import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("filesjs")
public class Filesjs {
    public static final Logger LOGGER = LogManager.getLogger();

    public Filesjs() {
        LOGGER.info("FilesJS mod initialized!");
    }
}
