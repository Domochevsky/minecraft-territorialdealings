package net.wwsf.domochevsky.territorialdealingsold.item;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.wwsf.domochevsky.territorialdealingsold.TerritoryHandler;

public class StartFactionDeed extends Item
{
	public StartFactionDeed()
	{
		this.setMaxStackSize(1);
		this.setFull3D();
		this.setCreativeTab(CreativeTabs.TOOLS);

		this.setRegistryName("startfactiondeed");
		this.setUnlocalizedName("territorychevsky_startfactiondeed");
	}


	@Override
	public String getItemStackDisplayName(ItemStack stack) { return "New Faction Deed"; }


	@Override
	public ActionResult<ItemStack> onItemRightClick(ItemStack stack, World world, EntityPlayer player, EnumHand hand)
	{
		if (world.isRemote) { return new ActionResult(EnumActionResult.PASS, stack); }	// Not doing this on client side

		BlockPos pos = new BlockPos(player.posX, player.posY, player.posZ);

		if (TerritoryHandler.tryToAddFaction(world, player, pos))
		{
			stack.stackSize -= 1;	// Success
		}

		return new ActionResult(EnumActionResult.PASS, stack);
	}


	@SideOnly(Side.CLIENT)
	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean par4)
	{
		super.addInformation(stack, player, list, par4);

		list.add("'The start of something great.'");
		list.add("Use this to start a faction");
		list.add("in your current chunk.");
		list.add("§9Each claimed chunk increases");
		list.add("§9territory upkeep by 1.");
		list.add("§cUsed up on successful creation.");

	    if (player.capabilities.isCreativeMode)
	    {
	    	list.add("§e[Creative Mode] Use on existing faction");
	    	list.add("§eterritory to assume leadership.");
	    }
    }
}
