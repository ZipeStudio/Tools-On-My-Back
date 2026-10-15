package me.zipestudio.tomb.mixin;

import me.zipestudio.tomb.client.TOMBClient;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Entity.class)
public abstract class ItemEntitySyncMixin {

    @Inject(at = @At("TAIL"), method = "onSyncedDataUpdated(Ljava/util/List;)V")
    public void onSyncedDataUpdated(List<SynchedEntityData.DataValue<?>> updatedItems, CallbackInfo ci) {

        if (!((Object) this instanceof ItemEntity item)) return;
        if (!item.level().isClientSide()) return;
        if (item.tickCount != 0) return;

        ItemStack leaving = item.getItem();
        if (leaving.isEmpty()) return;

        for (Player nearby : item.level().getEntitiesOfClass(Player.class, item.getBoundingBox().inflate(1.0D))) {
            TOMBClient.HELD_TOOLS.computeIfPresent(nearby.getUUID(), (uuid, ctx) -> {
                ctx.markItemDropped(leaving.copy());
                return ctx;
            });
        }
    }

}
