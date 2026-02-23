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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
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

    // In ResourceNodeBlock.java
    public ResourceNodeBlock(Properties properties) {
        // strength(-1.0F, 3600000.0F) makes it bedrock-like (unbreakable by normal means)
        // but the BreakEvent will still fire when a player "tries" to mine it or a Drill hits it.
        super(properties.strength(2.0f, 3600000.0f));
        this.registerDefaultState(this.stateDefinition.any().setValue(PURITY, 1).setValue(ORE_INDEX, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PURITY, ORE_INDEX);
    }

    private void handleHarvest(Level level, BlockPos pos, BlockState state, @Nullable Player player) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ResourceNodeBlockEntity nodeBE)) return;

        String oreId = nodeBE.getOreId();

        // Use the Registry to find the block, then get its item
        Block oreBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(oreId));
        Item drop = oreBlock.asItem();

        // If it's a block like 'Deepslate Iron Ore', it might drop 'Raw Iron'
        // This logic gets the standard drop for that block
        if (drop == Items.AIR) drop = Items.RAW_IRON;

        int count = (int) Math.pow(2, state.getValue(PURITY));
        ItemStack stack = new ItemStack(drop, count);

        if (player != null) {
            Block.popResource(level, pos, stack);
        } else {
            // MODERN CREATE 0.6 LOGIC:
            // Instead of spawning an entity, we look for an inventory to push into.
            // We check the block BELOW the node (where your chute/belt is).
            BlockPos collectionPos = pos.below();

            // Use NeoForge's Capability system to find an inventory (IItemHandler)
            var itemHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, collectionPos, Direction.UP);

            if (itemHandler != null) {
                // Push items directly into the Chute/Chest/Belt
                ItemHandlerHelper.insertItemStacked(itemHandler, stack, false);
            } else {
                // Fallback for manual mining or no-inventory setups
                ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() - 0.2, pos.getZ() + 0.5, stack);
                entity.setDeltaMovement(0, -0.1, 0);
                level.addFreshEntity(entity);
            }
        }
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity te, ItemStack stack) {
        // This handles manual mining
        super.playerDestroy(level, player, pos, state, te, stack);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (newState.isAir() && !isMoving) {
            if (level instanceof ServerLevel serverLevel) {
                // Find the nearest player to see if they are shift-breaking in creative
                Player player = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 5, false);

                if (player != null && player.isCreative() && player.isShiftKeyDown()) {
                    // The player wants to delete the block. Let it happen.
                    super.onRemove(state, level, pos, newState, isMoving);
                    return;
                }

                // --- Otherwise, handle the Infinite Respawn (for Drills/Mining) ---
                int purity = state.getValue(PURITY);
                this.handleHarvest(serverLevel, pos, state, null);

                serverLevel.getServer().execute(() -> {
                    serverLevel.setBlock(pos, state.setValue(PURITY, purity), 3);
                });
            }
        } else {
            super.onRemove(state, level, pos, newState, isMoving);
        }
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
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ResourceNodeBlockEntity nodeBE) {
            CustomData data = stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY);
            CompoundTag tag = data.copyTag();

            String id = tag.getString("oreId");
            int p = tag.getInt("purity");

            // Find the index of this ore in your scannableNodes list
            int index = 0;
            for (int i = 0; i < SatisfactoryFTBConfig.scannableNodes.size(); i++) {
                if (SatisfactoryFTBConfig.scannableNodes.get(i).baseNodeId.equals(id)) {
                    index = i;
                    break;
                }
            }

            nodeBE.setOreId(id);
            nodeBE.setPurity(p);

            // SYNC BOTH: This makes ore_index show up in F3 just like purity!
            level.setBlock(pos, state.setValue(PURITY, p).setValue(ORE_INDEX, index), 3);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ResourceNodeBlockEntity(pos, state);
    }
}
