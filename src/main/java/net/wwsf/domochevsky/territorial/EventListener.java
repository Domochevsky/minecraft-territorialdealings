package net.wwsf.domochevsky.territorial;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;
import net.minecraftforge.event.world.BlockEvent.PlaceEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;
import net.minecraftforge.fml.relauncher.Side;

public class EventListener 
{
	@SubscribeEvent
	public void onBlockPlaceEvent(PlaceEvent event)
	{
		if (event.getWorld().isRemote) { return; }	// Not doing this on client side
		
		Main.console("Block at x" + event.getPos().getX() + " y" + event.getPos().getY() + " z" + event.getPos().getZ() + " placed by player " + event.getPlayer().getName() + 
				" (ID " + event.getPlayer().getGameProfile().getId() + ").");
		
		Chunk chunk = event.getPlayer().worldObj.getChunkFromBlockCoords(event.getPos());

		if (TerritoryHandler.canPlayerEditChunk(event.getPlayer().worldObj, event.getPlayer(), chunk))	// Checks out
		{
			Main.console("Player is allowed to edit this chunk. Doing nothing.");
		}
		else
		{
			Main.console("Player is not allowed to edit this chunk. Cancelling.");
			event.setCanceled(true);	// Not having it
		}
	}
	
	
	@SubscribeEvent
	public void onBlockBreakEvent(BreakEvent event)
	{
		if (event.getWorld().isRemote) { return; }	// Not doing this on client side

		Main.console("Block at x" + event.getPos().getX() + " y" + event.getPos().getY() + " z" + event.getPos().getZ() + " broken by player " + event.getPlayer().getName() + 
				" (ID " + event.getPlayer().getGameProfile().getId() + ").");

		Chunk chunk = event.getPlayer().worldObj.getChunkFromBlockCoords(event.getPos());

		if (TerritoryHandler.canPlayerEditChunk(event.getPlayer().worldObj, event.getPlayer(), chunk))	// Checks out
		{
			Main.console("Player is allowed to edit this chunk. Doing nothing.");
		}
		else
		{
			Main.console("Player is not allowed to edit this chunk. Cancelling.");
			event.setCanceled(true);	// Not having it
		}
	}
	
	
	@SubscribeEvent
	public void onBucketUse(FillBucketEvent event)
	{
		if (event.getWorld().isRemote) { return; }	// Not doing this on client side

		RayTraceResult target = event.getTarget();

		if (target == null) { return; }	// Nevermind?

		BlockPos pos = target.getBlockPos();

		if (pos == null) { return; }	// Nevermind?

		Main.console("Bucket used by player " + event.getEntityPlayer().getName() + " (ID " + event.getEntityPlayer().getGameProfile().getId() + ") at x" + pos.getX() + " y" + pos.getY() + " z" + pos.getZ() + ".");

		Chunk chunk = event.getEntityPlayer().worldObj.getChunkFromBlockCoords(pos);

		if (TerritoryHandler.canPlayerEditChunk(event.getEntityPlayer().worldObj, event.getEntityPlayer(), chunk))	// Checks out
		{
			Main.console("Player is allowed to edit this chunk. Doing nothing.");
		}
		else
		{
			Main.console("Player is not allowed to edit this chunk. Cancelling.");
			event.setCanceled(true);	// Not having it
		}
	}
	
	
	@SubscribeEvent
	public void onRightClickBlock(RightClickBlock event)
	{
		if (event.getWorld().isRemote) { return; }		// Not doing this on client side
		if (!Main.shouldCheckInteraction()) { return; }	// Not my business

		Main.console("Block at x" + event.getPos().getX() + " y" + event.getPos().getY() + " z" + event.getPos().getZ() + " touched by player " + event.getEntityPlayer().getName() + 
				" (ID " + event.getEntityPlayer().getGameProfile().getId() + ").");

		Chunk chunk = event.getEntityPlayer().worldObj.getChunkFromBlockCoords(event.getPos());

		if (TerritoryHandler.canPlayerEditChunk(event.getEntityPlayer().worldObj, event.getEntityPlayer(), chunk))	// Checks out
		{
			Main.console("Player is allowed to interact with this chunk. Doing nothing.");
		}
		else
		{
			Main.console("Player is not allowed to interact with this chunk. Cancelling.");
			event.setCanceled(true);	// Not having it
		}
	}
	
	
	@SubscribeEvent
	public void onLeftClickBlock(LeftClickBlock event)
	{
		if (event.getWorld().isRemote) { return; }		// Not doing this on client side
		if (!Main.shouldCheckInteraction()) { return; }	// Not my business

		Main.console("Block at x" + event.getPos().getX() + " y" + event.getPos().getY() + " z" + event.getPos().getZ() + " hit by player " + event.getEntityPlayer().getName() + 
				" (ID " + event.getEntityPlayer().getGameProfile().getId() + ").");

		Chunk chunk = event.getEntityPlayer().worldObj.getChunkFromBlockCoords(event.getPos());

		if (TerritoryHandler.canPlayerEditChunk(event.getEntityPlayer().worldObj, event.getEntityPlayer(), chunk))	// Checks out
		{
			Main.console("Player is allowed to interact with this chunk. Doing nothing.");
		}
		else
		{
			Main.console("Player is not allowed to interact with this chunk. Cancelling.");
			event.setCanceled(true);	// Not having it
		}
	}
	
	
	@SubscribeEvent
	public void onServerTick(ServerTickEvent event)
	{
		if (event.phase != Phase.END) { return; }	// Only doing it once
		if (event.side == Side.CLIENT) { return; }
		
		// Chunk upkeep
		if (Main.shouldCheckUpkeep())
		{
			TerritoryHandler.checkUpkeep();
		}
		
		// Chunk control regeneration
		TerritoryHandler.checkChunkControl();
		
		// Autosave
		Main.checkAutoSave();
		
		Main.checkDisplayTick();
	}
}
