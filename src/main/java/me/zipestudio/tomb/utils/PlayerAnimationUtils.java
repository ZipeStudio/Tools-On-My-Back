package me.zipestudio.tomb.utils;

import me.zipestudio.tomb.client.TOMBClient;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Method;

public final class PlayerAnimationUtils {

    private static final String[][] LIBRARIES = {
            {"dev.kosmx.playerAnim.impl.IAnimatedPlayer", "getAnimation"},
            {"dev.kosmx.playerAnim.impl.IAnimatedPlayer", "playerAnimator_getAnimation"},
            {"com.zigythebird.playeranim.accessors.IAnimatedAvatar", "playerAnimLib$getAnimation"},
    };

    private static boolean looked;
    private static Class<?> animated;
    private static Method animationOf;
    private static Class<?> stateOwner;
    private static Method isActive;

    private PlayerAnimationUtils() {
    }

    public static boolean animating(Player holder) {
        if (!looked) look();
        if (animationOf == null || !animated.isInstance(holder)) return false;
        try {
            Object animation = animationOf.invoke(holder);
            if (animation == null) return false;
            if (animation.getClass() != stateOwner) {
                stateOwner = animation.getClass();
                isActive = stateOwner.getMethod("isActive");
            }
            return Boolean.TRUE.equals(isActive.invoke(animation));
        } catch (Throwable broken) {
            animationOf = null;
            TOMBClient.LOGGER.warn("Animation state unreadable, first person items stay visible", broken);
            return false;
        }
    }

    private static void look() {
        looked = true;
        for (String[] library : LIBRARIES) {
            try {
                animated = Class.forName(library[0]);
                animationOf = animated.getMethod(library[1]);
                return;
            } catch (Throwable missing) {
                animated = null;
                animationOf = null;
            }
        }
    }

}
