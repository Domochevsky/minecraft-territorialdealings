package net.wwsf.domochevsky.territorialdealingsold.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.wwsf.domochevsky.territorialdealingsold.TerritoryHandler;

public class LeaderRequiredItem extends Item
{
	// Placeholder, to identify items via instancing

	@Override
	public void onCreated(ItemStack stack, World world, EntityPlayer player)
    {
		if (!player.capabilities.isCreativeMode && TerritoryHandler.getFactionPlayerIsLeaderOf(player) == null)	// Only leader-types can craft this.
		{
			stack.stackSize = 0;
		}
    }
}
