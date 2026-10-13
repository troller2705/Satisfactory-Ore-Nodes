package com.troller2705.satisfactory_ore_nodes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Nameable;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class GeyserNodeBlockEntity extends BlockEntity implements Nameable {
    private int purity = 1;

    // NOTE: Make sure you register GEYSER_NODE_BE in your DeferredRegister!
    public GeyserNodeBlockEntity(BlockPos pos, BlockState state) {
        super(Satisfactory_ore_nodes.GEYSER_NODE_BE.get(), pos, state);
    }

    public void setPurity(int purity) {
        this.purity = purity;
        setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
    public int getPurity() { return purity; }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.purity = tag.getInt("purity");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("purity", this.purity);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getName() {
        String purityName = switch (this.purity) {
            case 0 -> "Impure";
            case 1 -> "Normal";
            case 2 -> "Pure";
            default -> "Unknown";
        };
        return Component.literal("Geyser (" + purityName + ")");
    }

    @Override
    public boolean hasCustomName() { return true; }

    @Override
    public Component getDisplayName() { return getName(); }

    @Nullable
    @Override
    public Component getCustomName() { return getName(); }
}