package com.troller2705.satisfactory_ore_nodes;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.component.Unbreakable;

public class FicsitExtractorItem extends PickaxeItem {

    public FicsitExtractorItem() {
        super(Tiers.IRON, new Item.Properties()
                .attributes(PickaxeItem.createAttributes(Tiers.IRON, 1.0F, -2.8F))
                // Adds the native Unbreakable component to the item.
                // 'false' hides the blue "Unbreakable" text from the tooltip so it looks like a standard item.
                .component(DataComponents.UNBREAKABLE, new Unbreakable(false))
        );
    }

}