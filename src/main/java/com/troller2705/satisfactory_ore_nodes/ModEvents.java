package com.troller2705.satisfactory_ore_nodes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = Satisfactory_ore_nodes.MODID)
public class ModEvents {


    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        BlockState state = event.getState();
        if (state.getBlock() instanceof ResourceNodeBlock nodeBlock) {
            Player player = event.getPlayer();

            // Check for Creative + Shift (Crouch)
            if (player != null && player.isCreative() && player.isShiftKeyDown()) {
                // Do NOT cancel the event. Do NOT drop extra items.
                // Just let Minecraft delete the block.
                return;
            }

            // --- Otherwise, run the Infinite Logic ---
            if (event.getLevel() instanceof Level serverLevel) {
                // Cancel the break so it stays infinite
                event.setCanceled(true);

                LevelAccessor level = event.getLevel();
                BlockPos pos = event.getPos();
                int purity = state.getValue(ResourceNodeBlock.PURITY);

                // Calculate yield based on purity (Impure=1, Normal=2, Pure=4)
                int count = (int) Math.pow(2, purity);

                // Spawn the items at the block's location
                Block.popResource((Level) level, pos, new ItemStack(nodeBlock.getDropItem(), count));

                serverLevel.sendBlockUpdated(event.getPos(), state, state, 3);
            }
        }
    }

}
