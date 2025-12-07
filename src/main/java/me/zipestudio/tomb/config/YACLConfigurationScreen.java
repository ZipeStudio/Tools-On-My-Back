package me.zipestudio.tomb.config;

import dev.isxander.yacl3.api.*;
import lombok.experimental.ExtensionMethod;
import me.zipestudio.tomb.utils.ModMenuUtils;
import me.zipestudio.tomb.utils.yacl.base.SimpleCategory;
import me.zipestudio.tomb.utils.yacl.base.SimpleGroup;
import me.zipestudio.tomb.utils.yacl.base.SimpleOption;
import me.zipestudio.tomb.utils.yacl.extension.SimpleOptionExtension;
import me.zipestudio.tomb.utils.yacl.screen.SimpleYACLScreen;
import me.zipestudio.tomb.utils.yacl.utils.SimpleContent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@ExtensionMethod(SimpleOptionExtension.class)
public class YACLConfigurationScreen {

	private static final Function<Boolean, Text> ENABLED_OR_DISABLED_FORMATTER = ModMenuUtils.getEnabledOrDisabledFormatter();

	private YACLConfigurationScreen() {
		throw new IllegalStateException("Screen class");
	}

	public static Screen createScreen(Screen parent) {
		LeafyConfig defConfig = LeafyConfig.getNewInstance();
		LeafyConfig config = LeafyConfig.getInstance();

		return SimpleYACLScreen.startBuilder(parent, config::saveAsync)
				.categories(
						getGeneralCategory(defConfig, config)
//						,
//						getToolsCategory(defConfig, config)
				)
				.build();
	}

