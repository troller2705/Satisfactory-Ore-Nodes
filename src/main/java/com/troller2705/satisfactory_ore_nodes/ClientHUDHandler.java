package com.troller2705.satisfactory_ore_nodes;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector4f;

@EventBusSubscriber(modid = Satisfactory_ore_nodes.MODID, value = Dist.CLIENT)
public class ClientHUDHandler {
    // These static matrices act as the 'bridge' from 3D to 2D
    private static Matrix4f syncedWorldMatrix = new Matrix4f();
    private static Vec3 syncedCameraPos = Vec3.ZERO;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        // Step 1: Capture the 3D matrices while the level is rendering
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            Minecraft mc = Minecraft.getInstance();
            syncedCameraPos = mc.gameRenderer.getMainCamera().getPosition();

            // Build the projection * view matrix exactly like FTB Chunks
            syncedWorldMatrix = new Matrix4f(RenderSystem.getProjectionMatrix());
            syncedWorldMatrix.mul(new Matrix4f().rotation(mc.gameRenderer.getMainCamera().rotation().conjugate(new org.joml.Quaternionf())));
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // Timer and Data checks
        long lastScan = mc.player.getPersistentData().getLong("last_scan_time");
        if (mc.level.getGameTime() - lastScan > (SatisfactoryFTBConfig.scanDuration * 20L)) return;

        String data = mc.player.getPersistentData().getString("scanned_nodes");
        if (data.isEmpty()) return;

        // Step 2: Use the SYNCED matrix to draw the waypoints
        if (SatisfactoryFTBConfig.showHudText) {
            renderWaypoints(event.getGuiGraphics(), mc, data);
        }

        if (SatisfactoryFTBConfig.showCompass) {
            renderCompass(event, mc, data);
        }
    }

    private static void renderWaypoints(GuiGraphics graphics, Minecraft mc, String data) {
        float halfW = graphics.guiWidth() / 2F;
        float halfH = graphics.guiHeight() / 2F;

        for (String entry : data.split(",")) {
            if (entry.trim().isEmpty()) continue;
            try {
                // FIX: Split the complex string to get the Long ID
                String[] parts = entry.split("\\|");
                BlockPos nodePos = BlockPos.of(Long.parseLong(parts[0]));

                // Relative world position
                Vector4f v = new Vector4f(
                        (float) (nodePos.getX() + 0.5 - syncedCameraPos.x),
                        (float) (nodePos.getY() + 1.8 - syncedCameraPos.y),
                        (float) (nodePos.getZ() + 0.5 - syncedCameraPos.z),
                        1F
                );

                // Matrix Transformation
                syncedWorldMatrix.transform(v);

                // Perspective check (prevents icons from appearing behind you)
                if (v.w() > 0.001F) {
                    v.div(v.w()); // Perspective division

                    // Mapping NDC (-1 to 1) to screen pixels
                    float ix = halfW + (v.x() * halfW);
                    float iy = halfH - (v.y() * halfH);

                    drawNodeInfo(graphics, mc, nodePos, (int)ix, (int)iy);
                }
            } catch (Exception e) {
                // Remove 'ignored' temporarily if you need to debug further!
            }
        }
    }

    private static void drawNodeInfo(GuiGraphics graphics, Minecraft mc, BlockPos pos, int x, int y) {
        // 1. Get the BlockEntity to find out what ore this actually is
        BlockEntity be = mc.level.getBlockEntity(pos);
        if (!(be instanceof ResourceNodeBlockEntity nodeBE)) return;

        String actualOreId = nodeBE.getOreId(); // e.g., "minecraft:iron_ore"
        BlockState state = mc.level.getBlockState(pos);

        // 0=Impure, 1=Normal, 2=Pure
        int purityIndex = state.hasProperty(ResourceNodeBlock.PURITY) ? state.getValue(ResourceNodeBlock.PURITY) : 1;

        for (SatisfactoryFTBConfig.NodeEntry node : SatisfactoryFTBConfig.scannableNodes) {
            // Compare the ID stored in the NBT, not the Master Node's block ID
            if (node.baseNodeId.equals(actualOreId)) {
                if (purityIndex < node.purities.size()) {
                    SatisfactoryFTBConfig.PurityEntry pEntry = node.purities.get(purityIndex);

                    // Apply your color logic
                    dev.ftb.mods.ftblibrary.icon.Color4I finalColor = node.baseColor;
                    if (purityIndex == 2) {
                        finalColor = finalColor.lerp(dev.ftb.mods.ftblibrary.icon.Color4I.WHITE, 0.3f);
                    } else if (purityIndex == 0) {
                        finalColor = finalColor.lerp(dev.ftb.mods.ftblibrary.icon.Color4I.BLACK, 0.3f);
                    }

                    // Get display name from the config-defined visual block
                    net.minecraft.resources.ResourceLocation visualLoc = net.minecraft.resources.ResourceLocation.parse(node.baseNodeId);
                    net.minecraft.world.level.block.Block visualBlock = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(visualLoc);
                    String displayName = visualBlock.getName().getString();

                    int dist = (int) Math.sqrt(pos.distToCenterSqr(mc.player.position()));
                    String text = node.hudSymbol + " " + displayName + " [" + pEntry.label + "] (" + dist + "m)";

                    graphics.pose().pushPose();
                    graphics.pose().translate(0, 0, 500);
                    graphics.drawCenteredString(mc.font, text, x, y, finalColor.rgb());
                    graphics.pose().popPose();
                }
                break;
            }
        }
    }

    private static void renderCompass(RenderGuiEvent.Post event, Minecraft mc, String data) {
        int centerX = event.getGuiGraphics().guiWidth() / 2;
        int y = 10;
        event.getGuiGraphics().fill(centerX - 100, y, centerX + 100, y + 1, 0xFFFFFFFF);

        for (String entry : data.split(",")) {
            if (entry.trim().isEmpty()) continue;

            try {
                // FIX: Split the entry by the pipe symbol
                String[] parts = entry.split("\\|");
                if (parts.length < 1) continue;

                // Parts[0] is the Long BlockPos
                BlockPos nodePos = BlockPos.of(Long.parseLong(parts[0]));

                double angleToNode = Math.atan2(nodePos.getZ() - mc.player.getZ(), nodePos.getX() - mc.player.getX());
                double playerAngle = Math.toRadians(mc.player.getYRot() + 90);
                double relativeAngle = angleToNode - playerAngle;

                while (relativeAngle < -Math.PI) relativeAngle += Math.PI * 2;
                while (relativeAngle > Math.PI) relativeAngle -= Math.PI * 2;

                if (Math.abs(relativeAngle) < Math.PI / 2) {
                    int xOffset = (int) (relativeAngle * 60);
                    String icon = "◆"; // Default icon

                    // Use the Ore Name we already saved in the string (Parts[1])
                    if (parts.length > 1) {
                        String oreName = parts[1];
                        for (SatisfactoryFTBConfig.NodeEntry node : SatisfactoryFTBConfig.scannableNodes) {
                            // Check if the oreName matches a node in our config to get the HUD symbol
                            if (node.baseNodeId.contains(oreName.toLowerCase().replace(" ", "_"))) {
                                icon = node.hudSymbol;
                                break;
                            }
                        }
                    }

                    event.getGuiGraphics().drawCenteredString(mc.font, icon, centerX + xOffset, y + 5, 0xFFFFFF);
                }
            } catch (Exception e) {
                // Log and skip if a single entry is malformed to prevent a full crash
                continue;
            }
        }
    }
}