package me.zipestudio.tomb.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
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

    /** Category ids selectable in the editor for "apply this config to every item of a type". */
    // "bundles" earns a category of its own because every dye is a separate item (lime_bundle,
    // orange_bundle, ...), so without one "apply to all similar" falls back to "same base item" and covers
    // a single colour. Harmless before 1.21.4, where the tag does not exist and matchesType returns false.
    public static final List<String> MATCH_TYPES = List.of(
            "swords", "axes", "pickaxes", "shovels", "hoes",
            "tridents", "shields", "bows", "crossbows", "fishing_rods", "maces", "bundles");

    public static Optional<ToolEntry> getToolEntry(ItemStack stack) {
        // The editor preview answers for itself. Resolving by item id would let a different entry naming
        // the same item win — a plain diamond_sword entry outranks a tuned "apply to all similar" one —
        // so the doll would show settings the editor does not have open. The layer is handed that exact
        // stack instance, so reference identity keeps this from touching any other lookup.
        ToolEntry previewEntry = me.zipestudio.tomb.client.TOMBClient.PREVIEW_ENTRY;
        if (previewEntry != null && stack == me.zipestudio.tomb.client.TOMBClient.PREVIEW_STACK) {
            return Optional.of(previewEntry);
        }

        // Most specific rule first, always. Narrowing the item beats narrowing the scope, and within either
        // scope an entry that also pins components beats one that takes the item as it comes — otherwise a
        // broad "all swords" rule would swallow a golden sword named "dagger" that has its own entry.
        List<ToolEntry> tools = leafyConfig.getTools();
        for (ToolEntry e : tools) if (!e.isApplyToSimilar() && discriminates(e) && e.matches(stack)) return Optional.of(e);
        for (ToolEntry e : tools) if (!e.isApplyToSimilar() && !discriminates(e) && e.matches(stack)) return Optional.of(e);
        for (ToolEntry e : tools) if (e.isApplyToSimilar() && discriminates(e) && e.matches(stack)) return Optional.of(e);
        for (ToolEntry e : tools) if (e.isApplyToSimilar() && !discriminates(e) && e.matches(stack)) return Optional.of(e);
        return Optional.empty();
    }

    /** The first category the stack belongs to, or "" if none. Drives the "apply to all similar" toggle. */
    public static String detectCategory(ItemStack stack) {
        for (String type : MATCH_TYPES) if (matchesType(stack, type)) return type;
        return "";
    }

    /** True when the stack belongs to the given item category (tag or class; covers modded items). */
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
            case "maces" -> item instanceof MaceItem;
            //? if >=1.21.4 {
            case "bundles" -> stack.is(ItemTags.BUNDLES);
            //?} else {
            /*case "bundles" -> false;   // dyed bundles (and the tag) do not exist yet
            *///?}
            default -> false;
        };
    }

    // Decoded discriminator patches cached by their JSON source, so matching does not re-parse every frame.
    private static final Map<String, DataComponentPatch> PATCH_CACHE = new HashMap<>();

    /**
     * True when the stack satisfies a component discriminator. The discriminator is a JSON-encoded
     * DataComponentPatch; a stack matches when it carries every listed component with an equal value
     * (a superset match, so unrelated components like damage are ignored). Empty = match any variant.
     * Components can only be resolved in-world (registry access), so off-world this matches the base item.
     */
    public static boolean componentsMatch(ItemStack stack, ToolEntry entry) {
        DataComponentPatch discriminator = storedComponents(entry);
        if (discriminator == null || discriminator.isEmpty()) return true;

        for (Map.Entry<DataComponentType<?>, Optional<?>> e : discriminator.entrySet()) {
            // each captured component only counts if this entry chose to match on it
            if (!componentSelected(entry, e.getKey())) continue;
            Object expected = e.getValue().orElse(null);
            Object actual = stack.get(e.getKey());
            if (!Objects.equals(expected, actual)) return false;
        }
        return true;
    }

    /** Whether this entry pins any component, i.e. whether it is narrower than "this item, any variant". */
    public static boolean discriminates(ToolEntry entry) {
        return !selectedComponents(entry).isEmpty();
    }

    /** The entry's captured discriminator, or null when there is none to apply. */
    private static DataComponentPatch storedComponents(ToolEntry entry) {
        String componentsJson = entry.getComponents();
        if (componentsJson == null || componentsJson.isEmpty()) return null;

        RegistryAccess registryAccess = registryAccess();
        if (registryAccess == null) return null;

        return PATCH_CACHE.computeIfAbsent(componentsJson, json -> decodePatch(json, registryAccess));
    }

    /**
     * Whether this entry matches on the given captured component. Only the three that
     * {@link #encodeComponents} records are answerable; anything else was never part of a discriminator.
     */
    private static boolean componentSelected(ToolEntry entry, DataComponentType<?> type) {
        if (type == DataComponents.CUSTOM_NAME) return entry.isMatchName();
        if (type == DataComponents.CUSTOM_MODEL_DATA) return entry.isMatchModelData();
        //? if >=1.21.4 {
        if (type == DataComponents.ITEM_MODEL) return entry.isMatchItemModel();
        //?}
        return false;
    }

    /**
     * Whether the entry actually captured the given component. A filter for something that was never
     * captured can do nothing — {@link #componentsMatch} walks the stored discriminator, so it never even
     * sees that type. The editor uses this only to explain the situation in the toggle's tooltip; it no
     * longer disables the toggle, because saving drops the components no filter used and a reopened entry
     * would come back with dead switches. An item whose look survives every filter is not being retextured
     * by NBT at all (a plain resourcepack override on the item id, for one).
     */
    public static boolean captured(ToolEntry entry, DataComponentType<?> type) {
        DataComponentPatch stored = storedComponents(entry);
        if (stored == null) return false;
        for (Map.Entry<DataComponentType<?>, Optional<?>> e : stored.entrySet()) {
            if (e.getKey() == type) return e.getValue().isPresent();
        }
        return false;
    }

    /** The discriminator reduced to the components this entry matches on — what the preview should show. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static DataComponentPatch selectedComponents(ToolEntry entry) {
        DataComponentPatch stored = storedComponents(entry);
        if (stored == null || stored.isEmpty()) return DataComponentPatch.EMPTY;

        DataComponentPatch.Builder selected = DataComponentPatch.builder();
        for (Map.Entry<DataComponentType<?>, Optional<?>> e : stored.entrySet()) {
            if (!componentSelected(entry, e.getKey())) continue;
            Object value = e.getValue().orElse(null);
            if (value != null) selected.set((DataComponentType) e.getKey(), value);
        }
        return selected.build();
    }

    /**
     * The entry's discriminator with everything it does not match on removed. A capture holds all three
     * components the mod knows about, but an entry that only matches on custom_model_data has no use for
     * the name or the item_model — and a plugin item's name alone runs to a couple of hundred characters
     * of JSON, which is what pushed shared profiles past the chat limit.
     *
     * @return the JSON for the selected components, "" when none are selected, or the stored string
     *         unchanged when there is no world to resolve the components against.
     */
    public static String selectedComponentsJson(ToolEntry entry) {
        String stored = entry.getComponents();
        if (stored == null || stored.isEmpty()) return "";
        RegistryAccess registryAccess = registryAccess();
        if (registryAccess == null) return stored;

        DataComponentPatch patch = selectedComponents(entry);
        if (patch.isEmpty()) return "";
        try {
            RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registryAccess);
            return DataComponentPatch.CODEC.encodeStart(ops, patch).result().map(JsonElement::toString).orElse(stored);
        } catch (Exception e) {
            return stored;
        }
    }

    /**
     * Whether two entries pin the same thing — compared on the components they actually match on, the way
     * {@link #componentsMatch} decides it, rather than on the stored strings. Those strings drift apart
     * legitimately: an entry still carrying the full capture and its own pruned profile mean exactly the
     * same item, and comparing text would call them different and duplicate the entry.
     */
    public static boolean sameDiscriminator(ToolEntry a, ToolEntry b) {
        // out of world nothing can be decoded, so fall back to comparing what is stored
        return registryAccess() == null
                ? a.effectiveComponents().equals(b.effectiveComponents())
                : selectedComponents(a).equals(selectedComponents(b));
    }

    /**
     * Drops from the entry whatever its filters do not use, so what is written to disk is what the entry
     * actually goes by. Called on save; nothing prunes while editing, so a filter switched off by mistake
     * can still be switched back on until the config is written.
     */
    public static void pruneComponents(ToolEntry entry) {
        String selected = selectedComponentsJson(entry);
        if (!selected.equals(entry.getComponents())) entry.setComponents(selected);
    }

    /**
     * Encodes the components that define an item's visual identity to a JSON discriminator string, so a
     * captured server/resourcepack item matches every instance of that look while ignoring volatile state
     * like damage or enchantments.
     *
     * <p>The custom name is part of that identity: CIT packs (CIT Resewn and friends) routinely retexture
     * purely by the name given in an anvil, and without it every such variant would be indistinguishable
     * from a plain item. All three are captured here; which of them an entry actually matches on is chosen
     * per entry (see {@code ToolEntry.matchName/matchModelData/matchItemModel}).
     */
    public static String encodeComponents(ItemStack stack) {
        RegistryAccess registryAccess = registryAccess();
        if (registryAccess == null) return "";

        DataComponentPatch.Builder builder = DataComponentPatch.builder();
        copyComponent(builder, stack, DataComponents.CUSTOM_MODEL_DATA);
        copyComponent(builder, stack, DataComponents.CUSTOM_NAME);
        //? if >=1.21.4 {
        copyComponent(builder, stack, DataComponents.ITEM_MODEL);
        //?}
        DataComponentPatch patch = builder.build();
        if (patch.isEmpty()) return "";

        try {
            RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registryAccess);
            return DataComponentPatch.CODEC.encodeStart(ops, patch).result().map(JsonElement::toString).orElse("");
        } catch (Exception e) {
            return "";
        }
    }

    private static <T> void copyComponent(DataComponentPatch.Builder builder, ItemStack stack, DataComponentType<T> type) {
        T value = stack.get(type);
        if (value != null) builder.set(type, value);
    }

    private static DataComponentPatch decodePatch(String json, RegistryAccess registryAccess) {
        try {
            RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registryAccess);
            return DataComponentPatch.CODEC.parse(ops, JsonParser.parseString(json)).result().orElse(DataComponentPatch.EMPTY);
        } catch (Exception e) {
            return DataComponentPatch.EMPTY;
        }
    }

    private static RegistryAccess registryAccess() {
        ClientLevel level = Minecraft.getInstance().level;
        return level == null ? null : level.registryAccess();
    }

    /** Rebuilds an ItemStack for the entry (item + discriminator components) — used by the editor preview. */
    public static ItemStack buildStack(ToolEntry entry) {
        Identifier id = Identifier.tryParse(entry.getItemId());
        if (id == null) return ItemStack.EMPTY;
        //? if >=1.21.2 {
        Item item = BuiltInRegistries.ITEM.getValue(id);
        //?} else {
        /*Item item = BuiltInRegistries.ITEM.get(id);
        *///?}
        ItemStack stack = new ItemStack(item);
        // Only the components this entry actually matches on: the preview then shows exactly what the entry
        // stands for. Applying an unmatched one would repaint the item as a variant it does not target —
        // a captured item_model, for instance, would keep showing the old colour after the id was changed.
        DataComponentPatch patch = selectedComponents(entry);
        if (!patch.isEmpty()) stack.applyComponents(patch);
        return stack;
    }

    public static float getToolScale(ItemStack stack) {
        return getToolEntry(stack).map(ToolEntry::getScale).orElseGet(() -> defaultScale(stack.getItem()));
    }

    public static float defaultScale(Item item) {
        if (item instanceof ShieldItem) return 1.5f;
        if (item instanceof ShearsItem) return 0.85f;
        if (isItemEqualsId(item, "farmersdelight:skillet")) return 1.2F;
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
        if (isItemEqualsId(item, "farmersdelight:skillet")) return true;
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
        if (item instanceof MaceItem) return 0f;
        if (isItemEqualsId(item, "farmersdelight:skillet")) return 157.5F;
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

    /**
     * Built-in offsets, in the same units the editor's sliders and a shared profile use: **+Y is up**.
     * The Y values here were flipped when that convention was fixed — the renderer negates Y once, on its
     * way into the model's own space, so everything above it can read the natural way round.
     */
    public static float[] defaultOffset(Item item) {
        if (item instanceof ShearsItem) return new float[]{0, -0.1F, 0};
        if (item instanceof ShieldItem) return new float[]{0, 0, -0.94F / 16F};
        if (item instanceof TridentItem) return new float[]{0, -0.1F, 0.065F};
        if (isItemEqualsId(item, "farmersdelight:skillet")) return new float[]{0, 0, -0.65F / 16F};
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

    /**
     * True when the two stacks are the same weapon or tool — not merely the same item type. Server plugins
     * routinely build many distinct weapons on one base item (every one of them a minecraft:diamond_sword,
     * told apart by custom_model_data, item_model, a custom name, ...), and comparing {@code getItem()}
     * alone made swapping between two of them look like nothing had changed, so the first never reached
     * the player's back.
     *
     * <p>Components that an item legitimately rewrites while it is being held are ignored, or every swing
     * would read as a change and keep promoting the held item onto its owner's own back: wear
     * ({@code DAMAGE}), a crossbow being loaded ({@code CHARGED_PROJECTILES}) and bundle contents.
     */
    public static boolean areStacksEqual(final ItemStack a, final ItemStack b) {
        if (a.isEmpty() || b.isEmpty() || a.getItem() != b.getItem()) return false;
        DataComponentPatch pa = a.getComponentsPatch();
        DataComponentPatch pb = b.getComponentsPatch();
        // the identical-patch case is the common one and costs nothing; the copies below are only built
        // when something actually differs, i.e. on an item swap or a durability tick
        return pa.equals(pb) || stableComponents(pa).equals(stableComponents(pb));
    }

    private static final Set<DataComponentType<?>> VOLATILE_WHILE_HELD = Set.of(
            DataComponents.DAMAGE,
            DataComponents.CHARGED_PROJECTILES,
            DataComponents.BUNDLE_CONTENTS
    );

    private static Map<DataComponentType<?>, Optional<?>> stableComponents(DataComponentPatch patch) {
        Map<DataComponentType<?>, Optional<?>> out = new HashMap<>();
        for (Map.Entry<DataComponentType<?>, Optional<?>> entry : patch.entrySet()) {
            if (!VOLATILE_WHILE_HELD.contains(entry.getKey())) out.put(entry.getKey(), entry.getValue());
        }
        return out;
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