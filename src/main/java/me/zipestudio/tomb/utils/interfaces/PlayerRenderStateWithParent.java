package me.zipestudio.tomb.utils.interfaces;

import net.minecraft.world.entity.player.Player;

@SuppressWarnings("java:S100")
public interface PlayerRenderStateWithParent {

    void tomb$setEntity(Player entity);
    Player tomb$getEntity();

}