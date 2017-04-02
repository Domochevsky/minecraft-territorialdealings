package net.wwsf.domochevsky.territorialdealingsold;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

public class TerritoryHandler 
{
	private static ArrayList<_Territory> factions = new ArrayList<_Territory>();


	public static ArrayList<_Territory> getFactionsForSaving() { return factions; }
	public static void setFactionsFromLoading(ArrayList<_Territory> fact) { factions = fact; }


	public static void setFactionsFromLoadingOld(_Territory[] oldFactions)
	{
		if (oldFactions == null) { return; }

		int slot = 0;

		while (slot < oldFactions.length && oldFactions[slot] != null)
		{
			if (oldFactions[slot].isFactionValid())
			{
				factions.add(oldFactions[slot]);
			}

			slot += 1;
		}
	}


	// Called when someone puts a block down.
	public static boolean tryToAddFaction(World world, EntityPlayer player, BlockPos pos)
	{
		if (world.isRemote) { return false; }	// Not doing this on client side

		Chunk chunk = world.getChunkFromBlockCoords(pos);	// Depth doesn't matter

		// Can you even start a faction here?
		if (isChunkOccupied(chunk))
		{
			if (player.capabilities.isCreativeMode)	// In creative mode, so taking over leadership
			{
				_Territory faction = getFactionFromChunk(chunk);

				Main.sendMessageToPlayer(player, "Assuming control of faction " + faction.getFactionName() + ".");
				faction.addMember(player);
				faction.setNewLeader(player);	// Careful with this. The rest of these functions are not prepared to account for being the leader of multiple factions
			}
			else
			{
				Main.sendMessageToPlayer(player, "This territory is already occupied by another faction.");
			}
			// else, regular player, so nothing to be done here

			return false;
		}	// Nope, already occupied by someone

		_Territory faction = getFactionPlayerIsMemberOf(player);

		if (faction != null)
		{
			Main.sendMessageToPlayer(player, "You are already a member of " + faction.getFactionName() + ".");
			return false;
		} // Already the leader of something

		addFaction(player, chunk);

		return true;
	}


	// Everything seems to check out, so let's do this
	private static void addFaction(EntityPlayer player, Chunk chunk)
	{
		_Territory territory  = new _Territory(player.worldObj.provider.getDimension(), player.getName() + "'s Group");

		Main.console("[TERRITORY HANDLER] Added a new faction in dimension " + territory.getDimensionID() +
				". Core chunk pos: X " + chunk.xPosition + " / Z " + chunk.zPosition);

		territory.addMember(player);	// Adding them to the faction
		territory.setLeader(player);	// And then declaring them leader

		territory.addChunk(chunk);		// Adding the core chunk first
		territory.setUpkeepPaid(true);	// Starting out paid

		factions.add(territory);	// And making that known

		// SFX
		Main.startFireworks(player);

		Main.sendMessageToPlayer(player, "New faction successfully founded and territory claimed! Faction name: " + territory.getFactionName());

		SaveHandler.saveFactionsToFile();	// Saving, just to make sure
	}


	// Returns true if the passed in chunk is already claimed by someone
	public static boolean isChunkOccupied(Chunk chunk)
	{
		if (chunk == null) { return false; }	// Can't occupy what doesn't exist

		Iterator<_Territory> it = factions.iterator();
		_Territory territory;

		while (it.hasNext())
		{
			territory = it.next();

			if (chunk.getWorld().provider.getDimension() == territory.getDimensionID())
			{
				if (territory.isChunkInTerritory(chunk))
				{
					Main.console("[TERRITORY HANDLER] Chunk at X " + chunk.xPosition + " / Z " + chunk.zPosition + " is occupied by faction " + territory.getFactionName() + ".");
					return true;
				}
			}
			// else, has been removed at some point or is in the wrong dimension. Don't care.
		}

		return false;	// Default
	}


	public static _Territory getFactionFromChunk(Chunk chunk)
	{
		Iterator<_Territory> it = factions.iterator();
		_Territory territory;

		while (it.hasNext())
		{
			territory = it.next();

			if (chunk.getWorld().provider.getDimension() == territory.getDimensionID())
			{
				if (territory.isChunkInTerritory(chunk))
				{
					return territory;
				}
				// else, not in it, so check the next one
			}
			// else, is in the wrong dimension. Don't care.
		}

		return null;	// None
	}


