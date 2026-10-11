package me.zipestudio.tomb.mixin;

import me.zipestudio.tomb.utils.interfaces.RefreshableToast;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
//? if <26.2 {
/*import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mutable;

import java.util.List;
*///?}

@Mixin(SystemToast.class)
public class SystemToastMixin implements RefreshableToast {

    @Unique
    private static final long TOMB_LEAVE_MS = 500L;

    @Shadow
    private boolean changed;

    @Shadow
    private long lastChanged;

    //? if <26.2 {
    /*@Shadow
    private Component title;

    @Shadow
    private List<FormattedCharSequence> messageLines;

    @Shadow
    @Final
    @Mutable
    private int width;
    *///?} else {
    @Shadow
    public void reset(Component title, Component message) {
    }
    //?}

    @Unique
    private Component tomb$pendingTitle;

    @Unique
    private Component tomb$pendingBody;

    @Unique
    private long tomb$leavingAt;

    @Unique
    private long tomb$lifeMs;

    @Unique
    private long tomb$armedAt;

    @Override
    public void tomb$prepare(long lifeMillis) {
        this.tomb$lifeMs = lifeMillis;
        this.tomb$armedAt = lifeMillis > 0L ? System.currentTimeMillis() : 0L;
        //? if <26.2 {
        /*tomb$refit();
        *///?}
    }

    @Override
    public void tomb$replace(Component pendingTitle, Component pendingBody) {
        this.tomb$pendingTitle = pendingTitle;
        this.tomb$pendingBody = pendingBody;
        this.tomb$leavingAt = System.currentTimeMillis();
        this.changed = false;
        this.lastChanged = Long.MIN_VALUE / 4L;
    }

    //? if <26.2 {
    /*@Unique
    private void tomb$refit() {
        Font font = Minecraft.getInstance().font;
        int text = font.width(this.title);
        for (FormattedCharSequence line : this.messageLines) text = Math.max(text, font.width(line));
        this.width = Math.max(160, text) + 30;
    }
    *///?}

    @Unique
    private void tomb$step(double displayTimeMultiplier) {
        long now = System.currentTimeMillis();

        if (this.tomb$leavingAt != 0L) {
            if (now - this.tomb$leavingAt < TOMB_LEAVE_MS) return;

            //? if <26.2 {
            /*this.title = this.tomb$pendingTitle;
            this.messageLines = Minecraft.getInstance().font.split(this.tomb$pendingBody, 200);
            tomb$refit();
            this.changed = true;
            *///?} else {
            reset(this.tomb$pendingTitle, this.tomb$pendingBody);
            //?}
            this.tomb$pendingTitle = null;
            this.tomb$pendingBody = null;
            this.tomb$leavingAt = 0L;
            if (this.tomb$lifeMs > 0L) this.tomb$armedAt = now;
            return;
        }

        if (this.tomb$armedAt != 0L && now - this.tomb$armedAt >= this.tomb$lifeMs * displayTimeMultiplier) {
            this.tomb$armedAt = 0L;
            this.changed = false;
            this.lastChanged = Long.MIN_VALUE / 4L;
        }
    }

    //? if >=1.21.4 {
    @Inject(at = @At("HEAD"), method = "update")
    public void onUpdate(net.minecraft.client.gui.components.toasts.ToastManager p_364721_, long p_364544_,
                         org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        tomb$step(p_364721_.getNotificationDisplayTimeMultiplier());
    }
    //?} else {
    /*@Inject(at = @At("HEAD"), method = "render")
    public void onRender(net.minecraft.client.gui.GuiGraphics guiGraphics,
                         net.minecraft.client.gui.components.toasts.ToastComponent toastComponent,
                         long timeSinceLastVisible,
                         org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<
                                 net.minecraft.client.gui.components.toasts.Toast.Visibility> cir) {
        tomb$step(toastComponent.getNotificationDisplayTimeMultiplier());
    }
    *///?}

}
