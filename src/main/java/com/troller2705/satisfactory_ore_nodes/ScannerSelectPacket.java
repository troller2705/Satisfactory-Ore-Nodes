package com.troller2705.satisfactory_ore_nodes;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ScannerSelectPacket(String oreId) implements CustomPacketPayload {

    public static final Type<ScannerSelectPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("satisfactory_ore_nodes", "scanner_select"));

    public static final StreamCodec<ByteBuf, ScannerSelectPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ScannerSelectPacket::oreId,
            ScannerSelectPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ScannerSelectPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            ItemStack stack = player.getMainHandItem();

            if (stack.getItem() instanceof NodeScannerItem) {
                // Save selection to the item
                CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                tag.putString("target_ore_id", payload.oreId());
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

                // Broadcast the boom sound to the world
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.5f, 1.5f);

                // Apply the cooldown so they can't spam it
                player.getCooldowns().addCooldown(stack.getItem(), 40);
            }
        });
    }
}