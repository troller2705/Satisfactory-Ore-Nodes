package com.troller2705.satisfactory_ore_nodes;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import org.joml.Matrix4f;

public class FluidNodeRenderer implements BlockEntityRenderer<FluidNodeBlockEntity> {

    public FluidNodeRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(FluidNodeBlockEntity be, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        String fluidId = be.getFluidId();
        if (fluidId == null || fluidId.isEmpty() || fluidId.equals("minecraft:empty")) return;

        Fluid fluid = BuiltInRegistries.FLUID.get(ResourceLocation.parse(fluidId));
        if (fluid == Fluids.EMPTY) return;

        // 1. Ask NeoForge for the Fluid's texture and color data
        IClientFluidTypeExtensions clientFluid = IClientFluidTypeExtensions.of(fluid);
        ResourceLocation texture = clientFluid.getStillTexture();
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);

        // Passing the level and block position ensures Water gets the correct Biome color!
        int color = clientFluid.getTintColor(fluid.defaultFluidState(), be.getLevel(), be.getBlockPos());

        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (color >> 24) & 0xFF;
        if (a == 0) a = 255; // Safety fallback for modded fluids that forget to set alpha

        // Use the translucent buffer so things like Water render see-through
        VertexConsumer vc = buffer.getBuffer(RenderType.translucent());
        poseStack.pushPose();

        Matrix4f pose = poseStack.last().pose();

        // Fluid Puddle Dimensions (Inlet slightly from the edges to prevent Z-fighting)
        float min = 0.01f;
        float max = 0.99f;
        float minY = 0.01f;
        float maxY = 0.25f; // Set this to 0.99f if you want a full cube instead of a puddle!

        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();

        // Top Face (Y = max)
        vc.addVertex(pose, min, maxY, max).setColor(r, g, b, a).setUv(u0, v1).setOverlay(combinedOverlay).setLight(combinedLight).setNormal(poseStack.last(), 0, 1, 0);
        vc.addVertex(pose, max, maxY, max).setColor(r, g, b, a).setUv(u1, v1).setOverlay(combinedOverlay).setLight(combinedLight).setNormal(poseStack.last(), 0, 1, 0);
        vc.addVertex(pose, max, maxY, min).setColor(r, g, b, a).setUv(u1, v0).setOverlay(combinedOverlay).setLight(combinedLight).setNormal(poseStack.last(), 0, 1, 0);
        vc.addVertex(pose, min, maxY, min).setColor(r, g, b, a).setUv(u0, v0).setOverlay(combinedOverlay).setLight(combinedLight).setNormal(poseStack.last(), 0, 1, 0);

        // North Face (Z = min)
        vc.addVertex(pose, max, maxY, min).setColor(r, g, b, a).setUv(u0, v1).setOverlay(combinedOverlay).setLight(combinedLight).setNormal(poseStack.last(), 0, 0, -1);
        vc.addVertex(pose, min, maxY, min).setColor(r, g, b, a).setUv(u1, v1).setOverlay(combinedOverlay).setLight(combinedLight).setNormal(poseStack.last(), 0, 0, -1);
        vc.addVertex(pose, min, minY, min).setColor(r, g, b, a).setUv(u1, v0).setOverlay(combinedOverlay).setLight(combinedLight).setNormal(poseStack.last(), 0, 0, -1);
        vc.addVertex(pose, max, minY, min).setColor(r, g, b, a).setUv(u0, v0).setOverlay(combinedOverlay).setLight(combinedLight).setNormal(poseStack.last(), 0, 0, -1);

        // South Face (Z = max)
        vc.addVertex(pose, min, maxY, max).setColor(r, g, b, a).setUv(u0, v1).setOverlay(combinedOverlay).setLight(combinedLight).setNormal(poseStack.last(), 0, 0, 1);
        vc.addVertex(pose, max, maxY, max).setColor(r, g, b, a).setUv(u1, v1).setOverlay(combinedOverlay).setLight(combinedLight).setNormal(poseStack.last(), 0, 0, 1);
        vc.addVertex(pose, max, minY, max).setColor(r, g, b, a).setUv(u1, v0).setOverlay(combinedOverlay).setLight(combinedLight).setNormal(poseStack.last(), 0, 0, 1);
        vc.addVertex(pose, min, minY, max).setColor(r, g, b, a).setUv(u0, v0).setOverlay(combinedOverlay).setLight(combinedLight).setNormal(poseStack.last(), 0, 0, 1);

        // West Face (X = min)
        vc.addVertex(pose, min, maxY, min).setColor(r, g, b, a).setUv(u0, v1).setOverlay(combinedOverlay).setLight(combinedLight).setNormal(poseStack.last(), -1, 0, 0);
        vc.addVertex(pose, min, maxY, max).setColor(r, g, b, a).setUv(u1, v1).setOverlay(combinedOverlay).setLight(combinedLight).setNormal(poseStack.last(), -1, 0, 0);
        vc.addVertex(pose, min, minY, max).setColor(r, g, b, a).setUv(u1, v0).setOverlay(combinedOverlay).setLight(combinedLight).setNormal(poseStack.last(), -1, 0, 0);
        vc.addVertex(pose, min, minY, min).setColor(r, g, b, a).setUv(u0, v0).setOverlay(combinedOverlay).setLight(combinedLight).setNormal(poseStack.last(), -1, 0, 0);

        // East Face (X = max)
        vc.addVertex(pose, max, maxY, max).setColor(r, g, b, a).setUv(u0, v1).setOverlay(combinedOverlay).setLight(combinedLight).setNormal(poseStack.last(), 1, 0, 0);
        vc.addVertex(pose, max, maxY, min).setColor(r, g, b, a).setUv(u1, v1).setOverlay(combinedOverlay).setLight(combinedLight).setNormal(poseStack.last(), 1, 0, 0);
        vc.addVertex(pose, max, minY, min).setColor(r, g, b, a).setUv(u1, v0).setOverlay(combinedOverlay).setLight(combinedLight).setNormal(poseStack.last(), 1, 0, 0);
        vc.addVertex(pose, max, minY, max).setColor(r, g, b, a).setUv(u0, v0).setOverlay(combinedOverlay).setLight(combinedLight).setNormal(poseStack.last(), 1, 0, 0);

        poseStack.popPose();
    }
}