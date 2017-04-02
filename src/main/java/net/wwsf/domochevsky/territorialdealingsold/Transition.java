package net.wwsf.domochevsky.territorialdealingsold;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketEntityEffect;
import net.minecraft.network.play.server.SPacketPlayerAbilities;
import net.minecraft.network.play.server.SPacketRespawn;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.WorldServer;

public class Transition 
{
	// Copying and modifying vanilla functions, so we don't try to come out of a portal on dimension change.
	// Looks like I need to travel deep.
	static void transferPlayerToDimension(EntityPlayerMP player, int dimensionTo, int targetHeight)
    {
		if (!net.minecraftforge.common.ForgeHooks.onTravelToDimension(player, dimensionTo)) { return; }	// Moving that here, to consolidate these functions.

		net.minecraft.world.Teleporter teleporter = player.mcServer.worldServerForDimension(dimensionTo).getDefaultTeleporter();
		PlayerList playerList = player.mcServer.getPlayerList();

        int dimensionFrom = player.dimension;

        // Changing dimensions...
        WorldServer wsPrev = player.mcServer.worldServerForDimension(player.dimension);

        player.dimension = dimensionTo;

        WorldServer wsNew = player.mcServer.worldServerForDimension(player.dimension);

        // Respawn? Used to recreate the player entity.
        player.connection.sendPacket(new SPacketRespawn(player.dimension, wsNew.getDifficulty(), wsNew.getWorldInfo().getTerrainType(), player.interactionManager.getGameType()));

        // Capabilities...
        playerList.updatePermissionLevel(player);

        // Begone from the old world?
        wsPrev.removeEntity(player);

        // Safety, I suppose.
        player.isDead = false;

        // We haven't reached the "set portal" level yet. Going deeper.
        transferEntityToWorld(player, dimensionFrom, wsPrev, wsNew, teleporter, targetHeight);

        // Getting chunks ready?
        playerList.preparePlayer(player, wsPrev);

        // Inserting their new position here? May not be necessary, since this is called by the drop and transfer, which sets the position beforehand and afterwards.
        player.connection.setPlayerLocation(player.posX, player.posY, player.posZ, player.rotationYaw, player.rotationPitch);

        // More formalities...
        player.interactionManager.setWorld(wsNew);
        player.connection.sendPacket(new SPacketPlayerAbilities(player.capabilities));

        playerList.updateTimeAndWeatherForPlayer(player, wsNew);
        playerList.syncPlayerInventory(player);

        // Reapplying potion effects
        for (PotionEffect potioneffect : player.getActivePotionEffects())
        {
            player.connection.sendPacket(new SPacketEntityEffect(player.getEntityId(), potioneffect));
        }

        // We're done.
        net.minecraftforge.fml.common.FMLCommonHandler.instance().firePlayerChangedDimensionEvent(player, dimensionFrom, dimensionTo);
    }


	// We must go deeper.
	private static void transferEntityToWorld(Entity entityIn, int lastDimension, WorldServer oldWorldIn, WorldServer toWorldIn, net.minecraft.world.Teleporter teleporter, int targetHeight)
    {
        net.minecraft.world.WorldProvider pOld = oldWorldIn.provider;
        net.minecraft.world.WorldProvider pNew = toWorldIn.provider;

        double moveFactor = pOld.getMovementFactor() / pNew.getMovementFactor();

        // What does this do?
        double newPosX = entityIn.posX * moveFactor;
        double newPosZ = entityIn.posZ * moveFactor;
        //double d2 = 8.0D;

        float entityRotationYaw = entityIn.rotationYaw;	// What is this being saved for?

        oldWorldIn.theProfiler.startSection("moving");

        // Overworld, where we're going
        if (entityIn.dimension == 1)
        {
            BlockPos blockpos;

            if (lastDimension == 1)
            {
                blockpos = toWorldIn.getSpawnPoint();	// From the End to the End?
            }
            else
            {
                blockpos = toWorldIn.getSpawnCoordinate();	// From Wherever to the End
            }

			newPosX = blockpos.getX();
			entityIn.posY = blockpos.getY();
			newPosZ = blockpos.getZ();

			entityIn.setLocationAndAngles(newPosX, entityIn.posY, newPosZ, 90.0F, 0.0F);

            if (entityIn.isEntityAlive())
            {
                oldWorldIn.updateEntityWithOptionalForce(entityIn, false);
            }
        }

        oldWorldIn.theProfiler.endSection();

        // ...anything else? Did not come from the overworld
        if (lastDimension != 1)
        {
            oldWorldIn.theProfiler.startSection("placing");
            newPosX = MathHelper.clamp_int((int)newPosX, -29999872, 29999872);
            newPosZ = MathHelper.clamp_int((int)newPosZ, -29999872, 29999872);

            if (entityIn.isEntityAlive())
            {
                entityIn.setLocationAndAngles(newPosX, targetHeight, newPosZ, entityIn.rotationYaw, entityIn.rotationPitch);

                //teleporter.placeInPortal(entityIn, entityRotationYaw);	// Found it. Begone with ye.

                toWorldIn.spawnEntityInWorld(entityIn);
                toWorldIn.updateEntityWithOptionalForce(entityIn, false);
            }

            oldWorldIn.theProfiler.endSection();
        }

        entityIn.setWorld(toWorldIn);
    }
}
