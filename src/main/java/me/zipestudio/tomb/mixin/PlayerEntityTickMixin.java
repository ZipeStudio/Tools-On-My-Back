package me.zipestudio.tomb.mixin;

import me.zipestudio.tomb.client.TOMBClient;
import me.zipestudio.tomb.client.HeldItemContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityTickMixin {

    @Inject(at = @At("HEAD"), method = "tick")
    public void tick(CallbackInfo ci) {
        final PlayerEntity player = (PlayerEntity) (Object) this;

        boolean isClient =
        //? if >=1.21.9 {
        /*player.getEntityWorld().isClient();
        *///?} else {
        player.getWorld().isClient();
        //?}

        if (!isClient) return;

        UUID uuid = player.getUuid();
        if (!player.isAlive() || player.isDead()) {
            TOMBClient.HELD_TOOLS.remove(uuid);
        } else {
            final HeldItemContext ctx = TOMBClient.HELD_TOOLS.computeIfAbsent(
                    uuid, v -> new HeldItemContext()
            );
            ctx.tick(player.getMainHandStack().copy());
        }
    }

    @Inject(at = @At("HEAD"), method = "requestRespawn")
    public void onRespawn(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        TOMBClient.HELD_TOOLS.remove(player.getUuid());
    }

}