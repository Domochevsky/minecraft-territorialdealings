package net.wwsf.domochevsky.territorialdealingsold.item;

import java.util.List;
import java.util.UUID;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.wwsf.domochevsky.territorialdealingsold.Main;
import net.wwsf.domochevsky.territorialdealingsold.TerritoryHandler;
import net.wwsf.domochevsky.territorialdealingsold._Territory;

public class InheritanceDeed extends LeaderRequiredItem
{
	public InheritanceDeed()
	{
		this.setMaxStackSize(1);	// Can hold a big stack of these
		this.setFull3D();
		this.setCreativeTab(CreativeTabs.TOOLS);

		this.setRegistryName("inheritancedeed");
		this.setUnlocalizedName("territorychevsky_inheritancedeed");
	}


	@Override
	public String getItemStackDisplayName(ItemStack stack)
	{
		if (stack.hasTagCompound())
		{
			return "Inheritance Deed of " + stack.getTagCompound().getString("factionName");
		}

		return "Inheritance Deed";
	}


	@Override
	public ActionResult<ItemStack> onItemRightClick(ItemStack stack, World world, EntityPlayer player, EnumHand hand)
	{
		if (world.isRemote) { return new ActionResult(EnumActionResult.PASS, stack); }	// Not doing this on client side

		if (stack.hasTagCompound())	// Has been tagged by a player
		{
			if (UUID.fromString(stack.getTagCompound().getString("playerUUID")).equals(player.getGameProfile().getId()))
			{
				Main.sendMessageToPlayer(player, "Your signature is still on the deed.");
			}
			else	// Not the player who signed this
			{
				_Territory faction = TerritoryHandler.getFactionPlayerIsLeaderOf(player);

				if (faction == null)
				{
					// Not a leader of anything, so handing them the faction now
					UUID uuid = UUID.fromString(stack.getTagCompound().getString("playerUUID"));
					faction = TerritoryHandler.getFactionPlayerIsLeaderOf(uuid);

					if (faction != null)
					{
						// exists, so changing leadership now
						faction.setNewLeader(player);

						stack.stackSize -= 1;	// Destroyed

						// SFX
						Main.startFireworks(player);
					}
					else	// else, that faction doesn't exist (anymore?)
					{
						Main.sendMessageToPlayer(player, "That faction doesn't seem to exist anymore.");
						Main.sendMessageToPlayer(player, "What a worthless item.");
					}
				}
				else
				{
					// Already the leader of a faction
					Main.sendMessageToPlayer(player, "You are already the leader of a faction.");
				}
			}
		}
		else	// Is blank, so filling it now
		{
			_Territory faction = TerritoryHandler.getFactionPlayerIsLeaderOf(player);

			if (faction != null)
			{
				stack.setTagCompound(new NBTTagCompound());	// Init
				stack.getTagCompound().setString("ownerName", player.getName());
				stack.getTagCompound().setString("playerUUID", player.getGameProfile().getId().toString());	// UUID of the faction leader
				stack.getTagCompound().setString("factionName", faction.getFactionName()); 					// Name of the faction

				stack.setItemDamage(1);

				Main.sendMessageToPlayer(player, "You have signed the inheritance deed. Take care.");
			}
			else
			{
				Main.sendMessageToPlayer(player, "You don't seem to be the leader of any faction.");
			}
			// else, not the leader of any faction, so doesn't matter
		}

		return new ActionResult(EnumActionResult.PASS, stack);
	}


	@SideOnly(Side.CLIENT)
	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean par4)
	{
	    super.addInformation(stack, player, list, par4);

	    list.add("Used to pass leadership to someone else.");

	    if (!stack.hasTagCompound())
	    {
	    	list.add("§eUse to sign this deed as a leader.");
	    }
	    else
	    {
	    	list.add("§9Signed by " + stack.getTagCompound().getString("ownerName") + ".");
	    	list.add("§9Leader of " + stack.getTagCompound().getString("factionName") + ".");
	    	list.add("§eUse to become leader of this faction.");
	    }
    }


	@Override
	public boolean showDurabilityBar(ItemStack stack) { return false; }
}
