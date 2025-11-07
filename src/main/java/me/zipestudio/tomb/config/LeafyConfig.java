package me.zipestudio.tomb.config;

import lombok.*;
import me.zipestudio.tomb.TOMBServer;
import me.zipestudio.tomb.utils.CodecUtils;
import me.zipestudio.tomb.utils.ConfigUtils;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import static me.zipestudio.tomb.utils.CodecUtils.option;

@Getter
@Setter
@AllArgsConstructor
public class LeafyConfig {

	public static final Codec<ToolEntry> TOOL_ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.fieldOf("itemId").forGetter(ToolEntry::getItemId),
			Codec.STRING.fieldOf("group").forGetter(ToolEntry::getGroup),
			Codec.BOOL.fieldOf("is3DModel").forGetter(ToolEntry::is3DModel),
			Codec.FLOAT.fieldOf("angle").forGetter(ToolEntry::getAngle),
			Codec.FLOAT.fieldOf("scale").orElse(1f).forGetter(ToolEntry::getScale),
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

	private static final Logger LOGGER = LoggerFactory.getLogger(TOMBServer.MOD_NAME + "/Config");

	private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve(TOMBServer.MOD_ID + ".json5").toFile();
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
		ConfigUtils.saveConfig(this, CODEC, CONFIG_FILE, LOGGER);
	}
}