package me.zipestudio.tomb.mixin;

import me.zipestudio.tomb.client.TOMBClient;
import me.zipestudio.tomb.client.HeldItemContext;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;
@Mixin(Player.class)
public abstract class PlayerTickMixin {

    @Inject(at = @At("HEAD"), method = "tick")
    public void tick(CallbackInfo ci) {

        final Player player = (Player) (Object) this;

        boolean isClient = player.level().isClientSide();

        if (!isClient) return;

        UUID uuid = player.getUUID();
        if (!player.isAlive() || player.isDeadOrDying()) {
            TOMBClient.HELD_TOOLS.remove(uuid);
        } else {
            final HeldItemContext ctx = TOMBClient.HELD_TOOLS.computeIfAbsent(
                    uuid, v -> new HeldItemContext()
            );
            ctx.tick(player.getMainHandItem().copy());
        }
    }

}