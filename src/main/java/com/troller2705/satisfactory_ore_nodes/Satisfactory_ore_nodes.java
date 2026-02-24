package com.troller2705.satisfactory_ore_nodes;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
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
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.*;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(Satisfactory_ore_nodes.MODID)
public class Satisfactory_ore_nodes
{
    public static final String MODID = "satisfactory_ore_nodes";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);

    // --- REGISTRATION ---

    // 1. The Resource Node Block
    public static final DeferredBlock<ResourceNodeBlock> MASTER_NODE = BLOCKS.register("resource_node",
            () -> new ResourceNodeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(2.0f, 6.0f) // Standard hardness
                    .noOcclusion())); // Allows the renderer to show through


    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ResourceNodeBlockEntity>> NODE_BE =
            BLOCK_ENTITY_TYPES.register("node_be", () ->
                    BlockEntityType.Builder.of(ResourceNodeBlockEntity::new, MASTER_NODE.get()).build(null));

    // 2. The Resource Node Item
    public static final DeferredItem<BlockItem> MASTER_NODE_ITEM = ITEMS.register("resource_node",
            () -> new BlockItem(MASTER_NODE.get(), new Item.Properties()));

    // 3. The Scanner Item
    public static final DeferredItem<Item> NODE_SCANNER = ITEMS.register("node_scanner",
            () -> new NodeScannerItem(new Item.Properties()));

    // 4. Creative Tab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SATISFACTORY_TAB = CREATIVE_MODE_TABS.register("satisfactory_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.satisfactory_ore_nodes"))
                    .icon(() -> new ItemStack(Items.IRON_ORE))
                    .displayItems((parameters, output) -> {
                        // Always use .get() for Deferred objects
                        output.accept(NODE_SCANNER.get());
                        output.accept(MASTER_NODE_ITEM.get());

                        // Since we are using the "original items" approach,
                        // add common ores here so players have them for configuration
                        output.accept(Items.RAW_IRON);
                        output.accept(Items.RAW_COPPER);
                        output.accept(Items.RAW_GOLD);
                        output.accept(Items.COAL);
                    }).build());

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<List<String>>> UNLOCKED_ORES =
            ATTACHMENT_TYPES.register("unlocked_ores",
                    () -> AttachmentType.builder(() -> (List<String>) new ArrayList<String>())
                            .serialize(Codec.list(Codec.STRING))
                            .copyOnDeath()
                            .build());

    public Satisfactory_ore_nodes(IEventBus modEventBus, ModContainer modContainer)
    {
        // Register FTB Library as the Config UI provider
        if (Dist.CLIENT.isClient())
        {
            modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                    (mc, parent) ->
                    {
                        SatisfactoryFTBConfig.openGui();
                        return null; // FTB opens its own screen immediately
                    });
        }

        SatisfactoryFTBConfig.load();


        // Register our registers to the event bus
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        ATTACHMENT_TYPES.register(modEventBus);

        modEventBus.addListener(this::registerRenderers);
        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::buildContents);


        // This is important: NeoForge.EVENT_BUS is for GAME events (like your BreakEvent)
        // modEventBus is for REGISTRATION events.
        NeoForge.EVENT_BUS.register(ModEvents.class);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        LOGGER.info("Satisfactory Nodes Initializing...");
    }

    private void clientSetup(final FMLClientSetupEvent event)
    {
        event.enqueueWork(() ->
        {

            // NOW run discovery when tags are ready
            SatisfactoryFTBConfig.autoDiscoverOres();

            LOGGER.info("Satisfactory Nodes: Discovery complete. Found {} nodes.",
                    SatisfactoryFTBConfig.scannableNodes.size());
        });
    }

    private void buildContents(BuildCreativeModeTabContentsEvent event)
    {
        if (event.getTabKey() == SATISFACTORY_TAB.getKey())
        {
            // Run discovery right as the tab is being opened for the first time
            if (SatisfactoryFTBConfig.scannableNodes.size() <= 1)
            { // 1 is your default Iron Node
                SatisfactoryFTBConfig.autoDiscoverOres();
            }
        }
    }

    private void registerRenderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event)
    {
        event.registerBlockEntityRenderer(NODE_BE.get(), ResourceNodeRenderer::new);
    }
}