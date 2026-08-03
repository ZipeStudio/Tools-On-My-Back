package me.zipestudio.tomb.config;

import lombok.*;
import me.zipestudio.tomb.TOMBServer;
import me.zipestudio.tomb.utils.CodecUtils;
import me.zipestudio.tomb.utils.ConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import static me.zipestudio.tomb.utils.CodecUtils.option;

@Getter
@Setter
@AllArgsConstructor
public class LeafyConfig {

	public static final Codec<ToolEntry> TOOL_ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
			// what item this entry targets
			Codec.STRING.fieldOf("itemId").forGetter(ToolEntry::getItemId),
			Codec.STRING.fieldOf("components").orElse("").forGetter(ToolEntry::getComponents),
			// Which captured components take part in matching. Default off: an entry then follows the item
			// itself, which is the behaviour to fall back on when a config predates these fields.
			// NOTE: this group is at 16 entries, RecordCodecBuilder's hard maximum (Products.P16). Any
			// further field has to move into a nested record with its own codec.
			Codec.BOOL.fieldOf("matchName").orElse(false).forGetter(ToolEntry::isMatchName),
			Codec.BOOL.fieldOf("matchModelData").orElse(false).forGetter(ToolEntry::isMatchModelData),
			Codec.BOOL.fieldOf("matchItemModel").orElse(false).forGetter(ToolEntry::isMatchItemModel),
			Codec.BOOL.fieldOf("applyToSimilar").orElse(false).forGetter(ToolEntry::isApplyToSimilar),
			// behaviour flags
			Codec.BOOL.fieldOf("enabled").orElse(true).forGetter(ToolEntry::isEnabled),
			Codec.BOOL.fieldOf("flip").orElse(false).forGetter(ToolEntry::isFlip),
			Codec.STRING.fieldOf("displayContext").orElse("fixed").forGetter(ToolEntry::getDisplayContext),
			// transform
			Codec.FLOAT.fieldOf("scale").orElse(1f).forGetter(ToolEntry::getScale),
			Codec.FLOAT.fieldOf("angleX").orElse(0f).forGetter(ToolEntry::getAngleX),
			Codec.FLOAT.fieldOf("angleY").orElse(0f).forGetter(ToolEntry::getAngleY),
			Codec.FLOAT.fieldOf("angleZ").orElse(0f).forGetter(ToolEntry::getAngleZ),
			Codec.FLOAT.fieldOf("offsetX").orElse(0f).forGetter(ToolEntry::getOffsetX),
			Codec.FLOAT.fieldOf("offsetY").orElse(0f).forGetter(ToolEntry::getOffsetY),
			Codec.FLOAT.fieldOf("offsetZ").orElse(0f).forGetter(ToolEntry::getOffsetZ)
	).apply(instance, ToolEntry::new));

	public static final Codec<LeafyConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			option("enableMod", true, Codec.BOOL, LeafyConfig::isEnableMod),
			option("renderWithCape", true, Codec.BOOL, LeafyConfig::isRenderWithCape),
			option("renderWithElytra", true, Codec.BOOL, LeafyConfig::isRenderWithElytra),
			Codec.list(TOOL_ENTRY_CODEC).fieldOf("tools").orElse(new ArrayList<>()).forGetter(LeafyConfig::getTools)
	).apply(instance, LeafyConfig::new));

	/**
	 * The order the file is written in. A codec hands its fields to the ops in the order its builder's map
	 * happens to iterate, not the order they are declared, so without this the saved config comes out
	 * shuffled — transform values before the item they belong to, and no two saves comparable by eye.
	 */
	public static final List<String> KEY_ORDER = List.of(
			"enableMod", "renderWithCape", "renderWithElytra", "tools",
			"itemId", "components", "matchName", "matchModelData", "matchItemModel", "applyToSimilar",
			"enabled", "flip", "displayContext",
			"scale", "angleX", "angleY", "angleZ", "offsetX", "offsetY", "offsetZ");

	private static final Logger LOGGER = LoggerFactory.getLogger(TOMBServer.MOD_NAME + "/Config");

	private static final File CONFIG_FILE = getConfigDir().resolve(TOMBServer.MOD_ID + ".json5").toFile();
	private static LeafyConfig INSTANCE;

	private boolean enableMod;
	private boolean renderWithCape;
	private boolean renderWithElytra;
	private List<ToolEntry> tools;

	private LeafyConfig() { throw new IllegalArgumentException(); }

	public static LeafyConfig getInstance() {
		return INSTANCE == null ? reload() : INSTANCE;
	}

	public static LeafyConfig reload() {
		return INSTANCE = LeafyConfig.read();
	}

	public static LeafyConfig getNewInstance() {
		return CodecUtils.parseNewInstanceHacky(CODEC);
	}

	private static LeafyConfig read() {
		return ConfigUtils.readConfig(CODEC, CONFIG_FILE, LOGGER);
	}

	public void saveAsync() {
		CompletableFuture.runAsync(this::save);
	}

	public void save() {
		// What a filter is switched off for is not part of the entry, so it has no business being in the
		// file: a captured plugin name or item_model the entry ignores is dead weight that also leaks into
		// anything shared from it.
		for (ToolEntry entry : getTools()) ConfigHandler.pruneComponents(entry);
		ConfigUtils.saveConfig(this, CODEC, CONFIG_FILE, LOGGER, KEY_ORDER);
	}

	//? if fabric {
	public static Path getConfigDir() {
		return net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir();
	}
	//?} else if neoforge {
	/*public static Path getConfigDir() {
		return net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get();
	}
	*///?}

}