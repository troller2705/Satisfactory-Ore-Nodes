package com.troller2705.satisfactory_ore_nodes;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(Satisfactory_ore_nodes.MODID)
public class Satisfactory_ore_nodes {
    public static final String MODID = "satisfactory_ore_nodes";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);

    // --- REGISTRATION ---

    // 1. The Resource Node Block
    public static final DeferredBlock<ResourceNodeBlock> IRON_NODE = BLOCKS.register("iron_node",
            () -> new ResourceNodeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.0f, 3600000.0f) // Hard to mine, but not bedrock-impossible
                    .requiresCorrectToolForDrops(), Items.RAW_IRON));

    public static final DeferredBlock<ResourceNodeBlock> COPPER_NODE = BLOCKS.register("copper_node",
            () -> new ResourceNodeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.0f, 3600000.0f) // Hard to mine, but not bedrock-impossible
                    .requiresCorrectToolForDrops(), Items.RAW_COPPER));

    // Register the Block Entity and link it to your Iron Node block
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ResourceNodeBlockEntity>> NODE_BE =
            BLOCK_ENTITY_TYPES.register("node_be", () ->
                    BlockEntityType.Builder.of(ResourceNodeBlockEntity::new, IRON_NODE.get(), COPPER_NODE.get()).build(null));

    // 2. The BlockItem so we can hold the node
    public static final DeferredItem<BlockItem> IRON_NODE_ITEM = ITEMS.registerSimpleBlockItem("iron_node", IRON_NODE);

    public static final DeferredItem<BlockItem> COPPER_NODE_ITEM = ITEMS.registerSimpleBlockItem("copper_node", COPPER_NODE);

    // 3. The Scanner Item
    public static final DeferredItem<Item> NODE_SCANNER = ITEMS.register("node_scanner",
            () -> new NodeScannerItem(new Item.Properties()));

    // 4. Creative Tab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SATISFACTORY_TAB = CREATIVE_MODE_TABS.register("satisfactory_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.satisfactory_ore_nodes"))
                    .icon(() -> IRON_NODE_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(IRON_NODE_ITEM.get());
                        output.accept(COPPER_NODE_ITEM.get());
                        output.accept(NODE_SCANNER.get());
                    }).build());

    public Satisfactory_ore_nodes(IEventBus modEventBus, ModContainer modContainer) {
        // Register FTB Library as the Config UI provider
        if (Dist.CLIENT.isClient()) {
            SatisfactoryFTBConfig.load(); // Load the .snbt file
            modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                    (mc, parent) -> {
                        SatisfactoryFTBConfig.openGui();
                        return null; // FTB opens its own screen immediately
                    });
        }

        // Register our registers to the event bus
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);

        modEventBus.addListener(this::commonSetup);


        // This is important: NeoForge.EVENT_BUS is for GAME events (like your BreakEvent)
        // modEventBus is for REGISTRATION events.
        NeoForge.EVENT_BUS.register(ModEvents.class);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Satisfactory Nodes Initializing...");
    }
}