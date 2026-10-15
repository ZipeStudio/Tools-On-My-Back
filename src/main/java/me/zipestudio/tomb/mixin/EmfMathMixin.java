package me.zipestudio.tomb.mixin;

import me.zipestudio.tomb.client.TOMBClient;
import me.zipestudio.tomb.client.screen.ToolEditorScreen;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "traben.entity_model_features.models.animation.math.EMFMath", remap = false)
public class EmfMathMixin {

    @Redirect(
            method = "getFrameTime",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;isPaused()Z", remap = true),
            require = 0
    )
    private static boolean tomb$pausedForFrameTime(Minecraft minecraft) {
        return !(TOMBClient.currentScreen() instanceof ToolEditorScreen) && minecraft.isPaused();
    }

}
