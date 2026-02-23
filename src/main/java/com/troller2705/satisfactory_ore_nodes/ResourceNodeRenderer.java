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
        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof ResourceNodeBlock)) return;

        // 1. Get the synced properties
        int index = state.getValue(ResourceNodeBlock.ORE_INDEX);
        int purity = state.getValue(ResourceNodeBlock.PURITY);

        // 2. Safety check for config bounds
        if (index >= SatisfactoryFTBConfig.scannableNodes.size()) return;

        // 3. Lookup the visual block from your config
        String visualId = SatisfactoryFTBConfig.scannableNodes.get(index).purities.get(purity).visualBlockId;
        BlockState visualState = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(visualId)).defaultBlockState();

        // 4. CRITICAL FIX: Actually draw the block on the screen
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                visualState,
                poseStack,
                buffer,
                combinedLight,
                combinedOverlay,
                net.neoforged.neoforge.client.model.data.ModelData.EMPTY,
                null
        );
    }
}