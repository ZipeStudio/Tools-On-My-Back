package me.zipestudio.tomb.mixin;

import me.zipestudio.tomb.client.TOMBClient;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Inject(at = @At("HEAD"), method = "handleLogin")
    public void onGameJoin(ClientboundLoginPacket packet, CallbackInfo ci) {
        TOMBClient.HELD_TOOLS.clear();
    }

}
