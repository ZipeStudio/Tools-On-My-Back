package me.zipestudio.tomb.mixin;

import me.zipestudio.tomb.client.TOMBClient;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Inventory.class)
public abstract class InventoryMixin {

    @Shadow
    @Final
    public Player player;

    //? if >=1.21.5 {
    @Shadow public abstract ItemStack getSelectedItem();
    //?} else {
    /*@Shadow public abstract ItemStack getSelected();
    *///?}

    @Inject(at = @At("HEAD"), method = "removeFromSelected")
    public void onSelected(boolean all, CallbackInfoReturnable<ItemStack> cir) {

        boolean isClient = player.level().isClientSide();

        if (!isClient) return;

        //? if >=1.21.5 {
        ItemStack selectedItem = this.getSelectedItem();
        //?} else {
        /*ItemStack selectedItem = this.getSelected();
        *///?}

        if (selectedItem.isEmpty()) return;

        TOMBClient.HELD_TOOLS.computeIfPresent(player.getUUID(), (k, ctx) -> {
            ctx.markItemDropped(selectedItem);
            return ctx;
        });

    }

}