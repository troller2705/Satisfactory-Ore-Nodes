package com.troller2705.satisfactory_ore_nodes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;


public class NodeScannerItem extends Item {
    public NodeScannerItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) {
            // CLIENT SIDE: Scan and save locally for the 3D renderer
            BlockPos playerPos = player.blockPosition();
            StringBuilder positions = new StringBuilder();
            int foundCount = 0; // Initialize the counter here

            // Scan 64 blocks out, 20 up/down
            for (BlockPos pos : BlockPos.betweenClosed(playerPos.offset(-64, -20, -64), playerPos.offset(64, 20, 64))) {
                // Get the block's unique ID
                String blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString();

                // FIX: Check the list for a matching ID within the NodeEntry objects
                // Inside NodeScannerItem scanning loop
                boolean isScannable = false;
                for (SatisfactoryFTBConfig.NodeEntry entry : SatisfactoryFTBConfig.scannableNodes) {
                    if (entry.baseNodeId.equals(blockId)) {
                        isScannable = true;
                        break;
                    }
                }

                if (isScannable) {
                    positions.append(pos.asLong()).append(",");
                    foundCount++;
                }
            }

            // Only update if we actually found something
            if (foundCount > 0) {
                player.getPersistentData().putString("scanned_nodes", positions.toString());
                player.getPersistentData().putLong("last_scan_time", level.getGameTime());
            }

            spawnPingWave(player);
        } else {
            // SERVER SIDE: Play the sound for all players to hear
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.5f, 1.5f);
        }

        player.getCooldowns().addCooldown(this, 40);
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