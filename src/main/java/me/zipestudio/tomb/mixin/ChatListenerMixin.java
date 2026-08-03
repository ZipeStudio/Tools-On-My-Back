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

/**
 * Swaps any profile written into a chat line for a short clickable chip, the way waypoint-sharing mods
 * do, leaving the rest of the sentence alone — the profile is copied to the clipboard and pasted into
 * whatever the player wants to say. Players without the mod still see the raw text, which is the point of
 * it being an ordinary chat message.
 *
 * <p>All three ways a line can arrive are covered, because a vanilla server and a plugin server do not use
 * the same one: signed player chat, a plain system message (what chat plugins broadcast after cancelling
 * the player's own message — the common case in practice), and disguised chat. All three handlers are
 * byte-identical from 1.21.1 through 26.2, so the injections need no version branch.
 *
 * <p>The signature cache is filled in ClientPacketListener before this listener runs, but the
 * acknowledgement is not — vanilla marks the message processed from inside the callback this injection
 * skips, so cancelling signed chat means doing that by hand (see below).
 */
@Mixin(ChatListener.class)
public class ChatListenerMixin {

    @Inject(method = "handlePlayerChatMessage", at = @At("HEAD"), cancellable = true)
    private void tomb$sharedProfile(PlayerChatMessage message, GameProfile profile, ChatType.Bound bound,
                                    CallbackInfo ci) {
        String raw = message.signedContent();
        MutableComponent rewritten = ProfileShare.rewrite(raw);
        if (rewritten == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        // Acknowledge it by hand before cancelling. Vanilla does this from inside the very lambda this
        // injection skips, and a signed message that is never marked processed never enters the last-seen
        // window the server checks. It takes the signature from 1.21.11 on and the whole message before
        // that; vanilla also skips the call when the message carries no signature, so this mirrors it.
        ClientPacketListener connection = minecraft.getConnection();
        if (connection != null && message.signature() != null) {
            //? if >=1.21.11 {
            connection.markMessageAsProcessed(message.signature(), true);
            //?} else {
            /*connection.markMessageAsProcessed(message, true);
            *///?}
        }
        // decorate() puts the sender back in front the way the server's chat type says it should be, so a
        // rewritten line still reads exactly like every other line in the chat
        TOMBClient.tell(bound.decorate(rewritten));
        ci.cancel();
    }

    /**
     * The path that actually matters on a plugin server: chat plugins cancel the player's message and
     * broadcast their own, which arrives as a system message and never touches the signed-chat path at
     * all. That is why a profile typed into chat there came out as plain text.
     *
     * <p>Nothing loops: the replacement is put back through the same handler, but its text is "TOMB" and
     * "[Import]" — the payload only survives inside the click event — so it no longer looks like a profile.
     */
    @Inject(method = "handleSystemMessage", at = @At("HEAD"), cancellable = true)
    private void tomb$sharedProfileSystem(Component message, boolean flag, CallbackInfo ci) {
        if (ProfileShare.isActionBar(flag)) return;   // the action bar is not chat and cannot be clicked
        Component rewritten = ProfileShare.rewriteComponent(message);
        if (rewritten == null) return;
        TOMBClient.tell(rewritten);
        ci.cancel();
    }

    /** Chat a server sends on someone's behalf without signing it — same treatment. */
    @Inject(method = "handleDisguisedChatMessage", at = @At("HEAD"), cancellable = true)
    private void tomb$sharedProfileDisguised(Component message, ChatType.Bound bound, CallbackInfo ci) {
        Component rewritten = ProfileShare.rewriteComponent(message);
        if (rewritten == null) return;
        TOMBClient.tell(bound.decorate(rewritten));
        ci.cancel();
    }
}
