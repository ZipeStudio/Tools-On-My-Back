package me.zipestudio.tomb.utils.modmenu;

import me.zipestudio.tomb.TOMBServer;
import me.zipestudio.tomb.client.TOMBClient;
import me.zipestudio.tomb.client.screen.ToolEditorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class ModMenuIntegration extends AbstractModMenuIntegration {

	@Override
	protected Screen createConfigScreen(Screen parent) {
		if (Minecraft.getInstance().level == null) {

			TOMBClient.toast(TOMBServer.text("name"), TOMBServer.text("editor.no_world.body"));
			return parent;
		}
		return new ToolEditorScreen(parent);
	}

}
