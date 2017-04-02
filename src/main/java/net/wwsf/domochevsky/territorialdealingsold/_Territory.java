package net.wwsf.domochevsky.territorialdealingsold;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Random;
import java.util.UUID;

import net.minecraft.block.material.MapColor;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

public class _Territory implements Serializable
{
	private static final long serialVersionUID = 1L;

	private UUID leaderUUID;
	private HashMap<UUID, String> members = new HashMap<UUID, String>();	// Mapping member names to their UUIDs and remembering the fact that they are members

	private int memberCount = 0;

	private int dimensionID;

	private String factionName;

	private ArrayList<_ClaimedChunk> chunks = new ArrayList<_ClaimedChunk>();
	private int chunkCount = 0;	// How many chunks are part of this territory

	private boolean upkeepPaid;
	private int upkeepStored;		// Keeping track of how much we have in the bank and deduct from that regularly

	private byte mapColor = -1;


	// ---
	private double returnX;
	private double returnY;
	private double returnZ;
	private int returnDim;


	public _Territory(int dimID, String name)
	{
		this.dimensionID = dimID;
		this.factionName = name;

		Random rand = new Random();
		this.mapColor = (byte) rand.nextInt(16);	// 0 to 15, applying a random color on creation
	}


	public boolean isFactionValid()
	{
		if (this.leaderUUID == null) { return false; }	// Has no leader
		if (this.members.isEmpty()) { return false; }	// Has no members

		return true;	// Default
	}


	public MapColor getMapColor()
	{
		if (this.mapColor == -1 || this.mapColor >= MapColor.COLORS.length)
		{
			// Init
			Random rand = new Random();
			this.mapColor = (byte) rand.nextInt(MapColor.COLORS.length);
		}

		return MapColor.COLORS[this.mapColor];
	}


	public boolean isLeader(EntityPlayer player)
	{
		if (player == null) { return false; }	// Can't have nothing as a leader

		if (this.leaderUUID != null && this.leaderUUID.equals(player.getGameProfile().getId())) { return true; }	// Is indeed the leader

		return false;	// Default
	}


	// New faction, first leader. Called after adding that player as a member
	public void setLeader(EntityPlayer player)
	{
		if (player == null) { return; }

		this.leaderUUID = player.getGameProfile().getId();	// Done

		this.sendMessageToFactionMembers(player.worldObj, player.getName() + " has become LEADER!");

		if (this.members.containsKey(this.leaderUUID))	// Is a known member, so changing their display name now
		{
			this.members.put(this.leaderUUID, "§6" + player.getName());
		}
	}


	// Existing faction, new leader
	public void setNewLeader(EntityPlayer player)
	{
		if (player == null) { return; }

		UUID prevLeader = this.leaderUUID;

		this.leaderUUID = player.getGameProfile().getId();	// Done

		if (this.members.containsKey(prevLeader))
		{
			String prevName = this.members.get(prevLeader);
			this.members.put(prevLeader, prevName.replace("§6", ""));	// Removing the gold name from the previous leader
		}

		this.members.put(this.leaderUUID, "§6" + player.getName());	// And adding the leader as a member

		this.sendMessageToFactionMembers(player.worldObj, player.getName() + " has become LEADER!");
	}


	public boolean isChunkInTerritory(Chunk chunk)
	{
		return (this.getChunkClaimFromTerritory(chunk) != null);	// True for existing, false for null
	}


	public void addChunk(Chunk chunk)
	{
		if (chunk == null) { return; }

		if (this.chunks.contains(chunk)) { return; }	// Already on my list

		_ClaimedChunk claim = new _ClaimedChunk(chunk.xPosition, chunk.zPosition);	// Fresh chunk claim

		this.chunks.add(claim);

		this.chunkCount += 1;
	}


	public boolean isPartOfFaction(EntityPlayer player)
	{
		if (player == null) { return false; }	// Can't be what doesn't exist

		//if (player.getGameProfile().getId().equals(this.leaderUUID)) { return true; }	// Is the leader

		if (this.members.containsKey(player.getGameProfile().getId()))
		{
			return true;
		}

		return false;	// Default
	}


	public void addMember(EntityPlayer player)
	{
		if (player == null) { return; }
		if (TerritoryHandler.getFactionPlayerIsMemberOf(player) != null) { return; }	// Already part of a faction

		this.members.put(player.getGameProfile().getId(), player.getName());

		this.memberCount += 1;

		this.sendMessageToFactionMembers(player.worldObj, player.getName() + " was added as faction member of " + this.getFactionName() + ".");
	}


	// Already verified to be a member
	public void removeMember(EntityPlayer player)
	{
		if (this.members.remove(player.getGameProfile().getId()) != null)	// Easy enough.
		{
			this.memberCount -= 1;

			this.sendMessageToFactionMembers(player.worldObj, player.getName() + " left the faction " + this.getFactionName() + ".");
		}
		// else, didn't get anything back, so likely wasn't in there
	}


