package com.troller2705.satisfactory_ore_nodes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.VibrationParticleOption;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.BlockPositionSource;

public class NodeScannerItem extends Item {
    public NodeScannerItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide) {
            // 1. Search Logic (Server-side)
            BlockPos playerPos = player.blockPosition();
            BlockPos closest = null;
            double minDist = Double.MAX_VALUE;

            // Scan a 64-block horizontal radius
            for (BlockPos pos : BlockPos.betweenClosed(playerPos.offset(-64, -16, -64), playerPos.offset(64, 16, 64))) {
                if (level.getBlockState(pos).getBlock() instanceof ResourceNodeBlock) {
                    double dist = pos.distSqr(playerPos);
                    if (dist < minDist) {
                        minDist = dist;
                        closest = pos.immutable();
                    }
                }
            }

            if (closest != null) {
                // Play "Target Acquired" sound
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 0.5f);

                // Send a Vibration Particle (the purple sculk-like streak) toward the node
                ((ServerLevel)level).sendParticles(new VibrationParticleOption(new BlockPositionSource(closest), 30),
                        player.getX(), player.getY() + 1, player.getZ(), 1, 0, 0, 0, 0);
            }
        } else {
            // 2. Visual Effect (Client-side)
            spawnPingWave(player);
        }

        player.getCooldowns().addCooldown(this, 40); // 2-second cooldown like Satisfactory
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    private void spawnPingWave(Player player) {
        Level level = player.level();
        // Create 3 concentric rings of particles
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