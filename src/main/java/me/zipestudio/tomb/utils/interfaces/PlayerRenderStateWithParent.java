package me.zipestudio.tomb.utils.interfaces;

import net.minecraft.entity.player.PlayerEntity;

@SuppressWarnings("java:S100")
public interface PlayerRenderStateWithParent {
    void tomb$setEntity(PlayerEntity entity);
    PlayerEntity tomb$getEntity();
}