	// Returns true if the player is a faction member of the passed in chunk
	public static boolean canPlayerEditChunk(EntityPlayer player, Chunk chunk)
	{
		// Validation
		if (player == null) { return false; }
		if (chunk == null) { return false; }

		if (player.capabilities.isCreativeMode)
		{
			Main.console("[TERRITORIAL DEALINGS] Player " + player.getName() + " is in creative mode.");
			return true;
		}	// Not getting in their way

		Iterator<_Territory> it = factions.iterator();
		_Territory territory;

		while (it.hasNext())
		{
			territory = it.next();

			if (chunk.getWorld().provider.getDimension() == territory.getDimensionID() && territory.isChunkInTerritory(chunk))
			{
				if (!territory.isUpkeepPaid())
				{
					Main.console("[TERRITORIAL DEALINGS] Territory of " + territory.getFactionName() + " is currently unprotected.");
					return true;
				}	// No protection right now

				if (territory.isPartOfFaction(player))
				{
					Main.console("[TERRITORIAL DEALINGS] Player " + player.getName() + " is a member of territory owner faction. (" + territory.getFactionName() + ")");
					return true;
				}	// You're a member, so that's fine
				else
				{
					Main.console("[TERRITORIAL DEALINGS] Player " + player.getName() + " is not a member of territory owner faction. (" + territory.getFactionName() + ")");
					return false;
				}	// Found the chunk, but you're not a member; so we're done here
			}
			// else, is in the wrong dimension. Don't care.
		}

		Main.console("[TERRITORIAL DEALINGS] No faction seems to have claimed this chunk.");

		return true;	// Default (That chunk isn't occupied by anyone)
	}


	// Returns true if this chunk was successfully added to the player's territory
	public static boolean addChunkToFaction(EntityPlayer player)
	{
		if (player == null) { return false; }

		// First of all, what faction is that player part of?
		_Territory faction = getFactionPlayerIsMemberOf(player);

		if (faction == null) // Not part of anything
		{
			Main.console("[addChunkToFaction] Player (" + player.getName() + ") is not part of any faction. Can't claim territory.");
			Main.sendMessageToPlayer(player, "You are not part of any faction and thus can't claim any territory.");

			return false;
		}
		// else, is part of a faction. That's good. Next step!

		Chunk chunk = player.worldObj.getChunkFromBlockCoords(new BlockPos(player));

		if (faction.isChunkInTerritory(chunk))
		{
			Main.console("[addChunkToFaction] Chunk at X " + chunk.xPosition + " / Z " + chunk.zPosition + " is already part of player's (" + player.getName() + ") faction.");

			return false;	// Can't claim what you already have
		}
		// else, not in your own territory. Does it belong to someone else?

		_Territory enemyFaction = getFactionFromChunk(chunk);

		if (enemyFaction != null)	// Chunk is claimed by someone else
		{
			// Starting takeover process
			Main.console("[addChunkToFaction] Chunk at X " + chunk.xPosition + " / Z " + chunk.zPosition + " is part of another faction. Starting takeover.");

			if (enemyFaction.isMemberOnline(player.worldObj))
			{
				enemyFaction.takeOverChunk(chunk);	// Deducting from that chunk's control strength
				Main.startFireworks(player);		// SFX
			}
			else
			{
				Main.sendMessageToPlayer(player, "No member of that faction is online. Cancelling takeover.");
			}

			return false;
		}

		// Unclaimed. Adding it now

		boolean proceed = false;

		Chunk tempChunk = player.worldObj.getChunkFromChunkCoords(chunk.xPosition + 1, chunk.zPosition);

		if (faction.isChunkInTerritory(tempChunk)) { proceed = true; }	// Is adjacent

		if (!proceed)
		{
			tempChunk = player.worldObj.getChunkFromChunkCoords(chunk.xPosition - 1, chunk.zPosition);

			if (faction.isChunkInTerritory(tempChunk)) { proceed = true; }	// Is adjacent
		}

		if (!proceed)
		{
			tempChunk = player.worldObj.getChunkFromChunkCoords(chunk.xPosition, chunk.zPosition + 1);

			if (faction.isChunkInTerritory(tempChunk)) { proceed = true; }	// Is adjacent
		}

		if (!proceed)
		{
			tempChunk = player.worldObj.getChunkFromChunkCoords(chunk.xPosition, chunk.zPosition - 1);

			if (faction.isChunkInTerritory(tempChunk)) { proceed = true; }	// Is adjacent
		}

		// Did ya find anything?
		if (!proceed)
		{
			Main.console("[addChunkToFaction] Chunk at X " + chunk.xPosition + " / Z " + chunk.zPosition + " is unclaimed, but not adjacent to existing territory of player's (" +
					player.getName() + ") faction.");

			Main.sendMessageToPlayer(player, "This chunk is not adjacent to your faction's territory.");

			return false;
		}

		Main.console("[addChunkToFaction] Chunk at X " + chunk.xPosition + " / Z " + chunk.zPosition + " is unclaimed and adjacent to existing territory at X " +
				tempChunk.xPosition + " / Z " + tempChunk.zPosition + ". Adding it.");

		faction.addChunk(chunk);	// Actually adding it now

		Main.sendMessageToPlayer(player, "Territory successfully claimed in the name of " + faction.getFactionName() + ".");

		// SFX
		Main.startFireworks(player);

		return true;
	}


