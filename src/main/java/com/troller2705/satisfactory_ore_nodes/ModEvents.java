package com.troller2705.satisfactory_ore_nodes;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = Satisfactory_ore_nodes.MODID)
public class ModEvents {


    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("nodes")
                .then(Commands.literal("unlock")
                        .then(Commands.argument("ore", ResourceLocationArgument.id()) // Use .id() instead of .string()
                                .executes(context -> {
                                    // Retrieve as a ResourceLocation
                                    ResourceLocation ore = ResourceLocationArgument.getId(context, "ore");
                                    String oreString = ore.toString();

                                    Player player = context.getSource().getPlayerOrException();
                                    List<String> unlocked = new ArrayList<>(player.getData(Satisfactory_ore_nodes.UNLOCKED_ORES));

                                    if (!unlocked.contains(oreString)) {
                                        unlocked.add(oreString);
                                        player.setData(Satisfactory_ore_nodes.UNLOCKED_ORES, unlocked);
                                        context.getSource().sendSuccess(() -> Component.literal("Unlocked: " + oreString), true);
                                    }
                                    return 1;
                                })))
                .then(Commands.literal("lock")
                        .then(Commands.argument("ore", ResourceLocationArgument.id()) // Use .id() instead of .string()
                                .executes(context -> {
                                    // Retrieve as a ResourceLocation
                                    ResourceLocation ore = ResourceLocationArgument.getId(context, "ore");
                                    String oreString = ore.toString();

                                    Player player = context.getSource().getPlayerOrException();
                                    List<String> unlocked = new ArrayList<>(player.getData(Satisfactory_ore_nodes.UNLOCKED_ORES));

                                    if (unlocked.contains(oreString)) {
                                        unlocked.remove(oreString);
                                        player.setData(Satisfactory_ore_nodes.UNLOCKED_ORES, unlocked);
                                        context.getSource().sendSuccess(() -> Component.literal("Locked: " + oreString), true);
                                    }
                                    return 1;
                                })))
        );
    }

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

                serverLevel.sendBlockUpdated(event.getPos(), state, state, 3);
            }
        }
    }

}
