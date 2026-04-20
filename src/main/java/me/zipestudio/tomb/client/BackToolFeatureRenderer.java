package me.zipestudio.tomb.client;

//? if >=1.21.11 {

/*import net.minecraft.client.model.player.PlayerModel;
        *///?} else {
import net.minecraft.client.model.PlayerModel;
 //?}

//? if >=1.21.9 {
/*import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
*///?} else {
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
//?}

//? if >=1.21.4 {
import net.minecraft.client.renderer.item.ItemStackRenderState;
//?}

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import me.zipestudio.tomb.utils.interfaces.PlayerRenderStateWithParent;
import net.minecraft.client.Minecraft;
import me.zipestudio.tomb.config.ConfigHandler;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public class BackToolFeatureRenderer /*? >=1.21.9 {*//*<M extends PlayerModel> extends PlayerItemInHandLayer<AvatarRenderState, M> *//*?} else {*/ <M extends PlayerModel> extends PlayerItemInHandLayer<PlayerRenderState, M> /*?}*/ {

    //? if >=1.21.9 {
    /*public BackToolFeatureRenderer(RenderLayerParent<AvatarRenderState, M> context) {
        super(context);
    }
    *///?} else if >=1.21.4 {
    public BackToolFeatureRenderer(RenderLayerParent<PlayerRenderState, M> context) {
        super(context);
    }
    //?} else {
    /*public BackToolFeatureRenderer(RenderLayerParent<PlayerRenderState, M> context) {
        super(context, Minecraft.getInstance().getItemRenderer());
    }
    *///?}

    //? if >=1.21.11 {
    /*@Override
    protected void submitArmWithItem(
            AvatarRenderState state,
            ItemStackRenderState itemState,
            ItemStack itemStack,
            HumanoidArm arm,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int light
    ) {
        Player playerEntity = ((PlayerRenderStateWithParent) state).tomb$getEntity();
        if (playerEntity == null) return;

        HeldItemContext ctx = TOMBClient.HELD_TOOLS.get(playerEntity.getUUID());
        if (ctx == null || state.isInvisible) return;

        if (state.showCape && state.skin.cape() != null && !ConfigHandler.shouldRenderWithCape()) {
            return;
        }

        if (state.bedOrientation != null) return;
        if (shouldSkipRendering(state)) return;

        ItemStack stack = ctx.previousMain;
        if (stack.isEmpty()) return;

        poseStack.pushPose();
        this.getParentModel().body.translateAndRotate(poseStack);

        float chestOffset = !state.chestEquipment.isEmpty()
                ? 1.2F
                : (state.showJacket ? 0.50F : 0.3F);

        renderStack(state, poseStack, collector, light, state.mainArm, stack, chestOffset);
        poseStack.popPose();
    }
    *///?} else if >=1.21.9 {
    /*@Override
    protected void submitArmWithItem(
            AvatarRenderState state,
            ItemStackRenderState itemState,
            HumanoidArm arm,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int light
    ) {

        Player playerEntity = ((PlayerRenderStateWithParent) state).tomb$getEntity();
        if (playerEntity == null) return;

        HeldItemContext ctx = TOMBClient.HELD_TOOLS.get(playerEntity.getUUID());
        if (ctx == null || state.isInvisible) return;

        if (state.showCape && state.skin.cape() != null && !ConfigHandler.shouldRenderWithCape()) {
            return;
        }

        if (state.bedOrientation != null) return;
        if (shouldSkipRendering(state)) return;

        ItemStack stack = ctx.previousMain;
        if (stack.isEmpty()) return;

        poseStack.pushPose();
        this.getParentModel().body.translateAndRotate(poseStack);

        float chestOffset = !state.chestEquipment.isEmpty()
                ? 1.2F
                : (state.showJacket ? 0.50F : 0.3F);

        renderStack(state, poseStack, collector, light, state.mainArm, stack, chestOffset);
        poseStack.popPose();
    }
    *///?} else if >=1.21.4 {
    @Override
    public void renderArmWithItem(PlayerRenderState playerRenderState, ItemStackRenderState itemStackRenderState, HumanoidArm humanoidArm, PoseStack poseStack, MultiBufferSource multiBufferSource, int i) {

        Player playerEntity = ((PlayerRenderStateWithParent) playerRenderState).tomb$getEntity();
        if (playerEntity == null) return;

        HeldItemContext ctx = TOMBClient.HELD_TOOLS.get(playerEntity.getUUID());
        if (ctx == null || playerRenderState.isInvisible) return;

        if (playerRenderState.showCape && playerRenderState.skin.capeTexture() != null && !ConfigHandler.shouldRenderWithCape()) {
            return;
        }

        if (playerRenderState.bedOrientation != null) return;
        if (shouldSkipRendering(playerRenderState)) return;

        ItemStack stack = ctx.previousMain;
        if (stack.isEmpty()) return;

        poseStack.pushPose();
        this.getParentModel().body.translateAndRotate(poseStack);

        float chestOffset = !playerRenderState.chestEquipment.isEmpty()
                ? 1.2F
                : (playerRenderState.showJacket ? 0.50F : 0.3F);

        renderStack(playerRenderState, poseStack, multiBufferSource, i, playerRenderState.mainArm, stack, chestOffset);
        poseStack.popPose();
    }
    //?} else {
    /*@Override
    public void renderArmWithItem(PlayerRenderState playerRenderState, @Nullable BakedModel bakedModel, ItemStack itemStack, ItemDisplayContext itemDisplayContext, HumanoidArm humanoidArm, PoseStack poseStack, MultiBufferSource multiBufferSource, int i) {

        Player playerEntity = ((PlayerRenderStateWithParent) playerRenderState).tomb$getEntity();
        if (playerEntity == null) return;

        HeldItemContext ctx = TOMBClient.HELD_TOOLS.get(playerEntity.getUUID());
        if (ctx == null || playerRenderState.isInvisible) return;

        if (playerRenderState.showCape && playerRenderState.skin.capeTexture() != null && !ConfigHandler.shouldRenderWithCape()) {
            return;
        }

        if (playerRenderState.bedOrientation != null) return;
        if (shouldSkipRendering(playerRenderState)) return;

        ItemStack stack = ctx.previousMain;
        if (stack.isEmpty()) return;

        poseStack.pushPose();
        this.getParentModel().body.translateAndRotate(poseStack);

        float chestOffset = !playerRenderState.chestItem.isEmpty()
                ? 1.2F
                : (playerRenderState.showJacket ? 0.50F : 0.3F);

        renderStack(playerRenderState, poseStack, multiBufferSource, i, playerRenderState.mainArm, stack, chestOffset);
        poseStack.popPose();
    }
    *///?}

    //? if >=1.21.9 {
    /*private void renderStack(AvatarRenderState state, PoseStack matrices, SubmitNodeCollector commandQueue,
                             int light, HumanoidArm mainArm, ItemStack stack, float chestOffset) {
        *///?} else {
    private void renderStack(PlayerRenderState state, PoseStack matrices, MultiBufferSource commandQueue,
                             int light, HumanoidArm mainArm, ItemStack stack, float chestOffset) {
    //?}

        matrices.pushPose();

        matrices.translate(0F, 4F / 16F, 2F / 16F + (chestOffset / 16F));
        matrices.translate(0F, 0F, 0.025F);

        Item item = stack.getItem();

        float scale = ConfigHandler.getToolScale(item);
        float angle = ConfigHandler.getToolAngle(item);
        float[] offset = ConfigHandler.getToolOffset(item);

        matrices.scale(scale, scale, scale);
        matrices.translate(offset[0], offset[1], offset[2]);

        if (mainArm == net.minecraft.world.entity.HumanoidArm.RIGHT) {
            matrices.scale(-1F, 1F, -1F);
        }

        if (ConfigHandler.hasTool3DModel(item)) {
            if (mainArm == net.minecraft.world.entity.HumanoidArm.LEFT) {
                matrices.mulPose(Axis.YP.rotationDegrees(180F));
                matrices.mulPose(Axis.ZP.rotationDegrees(angle));
            } else {
                matrices.mulPose(Axis.ZP.rotationDegrees(-angle));
            }
        } else {
            matrices.mulPose(Axis.ZP.rotationDegrees(angle));
        }

        //? if >=1.21.11 {
        /*if (item.getDefaultInstance().is(ItemTags.SPEARS)) {
            float yaw = (mainArm == net.minecraft.world.entity.HumanoidArm.LEFT) ? -90 : 90;
            matrices.mulPose(Axis.YP.rotationDegrees(yaw));
            matrices.mulPose(Axis.ZP.rotationDegrees((mainArm == net.minecraft.world.entity.HumanoidArm.LEFT) ? 1.6F : -1.6F));
        }
        *///?}

        //? if >=1.21.5 {
        /*Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer().renderItem(
                ((PlayerRenderStateWithParent) state).tomb$getEntity(),
                stack,
                ConfigHandler.getDisplayContext(item),
                matrices,
                commandQueue,
                light
        );
        *///?} else {

        //TODO: idk why this shit doesn't work on NeoForge <1.21.5

        Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer().renderItem(
                ((PlayerRenderStateWithParent) state).tomb$getEntity(),
                stack,
                ConfigHandler.getDisplayContext(item),
                false,
                matrices,
                commandQueue,
                light
        );
        //?}

        matrices.popPose();
    }

    //? if >=1.21.9 {
    /*private boolean shouldSkipRendering(AvatarRenderState player) {
        if (!ConfigHandler.isModEnabled()) return true;
        if (ConfigHandler.shouldRenderWithElytra()) return false;
        return player.chestEquipment.getItem() == Items.ELYTRA;
    }
    *///?} else if >=1.21.4 {
    private boolean shouldSkipRendering(PlayerRenderState player) {
        if (!ConfigHandler.isModEnabled()) return true;
        if (ConfigHandler.shouldRenderWithElytra()) return false;
        return player.chestEquipment.getItem() == Items.ELYTRA;
    }
    //?} else {
    /*private boolean shouldSkipRendering(PlayerRenderState player) {
        if (!ConfigHandler.isModEnabled()) return true;
        if (ConfigHandler.shouldRenderWithElytra()) return false;
        return player.chestItem.getItem() == Items.ELYTRA;
    }
    *///?}


}