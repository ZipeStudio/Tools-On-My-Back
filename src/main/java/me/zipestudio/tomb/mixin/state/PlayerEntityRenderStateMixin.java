package me.zipestudio.tomb.mixin.state;

//? >=1.21.2 {
import me.zipestudio.tomb.utils.interfaces.PlayerRenderStateWithParent;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(PlayerEntityRenderState.class)
public class PlayerEntityRenderStateMixin implements PlayerRenderStateWithParent {

    @Unique
    private PlayerEntity entity;

    @Override
    public void tomb$setEntity(PlayerEntity entity) {
        if (entity == null) return;
        this.entity = entity;
    }

    @Override
    public PlayerEntity tomb$getEntity() {
        return this.entity;
    }

}
//?}