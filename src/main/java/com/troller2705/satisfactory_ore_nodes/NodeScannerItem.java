package com.troller2705.satisfactory_ore_nodes;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
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

import java.util.List;

public class NodeScannerItem extends Item {
    public NodeScannerItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    // Required so the game lets us hold the button down
    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // Starts the hold timer
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int count) {
        if (!level.isClientSide && entity instanceof Player player) {
            int ticksHeld = this.getUseDuration(stack, entity) - count;

            // If held for 6 ticks (~300ms), open the Radial Menu
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

            // If released BEFORE 6 ticks (a fast tap!), perform a standard scan
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

    // Extracted out so the Radial Screen can trigger the scan instantly when you release the mouse
    public static void performClientScan(Player player, String targetOreId) {
        Level level = player.level();
        BlockPos playerPos = player.blockPosition();
        StringBuilder positions = new StringBuilder();
        int foundCount = 0;

        for (BlockPos pos : BlockPos.betweenClosed(playerPos.offset(-64, -20, -64), playerPos.offset(64, 20, 64))) {
            BlockState scanState = level.getBlockState(pos);

            if (scanState.getBlock() instanceof ResourceNodeBlock) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof ResourceNodeBlockEntity nodeBE) {
                    String oreId = nodeBE.getOreId();
                    if (!oreId.equals(targetOreId)) continue;

                    int purity = scanState.getValue(ResourceNodeBlock.PURITY);
                    String oreName = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(oreId)).getName().getString();

                    positions.append(pos.asLong()).append("|").append(oreName).append("|").append(purity).append(",");
                    foundCount++;
                }
            }
        }

        if (foundCount > 0) {
            player.getPersistentData().putString("scanned_nodes", positions.toString());
            player.getPersistentData().putLong("last_scan_time", level.getGameTime());
        }

        // Spawn holographic ring
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