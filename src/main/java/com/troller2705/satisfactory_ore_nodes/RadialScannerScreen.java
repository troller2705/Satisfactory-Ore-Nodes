package com.troller2705.satisfactory_ore_nodes;

import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;

public class RadialScannerScreen extends Screen {

    // UI Textures
    private static final ResourceLocation BG_TEXTURE = ResourceLocation.fromNamespaceAndPath("satisfactory_ore_nodes", "textures/gui/bg.png");
    private static final ResourceLocation HIGHLIGHT_TEXTURE = ResourceLocation.fromNamespaceAndPath("satisfactory_ore_nodes", "textures/gui/highlight.png");
    private static final ResourceLocation ARROW_TEXTURE = ResourceLocation.fromNamespaceAndPath("satisfactory_ore_nodes", "textures/gui/arrow.png");

    private final List<SatisfactoryFTBConfig.NodeEntry> availableNodes;
    private int selectedIndex = -1;

    // Set this to the actual pixel dimensions of your UI images
    private static final int UI_SIZE = 256;

    public RadialScannerScreen(List<SatisfactoryFTBConfig.NodeEntry> availableNodes) {
        super(Component.literal("Scanner Menu"));
        this.availableNodes = availableNodes;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (availableNodes.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, "No Ores Unlocked!", this.width / 2, this.height / 2, 0xFF5555);
            return;
        }

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Calculate Mouse Angle & Selection
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double distanceSq = dx * dx + dy * dy;
        double sliceAngle = 360.0 / availableNodes.size();

        if (distanceSq > 400) {
            double mouseAngle = (Math.toDegrees(Math.atan2(dy, dx)) + 360 + 90) % 360;
            selectedIndex = (int) Math.round(mouseAngle / sliceAngle) % availableNodes.size();
        } else {
            selectedIndex = -1;
        }

        // ==========================================
        // 1. ENABLE ALPHA BLENDING FOR TRANSLUCENCY
        // ==========================================
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // ==========================================
        // 2. DRAW STATIC BACKGROUND
        // ==========================================
        guiGraphics.blit(BG_TEXTURE, centerX - (UI_SIZE / 2), centerY - (UI_SIZE / 2), 0, 0, UI_SIZE, UI_SIZE, UI_SIZE, UI_SIZE);

        // ==========================================
        // 3. DRAW ROTATING HIGHLIGHT & ARROW
        // ==========================================
        if (selectedIndex != -1) {
            float rotationDegrees = (float) (selectedIndex * sliceAngle);

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(centerX, centerY, 0);
            guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(rotationDegrees));

            guiGraphics.blit(HIGHLIGHT_TEXTURE, -(UI_SIZE / 2), -(UI_SIZE / 2), 0, 0, UI_SIZE, UI_SIZE, UI_SIZE, UI_SIZE);
            guiGraphics.blit(ARROW_TEXTURE, -(UI_SIZE / 2), -(UI_SIZE / 2), 0, 0, UI_SIZE, UI_SIZE, UI_SIZE, UI_SIZE);

            guiGraphics.pose().popPose();
        }

        // ==========================================
        // 4. DISABLE BLENDING
        // ==========================================
        RenderSystem.disableBlend();

        // ==========================================
        // 5. DRAW ICONS & CENTER TEXT
        // ==========================================
        double iconRadius = 95.0;

        for (int i = 0; i < availableNodes.size(); i++) {
            double angle = Math.toRadians((i * sliceAngle) - 90);
            int x = centerX + (int) (Math.cos(angle) * iconRadius) - 8;
            int y = centerY + (int) (Math.sin(angle) * iconRadius) - 8;

            SatisfactoryFTBConfig.NodeEntry node = availableNodes.get(i);
            ItemStack iconStack = new ItemStack(BuiltInRegistries.BLOCK.get(ResourceLocation.parse(node.baseNodeId)));

            guiGraphics.renderItem(iconStack, x, y);

            if (i == selectedIndex) {
                guiGraphics.drawCenteredString(this.font, BuiltInRegistries.BLOCK.get(ResourceLocation.parse(node.baseNodeId)).getName().getString(), centerX, centerY - 4, 0xFFFFFF);
            }
        }
    }

    // ==========================================
    // SELECTION LOGIC
    // ==========================================
    private void executeSelection() {
        if (selectedIndex != -1) {
            String selectedOreId = availableNodes.get(selectedIndex).baseNodeId;
            PacketDistributor.sendToServer(new ScannerSelectPacket(selectedOreId));
            NodeScannerItem.performClientScan(Minecraft.getInstance().player, selectedOreId);
        }
        this.onClose();
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (Minecraft.getInstance().options.keyUse.matchesMouse(button)) {
            executeSelection();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (Minecraft.getInstance().options.keyUse.matches(keyCode, scanCode)) {
            executeSelection();
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}