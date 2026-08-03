package me.zipestudio.tomb.client;

//? if >=1.21.11 {
import me.zipestudio.tomb.config.ConfigHandler;
import me.zipestudio.tomb.utils.interfaces.PlayerRenderStateWithParent;
import net.minecraft.client.model.player.PlayerModel;
//?} else {
/*import net.minecraft.client.model.PlayerModel;
 *///?}

//? if >=1.21.9 {
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
//?} else if >=1.21.2 {
/*import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
*///?} else {
/*import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.PlayerModelPart;
*///?}

//? if >=1.21.4 {
import net.minecraft.client.renderer.item.ItemStackRenderState;
//?} else {
/*import net.minecraft.client.resources.model.BakedModel;
*///?}

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
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public class BackToolFeatureRenderer
//? >=1.21.9 {
<M extends PlayerModel> extends PlayerItemInHandLayer<AvatarRenderState, M> {
//?} else if >=1.21.2 {
/*<M extends PlayerModel> extends PlayerItemInHandLayer<PlayerRenderState, M> {
*///?} else {
/*<M extends PlayerModel<AbstractClientPlayer>> extends PlayerItemInHandLayer<AbstractClientPlayer, M> {
*///?}

    //? if >=1.21.9 {
    public BackToolFeatureRenderer(RenderLayerParent<AvatarRenderState, M> context) {
        super(context);
    }
    //?} else if >=1.21.4 {
    /*public BackToolFeatureRenderer(RenderLayerParent<PlayerRenderState, M> context) {
        super(context);
    }
    *///?} else if >=1.21.2 {
    /*public BackToolFeatureRenderer(RenderLayerParent<PlayerRenderState, M> context) {
        super(context, Minecraft.getInstance().getItemRenderer());
    }
    *///?} else {
    /*public BackToolFeatureRenderer(RenderLayerParent<AbstractClientPlayer, M> context) {
        super(context, Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer());
    }
    *///?}

    //? if >=1.21.11 {
    @Override
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

        boolean preview = playerEntity.getUUID().equals(TOMBClient.PREVIEW_PLAYER) && !TOMBClient.PREVIEW_STACK.isEmpty();
        // The editor's stand-in shows the previewed entry and nothing else. It borrows the real player's
        // uuid to get their skin, so without this it falls through below and picks up whatever they are
        // really carrying — which is what left an item on the doll after every entry had been deleted.
        if (!preview && playerEntity == TOMBClient.PREVIEW_DUMMY) return;
        ItemStack stack;
        if (preview) {
            stack = TOMBClient.PREVIEW_STACK;
        } else {
            HeldItemContext ctx = TOMBClient.HELD_TOOLS.get(playerEntity.getUUID());
            if (ctx == null || state.isInvisible) return;

            if (state.showCape && state.skin.cape() != null && !ConfigHandler.shouldRenderWithCape()) {
                return;
            }

            if (state.bedOrientation != null) return;
            if (shouldSkipRendering(state)) return;

            stack = ctx.previousMain;
        }
        if (stack.isEmpty()) return;

        poseStack.pushPose();
        this.getParentModel().body.translateAndRotate(poseStack);

        float chestOffset = !state.chestEquipment.isEmpty()
                ? 1.2F
                : (state.showJacket ? 0.50F : 0.3F);

        renderStack(state, poseStack, collector, light, state.mainArm, stack, chestOffset);
        poseStack.popPose();
    }
    //?} else if >=1.21.9 {
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

        boolean preview = playerEntity.getUUID().equals(TOMBClient.PREVIEW_PLAYER) && !TOMBClient.PREVIEW_STACK.isEmpty();
        // The editor's stand-in shows the previewed entry and nothing else. It borrows the real player's
        // uuid to get their skin, so without this it falls through below and picks up whatever they are
        // really carrying — which is what left an item on the doll after every entry had been deleted.
        if (!preview && playerEntity == TOMBClient.PREVIEW_DUMMY) return;
        ItemStack stack;
        if (preview) {
            stack = TOMBClient.PREVIEW_STACK;
        } else {
            HeldItemContext ctx = TOMBClient.HELD_TOOLS.get(playerEntity.getUUID());
            if (ctx == null || state.isInvisible) return;

            if (state.showCape && state.skin.cape() != null && !ConfigHandler.shouldRenderWithCape()) {
                return;
            }

            if (state.bedOrientation != null) return;
            if (shouldSkipRendering(state)) return;

            stack = ctx.previousMain;
        }
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
    /*@Override
    public void renderArmWithItem(PlayerRenderState playerRenderState, ItemStackRenderState itemStackRenderState, HumanoidArm humanoidArm, PoseStack poseStack, MultiBufferSource multiBufferSource, int i) {

        Player playerEntity = ((PlayerRenderStateWithParent) playerRenderState).tomb$getEntity();
        if (playerEntity == null) return;

        boolean preview = playerEntity.getUUID().equals(TOMBClient.PREVIEW_PLAYER) && !TOMBClient.PREVIEW_STACK.isEmpty();
        // The editor's stand-in shows the previewed entry and nothing else. It borrows the real player's
        // uuid to get their skin, so without this it falls through below and picks up whatever they are
        // really carrying — which is what left an item on the doll after every entry had been deleted.
        if (!preview && playerEntity == TOMBClient.PREVIEW_DUMMY) return;
        ItemStack stack;
        if (preview) {
            stack = TOMBClient.PREVIEW_STACK;
        } else {
            HeldItemContext ctx = TOMBClient.HELD_TOOLS.get(playerEntity.getUUID());
            if (ctx == null || playerRenderState.isInvisible) return;

            if (playerRenderState.showCape && playerRenderState.skin.capeTexture() != null && !ConfigHandler.shouldRenderWithCape()) {
                return;
            }

            if (playerRenderState.bedOrientation != null) return;
            if (shouldSkipRendering(playerRenderState)) return;

            stack = ctx.previousMain;
        }
        if (stack.isEmpty()) return;

        poseStack.pushPose();
        this.getParentModel().body.translateAndRotate(poseStack);

        float chestOffset = !playerRenderState.chestEquipment.isEmpty()
                ? 1.2F
                : (playerRenderState.showJacket ? 0.50F : 0.3F);

        renderStack(playerRenderState, poseStack, multiBufferSource, i, playerRenderState.mainArm, stack, chestOffset);
        poseStack.popPose();
    }
    *///?} else if >=1.21.2 {
    /*@Override
    public void renderArmWithItem(PlayerRenderState playerRenderState, @Nullable BakedModel bakedModel, ItemStack itemStack, ItemDisplayContext itemDisplayContext, HumanoidArm humanoidArm, PoseStack poseStack, MultiBufferSource multiBufferSource, int i) {

        Player playerEntity = ((PlayerRenderStateWithParent) playerRenderState).tomb$getEntity();
        if (playerEntity == null) return;

        boolean preview = playerEntity.getUUID().equals(TOMBClient.PREVIEW_PLAYER) && !TOMBClient.PREVIEW_STACK.isEmpty();
        // The editor's stand-in shows the previewed entry and nothing else. It borrows the real player's
        // uuid to get their skin, so without this it falls through below and picks up whatever they are
        // really carrying — which is what left an item on the doll after every entry had been deleted.
        if (!preview && playerEntity == TOMBClient.PREVIEW_DUMMY) return;
        ItemStack stack;
        if (preview) {
            stack = TOMBClient.PREVIEW_STACK;
        } else {
            HeldItemContext ctx = TOMBClient.HELD_TOOLS.get(playerEntity.getUUID());
            if (ctx == null || playerRenderState.isInvisible) return;

            if (playerRenderState.showCape && playerRenderState.skin.capeTexture() != null && !ConfigHandler.shouldRenderWithCape()) {
                return;
            }

            if (playerRenderState.bedOrientation != null) return;
            if (shouldSkipRendering(playerRenderState)) return;

            stack = ctx.previousMain;
        }
        if (stack.isEmpty()) return;

        poseStack.pushPose();
        this.getParentModel().body.translateAndRotate(poseStack);

        float chestOffset = !playerRenderState.chestItem.isEmpty()
                ? 1.2F
                : (playerRenderState.showJacket ? 0.50F : 0.3F);

        renderStack(playerRenderState, poseStack, multiBufferSource, i, playerRenderState.mainArm, stack, chestOffset);
        poseStack.popPose();
    }
    *///?} else {
    /*@Override
    public void render(PoseStack poseStack, MultiBufferSource multiBufferSource, int light, AbstractClientPlayer playerEntity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {

        boolean preview = playerEntity.getUUID().equals(TOMBClient.PREVIEW_PLAYER) && !TOMBClient.PREVIEW_STACK.isEmpty();
        // The editor's stand-in shows the previewed entry and nothing else. It borrows the real player's
        // uuid to get their skin, so without this it falls through below and picks up whatever they are
        // really carrying — which is what left an item on the doll after every entry had been deleted.
        if (!preview && playerEntity == TOMBClient.PREVIEW_DUMMY) return;

        ItemStack stack;
        if (preview) {
            stack = TOMBClient.PREVIEW_STACK;
        } else {
            HeldItemContext ctx = TOMBClient.HELD_TOOLS.get(playerEntity.getUUID());
            if (ctx == null || playerEntity.isInvisible()) return;

            if (playerEntity.isModelPartShown(PlayerModelPart.CAPE) && playerEntity.getSkin().capeTexture() != null && !ConfigHandler.shouldRenderWithCape()) {
                return;
            }

            if (playerEntity.isSleeping()) return;
            if (shouldSkipRendering(playerEntity)) return;

            stack = ctx.previousMain;
        }
        if (stack.isEmpty()) return;

        poseStack.pushPose();
        this.getParentModel().body.translateAndRotate(poseStack);

        float chestOffset = !playerEntity.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
                ? 1.2F
                : (playerEntity.isModelPartShown(PlayerModelPart.JACKET) ? 0.50F : 0.3F);

        renderStack(playerEntity, poseStack, multiBufferSource, light, playerEntity.getMainArm(), stack, chestOffset);
        poseStack.popPose();
    }
    *///?}

    //? if >=1.21.9 {
    private void renderStack(AvatarRenderState state, PoseStack matrices, SubmitNodeCollector commandQueue,
                             int light, HumanoidArm mainArm, ItemStack stack, float chestOffset) {
        //?} else if >=1.21.2 {
    /*private void renderStack(PlayerRenderState state, PoseStack matrices, MultiBufferSource commandQueue,
                             int light, HumanoidArm mainArm, ItemStack stack, float chestOffset) {
    *///?} else {
    /*private void renderStack(AbstractClientPlayer state, PoseStack matrices, MultiBufferSource commandQueue,
                             int light, HumanoidArm mainArm, ItemStack stack, float chestOffset) {
    *///?}

        matrices.pushPose();

        matrices.translate(0F, 4F / 16F, 2F / 16F + (chestOffset / 16F));
        matrices.translate(0F, 0F, 0.025F);

        float scale = ConfigHandler.getToolScale(stack);
        float angleZ = ConfigHandler.getToolAngleZ(stack);
        float angleX = ConfigHandler.getToolAngleX(stack);
        float angleY = ConfigHandler.getToolAngleY(stack);
        float[] offset = ConfigHandler.getToolOffset(stack);

        matrices.scale(scale, scale, scale);
        matrices.translate(offset[0], offset[1], offset[2]);

        if (mainArm == HumanoidArm.RIGHT) {
            matrices.scale(-1F, 1F, -1F);
        }

        if (ConfigHandler.shouldFlip(stack)) {
            if (mainArm == HumanoidArm.LEFT) {
                matrices.mulPose(Axis.YP.rotationDegrees(180F));
                matrices.mulPose(Axis.ZP.rotationDegrees(angleZ));
            } else {
                matrices.mulPose(Axis.ZP.rotationDegrees(-angleZ));
            }
        } else {
            matrices.mulPose(Axis.ZP.rotationDegrees(angleZ));
        }

        matrices.mulPose(Axis.XP.rotationDegrees(angleX));
        matrices.mulPose(Axis.YP.rotationDegrees(angleY));

        //? if >=1.21.11 {
        if (stack.is(ItemTags.SPEARS)) {
            float yaw = (mainArm == HumanoidArm.LEFT) ? -90 : 90;
            matrices.mulPose(Axis.YP.rotationDegrees(yaw));
            matrices.mulPose(Axis.ZP.rotationDegrees((mainArm == HumanoidArm.LEFT) ? 1.6F : -1.6F));
        }
        //?}
        //? if >=1.21.5 {
        Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer().renderItem(
                ((PlayerRenderStateWithParent) state).tomb$getEntity(),
                stack,
                ConfigHandler.getDisplayContext(stack),
                matrices,
                commandQueue,
                light
        );
        //?} else if >=1.21.2 {
        /*Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer().renderItem(
                ((PlayerRenderStateWithParent) state).tomb$getEntity(),
                stack,
                ConfigHandler.getDisplayContext(stack),
                false,
                matrices,
                commandQueue,
                light
        );
        *///?} else {
        /*Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer().renderItem(
                state,
                stack,
                ConfigHandler.getDisplayContext(stack),
                false,
                matrices,
                commandQueue,
                light
        );
        *///?}
        matrices.popPose();
    }

    //? if >=1.21.9 {
    private boolean shouldSkipRendering(AvatarRenderState player) {
        if (!ConfigHandler.isModEnabled()) return true;
        if (ConfigHandler.shouldRenderWithElytra()) return false;
        return player.chestEquipment.getItem() == Items.ELYTRA;
    }
    //?} else if >=1.21.4 {
    /*private boolean shouldSkipRendering(PlayerRenderState player) {
        if (!ConfigHandler.isModEnabled()) return true;
        if (ConfigHandler.shouldRenderWithElytra()) return false;
        return player.chestEquipment.getItem() == Items.ELYTRA;
    }
    *///?} else if >=1.21.2 {
    /*private boolean shouldSkipRendering(PlayerRenderState player) {
        if (!ConfigHandler.isModEnabled()) return true;
        if (ConfigHandler.shouldRenderWithElytra()) return false;
        return player.chestItem.getItem() == Items.ELYTRA;
    }
    *///?} else {
    /*private boolean shouldSkipRendering(AbstractClientPlayer player) {
        if (!ConfigHandler.isModEnabled()) return true;
        if (ConfigHandler.shouldRenderWithElytra()) return false;
        return player.getItemBySlot(EquipmentSlot.CHEST).getItem() == Items.ELYTRA;
    }
    *///?}


}