package net.wwsf.domochevsky.territorialdealingsold.item;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
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

public class FactionLedger extends LeaderRequiredItem
{
	public FactionLedger()
	{
		this.setMaxStackSize(1);	// Can hold a big stack of these
		this.setFull3D();
		this.setCreativeTab(CreativeTabs.TOOLS);

		this.setRegistryName("factionledger");
		this.setUnlocalizedName("territorychevsky_factionledger");
	}


	@Override
	public String getItemStackDisplayName(ItemStack stack)
	{
		if (stack.hasTagCompound())
		{
			return "Faction Ledger of " + stack.getTagCompound().getString("factionName");
		}

		return "Blank Faction Ledger";
	}


	@Override
	public ActionResult<ItemStack> onItemRightClick(ItemStack stack, World world, EntityPlayer player, EnumHand hand)
	{
		if (world.isRemote) { return new ActionResult(EnumActionResult.PASS, stack); }	// Not doing this on client side

		if (stack.hasTagCompound())	// Has been tagged by a leader, so displaying info about that faction now that you used it
		{
			this.displayFactionInfo(player, stack);
		}
		else
		{
			_Territory faction = TerritoryHandler.getFactionPlayerIsLeaderOf(player);

			if (faction != null)
			{
				stack.setTagCompound(new NBTTagCompound());	// Init
				stack.getTagCompound().setString("ownerName", player.getName());
				stack.getTagCompound().setString("playerUUID", player.getGameProfile().getId().toString());	// UUID of the faction leader
				stack.getTagCompound().setString("factionName", faction.getFactionName()); 					// Name of the faction

				stack.setItemDamage(1);
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

	    list.add("Holds information about a");
	    list.add("faction and its members.");

	    if (!stack.hasTagCompound())
	    {
	    	list.add("It's blank.");
	    	list.add("§eUse to fill it as a leader.");
	    }
	    else
	    {
	    	list.add("§9Signed by " + stack.getTagCompound().getString("ownerName") + ".");
	    	list.add("§9Faction: " + stack.getTagCompound().getString("factionName") + ".");
	    	list.add("§eUse to read it.");
	    }
    }


	@Override
	public boolean showDurabilityBar(ItemStack stack) { return false; }


	// Comes in with the player we're telling this
	private void displayFactionInfo(EntityPlayer player, ItemStack stack)
	{
		UUID uuid = UUID.fromString(stack.getTagCompound().getString("playerUUID"));
		_Territory territory = TerritoryHandler.getFactionPlayerIsLeaderOf(uuid);

		if (territory == null)
		{
			Main.sendMessageToPlayer(player, "This ledger doesn't seem to be valid anymore. Who signed it?");
			return;
		}

		Main.sendMessageToPlayer(player, "§9Faction: " + territory.getFactionName());
		Main.sendMessageToPlayer(player, "§aTerritory: " + territory.getChunkCount() + " Chunks (Protected: " + territory.isUpkeepPaid() + ")");
		Main.sendMessageToPlayer(player, "§eIn vault: " + territory.getUpkeepStored() + " VALUABLES");
		Main.sendMessageToPlayer(player, "Members:");

		Iterator<Entry<UUID, String>> memberIt = territory.getMemberListIterator();

		if (!memberIt.hasNext())	// empty?
		{
			Main.sendMessageToPlayer(player, "§c- NONE (This should not be possible.)");
		}

		while (memberIt.hasNext())	// Listing all members of this faction
		{
			Main.sendMessageToPlayer(player, "- " + memberIt.next().getValue());
		}
	}
}
