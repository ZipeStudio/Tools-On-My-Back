package me.zipestudio.tomb.client;

import lombok.Getter;
import me.zipestudio.tomb.TOMBServer;
import me.zipestudio.tomb.config.ToolEntry;
import me.zipestudio.tomb.utils.interfaces.RefreshableToast;
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

	public static UUID PREVIEW_PLAYER = null;
	public static ItemStack PREVIEW_STACK = ItemStack.EMPTY;
	public static ToolEntry PREVIEW_ENTRY = null;
	public static Player PREVIEW_DUMMY = null;

	public static final long TOAST_DISPLAY_MS = 2000L;

	private static Component lastToastBody = null;

	//? if >=1.21 {
	public static final SystemToast.SystemToastId TOMB_PERIODIC_NOTIFICATION = new SystemToast.SystemToastId(TOAST_DISPLAY_MS);
	//?} else {
	/*public static final SystemToast.SystemToastIds TOMB_PERIODIC_NOTIFICATION =
			SystemToast.SystemToastIds.PERIODIC_NOTIFICATION;
	*///?}

	public static Logger LOGGER = LoggerFactory.getLogger(TOMBServer.MOD_NAME + "/Client");

	public static void onInitializeClient() {
		LOGGER.info("{} Client Initialized", TOMBServer.MOD_NAME);
	}

	public static void tell(Component message) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null) return;
		//? if >=26.1 {
		minecraft.player.sendSystemMessage(message);
		//?} else {
		/*minecraft.player.displayClientMessage(message, false);
		*///?}
	}

	public static void toast(Component title, Component body) {
		//? if >=26.2 {
		var toasts = Minecraft.getInstance().gui.toastManager();
		//?} else if >=1.21.4 {
		/*var toasts = Minecraft.getInstance().getToastManager();
		*///?} else {
		/*var toasts = Minecraft.getInstance().getToasts();
		*///?}
		//? if >=26.2 {
		SystemToast showing = toasts.getToast(SystemToast.class, TOMB_PERIODIC_NOTIFICATION);
		if (showing != null) {
			if (!body.equals(lastToastBody)) {
				((RefreshableToast) showing).tomb$replace(title, body);
				lastToastBody = body;
			}
			return;
		}
		lastToastBody = body;
		SystemToast.add(toasts, TOMB_PERIODIC_NOTIFICATION, title, body);
		//?} else if >=1.21 {
		/*SystemToast showing = toasts.getToast(SystemToast.class, TOMB_PERIODIC_NOTIFICATION);
		if (showing != null) {
			if (!body.equals(lastToastBody)) {
				((RefreshableToast) showing).tomb$replace(title, body);
				lastToastBody = body;
			}
			return;
		}
		lastToastBody = body;
		SystemToast fresh = SystemToast.multiline(Minecraft.getInstance(), TOMB_PERIODIC_NOTIFICATION, title, body);
		((RefreshableToast) fresh).tomb$prepare(0L);
		toasts.addToast(fresh);
		*///?} else {
		/*SystemToast showing = toasts.getToast(SystemToast.class, TOMB_PERIODIC_NOTIFICATION);
		if (showing != null) {
			if (!body.equals(lastToastBody)) {
				((RefreshableToast) showing).tomb$replace(title, body);
				lastToastBody = body;
			}
			return;
		}
		lastToastBody = body;
		SystemToast fresh = SystemToast.multiline(Minecraft.getInstance(), TOMB_PERIODIC_NOTIFICATION, title, body);
		((RefreshableToast) fresh).tomb$prepare(TOAST_DISPLAY_MS);
		toasts.addToast(fresh);
		*///?}
	}

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
