package me.zipestudio.tomb.config;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

@Getter
@Setter
@AllArgsConstructor
// Value equality, so the editor can tell a changed config from a saved one. Lombok reads through the
// getters, which is what makes a legacy "FIXED" display context compare equal to a fresh "fixed" one.
// Safe next to savedByEntry: an IdentityHashMap compares with ==, never with equals.
@EqualsAndHashCode
public class ToolEntry {

    // --- what item this entry targets ---
    private String itemId;
    // Component discriminator: JSON-encoded DataComponentPatch. Lets the same base item (e.g.
    // diamond_sword) be configured independently per resourcepack/plugin variant that differs by
    // components (custom_model_data, item_model, ...). Empty = match the base item only.
    private String components;
    // Which of the captured components actually take part in matching, chosen per entry. All off by
    // default: the entry then follows the item itself and covers every variant of it. "Add from hand"
    // switches on the ones the captured item actually carries, except the name — items get renamed in an
    // anvil all the time, so matching on it has to be a deliberate choice.
    private boolean matchName;
    private boolean matchModelData;
    private boolean matchItemModel;
    // When true, this entry applies to every item "similar" to itemId — the same category (swords,
    // axes, ...) if it has one, otherwise every stack of the same base item. One edit covers them all.
    private boolean applyToSimilar;

    // --- behaviour flags ---
    private boolean enabled = true;
    // Turns the item to face the other way: a 180 degree yaw for a left main arm, an inverted Z
    // rotation for a right one. Models with a front and a back need it; a flat sprite does not.
    private boolean flip;
    private String displayContext = DEFAULT_DISPLAY_CONTEXT;

    // --- transform ---
    private float scale;
    private float angleX;  // rotation around X (pitch)
    private float angleY;  // rotation around Y (yaw)
    private float angleZ;  // rotation around Z (roll) — was the original single-axis "angle"
    private float offsetX;
    private float offsetY;
    private float offsetZ;

    public ToolEntry() {
        this("minecraft:diamond_sword", "", false, false, false, false, true, false, DEFAULT_DISPLAY_CONTEXT,
                1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
    }

    /** Lower case, because that is what {@code ItemDisplayContext.getSerializedName()} gives. */
    public static final String DEFAULT_DISPLAY_CONTEXT = "fixed";

    /** Normalised on the way out too: a config written before this stored the enum's own spelling. */
    public String getDisplayContext() {
        return displayContext == null || displayContext.isEmpty()
                ? DEFAULT_DISPLAY_CONTEXT
                : displayContext.toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * Kept in the serialized (lower case) spelling. The editor writes {@code getSerializedName()} while the
     * built-in default used to be the enum's own name, so the same setting was stored two ways and two
     * configs for the same item did not read as equal.
     */
    public void setDisplayContext(String displayContext) {
        this.displayContext = displayContext == null || displayContext.isEmpty()
                ? DEFAULT_DISPLAY_CONTEXT
                : displayContext.toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * The discriminator that actually takes part in matching: with every filter off, the captured
     * components are dead weight the entry ignores. Two entries whose effective discriminators are equal
     * target the same thing, however much either happens to be carrying.
     */
    public String effectiveComponents() {
        boolean anyFilter = matchName || matchModelData || matchItemModel;
        return anyFilter && components != null ? components : "";
    }

    public Identifier getId() {
        return Identifier.parse(itemId);
    }

    /**
     * True when this entry applies to the given stack: the item id matches and, when a component
     * discriminator is set, the stack carries (a superset of) those components.
     */
    public boolean matches(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Identifier id = Identifier.tryParse(itemId);
        if (id == null) return false;

        if (applyToSimilar) {
            //? if >=1.21.2 {
            String category = ConfigHandler.detectCategory(BuiltInRegistries.ITEM.getValue(id).getDefaultInstance());
            //?} else {
            /*String category = ConfigHandler.detectCategory(BuiltInRegistries.ITEM.get(id).getDefaultInstance());
            *///?}
            // "similar" widens which items are in scope; the component filter still narrows within it, so a
            // rule for swords named "dagger" stays a rule about daggers rather than about every sword
            boolean inScope = !category.isEmpty()
                    ? ConfigHandler.matchesType(stack, category)
                    : id.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
            return inScope && ConfigHandler.componentsMatch(stack, this);
        }

        if (!id.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()))) return false;
        return ConfigHandler.componentsMatch(stack, this);
    }

}