	public static _Territory getFactionPlayerIsMemberOf(EntityPlayer player)
	{
		if (player == null) { return null; }

		Iterator<_Territory> it = factions.iterator();
		_Territory territory;

		while (it.hasNext())
		{
			territory = it.next();

			if (territory.isPartOfFaction(player))
			{
				return territory;
			}
		}

		return null;	// Fallback
	}


	// Returns the "health" (more "grip") of the passed in chunk, for display purposes
	/*public static int getChunkControlStrength(_Territory faction, Chunk chunk)
	{
		// Validation
		if (faction == null) { return 0; }
		if (chunk == null) { return 0; }

		_ClaimedChunk claim = faction.getChunkClaimFromTerritory(chunk);

		if (claim == null) { return 0; }

		return claim.getControlStrength();	// Checks out
	}*/


	/*public static _Territory getFactionByID(int id)
	{
		if (id < 0 || id >= factions.length) { return null; }

		if (factions[id] != null && factions[id].isInvalid) { return null; }

		return factions[id];
	}*/


	public static _Territory getFactionPlayerIsLeaderOf(EntityPlayer player)
	{
		if (player == null) { return null; }

		Iterator<_Territory> it = factions.iterator();
		_Territory territory;

		while (it.hasNext())
		{
			territory = it.next();

			if (territory.isLeader(player))
			{
				return territory;
			}
			// else, not the leader of this faction
		}

		return null;	// Default. Not the leader of anything
	}


	// Same method as above, but only uses the UUID
	public static _Territory getFactionPlayerIsLeaderOf(UUID leaderUUID)
	{
		if (leaderUUID == null) { return null; }

		Iterator<_Territory> it = factions.iterator();
		_Territory territory;

		while (it.hasNext())
		{
			territory = it.next();

			if (territory.isLeader(leaderUUID))
			{
				return territory;
			}
			// else, not the leader of this faction
		}

		return null;
	}


	// "Damaged" chunks regain X control. Done once a day
	public static void refreshChunkControl()
	{
		Main.console("[TERRITORIAL DEALINGS] Refreshing chunk control of all territories by " + Main.getStrengthRefreshAmount() + ". Max control strength: " + Main.getMaxControlStrength());

		Iterator<_Territory> it = factions.iterator();

		while (it.hasNext())
		{
			it.next().refreshChunkControl();
		}
	}


	// Passing World down so at the end faction members can be informed
	public static void consumeUpkeep(World world)
	{
		Main.console("[TERRITORIAL DEALINGS] Consuming upkeep for all claimed territories now.");

		Iterator<_Territory> it = factions.iterator();

		while (it.hasNext())
		{
			it.next().consumeUpkeep(world);
		}
	}


	public static void removeTerritory(_Territory territory)
	{
		if (territory == null) { return; }

		factions.remove(territory);	// Done.
	}


	public static Iterator<_Territory> getFactionIterator() { return factions.iterator(); }
}
