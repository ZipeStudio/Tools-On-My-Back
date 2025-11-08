package me.zipestudio.tomb.mixin;

import me.zipestudio.tomb.client.TOMBClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerInventory.class)
public abstract class PlayerInventoryMixin {

    @Shadow
    @Final
    public PlayerEntity player;

    //? if >=1.21.5 {
    @Shadow
    public abstract ItemStack getSelectedStack();
    //?} else {
    /*@Shadow public abstract ItemStack getMainHandStack();
    *///?}

    @Inject(at = @At("HEAD"), method = "dropSelectedItem")
    public void onSelected(boolean entireStack, CallbackInfoReturnable<ItemStack> cir) {

        boolean isClient =
        //? if >=1.21.9 {
        /*player.getEntityWorld().isClient();
        *///?} else {
        player.getWorld().isClient();
        //?}

        if (!isClient) return;

        ItemStack selectedItem =
        //? if >=1.21.5 {
        this.getSelectedStack();
        //?} else {
        /*this.getMainHandStack();
        *///?}

        if (selectedItem.isEmpty()) return;

        TOMBClient.HELD_TOOLS.computeIfPresent(player.getUuid(), (k, ctx) -> {

            ctx.markItemDropped(selectedItem);

            return ctx;
        });

    }

}