package me.zipestudio.tomb.config;

import me.zipestudio.tomb.client.TOMBClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Identifier;

import java.util.Optional;

public class ConfigHandler {

    private static final LeafyConfig leafyConfig = TOMBClient.getLeafyConfig();

    public static boolean isItemEnabled(Item item) {

        if (!leafyConfig.isEnableMod()) return false;

        if (getToolEntry(item).isPresent()) return true;

        ItemStack stack = item.getDefaultStack();

        if (stack.isIn(ItemTags.SWORDS) ||
                stack.isIn(ItemTags.AXES) ||
                stack.isIn(ItemTags.HOES) ||
                stack.isIn(ItemTags.PICKAXES) ||
                stack.isIn(ItemTags.SHOVELS) ||
                stack.isIn(ItemTags.STRIDER_TEMPT_ITEMS)) {
            return true;
        }

        if (item instanceof MaceItem ||
                item instanceof ShieldItem ||
                item instanceof TridentItem ||
                item instanceof RangedWeaponItem ||
                item instanceof BowItem ||
                item instanceof CrossbowItem ||
                item instanceof FishingRodItem) {
            return true;
        }

        return stack.get(DataComponentTypes.TOOL) != null;
    }

    public static Optional<ToolEntry> getToolEntry(Item item) {
        return leafyConfig.getTools().stream()
                .filter(e -> e.getId().equals(Registries.ITEM.getId(item)))
                .findFirst();
    }

    public static float getToolScale(Item item) {
        return getToolEntry(item)
                .map(ToolEntry::getScale)
                .orElseGet(() -> {
                    if (item instanceof ShieldItem) return 1.5f;
                    if (isItemEqualsId(item, "farmersdelight:skillet")) return 1.2F;
                    return 1.0f;
                });
    }

    public static boolean hasTool3DModel(Item item) {
        return getToolEntry(item)
                .map(ToolEntry::is3DModel)
                .orElseGet(() -> {
                    if (item instanceof ShieldItem) return true;
                    if (isItemEqualsId(item, "farmersdelight:skillet")) return true;
                    return false;
                });
    }

    public static float getToolAngle(Item item) {
        return getToolEntry(item)
                .map(ToolEntry::getAngle)
                .orElseGet(() -> {
                    if (item instanceof ShieldItem) return 157.5F;
                    if (item instanceof TridentItem) return 0f;
                    if (item instanceof RangedWeaponItem) return 90f;
                    if (item instanceof FishingRodItem) return 180f;
                    if (item instanceof MaceItem) return 0f;
                    if (isItemEqualsId(item, "farmersdelight:skillet")) return 157.5F;
                    return 0f;
                });
    }

    public static String getToolGroup(Item item) {
        return getToolEntry(item)
                .map(ToolEntry::getGroup)
                .orElse("BACK");
    }

    public static float[] getToolOffset(Item item) {
        return getToolEntry(item)
                .map(entry -> new float[]{entry.getOffsetX(), entry.getOffsetY(), entry.getOffsetZ()})
                .orElseGet(() -> {
                    if (item instanceof ShieldItem) return new float[]{0, 0, -0.94F / 16F};
                    if (isItemEqualsId(item, "farmersdelight:skillet")) return new float[]{0, 0, -0.65F / 16F};
                    return new float[]{0f, 0f, 0f};
                });
    }

    private static Identifier getIdentifierFromItem(Item item) {
        return Registries.ITEM.getId(item);
    }

    public static boolean isItemEqualsId(Item item, String id) {
        return getIdentifierFromItem(item).toString().equals(id);
    }

    public static boolean areStacksEqual(final ItemStack a, final ItemStack b) {
        return !a.isEmpty() && !b.isEmpty() && a.getItem() == b.getItem();
    }

    public static boolean shouldRenderWithCape() {
        return leafyConfig.isRenderWithCape();
    }

    public static boolean shouldRenderWithElytra() {
        return leafyConfig.isRenderWithElytra();
    }

}