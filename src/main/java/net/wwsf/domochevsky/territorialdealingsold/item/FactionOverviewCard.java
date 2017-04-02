package net.wwsf.domochevsky.territorialdealingsold.item;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.wwsf.domochevsky.territorialdealingsold.Main;
import net.wwsf.domochevsky.territorialdealingsold.TerritoryHandler;
import net.wwsf.domochevsky.territorialdealingsold._Territory;

public class FactionOverviewCard extends Item
{

	public FactionOverviewCard()
	{
		this.setMaxStackSize(1);	// Can hold a big stack of these
		this.setFull3D();
		this.setCreativeTab(CreativeTabs.TOOLS);

		this.setRegistryName("overviewcard");
		this.setUnlocalizedName("territorychevsky_overviewcard");
	}


	@Override
	public String getItemStackDisplayName(ItemStack stack) { return "Overview Card"; }


	@Override
	public ActionResult<ItemStack> onItemRightClick(ItemStack stack, World world, EntityPlayer player, EnumHand hand)
	{
		if (world.isRemote) { return new ActionResult(EnumActionResult.PASS, stack); }	// Not doing this on client side

		if (!player.capabilities.isCreativeMode) { return new ActionResult(EnumActionResult.PASS, stack); }	// Creative mode only

		if (player.isSneaking())
		{
			this.toggleChunkProtection(player);
		}
		else
		{
			this.getFactionInfo(player);
		}

		return new ActionResult(EnumActionResult.PASS, stack);
	}


	private void getFactionInfo(EntityPlayer player)
	{
		Main.sendMessageToPlayer(player, "Factions:");

		Iterator<_Territory> it = TerritoryHandler.getFactionIterator();
		_Territory territory;

		if (!it.hasNext())
		{
			Main.sendMessageToPlayer(player, "§7NONE");	// No factions
		}

		while (it.hasNext())
		{
			territory = it.next();

			Main.sendMessageToPlayer(player, "§9" + territory.getFactionName() + " (" + territory.getChunkCount() + " Chunks / " +
					territory.getUpkeepStored() + " Valuables / Protection: " + territory.isUpkeepPaid() + ")");

			Iterator<Entry<UUID, String>> memberIt = territory.getMemberListIterator();
			Entry<UUID, String> entry;

			if (!memberIt.hasNext())	// empty?
			{
				Main.sendMessageToPlayer(player, "§c- NONE (This should not be possible.)");
			}

			while (memberIt.hasNext())	// Listing all members of this faction
			{
				entry = memberIt.next();

				if (Main.isDebugEnabled()) // Bonus info: The player UUID
				{
					Main.sendMessageToPlayer(player, "- " + entry.getValue() + "§f (UUID " + entry.getKey() + ")");
				}
				else
				{
					Main.sendMessageToPlayer(player, "- " + entry.getValue());	// Counting up all members
				}
			}
		}
	}


	private void toggleChunkProtection(EntityPlayer player)
	{
		// Player is already verified to exist and be in creative mode

		BlockPos pos = new BlockPos(player.posX, player.posY, player.posZ);

		Chunk chunk = player.worldObj.getChunkFromBlockCoords(pos);

		_Territory faction = TerritoryHandler.getFactionFromChunk(chunk);

		if (faction == null) { return; }	// Not occupied

		if (faction.isUpkeepPaid())
		{
			faction.setUpkeepPaid(false);
			Main.sendMessageToPlayer(player, "Disabled chunk protection for faction " + faction.getFactionName() + " until next upkeep payment.");
		}
		else
		{
			faction.setUpkeepPaid(true);
			Main.sendMessageToPlayer(player, "Enabled chunk protection for faction " + faction.getFactionName() + " until next upkeep payment.");
		}
	}


	@SideOnly(Side.CLIENT)
	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean par4)
	{
		super.addInformation(stack, player, list, par4);

		list.add("Use to display a list of all");
		list.add("factions and their members.");
		list.add("Crouch-use to toggle chunk protection");
		list.add("of the faction whose chunk you're");
		list.add("standing in right now.");
		list.add("§cOnly usable in creative mode.");
    }
}
