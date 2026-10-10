package com.troller2705.satisfactory_ore_nodes;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Arrays;
import java.util.List;

public record OpenScannerMenuPacket(String unlockedOresStr) implements CustomPacketPayload {

    public static final Type<OpenScannerMenuPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("satisfactory_ore_nodes", "open_scanner_menu"));

    public static final StreamCodec<ByteBuf, OpenScannerMenuPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, OpenScannerMenuPacket::unlockedOresStr,
            OpenScannerMenuPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenScannerMenuPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Ensure we only try to open the screen on the physical client
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientMenuHandler.openMenu(payload.unlockedOresStr());
            }
        });
    }

    // Inner class prevents the server from crashing by safely isolating Client-only code
    private static class ClientMenuHandler {
        public static void openMenu(String unlockedStr) {
            List<SatisfactoryFTBConfig.NodeEntry> available;

            if (unlockedStr.equals("none")) {
                available = List.of(); // Empty list, will show "No Ores Unlocked"
            } else {
                List<String> unlocked = Arrays.asList(unlockedStr.split(","));
                available = SatisfactoryFTBConfig.scannableNodes.stream()
                        .filter(node -> unlocked.contains(node.baseNodeId))
                        .toList();
            }

            Minecraft.getInstance().setScreen(new RadialScannerScreen(available));
        }
    }
}