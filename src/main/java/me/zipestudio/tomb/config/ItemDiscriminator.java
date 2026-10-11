package me.zipestudio.tomb.config;

import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
//? if >=1.20.5 {
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.resources.RegistryOps;

import java.util.Optional;
//?} else {
/*import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
*///?}

public final class ItemDiscriminator {

    private ItemDiscriminator() {
    }

    public static boolean supportsItemModel() {
        //? if >=1.21.4 {
        return true;
        //?} else {
        /*return false;
        *///?}
    }

    //? if >=1.20.5 {

    private static final Map<String, DataComponentPatch> CACHE = new HashMap<>();

    private static final Set<DataComponentType<?>> VOLATILE_WHILE_HELD = Set.of(
            DataComponents.DAMAGE,
            DataComponents.CHARGED_PROJECTILES,
            DataComponents.BUNDLE_CONTENTS
    );

    public static boolean matches(ItemStack stack, ToolEntry entry) {
        DataComponentPatch discriminator = stored(entry);
        if (discriminator == null || discriminator.isEmpty()) return true;

        DataComponentPatch.SplitResult captured = discriminator.split();
        for (TypedDataComponent<?> component : captured.added()) {
            if (!selected(entry, component.type())) continue;
            if (!Objects.equals(component.value(), stack.get(component.type()))) return false;
        }
        for (DataComponentType<?> type : captured.removed()) {
            if (!selected(entry, type)) continue;
            if (stack.get(type) != null) return false;
        }
        return true;
    }

    public static boolean discriminates(ToolEntry entry) {
        return !selectedPatch(entry).isEmpty();
    }

    public static boolean same(ToolEntry a, ToolEntry b) {
        return registryAccess() == null
                ? a.effectiveComponents().equals(b.effectiveComponents())
                : selectedPatch(a).equals(selectedPatch(b));
    }

