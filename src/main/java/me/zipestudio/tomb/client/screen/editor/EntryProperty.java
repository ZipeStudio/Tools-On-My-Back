package me.zipestudio.tomb.client.screen.editor;

import me.zipestudio.tomb.TOMBServer;
import me.zipestudio.tomb.config.ToolEntry;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;

public final class EntryProperty {

    private static final float EPSILON = 1.0E-4f;

    private EntryProperty() {
    }

    public interface FloatGetter {
        float get(ToolEntry entry);
    }

    public interface FloatSetter {
        void set(ToolEntry entry, float value);
    }

    public interface FlagGetter {
        boolean get(ToolEntry entry);
    }

    public interface FlagSetter {
        void set(ToolEntry entry, boolean value);
    }

    public record FloatProperty(String key, float min, float max, boolean wholeNumbers,
                                FloatGetter getter, FloatSetter setter) {

        public MutableComponent label() {
            return TOMBServer.text(key);
        }

        public MutableComponent tooltip() {
            return TOMBServer.text(key + ".tooltip");
        }

        public void copy(ToolEntry from, ToolEntry to) {
            setter.set(to, getter.get(from));
        }

        public boolean same(ToolEntry from, ToolEntry to) {
            return Math.abs(getter.get(from) - getter.get(to)) < EPSILON;
        }
    }

    public record FlagProperty(String key, boolean inResetEntry, FlagGetter getter, FlagSetter setter) {

        public MutableComponent label() {
            return TOMBServer.text(key);
        }

        public MutableComponent tooltip() {
            return TOMBServer.text(key + ".tooltip");
        }

        public void copy(ToolEntry from, ToolEntry to) {
            setter.set(to, getter.get(from));
        }

        public boolean same(ToolEntry from, ToolEntry to) {
            return getter.get(from) == getter.get(to);
        }
    }

    public static final FloatProperty SCALE =
            new FloatProperty("editor.scale", 0.1f, 3f, false, ToolEntry::getScale, ToolEntry::setScale);
    public static final FloatProperty ANGLE_X = angle("editor.rot_x", ToolEntry::getAngleX, ToolEntry::setAngleX);
    public static final FloatProperty ANGLE_Y = angle("editor.rot_y", ToolEntry::getAngleY, ToolEntry::setAngleY);
    public static final FloatProperty ANGLE_Z = angle("editor.rot_z", ToolEntry::getAngleZ, ToolEntry::setAngleZ);
    public static final FloatProperty OFFSET_X = offset("editor.off_x", ToolEntry::getOffsetX, ToolEntry::setOffsetX);
    public static final FloatProperty OFFSET_Y = offset("editor.off_y", ToolEntry::getOffsetY, ToolEntry::setOffsetY);
    public static final FloatProperty OFFSET_Z = offset("editor.off_z", ToolEntry::getOffsetZ, ToolEntry::setOffsetZ);

    public static final FlagProperty ENABLED =
            new FlagProperty("editor.enabled", true, ToolEntry::isEnabled, ToolEntry::setEnabled);
    public static final FlagProperty FLIP =
            new FlagProperty("editor.flip", true, ToolEntry::isFlip, ToolEntry::setFlip);
    public static final FlagProperty APPLY_TO_SIMILAR =
            new FlagProperty("editor.apply_to_similar", true, ToolEntry::isApplyToSimilar, ToolEntry::setApplyToSimilar);

    public static final FlagProperty MATCH_NAME =
            new FlagProperty("editor.match_name", false, ToolEntry::isMatchName, ToolEntry::setMatchName);
    public static final FlagProperty MATCH_MODEL_DATA =
            new FlagProperty("editor.match_model_data", false, ToolEntry::isMatchModelData, ToolEntry::setMatchModelData);
    public static final FlagProperty MATCH_ITEM_MODEL =
            new FlagProperty("editor.match_item_model", false, ToolEntry::isMatchItemModel, ToolEntry::setMatchItemModel);

    private static final List<FloatProperty> LOOK_NUMBERS =
            List.of(SCALE, ANGLE_X, ANGLE_Y, ANGLE_Z, OFFSET_X, OFFSET_Y, OFFSET_Z);

    private static final List<FlagProperty> TARGET_FLAGS =
            List.of(MATCH_NAME, MATCH_MODEL_DATA, MATCH_ITEM_MODEL, APPLY_TO_SIMILAR, ENABLED);

    private static FloatProperty angle(String key, FloatGetter getter, FloatSetter setter) {
        return new FloatProperty(key, -180f, 180f, true, getter, setter);
    }

    private static FloatProperty offset(String key, FloatGetter getter, FloatSetter setter) {
        return new FloatProperty(key, -2f, 2f, false, getter, setter);
    }

    public static void copyLook(ToolEntry from, ToolEntry to) {
        to.setDisplayContext(from.getDisplayContext());
        FLIP.copy(from, to);
        for (FloatProperty property : LOOK_NUMBERS) property.copy(from, to);
    }

    public static void copyAll(ToolEntry from, ToolEntry to) {
        for (FlagProperty flag : TARGET_FLAGS) flag.copy(from, to);
        copyLook(from, to);
    }
}
