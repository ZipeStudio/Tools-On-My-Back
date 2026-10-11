package me.zipestudio.tomb.mixin;

import me.zipestudio.tomb.client.TOMBClient;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//? if >=26.3 {
/*import net.minecraft.util.Prediction;
import net.minecraft.world.entity.LivingEntity;
*///?}

//? if >=26.3 {
/*@Mixin(LivingEntity.class)
public abstract class ItemDropMixin {

    @Inject(
            at = @At("HEAD"),
            method = "drop(Lnet/minecraft/world/item/ItemStack;ZLnet/minecraft/util/Prediction;)Lnet/minecraft/world/entity/item/ItemEntity;"
    )
    public void onDropStack(ItemStack itemStack, boolean thrownFromHand, Prediction prediction,
                            CallbackInfoReturnable<ItemEntity> cir) {
        if ((Object) this instanceof Player player) markDropped(player, itemStack);
    }
*///?} else {
@Mixin(Player.class)
public abstract class ItemDropMixin {

    @Inject(
            at = @At("HEAD"),
            method = "drop(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/entity/item/ItemEntity;"
    )
    public void onDropStack(ItemStack itemStack, boolean thrownFromHand,
                            CallbackInfoReturnable<ItemEntity> cir) {
        markDropped((Player) (Object) this, itemStack);
    }
//?}

    @Unique
    private static void markDropped(Player player, ItemStack itemStack) {
        if (!player.level().isClientSide()) return;
        if (itemStack.isEmpty()) return;

        TOMBClient.HELD_TOOLS.computeIfPresent(player.getUUID(), (uuid, ctx) -> {
            ctx.markItemDropped(itemStack.copy());
            return ctx;
        });
    }
}
