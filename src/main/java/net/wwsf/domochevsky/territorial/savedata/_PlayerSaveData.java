package net.wwsf.domochevsky.territorial.savedata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;

public class _PlayerSaveData implements Serializable 
{
	static final long serialVersionUID = 1L;
	
	private UUID playerID;						// The player we are attached to
	
	private ArrayList<UUID> factionMemberships;	// The factions we are members of
	private UUID factionLeadership;				// The faction we are the leader of. Can only be one.
	
	
	public _PlayerSaveData(UUID id)
	{
		this.playerID = id;
		// Init
		this.factionMemberships = new ArrayList<UUID>();
	}


	public ArrayList<UUID> getMemberList() 
	{
		return this.factionMemberships;
	}
	
	
	public void setMemberList(ArrayList<UUID> list)
	{
		this.factionMemberships = list;
	}


	public UUID getLeadership() 
	{
		return this.factionLeadership;
	}
	
	
	public void setLeadership(UUID factionID)
	{
		this.factionLeadership = factionID;
	}
}
