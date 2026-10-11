package me.zipestudio.tomb.client.screen.editor;

import me.zipestudio.tomb.config.ConfigHandler;
import me.zipestudio.tomb.config.ItemDiscriminator;
import me.zipestudio.tomb.config.LeafyConfig;
import me.zipestudio.tomb.config.ToolEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class EditorSession {

    private final LeafyConfig config;

    private List<ToolEntry> snapshot;
    private boolean savedEnableMod;
    private boolean savedRenderWithCape;
    private boolean savedRenderWithElytra;

    private final Map<ToolEntry, ToolEntry> savedByEntry = new IdentityHashMap<>();

    private int index;

    public EditorSession() {
        this.config = LeafyConfig.getInstance();
        this.snapshot = deepCopy(config.getTools());
        config.setTools(new ArrayList<>(config.getTools()));
        pairWithSaved();
        snapshotGlobals();
        if (index >= entries().size()) index = entries().size() - 1;
    }

    public List<ToolEntry> entries() {
        return config.getTools();
    }

    public int index() {
        return index;
    }

    public ToolEntry selected() {
        List<ToolEntry> entries = entries();
        return (index >= 0 && index < entries.size()) ? entries.get(index) : null;
    }

    public void select(int newIndex) {
        int size = entries().size();
        index = size == 0 ? 0 : Math.floorMod(newIndex, size);
    }

    public void snapshotGlobals() {
        savedEnableMod = config.isEnableMod();
        savedRenderWithCape = config.isRenderWithCape();
        savedRenderWithElytra = config.isRenderWithElytra();
    }

    public boolean dirty() {
        if (config.isEnableMod() != savedEnableMod
                || config.isRenderWithCape() != savedRenderWithCape
                || config.isRenderWithElytra() != savedRenderWithElytra) return true;
        List<ToolEntry> live = entries();
        if (live.size() != snapshot.size()) return true;
        for (int i = 0; i < live.size(); i++) {
            if (!live.get(i).equals(snapshot.get(i))) return true;
        }
        return false;
    }

    public void save() {
        config.save();
        this.snapshot = deepCopy(entries());
        pairWithSaved();
        snapshotGlobals();
    }

    public void cancel() {
        restoreGlobals();
        config.setTools(deepCopy(snapshot));
        pairWithSaved();
        if (index >= entries().size()) index = entries().size() - 1;
    }

    public void saveGlobals() {
        if (config.isEnableMod() == savedEnableMod
                && config.isRenderWithCape() == savedRenderWithCape
                && config.isRenderWithElytra() == savedRenderWithElytra) return;
        List<ToolEntry> live = entries();
        try {
            config.setTools(snapshot);
            config.save();
        } finally {
            config.setTools(live);
        }
        snapshotGlobals();
    }

    public void restoreGlobals() {
        config.setEnableMod(savedEnableMod);
        config.setRenderWithCape(savedRenderWithCape);
        config.setRenderWithElytra(savedRenderWithElytra);
    }

    public void discardUnsaved() {
        restoreGlobals();
        config.setTools(new ArrayList<>(snapshot));
    }

    private void pairWithSaved() {
        savedByEntry.clear();
        List<ToolEntry> live = entries();
        for (int i = 0; i < live.size() && i < snapshot.size(); i++) {
            savedByEntry.put(live.get(i), snapshot.get(i));
        }
    }

    public ToolEntry savedOrDefaults(ToolEntry entry) {
        if (entry == null) return null;
        ToolEntry saved = savedByEntry.get(entry);
        return saved != null ? saved : defaultsFor(entry);
    }

    public ToolEntry defaultsFor(ToolEntry entry) {
        if (entry == null) return null;
        ToolEntry defaults = new ToolEntry();
        defaults.setItemId(entry.getItemId());
        seedFromItemDefaults(defaults, ConfigHandler.buildStack(entry).getItem());
        return defaults;
    }

    public void addFromStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;

        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        List<ToolEntry> existing = entries();
        for (int i = 0; i < existing.size(); i++) {
            ToolEntry candidate = existing.get(i);
            if (itemId.equals(candidate.getItemId()) && ConfigHandler.componentsMatch(stack, candidate)) {
                index = i;
                return;
            }
        }

        ToolEntry entry = new ToolEntry();
        entry.setItemId(itemId);
        entry.setComponents(ConfigHandler.encodeComponents(stack));
        entry.setMatchModelData(ItemDiscriminator.hasModelData(stack));
        seedFromItemDefaults(entry, stack.getItem());
        entries().add(entry);
        index = entries().size() - 1;
    }

    public void addEmpty() {
        ToolEntry entry = new ToolEntry();
        entry.setItemId("");
        entries().add(entry);
        index = entries().size() - 1;
    }

    public void removeSelected() {
        List<ToolEntry> entries = entries();
        if (index >= 0 && index < entries.size()) {
            entries.remove(index);
            if (index >= entries.size()) index = entries.size() - 1;
        }
    }

    public void move(int from, int to) {
        List<ToolEntry> entries = entries();
        entries.add(to, entries.remove(from));
        index = to;
    }

    public boolean addImported(ToolEntry imported) {
        List<ToolEntry> entries = entries();
        for (int i = 0; i < entries.size(); i++) {
            ToolEntry existing = entries.get(i);
            if (sameTarget(existing, imported)) {
                EntryProperty.copyAll(imported, existing);
                index = i;
                return true;
            }
        }
        entries.add(imported);
        index = entries.size() - 1;
        return false;
    }

    private static boolean sameTarget(ToolEntry a, ToolEntry b) {
        return Objects.equals(a.getItemId(), b.getItemId()) && ConfigHandler.sameDiscriminator(a, b);
    }

    private static void seedFromItemDefaults(ToolEntry entry, net.minecraft.world.item.Item item) {
        entry.setScale(ConfigHandler.defaultScale(item));
        entry.setAngleZ(ConfigHandler.defaultAngleZ(item));
        entry.setFlip(ConfigHandler.defaultFlip(item));
        entry.setDisplayContext(ConfigHandler.defaultDisplayContext(item).getSerializedName());
        float[] offset = ConfigHandler.defaultOffset(item);
        entry.setOffsetX(offset[0]);
        entry.setOffsetY(offset[1]);
        entry.setOffsetZ(offset[2]);
    }

    private static List<ToolEntry> deepCopy(List<ToolEntry> source) {
        List<ToolEntry> copy = new ArrayList<>();
        for (ToolEntry entry : source) {
            copy.add(new ToolEntry(entry.getItemId(), entry.getComponents(),
                    entry.isMatchName(), entry.isMatchModelData(), entry.isMatchItemModel(),
                    entry.isApplyToSimilar(), entry.isEnabled(), entry.isFlip(),
                    entry.getDisplayContext(), entry.getScale(),
                    entry.getAngleX(), entry.getAngleY(), entry.getAngleZ(),
                    entry.getOffsetX(), entry.getOffsetY(), entry.getOffsetZ()));
        }
        return copy;
    }
}
