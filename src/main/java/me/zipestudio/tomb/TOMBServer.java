package me.zipestudio.tomb;

import net.fabricmc.api.EnvType;
import net.minecraft.text.*;
import net.minecraft.util.Identifier;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TOMBServer implements ModInitializer {

    public static final String MOD_NAME = /*$ mod_name*/ "Tools On My Back";
    public static final String MOD_ID = /*$ mod_id*/ "tools-on-my-back";
    public static final String YACL_DEPEND_VERSION = /*$ yacl*/ "3.7.1+1.21.6-fabric";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    public static MutableText text(String path, Object... args) {
        return Text.translatable(String.format("%s.%s", MOD_ID, path), args);
    }

    @Override
    public void onInitialize() {
        TOMBServer.run(EnvType.SERVER, () -> LOGGER.info("You are loading {} on a server. {} is a client side-only mod!", MOD_NAME, MOD_NAME));
        TOMBServer.run(EnvType.CLIENT, () -> LOGGER.info("{} Initialized", MOD_NAME));
    }

    public static void run(EnvType type, Runnable runnable) {
        if (FabricLoader.getInstance().getEnvironmentType() == type) {
            runnable.run();
        }
    }

}