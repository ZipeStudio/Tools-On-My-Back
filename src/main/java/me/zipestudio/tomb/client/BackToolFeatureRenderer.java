package me.zipestudio.tomb.client;

//? if >=1.21.9 {
/*import net.fabricmc.fabric.impl.client.indigo.renderer.render.ItemRenderContext;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.ItemRenderState;
*///?}

import me.zipestudio.tomb.config.ConfigHandler;
import me.zipestudio.tomb.utils.interfaces.PlayerRenderStateWithParent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.feature.PlayerHeldItemFeatureRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.Arm;
import net.minecraft.util.math.RotationAxis;

public class BackToolFeatureRenderer /*? >=1.21.4 {*/ <M extends PlayerEntityModel> extends PlayerHeldItemFeatureRenderer<PlayerEntityRenderState, M> /*?} else {*/ /*extends HeldItemFeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> *//*?}*/ {

    //? if >=1.21.4 {
    public BackToolFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, M> context) {
        super(context);
    }
    //?} else if >=1.21.2 {
    /*public BackToolFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context, ItemRenderer itemRenderer) {
        super(context, itemRenderer);
    }
    *///?}

    //? if >=1.21.9 {
    /*@Override
    protected void renderItem(PlayerEntityRenderState playerState, ItemRenderState itemRenderState, Arm arm, MatrixStack matrices, OrderedRenderCommandQueue commandQueue, int light) {

        PlayerEntity playerEntity = ((PlayerRenderStateWithParent) playerState).tomb$getEntity();
        if (playerEntity == null) return;

        HeldItemContext ctx = TOMBClient.HELD_TOOLS.get(playerEntity.getUuid());
        if (ctx == null || playerState.invisible) return;

        if (playerState.capeVisible && playerState.skinTextures.cape() != null && !ConfigHandler.shouldRenderWithCape())
            return;
        if (playerState.sleepingDirection != null) return;
        if (shouldSkipRendering(playerState, ctx)) return;

        ItemStack stack = ctx.previousMain;
        if (stack.isEmpty()) return;

        matrices.push();
        this.getContextModel().body.applyTransform(matrices);

        float chestOffset = !playerState.equippedChestStack.isEmpty()
                ? 1.2F
                : (playerState.jacketVisible ? 0.50F : 0.3F);

        renderStack(playerState, matrices, commandQueue, light, playerState.mainArm, stack, chestOffset);
        matrices.pop();
    }
    *///?} else {
    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider provider, int light,
                       PlayerEntityRenderState playerState, float limbAngle, float limbDistance) {

        PlayerEntity playerEntity = ((PlayerRenderStateWithParent) playerState).tomb$getEntity();
        if (playerEntity == null) return;

        HeldItemContext ctx = TOMBClient.HELD_TOOLS.get(playerEntity.getUuid());
        if (ctx == null || playerState.invisible) return;

        if (playerState.capeVisible && playerState.skinTextures.capeTexture() != null && !ConfigHandler.shouldRenderWithCape())
            return;

        if (playerState.sleepingDirection != null)
            return;

        if (shouldSkipRendering(playerState, ctx))
            return;

        ItemStack stack = ctx.previousMain;
        if (stack.isEmpty())
            return;

        matrices.push();

        //? if >=1.21.5 {
        this.getContextModel().body.applyTransform(matrices);
         //?} else {
        /*this.getContextModel().body.rotate(matrices);
        *///?}

        float chestOffset = !playerState.equippedChestStack.isEmpty()
                ? 1.2F
                : (playerState.jacketVisible ? 0.50F : 0.3F);

        renderStack(matrices, provider, light, playerState.mainArm, stack, chestOffset);
        matrices.pop();
    }
    //?}

    //? >=1.21.9 {
    /*private void renderStack(PlayerEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue commandQueue, int light,
                             Arm mainArm, ItemStack stack, float chestOffset) {
    *///?} else {
    private void renderStack(MatrixStack matrices, VertexConsumerProvider provider, int light,
                             Arm mainArm, ItemStack stack, float chestOffset) {
        //?}

        matrices.push();

        matrices.translate(0F, 4F / 16F, 2F / 16F + (chestOffset / 16F));
        matrices.translate(0F, 0F, 0.025F);

        Item item = stack.getItem();

        float scale = ConfigHandler.getToolScale(item);
        float angle = ConfigHandler.getToolAngle(item);
        float[] offset = ConfigHandler.getToolOffset(item);

        matrices.scale(scale, scale, scale);
        matrices.translate(offset[0], offset[1], offset[2]);

        if (mainArm == Arm.RIGHT) {
            matrices.scale(-1F, 1F, -1F);
        }

        if (ConfigHandler.hasTool3DModel(item)) {
            if (mainArm == Arm.LEFT) {
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180F));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle));
            } else {
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-angle));
            }
        } else {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle));
        }

        //? if >=1.21.9 {
        /*MinecraftClient.getInstance().getEntityRenderDispatcher().getHeldItemRenderer().renderItem(
                ((PlayerRenderStateWithParent) state).tomb$getEntity(),
                stack,
                ItemDisplayContext.FIXED,
                matrices,
                commandQueue,
                light
        );
        *///?} else >=1.21.5 {
        MinecraftClient.getInstance().getItemRenderer().renderItem(
                stack, ItemDisplayContext.FIXED, light, OverlayTexture.DEFAULT_UV,
                matrices, provider, null, 0
        );
        //?} else {
        /*MinecraftClient.getInstance().getItemRenderer().renderItem(
                stack, ModelTransformationMode.FIXED, light, OverlayTexture.DEFAULT_UV,
                matrices, provider, null, 0
        );
        *///?}

        matrices.pop();
    }

    private boolean shouldSkipRendering(PlayerEntityRenderState player, HeldItemContext ctx) {
        if (!ConfigHandler.shouldRenderWithElytra())
            return false;

        return player.equippedChestStack.getItem() == Items.ELYTRA;
    }

}