package net.wwsf.domochevsky.territorialdealingsold.net;

import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class LeaderConfirmPacket implements IMessageHandler<LeaderConfirmMessage, LeaderAnswerMessage>
{
	@Override
	public LeaderAnswerMessage onMessage(LeaderConfirmMessage message, MessageContext ctx)
	{
		if (ctx.side.isServer())	// just to make sure that the side is correct
		{
			return ServerHelper.isPlayerLeader(message.worldID, message.entityID);	// Sending an answer back. The client will set a bool based on this
		}

		return null;	// Don't care about returning anything
	}
}
