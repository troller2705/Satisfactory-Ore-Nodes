package com.troller2705.satisfactory_ore_nodes;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;

public class ResourceNodeBlock extends Block implements EntityBlock
{
    // Define the property (0 to 2)
    public static final IntegerProperty PURITY = IntegerProperty.create("purity", 0, 2);
    public static final IntegerProperty ORE_INDEX = IntegerProperty.create("ore_index", 0, 64);

    public ResourceNodeBlock(Properties properties) {
        // strength(-1.0F, 3600000.0F) makes it bedrock-like (unbreakable by normal means)
        // but the BreakEvent will still fire when a player "tries" to mine it or a Drill hits it.
        super(properties.strength(2.0f, 6.0f));
        this.registerDefaultState(this.stateDefinition.any().setValue(PURITY, 1).setValue(ORE_INDEX, 0));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);

        if (!stack.isEmpty() && !level.isClientSide && level.getBlockEntity(pos) instanceof ResourceNodeBlockEntity nodeBE) {
            // Get the item's name (e.g., "minecraft:raw_iron" or "create:copper_ore")
            String heldItemPath = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();

            // Standardize the name by stripping common prefixes/suffixes
            String materialKey = heldItemPath.replace("raw_", "").replace("_ore", "").replace("_ingot", "");

            for (int i = 0; i < SatisfactoryFTBConfig.scannableNodes.size(); i++) {
                var nodeEntry = SatisfactoryFTBConfig.scannableNodes.get(i);
                String configBaseName = ResourceLocation.parse(nodeEntry.baseNodeId).getPath().replace("_ore", "");

                // If the item in hand matches a material in your config
                if (materialKey.equals(configBaseName) || heldItemPath.contains(configBaseName)) {

                    // Cycle purity: 0 (Impure) -> 1 (Normal) -> 2 (Pure)
                    int nextPurity = (state.getValue(PURITY) + 1) % 3;

                    // Sync the BlockEntity and BlockState
                    nodeBE.setOreId(nodeEntry.baseNodeId);
                    nodeBE.setPurity(nextPurity);
                    level.setBlock(pos, state.setValue(PURITY, nextPurity).setValue(ORE_INDEX, i), 3);

                    player.displayClientMessage(Component.literal("Configured: " + nodeEntry.hudSymbol + " [" + nextPurity + "]")
                            .withStyle(ChatFormatting.GREEN), true);

                    return InteractionResult.SUCCESS;
                }
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PURITY, ORE_INDEX);
    }

    private void handleHarvest(Level level, BlockPos pos, BlockState state, @Nullable Player player) {
        BlockEntity be = level.getBlockEntity(pos);
        String oreId = "";

        // 1. Get the ID from the BlockEntity if it still exists
        if (be instanceof ResourceNodeBlockEntity nodeBE) {
            oreId = nodeBE.getOreId();
        }

        // 2. Fallback: If BE is already gone, use the ORE_INDEX from the BlockState
        if (oreId == null || oreId.isEmpty() || oreId.equals("minecraft:air")) {
            int index = state.getValue(ORE_INDEX);
            if (index >= 0 && index < SatisfactoryFTBConfig.scannableNodes.size()) {
                oreId = SatisfactoryFTBConfig.scannableNodes.get(index).baseNodeId;
            }
        }

        // 3. Determine the drop item
        ResourceLocation loc = ResourceLocation.parse(oreId);
        Block oreBlock = BuiltInRegistries.BLOCK.get(loc);
        Item drop = oreBlock.asItem();

        // Fix: If the block doesn't drop itself (like Iron Ore), force the Raw version
        if (drop == Items.AIR || oreId.contains("_ore")) {
            if (oreId.contains("iron")) drop = Items.RAW_IRON;
            else if (oreId.contains("copper")) drop = Items.RAW_COPPER;
            else if (oreId.contains("gold")) drop = Items.RAW_GOLD;
            else if (oreId.contains("coal")) drop = Items.COAL;
            else if (oreId.contains("uranium")) drop = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "raw_uranium"));
        }

        // Final safety fallback
        if (drop == Items.AIR) drop = Items.IRON_NUGGET;

        int count = (int) Math.pow(2, state.getValue(PURITY));
        ItemStack stack = new ItemStack(drop, count);

        // 4. Physical Spawning Logic
        if (player != null) {
            Block.popResource(level, pos, stack);
        } else {
            BlockPos collectionPos = pos.below();
            var itemHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, collectionPos, Direction.UP);

            if (itemHandler != null) {
                ItemHandlerHelper.insertItemStacked(itemHandler, stack, false);
            } else {
                // Ensure entities only spawn on the server
                if (!level.isClientSide) {
                    ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
                    entity.setPickUpDelay(10);
                    level.addFreshEntity(entity);
                }
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && newState.isAir() && !isMoving) {
            ServerLevel serverLevel = (ServerLevel) level;
            Player player = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 5, false);

            // Permadelete logic
            if (player != null && player.isCreative() && player.isShiftKeyDown()) {
                super.onRemove(state, level, pos, newState, isMoving);
                return;
            }

            this.handleHarvest(serverLevel, pos, state, null);

            // Infinite Respawn: Preserve state data
            int purity = state.getValue(PURITY);
            int index = state.getValue(ORE_INDEX);
            serverLevel.getServer().execute(() -> {
                serverLevel.setBlock(pos, state.setValue(PURITY, purity).setValue(ORE_INDEX, index), 3);
            });
        } else {
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Override
    public net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        // This tells Minecraft to use your BlockEntityRenderer (ResourceNodeRenderer)
        // for visuals instead of a .json model.
        return net.minecraft.world.level.block.RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        ItemStack stack = new ItemStack(this);
        BlockEntity be = level.getBlockEntity(pos);

        if (be instanceof ResourceNodeBlockEntity nodeBE) {
            CompoundTag tag = new CompoundTag();
            tag.putString("oreId", nodeBE.getOreId());
            tag.putInt("purity", state.getValue(PURITY));

            BlockItem.setBlockEntityData(stack, Satisfactory_ore_nodes.NODE_BE.get(), tag);

            // Dynamic Name for the item in your hand
            String oreName = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(nodeBE.getOreId()))
                    .getName().getString();
            stack.set(DataComponents.CUSTOM_NAME, Component.literal(oreName + " Node"));
        }

        return stack;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        // Nodes now start as "Blank" or "Iron" by default until configured by right-clicking with an ore
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ResourceNodeBlockEntity nodeBE) {
            // Optional: Initialize with a default from your config
            if (!SatisfactoryFTBConfig.scannableNodes.isEmpty()) {
                var defaultNode = SatisfactoryFTBConfig.scannableNodes.get(0);
                nodeBE.setOreId(defaultNode.baseNodeId);
                level.setBlock(pos, state.setValue(PURITY, 1).setValue(ORE_INDEX, 0), 3);
            }
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ResourceNodeBlockEntity(pos, state);
    }
}