	private static ConfigCategory getGeneralCategory(LeafyConfig defConfig, LeafyConfig config) {
		return SimpleCategory.startBuilder("general")
				.options(
						SimpleOption.<Boolean>startBuilder("enableMod")
								.withBinding(defConfig.isEnableMod(), config::isEnableMod, config::setEnableMod, false)
								.withController(ENABLED_OR_DISABLED_FORMATTER)
								.withDescription(SimpleContent.NONE)
								.build(),

						SimpleOption.<Boolean>startBuilder("renderWithCape")
								.withBinding(defConfig.isRenderWithCape(), config::isRenderWithCape, config::setRenderWithCape, false)
								.withController(ENABLED_OR_DISABLED_FORMATTER)
								.withDescription(SimpleContent.NONE)
								.build(),

						SimpleOption.<Boolean>startBuilder("renderWithElytra")
								.withBinding(defConfig.isRenderWithElytra(), config::isRenderWithElytra, config::setRenderWithElytra, false)
								.withController(ENABLED_OR_DISABLED_FORMATTER)
								.withDescription(SimpleContent.NONE)
								.build()

				)
				.build();
	}

//	private static ConfigCategory getToolsCategory(LeafyConfig defConfig, LeafyConfig config) {
//
//		if (config.getTools().isEmpty()) {
//			return SimpleCategory.startBuilder("tools")
//					.groups(getToolsManagementGroup(config))
//					.build();
//		} else {
//			return SimpleCategory.startBuilder("tools")
//					.groups(
//							getToolsManagementGroup(config),
//							getToolsListGroup(defConfig, config)
//					)
//					.build();
//		}
//	}
//
//	private static OptionGroup getToolsManagementGroup(LeafyConfig config) {
//		return SimpleGroup.startBuilder("tools_management").options(
//
//				SimpleOption.startButtonBuilder("addTool", (screen, button) -> {
//							List<ToolEntry> tools = new ArrayList<>(config.getTools());
//							tools.add(new ToolEntry());
//							config.setTools(tools);
//							config.saveAsync();
//
//							screen.close();
//							MinecraftClient.getInstance().setScreen(createScreen(screen));
//						})
//						.withDescription(SimpleContent.NONE)
//						.build(),
//
//				config.getTools().isEmpty() ? null :
//						SimpleOption.startButtonBuilder("clearTools", (screen, button) -> {
//									config.setTools(new ArrayList<>());
//									config.saveAsync();
//									screen.close();
//									MinecraftClient.getInstance().setScreen(createScreen(screen));
//								})
//								.withDescription(SimpleContent.NONE)
//								.build()
//
//		).build();
//	}
//
//	private static OptionGroup getToolsListGroup(LeafyConfig defConfig, LeafyConfig config) {
//		List<Option<?>> options = new ArrayList<>();
//
//		for (int i = 0; i < config.getTools().size(); i++) {
//			final int toolIndex = i;
//			ToolEntry tool = config.getTools().get(i);
//
//			OptionGroup toolGroup = SimpleGroup.startBuilder("tool_" + i)
//					.options(
//							SimpleOption.<String>startBuilder("tool_item_" + i)
//									.withBinding(
//											tool.getItemId(),
//											() -> config.getTools().get(toolIndex).getItemId(),
//											(newId) -> updateToolField(config, toolIndex, "itemId", newId),
//											false
//									)
//									.withController()
//									.withDescription(SimpleContent.NONE)
//									.build(),
//
//							SimpleOption.<String>startBuilder("tool_group_" + i)
//									.withBinding(
//											tool.getGroup(),
//											() -> config.getTools().get(toolIndex).getGroup(),
//											(newGroup) -> updateToolField(config, toolIndex, "group", newGroup),
//											false
//									)
//									.withController()
//									.withDescription(SimpleContent.NONE)
//									.build(),
//
//							SimpleOption.<Float>startBuilder("tool_angle_" + i)
//									.withBinding(
//											tool.getAngle(),
//											() -> config.getTools().get(toolIndex).getAngle(),
//											(newAngle) -> updateToolField(config, toolIndex, "angle", newAngle),
//											false
//									)
//									.withController(-180f, 180f, 1f, true)
//									.withDescription(SimpleContent.NONE)
//									.build(),
//
//							SimpleOption.<Float>startBuilder("tool_scale_" + i)
//									.withBinding(
//											tool.getScale(),
//											() -> config.getTools().get(toolIndex).getScale(),
//											(newScale) -> updateToolField(config, toolIndex, "scale", newScale),
//											false
//									)
//									.withController(0.1f, 3f, 0.1f, true)
//									.withDescription(SimpleContent.NONE)
//									.build(),
//
//							SimpleOption.<Float>startBuilder("tool_offsetX_" + i)
//									.withBinding(
//											tool.getOffsetX(),
//											() -> config.getTools().get(toolIndex).getOffsetX(),
//											(newOffset) -> updateToolField(config, toolIndex, "offsetX", newOffset),
//											false
//									)
//									.withController(-2f, 2f, 0.1f, true)
//									.withDescription(SimpleContent.NONE)
//									.build(),
//
//							SimpleOption.<Float>startBuilder("tool_offsetY_" + i)
//									.withBinding(
//											tool.getOffsetY(),
//											() -> config.getTools().get(toolIndex).getOffsetY(),
//											(newOffset) -> updateToolField(config, toolIndex, "offsetY", newOffset),
//											false
//									)
//									.withController(-2f, 2f, 0.1f, true)
//									.withDescription(SimpleContent.NONE)
//									.build(),
//
//							SimpleOption.<Float>startBuilder("tool_offsetZ_" + i)
//									.withBinding(
//											tool.getOffsetZ(),
//											() -> config.getTools().get(toolIndex).getOffsetZ(),
//											(newOffset) -> updateToolField(config, toolIndex, "offsetZ", newOffset),
//											false
//									)
//									.withController(-2f, 2f, 0.1f, true)
//									.withDescription(SimpleContent.NONE)
//									.build(),
//
//							SimpleOption.startButtonBuilder("remove_tool_" + i, (screen, button) -> {
//										List<ToolEntry> tools = new ArrayList<>(config.getTools());
//										if (toolIndex < tools.size()) {
//											tools.remove(toolIndex);
//											config.setTools(tools);
//											config.saveAsync();
//											screen.close();
//											MinecraftClient.getInstance().setScreen(createScreen(screen));
//										}
//									})
//									.withDescription(SimpleContent.NONE)
//									.build()
//					)
//					.build();
//
//			options.addAll(toolGroup.options());
//		}
//
//		return SimpleGroup.startBuilder("tools_list")
//				.options(options.toArray(new Option[0]))
//				.build();
//	}
//
//	private static void updateToolField(LeafyConfig config, int index, String field, Object value) {
//		List<ToolEntry> tools = new ArrayList<>(config.getTools());
//		if (index >= 0 && index < tools.size()) {
//			ToolEntry oldTool = tools.get(index);
//			ToolEntry newTool = new ToolEntry(
//					field.equals("isEnabled") ? (Boolean) value : oldTool.isEnabled(),
//					field.equals("itemId") ? (String) value : oldTool.getItemId(),
//					field.equals("group") ? (String) value : oldTool.getGroup(),
//					field.equals("is3DModel") ? (Boolean) value : oldTool.is3DModel(),
//					field.equals("angle") ? (Float) value : oldTool.getAngle(),
//					field.equals("scale") ? (Float) value : oldTool.getScale(),
//					field.equals("offsetX") ? (Float) value : oldTool.getOffsetX(),
//					field.equals("offsetY") ? (Float) value : oldTool.getOffsetY(),
//					field.equals("offsetZ") ? (Float) value : oldTool.getOffsetZ()
//			);
//			tools.set(index, newTool);
//			config.setTools(tools);
//			config.saveAsync();
//		}
//	}
}