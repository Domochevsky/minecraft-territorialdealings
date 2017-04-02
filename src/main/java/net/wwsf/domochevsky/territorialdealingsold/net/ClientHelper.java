package net.wwsf.domochevsky.territorialdealingsold.net;

import net.wwsf.domochevsky.territorialdealingsold.Main;

public class ClientHelper 
{
	// Called only by the client
	private static boolean isLeader;
	//private static int sendDelay;

	// Asked by the recipe handler when all items match
	public static boolean isPlayerLeader(int worldID, int entityID)
	{
		// Any leader will do, for crafting purposes
		LeaderConfirmMessage msg = new LeaderConfirmMessage(worldID, entityID);
		PacketHandler.sendMsgToServer(msg);

		Main.console("[CLIENT] Asking the server if the player is a leader for crafting results. Previous result: " + isLeader);

		return isLeader;	// Will be up to date eventually, since the server needs time to send this back to the client
	}


	// The answer that came back from the server
	public static void setPlayerIsLeader(boolean set)
	{
		isLeader = set;
	}
}
