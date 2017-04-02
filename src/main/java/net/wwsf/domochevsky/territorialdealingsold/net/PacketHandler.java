package net.wwsf.domochevsky.territorialdealingsold.net;

import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class PacketHandler 
{
	private static SimpleNetworkWrapper net;
	private static int nextPacketId = 0;


	public static void initPackets()
	{
		net = NetworkRegistry.INSTANCE.newSimpleChannel("territorychevsky".toUpperCase());

		registerMessage(LeaderConfirmPacket.class, LeaderConfirmMessage.class);	// From client to server
		registerMessage(LeaderAnswerPacket.class, LeaderAnswerMessage.class);	// From server to client
	}


	private static void registerMessage(Class packet, Class message)
	{
		net.registerMessage(packet, message, nextPacketId, Side.CLIENT);	// Only care about sending things to the client
		net.registerMessage(packet, message, nextPacketId, Side.SERVER);	// Sending this to the server
		nextPacketId++;
	}


	public static void sendMsgToServer(IMessage msg)
	{
		if (net == null)
		{
			System.out.println("[TERRITORIAL DEALINGS] Warning, net handler is null. How'd that happen? Trying to register it again now.");
			initPackets();

			net.sendToServer(msg);
		}
		else
		{
			net.sendToServer(msg);
		}
	}
}
