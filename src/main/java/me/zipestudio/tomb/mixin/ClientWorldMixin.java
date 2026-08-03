package me.zipestudio.tomb.mixin;

import me.zipestudio.tomb.client.TOMBClient;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
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
        if (entity instanceof Player player) {
            TOMBClient.HELD_TOOLS.remove(player.getUUID());
            return;
        }
        // A projectile names its thrower outright, so it needs no guessing at all: the spawn packet's spare
        // data int is the owner's entity id and Projectile.recreateFromPacket resolves it into setOwner
        // before this runs. Proximity would not work for these anyway — a thrown trident covers about two
        // and a half blocks per tick, so by the time the packet lands it is nowhere near its thrower.
        if (entity instanceof Projectile projectile) {
            tomb$noteProjectile(projectile);
            return;
        }

        if (!(entity instanceof ItemEntity item)) return;
        ItemStack leaving = item.getItem();
        if (leaving.isEmpty()) return;

        // Only the player this client controls runs Inventory.removeFromSelected or Player.drop, so a
        // remote player throwing something reaches this client as nothing but an emptied hand — which is
        // indistinguishable from switching to an empty slot, and would put the tool on their back instead
        // of taking it off. Neither entity names who released it, so the stack spawning against someone is
        // the trace to go on: it is created on the thrower's own eye line.
        //
        // No filtering is needed here. HeldItemContext only reacts to a dropped stack that matches what
        // that player holds or already wears on their back, so anything nobody threw passes straight
        // through — an arrow leaving a bow included, since its stack is the arrow and not the bow.
        for (Player nearby : entity.level().getEntitiesOfClass(Player.class, entity.getBoundingBox().inflate(1.0D))) {
            TOMBClient.HELD_TOOLS.computeIfPresent(nearby.getUUID(), (uuid, ctx) -> {
                ctx.markItemDropped(leaving.copy());
                return ctx;
            });
        }
    }

    /**
     * Takes the thrown item off its owner's back. A thrown trident never becomes an ItemEntity — it flies
     * as an arrow, carrying the item with it — so without this, throwing one in survival parks it on the
     * thrower's back instead.
     *
     * <p>The stack is matched by item alone and then the owner's *own* stack is what gets recorded: a
     * projectile rebuilt from a spawn packet carries a default pickup stack, not the enchanted trident that
     * was actually thrown, so comparing components against it would never line up. Matching by item is
     * also what keeps a bow safe — an arrow's stack is the arrow, never the weapon that fired it.
     */
    @Unique
    private static void tomb$noteProjectile(Projectile projectile) {
        if (!(projectile.getOwner() instanceof Player owner)) return;

        ItemStack flying = tomb$projectileItem(projectile);
        if (flying.isEmpty()) return;

        TOMBClient.HELD_TOOLS.computeIfPresent(owner.getUUID(), (uuid, ctx) -> {
            if (!ctx.activeMain.isEmpty() && ctx.activeMain.getItem() == flying.getItem()) {
                ctx.markItemDropped(ctx.activeMain.copy());
            }
            return ctx;
        });
    }

    /** The item a projectile was made from, or empty for ones that carry none. */
    @Unique
    private static ItemStack tomb$projectileItem(Projectile projectile) {
        //? if >=1.21.11 {
        if (projectile instanceof net.minecraft.world.entity.projectile.arrow.AbstractArrow arrow) {
            return arrow.getPickupItemStackOrigin();
        }
        //?} else {
        /*if (projectile instanceof net.minecraft.world.entity.projectile.AbstractArrow arrow) {
            return arrow.getPickupItemStackOrigin();
        }
        *///?}
        return ItemStack.EMPTY;
    }

}
