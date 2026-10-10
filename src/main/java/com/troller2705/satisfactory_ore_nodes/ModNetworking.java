package com.troller2705.satisfactory_ore_nodes;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

// This annotation tells NeoForge to automatically register these packets when the game boots!
@EventBusSubscriber(modid = "satisfactory_ore_nodes")
public class ModNetworking {

    @SubscribeEvent
    public static void registerPackets(final RegisterPayloadHandlersEvent event) {
        // Creates a registrar for your mod
        PayloadRegistrar registrar = event.registrar("1.0");

        // Registers the Client -> Server packet (Selecting an ore)
        registrar.playToServer(ScannerSelectPacket.TYPE, ScannerSelectPacket.STREAM_CODEC, ScannerSelectPacket::handle);

        // Registers the Server -> Client packet (Opening the menu)
        registrar.playToClient(OpenScannerMenuPacket.TYPE, OpenScannerMenuPacket.STREAM_CODEC, OpenScannerMenuPacket::handle);

        System.out.println("[SatisfactoryNodes] Network Packets Registered Successfully!");
    }
}