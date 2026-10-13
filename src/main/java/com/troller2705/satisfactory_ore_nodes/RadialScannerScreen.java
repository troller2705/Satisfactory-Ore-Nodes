package com.troller2705.satisfactory_ore_nodes;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class RadialScannerScreen extends Screen {

    private static final ResourceLocation BG_TEXTURE = ResourceLocation.fromNamespaceAndPath("satisfactory_ore_nodes", "textures/gui/bg.png");
    private static final ResourceLocation ARROW_TEXTURE = ResourceLocation.fromNamespaceAndPath("satisfactory_ore_nodes", "textures/gui/arrow.png");

    private final List<SatisfactoryFTBConfig.NodeEntry> availableNodes;
    private int selectedIndex = -1;
    private static final int UI_SIZE = 256;

    // Tracks the animation progress (0.0 to 1.0) for every slice
    private final float[] sliceProgress;
    // Tracks the system time to ensure silky smooth framerate-independent animations
    private long lastTime = 0;

    public RadialScannerScreen(List<SatisfactoryFTBConfig.NodeEntry> availableNodes) {
        super(Component.literal("Scanner Menu"));
        this.availableNodes = availableNodes;
        this.sliceProgress = new float[availableNodes.size()];
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (availableNodes.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, "No Ores Unlocked!", this.width / 2, this.height / 2, 0xFF5555);
            return;
        }

        // ==========================================
        // TRUE DELTA TIME CALCULATION
        // ==========================================
        if (lastTime == 0) lastTime = net.minecraft.Util.getMillis();
        long currentTime = net.minecraft.Util.getMillis();
        // Calculate seconds passed since last frame (capped at 0.1s to prevent lag spike jumps)
        float deltaTime = Math.min((currentTime - lastTime) / 1000.0f, 0.1f);
        lastTime = currentTime;

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Dynamic Slice Calculation
        double sliceAngle = 360.0 / availableNodes.size();
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double distanceSq = dx * dx + dy * dy;

        if (distanceSq > 400) {
            double mouseAngle = (Math.toDegrees(Math.atan2(dy, dx)) + 360 + 90) % 360;
            selectedIndex = (int) Math.round(mouseAngle / sliceAngle) % availableNodes.size();
        } else {
            selectedIndex = -1;
        }

        // ==========================================
        // 1. DRAW STATIC BACKGROUND
        // ==========================================
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.blit(BG_TEXTURE, centerX - (UI_SIZE / 2), centerY - (UI_SIZE / 2), 0, 0, UI_SIZE, UI_SIZE, UI_SIZE, UI_SIZE);

        // ==========================================
        // 2. PROGRAMMATIC DYNAMIC HIGHLIGHT WEDGE
        // ==========================================
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();

        int r = 230, g = 126, b = 34;
        int steps = (int) Math.max(10, sliceAngle / 2);
        double angleStep = sliceAngle / steps;

        for (int i = 0; i < availableNodes.size(); i++) {

            float target = (i == selectedIndex) ? 1.0f : 0.0f;

            // Increment or decrement progress based on actual time, not frames (speed: 12 = ~83ms fade)
            if (sliceProgress[i] < target) {
                sliceProgress[i] = Math.min(1.0f, sliceProgress[i] + deltaTime * 12.0f);
            } else if (sliceProgress[i] > target) {
                sliceProgress[i] = Math.max(0.0f, sliceProgress[i] - deltaTime * 12.0f);
            }

            // Only draw if it's actually visible enough to be seen
            if (sliceProgress[i] > 0.01f) {

                // CUBIC EASE-OUT: Makes the animation fast at first, then gently settles into place
                float p = sliceProgress[i];
                float ease = 1.0f - (1.0f - p) * (1.0f - p) * (1.0f - p);

                // Animate Alpha (0 to 140)
                int a = (int)(140 * ease);

                // Animate Physical Radius (Wedge expands 7px inward and outward for a "pop" effect!)
                float innerRadius = 90.0f - (7.0f * ease);
                float outerRadius = 121.0f + (7.0f * ease);

                double startAngle = (i * sliceAngle) - 90 - (sliceAngle / 2);

                Tesselator tesselator = Tesselator.getInstance();
                BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
                org.joml.Matrix4f matrix = guiGraphics.pose().last().pose();

                for (int step = 0; step <= steps; step++) {
                    double currentAngle = Math.toRadians(startAngle + (step * angleStep));
                    float cos = (float) Math.cos(currentAngle);
                    float sin = (float) Math.sin(currentAngle);

                    buffer.addVertex(matrix, centerX + (cos * innerRadius), centerY + (sin * innerRadius), 0.0F).setColor(r, g, b, a);
                    buffer.addVertex(matrix, centerX + (cos * outerRadius), centerY + (sin * outerRadius), 0.0F).setColor(r, g, b, a);
                }

                BufferUploader.drawWithShader(buffer.buildOrThrow());
            }
        }

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();

        // ==========================================
        // 3. DRAW ROTATING ARROW OVERLAY
        // ==========================================
        if (selectedIndex != -1) {
            float arrowRotation = (float) Math.toDegrees(Math.atan2(dy, dx)) + 90.0f;

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(centerX, centerY, 0);
            guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(arrowRotation));

            guiGraphics.blit(ARROW_TEXTURE, -(UI_SIZE / 2), -(UI_SIZE / 2), 0, 0, UI_SIZE, UI_SIZE, UI_SIZE, UI_SIZE);
            guiGraphics.pose().popPose();
        }

        RenderSystem.disableBlend();

        // ==========================================
        // 4. DRAW ICONS & CENTER TEXT
        // ==========================================
        double iconRadius = 100.0;

        for (int i = 0; i < availableNodes.size(); i++) {

            // Grab the exact same ease curve so the icon scales perfectly in sync with the wedge
            float p = sliceProgress[i];
            float ease = 1.0f - (1.0f - p) * (1.0f - p) * (1.0f - p);

            double angle = Math.toRadians((i * sliceAngle) - 90);
            int iconCenterX = centerX + (int) (Math.cos(angle) * iconRadius);
            int iconCenterY = centerY + (int) (Math.sin(angle) * iconRadius);

            SatisfactoryFTBConfig.NodeEntry node = availableNodes.get(i);
            ResourceLocation loc = ResourceLocation.parse(node.baseNodeId);
            ItemStack iconStack = new ItemStack(BuiltInRegistries.BLOCK.get(loc));

            // Fallback: If it's a fluid (invisible block), try to grab its bucket equivalent for the UI icon!
            if (iconStack.isEmpty() || iconStack.getItem() == net.minecraft.world.item.Items.AIR) {
                if (node.baseNodeId.equals("satisfactory_ore_nodes:geyser_node")) {
                    iconStack = new ItemStack(Satisfactory_ore_nodes.GEYSER_NODE_ITEM.get());
                } else {
                    ResourceLocation bucketLoc = ResourceLocation.fromNamespaceAndPath(loc.getNamespace(), loc.getPath() + "_bucket");
                    iconStack = new ItemStack(BuiltInRegistries.ITEM.get(bucketLoc));
                }
            }

            // Animate Icon Scale (Grows from 1.5x up to 1.8x when hovered!)
            float currentScale = 1.5f + (0.3f * ease);

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(iconCenterX, iconCenterY, 0);
            guiGraphics.pose().scale(currentScale, currentScale, 1.0f);

            guiGraphics.renderItem(iconStack, -8, -8);
            guiGraphics.pose().popPose();

            guiGraphics.drawCenteredString(this.font, "Scanning for", centerX, centerY - 24, 0xFFFFFF);

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