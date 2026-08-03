package me.zipestudio.tomb.utils.modmenu;

import me.zipestudio.tomb.TOMBServer;
import me.zipestudio.tomb.client.TOMBClient;
import me.zipestudio.tomb.client.screen.ToolEditorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class ModMenuIntegration extends AbstractModMenuIntegration {

	/**
	 * The editor is the config screen: everything the mod has to offer, the three mod-wide switches
	 * included, lives in it. It needs a world, though — the doll is a real entity and item components
	 * resolve through the world's registries — so from the main menu it says so and stays put.
	 */
	@Override
	protected Screen createConfigScreen(Screen parent) {
		if (Minecraft.getInstance().level == null) {

			TOMBClient.toast(TOMBServer.text("name"), TOMBServer.text("editor.no_world.body"));
			return parent;
		}
		return new ToolEditorScreen(parent);
	}

}
