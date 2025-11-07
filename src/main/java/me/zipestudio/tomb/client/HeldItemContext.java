package me.zipestudio.tomb.client;

import me.zipestudio.tomb.config.ConfigHandler;
import net.minecraft.item.ItemStack;

public class HeldItemContext {

    public ItemStack droppedStack = ItemStack.EMPTY;
    public ItemStack activeMain = ItemStack.EMPTY;
    public ItemStack previousMain = ItemStack.EMPTY;
    public int dropCooldown = 0;

    public void tick(ItemStack main) {

        if (dropCooldown > 0) {
            dropCooldown--;
        }

        if (dropCooldown > 0 && main.isEmpty()) {
            activeMain = main;
            return;
        }

        if (!droppedStack.isEmpty() && ConfigHandler.areStacksEqual(droppedStack, activeMain)) {
            activeMain = ItemStack.EMPTY;
            droppedStack = ItemStack.EMPTY;
            return;
        }

        if (ConfigHandler.areStacksEqual(droppedStack, previousMain)) {
            previousMain = ItemStack.EMPTY;
        }

        if (ConfigHandler.areStacksEqual(main, previousMain)) {
            previousMain = ItemStack.EMPTY;
        }

        if (!ConfigHandler.areStacksEqual(main, activeMain) && ConfigHandler.isItemEnabled(activeMain.getItem())) {
            previousMain = activeMain;
        }

        activeMain = main;
    }

    public void markItemDropped(ItemStack dropped) {
        this.droppedStack = dropped;
        this.dropCooldown = 2;
    }

    public void clear() {
        this.droppedStack = ItemStack.EMPTY;
        this.activeMain = ItemStack.EMPTY;
        this.previousMain = ItemStack.EMPTY;
        this.dropCooldown = 0;
    }

}