package com.troller2705.satisfactory_ore_nodes;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

@EventBusSubscriber(modid = Satisfactory_ore_nodes.MODID, value = Dist.CLIENT)
public class ScannerHoloRenderer {

    @SubscribeEvent
    public static void renderScannerHolograms(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        CompoundTag data = player.getPersistentData();
        if (!data.contains("scanned_nodes")) return;

        long lastScanTime = data.getLong("last_scan_time");
        long elapsedTicks = player.level().getGameTime() - lastScanTime;

        int durationTicks = SatisfactoryFTBConfig.scanDuration * 20;

        if (elapsedTicks > durationTicks) return;

        float alpha = 1.0f;
        if (elapsedTicks > durationTicks - 20) {
            alpha = 1.0f - ((elapsedTicks - (durationTicks - 20)) / 20.0f);
        } else if (elapsedTicks < 10) {
            alpha = elapsedTicks / 10.0f;
        }
        if (alpha <= 0) return;

        String nodesRaw = data.getString("scanned_nodes");
        if (nodesRaw.isEmpty()) return;

        PoseStack poseStack = event.getPoseStack();
        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        String[] nodes = nodesRaw.split(",");

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);
        Matrix4f matrix = poseStack.last().pose();

        int r = 250, g = 149, b = 73;
        int beamAlpha = (int) (180 * alpha);

        for (String nodeStr : nodes) {
            if (nodeStr.isEmpty()) continue;
            String[] parts = nodeStr.split("\\|");
            if (parts.length < 3) continue;

            long posLong = Long.parseLong(parts[0]);
            BlockPos pos = BlockPos.of(posLong);
            String oreName = parts[1];
            int purity = Integer.parseInt(parts[2]);

            String purityStr = switch (purity) {
                case 0 -> "Impure";
                case 1 -> "Normal";
                case 2 -> "Pure";
                default -> "Unknown";
            };

            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.5;
            double z = pos.getZ() + 0.5;

            double dist = Math.sqrt(player.distanceToSqr(x, y, z));
            String distStr = String.format("%.0fm", dist);

            BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

            float w = 0.1f;
            float h = 65.0f;

            // North
            buffer.addVertex(matrix, (float)(x - w), (float)y, (float)(z - w)).setColor(r, g, b, beamAlpha);
            buffer.addVertex(matrix, (float)(x + w), (float)y, (float)(z - w)).setColor(r, g, b, beamAlpha);
            buffer.addVertex(matrix, (float)(x + w), (float)(y + h), (float)(z - w)).setColor(r, g, b, 0);
            buffer.addVertex(matrix, (float)(x - w), (float)(y + h), (float)(z - w)).setColor(r, g, b, 0);
            // South
            buffer.addVertex(matrix, (float)(x - w), (float)y, (float)(z + w)).setColor(r, g, b, beamAlpha);
            buffer.addVertex(matrix, (float)(x + w), (float)y, (float)(z + w)).setColor(r, g, b, beamAlpha);
            buffer.addVertex(matrix, (float)(x + w), (float)(y + h), (float)(z + w)).setColor(r, g, b, 0);
            buffer.addVertex(matrix, (float)(x - w), (float)(y + h), (float)(z + w)).setColor(r, g, b, 0);
            // East
            buffer.addVertex(matrix, (float)(x + w), (float)y, (float)(z - w)).setColor(r, g, b, beamAlpha);
            buffer.addVertex(matrix, (float)(x + w), (float)y, (float)(z + w)).setColor(r, g, b, beamAlpha);
            buffer.addVertex(matrix, (float)(x + w), (float)(y + h), (float)(z + w)).setColor(r, g, b, 0);
            buffer.addVertex(matrix, (float)(x + w), (float)(y + h), (float)(z - w)).setColor(r, g, b, 0);
            // West
            buffer.addVertex(matrix, (float)(x - w), (float)y, (float)(z - w)).setColor(r, g, b, beamAlpha);
            buffer.addVertex(matrix, (float)(x - w), (float)y, (float)(z + w)).setColor(r, g, b, beamAlpha);
            buffer.addVertex(matrix, (float)(x - w), (float)(y + h), (float)(z + w)).setColor(r, g, b, 0);
            buffer.addVertex(matrix, (float)(x - w), (float)(y + h), (float)(z - w)).setColor(r, g, b, 0);

            BufferUploader.drawWithShader(buffer.buildOrThrow());

            poseStack.pushPose();

            double bobbing = Math.sin((player.level().getGameTime() + event.getPartialTick().getGameTimeDeltaTicks()) * 0.1) * 0.15;
            poseStack.translate(x, y + 2.8 + bobbing, z);

            poseStack.mulPose(camera.rotation());

            // FIX: X scale is now positive (0.04f instead of -0.04f) to prevent mirroring!
            poseStack.scale(0.04f, -0.04f, 0.04f);

            Matrix4f textMatrix = poseStack.last().pose();

            Component titleText = Component.literal(oreName).withStyle(ChatFormatting.BOLD);
            Component detailText = Component.literal(purityStr + " • " + distStr);

            float titleWidth = mc.font.width(titleText);
            float detailWidth = mc.font.width(detailText);

            int textAlpha = (int) (255 * alpha);
            int bgAlpha = (int) (130 * alpha);
            int colorPacked = (textAlpha << 24) | 0xFFFFFF;
            int bgPacked = (bgAlpha << 24) | 0x000000;

            mc.font.drawInBatch(titleText, -titleWidth / 2f, -10, colorPacked, false, textMatrix, buffers, net.minecraft.client.gui.Font.DisplayMode.SEE_THROUGH, bgPacked, 255);
            mc.font.drawInBatch(detailText, -detailWidth / 2f, 2, colorPacked, false, textMatrix, buffers, net.minecraft.client.gui.Font.DisplayMode.SEE_THROUGH, bgPacked, 255);

            poseStack.popPose();
        }

        buffers.endBatch();
        poseStack.popPose();

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}