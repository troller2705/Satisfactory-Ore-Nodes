package com.troller2705.satisfactory_ore_nodes;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.BuiltInRegistries;

public class ResourceNodeRenderer implements BlockEntityRenderer<ResourceNodeBlockEntity> {

    public ResourceNodeRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(ResourceNodeBlockEntity be, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        String oreId = be.getOreId();
        int purity = be.getPurity();
        String visualBlockId = "minecraft:stone"; // Fallback

        // Find visual from Config
        for (SatisfactoryFTBConfig.NodeEntry node : SatisfactoryFTBConfig.scannableNodes) {
            if (node.baseNodeId.equals(oreId)) {
                if (purity < node.purities.size()) {
                    visualBlockId = node.purities.get(purity).visualBlockId;
                }
                break;
            }
        }

        // Inside the render method
        BlockState visualState = Blocks.MAGENTA_GLAZED_TERRACOTTA.defaultBlockState(); // ERROR TEXTURE

        if (be.getOreId() != null && !be.getOreId().isEmpty() && !be.getOreId().equals("minecraft:air")) {
            String visualId = be.getOreId(); // Fallback to itself

            for (SatisfactoryFTBConfig.NodeEntry node : SatisfactoryFTBConfig.scannableNodes) {
                if (node.baseNodeId.equals(be.getOreId())) {
                    visualId = node.purities.get(be.getPurity()).visualBlockId;
                    break;
                }
            }
            visualState = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(visualId)).defaultBlockState();
        }
    }
}