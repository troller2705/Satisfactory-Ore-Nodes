package com.troller2705.satisfactory_ore_nodes;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public class ScannerMenu extends ChestMenu {
    private final ItemStack scannerStack;
    private final int page;
    private final NodeScannerItem itemInstance;

    public ScannerMenu(int id, Inventory playerInv, ItemStack scanner, int page) {
        super(MenuType.GENERIC_9x6, id, playerInv, new SimpleContainer(54), 6);
        this.scannerStack = scanner;
        this.page = page;
        this.itemInstance = (NodeScannerItem) scanner.getItem();
    }

    @Override
    public void clicked(int slotId, int button, net.minecraft.world.inventory.ClickType clickType, Player player) {
        // Top 45 slots are for Ores, bottom row (45-53) is for Navigation
        if (slotId >= 0 && slotId < 54) {
            ItemStack clickedStack = this.getContainer().getItem(slotId);
            if (clickedStack.isEmpty()) return;

            // Handle Navigation Buttons
            if (slotId == 45 && page > 0) { // Previous Page
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 1f, 1f);
                itemInstance.openVanillaMenu(player, scannerStack, page - 1);
                return;
            }
            if (slotId == 53) { // Next Page
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 1f, 1f);
                itemInstance.openVanillaMenu(player, scannerStack, page + 1);
                return;
            }

            // Handle Ore Selection
            String oreId = clickedStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                    .copyTag().getString("target_id");

            if (!oreId.isEmpty()) {
                CompoundTag tag = scannerStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                tag.putString("target_ore_id", oreId);
                scannerStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 1.0f, 1.2f);
                player.closeContainer();
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }
}