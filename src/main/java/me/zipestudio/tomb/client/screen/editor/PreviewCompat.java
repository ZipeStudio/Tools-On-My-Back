package me.zipestudio.tomb.client.screen.editor;

import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.entity.player.Player;
//? if >=1.21.9 {
import net.minecraft.world.phys.Vec3;
//?}

import java.util.UUID;

public final class PreviewCompat {

    private PreviewCompat() {
    }

    private static final double NAME_TAG_RANGE = 64.0;

    private static UUID steveUuid;
    private static UUID alexUuid;

    public static void hideNameTag(RemotePlayer dummy, Player real) {
        //? if >=1.21.9 {
        //?} else {
        /*dummy.setPos(real.getX() + NAME_TAG_RANGE * 2.0, real.getY(), real.getZ());
        *///?}
    }

    public static void seedCape(RemotePlayer dummy) {
        dummy.setOldPosAndRot();
        //? if >=1.21.9 {
        Vec3 position = dummy.position();
        dummy.avatarState().tick(position, Vec3.ZERO);
        dummy.avatarState().tick(position, Vec3.ZERO);
        //?} else {
        /*dummy.xCloak = dummy.xCloakO = dummy.getX();
        dummy.yCloak = dummy.yCloakO = dummy.getY();
        dummy.zCloak = dummy.zCloakO = dummy.getZ();
        *///?}
    }

    private static String skinPath(UUID candidate) {
        //? if >=1.21.9 {
        return DefaultPlayerSkin.get(candidate).body().texturePath().toString();
        //?} else if >=1.21 {
        /*return DefaultPlayerSkin.get(candidate).texture().toString();
        *///?} else {
        /*return DefaultPlayerSkin.getDefaultSkin(candidate).toString();
        *///?}
    }

    private static boolean isSlim(UUID candidate) {
        //? if >=1.21 {
        return DefaultPlayerSkin.get(candidate).model().name().equals("SLIM");
        //?} else {
        /*return DefaultPlayerSkin.getSkinModelName(candidate).equals("slim");
        *///?}
    }

    public static UUID defaultSkinUuid(boolean alex) {
        UUID cached = alex ? alexUuid : steveUuid;
        if (cached != null) return cached;
        String wanted = alex ? "slim/alex" : "wide/steve";
        UUID fallback = null;
        for (int candidateId = 0; candidateId < 512; candidateId++) {
            UUID candidate = new UUID(0L, candidateId);
            String path = skinPath(candidate);
            if (path.contains(wanted)) {
                if (alex) alexUuid = candidate;
                else steveUuid = candidate;
                return candidate;
            }
            if (fallback == null && isSlim(candidate) == alex) fallback = candidate;
        }
        return fallback != null ? fallback : new UUID(0L, alex ? 1L : 0L);
    }
}
