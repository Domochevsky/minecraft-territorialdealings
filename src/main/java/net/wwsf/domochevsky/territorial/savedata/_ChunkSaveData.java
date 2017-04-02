package net.wwsf.domochevsky.territorial.savedata;

import java.io.Serializable;

import net.minecraft.util.math.ChunkPos;

public class _ChunkSaveData implements Serializable
{
	public int posX;
	public int posZ;
	
	public int strength;
	
	
	public _ChunkSaveData(ChunkPos chunk, int chunkControlMax) 
	{
		this.posX = chunk.chunkXPos;
		this.posZ = chunk.chunkZPos;
		
		this.strength = chunkControlMax;
	}


	@Override
	public boolean equals(Object obj)
    {
        if (this == obj) 
        { 
        	return true; 	// this is us, so matches
        }	
        
        else if (!(obj instanceof _ChunkSaveData))
        {
            return false;	// Cannot possibly be us
        }
        else
        {
        	// Does our position match?
        	_ChunkSaveData chunkpos = (_ChunkSaveData) obj;
        	
            return this.posX == chunkpos.posX && this.posZ == chunkpos.posZ;
        }
    }
}
