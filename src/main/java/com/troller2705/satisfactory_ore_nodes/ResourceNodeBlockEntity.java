package com.troller2705.satisfactory_ore_nodes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ResourceNodeBlockEntity extends BlockEntity
{
    private String oreId = "minecraft:air"; // What ore this node represents
    private int purity = 1; // Default to Normal

    public ResourceNodeBlockEntity(BlockPos pos, BlockState state) {
        super(Satisfactory_ore_nodes.NODE_BE.get(), pos, state);
    }

    public void setOreId(String oreId) {
        this.oreId = oreId;
        this.setChanged();
        if (this.level != null) {
            // This forces the client to re-run the Renderer
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }
    public String getOreId() { return oreId; }

    public void setPurity(int purity) { this.purity = purity; setChanged(); }
    public int getPurity() { return purity; }

    // Save/Load purity to NBT so it survives world restarts
    // Inside ResourceNodeBlockEntity.java
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.oreId = tag.getString("oreId"); // Use lowercase 'd'
        this.purity = tag.getInt("purity");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("oreId", this.oreId);
        tag.putInt("purity", this.purity);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries); // Crucial: Pack your oreId for the client
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        // Tells the client to update its NBT when the block is placed/changed
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            this.loadAdditional(tag, registries);
            // Force a rerender now that we have the oreId
            if (level != null && level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }
}
