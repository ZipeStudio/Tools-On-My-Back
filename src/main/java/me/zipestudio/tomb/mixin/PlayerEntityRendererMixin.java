package me.zipestudio.tomb.mixin;

import me.zipestudio.tomb.client.BackToolFeatureRenderer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin /*? >=1.21.4 {*/
        /*<T extends AbstractClientPlayerEntity,
                S extends PlayerEntityRenderState,
                M extends PlayerEntityModel>
        extends LivingEntityRenderer<T, S, M> *//*?} else {*/ <M extends PlayerEntityModel> extends LivingEntityRenderer<LivingEntity, PlayerEntityRenderState, PlayerEntityModel> /*?}*/ {

    public PlayerEntityRendererMixin(EntityRendererFactory.Context ctx, M model, float shadowRadius) {
        super(ctx, model, shadowRadius);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void addBackToolFeature(EntityRendererFactory.Context ctx, boolean slim, CallbackInfo ci) {

        //? if >=1.21.4 {
        /*this.addFeature(new BackToolFeatureRenderer(this));
        *///?} else {
        this.addFeature(new BackToolFeatureRenderer(this, ctx.getItemRenderer()));
        //?}
    }

}