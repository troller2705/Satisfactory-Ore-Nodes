package com.troller2705.satisfactory_ore_nodes;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

public class DynamicNodeItem extends BlockItem
{
    private final String oreId;
    private final int purity;
    private final int oreIndex;

    public DynamicNodeItem(Block block, Properties props, String oreId, int purity, int oreIndex) {
        super(block, props);
        this.oreId = oreId;
        this.purity = purity;
        this.oreIndex = oreIndex;
    }

    public String getOreId() { return oreId; }
    public int getPurity() { return purity; }
    public int getOreIndex() { return oreIndex; }
}
