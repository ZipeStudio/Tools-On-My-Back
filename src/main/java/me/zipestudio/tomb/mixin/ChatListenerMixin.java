package me.zipestudio.tomb.mixin;

import com.mojang.authlib.GameProfile;
import me.zipestudio.tomb.client.ProfileShare;
import me.zipestudio.tomb.client.TOMBClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.PlayerChatMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatListener.class)
public class ChatListenerMixin {

    @Inject(method = "handlePlayerChatMessage", at = @At("HEAD"), cancellable = true)
    private void tomb$sharedProfile(PlayerChatMessage message, GameProfile sender, ChatType.Bound boundChatType,
                                    CallbackInfo ci) {
        String raw = message.signedContent();
        MutableComponent rewritten = ProfileShare.rewrite(raw);
        if (rewritten == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        ClientPacketListener connection = minecraft.getConnection();
        if (connection != null && message.signature() != null) {
            //? if >=1.21.5 {
            connection.markMessageAsProcessed(message.signature(), true);
            //?} else {
            /*connection.markMessageAsProcessed(message, true);
            *///?}
        }
        TOMBClient.tell(boundChatType.decorate(rewritten));
        ci.cancel();
    }

    @Inject(method = "handleSystemMessage", at = @At("HEAD"), cancellable = true)
    private void tomb$sharedProfileSystem(Component message, boolean remote, CallbackInfo ci) {
        //? if <26.1 {
        /*if (remote) return;
        *///?}
        Component rewritten = ProfileShare.rewriteComponent(message);
        if (rewritten == null) return;
        TOMBClient.tell(rewritten);
        ci.cancel();
    }

    @Inject(method = "handleDisguisedChatMessage", at = @At("HEAD"), cancellable = true)
    private void tomb$sharedProfileDisguised(Component message, ChatType.Bound boundChatType, CallbackInfo ci) {
        Component rewritten = ProfileShare.rewriteComponent(message);
        if (rewritten == null) return;
        TOMBClient.tell(boundChatType.decorate(rewritten));
        ci.cancel();
    }
}
