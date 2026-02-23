package com.troller2705.satisfactory_ore_nodes;

import dev.ftb.mods.ftblibrary.config.*;
import dev.ftb.mods.ftblibrary.config.ui.EditConfigScreen;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import net.minecraft.client.Minecraft;
import dev.ftb.mods.ftblibrary.snbt.SNBT;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import java.nio.file.Path;

import java.util.ArrayList;
import java.util.List;

public class SatisfactoryFTBConfig
{

    // This defines the common "c:ores" tag used by almost all mods in 1.21.1
    public static final TagKey<Block> C_ORES = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("c", "ores")
    );

    private static final Path CONFIG_FILE = Minecraft.getInstance().gameDirectory.toPath().resolve("config/satisfactory_nodes.snbt");

    public static void save()
    {
        SNBTCompoundTag root = new SNBTCompoundTag();
        root.putBoolean("show_hud", showHudText);
        root.putBoolean("show_compass", showCompass);
        root.putInt("scan_duration", scanDuration);

        ListTag nodesTag = new ListTag();
        for (NodeEntry node : scannableNodes)
        {
            CompoundTag nodeTag = new CompoundTag();
            nodeTag.putString("id", node.baseNodeId);
            nodeTag.putString("symbol", node.hudSymbol);
            nodeTag.putInt("color", node.baseColor.rgb());

            ListTag puritiesTag = new ListTag();
            for (PurityEntry p : node.purities)
            {
                CompoundTag pTag = new CompoundTag();
                pTag.putString("label", p.label);
                pTag.putString("visual", p.visualBlockId);
                puritiesTag.add(pTag);
            }
            nodeTag.put("purities", puritiesTag);
            nodesTag.add(nodeTag);
        }
        root.put("nodes", nodesTag);
        SNBT.write(CONFIG_FILE, root);
    }

    public static void load()
    {
        SNBTCompoundTag root = SNBT.read(CONFIG_FILE);
        if (root == null) return;

        showHudText = root.getBoolean("show_hud");
        showCompass = root.getBoolean("show_compass");
        scanDuration = root.getInt("scan_duration");

        if (root.contains("nodes", Tag.TAG_LIST))
        {
            scannableNodes.clear();
            ListTag nodesTag = root.getList("nodes", Tag.TAG_COMPOUND);
            for (int i = 0; i < nodesTag.size(); i++)
            {
                CompoundTag n = nodesTag.getCompound(i);
                List<PurityEntry> purities = new ArrayList<>();
                ListTag pList = n.getList("purities", Tag.TAG_COMPOUND);
                for (int j = 0; j < pList.size(); j++)
                {
                    CompoundTag p = pList.getCompound(j);
                    purities.add(new PurityEntry(p.getString("label"), p.getString("visual")));
                }
                scannableNodes.add(new NodeEntry(n.getString("id"), n.getString("symbol"), Color4I.rgb(n.getInt("color")), purities));
            }
        }
    }

    public static class PurityEntry
    {
        public String label;
        public String visualBlockId;

        public PurityEntry(String label, String visualBlockId)
        {
            this.label = label;
            this.visualBlockId = visualBlockId;
        }

        // Fix: Ensures a fresh instance is created for every new list entry
        public PurityEntry copy()
        {
            return new PurityEntry(this.label, this.visualBlockId);
        }

        @Override
        public String toString()
        {
            return label;
        }
    }

    public static class NodeEntry
    {
        public String baseNodeId;
        public String hudSymbol; // New Field: e.g., "Fe"
        public Color4I baseColor;
        public List<PurityEntry> purities;

        public NodeEntry(String baseNodeId, String hudSymbol, Color4I baseColor, List<PurityEntry> purities)
        {
            this.baseNodeId = baseNodeId;
            this.hudSymbol = hudSymbol;
            this.baseColor = baseColor;
            this.purities = purities;
        }

        // Fix: Deep copy ensures purities lists aren't shared between nodes
        public NodeEntry copy()
        {
            List<PurityEntry> pCopy = new ArrayList<>();
            for (PurityEntry p : purities) pCopy.add(p.copy());
            return new NodeEntry(this.baseNodeId, this.hudSymbol, this.baseColor, pCopy);
        }

        @Override
        public String toString()
        {
            return baseNodeId;
        }
    }

    public static boolean showHudText = true;
    public static boolean showCompass = true;
    public static int scanDuration = 60;

    public static List<NodeEntry> scannableNodes = new ArrayList<>(List.of(
            new NodeEntry("satisfactory_ore_nodes:iron_node", "Fe", Color4I.rgb(0x55FFFF), new ArrayList<>(List.of(
                    new PurityEntry("Impure", "minecraft:stone"),
                    new PurityEntry("Normal", "minecraft:iron_ore"),
                    new PurityEntry("Pure", "minecraft:raw_iron_block")
            )))
    ));

    public static void openGui()
    {
        Minecraft mc = Minecraft.getInstance();
        ConfigGroup mainGroup = new ConfigGroup(Satisfactory_ore_nodes.MODID, (accepted) ->
        {
            if (accepted)
            {
                save(); // Persistent save to satisfactory_nodes.snbt
            }
            mc.setScreen(null);
        });

        ConfigGroup uiGroup = mainGroup.getOrCreateSubgroup("ui_settings");
        uiGroup.addBool("show_hud_text", showHudText, b -> showHudText = b, true);
        uiGroup.addBool("show_compass", showCompass, b -> showCompass = b, true);

        ConfigGroup scanGroup = mainGroup.getOrCreateSubgroup("scanning");
        scanGroup.addInt("scan_duration", scanDuration, i -> scanDuration = i, 60, 5, 300);

        scanGroup.addList("scannable_nodes", scannableNodes, new ConfigValue<NodeEntry>()
        {
            @Override
            public void onClicked(Widget widget, MouseButton button, ConfigCallback callback)
            {
                // We create a CLONE for editing so we don't touch the original until accepted
                final NodeEntry editingNode = value.copy();
                ConfigGroup entryGroup = new ConfigGroup("edit_node", (accepted) ->
                {
                    if (accepted)
                    {
                        // Apply the changes back to the actual object in the list
                        value.baseNodeId = editingNode.baseNodeId;
                        value.baseColor = editingNode.baseColor;
                        value.purities = editingNode.purities;
                        callback.save(true);
                    }
                });

                entryGroup.addString("node_block_id", editingNode.baseNodeId, s -> editingNode.baseNodeId = s, "");
                entryGroup.add("base_node_color", new ColorConfig(), editingNode.baseColor, c -> editingNode.baseColor = c, Color4I.WHITE);

                // Nested Purity Editor
                entryGroup.addList("purity_settings", editingNode.purities, new ConfigValue<PurityEntry>()
                {
                    @Override
                    public void onClicked(Widget w, MouseButton b, ConfigCallback cb)
                    {
                        // Again, create a LOCAL copy for this specific purity row
                        final PurityEntry editingPurity = value.copy();
                        ConfigGroup pGroup = new ConfigGroup("edit_purity", (pAccepted) ->
                        {
                            if (pAccepted)
                            {
                                value.label = editingPurity.label;
                                value.visualBlockId = editingPurity.visualBlockId;
                                cb.save(true);
                            }
                        });

                        pGroup.addString("purity_label", editingPurity.label, s -> editingPurity.label = s, "");
                        pGroup.addString("visual_block_id", editingPurity.visualBlockId, s -> editingPurity.visualBlockId = s, "");

                        new EditConfigScreen(pGroup).openGui();
                    }
                }, new PurityEntry("New Purity", "minecraft:air").copy());

                new EditConfigScreen(entryGroup).openGui();
            }
        }, new NodeEntry("new_node", "??", Color4I.WHITE, new ArrayList<>()).copy());

        new EditConfigScreen(mainGroup).openGui();
    }

    public static void autoDiscoverOres() {
        var itemRegistry = BuiltInRegistries.ITEM;
        var blockRegistry = BuiltInRegistries.BLOCK;

        itemRegistry.holders().forEach(holder -> {
            // Check for "ore" in the name if tags are failing us
            String path = holder.key().location().getPath();
            String namespace = holder.key().location().getNamespace();

            // Filter: Must have "ore" in the name and NOT be from our own mod
            if (path.contains("ore") && !namespace.equals(Satisfactory_ore_nodes.MODID)) {
                ResourceLocation loc = holder.key().location();
                String oreId = loc.toString();

                // Make sure this item actually has a block associated with it
                if (!blockRegistry.containsKey(loc)) return;

                // Skip if already configured
                if (scannableNodes.stream().anyMatch(n -> n.baseNodeId.equals(oreId))) return;

                // Guess Symbol (e.g., "tin_ore" -> "TI")
                String guessedSymbol = path.substring(0, Math.min(path.length(), 2)).toUpperCase();

                // Create: Compacted Logic
                String simpleName = path.replace("_ore", "").replace("ore_", "");
                ResourceLocation compactLoc = ResourceLocation.fromNamespaceAndPath("create_compacted", "compact_" + simpleName);
                String compactId = blockRegistry.containsKey(compactLoc) ? compactLoc.toString() : oreId;

                List<PurityEntry> autoPurities = new ArrayList<>(List.of(
                        new PurityEntry("Impure", oreId),
                        new PurityEntry("Normal", oreId),
                        new PurityEntry("Pure", compactId)
                ));

                scannableNodes.add(new NodeEntry(oreId, guessedSymbol, Color4I.GRAY, autoPurities));
            }
        });
        save();
    }
}