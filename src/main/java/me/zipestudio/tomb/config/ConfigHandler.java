package me.zipestudio.tomb.config;

import net.minecraft.client.Minecraft;
//? if >=1.20.5 {
import net.minecraft.core.component.DataComponents;
//?}
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ConfigHandler {

    private static final LeafyConfig leafyConfig = LeafyConfig.getInstance();

    private static final String SKILLET_ID = "farmersdelight:skillet";

    public static boolean isCurrentItem(ItemStack stack) {

        Item item = stack.getItem();

        Optional<ToolEntry> entry = getToolEntry(stack);
        if (entry.isPresent()) {
            return entry.get().isEnabled();
        }

        if (stack.is(ItemTags.SWORDS) ||
                stack.is(ItemTags.AXES) ||
                stack.is(ItemTags.HOES) ||
                stack.is(ItemTags.PICKAXES) ||
                stack.is(ItemTags.SHOVELS)) {
            return true;
        }

        //? if >=1.21.11 {
        if (stack.is(ItemTags.SPEARS)) {
            return true;
        }
        //?}

        if (isMace(item) ||
                item instanceof ShieldItem ||
                item instanceof TridentItem ||
                item instanceof ProjectileWeaponItem ||
                item instanceof BowItem ||
                item instanceof CrossbowItem ||
                item instanceof FishingRodItem ||
                isSkillet(item)
        ) {
            return true;
        }

        return isTool(stack);
    }

    private static boolean isSkillet(Item item) {
        return isItemEqualsId(item, SKILLET_ID);
    }

    private static boolean isTool(ItemStack stack) {
        //? if >=1.20.5 {
        return stack.get(DataComponents.TOOL) != null;
        //?} else {
        /*Item item = stack.getItem();
        return item instanceof TieredItem || item instanceof ShearsItem;
        *///?}
    }

    public static final List<String> MATCH_TYPES = List.of(
            "swords", "axes", "pickaxes", "shovels", "hoes",
            "tridents", "shields", "bows", "crossbows", "fishing_rods", "maces", "bundles");

    public static Optional<ToolEntry> getToolEntry(ItemStack stack) {
        ToolEntry previewEntry = me.zipestudio.tomb.client.TOMBClient.PREVIEW_ENTRY;
        if (previewEntry != null && stack == me.zipestudio.tomb.client.TOMBClient.PREVIEW_STACK) {
            return Optional.of(previewEntry);
        }

        List<ToolEntry> tools = leafyConfig.getTools();
        for (ToolEntry e : tools) if (!e.isApplyToSimilar() && discriminates(e) && e.matches(stack)) return Optional.of(e);
        for (ToolEntry e : tools) if (!e.isApplyToSimilar() && !discriminates(e) && e.matches(stack)) return Optional.of(e);
        for (ToolEntry e : tools) if (e.isApplyToSimilar() && discriminates(e) && e.matches(stack)) return Optional.of(e);
        for (ToolEntry e : tools) if (e.isApplyToSimilar() && !discriminates(e) && e.matches(stack)) return Optional.of(e);
        return Optional.empty();
    }

    public static String detectCategory(ItemStack stack) {
        for (String type : MATCH_TYPES) if (matchesType(stack, type)) return type;
        return "";
    }

    private static boolean isMace(Item item) {
        //? if >=1.21 {
        return item instanceof MaceItem;
        //?} else {
        /*return false;
        *///?}
    }

    public static boolean matchesType(ItemStack stack, String type) {
        Item item = stack.getItem();
        return switch (type) {
            case "swords" -> stack.is(ItemTags.SWORDS);
            case "axes" -> stack.is(ItemTags.AXES);
            case "pickaxes" -> stack.is(ItemTags.PICKAXES);
            case "shovels" -> stack.is(ItemTags.SHOVELS);
            case "hoes" -> stack.is(ItemTags.HOES);
            case "tridents" -> item instanceof TridentItem;
            case "shields" -> item instanceof ShieldItem;
            case "bows" -> item instanceof BowItem;
            case "crossbows" -> item instanceof CrossbowItem;
            case "fishing_rods" -> item instanceof FishingRodItem;
            case "maces" -> isMace(item);
            //? if >=1.21.4 {
            case "bundles" -> stack.is(ItemTags.BUNDLES);
            //?} else {
            /*case "bundles" -> false;
            *///?}
            default -> false;
        };
    }

    public static boolean componentsMatch(ItemStack stack, ToolEntry entry) {
        return ItemDiscriminator.matches(stack, entry);
    }

    public static boolean discriminates(ToolEntry entry) {
        return ItemDiscriminator.discriminates(entry);
    }

    public static String selectedComponentsJson(ToolEntry entry) {
        return ItemDiscriminator.selectedJson(entry);
    }

    public static boolean sameDiscriminator(ToolEntry a, ToolEntry b) {
        return ItemDiscriminator.same(a, b);
    }

    public static void pruneComponents(ToolEntry entry) {
        String selected = ItemDiscriminator.selectedJson(entry);
        if (!selected.equals(entry.getComponents())) entry.setComponents(selected);
    }

    public static String encodeComponents(ItemStack stack) {
        return ItemDiscriminator.capture(stack);
    }

    public static ItemStack buildStack(ToolEntry entry) {
        Identifier id = Identifier.tryParse(entry.getItemId());
        if (id == null) return ItemStack.EMPTY;
        //? if >=1.21.2 {
        Item item = BuiltInRegistries.ITEM.getValue(id);
        //?} else {
        /*Item item = BuiltInRegistries.ITEM.get(id);
        *///?}
        ItemStack stack = new ItemStack(item);
        ItemDiscriminator.applyTo(stack, entry);
        return stack;
    }

    public static float getToolScale(ItemStack stack) {
        return getToolEntry(stack).map(ToolEntry::getScale).orElseGet(() -> defaultScale(stack.getItem()));
    }

    public static float defaultScale(Item item) {
        if (item instanceof ShieldItem) return 1.5f;
        if (item instanceof ShearsItem) return 0.85f;
        if (isSkillet(item)) return 1.2F;
        return 1.0f;
    }

    public static ItemDisplayContext getDisplayContext(ItemStack stack) {
        return getToolEntry(stack).map(ToolEntry::getDisplayContext).map(ConfigHandler::parseDisplayContext)
                .orElseGet(() -> defaultDisplayContext(stack.getItem()));
    }

    public static ItemDisplayContext defaultDisplayContext(Item item) {
        if (item instanceof TridentItem) return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        //? if >=1.21.11 {
        if (item.getDefaultInstance().is(ItemTags.SPEARS))
            return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        //?}
        return ItemDisplayContext.FIXED;
    }

    public static boolean shouldFlip(ItemStack stack) {
        return getToolEntry(stack).map(ToolEntry::isFlip).orElseGet(() -> defaultFlip(stack.getItem()));
    }

    public static boolean defaultFlip(Item item) {
        if (item instanceof ShieldItem) return true;
        if (isSkillet(item)) return true;
        //? if >=1.21.11 {
        if (item.getDefaultInstance().is(ItemTags.SPEARS)) return true;
        //?}
        if (item instanceof TridentItem) return true;
        return false;
    }

    public static float getToolAngleZ(ItemStack stack) {
        return getToolEntry(stack).map(ToolEntry::getAngleZ).orElseGet(() -> defaultAngleZ(stack.getItem()));
    }

    public static float defaultAngleZ(Item item) {
        if (item instanceof ShieldItem) return 157.5F;
        if (item instanceof TridentItem) return -40f;
        if (item instanceof ProjectileWeaponItem) return 90f;
        if (item instanceof FishingRodItem) return 180f;
        if (isMace(item)) return 0f;
        if (isSkillet(item)) return 157.5F;
        //? if >=1.21.11 {
        if (item.getDefaultInstance().is(ItemTags.SPEARS)) return -40F;
        //?}
        return 0f;
    }

    public static float getToolAngleX(ItemStack stack) {
        return getToolEntry(stack).map(ToolEntry::getAngleX).orElse(0f);
    }

    public static float getToolAngleY(ItemStack stack) {
        return getToolEntry(stack).map(ToolEntry::getAngleY).orElse(0f);
    }

    public static float[] getToolOffset(ItemStack stack) {
        return getToolEntry(stack)
                .map(entry -> new float[]{entry.getOffsetX(), entry.getOffsetY(), entry.getOffsetZ()})
                .orElseGet(() -> defaultOffset(stack.getItem()));
    }

    public static float[] defaultOffset(Item item) {
        if (item instanceof ShearsItem) return new float[]{0, -0.1F, 0};
        if (item instanceof ShieldItem) return new float[]{0, 0, -0.94F / 16F};
        if (item instanceof TridentItem) return new float[]{0, -0.1F, 0.065F};
        if (isSkillet(item)) return new float[]{0, 0, -0.65F / 16F};
        //? if >=1.21.11 {
        if (item.getDefaultInstance().is(ItemTags.SPEARS)) return new float[]{0, 0.1F, 0F};
        //?}
        return new float[]{0f, 0f, 0f};
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

    private static Identifier getIdentifierFromItem(Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }

    public static boolean isItemEqualsId(Item item, String id) {
        return getIdentifierFromItem(item).toString().equals(id);
    }

    public static boolean areStacksEqual(final ItemStack a, final ItemStack b) {
        if (a.isEmpty() || b.isEmpty() || a.getItem() != b.getItem()) return false;
        return ItemDiscriminator.sameItemIgnoringWear(a, b);
    }


    public static boolean isModEnabled() {
        return leafyConfig.isEnableMod();
    }

    public static boolean shouldRenderWithCape() {
        return leafyConfig.isRenderWithCape();
    }

    public static boolean shouldRenderInFirstPerson() {
        return leafyConfig.isRenderInFirstPerson();
    }

    public static boolean shouldRenderWithElytra() {
        return leafyConfig.isRenderWithElytra();
    }

}