package me.zipestudio.tomb.utils;

import me.zipestudio.tomb.TOMBServer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import me.zipestudio.tomb.utils.yacl.utils.SimpleContent;

import java.util.function.Function;

public class ModMenuUtils {

	public static String getOptionKey(String optionId) {
		return String.format("modmenu.option.%s", optionId);
	}

	public static String getCategoryKey(String categoryId) {
		return String.format("modmenu.category.%s", categoryId);
	}

	public static String getGroupKey(String groupId) {
		return String.format("modmenu.group.%s", groupId);
	}

	public static Text getName(String key) {
		return TOMBServer.text(key + ".name");
	}

	public static Text getDescription(String key) {
		return TOMBServer.text(key + ".description");
	}

	public static Identifier getContentId(SimpleContent content, String contentId) {
		return TOMBServer.id(String.format("textures/config/%s.%s", contentId, content.getFileExtension()));
	}

	public static Text getModTitle() {
		return TOMBServer.text("modmenu.title");
	}

	public static Function<Boolean, Text> getEnabledOrDisabledFormatter() {
		return state -> TOMBServer.text("modmenu.formatter.enabled_or_disabled." + state);
	}

	public static Text getNoConfigScreenMessage() {
		return TOMBServer.text("modmenu.no_config_library_screen.message");
	}

	public static Text getOldConfigScreenMessage(String version) {
		return TOMBServer.text("modmenu.old_config_library_screen.message", version, TOMBServer.YACL_DEPEND_VERSION);
	}
}
