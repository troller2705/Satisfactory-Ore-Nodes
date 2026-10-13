package com.troller2705.satisfactory_ore_nodes;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public class FluidNodeBlock extends Block implements EntityBlock {

    public static final IntegerProperty PURITY = IntegerProperty.create("purity", 0, 2);

    public FluidNodeBlock(Properties properties) {
        // -1.0f resistance makes it indestructible in survival
        super(properties.strength(-1.0f, 3600000.0f));
        this.registerDefaultState(this.stateDefinition.any().setValue(PURITY, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PURITY);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!player.isCreative()) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("You need a Fluid Extractor to pump this node!").withStyle(ChatFormatting.RED), true);
            }
            return InteractionResult.SUCCESS;
        }

        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (level.getBlockEntity(pos) instanceof FluidNodeBlockEntity nodeBE) {
            if (!level.isClientSide) {

                // 1. Ask NeoForge if the held item contains any fluids (Works for buckets AND modded tanks!)
                var fluidHandler = stack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM);

                if (fluidHandler != null && fluidHandler.getTanks() > 0) {
                    net.neoforged.neoforge.fluids.FluidStack fluidStack = fluidHandler.getFluidInTank(0);

                    if (!fluidStack.isEmpty()) {
                        String fluidId = net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(fluidStack.getFluid()).toString();

                        if (!fluidId.equals("minecraft:empty")) {
                            int nextPurity = (state.getValue(PURITY) + 1) % 3;
                            nodeBE.setFluidId(fluidId);
                            nodeBE.setPurity(nextPurity);
                            level.setBlock(pos, state.setValue(PURITY, nextPurity), 3);

                            player.displayClientMessage(Component.literal("Configured Fluid: " + fluidId + " [" + nextPurity + "]").withStyle(ChatFormatting.AQUA), true);
                        }
                    }
                } else if (stack.isEmpty()) {
                    // 2. Empty hand just cycles purity
                    int nextPurity = (state.getValue(PURITY) + 1) % 3;
                    nodeBE.setPurity(nextPurity);
                    level.setBlock(pos, state.setValue(PURITY, nextPurity), 3);
                    player.displayClientMessage(Component.literal("Purity set to: " + nextPurity).withStyle(ChatFormatting.YELLOW), true);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        if (player.isCreative() && player.isShiftKeyDown()) {
            return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
        }
        return false;
    }

    @Override
    public net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return net.minecraft.world.level.block.RenderShape.MODEL;
    }

    @Override
    public ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader level, net.minecraft.core.BlockPos pos, BlockState state) {
        ItemStack stack = new ItemStack(this);
        if (level.getBlockEntity(pos) instanceof FluidNodeBlockEntity nodeBE) {
            String fluidId = nodeBE.getFluidId();
            int purityVal = nodeBE.getPurity();

            net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA, stack, tag -> {
                tag.putString("id", net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(Satisfactory_ore_nodes.FLUID_NODE_BE.get()).toString());
                tag.putString("fluidId", fluidId);
                tag.putInt("purity", purityVal);
            });
        }
        return stack;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidNodeBlockEntity(pos, state);
    }
}