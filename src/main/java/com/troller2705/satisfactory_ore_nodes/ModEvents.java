package com.troller2705.satisfactory_ore_nodes;

import net.minecraft.core.BlockPos;
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
        System.out.println("BREAK EVENT TRIGGERED FOR: " + event.getState().getBlock().getName().getString());
        BlockState state = event.getState();

        // Check if the block being broken is our Resource Node
        if (state.getBlock() instanceof ResourceNodeBlock) {
            LevelAccessor level = event.getLevel();
            BlockPos pos = event.getPos();
            int purity = state.getValue(ResourceNodeBlock.PURITY);

            // Calculate yield based on purity (Impure=1, Normal=2, Pure=4)
            int count = (int) Math.pow(2, purity);

            // Spawn the items at the block's location
            Block.popResource((Level) level, pos, new ItemStack(Items.RAW_IRON, count));

            // CANCEL the event so the block is never actually removed
            event.setCanceled(true);
        }
    }
}
