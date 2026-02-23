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

        int index = state.getValue(ResourceNodeBlock.ORE_INDEX);
        int purity = state.getValue(ResourceNodeBlock.PURITY);

        if (index >= SatisfactoryFTBConfig.scannableNodes.size()) return;

        String visualId = SatisfactoryFTBConfig.scannableNodes.get(index).purities.get(purity).visualBlockId;
        BlockState visualState = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(visualId)).defaultBlockState();

        // FIXED RENDERING CALL
        poseStack.pushPose();

        // Ensure we are using the correct buffer for block rendering
        net.minecraft.client.renderer.RenderType rt = net.minecraft.client.renderer.ItemBlockRenderTypes.getRenderLayer(visualState.getFluidState());
        com.mojang.blaze3d.vertex.VertexConsumer vc = buffer.getBuffer(rt);

        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                visualState,
                poseStack,
                buffer,
                combinedLight,
                combinedOverlay,
                net.neoforged.neoforge.client.model.data.ModelData.EMPTY,
                null
        );

        poseStack.popPose();
    }
}