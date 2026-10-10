package com.troller2705.satisfactory_ore_nodes;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;
import java.util.List;

public class ResourceNodeBlock extends Block implements EntityBlock {

    public static final IntegerProperty PURITY = IntegerProperty.create("purity", 0, 2);
    public static final IntegerProperty ORE_INDEX = IntegerProperty.create("ore_index", 0, 64);

    public ResourceNodeBlock(Properties properties) {
        super(properties.strength(3.0f, 6.0f));
        this.registerDefaultState(this.stateDefinition.any().setValue(PURITY, 1).setValue(ORE_INDEX, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PURITY, ORE_INDEX);
    }

    // ==========================================
    // 1. RIGHT-CLICK LOGIC (Extraction & Config)
    // ==========================================
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);

        if (!stack.isEmpty() && level.getBlockEntity(pos) instanceof ResourceNodeBlockEntity nodeBE) {

            // --- ACTION A: EXTRACT RESOURCES WITH CUSTOM TOOL ---
            if (stack.getItem() instanceof FicsitExtractorItem) {

                // COOLDOWN CHECK: Stop macro spam right here
                if (player.getCooldowns().isOnCooldown(stack.getItem())) {
                    return InteractionResult.CONSUME;
                }

                if (!level.isClientSide) {
                    // Get the drops
                    ItemStack dropStack = getDropStack((ServerLevel) level, pos, state, nodeBE.getOreId(), stack);

                    // Pop it on the floor for the player
                    Block.popResource(level, pos, dropStack);

                    // Play a satisfying mining sound
                    level.playSound(null, pos, SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.BLOCKS, 1.0F, 1.0F);

                    // Apply the cooldown (15 ticks = 0.75 seconds)
                    player.getCooldowns().addCooldown(stack.getItem(), 15);
                }
                return InteractionResult.SUCCESS;
            }

            // --- ACTION B: CONFIGURE NODE WITH RAW ORE ---
            if (!level.isClientSide) {
                String heldItemPath = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
                String materialKey = heldItemPath.replace("raw_", "").replace("_ore", "").replace("_ingot", "");

                for (int i = 0; i < SatisfactoryFTBConfig.scannableNodes.size(); i++) {
                    var nodeEntry = SatisfactoryFTBConfig.scannableNodes.get(i);
                    String configBaseName = ResourceLocation.parse(nodeEntry.baseNodeId).getPath().replace("_ore", "");

                    if (materialKey.equals(configBaseName) || heldItemPath.contains(configBaseName)) {
                        int nextPurity = (state.getValue(PURITY) + 1) % 3;
                        nodeBE.setOreId(nodeEntry.baseNodeId);
                        nodeBE.setPurity(nextPurity);
                        level.setBlock(pos, state.setValue(PURITY, nextPurity).setValue(ORE_INDEX, i), 3);

                        player.displayClientMessage(Component.literal("Configured: " + nodeEntry.hudSymbol + " [" + nextPurity + "]")
                                .withStyle(ChatFormatting.GREEN), true);
                        return InteractionResult.SUCCESS;
                    }
                }
            }
        }
        return InteractionResult.PASS;
    }

    // ==========================================
    // 2. PREVENT LEFT-CLICK BREAKING
    // ==========================================
    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        // Permadelete Logic for Creative Mode (MUST run on both client and server to sync properly)
        if (player.isCreative() && player.isShiftKeyDown()) {
            return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
        }

        if (!level.isClientSide) {
            // Remind them to right-click
            player.displayClientMessage(Component.literal("Use Right-Click with a FICSIT Extractor to mine this node!").withStyle(ChatFormatting.RED), true);
            ((ServerLevel) level).sendBlockUpdated(pos, state, state, 3);
        }
        return false; // Cancels the break perfectly for normal mining
    }

    // ==========================================
    // 3. DRILL MINING & INFINITE RESPAWN
    // ==========================================
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && newState.isAir() && !isMoving) {
            ServerLevel serverLevel = (ServerLevel) level;

            // CRITICAL FIX: Stop the block from respawning if a Creative player just shift-broke it!
            Player nearestPlayer = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 5, false);
            if (nearestPlayer != null && nearestPlayer.isCreative() && nearestPlayer.isShiftKeyDown()) {
                super.onRemove(state, level, pos, newState, isMoving);
                return;
            }

            // Capture NBT before the block entity is wiped
            String savedOreId = "";
            if (serverLevel.getBlockEntity(pos) instanceof ResourceNodeBlockEntity be) {
                savedOreId = be.getOreId();
            }
            final String finalOreId = savedOreId;

            // Generate drops using our helper method
            ItemStack dropStack = getDropStack(serverLevel, pos, state, finalOreId, ItemStack.EMPTY);

            // Try inserting into ANY adjacent inventory
            for (Direction dir : Direction.values()) {
                BlockPos adjPos = pos.relative(dir);
                var itemHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, adjPos, dir.getOpposite());

                if (itemHandler != null) {
                    dropStack = ItemHandlerHelper.insertItemStacked(itemHandler, dropStack, false);
                    if (dropStack.isEmpty()) break;
                }
            }

            // If inventories are full/missing, safely spawn on ground
            if (!dropStack.isEmpty()) {
                ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, dropStack);
                entity.setPickUpDelay(10);
                level.addFreshEntity(entity);
            }

            // Infinite Respawn 1 tick later
            int purity = state.getValue(PURITY);
            int index = state.getValue(ORE_INDEX);
            serverLevel.getServer().tell(new net.minecraft.server.TickTask(serverLevel.getServer().getTickCount() + 1, () -> {
                serverLevel.setBlock(pos, state.setValue(PURITY, purity).setValue(ORE_INDEX, index), 3);
                if (serverLevel.getBlockEntity(pos) instanceof ResourceNodeBlockEntity newBE && !finalOreId.isEmpty()) {
                    newBE.setOreId(finalOreId);
                    newBE.setPurity(purity);
                }
            }));
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    // ==========================================
    // 4. SHARED DROP HELPER (Loot Table Driven)
    // ==========================================
    private ItemStack getDropStack(ServerLevel level, BlockPos pos, BlockState state, String oreId, ItemStack tool) {
        if (oreId == null || oreId.isEmpty() || oreId.equals("minecraft:air")) {
            int index = state.getValue(ORE_INDEX);
            if (index >= 0 && index < SatisfactoryFTBConfig.scannableNodes.size()) {
                oreId = SatisfactoryFTBConfig.scannableNodes.get(index).baseNodeId;
            }
        }

        ResourceLocation loc = ResourceLocation.parse(oreId);
        Block oreBlock = BuiltInRegistries.BLOCK.get(loc);
        BlockState oreState = oreBlock.defaultBlockState();

        // If no tool is provided (e.g., a Drill broke the node), simulate a max-tier tool
        // so the vanilla loot table doesn't reject the drop for lacking a required tier.
        ItemStack simulatedTool = (tool == null || tool.isEmpty()) ? new ItemStack(Items.NETHERITE_PICKAXE) : tool;

        // Build the native Loot Context parameters
        LootParams.Builder params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.TOOL, simulatedTool);

        // Query the underlying ore's actual JSON loot table
        List<ItemStack> drops = oreState.getDrops(params);

        // Fallback to iron nuggets if the loot table is somehow empty
        ItemStack finalDrop = drops.isEmpty() ? new ItemStack(Items.IRON_NUGGET) : drops.get(0).copy();

        // Apply purity multiplier to the base loot table drop count
        int multiplier = (int) Math.pow(2, state.getValue(PURITY));
        finalDrop.setCount(finalDrop.getCount() * multiplier);

        return finalDrop;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of();
    }

    @Override
    public net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
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

            String oreName = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(nodeBE.getOreId())).getName().getString();
            stack.set(DataComponents.CUSTOM_NAME, Component.literal(oreName + " Node"));
        }
        return stack;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ResourceNodeBlockEntity nodeBE) {
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