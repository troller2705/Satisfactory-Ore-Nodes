package com.troller2705.satisfactory_ore_nodes;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = Satisfactory_ore_nodes.MODID, value = Dist.CLIENT)
public class ScannerCompassOverlay {

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!SatisfactoryFTBConfig.showCompass) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        CompoundTag data = mc.player.getPersistentData();
        if (!data.contains("scanned_nodes")) return;

        long lastScan = data.getLong("last_scan_time");
        long elapsedTicks = mc.level.getGameTime() - lastScan;
        int durationTicks = SatisfactoryFTBConfig.scanDuration * 20;

        if (elapsedTicks > durationTicks) return;

        // Synchronize fade-in/out alpha with the 3D holograms
        float alpha = 1.0f;
        if (elapsedTicks > durationTicks - 20) {
            alpha = 1.0f - ((elapsedTicks - (durationTicks - 20)) / 20.0f);
        } else if (elapsedTicks < 10) {
            alpha = elapsedTicks / 10.0f;
        }
        if (alpha <= 0) return;

        String nodesRaw = data.getString("scanned_nodes");
        if (nodesRaw.isEmpty()) return;

        int centerX = event.getGuiGraphics().guiWidth() / 2;
        int y = 15; // Spaced slightly below the top of the screen

        int lineAlpha = (int) (136 * alpha); // 0x88 in hex
        int tickAlpha = (int) (170 * alpha); // 0xAA in hex
        int textAlpha = (int) (255 * alpha);

        // Draw the horizontal compass tick line
        event.getGuiGraphics().fill(centerX - 120, y, centerX + 120, y + 1, (lineAlpha << 24) | 0xFFFFFF);
        // Center forward-facing tick
        event.getGuiGraphics().fill(centerX - 1, y - 2, centerX + 1, y + 3, (tickAlpha << 24) | 0xFFFFFF);

        for (String entry : nodesRaw.split(",")) {
            if (entry.trim().isEmpty()) continue;

            try {
                String[] parts = entry.split("\\|");
                if (parts.length < 2) continue;

                BlockPos nodePos = BlockPos.of(Long.parseLong(parts[0]));
                String oreName = parts[1];

                double angleToNode = Math.atan2(nodePos.getZ() - mc.player.getZ(), nodePos.getX() - mc.player.getX());
                double playerAngle = Math.toRadians(mc.player.getYRot() + 90);
                double relativeAngle = angleToNode - playerAngle;

                // Normalize angle
                while (relativeAngle < -Math.PI) relativeAngle += Math.PI * 2;
                while (relativeAngle > Math.PI) relativeAngle -= Math.PI * 2;

                // If within 90 degrees (Field of View)
                if (Math.abs(relativeAngle) < Math.PI / 2) {

                    // Math.PI / 2 is ~1.57. Multiplied by 76 maps perfectly to our 120px wide compass line
                    int xOffset = (int) (relativeAngle * 76);

                    String icon = "◆";
                    int color = 0xFFFFFF; // Default white fallback

                    // Match the stored Ore/Fluid name against the config to grab the HUD Symbol and Color!
                    String searchKey = oreName.toLowerCase().replace(" ", "_");
                    for (SatisfactoryFTBConfig.NodeEntry node : SatisfactoryFTBConfig.scannableNodes) {
                        if (node.baseNodeId.contains(searchKey)) {
                            icon = node.hudSymbol;
                            color = node.baseColor.rgb();
                            break;
                        }
                    }

                    // Pack the alpha value into the config's RGB color
                    int finalColor = (textAlpha << 24) | (color & 0x00FFFFFF);

                    // Draw with a drop shadow to ensure it's readable against the bright sky
                    event.getGuiGraphics().drawCenteredString(mc.font, icon, centerX + xOffset, y + 4, finalColor);
                }
            } catch (Exception e) {
                // Skips a malformed entry without breaking the rest of the compass
                continue;
            }
        }
    }
}