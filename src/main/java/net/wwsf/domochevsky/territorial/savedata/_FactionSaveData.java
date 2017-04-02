package net.wwsf.domochevsky.territorial.savedata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;

import net.minecraft.util.math.ChunkPos;
import net.wwsf.domochevsky.territorial.Main;

public class _FactionSaveData implements Serializable
{
	static final long serialVersionUID = 1L;
	
	private UUID factionID;			// To uniquely identify this faction by. Also used by player data to identify if they're part of this faction.
	private String factionName;		// What is this called?
	private int dimensionID;		// Factions can only exist in single worlds, since claiming new chunks requires adjacency.
	
	private ArrayList<_ChunkSaveData> chunkList;	// The chunks that are part of our territory and their current control strength
	
	private boolean isProtected;	// If this is false then chunks can be freely edited
	private int valuables;			// The treasury of this faction
	
	//private UUID leaderID;				// One leader
	//private ArrayList<UUID> memberIDs;	// Many members
	
	// Factions do not hold info about who is a member
	
	
	public _FactionSaveData(UUID id, int world, String name)
	{
		this.factionID = id;
		this.dimensionID = world;
		this.factionName = name;
		
		// Init		
		this.chunkList = new ArrayList<_ChunkSaveData>();
		this.isProtected = true;
	}
	
	
	public int getDimensionID() { return this.dimensionID; }
	
	
	public boolean isProtected() { return this.isProtected; }
	public void setProtected(boolean set) { this.isProtected = set; }
	
	
	public int getValuables() { return this.valuables; }
	public void setValuables(int set) { this.valuables = set; }
	
	
	public ArrayList<_ChunkSaveData> getChunkList() { return this.chunkList; }
	//public Iterator<Entry<ChunkPos, Integer>> getChunkIterator() { return this.chunks.entrySet().iterator(); }
	
	
	public int getChunkAmount() { return this.chunkList.size(); }


	public UUID getID() { return this.factionID; }
	
	
	public String getName() { return this.factionName; }
	public void setName(String name) { this.factionName = name; }
	

	public void addChunk(ChunkPos chunk) 
	{
		if (this.chunkList.contains(chunk))
		{
			// Already known
		}
		else
		{
			_ChunkSaveData data = new _ChunkSaveData(chunk, Main.getChunkControlMax());
			this.chunkList.add(data);
		}
	}
	
	
	public void removeChunk(ChunkPos pos) 
	{
		_ChunkSaveData data = new _ChunkSaveData(pos, 0);
		
		if (this.chunkList.contains(data))	// Will compare by position
		{
			this.chunkList.remove(data);
		}
		// else, huh?
	}
	
	
	public int getControlStrengthFromChunkPos(ChunkPos pos)
	{
		_ChunkSaveData data = new _ChunkSaveData(pos, 0);
		
		int index = this.chunkList.indexOf(data);
		
		if (index == -1)
		{
			return -1;	// Not a known chunk
			
		}
		else
		{
			return this.chunkList.get(index).strength;	// Here ya go
		}
	}


	public void setControlStrengthForChunkPos(ChunkPos pos, int strength) 
	{
		_ChunkSaveData data = new _ChunkSaveData(pos, 0);
		
		int index = this.chunkList.indexOf(data);
		
		if (index != -1)
		{
			this.chunkList.get(index).strength = strength;	// Overriding what was there before
		}
		// else... imagine me shrugging.
	}


	public boolean isChunkClaimed(ChunkPos pos) 
	{
		_ChunkSaveData data = new _ChunkSaveData(pos, 0);
		
		int index = this.chunkList.indexOf(data);
		
		return (index != -1);	// Is it?
	}
}
