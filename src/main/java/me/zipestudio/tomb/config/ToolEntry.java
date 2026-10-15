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
@EqualsAndHashCode
public class ToolEntry {

    private String itemId;
    private String components;
    private boolean matchName;
    private boolean matchModelData;
    private boolean matchItemModel;
    private String scope = "";

    private boolean enabled = true;
    private boolean flip;

    public static final String DEFAULT_DISPLAY_CONTEXT = "fixed";
    private String displayContext = DEFAULT_DISPLAY_CONTEXT;

    private float scale;
    private float angleX;
    private float angleY;
    private float angleZ;
    private float offsetX;
    private float offsetY;
    private float offsetZ;


    public ToolEntry() {
        this("minecraft:diamond_sword", "", false, false, false, "", true, false, DEFAULT_DISPLAY_CONTEXT,
                1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
    }

    public String getDisplayContext() {
        return displayContext == null || displayContext.isEmpty()
                ? DEFAULT_DISPLAY_CONTEXT
                : displayContext.toLowerCase(java.util.Locale.ROOT);
    }

    public void setDisplayContext(String displayContext) {
        this.displayContext = displayContext == null || displayContext.isEmpty()
                ? DEFAULT_DISPLAY_CONTEXT
                : displayContext.toLowerCase(java.util.Locale.ROOT);
    }

    public String effectiveComponents() {
        boolean anyFilter = matchName || matchModelData || matchItemModel;
        return anyFilter && components != null ? components : "";
    }

    public Identifier getId() {
        //? if >=1.21 {
        return Identifier.parse(itemId);
        //?} else {
        /*return new Identifier(itemId);
        *///?}
    }

    public boolean matches(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Identifier id = Identifier.tryParse(itemId);
        if (id == null) return false;

        if (scope != null && !scope.isEmpty()) {
            return ConfigHandler.inScope(stack, id, scope) && ConfigHandler.componentsMatch(stack, this);
        }

        if (!id.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()))) return false;
        return ConfigHandler.componentsMatch(stack, this);
    }

}
