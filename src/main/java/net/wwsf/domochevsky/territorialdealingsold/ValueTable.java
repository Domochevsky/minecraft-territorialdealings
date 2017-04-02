package net.wwsf.domochevsky.territorialdealingsold;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;

class ValueTable 
{
	// How much are all items worth?
	private static int emeraldValue = 3;
	private static int diamondValue = 2;
	private static int goldValue = 1;


	public static void setValueForGold(int value) { goldValue = value; }
	public static void setValueForEmerald(int value) { emeraldValue = value; }
	public static void setValueForDiamond(int value) { diamondValue = value; }


	static int getValueFromItem(Item item)
	{
		if (item == null) { return 0; }

		if (item == Items.EMERALD) { return emeraldValue; }
		else if (item == Items.DIAMOND) { return diamondValue; }
		else if (item == Items.GOLD_INGOT) { return goldValue; }

		return 0;	// Unknown item
	}


	static int getValueFromBlock(Block block)
	{
		if (block == null) { return 0; }

		if (block == Blocks.EMERALD_BLOCK) { return emeraldValue * 9; }
		else if (block == Blocks.DIAMOND_BLOCK) { return diamondValue * 9; }
		else if (block == Blocks.GOLD_BLOCK) { return goldValue * 9; }

		return 0;
	}
}
