package me.zipestudio.tomb.utils.interfaces;

import net.minecraft.network.chat.Component;

@SuppressWarnings("java:S100")
public interface RefreshableToast {

    void tomb$replace(Component title, Component body);

    void tomb$prepare(long lifeMillis);

}