	// Called whenever someone who's not part of this faction tries to claim this chunk
	public void takeOverChunk(Chunk chunk)
	{
		Iterator<_ClaimedChunk> it = this.chunks.iterator();
		_ClaimedChunk claim;

		while (it.hasNext())
		{
			claim = it.next();

			if (chunk.xPosition == claim.getChunkPosX() && chunk.zPosition == claim.getChunkPosZ())	// Found it
			{
				claim.decreaseControlStrength();

				this.sendMessageToFactionMembers(chunk.getWorld(), "Someone is trying to take over the chunk at X " + claim.getChunkPosX() + " / Z " + claim.getChunkPosZ() +
						"! Control strength: " + claim.getControlStrength());

				// Is the protection up?
				if (!this.isUpkeepPaid()) { claim.DisableStrength(); }

				// Any control left?
				if (claim.getControlStrength() <= 0)
				{
					it.remove();	// Begone

					this.sendMessageToFactionMembers(chunk.getWorld(), "Chunk control lost!");
					this.chunkCount -= 1;

					this.verifyTerritory(claim, chunk.getWorld(), chunk);
				}
			}
			// else, coords don't check out
		}
	}


	public void sendMessageToFactionMembers(World world, String msg)
	{
		Iterator it = world.playerEntities.iterator();

		while (it.hasNext())
		{
			EntityPlayer player = (EntityPlayer) it.next();

			if (this.isPartOfFaction(player)) { Main.sendMessageToPlayer(player, msg); }	// Here ya go
		}
	}


	// Will try the leader first and then whatever member it can find if that didn't work out
	public void dropValuablesAtFactionMember(World world)
	{
		Iterator it = world.playerEntities.iterator();

		while (it.hasNext())
		{
			EntityPlayer player = (EntityPlayer) it.next();

			if (this.isLeader(player))	// Leader first
			{
				this.dropValuables(world, player);
				return;
			}
			else if (this.isPartOfFaction(player)) // Found a player that is part of our faction. Throwing all this faction's valuables at their feet
			{
				this.dropValuables(world, player);
				return;
			}
		}
	}


	// Dumping
	private void dropValuables(World world, EntityPlayer player)
	{
		while (this.getUpkeepStored() > 0)
		{
			int amount = Main.getMaxPaymentOrderAmount();	// Defaulting to the max

			if (this.getUpkeepStored() < Main.getMaxPaymentOrderAmount()) { amount = this.getUpkeepStored(); }	// Not enough left to go over bounds, which is just fine.

			ItemStack stack = new ItemStack(Main.getPaymentOrder(), 1, amount);

			EntityItem entityitem = new EntityItem(world, player.posX, player.posY + 1.0d, player.posZ, stack);
			entityitem.setDefaultPickupDelay();

			Main.console("[TERRITORIAL DEALINGS] Dropping all valuables at position of " + player.getName() + ". Current amount: " + amount + ". Leftover: " + this.getUpkeepStored());

			// And dropping it
			world.spawnEntityInWorld(entityitem);

			this.upkeepStored -= amount;
		}

		// Done, there should now be nothing left in this faction's vault.
	}


	public _ClaimedChunk getChunkClaimFromTerritory(Chunk chunk)
	{
		if (chunk == null) { return null; }

		Iterator<_ClaimedChunk> it = this.chunks.iterator();
		_ClaimedChunk claim;

		while (it.hasNext())
		{
			claim = it.next();

			if (chunk.xPosition == claim.getChunkPosX() && chunk.zPosition == claim.getChunkPosZ())
			{
				return claim; // Found it
			}
			// else, coords don't check out
		}

		return null;
	}


	private void verifyTerritory(_ClaimedChunk claim, World world, Chunk chunk)
	{
		Main.console("[TERRITORY] Verified territory. Claimed chunks of faction " + this.getFactionName() + ": " + this.getChunkCount());

		if (this.getChunkCount() <= 0)
		{
			TerritoryHandler.removeTerritory(this);

			this.sendMessageToFactionMembers(world, "Faction " + this.getFactionName() + " has no territory left! Disbanding.");

			// Dropping all remaining vault contents as payment orders of certain value at the last chunk
			Main.console("[TERRITORY] Dropping remaining vault contents at last chunk. Value: " + this.getUpkeepStored());

			this.dropValuablesAtFactionMember(world);
		}
		// else, still have some chunks
	}


	public void refreshChunkControl()
	{
		Iterator<_ClaimedChunk> it = this.chunks.iterator();
		_ClaimedChunk claim;

		while (it.hasNext())
		{
			claim = it.next();

			if (claim.getControlStrength() < Main.getMaxControlStrength())
			{
				claim.refreshControlStrength(); // Refreshing by this much
			}
			// else, at or above max strength. Nothing to be done here
		}
	}


