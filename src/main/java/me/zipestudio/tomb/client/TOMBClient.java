package me.zipestudio.tomb.client;

import lombok.Getter;
import me.zipestudio.tomb.TOMBServer;
import me.zipestudio.tomb.config.ToolEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.slf4j.*;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public class TOMBClient {

	@Getter
	public static final Map<UUID, HeldItemContext> HELD_TOOLS = new WeakHashMap<>();

	// Live editor preview: while the tool editor is open, the back-tool layer renders PREVIEW_STACK on
	// PREVIEW_PLAYER regardless of what they hold, so unsaved edits show immediately. Null/empty = off.
	public static UUID PREVIEW_PLAYER = null;
	public static ItemStack PREVIEW_STACK = ItemStack.EMPTY;
	// The entry the editor currently has open. Settings are normally resolved from the stack's item id,
	// which means another entry naming the same item can answer instead — a second diamond_sword entry
	// outranks a tuned "apply to all similar" one, for instance. While previewing, that lookup is answered
	// with this entry so the doll always shows the config being edited rather than whichever one wins.
	public static ToolEntry PREVIEW_ENTRY = null;
	// The editor's stand-in entity. For the "You" subject it deliberately borrows the real player's uuid so
	// the skin matches, which also makes it pick up their live back tool — visible as an item lingering on
	// the doll after every entry has been deleted. The layer compares against this to keep the doll showing
	// the preview and nothing else. The real player, being a different object, is unaffected.
	public static Player PREVIEW_DUMMY = null;

	public static final SystemToast.SystemToastId TOMB_PERIODIC_NOTIFICATION = new SystemToast.SystemToastId(2000);

	public static Logger LOGGER = LoggerFactory.getLogger(TOMBServer.MOD_NAME + "/Client");

	public static void onInitializeClient() {
		LOGGER.info("{} Client Initialized", TOMBServer.MOD_NAME);
	}

	/**
	 * Says something to the player in chat. The one-liner for this changed hands at 26.1: Player's
	 * displayClientMessage(Component, boolean) is gone there and LocalPlayer took over with
	 * sendSystemMessage(Component). Both verified per version with javap.
	 */
	public static void tell(Component message) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null) return;
		//? if >=26.1 {
		minecraft.player.sendSystemMessage(message);
		//?} else {
		/*minecraft.player.displayClientMessage(message, false);
		*///?}
	}

	/**
	 * A toast, for saying something while a screen is open — chat and the HUD are not drawn behind one.
	 * The toast queue moved twice: Minecraft.getToasts() ≤1.21.1, getToastManager() from 1.21.4, and onto
	 * the Gui at 26.2. {@code var} keeps the differing type names out of the shared call.
	 */
	public static void toast(Component title, Component body) {
		//? if >=26.2 {
		var toasts = Minecraft.getInstance().gui.toastManager();
		//?} else if >=1.21.4 {
		/*var toasts = Minecraft.getInstance().getToastManager();
		*///?} else {
		/*var toasts = Minecraft.getInstance().getToasts();
		*///?}
		SystemToast.addOrUpdate(toasts, TOMB_PERIODIC_NOTIFICATION, title, body);
	}

	/**
	 * The screen that is open, if any. Same move as {@link #setScreen}: 26.2 made Minecraft's own field
	 * private and handed the screen to the Gui, so reading it directly stops compiling there.
	 */
	public static Screen currentScreen() {
		//? if >=26.2 {
		return Minecraft.getInstance().gui.screen();
		//?} else {
		/*return Minecraft.getInstance().screen;
		*///?}
	}

	public static void setScreen(Screen parent) {
		//? if >=26.2 {
		Minecraft.getInstance().gui.setScreen(parent);
		//?} else {
		/*Minecraft.getInstance().setScreen(parent);
		 *///?}
	}

}
