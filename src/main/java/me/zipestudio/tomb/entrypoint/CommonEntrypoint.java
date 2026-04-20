package me.zipestudio.tomb.entrypoint;

//? if fabric {

import me.zipestudio.tomb.TOMBServer;

import net.fabricmc.api.ModInitializer;

public class CommonEntrypoint implements ModInitializer {

	@Override
	public void onInitialize() {
		TOMBServer.onInitialize();
	}

}

//?} elif neoforge {
/*import me.zipestudio.tomb.TOMBServer;
import net.neoforged.fml.common.Mod;

@Mod(TOMBServer.MOD_ID)
public class CommonEntrypoint {

	public CommonEntrypoint() {
		TOMBServer.onInitialize();
	}

}

*///?}