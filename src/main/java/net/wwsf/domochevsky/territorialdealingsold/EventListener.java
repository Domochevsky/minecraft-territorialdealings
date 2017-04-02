package net.wwsf.domochevsky.territorialdealingsold;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;
import net.minecraftforge.event.world.BlockEvent.PlaceEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.wwsf.domochevsky.territorialdealingsold.item.LeaderRequiredItem;

public class EventListener 
{
	@SubscribeEvent
	public void onBlockPlaceEvent(PlaceEvent event)
	{
		if (event.getWorld().isRemote) { return; }	// Not doing this on client side

		Main.console("[TERRITORIAL DEALINGS] Player " + event.getPlayer().getName() + " placed a block at pos x " + event.getPos().getX() + " y " + event.getPos().getY() + " z " + event.getPos().getZ() + ".");

		Chunk chunk = event.getPlayer().worldObj.getChunkFromBlockCoords(event.getPos());

		if (TerritoryHandler.canPlayerEditChunk(event.getPlayer(), chunk))	// Checks out
		{
			Main.console("[TERRITORIAL DEALINGS] Player is allowed to edit this chunk. Doing nothing.");
		}
		else
		{
			Main.console("[TERRITORIAL DEALINGS] Player is not allowed to edit this chunk. Cancelling.");
			event.setCanceled(true);	// Not having it
		}
	}


	@SubscribeEvent
	public void onBlockBreakEvent(BreakEvent event)
	{
		if (event.getWorld().isRemote) { return; }

		Main.console("[TERRITORIAL DEALINGS] Block at x " + event.getPos().getX() + " / y " + event.getPos().getY() + " / z " + event.getPos().getZ() + " broken by player " + event.getPlayer().getName() + ".");

		Chunk chunk = event.getPlayer().worldObj.getChunkFromBlockCoords(event.getPos());

		if (TerritoryHandler.canPlayerEditChunk(event.getPlayer(), chunk))	// Checks out
		{
			Main.console("[TERRITORIAL DEALINGS] Player is allowed to edit this chunk. Doing nothing.");
		}
		else
		{
			Main.console("[TERRITORIAL DEALINGS] Player is not allowed to edit this chunk. Cancelling.");
			event.setCanceled(true);	// Not having it
		}
	}


	@SubscribeEvent
	public void onBucketUse(FillBucketEvent event)
	{
		if (event.getWorld().isRemote) { return; }

		RayTraceResult target = event.getTarget();

		if (target == null) { return; }	// Nevermind?

		BlockPos pos = target.getBlockPos();

		if (pos == null) { return; }	// Nevermind?

		Main.console("[TERRITORIAL DEALINGS] Bucket used by player " + event.getEntityPlayer().getName() + " at x " + pos.getX() + " / y " + pos.getY() + " / z " + pos.getZ() + ".");

		Chunk chunk = event.getEntityPlayer().worldObj.getChunkFromBlockCoords(pos);

		if (TerritoryHandler.canPlayerEditChunk(event.getEntityPlayer(), chunk))	// Checks out
		{
			Main.console("[TERRITORIAL DEALINGS] Player is allowed to edit this chunk. Doing nothing.");
		}
		else
		{
			Main.console("[TERRITORIAL DEALINGS] Player is not allowed to edit this chunk. Cancelling.");
			event.setCanceled(true);	// Not having it
		}
	}

	@SubscribeEvent
	public void onRightClickBlock(RightClickBlock event)
	{
		if (event.getWorld().isRemote) { return; }
		if (Main.isPlayerInteractionAllowed()) { return; }	// Not my business

		Main.console("[TERRITORIAL DEALINGS] Block at x " + event.getPos().getX() + " / y " + event.getPos().getY() + " / z " + event.getPos().getZ() + " touched by player " + event.getEntityPlayer().getName() + ".");

		Chunk chunk = event.getEntityPlayer().worldObj.getChunkFromBlockCoords(event.getPos());

		if (TerritoryHandler.canPlayerEditChunk(event.getEntityPlayer(), chunk))	// Checks out
		{
			Main.console("[TERRITORIAL DEALINGS] Player is allowed to interact with this chunk. Doing nothing.");
		}
		else
		{
			Main.console("[TERRITORIAL DEALINGS] Player is not allowed to interact with this chunk. Cancelling.");
			event.setCanceled(true);	// Not having it
		}
	}


	@SubscribeEvent
	public void onLeftClickBlock(LeftClickBlock event)
	{
		if (event.getWorld().isRemote) { return; }
		if (Main.isPlayerInteractionAllowed()) { return; }	// Not my business

		Main.console("[TERRITORIAL DEALINGS] Block at x " + event.getPos().getX() + " / y " + event.getPos().getY() + " / z " + event.getPos().getZ() + " hit by player " + event.getEntityPlayer().getName() + ".");

		Chunk chunk = event.getEntityPlayer().worldObj.getChunkFromBlockCoords(event.getPos());

		if (TerritoryHandler.canPlayerEditChunk(event.getEntityPlayer(), chunk))	// Checks out
		{
			Main.console("[TERRITORIAL DEALINGS] Player is allowed to interact with this chunk. Doing nothing.");
		}
		else
		{
			Main.console("[TERRITORIAL DEALINGS] Player is not allowed to interact with this chunk. Cancelling.");
			event.setCanceled(true);	// Not having it
		}
	}


	@SubscribeEvent
	public void onItemCrafted(ItemCraftedEvent event)
	{
		if (event.player.worldObj.isRemote) { return; }	// Server side only

		if (event.crafting.getItem() instanceof LeaderRequiredItem)	// Only leaders can craft this item
		{
			if (TerritoryHandler.getFactionPlayerIsLeaderOf(event.player) == null)
			{
				Main.console("Item is leader-only, but the player is not a leader. Removing the item.");
				event.crafting.stackSize = 0;	// Not having it

				//event.setCanceled(true);	// That event cannot be cancelled. How do I reliably stop the creation of this item? Setting the stack size itself seems to be delayed until the metadata changes,
											// or some time passes and a sync is started
				//TODO: Refund the items used?
			}
			// else, is leader of at least one faction. Checks out.
		}
	}


	public static int upkeepTick = 0;


	@SubscribeEvent
	public void onWorldTick(WorldTickEvent event)
	{
		if (event.phase != Phase.END) { return; }	// Only doing it once
		if (event.side == Side.CLIENT) { return; }

		upkeepTick += 1;

		if (upkeepTick >= Main.getConsumeUpkeepTick())
		{
			TerritoryHandler.consumeUpkeep(event.world);
			upkeepTick = 0;	// Reset
		}
	}


	public static int dayTick = 0;
	public static int saveTick = 0;


	@SubscribeEvent
	public void onServerTick(ServerTickEvent event)
	{
		if (event.phase != Phase.END) { return; }	// Only doing it once
		if (event.side == Side.CLIENT) { return; }

		dayTick += 1;	// Ticking up

		if (dayTick >= Main.getControlRefreshTick())	// A day has passed. Refreshing faction chunk health now
		{
			TerritoryHandler.refreshChunkControl();
			dayTick = 0;	// Reset
		}

		saveTick += 1;

		if (saveTick >= Main.getSaveTick())	// Autosave
		{
			SaveHandler.saveFactionsToFile();
			saveTick = 0;	// Reset
		}
	}
}
