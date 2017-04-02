package net.wwsf.domochevsky.territorialdealingsold.item;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.wwsf.domochevsky.territorialdealingsold.Main;
import net.wwsf.domochevsky.territorialdealingsold.TerritoryHandler;
import net.wwsf.domochevsky.territorialdealingsold._Territory;

public class FactionRenameOrder extends LeaderRequiredItem
{
	public FactionRenameOrder()
	{
		this.setMaxStackSize(64);	// Can hold a big stack of these
		this.setFull3D();
		this.setCreativeTab(CreativeTabs.TOOLS);

		this.setRegistryName("factionnameorder");
		this.setUnlocalizedName("territorychevsky_factionnameorder");
	}


	@Override
	public String getItemStackDisplayName(ItemStack stack) { return "Faction Name Order"; }


	@Override
	public ActionResult<ItemStack> onItemRightClick(ItemStack stack, World world, EntityPlayer player, EnumHand hand)
	{
		if (world.isRemote) { return new ActionResult(EnumActionResult.PASS, stack); }	// Not doing this on client side

		if (stack.hasDisplayName())
		{
			if (this.changeFactionName(player, stack.getDisplayName()))
			{
				stack.stackSize -= 1;	// Successful
			}
		}

		return new ActionResult(EnumActionResult.PASS, stack);
	}


	private boolean changeFactionName(EntityPlayer player, String newName)
	{
		if (player == null) { return false; }

		_Territory faction = TerritoryHandler.getFactionPlayerIsLeaderOf(player);

		if (faction == null) { return false; }	// Not a leader of anything

		faction.setFactionName(newName);

		faction.sendMessageToFactionMembers(player.worldObj, "Your faction has been renamed to " + faction.getFactionName() + ".");

		Main.console("Player " + player.getName() + " renamed faction to " + faction.getFactionName() + ".");

		return true;	// Checked out
	}


	@SideOnly(Side.CLIENT)
	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean par4)
	{
	    super.addInformation(stack, player, list, par4);

	    list.add("Red hot letters are burned");
	    list.add("into this sheet of paper.");
	    list.add("Use this to change the");
	    list.add("name of your faction.");

	    if (!stack.hasDisplayName())
	    {
	    	list.add("§cGive it a name on an anvil first.");
	    }

	    list.add("§cLeader-usable only.");
		list.add("§cUsed up on successful rename.");
    }
}
