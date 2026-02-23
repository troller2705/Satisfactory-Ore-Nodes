package com.troller2705.satisfactory_ore_nodes;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

@EventBusSubscriber(modid = Satisfactory_ore_nodes.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModelEvents {

    @SubscribeEvent
    public static void onModelRegister(ModelEvent.RegisterAdditional event) {
        // Wrap the path in an inventory variant so the registry accepts it
        ResourceLocation masterBase = ResourceLocation.fromNamespaceAndPath(Satisfactory_ore_nodes.MODID, "item/resource_node");
        event.register(ModelResourceLocation.inventory(masterBase));
    }

    @SubscribeEvent
    public static void onModelBaking(ModelEvent.ModifyBakingResult event) {
        // 1. Look up the baked version using the exact same inventory location
        ResourceLocation masterBase = ResourceLocation.fromNamespaceAndPath(Satisfactory_ore_nodes.MODID, "item/resource_node");
        ModelResourceLocation masterInventoryLoc = ModelResourceLocation.inventory(masterBase);

        var masterModel = event.getModels().get(masterInventoryLoc);

        if (masterModel != null) {
            // 2. Map every dynamic item name to this baked model
            for (String registryName : Satisfactory_ore_nodes.NODE_ITEMS.keySet()) {
                ResourceLocation dynamicBase = ResourceLocation.fromNamespaceAndPath(Satisfactory_ore_nodes.MODID, "item/" + registryName);
                ModelResourceLocation dynamicInventoryLoc = ModelResourceLocation.inventory(dynamicBase);

                event.getModels().put(dynamicInventoryLoc, masterModel);
            }
        }
    }
}