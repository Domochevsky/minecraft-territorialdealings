package net.wwsf.domochevsky.territorialdealingsold.item;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
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

public class PlayerIDCard extends Item
{
	public PlayerIDCard()
	{
		this.setMaxStackSize(1);
		this.setMaxDamage(1);
		this.setFull3D();
		this.setCreativeTab(CreativeTabs.TOOLS);

		this.setRegistryName("playerid");
		this.setUnlocalizedName("territorychevsky_playerid");
	}


	@Override
	public String getItemStackDisplayName(ItemStack stack)
	{
		if (stack.hasTagCompound())
		{
			return "Player ID Card of " + stack.getTagCompound().getString("ownerName");
		}

		return "Blank Player ID Card";
	}


	@Override
	public ActionResult<ItemStack> onItemRightClick(ItemStack stack, World world, EntityPlayer player, EnumHand hand)
	{
		if (world.isRemote) { return new ActionResult(EnumActionResult.PASS, stack); }	// Not doing this on client side

		if (stack.hasTagCompound())	// Has been tagged by a player
		{
			if (UUID.fromString(stack.getTagCompound().getString("playerUUID")).equals(player.getGameProfile().getId()))
			{
				// This is the player who has tagged this card
				_Territory faction = TerritoryHandler.getFactionPlayerIsMemberOf(player);

				if (faction != null)
				{
					if (player.isSneaking())	// Is sneaking, so leaving their faction now
					{
						this.leaveFaction(player);
					}
					else	// Refreshing the faction name
					{
						stack.getTagCompound().setString("factionName", faction.getFactionName());

					}
					// else, not sneaking or not part of any factions, so nothing to be done here
				}
			}
			else	// Not the player who tagged this card, so trying to add them to their faction now (if they have one)
			{
				if (player.isSneaking())
				{
					if (this.removePlayerFromFaction(world, player, UUID.fromString(stack.getTagCompound().getString("playerUUID"))))
					{
						stack.getTagCompound().setString("factionName", "NONE");
					}
				}
				else
				{
					// Adding them now

					_Territory faction = this.addPlayerToFaction(world, player, UUID.fromString(stack.getTagCompound().getString("playerUUID")));

					if (faction != null)
					{
						stack.getTagCompound().setString("factionName", faction.getFactionName());
					}
				}
			}
		}
		else	// Is blank, so filling it now (and removing us from all of our previous factions? Might not be necessary)
		{
			stack.setTagCompound(new NBTTagCompound());	// Init

			stack.getTagCompound().setString("ownerName", player.getName());
			stack.getTagCompound().setString("playerUUID", player.getGameProfile().getId().toString());

			stack.getTagCompound().setString("factionName", "NONE");

			_Territory faction = TerritoryHandler.getFactionPlayerIsMemberOf(player);

			if (faction != null)
			{
				stack.getTagCompound().setString("factionName", faction.getFactionName());
			}
			// else, not part of a faction, so doesn't matter

			stack.setItemDamage(1);
		}

		return new ActionResult(EnumActionResult.PASS, stack);
	}


	private void leaveFaction(EntityPlayer player)
	{
		// Shift-used their own ID card

		_Territory faction = TerritoryHandler.getFactionPlayerIsMemberOf(player);

		if (faction != null)
		{
			if (faction.isLeader(player))
			{
				TerritoryHandler.removeTerritory(faction);

				faction.dropValuablesAtFactionMember(player.worldObj);
				faction.sendMessageToFactionMembers(player.worldObj, "The leader of " + faction.getFactionName() + " left the faction. Disbanding!");
				Main.console("[TERRITORIAL DEALINGS] " + player.getName() + ", the leader of " + faction.getFactionName() + ", left the faction. Disbanding!");
			}
			else // Not the leader, but at least a member
			{
				faction.removeMember(player);
				Main.sendMessageToPlayer(player, "You left your faction.");
				Main.console("[TERRITORIAL DEALINGS] " + player.getName() + ", a member of " + faction.getFactionName() + ", left the faction.");
			}
		}
	}


	private _Territory addPlayerToFaction(World world, EntityPlayer leader, UUID uuid)
	{
		_Territory faction = TerritoryHandler.getFactionPlayerIsLeaderOf(leader);

		if (faction == null)
		{
			//Main.sendMessageToPlayer(leader, "Couldn't add that player to your faction. You don't seem to be the leader of anything.");
			return null;
		}

		Iterator<EntityPlayer> it = world.playerEntities.iterator();

		while (it.hasNext())
		{
			EntityPlayer player = it.next();

			if (player.getGameProfile().getId().equals(uuid))
			{
				faction.addMember(player);

				return faction;	// Done
			}
			// else, not the player I'm looking for.
		}

		// Still here? Then I couldn't find that player

		Main.sendMessageToPlayer(leader, "Couldn't add that player to your faction. They might not be online.");
		return null;
	}


	private boolean removePlayerFromFaction(World world, EntityPlayer leader, UUID playerUUID)
	{
		_Territory faction = TerritoryHandler.getFactionPlayerIsLeaderOf(leader);

		Iterator it = world.playerEntities.iterator();

		while (it.hasNext())
		{
			EntityPlayer player = (EntityPlayer) it.next();

			if (player.getGameProfile().getId().equals(playerUUID))
			{
				// Found the player
				if (faction.isLeader(player))	// That's the leader, too
				{
					TerritoryHandler.removeTerritory(faction);
					faction.dropValuablesAtFactionMember(player.worldObj);
					faction.sendMessageToFactionMembers(leader.worldObj, "The leader of " + faction.getFactionName() + " left the faction. Disbanding!");
					return true;
				}

				else if (faction.isPartOfFaction(player))	// Regular member
				{
					faction.removeMember(player);
					Main.sendMessageToPlayer(leader, player.getName() + " has been removed from " + faction.getFactionName() + ".");
					return true;
				}
				else
				{
					Main.sendMessageToPlayer(leader, player.getName() + " is not a member of " + faction.getFactionName() + ".");
					return false;	// Done either way
				}
				// else, neither leader nor regular member
			}
			// else, not the player I'm looking for.
		}

		// Still here? Then I couldn't find that player
		Main.sendMessageToPlayer(leader, "Couldn't remove that player from your faction. They might not be online.");
		return false;
	}


	@SideOnly(Side.CLIENT)
	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean par4)
	{
	    super.addInformation(stack, player, list, par4);

	    if (!stack.hasTagCompound())
	    {
	    	list.add("Use to sign this card with your ID.");
	    }
	    else
	    {
	    	list.add("Signed by " + stack.getTagCompound().getString("ownerName"));
	    	list.add("Member of " + stack.getTagCompound().getString("factionName"));
	    	list.add("§cCrouch-use to leave your faction.");
	    	list.add("§eLeader: Use/crouch-use to add/remove");
	    	list.add("§ethis player to/from your faction.");
	    	list.add("§cLeader: If you crouch-use your own");
	    	list.add("§ccard, your faction will disband.");
	    }
    }


	@Override
	public boolean showDurabilityBar(ItemStack stack) { return false; }
}
