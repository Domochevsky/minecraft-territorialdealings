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

public class ReturnOrder extends LeaderRequiredItem
{
	public ReturnOrder()
	{
		this.setMaxStackSize(64);	// Can hold a big stack of these
		this.setFull3D();
		this.setCreativeTab(CreativeTabs.TOOLS);

		this.setRegistryName("returnorder");
		this.setUnlocalizedName("territorychevsky_returnorder");
	}


	@Override
	public String getItemStackDisplayName(ItemStack stack)
	{
		return "Return Order";
	}


	@Override
	public ActionResult<ItemStack> onItemRightClick(ItemStack stack, World world, EntityPlayer player, EnumHand hand)
	{
		if (world.isRemote) { return new ActionResult(EnumActionResult.PASS, stack); }	// Not doing this on client side

		if (player.isSneaking())
		{
			// Set
			_Territory faction = TerritoryHandler.getFactionPlayerIsLeaderOf(player);

			if (faction == null)
			{
				Main.sendMessageToPlayer(player, "You don't seem to be a faction leader.");
			}
			else
			{
				faction.markPositionForReturn(player);
			}
		}
		else
		{
			_Territory faction = TerritoryHandler.getFactionPlayerIsMemberOf(player);

			if (faction == null)
			{
				Main.sendMessageToPlayer(player, "You don't seem to be a member of a faction.");
			}
			else
			{
				// Return
				Main.sendMessageToPlayer(player, "Returning...");

				// SFX
				Main.startFireworks(player);

				//world.playSound((EntityPlayer)null, player.posX, player.posY, player.posZ, SoundEvents.entity_firework_launch, SoundCategory.AMBIENT, 3.0F, 1.0F);
				//world.playAuxSFXAtEntity(player, "fireworks.largeBlast", 1.4F, 0.5F);

				// Make it happen
				faction.moveMemberToReturnPoint(player);

				stack.stackSize -= 1;	// Used up
			}
		}

		return new ActionResult(EnumActionResult.PASS, stack);
	}


	@SideOnly(Side.CLIENT)
	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean par4)
	{
	    super.addInformation(stack, player, list, par4);

	    list.add("Use to jump to your faction's return point.");
	    list.add("§eLeader: Crouch-use to mark a point.");
	    list.add("§cThe return point must be in your territory.");
    }


	@Override
	public boolean showDurabilityBar(ItemStack stack) { return false; }
}
