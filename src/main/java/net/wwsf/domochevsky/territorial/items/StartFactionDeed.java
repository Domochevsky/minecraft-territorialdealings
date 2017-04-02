package net.wwsf.domochevsky.territorial.items;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.wwsf.domochevsky.territorial.ChatFormatting;
import net.wwsf.domochevsky.territorial.Main;
import net.wwsf.domochevsky.territorial.TerritoryHandler;
import net.wwsf.domochevsky.territorial.savedata._FactionSaveData;

public class StartFactionDeed extends _ItemBase
{
	public StartFactionDeed()
	{
		this.setMaxStackSize(1);
		this.setFull3D();
		this.setCreativeTab(CreativeTabs.TOOLS);

		this.registerWithName("startfactiondeed");
	}
	
	
	@Override
	public String getItemStackDisplayName(ItemStack stack) { return "New Faction Deed"; }
	
	
	@SideOnly(Side.CLIENT)
	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean par4)
	{
		super.addInformation(stack, player, list, par4);

		list.add("'The start of something great.'");
		list.add("Use this to create a new faction");
		list.add("in your current chunk.");
		list.add(ChatFormatting.YELLOW + "Each claimed chunk increases");
		list.add(ChatFormatting.YELLOW + "territory upkeep by 1.");
		list.add(ChatFormatting.RED + "Used up on successful creation.");
    }
	
	
	@Override
	public ActionResult<ItemStack> onItemRightClick(ItemStack stack, World world, EntityPlayer player, EnumHand hand)
	{
		if (world.isRemote) { return new ActionResult(EnumActionResult.PASS, stack); }	// Not doing this on client side

		BlockPos pos = new BlockPos(player.posX, player.posY, player.posZ);
		Chunk chunk = world.getChunkFromBlockCoords(pos);
		
		// Step 1, does someone already own this chunk?
		_FactionSaveData faction = TerritoryHandler.getFactionByChunkPos(world.provider.getDimension(), chunk.getChunkCoordIntPair());
		
		if (faction != null)
		{
			// They do, so not letting this happen.
			Main.sendMessageToPlayer(player, "Faction '" + faction.getName() + "' already claimed this chunk.");
			
			return new ActionResult(EnumActionResult.FAIL, stack);
		}
		
		// Step 1.1, are you already the leader of a faction?
		if (TerritoryHandler.isPlayerLeader(player))
		{
			Main.sendMessageToPlayer(player, "You are already the leader of a faction.");
			
			return new ActionResult(EnumActionResult.FAIL, stack);
		}
		
		// Step 2, marking a new claim
		faction = TerritoryHandler.startFactionInChunk(world, player, chunk);

		// Step 3, consuming the item
		stack.stackSize = 0;
		
		// Step 4, confirmation
		Main.sendMessageToPlayer(player, "You started a new faction! It's called'" + faction.getName() + "'.");
		Main.sendMessageToPlayer(player, "You can rename it with a Faction Name Order and an anvil.");
		
		// SFX
		Main.startFireworks(player);
		Main.startFireworks(player);

		return new ActionResult(EnumActionResult.PASS, stack);
	}
}
