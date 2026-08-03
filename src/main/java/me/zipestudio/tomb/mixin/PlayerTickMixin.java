package me.zipestudio.tomb.mixin;

import me.zipestudio.tomb.client.TOMBClient;
import me.zipestudio.tomb.client.HeldItemContext;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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

    @Inject(
            at = @At("HEAD"),
            method = "drop(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/entity/item/ItemEntity;"
    )
    public void onDropStack(ItemStack stack, boolean includeThrowerName, CallbackInfoReturnable<ItemEntity> cir) {

        final Player player = (Player) (Object) this;

        if (!player.level().isClientSide()) return;
        if (stack.isEmpty()) return;

        TOMBClient.HELD_TOOLS.computeIfPresent(player.getUUID(), (k, ctx) -> {
            ctx.markItemDropped(stack.copy());
            return ctx;
        });
    }

}
