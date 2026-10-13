package com.troller2705.satisfactory_ore_nodes;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class NodeScannerItem extends Item {

    // Helper record for the clustering math
    private record NodeClusterData(BlockPos pos, String oreName, int purity) {}

    public NodeScannerItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int count) {
        if (!level.isClientSide && entity instanceof Player player) {
            int ticksHeld = this.getUseDuration(stack, entity) - count;

            if (ticksHeld == 6) {
                List<String> unlocked = player.getData(Satisfactory_ore_nodes.UNLOCKED_ORES);
                String unlockedStr = (unlocked == null || unlocked.isEmpty()) ? "none" : String.join(",", unlocked);

                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                        (net.minecraft.server.level.ServerPlayer) player,
                        new OpenScannerMenuPacket(unlockedStr)
                );
            }
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (entity instanceof Player player) {
            int ticksHeld = this.getUseDuration(stack, entity) - timeLeft;

            if (ticksHeld < 6) {
                String targetOreId = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("target_ore_id");

                if (targetOreId.isEmpty()) {
                    if (level.isClientSide) player.displayClientMessage(Component.literal("No ore selected! Hold Right-Click to choose.").withStyle(ChatFormatting.RED), true);
                    return;
                }

                if (level.isClientSide) {
                    performClientScan(player, targetOreId);
                } else {
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.5f, 1.5f);
                    player.getCooldowns().addCooldown(this, 40);
                }
            }
        }
    }

    public static void performClientScan(Player player, String targetId) {
        Level level = player.level();
        BlockPos playerPos = player.blockPosition();

        List<NodeClusterData> rawNodes = new ArrayList<>();

        // 1. Gather all raw nodes in range
        for (BlockPos pos : BlockPos.betweenClosed(playerPos.offset(-64, -20, -64), playerPos.offset(64, 20, 64))) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be == null) continue;

            String foundId = null;
            int purity = 1;
            String nodeName = "";

            if (be instanceof ResourceNodeBlockEntity oreBE) {
                foundId = oreBE.getOreId();
                purity = oreBE.getPurity();
                nodeName = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(foundId)).getName().getString();
            }
            else if (be instanceof FluidNodeBlockEntity fluidBE) {
                foundId = fluidBE.getFluidId();
                purity = fluidBE.getPurity();
                net.minecraft.world.level.material.Fluid fluid = BuiltInRegistries.FLUID.get(ResourceLocation.parse(foundId));
                nodeName = fluid.getFluidType().getDescription().getString();
            }
            else if (be instanceof GeyserNodeBlockEntity geyserBE) {
                foundId = "satisfactory_ore_nodes:geyser_node";
                purity = geyserBE.getPurity();
                nodeName = "Geyser";
            }

            if (foundId != null && foundId.equals(targetId)) {
                rawNodes.add(new NodeClusterData(pos.immutable(), nodeName, purity));
            }
        }

        // 2. Cluster nodes that are close to each other
        if (!rawNodes.isEmpty()) {
            List<NodeClusterData> clusteredNodes = new ArrayList<>();
            boolean[] grouped = new boolean[rawNodes.size()];

            double clusterRadiusSq = 16.0 * 16.0;

            for (int i = 0; i < rawNodes.size(); i++) {
                if (grouped[i]) continue;

                NodeClusterData baseNode = rawNodes.get(i);
                List<NodeClusterData> currentGroup = new ArrayList<>();
                currentGroup.add(baseNode);
                grouped[i] = true;

                for (int j = i + 1; j < rawNodes.size(); j++) {
                    if (grouped[j]) continue;
                    NodeClusterData otherNode = rawNodes.get(j);

                    if (baseNode.purity() == otherNode.purity() && baseNode.oreName().equals(otherNode.oreName())) {
                        if (baseNode.pos().distSqr(otherNode.pos()) <= clusterRadiusSq) {
                            currentGroup.add(otherNode);
                            grouped[j] = true;
                        }
                    }
                }

                int sumX = 0, sumY = 0, sumZ = 0;
                for (NodeClusterData n : currentGroup) {
                    sumX += n.pos().getX();
                    sumY += n.pos().getY();
                    sumZ += n.pos().getZ();
                }
                BlockPos centerPos = new BlockPos(sumX / currentGroup.size(), sumY / currentGroup.size(), sumZ / currentGroup.size());

                clusteredNodes.add(new NodeClusterData(centerPos, baseNode.oreName(), baseNode.purity()));
            }

            StringBuilder positions = new StringBuilder();
            for (NodeClusterData cluster : clusteredNodes) {
                positions.append(cluster.pos().asLong()).append("|")
                        .append(cluster.oreName()).append("|")
                        .append(cluster.purity()).append(",");
            }

            player.getPersistentData().putString("scanned_nodes", positions.toString());
            player.getPersistentData().putLong("last_scan_time", level.getGameTime());
        }

        // 3. Spawn holographic sonic boom ring
        for (int r = 1; r <= 3; r++) {
            double radius = r * 2.0;
            for (int d = 0; d < 360; d += 15) {
                double rad = Math.toRadians(d);
                level.addParticle(ParticleTypes.SONIC_BOOM,
                        player.getX() + Math.cos(rad) * radius,
                        player.getY(),
                        player.getZ() + Math.sin(rad) * radius,
                        0, 0, 0);
            }
        }
    }
}