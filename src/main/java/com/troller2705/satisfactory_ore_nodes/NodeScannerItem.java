package com.troller2705.satisfactory_ore_nodes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;


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
                BlockState scanState = level.getBlockState(pos);

                // 1. Look for the Master Node Block
                if (scanState.getBlock() instanceof ResourceNodeBlock) {
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be instanceof ResourceNodeBlockEntity nodeBE) {
                        // 2. Extract data for the HUD
                        String oreId = nodeBE.getOreId();
                        int purity = scanState.getValue(ResourceNodeBlock.PURITY);

                        // Get the friendly name (e.g., "Iron Ore")
                        String oreName = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(oreId))
                                .getName().getString();

                        // 3. Save to your Waypoint/Beacon system
                        // Example: beaconManager.addBeacon(pos, oreName, purity);
                        positions.append(pos.asLong()).append("|").append(oreName).append("|").append(purity).append(",");
                        foundCount++;
                    }
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