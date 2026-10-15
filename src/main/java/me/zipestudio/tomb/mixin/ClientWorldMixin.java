package me.zipestudio.tomb.mixin;

import me.zipestudio.tomb.client.TOMBClient;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientWorldMixin {

    @Shadow
    @Nullable
    public abstract Entity getEntity(int id);

    @Inject(at = @At("HEAD"), method = "disconnect")
    public void onDisconnect(CallbackInfo ci) {
        TOMBClient.HELD_TOOLS.clear();
    }

    @Inject(at = @At("HEAD"), method = "removeEntity")
    public void onRemoveEntity(int id, Entity.RemovalReason reason, CallbackInfo ci) {
        if (!(this.getEntity(id) instanceof Player player)) return;
        TOMBClient.HELD_TOOLS.remove(player.getUUID());
    }

    //? if >=1.21 {
    @Inject(at = @At("HEAD"), method = "addEntity")
    public void onAddEntity(Entity entity, CallbackInfo ci) {
        tomb$noteAddedEntity(entity);
    }
    //?} else {
    /*@Inject(at = @At("HEAD"), method = "addEntity")
    public void onAddEntity(int entityId, Entity entityToSpawn, CallbackInfo ci) {
        tomb$noteAddedEntity(entityToSpawn);
    }
    *///?}

    @Unique
    private static void tomb$noteAddedEntity(Entity entity) {
        if (entity instanceof Player player) {
            TOMBClient.HELD_TOOLS.remove(player.getUUID());
            return;
        }
        if (entity instanceof Projectile projectile) {
            tomb$noteProjectile(projectile);
        }
    }

    @Unique
    private static void tomb$noteProjectile(Projectile projectile) {

        if (!(projectile.getOwner() instanceof Player owner)) return;
        ItemStack projectileItem = tomb$projectileItem(projectile);
        if (projectileItem.isEmpty()) return;

        TOMBClient.HELD_TOOLS.computeIfPresent(owner.getUUID(), (uuid, ctx) -> {
            if (!ctx.activeMain.isEmpty() && ctx.activeMain.getItem() == projectileItem.getItem()) {
                ctx.markItemDropped(ctx.activeMain.copy());
            }
            return ctx;
        });
    }

    @Unique
    private static ItemStack tomb$projectileItem(Projectile projectile) {
        //? if >=1.21.11 {
        if (projectile instanceof net.minecraft.world.entity.projectile.arrow.AbstractArrow arrow) {
            return arrow.getPickupItemStackOrigin();
        }
        //?} else if >=1.21 {
        /*if (projectile instanceof net.minecraft.world.entity.projectile.AbstractArrow arrow) {
            return arrow.getPickupItemStackOrigin();
        }
        *///?} else {
        /*if (projectile instanceof net.minecraft.world.entity.projectile.ThrownTrident) {
            return new ItemStack(net.minecraft.world.item.Items.TRIDENT);
        }
        *///?}
        return ItemStack.EMPTY;
    }

}
