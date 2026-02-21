package com.troller2705.satisfactory_ore_nodes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ResourceNodeBlockEntity extends BlockEntity
{
    private int purity = 1; // Default to Normal

    public ResourceNodeBlockEntity(BlockPos pos, BlockState state) {
        super(Satisfactory_ore_nodes.NODE_BE.get(), pos, state);
    }

    public void setPurity(int purity) { this.purity = purity; setChanged(); }
    public int getPurity() { return purity; }

    // Save/Load purity to NBT so it survives world restarts
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("purity", purity);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.purity = tag.getInt("purity");
    }
}
