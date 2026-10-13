package com.troller2705.satisfactory_ore_nodes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Nameable;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class FluidNodeBlockEntity extends BlockEntity implements Nameable {
    private String fluidId = "minecraft:water";
    private int purity = 1;

    // NOTE: Make sure you register FLUID_NODE_BE in your DeferredRegister!
    public FluidNodeBlockEntity(BlockPos pos, BlockState state) {
        super(Satisfactory_ore_nodes.FLUID_NODE_BE.get(), pos, state);
    }

    public void setFluidId(String fluidId) {
        this.fluidId = fluidId;
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
    public String getFluidId() { return fluidId; }

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
        this.fluidId = tag.getString("fluidId");
        this.purity = tag.getInt("purity");
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("fluidId", this.fluidId);
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
        if (this.fluidId == null || this.fluidId.isEmpty() || this.fluidId.equals("minecraft:empty")) {
            return Component.literal("Fluid Node");
        }

        // Convert "minecraft:water" to "Water"
        net.minecraft.world.level.material.Fluid baseFluid = BuiltInRegistries.FLUID.get(ResourceLocation.parse(this.fluidId));
        String fluidName = BuiltInRegistries.FLUID.getKey(baseFluid).getPath();
        fluidName = fluidName.substring(0, 1).toUpperCase() + fluidName.substring(1).replace("_", " ");

        String purityName = switch (this.purity) {
            case 0 -> "Impure";
            case 1 -> "Normal";
            case 2 -> "Pure";
            default -> "Unknown";
        };

        return Component.literal(fluidName + " Node (" + purityName + ")");
    }

    @Override
    public boolean hasCustomName() { return true; }

    @Override
    public Component getDisplayName() { return getName(); }

    @Nullable
    @Override
    public Component getCustomName() { return getName(); }
}