	public void addUpkeep(EntityPlayer player, int amount)
	{
		if (amount <= 0) { return; }	// Only ADDING to it

		this.upkeepStored += amount;
		this.setUpkeepPaid(true);	// Just to make sure

		this.sendMessageToFactionMembers(player.worldObj, "Added " + amount + " VALUABLES to territory vault. Total: " + this.getUpkeepStored());
	}


	// Happens once every x ticks
	public void consumeUpkeep(World world)
	{
		if (Main.getUpkeepMultiplier() <= 0) { return; }	// Nothing to deduct here, so not bothering.

		this.sendMessageToFactionMembers(world, "Deducting territory upkeep of " + (this.getChunkCount() * Main.getUpkeepMultiplier()) + " VALUABLES from the vault (" +
				this.getUpkeepStored() + " VALUABLES) now.");

		Main.console("[TERRITORIAL DEALINGS] Deducting territory upkeep of " + (this.getChunkCount() * Main.getUpkeepMultiplier()) + " VALUABLES from the vault (" +
				this.getUpkeepStored() + " VALUABLES) now.");

		this.upkeepStored -= (this.getChunkCount() * Main.getUpkeepMultiplier());

		if (this.getUpkeepStored() < 0) // Less then than we need
		{
			this.upkeepStored = 0;		// Just making sure
			this.setUpkeepPaid(false);	// No protection anymore
			this.sendMessageToFactionMembers(world, "Deduction failed! Disabling territory protection.");
		}
		else
		{
			this.sendMessageToFactionMembers(world, "Deduction successful. Territory protection is enabled.");
		}
	}


	public boolean isMemberOnline(World world)
	{
		if (!Main.doesPlayerNeedToBeOnline()) { return true; }	// Don't care. Go!
		if (!this.isUpkeepPaid()) { return true; }				// Not protected right now, so go for it

		Iterator it = world.playerEntities.iterator();

		double percentOnline = 0;
		double percentIncrease = 100 / this.memberCount;	// Each member is this much percentage worth

		while (it.hasNext())
		{
			EntityPlayer player = (EntityPlayer) it.next();

			if (this.isPartOfFaction(player))
			{
				percentOnline += percentIncrease;

				if (percentOnline >= Main.getRequiredPlayerAmount()) { return true; }	// Enough members are online. Go for it.
			}
		}

		return false;
	}


	public int getDimensionID() { return this.dimensionID; }


	public String getFactionName() { return this.factionName; }
	public void setFactionName(String factionName) { this.factionName = factionName; }


	public boolean isUpkeepPaid() { return this.upkeepPaid; }
	public void setUpkeepPaid(boolean upkeepPaid) { this.upkeepPaid = upkeepPaid; }


	public int getChunkControlStrength(Chunk chunk)
	{
		if (chunk == null) { return 0; }

		_ClaimedChunk claim = this.getChunkClaimFromTerritory(chunk);

		if (claim == null) { return 0; }

		return claim.getControlStrength();
	}


	public int getChunkCount() { return this.chunkCount; }

	public int getUpkeepStored() { return this.upkeepStored; }


	public Iterator<Entry<UUID, String>> getMemberListIterator()
	{
		return this.members.entrySet().iterator();
	}


	public boolean isLeader(UUID uuid)
	{
		return this.leaderUUID.equals(uuid);
	}


	public int getMembercount() { return this.memberCount; }


	public void markPositionForReturn(EntityPlayer player)
	{
		if (!this.isLeader(player)) { return; }

		Chunk chunk = player.worldObj.getChunkFromBlockCoords(new BlockPos(player));

		if (!this.isChunkInTerritory(chunk)) // Not part of our territory
		{
			Main.sendMessageToPlayer(player, "This location doesn't seem to be part of your territory.");
			return;
		}

		// Alright, marking their dimension and xyz as return position
		this.returnX = player.posX;
		this.returnY = player.posY;
		this.returnZ = player.posZ;

		this.returnDim = player.dimension;

		Main.sendMessageToPlayer(player, "Marked this position as return point for your faction members.");
	}


	// Assuming the passed in entity to be a member
	public void moveMemberToReturnPoint(EntityPlayer player)
	{
		if (this.returnX == 0 && this.returnY == 0 && this.returnZ == 0) // No return point set
		{
			Main.sendMessageToPlayer(player, "Looks like no return point has been set set.");
			return;
		}

		player.setPositionAndUpdate(this.returnX, this.returnY, this.returnZ);

		// Different dimension, so transfering them
		if (this.returnDim != player.dimension)
		{
			Transition.transferPlayerToDimension((EntityPlayerMP) player, this.returnDim, (int) this.returnY);

			player.setPositionAndUpdate(this.returnX, this.returnY, this.returnZ);	// Setting it again, just to make sure
		}
	}
}
