package me.zipestudio.tomb.utils.modmenu;

//? if fabric {
import com.terraformersmc.modmenu.api.*;
import net.minecraft.client.gui.screens.Screen;

public abstract class AbstractModMenuIntegration implements ModMenuApi {

	// The mod's own editor is the config screen, so there is no config library to check for.
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return this::createConfigScreen;
	}

	protected abstract Screen createConfigScreen(Screen parent);
}

//?} elif neoforge {

/*import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.minecraft.client.gui.screens.Screen;

public abstract class AbstractModMenuIntegration {

	// The mod's own editor is the config screen, so there is no config library to check for.
	public void register(ModContainer container) {
		container.registerExtensionPoint(IConfigScreenFactory.class,
				(modContainer, parent) -> this.createConfigScreen(parent));
	}

	protected abstract Screen createConfigScreen(Screen parent);

}

*///?}
