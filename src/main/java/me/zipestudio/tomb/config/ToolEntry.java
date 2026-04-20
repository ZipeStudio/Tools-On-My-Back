package me.zipestudio.tomb.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;

@Getter
@Setter
@AllArgsConstructor
public class ToolEntry {

    private boolean enabled = true;
    private String itemId;
    private String group;
    private boolean is3DModel;
    private String displayContext = "FIXED";
    private float scale;
    private float angle;
    private float offsetX;
    private float offsetY;
    private float offsetZ;

    public ToolEntry() {
        this(true, "minecraft:diamond_sword", "BACK", false, "FIXED", 1.0f, 0.0f, 0.0f, 0.0f, 0.0f);
    }

    public ResourceLocation getId() {
        return ResourceLocation.parse(itemId);
    }

}