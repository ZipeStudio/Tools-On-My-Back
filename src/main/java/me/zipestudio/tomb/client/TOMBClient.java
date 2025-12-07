package me.zipestudio.tomb.client;

import lombok.Getter;
import me.zipestudio.tomb.TOMBServer;
import me.zipestudio.tomb.config.LeafyConfig;
import org.slf4j.*;

import net.fabricmc.api.ClientModInitializer;


import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public class TOMBClient implements ClientModInitializer {

	@Getter
	public static final Map<UUID, HeldItemContext> HELD_TOOLS = new WeakHashMap<>();

	public static Logger LOGGER = LoggerFactory.getLogger(TOMBServer.MOD_NAME + "/Client");

	@Override
	public void onInitializeClient() {

	}

}
