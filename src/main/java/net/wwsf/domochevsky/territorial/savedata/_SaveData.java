package net.wwsf.domochevsky.territorial.savedata;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.UUID;

public class _SaveData implements Serializable
{
	// This is what gets saved to file
	private HashMap<UUID, _PlayerSaveData> playerMap = new HashMap<UUID, _PlayerSaveData>();
	private HashMap<UUID, _FactionSaveData> factionMap = new HashMap<UUID, _FactionSaveData>();
	
	private int currentSaveTick;	// Keeping track across server restarts how long we've gone without saving
	private int currentUpkeepTick;	// Save for refreshing territory control
	private int currentControlTick;	// Save for repairing chunk control
	
	private int currentDisplayTick;	// Items get put down and back up whenever their nbt data changes, making them unsuitable for storing cooldowns.
									// So the claim order display tick gets put here instead, for a global cooldown. Responds to server ticks instead.
	
	public HashMap<UUID, _PlayerSaveData> getPlayerMap() { return this.playerMap; }
	public HashMap<UUID, _FactionSaveData> getFactionMap() { return this.factionMap; }
	
	
	public Iterator<Entry<UUID, _FactionSaveData>> getFactionMapIterator()
	{
		return this.factionMap.entrySet().iterator();
	}
	
	
	public Iterator<Entry<UUID, _PlayerSaveData>> getPlayerMapIterator() 
	{
		return this.playerMap.entrySet().iterator();
	}


	public int getUpkeepTick() { return this.currentUpkeepTick; }
	public void setUpkeepTick(int set) { this.currentUpkeepTick = set; }
	
	
	public int getControlTick() { return this.currentControlTick; }
	public void setControlTick(int set) { this.currentControlTick = set; }
	
	
	public int getSaveTick() { return this.currentSaveTick; }
	public void setSaveTick(int set) { this.currentSaveTick = set; }
	
	
	public int getDisplayTick() { return this.currentDisplayTick; }
	public void setDisplayTick(int set) { this.currentDisplayTick = set; }
	public boolean isDisplayReady() { return (this.currentDisplayTick <= 0); }
}
