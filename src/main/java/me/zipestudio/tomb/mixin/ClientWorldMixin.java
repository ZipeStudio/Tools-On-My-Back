package me.zipestudio.tomb.mixin;

import me.zipestudio.tomb.client.TOMBClient;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientWorldMixin {

    @Shadow
    @Nullable
    public abstract Entity getEntity(int i);

    @Inject(at = @At("HEAD"), method = "disconnect")
    public void onDisconnect(CallbackInfo ci) {
        TOMBClient.HELD_TOOLS.clear();
    }

    @Inject(at = @At("HEAD"), method = "removeEntity")
    public void onRemoveEntity(int i, Entity.RemovalReason removalReason, CallbackInfo ci) {
        if (!(this.getEntity(i) instanceof Player player)) return;
        TOMBClient.HELD_TOOLS.remove(player.getUUID());
    }

    @Inject(at = @At("HEAD"), method = "addEntity")
    public void onAddEntity(Entity entity, CallbackInfo ci) {
        if (!(entity instanceof Player player)) return;
        TOMBClient.HELD_TOOLS.remove(player.getUUID());
    }

}