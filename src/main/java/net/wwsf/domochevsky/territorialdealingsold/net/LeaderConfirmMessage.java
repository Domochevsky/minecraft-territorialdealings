package net.wwsf.domochevsky.territorialdealingsold.net;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class LeaderConfirmMessage implements IMessage
{
	int entityID;
	int worldID;

	public LeaderConfirmMessage() {}  // this constructor is required otherwise you'll get errors (used somewhere in fml through reflection)


	// Sending a message to the client to display particles at a specific entity's position
	public LeaderConfirmMessage(int worldID, int entityID)
	{
		this.entityID = entityID;
		this.worldID = worldID;
	}


	@Override
	public void fromBytes(ByteBuf buf)
	{
		// the order is important
		this.entityID = buf.readInt();
		this.worldID = buf.readInt();
	}


	@Override
	public void toBytes(ByteBuf buf)
	{
		buf.writeInt(this.entityID);
		buf.writeInt(this.worldID);
	}
}
