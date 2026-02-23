package com.troller2705.satisfactory_ore_nodes;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.state.BlockState;

// THIS IS THE RENDERER
public class ResourceNodeItemRenderer extends BlockEntityWithoutLevelRenderer {

    public ResourceNodeItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        // Get the name: e.g., "pure_minecraft_iron_ore_node"
        String name = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();

        // Parse the name to find the right ore and purity
        int purity = name.contains("pure_") ? 2 : name.contains("impure_") ? 0 : 1;

        // Find the ore index by matching the name against your config
        int index = 0;
        for (int i = 0; i < SatisfactoryFTBConfig.scannableNodes.size(); i++) {
            String baseId = SatisfactoryFTBConfig.scannableNodes.get(i).baseNodeId.replace(":", "_");
            if (name.contains(baseId)) {
                index = i;
                break;
            }
        }

        // Standard rendering logic
        String visualId = SatisfactoryFTBConfig.scannableNodes.get(index).purities.get(purity).visualBlockId;
        BlockState stateToRender = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(visualId)).defaultBlockState();

        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                stateToRender, poseStack, buffer, combinedLight, combinedOverlay,
                net.neoforged.neoforge.client.model.data.ModelData.EMPTY, null
        );
    }
}