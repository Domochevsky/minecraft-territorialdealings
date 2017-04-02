package net.wwsf.domochevsky.territorial;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.wwsf.domochevsky.territorial.savedata._ChunkSaveData;
import net.wwsf.domochevsky.territorial.savedata._FactionSaveData;
import net.wwsf.domochevsky.territorial.savedata._PlayerSaveData;
import net.wwsf.domochevsky.territorial.savedata._SaveData;

public class TerritoryHandler 
{
	private static _SaveData saveData;	// Save data in one single point, ready to be stored to disk
	
	
	// Pretty much only used for saving and loading
	public static _SaveData getSaveData() { return saveData; }
	static void setSaveData(_SaveData set) { saveData = set; }
	

	static boolean canPlayerEditChunk(World world, EntityPlayer player, Chunk chunk) 
	{
		// Safeties
		if (player == null) { return false; }	// Who're you?
		if (world == null) { return false; }	// Where are you?
		if (chunk == null) { return false; }	// What're you trying to do?
		
		// Step 1, is the chunk claimed by any particular faction?
		ChunkPos chunkPos = chunk.getChunkCoordIntPair();
		
		_FactionSaveData faction = getFactionByChunkPos(world.provider.getDimension(), chunkPos);
		
		if (faction == null)
		{
			// Isn't claimed by any faction, so no contest there.
			return true;
		}
		
		if (!faction.isProtected())
		{
			// Isn't currently protected
			return true;
		}
		
		// Step 2, is the player is a member of that faction?
		return isPlayerMemberOfFaction(faction.getID(), player, player.capabilities.isCreativeMode);	// If you are you get to edit the chunk.
	}

	
	public static _FactionSaveData getFactionByChunkPos(int worldID, ChunkPos pos)
	{
		//Main.console("Getting faction by chunk position...");
		
		Iterator<Entry<UUID, _FactionSaveData>> it = saveData.getFactionMapIterator();
		Entry<UUID, _FactionSaveData> entry;
		
		while (it.hasNext())
		{
			entry = it.next();
			
			//Main.console("...checking faction " + entry.getKey() + "...");
			
			if (entry.getValue().getDimensionID() != worldID) { continue; }		// Wrong dimension
			
			// Is that chunk pos in your list?
			if (entry.getValue().isChunkClaimed(pos))
			{
				return entry.getValue();	// Found it!
			}
		}
		
		return null; 	// Didn't find anything.
	}
	
	
	// I'm assuming that leaders are also members by default.
	static boolean isPlayerMemberOfFaction(UUID factionID, EntityPlayer player, boolean isCreative)
	{
		UUID playerID = getIDFromPlayer(player);
		
		if (isCreative) { return true; }	// Creative mode players are members of ALL factions. Leaders, too.
		
		if (saveData.getPlayerMap().containsKey(playerID))
		{
			// Is a known player, so checking their data
			_PlayerSaveData playerData = saveData.getPlayerMap().get(playerID);
			
			if (playerData.getMemberList().contains(factionID))
			{
				// They're a member of that faction, yay.
				return true;
			}
			else
			{
				// Not a member, so not allowed to edit.
				return false;
			}
		}
		else
		{
			// Is not a known player, so adding them now.
			_PlayerSaveData playerData = new _PlayerSaveData(playerID);
			
			saveData.getPlayerMap().put(playerID, playerData);
			
			// Cannot be a member of faction yet, since they're new.			
			return false;
		}
	}


