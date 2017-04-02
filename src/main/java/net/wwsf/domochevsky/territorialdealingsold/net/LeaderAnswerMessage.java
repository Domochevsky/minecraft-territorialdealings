package net.wwsf.domochevsky.territorialdealingsold.net;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class LeaderAnswerMessage implements IMessage
{
	boolean isLeader;

	public LeaderAnswerMessage() {}  // this constructor is required otherwise you'll get errors (used somewhere in fml through reflection)


	@Override
	public void fromBytes(ByteBuf buf)
	{
		// the order is important
		this.isLeader = buf.readBoolean();
	}


	@Override
	public void toBytes(ByteBuf buf)
	{
		buf.writeBoolean(this.isLeader);
	}


	public void setIsLeader() { this.isLeader = true; }
}
