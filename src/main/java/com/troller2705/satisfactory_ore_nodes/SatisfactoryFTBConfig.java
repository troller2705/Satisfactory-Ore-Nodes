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
import java.util.Map;

public class SatisfactoryFTBConfig
{
    // Common "c:ores" tag
    public static final TagKey<Block> C_ORES = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("c", "ores")
    );

    private static final Path CONFIG_FILE = Minecraft.getInstance().gameDirectory.toPath().resolve("config/satisfactory_nodes.snbt");

    // Periodic Table Symbol Dictionary
    private static final Map<String, String> ELEMENT_SYMBOLS = Map.ofEntries(
            Map.entry("iron", "Fe"),
            Map.entry("gold", "Au"),
            Map.entry("copper", "Cu"),
            Map.entry("tin", "Sn"),
            Map.entry("lead", "Pb"),
            Map.entry("silver", "Ag"),
            Map.entry("nickel", "Ni"),
            Map.entry("platinum", "Pt"),
            Map.entry("zinc", "Zn"),
            Map.entry("aluminum", "Al"),
            Map.entry("aluminium", "Al"),
            Map.entry("bauxite", "Al"),
            Map.entry("uranium", "U"),
            Map.entry("osmium", "Os"),
            Map.entry("titanium", "Ti"),
            Map.entry("tungsten", "W"),
            Map.entry("lithium", "Li"),
            Map.entry("cobalt", "Co"),
            Map.entry("iridium", "Ir"),
            // Non-Metals & Satisfactory Specials
            Map.entry("coal", "C"),
            Map.entry("diamond", "C"),
            Map.entry("emerald", "Em"),
            Map.entry("lapis", "Lp"),
            Map.entry("redstone", "Rs"),
            Map.entry("quartz", "Qz"),
            Map.entry("sulfur", "S"),
            Map.entry("sam", "SAM"),
            Map.entry("limestone", "Ls"),
            Map.entry("caterium", "Ct"),
            // Fluids / Gases
            Map.entry("water", "H2O"),
            Map.entry("lava", "Lva"),
            Map.entry("nitrogen", "N"),
            Map.entry("oil", "Oil"),
            Map.entry("crude_oil", "Oil")
    );

    // Periodic Table Color Dictionary (Hex 0xRRGGBB)
    private static final Map<String, Integer> ELEMENT_COLORS = Map.ofEntries(
            Map.entry("iron", 0xD8AF93),       // Minecraft Iron Tan/Grey
            Map.entry("gold", 0xFDF55F),       // Yellow Gold
            Map.entry("copper", 0xE77C56),     // Orange Copper
            Map.entry("tin", 0x9CB4B8),        // Light Blueish-Grey
            Map.entry("lead", 0x4D4D4F),       // Dark Grey
            Map.entry("silver", 0xC0C0C0),     // Silver
            Map.entry("nickel", 0xE8E8B7),     // Pale Yellow-Grey
            Map.entry("platinum", 0xA3C2C2),   // Cyan-Grey
            Map.entry("zinc", 0xA5B79C),       // Pale Green-Grey
            Map.entry("aluminum", 0xCDD6DD),   // Light Blue-Grey
            Map.entry("aluminium", 0xCDD6DD),  // Light Blue-Grey
            Map.entry("bauxite", 0xCC7B70),    // Satisfactory Bauxite Pink/Salmon
            Map.entry("uranium", 0x39FF14),    // Neon Green
            Map.entry("osmium", 0x7A8EA3),     // Slate Blue
            Map.entry("titanium", 0xE0E0E0),   // White-Grey
            Map.entry("tungsten", 0x303030),   // Very Dark Grey
            Map.entry("lithium", 0xE6E6E6),    // White
            Map.entry("cobalt", 0x0047AB),     // Cobalt Blue
            Map.entry("iridium", 0xE6E6E6),    // White-Grey
            Map.entry("coal", 0x2A2A2A),       // Dark Grey/Black
            Map.entry("diamond", 0x4AEDE0),    // Cyan
            Map.entry("emerald", 0x41F384),    // Emerald Green
            Map.entry("lapis", 0x27438A),      // Deep Blue
            Map.entry("redstone", 0xFF0000),   // Bright Red
            Map.entry("quartz", 0xE8E4DE),     // Off-White
            Map.entry("sulfur", 0xE2E034),     // Sulfur Yellow
            Map.entry("sam", 0x981AE8),        // Strange Alien Purple
            Map.entry("limestone", 0xD3D3CB),  // Pale Tan/Grey
            Map.entry("caterium", 0xF0D24D),   // Bright Gold
            Map.entry("water", 0x3498DB),      // Liquid Blue
            Map.entry("lava", 0xFF5500),       // Magma Orange
            Map.entry("nitrogen", 0x00FFFF),   // Cyan Gas
            Map.entry("oil", 0x111111),        // Pitch Black
            Map.entry("crude_oil", 0x111111)   // Pitch Black
    );

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
        public String hudSymbol;
        public Color4I baseColor;
        public List<PurityEntry> purities;

        public NodeEntry(String baseNodeId, String hudSymbol, Color4I baseColor, List<PurityEntry> purities)
        {
            this.baseNodeId = baseNodeId;
            this.hudSymbol = hudSymbol;
            this.baseColor = baseColor;
            this.purities = purities;
        }

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

    // Starts completely empty! Everything is auto-discovered now.
    public static List<NodeEntry> scannableNodes = new ArrayList<>();

    public static void openGui()
    {
        Minecraft mc = Minecraft.getInstance();
        ConfigGroup mainGroup = new ConfigGroup(Satisfactory_ore_nodes.MODID, (accepted) ->
        {
            if (accepted)
            {
                save();
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
                final NodeEntry editingNode = value.copy();
                ConfigGroup entryGroup = new ConfigGroup("edit_node", (accepted) ->
                {
                    if (accepted)
                    {
                        value.baseNodeId = editingNode.baseNodeId;
                        value.hudSymbol = editingNode.hudSymbol;
                        value.baseColor = editingNode.baseColor;
                        value.purities = editingNode.purities;
                        callback.save(true);
                    }
                });

                entryGroup.addString("node_block_id", editingNode.baseNodeId, s -> editingNode.baseNodeId = s, "");
                entryGroup.addString("hud_symbol", editingNode.hudSymbol, s -> editingNode.hudSymbol = s, "??");
                entryGroup.add("base_node_color", new ColorConfig(), editingNode.baseColor, c -> editingNode.baseColor = c, Color4I.WHITE);

                entryGroup.addList("purity_settings", editingNode.purities, new ConfigValue<PurityEntry>()
                {
                    @Override
                    public void onClicked(Widget w, MouseButton b, ConfigCallback cb)
                    {
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
        var fluidRegistry = BuiltInRegistries.FLUID;

        // ==========================================
        // 1. AUTO-DISCOVER ORES
        // ==========================================
        itemRegistry.holders().forEach(holder -> {
            String path = holder.key().location().getPath();
            String namespace = holder.key().location().getNamespace();

            if ((path.contains("ore") || path.contains("limestone")) && !namespace.equals(Satisfactory_ore_nodes.MODID) && !path.contains("deepslate")) {
                ResourceLocation loc = holder.key().location();
                String oreId = loc.toString();

                if (!blockRegistry.containsKey(loc)) return;
                if (scannableNodes.stream().anyMatch(n -> n.baseNodeId.equals(oreId))) return;

                String simpleName = path.replace("_ore", "").replace("ore_", "");

                String symbol;
                if (ELEMENT_SYMBOLS.containsKey(simpleName)) {
                    symbol = ELEMENT_SYMBOLS.get(simpleName);
                } else {
                    String guessed = simpleName.substring(0, Math.min(simpleName.length(), 2));
                    symbol = guessed.substring(0, 1).toUpperCase() + guessed.substring(1).toLowerCase();
                }

                int colorHex = ELEMENT_COLORS.getOrDefault(simpleName, 0xAAAAAA);

                ResourceLocation compactLoc = ResourceLocation.fromNamespaceAndPath("create_compacted", "compact_" + simpleName);
                String compactId = blockRegistry.containsKey(compactLoc) ? compactLoc.toString() : oreId;

                List<PurityEntry> autoPurities = new ArrayList<>(List.of(
                        new PurityEntry("Impure", oreId),
                        new PurityEntry("Normal", oreId),
                        new PurityEntry("Pure", compactId)
                ));

                scannableNodes.add(new NodeEntry(oreId, symbol, Color4I.rgb(colorHex), autoPurities));
            }
        });

        // ==========================================
        // 2. AUTO-DISCOVER FLUIDS
        // ==========================================
        fluidRegistry.holders().forEach(holder -> {
            String path = holder.key().location().getPath();

            // Skip flowing variants so we don't double-register fluids
            if (path.contains("flowing")) return;

            // Target essential Satisfactory-themed fluids from any mod
            if (path.equals("water") || path.equals("lava") || path.contains("oil") || path.contains("nitrogen") || path.contains("fuel")) {
                String fluidId = holder.key().location().toString();

                if (scannableNodes.stream().anyMatch(n -> n.baseNodeId.equals(fluidId))) return;

                String simpleName = path;

                String symbol;
                if (ELEMENT_SYMBOLS.containsKey(simpleName)) {
                    symbol = ELEMENT_SYMBOLS.get(simpleName);
                } else {
                    String guessed = simpleName.substring(0, Math.min(simpleName.length(), 2));
                    symbol = guessed.substring(0, 1).toUpperCase() + guessed.substring(1).toLowerCase();
                }

                int colorHex = ELEMENT_COLORS.getOrDefault(simpleName, 0x3498DB);

                // Point all fluids to our invisible fluid visual block so the Java puddle renderer can handle it
                List<PurityEntry> fluidPurities = new ArrayList<>(List.of(
                        new PurityEntry("Impure", "satisfactory_ore_nodes:fluid_node"),
                        new PurityEntry("Normal", "satisfactory_ore_nodes:fluid_node"),
                        new PurityEntry("Pure", "satisfactory_ore_nodes:fluid_node")
                ));

                scannableNodes.add(new NodeEntry(fluidId, symbol, Color4I.rgb(colorHex), fluidPurities));
            }
        });

        // ==========================================
        // 3. AUTO-DISCOVER GEYSER
        // ==========================================
        String geyserId = "satisfactory_ore_nodes:geyser_node";
        if (scannableNodes.stream().noneMatch(n -> n.baseNodeId.equals(geyserId))) {
            List<PurityEntry> geyserPurities = new ArrayList<>(List.of(
                    new PurityEntry("Impure", geyserId),
                    new PurityEntry("Normal", geyserId),
                    new PurityEntry("Pure", geyserId)
            ));
            scannableNodes.add(new NodeEntry(geyserId, "Gy", Color4I.rgb(0xAAAAAA), geyserPurities));
        }

        save();
    }
}