	// Trusts that you already verified that the chunk is not claimed by anyone.
	public static _FactionSaveData startFactionInChunk(World world, EntityPlayer player, Chunk chunk) 
	{
		// Safeties
		if (player == null) { return null; }	// Who're you?
		if (world == null) { return null; }		// Where are you?
		if (chunk == null) { return null; }		// What're you trying to do?
		
		UUID playerID = getIDFromPlayer(player);
		
		Main.console("Starting a new faction for player " + player.getName() + " (ID " + playerID + ")...");
		
		// Step 1, create the faction
		_FactionSaveData faction = new _FactionSaveData(UUID.randomUUID(), world.provider.getDimension(), player.getName() + "'s faction");
		
		// Step 1.1, claiming the first chunk
		faction.addChunk(chunk.getChunkCoordIntPair());
		
		// Step 2, add the player both as member and leader
		_PlayerSaveData playerData;
		
		if (saveData.getPlayerMap().containsKey(playerID))
		{
			// Is already known, so we can use their profile
			Main.console("...player is known, using their profile...");
			
			playerData = saveData.getPlayerMap().get(playerID);
		}
		else
		{
			// Is not yet known, so adding them now
			Main.console("...player is not known, making a new profile...");
			
			playerData = new _PlayerSaveData(playerID);
			saveData.getPlayerMap().put(playerID, playerData);
		}
		
		Main.console("...adding player as member and leader...");

		ArrayList<UUID> memberships = playerData.getMemberList();	// Get...
		memberships.add(faction.getID());							// ..set...
		playerData.setMemberList(memberships);						// ...return
		
		playerData.setLeadership(faction.getID());					// You're the leader now.
		
		
		// Step 3, make the faction known
		saveData.getFactionMap().put(faction.getID(), faction);
		
		Main.console("...done.");
		
		// Step 4, handing the new faction back
		return faction;
	}
	
	
	public static void checkUpkeep()
	{
		int currentTick = saveData.getUpkeepTick();
		int maxTick = Main.getUpkeepTickMax();
		
		if (currentTick < maxTick)
		{
			// Not yet
			saveData.setUpkeepTick(currentTick + 1);
			
			return;
		}
		else
		{
			// Reset
			saveData.setUpkeepTick(0);
		}
		
		int upkeep = Main.getUpkeepCost();
		
		// Alright, let's do this.
		Main.console("Checking upkeep for all controlled chunks. Cost per chunk: " + upkeep + " VALUABLES ...");
		
		// Faction by faction
		Iterator<Entry<UUID, _FactionSaveData>> facIt = saveData.getFactionMapIterator();
		Entry<UUID, _FactionSaveData> facEntry;
		
		while (facIt.hasNext())
		{
			facEntry = facIt.next();
			
			Main.console("...checking faction " + facEntry.getKey() + "...");
			
			int valuables = facEntry.getValue().getValuables();
			int cost = 0;
			
			if (valuables <= 0)
			{
				// Cannot possibly pay for anything
			}
			
			// How much is this gonna cost ya...
			cost = upkeep * facEntry.getValue().getChunkAmount();
			
			if (cost > valuables)
			{
				// Cannot pay, so protection is lost. Not deducting remaining valuables.
				facEntry.getValue().setProtected(false);
				
				Main.console("...faction cannot pay upkeep. Disabling protection...");
			}
			else
			{
				// Seems affordable
				facEntry.getValue().setValuables(valuables - cost);
				facEntry.getValue().setProtected(true);
				
				Main.console("...faction successfully paid " + cost + " in upkeep. Enabling protection...");
			}			
		}
		
		Main.console("...done checking upkeep.");
	}
	
	
	public static boolean isPlayerLeader(EntityPlayer player) 
	{
		if (player == null) { return false; }
		
		UUID playerID = getIDFromPlayer(player);
		
		if (saveData.getPlayerMap().containsKey(playerID))
		{
			// Are you the leader of something?
			return (saveData.getPlayerMap().get(playerID).getLeadership() != null);
		}
		else
		{
			// Not a known player, so cannot be a leader of anything
			return false;
		}
	}
	
	
	public static String getNameOfFaction(UUID factionID) 
	{
		if (saveData.getFactionMap().containsKey(factionID))
		{
			// Here ya go
			return saveData.getFactionMap().get(factionID).getName();
		}
		else
		{
			// No idea what you want
			return null;
		}
	}
	
	
	private static UUID getIDFromPlayer(EntityPlayer player)
	{
		if (player == null)
		{
			return null;
		}
		
		return player.getGameProfile().getId();
	}
	
	
	public static _FactionSaveData getFactionFromLeader(EntityPlayer player) 
	{
		if (player == null) { return null; }
		
		UUID playerID = getIDFromPlayer(player);
		UUID factionID = saveData.getPlayerMap().get(playerID).getLeadership();	// Who're you the leader of?
		
		if (factionID == null)
		{
			// No leader of anything
			return null;
		}
		
		if (saveData.getFactionMap().containsKey(factionID))
		{
			// Here y ago
			return saveData.getFactionMap().get(factionID);
		}
		else
		{
			// Leader of a non-existent faction?
			return null;
		}
	}
	
	
	public static void claimChunkForFaction(Chunk chunk, UUID factionID) 
	{
		if (chunk == null) { return; }
		if (factionID == null) { return; }
		
		if (saveData.getFactionMap().containsKey(factionID))
		{
			// Exists, so adding it.
			saveData.getFactionMap().get(factionID).addChunk(chunk.getChunkCoordIntPair());
		}
		else
		{
			// ...that faction doesn't exist. What.
		}
	}
	
	
	public static boolean isChunkOwnedByFaction(UUID factionID, Chunk chunk)
	{
		if (factionID == null) { return false; }
		if (chunk == null) { return false; }
		
		_FactionSaveData faction = saveData.getFactionMap().get(factionID);
		
		if (faction == null) { return false; }	// No faction with that ID exists.
		
		return faction.isChunkClaimed(chunk.getChunkCoordIntPair());	// Does it?
	}
	
	
	public static boolean isChunkAdjacentToTerritory(World world, UUID factionID, Chunk chunk) 
	{
		if (factionID == null) { return false; }
		if (chunk == null) { return false; }
		
		_FactionSaveData faction = saveData.getFactionMap().get(factionID);
		
		if (faction == null) { return false; }	// No faction with that ID exists.
		
		// Once round
		Chunk tempChunk = world.getChunkFromChunkCoords(chunk.xPosition + 1, chunk.zPosition);
		if (tempChunk != null && faction.isChunkClaimed(tempChunk.getChunkCoordIntPair())) { return true; }	// Checks out.
		
		tempChunk = world.getChunkFromChunkCoords(chunk.xPosition - 1, chunk.zPosition);
		if (tempChunk != null && faction.isChunkClaimed(tempChunk.getChunkCoordIntPair())) { return true; }	// Checks out.
		
		tempChunk = world.getChunkFromChunkCoords(chunk.xPosition, chunk.zPosition + 1);
		if (tempChunk != null && faction.isChunkClaimed(tempChunk.getChunkCoordIntPair())) { return true; }	// Checks out.
		
		tempChunk = world.getChunkFromChunkCoords(chunk.xPosition, chunk.zPosition - 1);
		if (tempChunk != null && faction.isChunkClaimed(tempChunk.getChunkCoordIntPair())) { return true; }	// Checks out.
		
		return false;
	}
	
	
	public static void checkChunkControl() 
	{
		int currentTick = saveData.getControlTick();
		int maxTick = Main.getChunkTickMax();
		
		if (currentTick < maxTick)
		{
			// Not yet
			saveData.setControlTick(currentTick + 1);
			
			return;
		}
		else
		{
			// Reset
			saveData.setControlTick(0);
		}
		
		Main.console("Refreshing chunk control strength by " + Main.getChunkControlRefreshValue() + " now...");
		
		Iterator<Entry<UUID, _FactionSaveData>> it = saveData.getFactionMapIterator();
		Entry<UUID, _FactionSaveData> entry;
		
		while (it.hasNext())
		{
			entry = it.next();
			
			Main.console("...checking faction " + entry.getValue().getName()  + " (ID " + entry.getKey() + ")...");
			
			Iterator<_ChunkSaveData> chunkIt = entry.getValue().getChunkList().iterator();
			_ChunkSaveData chunk;
			
			while (chunkIt.hasNext())
			{
				chunk = chunkIt.next();
				
				if (chunk.strength < Main.getChunkControlMax())
				{
					chunk.strength += Main.getChunkControlRefreshValue();
					
					if (chunk.strength > Main.getChunkControlMax())
					{
						// Making sure we don't spill over
						chunk.strength = Main.getChunkControlMax();
					}
				}
				// else, is at max strength. Nothing to be done here.
			}
		}
		
		Main.console("... done refreshing chunk control.");
	}
	
	
	// See if enough players of that faction are online to be able to attack it.
	public static boolean canAttackFaction(World world, _FactionSaveData faction) 
	{
		if (faction == null) { return false; }	// ...who're you trying to attack?
		
		// Step 1, can factions be attacked at all?
		if (!Main.allowAttacks()) { return false; }
		
		// Step 2, do we care whether or not anyone is online?
		if (!Main.doesPlayerNeedToBeOnline()) { return true; }	// No requirement, so have at it.
		
		// Step 3, how many need to be online and do we have that many, percentage-wise?
		ArrayList<UUID> factionMembers = getMembersFromFaction(faction.getID());	// We got this many faction members...
		
		int membersMax = factionMembers.size();					// Regular count
		
		int membersOnline = 0;									// In percent
		int membersRequired = Main.getRequiredPlayerAmount();	// In percent
		
		// ...now how many of them are online?
		Iterator<EntityPlayer> it = world.playerEntities.iterator();
		
		while (it.hasNext())
		{
			EntityPlayer player = it.next();
			
			if (isPlayerMemberOfFaction(faction.getID(), player, false))
			{
				// How many percent does each member account for?
				membersOnline += (100 / membersMax);
				
				if (membersOnline >= membersRequired)
				{
					return true;	// That's enough
				}
			}
		}
		
		return false;	// Not enough, hm?
	}
	
	
	private static ArrayList<UUID> getMembersFromFaction(UUID factionID) 
	{
		ArrayList<UUID> list = new ArrayList<UUID>();
		
		Iterator<Entry<UUID, _PlayerSaveData>> it = saveData.getPlayerMapIterator();
		Entry<UUID, _PlayerSaveData> entry;
		
		while (it.hasNext())
		{
			entry = it.next();
			
			if (entry.getValue().getMemberList().contains(factionID))
			{
				// You are a member!
				list.add(entry.getKey());
			}
		}
		
		return list;
	}
	
	
	public static void disbandFaction(World world, UUID factionID, Entity dropEntity) 
	{
		if (world == null) { return; }
		if (factionID == null) { return; }
		
		_FactionSaveData faction = saveData.getFactionMap().get(factionID);
		
		if (faction == null) 
		{ 
			// Not a known faction, hm?
			Main.console("Warning: Someone tried to disband a faction that doesn't seem to exist. ID: " + factionID);
			return; 
		}
		
		// Disband this faction, kick all members out, tell them about it and scatter their valuables at the feet of the entity specified
		Main.console("Disbanding faction " + faction.getName() + " (ID " + factionID + ")...");
		
		// Step 1, kick all members out and tell them about it
		Main.sendMessageToFactionMembers(world, factionID, "Disbanding faction " + faction.getName() + "!");
		
		Iterator<Entry<UUID, _PlayerSaveData>> playerIt = saveData.getPlayerMapIterator();
		Entry<UUID, _PlayerSaveData> playerData;
		
		while (playerIt.hasNext())
		{
			playerData = playerIt.next();
			
			if (factionID.equals(playerData.getValue().getLeadership()))
			{
				// Oh hello, leader.
				Main.console("...removing leader " + playerData.getKey() + "...");
				
				playerData.getValue().setLeadership(null);
			}
			
			if (playerData.getValue().getMemberList().contains(factionID))
			{
				// Hello, member.
				Main.console("...removing member " + playerData.getKey() + "...");
				playerData.getValue().getMemberList().remove(factionID);
			}
		}
		
		Main.console("...removing the faction itself...");
		
		// Step 2, begone with the faction
		saveData.getFactionMap().remove(factionID);
		
		// Step 3, scattering valuables
		if (faction.getValuables() <= 0)
		{
			Main.console("...no VALUABLES to scatter. Done removing faction.");
			return;
		}
		
		Main.console("...scattering " + faction.getValuables() + " VALUABLES...");
		
		if (dropEntity == null)
		{
			// We have no entity to drop this at. Now what?
			// TODO
		}
		else
		{
			// We have an entity. Dropping a new (set of) payment order(s) at their feet
			// TODO
		}
		
		Main.console("...done disbanding faction.");
	}
}
