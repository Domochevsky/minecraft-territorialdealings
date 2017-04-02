package net.wwsf.domochevsky.territorialdealingsold.net;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class LeaderAnswerPacket implements IMessageHandler<LeaderAnswerMessage, IMessage>
{
	@Override
	public IMessage onMessage(LeaderAnswerMessage message, MessageContext ctx)
	{
		if (ctx.side.isClient())	// just to make sure that the side is correct
		{
			ClientHelper.setPlayerIsLeader(message.isLeader);
		}

		return null;	// Don't care about returning anything
	}
}
