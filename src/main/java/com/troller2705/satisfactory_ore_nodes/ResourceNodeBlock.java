package com.troller2705.satisfactory_ore_nodes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;

public class ResourceNodeBlock extends Block implements EntityBlock
{
    // Define the property (0 to 2)
    public static final IntegerProperty PURITY = IntegerProperty.create("purity", 0, 2);

    // In ResourceNodeBlock.java
    public ResourceNodeBlock(Properties properties) {
        // strength(-1.0F, 3600000.0F) makes it bedrock-like (unbreakable by normal means)
        // but the BreakEvent will still fire when a player "tries" to mine it or a Drill hits it.
        super(properties.strength(2.0f, 3600000.0f));
        this.registerDefaultState(this.stateDefinition.any().setValue(PURITY, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PURITY);
    }

    private void handleHarvest(Level level, BlockPos pos, BlockState state, @Nullable Player player) {
        int purity = state.getValue(PURITY);
        int count = (int) Math.pow(2, purity);
        ItemStack stack = new ItemStack(Items.RAW_IRON, count);

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
                // This is the part that stationary drills trigger
                this.handleHarvest(serverLevel, pos, state, null);

                // Re-spawn the block
                serverLevel.getServer().execute(() -> {
                    serverLevel.setBlock(pos, state, 3);
                });
            }
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        BlockState state = event.getState();

        if (state.getBlock() instanceof ResourceNodeBlock) {
            if (event.getLevel() instanceof Level serverLevel) {
                BlockPos pos = event.getPos();
                Player player = event.getPlayer();
                int purity = state.getValue(ResourceNodeBlock.PURITY);

                // 1. Calculate and drop items
                int count = (int) Math.pow(2, purity);
                Block.popResource(serverLevel, pos, new ItemStack(Items.RAW_IRON, count));

                // 2. Feedback for the player (if it's a player mining)
                if (player != null) {
                    String purityName = purity == 0 ? "Impure" : (purity == 2 ? "Pure" : "Normal");
                    player.displayClientMessage(Component.literal("Harvested " + purityName + " Node"), true);
                }

                // 3. CANCEL the break and sync with client to prevent ghosting
                event.setCanceled(true);
                serverLevel.sendBlockUpdated(pos, state, state, 3);
            }
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ResourceNodeBlockEntity(pos, state);
    }
}