    public static String selectedJson(ToolEntry entry) {
        String stored = entry.getComponents();
        if (stored == null || stored.isEmpty()) return "";
        RegistryAccess registryAccess = registryAccess();
        if (registryAccess == null) return stored;

        DataComponentPatch patch = selectedPatch(entry);
        if (patch.isEmpty()) return "";
        try {
            RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registryAccess);
            return DataComponentPatch.CODEC.encodeStart(ops, patch).result().map(JsonElement::toString).orElse(stored);
        } catch (Exception e) {
            return stored;
        }
    }

    public static String capture(ItemStack stack) {
        RegistryAccess registryAccess = registryAccess();
        if (registryAccess == null) return "";

        DataComponentPatch.Builder builder = DataComponentPatch.builder();
        copy(builder, stack, DataComponents.CUSTOM_MODEL_DATA);
        copy(builder, stack, DataComponents.CUSTOM_NAME);
        //? if >=1.21.4 {
        copy(builder, stack, DataComponents.ITEM_MODEL);
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

    public static void applyTo(ItemStack stack, ToolEntry entry) {
        DataComponentPatch patch = selectedPatch(entry);
        if (!patch.isEmpty()) stack.applyComponents(patch);
    }

    public static boolean hasModelData(ItemStack stack) {
        return stack.get(DataComponents.CUSTOM_MODEL_DATA) != null;
    }

    public static boolean sameItemIgnoringWear(ItemStack a, ItemStack b) {
        DataComponentPatch pa = a.getComponentsPatch();
        DataComponentPatch pb = b.getComponentsPatch();
        return pa.equals(pb) || stable(pa).equals(stable(pb));
    }

    private static boolean selected(ToolEntry entry, DataComponentType<?> type) {
        if (type == DataComponents.CUSTOM_NAME) return entry.isMatchName();
        if (type == DataComponents.CUSTOM_MODEL_DATA) return entry.isMatchModelData();
        //? if >=1.21.4 {
        if (type == DataComponents.ITEM_MODEL) return entry.isMatchItemModel();
        //?}
        return false;
    }

    private static DataComponentPatch stored(ToolEntry entry) {
        String componentsJson = entry.getComponents();
        if (componentsJson == null || componentsJson.isEmpty()) return null;

        RegistryAccess registryAccess = registryAccess();
        if (registryAccess == null) return null;

        return CACHE.computeIfAbsent(componentsJson, json -> decode(json, registryAccess));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static DataComponentPatch selectedPatch(ToolEntry entry) {
        DataComponentPatch stored = stored(entry);
        if (stored == null || stored.isEmpty()) return DataComponentPatch.EMPTY;

        DataComponentPatch.Builder builder = DataComponentPatch.builder();
        for (TypedDataComponent<?> component : stored.split().added()) {
            if (!selected(entry, component.type())) continue;
            builder.set((DataComponentType) component.type(), component.value());
        }
        return builder.build();
    }

    private static <T> void copy(DataComponentPatch.Builder builder, ItemStack stack, DataComponentType<T> type) {
        T value = stack.get(type);
        if (value != null) builder.set(type, value);
    }

    private static DataComponentPatch decode(String json, RegistryAccess registryAccess) {
        try {
            RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registryAccess);
            return DataComponentPatch.CODEC.parse(ops, JsonParser.parseString(json)).result()
                    .orElse(DataComponentPatch.EMPTY);
        } catch (Exception e) {
            return DataComponentPatch.EMPTY;
        }
    }

    private static Map<DataComponentType<?>, Optional<?>> stable(DataComponentPatch patch) {
        Map<DataComponentType<?>, Optional<?>> out = new HashMap<>();
        DataComponentPatch.SplitResult split = patch.split();
        for (TypedDataComponent<?> component : split.added()) {
            if (!VOLATILE_WHILE_HELD.contains(component.type())) {
                out.put(component.type(), Optional.of(component.value()));
            }
        }
        for (DataComponentType<?> type : split.removed()) {
            if (!VOLATILE_WHILE_HELD.contains(type)) out.put(type, Optional.empty());
        }
        return out;
    }

    private static RegistryAccess registryAccess() {
        ClientLevel level = Minecraft.getInstance().level;
        return level == null ? null : level.registryAccess();
    }

    //?} else {

    /*private static final String CUSTOM_MODEL_DATA = "CustomModelData";

    private static final Map<String, CompoundTag> CACHE = new HashMap<>();

    private static final Set<String> VOLATILE_WHILE_HELD = Set.of(
            ItemStack.TAG_DAMAGE,
            "ChargedProjectiles",
            "Items"
    );

    public static boolean matches(ItemStack stack, ToolEntry entry) {
        CompoundTag discriminator = stored(entry);
        if (discriminator == null || discriminator.isEmpty()) return true;

        if (entry.isMatchName() && !Objects.equals(name(discriminator), name(stack.getTag()))) return false;
        return !entry.isMatchModelData()
                || Objects.equals(modelData(discriminator), modelData(stack.getTag()));
    }

    public static boolean discriminates(ToolEntry entry) {
        return !selectedTag(entry).isEmpty();
    }

    public static boolean same(ToolEntry a, ToolEntry b) {
        return selectedTag(a).equals(selectedTag(b));
    }

    public static String selectedJson(ToolEntry entry) {
        CompoundTag selected = selectedTag(entry);
        return selected.isEmpty() ? "" : selected.toString();
    }

    public static String capture(ItemStack stack) {
        CompoundTag out = new CompoundTag();
        String name = name(stack.getTag());
        if (name != null) {
            CompoundTag display = new CompoundTag();
            display.putString(ItemStack.TAG_DISPLAY_NAME, name);
            out.put(ItemStack.TAG_DISPLAY, display);
        }
        Integer modelData = modelData(stack.getTag());
        if (modelData != null) out.putInt(CUSTOM_MODEL_DATA, modelData);
        return out.isEmpty() ? "" : out.toString();
    }

    public static void applyTo(ItemStack stack, ToolEntry entry) {
        CompoundTag selected = selectedTag(entry);
        if (!selected.isEmpty()) stack.getOrCreateTag().merge(selected);
    }

    public static boolean hasModelData(ItemStack stack) {
        return modelData(stack.getTag()) != null;
    }

    public static boolean sameItemIgnoringWear(ItemStack a, ItemStack b) {
        return Objects.equals(stable(a.getTag()), stable(b.getTag()));
    }

    private static CompoundTag selectedTag(ToolEntry entry) {
        CompoundTag stored = stored(entry);
        CompoundTag out = new CompoundTag();
        if (stored == null || stored.isEmpty()) return out;

        if (entry.isMatchName()) {
            String name = name(stored);
            if (name != null) {
                CompoundTag display = new CompoundTag();
                display.putString(ItemStack.TAG_DISPLAY_NAME, name);
                out.put(ItemStack.TAG_DISPLAY, display);
            }
        }
        if (entry.isMatchModelData()) {
            Integer modelData = modelData(stored);
            if (modelData != null) out.putInt(CUSTOM_MODEL_DATA, modelData);
        }
        return out;
    }

    private static CompoundTag stored(ToolEntry entry) {
        String snbt = entry.getComponents();
        if (snbt == null || snbt.isEmpty()) return null;
        return CACHE.computeIfAbsent(snbt, ItemDiscriminator::decode);
    }

    private static CompoundTag decode(String snbt) {
        try {
            return TagParser.parseTag(snbt);
        } catch (Exception e) {
            return new CompoundTag();
        }
    }

    private static String name(CompoundTag tag) {
        if (tag == null) return null;
        CompoundTag display = tag.getCompound(ItemStack.TAG_DISPLAY);
        return display.contains(ItemStack.TAG_DISPLAY_NAME, Tag.TAG_STRING)
                ? display.getString(ItemStack.TAG_DISPLAY_NAME)
                : null;
    }

    private static Integer modelData(CompoundTag tag) {
        if (tag == null) return null;
        return tag.contains(CUSTOM_MODEL_DATA, Tag.TAG_INT) ? tag.getInt(CUSTOM_MODEL_DATA) : null;
    }

    private static CompoundTag stable(CompoundTag tag) {
        if (tag == null) return new CompoundTag();
        CompoundTag copy = tag.copy();
        for (String key : VOLATILE_WHILE_HELD) copy.remove(key);
        return copy;
    }

    *///?}
}
