package me.zipestudio.tomb.config;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.*;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ConfigHandler {

    private static final LeafyConfig leafyConfig = LeafyConfig.getInstance();

    public static boolean isCurrentItem(Item item) {

        Optional<ToolEntry> entry = getToolEntry(item);
        if (entry.isPresent()) {
            return entry.get().isEnabled();
        }

        ItemStack stack = item.getDefaultInstance();

        if (stack.is(ItemTags.SWORDS) ||
                stack.is(ItemTags.AXES) ||
                stack.is(ItemTags.HOES) ||
                stack.is(ItemTags.PICKAXES) ||
                stack.is(ItemTags.SHOVELS)) {
            return true;
        }

        //? if >=1.21.11 {
        /*if (stack.is(ItemTags.SPEARS)) {
            return true;
        }
        *///?}

        if (item instanceof MaceItem ||
                item instanceof ShieldItem ||
                item instanceof TridentItem ||
                item instanceof ProjectileWeaponItem ||
                item instanceof BowItem ||
                item instanceof CrossbowItem ||
                item instanceof FishingRodItem
        ) {
            return true;
        }

        return stack.get(DataComponents.TOOL) != null;
    }

    public static Optional<ToolEntry> getToolEntry(Item item) {
        return leafyConfig.getTools().stream()
                .filter(e -> e.getId().equals(BuiltInRegistries.ITEM.getKey(item)))
                .findFirst();
    }

    public static float getToolScale(Item item) {
        return getToolEntry(item)
                .map(ToolEntry::getScale)
                .orElseGet(() -> {

                    if (item instanceof ShieldItem) return 1.5f;
                    if (item instanceof ShearsItem) return 0.85f;
                    if (isItemEqualsId(item, "farmersdelight:skillet")) return 1.2F;

                    return 1.0f;
                });
    }

    public static ItemDisplayContext getDisplayContext(Item item) {
        return getToolEntry(item)
                .map(ToolEntry::getDisplayContext)
                .map(ConfigHandler::parseDisplayContext)
                .orElseGet(() -> {

                    if (item instanceof TridentItem) return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;

                    //? if >=1.21.11 {
                    /*if (item.getDefaultInstance().is(ItemTags.SPEARS))
                        return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
                    *///?}

                    return ItemDisplayContext.FIXED;
                });
    }

    public static boolean hasTool3DModel(Item item) {
        return getToolEntry(item)
                .map(ToolEntry::is3DModel)
                .orElseGet(() -> {
                    if (item instanceof ShieldItem) return true;
                    if (isItemEqualsId(item, "farmersdelight:skillet")) return true;

                    //? if >=1.21.11 {
                    /*if (item.getDefaultInstance().is(ItemTags.SPEARS)) return true;
                    *///?}

                    if (item instanceof TridentItem) return true;

                    return false;
                });
    }

    public static float getToolAngle(Item item) {
        return getToolEntry(item)
                .map(ToolEntry::getAngle)
                .orElseGet(() -> {
                    if (item instanceof ShieldItem) return 157.5F;
                    if (item instanceof TridentItem) return -40f;
                    if (item instanceof ProjectileWeaponItem) return 90f;
                    if (item instanceof FishingRodItem) return 180f;
                    if (item instanceof MaceItem) return 0f;
                    if (isItemEqualsId(item, "farmersdelight:skillet")) return 157.5F;

                    //? if >=1.21.11 {
                    /*if (item.getDefaultInstance().is(ItemTags.SPEARS)) return -40F;
                    *///?}

                    return 0f;
                });
    }

    public static float[] getToolOffset(Item item) {
        return getToolEntry(item)
                .map(entry -> new float[]{entry.getOffsetX(), entry.getOffsetY(), entry.getOffsetZ()})
                .orElseGet(() -> {
                    if (item instanceof ShearsItem) return new float[]{0, 0.1F, 0};
                    if (item instanceof ShieldItem) return new float[]{0, 0, -0.94F / 16F};
                    if (item instanceof TridentItem) return new float[]{0, 0.1F, 0.065F};
                    if (isItemEqualsId(item, "farmersdelight:skillet")) return new float[]{0, 0, -0.65F / 16F};

                    //? if >=1.21.11 {
                    /*if (item.getDefaultInstance().is(ItemTags.SPEARS)) return new float[]{0, -0.1F, 0F};
                    *///?}

                    return new float[]{0f, 0f, 0f};
                });
    }

    public static String getToolGroup(Item item) {
        return getToolEntry(item)
                .map(ToolEntry::getGroup)
                .orElse("BACK");
    }

    private static final Map<String, ItemDisplayContext> DISPLAY_CONTEXT_MAP = Arrays.stream(ItemDisplayContext.values())
            .collect(Collectors.toMap(
                    ctx -> ctx.getSerializedName().toLowerCase(Locale.ROOT),
                    Function.identity()
            ));

    public static ItemDisplayContext parseDisplayContext(String name) {
        if (name == null) return ItemDisplayContext.FIXED;
        return DISPLAY_CONTEXT_MAP.getOrDefault(name.toLowerCase(Locale.ROOT), ItemDisplayContext.FIXED);
    }


    private static ResourceLocation getResourceLocationFromItem(Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }

    public static boolean isItemEqualsId(Item item, String id) {
        return getResourceLocationFromItem(item).toString().equals(id);
    }

    public static boolean areStacksEqual(final ItemStack a, final ItemStack b) {
        return !a.isEmpty() && !b.isEmpty() && a.getItem() == b.getItem();
    }

    public static boolean isModEnabled() {
        return leafyConfig.isEnableMod();
    }

    public static boolean shouldRenderWithCape() {
        return leafyConfig.isRenderWithCape();
    }

    public static boolean shouldRenderWithElytra() {
        return leafyConfig.isRenderWithElytra();
    }

}