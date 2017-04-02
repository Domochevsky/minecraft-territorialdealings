package net.wwsf.domochevsky.territorialdealingsold;

import java.io.Serializable;

public class _ClaimedChunk implements Serializable
{
	private static final long serialVersionUID = 1L;

	private int posX;
	private int posZ;

	private int controlStrength;	// This needs to be reduced to 0 to remove control and return this chunk to neutral (and claimable)

	public _ClaimedChunk(int chunkX, int chunkZ)
	{
		this.posX = chunkX;
		this.posZ = chunkZ;

		this.controlStrength = Main.getMaxControlStrength();
	}


	public int getControlStrength() { return this.controlStrength; }
	public void decreaseControlStrength() { this.controlStrength -= 1; }
	public void DisableStrength() { this.controlStrength = 0; }	// Someone didn't pay for their upkeep


	public int getChunkPosX() { return this.posX; }
	public int getChunkPosZ() { return this.posZ; }


	public void refreshControlStrength()
	{
		this.controlStrength += Main.getStrengthRefreshAmount();

		if (this.controlStrength > Main.getMaxControlStrength())
		{
			this.controlStrength = Main.getMaxControlStrength();	// Cap
		}
		// else, at or below max strength. That's fine
	}
}
