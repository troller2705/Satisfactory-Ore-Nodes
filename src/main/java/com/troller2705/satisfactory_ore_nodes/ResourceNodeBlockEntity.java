package com.troller2705.satisfactory_ore_nodes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Nameable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class ResourceNodeBlockEntity extends BlockEntity implements Nameable
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
            // Use flag 3 (1 | 2) to ensure the block is updated and the change is sent to the client
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
    public String getOreId() { return oreId; }

    public void setPurity(int purity) { this.purity = purity; setChanged(); }
    public int getPurity() { return purity; }

    // Save/Load purity to NBT so it survives world restarts
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.oreId = tag.getString("oreId");
        this.purity = tag.getInt("purity");
//        System.out.println("LOADED ORE ID: " + this.oreId);

        // CRITICAL: If we are on the server, tell the client about this new data
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
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
        saveAdditional(tag, registries); // Ensures oreId and purity are in the packet
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
            // Force load the ID immediately
            this.oreId = tag.getString("oreId");
            this.purity = tag.getInt("purity");

            // This makes sure F3 sees the new data instantly on the client side
            if (this.level != null && this.level.isClientSide) {
                this.level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }

    // ==========================================
    // NAMEABLE IMPLEMENTATION (FOR JADE/WTHIT/TOP)
    // ==========================================

    @Override
    public Component getName() {
        if (this.oreId == null || this.oreId.isEmpty() || this.oreId.equals("minecraft:air")) {
            return Component.literal("Resource Node");
        }

        Block baseBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(this.oreId));
        String oreName = baseBlock.getName().getString().replaceFirst("(?i)\\s*ore$", "");

        String purityName = switch (this.purity) {
            case 0 -> "Impure";
            case 1 -> "Normal";
            case 2 -> "Pure";
            default -> "Unknown";
        };

        return Component.literal(oreName + " Node (" + purityName + ")");
    }

    @Override
    public boolean hasCustomName() {
        // Tells Jade that it MUST use our custom name instead of the block registry name
        return true;
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Nullable
    @Override
    public Component getCustomName() {
        return getName();
    }
}