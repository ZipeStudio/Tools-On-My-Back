package me.zipestudio.tomb.mixin;

//? if >=1.21.11 {
/*import net.minecraft.client.model.player.PlayerModel;
 *///?} else {
import net.minecraft.client.model.PlayerModel;
//?}

import me.zipestudio.tomb.client.BackToolFeatureRenderer;
import me.zipestudio.tomb.utils.interfaces.PlayerRenderStateWithParent;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >=1.21.9 {

/*//? if >=1.21.11 {
/^import net.minecraft.client.model.player.PlayerModel;
^///?} else {
import net.minecraft.client.model.PlayerModel;
//?}
import net.minecraft.world.entity.Avatar;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin
        <T extends AbstractClientPlayer, S extends AvatarRenderState, M extends PlayerModel>
        extends LivingEntityRenderer<T, S, M> {

    public AvatarRendererMixin(EntityRendererProvider.Context ctx, M model, float shadowRadius) {
        super(ctx, model, shadowRadius);
    }

    @SuppressWarnings("unsafe")
    @Inject(method = "<init>", at = @At("TAIL"))
    private void addBackToolFeature(EntityRendererProvider.Context ctx, boolean slim, CallbackInfo ci) {
        this.addLayer(new BackToolFeatureRenderer(this));
    }

    @SuppressWarnings("unsafe")
    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("HEAD")
    )
    private void injectRenderState(Avatar avatar, AvatarRenderState state, float tickDelta, CallbackInfo ci) {
        if (!(avatar instanceof AbstractClientPlayer player)) return;
        ((PlayerRenderStateWithParent) state).tomb$setEntity(player);
    }

}
*///?} else {
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;

@Mixin(PlayerRenderer.class)
public abstract class AvatarRendererMixin
        <T extends AbstractClientPlayer, S extends PlayerRenderState, M extends PlayerModel>
        extends LivingEntityRenderer<T, S, M> {

    public AvatarRendererMixin(EntityRendererProvider.Context ctx, M model, float shadowRadius) {
        super(ctx, model, shadowRadius);
    }

    @SuppressWarnings("unsafe")
    @Inject(method = "<init>", at = @At("TAIL"))
    private void addBackToolFeature(EntityRendererProvider.Context ctx, boolean slim, CallbackInfo ci) {
        this.addLayer(new BackToolFeatureRenderer(this));
    }

    @SuppressWarnings("unsafe")
    @Inject(
            method = "extractRenderState(Lnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;F)V",
            at = @At("HEAD")
    )
    private void injectRenderState(AbstractClientPlayer abstractClientPlayer, PlayerRenderState state, float f, CallbackInfo ci) {
        ((PlayerRenderStateWithParent) state).tomb$setEntity(abstractClientPlayer);
    }

}
//?}