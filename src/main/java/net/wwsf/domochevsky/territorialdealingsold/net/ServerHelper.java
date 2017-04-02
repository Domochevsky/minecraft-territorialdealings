package net.wwsf.domochevsky.territorialdealingsold.net;

import java.util.Iterator;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.wwsf.domochevsky.territorialdealingsold.TerritoryHandler;

public class ServerHelper 
{
	public static LeaderAnswerMessage isPlayerLeader(int worldID, int entityID)
	{
		LeaderAnswerMessage msg = new LeaderAnswerMessage();	// You'll get a return message either way

		if (isPlayerLeader_2(worldID, entityID))
		{
			msg.setIsLeader();
		}
		// else, not leader, so will return false

		return msg;
	}


	private static boolean isPlayerLeader_2(int worldID, int entityID)
	{
		//List players = MinecraftServer.getServer().getConfigurationManager().playerEntityList;
		//List players = MinecraftServer.getServer().getPlayerList();
		World world = DimensionManager.getWorld(worldID);

		if (world == null) { return false; }	// Don't know where you are.

		List players = world.playerEntities;

		if (players.isEmpty()) { return false; }	// No players? Who called this? D:

		Iterator<EntityPlayerMP> it = players.iterator();
		EntityPlayerMP player;

		while (it.hasNext())
		{
			player = it.next();

			if (player.getEntityId() == entityID)
			{
				// Found 'em!
				if (TerritoryHandler.getFactionPlayerIsLeaderOf(player) == null)
				{
					return false;
				}
				else
				{
					return true;
				}
			}
			// else, not the one I'm looking for
		}

		return false;
	}
}
