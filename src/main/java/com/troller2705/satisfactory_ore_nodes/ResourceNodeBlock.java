package com.troller2705.satisfactory_ore_nodes;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
    private final Item dropItem;

    // In ResourceNodeBlock.java
    public ResourceNodeBlock(Properties properties, Item dropItem) {
        // strength(-1.0F, 3600000.0F) makes it bedrock-like (unbreakable by normal means)
        // but the BreakEvent will still fire when a player "tries" to mine it or a Drill hits it.
        super(properties.strength(2.0f, 3600000.0f));
        this.dropItem = dropItem; // e.g., Items.RAW_COPPER
        this.registerDefaultState(this.stateDefinition.any().setValue(PURITY, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PURITY);
    }

    public Item getDropItem() {
        return this.dropItem;
    }

    private void handleHarvest(Level level, BlockPos pos, BlockState state, @Nullable Player player) {
        int purity = state.getValue(PURITY);
        int count = (int) Math.pow(2, purity);
        ItemStack stack = new ItemStack(this.dropItem, count);

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
        ItemStack stack = super.getCloneItemStack(state, target, level, pos, player);
        int purity = state.getValue(PURITY);

        // 1. Set the Display Name
        String purityName = purity == 0 ? "Impure" : (purity == 2 ? "Pure" : "Normal");
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(purityName + " " + this.getName().getString())
                .withStyle(purity == 2 ? ChatFormatting.AQUA : ChatFormatting.GRAY));

        // 2. Fix: Create the tag separately
        CompoundTag tag = new CompoundTag();
        tag.putInt("purity", purity);

        // 3. Apply the tag to the BlockItem's data
        BlockItem.setBlockEntityData(stack, Satisfactory_ore_nodes.NODE_BE.get(), tag);

        return stack;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        // When placed from a stack that has NBT data, update the block state
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ResourceNodeBlockEntity nodeBE) {
                // If the item had a "purity" tag, update the block
                CompoundTag tag = stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY).copyTag();
                if (tag.contains("purity")) {
                    int p = tag.getInt("purity");
                    level.setBlock(pos, state.setValue(PURITY, p), 3);
                }
            }
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ResourceNodeBlockEntity(pos, state);
    }
}
