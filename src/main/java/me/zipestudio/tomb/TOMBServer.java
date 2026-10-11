package me.zipestudio.tomb;

import net.minecraft.network.chat.*;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TOMBServer {

	public static final String MOD_NAME = /*$ mod_name*/ "Tools On My Back (TOMB)";
	public static final String MOD_ID = /*$ mod_id*/ "tools_on_my_back";

	public static Logger LOGGER = LoggerFactory.getLogger(TOMBServer.MOD_NAME);

	public static Identifier id(String path) {
		//? if >=1.21 {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
		//?} else {
		/*return Identifier.tryBuild(MOD_ID, path);
		 *///?}
	}

	public static Identifier parseId(String path) {
		//? if >=1.21 {
		return Identifier.parse(path);
		//?} else {
		/*return new Identifier(path);
		 *///?}
	}

	public static MutableComponent text(String path, Object... args) {
		return Component.translatable(String.format("%s.%s", MOD_ID, path), args);
	}

	public static void onInitialize() {
		LOGGER.info("{} Initialized", MOD_NAME);
	}

}