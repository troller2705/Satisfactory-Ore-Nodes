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

@Mod(Satisfactory_ore_nodes.MODID)
public class Satisfactory_ore_nodes
{
    public static final String MODID = "satisfactory_ore_nodes";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);

    // ==========================================
    // BLOCK REGISTRATION
    // ==========================================

    // 1. The Resource Node (Solid Ores)
    public static final DeferredBlock<ResourceNodeBlock> MASTER_NODE = BLOCKS.register("resource_node",
            () -> new ResourceNodeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(2.0f, 6.0f)
                    .noOcclusion()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ResourceNodeBlockEntity>> NODE_BE =
            BLOCK_ENTITY_TYPES.register("node_be", () ->
                    BlockEntityType.Builder.of(ResourceNodeBlockEntity::new, MASTER_NODE.get()).build(null));

    // 2. The Fluid Node
    public static final DeferredBlock<FluidNodeBlock> FLUID_NODE = BLOCKS.register("fluid_node",
            () -> new FluidNodeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WATER)
                    .noOcclusion()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidNodeBlockEntity>> FLUID_NODE_BE =
            BLOCK_ENTITY_TYPES.register("fluid_node_be", () ->
                    BlockEntityType.Builder.of(FluidNodeBlockEntity::new, FLUID_NODE.get()).build(null));

    // 3. The Geyser Node
    public static final DeferredBlock<GeyserNodeBlock> GEYSER_NODE = BLOCKS.register("geyser_node",
            () -> new GeyserNodeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .noOcclusion()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GeyserNodeBlockEntity>> GEYSER_NODE_BE =
            BLOCK_ENTITY_TYPES.register("geyser_node_be", () ->
                    BlockEntityType.Builder.of(GeyserNodeBlockEntity::new, GEYSER_NODE.get()).build(null));

    // ==========================================
    // ITEM REGISTRATION
    // ==========================================

    public static final DeferredItem<BlockItem> MASTER_NODE_ITEM = ITEMS.register("resource_node",
            () -> new BlockItem(MASTER_NODE.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> FLUID_NODE_ITEM = ITEMS.register("fluid_node",
            () -> new BlockItem(FLUID_NODE.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> GEYSER_NODE_ITEM = ITEMS.register("geyser_node",
            () -> new BlockItem(GEYSER_NODE.get(), new Item.Properties()));

    public static final DeferredItem<Item> NODE_SCANNER = ITEMS.register("node_scanner",
            () -> new NodeScannerItem(new Item.Properties()));

    public static final DeferredItem<Item> EXTRACTOR_ITEM = ITEMS.register("extractor",
            FicsitExtractorItem::new);

    // ==========================================
    // CREATIVE TAB
    // ==========================================

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SATISFACTORY_TAB = CREATIVE_MODE_TABS.register("satisfactory_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.satisfactory_ore_nodes"))
                    .icon(() -> new ItemStack(Items.IRON_ORE))
                    .displayItems((parameters, output) -> {
                        output.accept(NODE_SCANNER.get());
                        output.accept(EXTRACTOR_ITEM.get());
                        output.accept(MASTER_NODE_ITEM.get());
                        output.accept(FLUID_NODE_ITEM.get()); // Added Fluid Node
                        output.accept(GEYSER_NODE_ITEM.get()); // Added Geyser Node
                    }).build());

    // ==========================================
    // ATTACHMENTS & SETUP
    // ==========================================

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
        if (Dist.CLIENT.isClient())
        {
            modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                    (mc, parent) ->
                    {
                        SatisfactoryFTBConfig.openGui();
                        return null;
                    });
        }

        SatisfactoryFTBConfig.load();

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        ATTACHMENT_TYPES.register(modEventBus);

        modEventBus.addListener(this::registerRenderers);
        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::buildContents);

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
            SatisfactoryFTBConfig.autoDiscoverOres();
            LOGGER.info("Satisfactory Nodes: Discovery complete. Found {} nodes.",
                    SatisfactoryFTBConfig.scannableNodes.size());
        });
    }

    private void buildContents(BuildCreativeModeTabContentsEvent event)
    {
        if (event.getTabKey() == SATISFACTORY_TAB.getKey())
        {
            if (SatisfactoryFTBConfig.scannableNodes.size() <= 1)
            {
                SatisfactoryFTBConfig.autoDiscoverOres();
            }
        }
    }

    private void registerRenderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event)
    {
        // Ore node uses the dynamic Java renderer
        event.registerBlockEntityRenderer(NODE_BE.get(), ResourceNodeRenderer::new);

        // Fluid node uses the new puddle renderer
        event.registerBlockEntityRenderer(FLUID_NODE_BE.get(), FluidNodeRenderer::new);
    }

    @SubscribeEvent
    public static void registerPackets(final net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) {
        event.registrar("satisfactory_ore_nodes")
                .playToServer(ScannerSelectPacket.TYPE, ScannerSelectPacket.STREAM_CODEC, ScannerSelectPacket::handle)
                .playToClient(OpenScannerMenuPacket.TYPE, OpenScannerMenuPacket.STREAM_CODEC, OpenScannerMenuPacket::handle);
    }
}