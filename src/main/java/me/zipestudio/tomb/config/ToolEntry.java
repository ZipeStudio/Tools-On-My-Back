package me.zipestudio.tomb.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.Identifier;

@Getter
@Setter
@AllArgsConstructor
public class ToolEntry {

    private String itemId;
    private String group;
    private boolean is3DModel;
    private float scale;
    private float angle;
    private float offsetX;
    private float offsetY;
    private float offsetZ;

    public ToolEntry() {
        this("minecraft:diamond_sword", "BACK", false, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f);
    }

    public Identifier getId() {
        return Identifier.of(itemId);
    }
}