package me.zipestudio.tomb.entrypoint;

//? if fabric {

import net.fabricmc.api.ClientModInitializer;
import me.zipestudio.tomb.client.TOMBClient;

public class ClientEntrypoint implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		TOMBClient.onInitializeClient();
	}

}

//?} elif neoforge {
/*import me.zipestudio.tomb.TOMBServer;
import me.zipestudio.tomb.client.TOMBClient;
import me.zipestudio.tomb.utils.modmenu.ModMenuIntegration;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(value = TOMBServer.MOD_ID, dist = Dist.CLIENT)
public class ClientEntrypoint {

	public ClientEntrypoint(ModContainer container) {
		TOMBClient.onInitializeClient();

		ModMenuIntegration integration = new ModMenuIntegration();
		integration.register(container);
	}

}

*///?}