package me.zipestudio.tomb.client;

import lombok.Getter;
import me.zipestudio.tomb.TOMBServer;
import org.slf4j.*;


import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public class TOMBClient {

	@Getter
	public static final Map<UUID, HeldItemContext> HELD_TOOLS = new WeakHashMap<>();

	public static Logger LOGGER = LoggerFactory.getLogger(TOMBServer.MOD_NAME + "/Client");

	public static void onInitializeClient() {
		LOGGER.info("{} Client Initialized", TOMBServer.MOD_NAME);
	}

}
