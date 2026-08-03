package me.zipestudio.tomb.mixin;

import me.zipestudio.tomb.client.ProfileShare;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Catches the click on a shared-profile chat line before vanilla turns it into a command the server would
 * only answer with "unknown command". Everything the click does happens on this client.
 *
 * <p>Where that interception has to happen moved in 1.21.11: before it, a screen resolved the click itself
 * in {@code handleComponentClicked(Style)}; from it on, run-command clicks funnel through the static
 * {@code clickCommandAction}. Both were confirmed present/absent per version with javap.
 */
//? if >=1.21.11 {
@Mixin(Screen.class)
public class ScreenClickMixin {

    @Inject(method = "clickCommandAction", at = @At("HEAD"), cancellable = true)
    private static void tomb$profileClick(LocalPlayer player, String command,
                                          Screen screen, CallbackInfo ci) {
        if (ProfileShare.handleClick(command)) ci.cancel();
    }
}
//?} else {
/*@Mixin(Screen.class)
public class ScreenClickMixin {

    @Inject(method = "handleComponentClicked", at = @At("HEAD"), cancellable = true)
    private void tomb$profileClick(Style style, CallbackInfoReturnable<Boolean> cir) {
        if (style == null || style.getClickEvent() == null) return;
        ClickEvent event = style.getClickEvent();
        if (event.getAction() != ClickEvent.Action.RUN_COMMAND) return;
        if (ProfileShare.handleClick(event.getValue())) cir.setReturnValue(true);
    }
}
*///?}
