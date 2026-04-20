package me.zipestudio.tomb.mixin;

//? if >=1.21.2 {

import me.zipestudio.tomb.utils.interfaces.PlayerRenderStateWithParent;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

//? if >=1.21.9 {
/*import net.minecraft.client.renderer.entity.state.AvatarRenderState;
@Mixin(AvatarRenderState.class)
*///?} else {
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
@Mixin(PlayerRenderState.class)
//?}

public class AvatarRenderStateMixin implements PlayerRenderStateWithParent {

    @Unique
    private Player entity;

    @Override
    public void tomb$setEntity(Player entity) {
        if (entity == null) return;
        this.entity = entity;
    }

    @Override
    public Player tomb$getEntity() {
        return this.entity;
    }

}

//?}