package net.wwsf.domochevsky.territorialdealingsold.item;

import java.util.List;

import net.minecraft.block.BlockDirt;
import net.minecraft.block.BlockStone;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.storage.MapData;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.wwsf.domochevsky.territorialdealingsold.TerritoryHandler;
import net.wwsf.domochevsky.territorialdealingsold._Territory;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;

public class TerritoryMap extends ItemMap
{
	public TerritoryMap()
	{
		this.setMaxStackSize(1);	// Can hold a big stack of these
		this.setCreativeTab(CreativeTabs.TOOLS);
		this.setHasSubtypes(true);

		this.setRegistryName("territorymap");
		this.setUnlocalizedName("territorychevsky_territorymap");
	}


	@Override
	public String getItemStackDisplayName(ItemStack stack) { return "Territory Map"; }


	@Override
	public MapData getMapData(ItemStack stack, World world)
	{
		if (!this.isMapReady(stack)) { return null; }

		String mapID = "map_" + stack.getItemDamage();
		MapData mapdata = (MapData)world.loadItemData(MapData.class, mapID);

		if (mapdata == null && !world.isRemote)
		{
			stack.setItemDamage(world.getUniqueDataId("map"));

			mapID = "map_" + stack.getItemDamage();
			mapdata = new MapData(mapID);
			mapdata.scale = 2;				// Default scale

			int iUnknown = 128 * (1 << mapdata.scale);

			mapdata.xCenter = Math.round(world.getWorldInfo().getSpawnX() / iUnknown) * iUnknown;
			mapdata.zCenter = Math.round(world.getWorldInfo().getSpawnZ() / iUnknown) * iUnknown;

			mapdata.dimension = world.provider.getDimension();

			mapdata.markDirty();
			world.setItemData(mapID, mapdata);
		}

		return mapdata;
    }


	@Override
	public Packet<?> createMapDataPacket(ItemStack stack, World worldIn, EntityPlayer player)
    {
		MapData mapdata = this.getMapData(stack, worldIn);

		if (mapdata == null)
        {
            mapdata = new MapData("map_0");
            worldIn.setItemData("map_0", mapdata);
        }

        return mapdata.getMapPacket(stack, worldIn, player);
    }


	@Override
	public void onUpdate(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected)
    {
        if (!worldIn.isRemote)
        {
            MapData mapdata = this.getMapData(stack, worldIn);

            if (mapdata == null)
            {
                mapdata = new MapData("map_0");
                worldIn.setItemData("map_0", mapdata);
            }

            if (isSelected || entityIn instanceof EntityPlayer && ((EntityPlayer)entityIn).getHeldItemOffhand() == stack)
            {
                this.updateMapData(worldIn, entityIn, mapdata);
            }
        }
    }


	@Override
	public void onCreated(ItemStack stack, World world, EntityPlayer player)
	{
		// Nothing to be done here
    }


	@Override
	public ActionResult<ItemStack> onItemRightClick(ItemStack stack, World world, EntityPlayer player, EnumHand hand)
    {
		if (this.isMapReady(stack))
		{
			// Is already a map, so nothing to be done here
		}
		else
		{
			// Initiating it now
			stack.setTagCompound(new NBTTagCompound());
			stack.getTagCompound().setBoolean("initiated", true);

			stack.setItemDamage(world.getUniqueDataId("map"));

			String uniqueName = "map_" + stack.getItemDamage();

			MapData mapdata = new MapData(uniqueName);
	        world.setItemData(uniqueName, mapdata);

	        mapdata.scale = 2;					// Default scale
	        int i = 128 * (1 << mapdata.scale);

	        mapdata.xCenter = (int)(Math.round(player.posX / i) * i);
	        mapdata.zCenter = (int)(Math.round(player.posZ / i) * i);
	        mapdata.dimension = world.provider.getDimension();

	        mapdata.markDirty();
		}

        return new ActionResult(EnumActionResult.PASS, stack);
    }


    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List info, boolean showAdvancedInfo)
    {


        if (!this.isMapReady(stack))
        {
        	info.add("It's currently blank. Use the");
        	info.add("map to start inscribing it.");
        }
        else
        {
        	MapData mapData = this.getMapData(stack, player.worldObj);

        	info.add("A map of the land.");
        	//info.add("Center: x" + mapData.xCenter + " z" + mapData.zCenter);
        	info.add("Dimension: " + mapData.dimension);

        	if (showAdvancedInfo)
        	{
        		info.add("Scaling at 1:" + (1 << mapData.scale));
                info.add("(Level " + mapData.scale + "/" + 4 + ")");
        	}
        }
    }


    private boolean isMapReady(ItemStack stack)
    {
    	if (!stack.hasTagCompound()) { return false; }
		if (!stack.getTagCompound().getBoolean("initiated")) { return false; }

		return true;	// Checks out
    }


    @Override
	public void updateMapData(World worldIn, Entity viewer, MapData data)
    {
        if (worldIn.provider.getDimensionType().getId() == data.dimension && viewer instanceof EntityPlayer)
        {
            int mapScale = 1 << data.scale;

            int mapCenterX = data.xCenter;
            int mapCenterZ = data.zCenter;

            int mapX = MathHelper.floor_double(viewer.posX - mapCenterX) / mapScale + 64;
            int mapZ = MathHelper.floor_double(viewer.posZ - mapCenterZ) / mapScale + 64;

            int mapY = 128 / mapScale;

            if (worldIn.provider.getHasNoSky())
            {
                mapY /= 2;
            }

            MapData.MapInfo mapdata$mapinfo = data.getMapInfo((EntityPlayer)viewer);

            ++mapdata$mapinfo.step;

            boolean flag = false;

            for (int unknownInt = mapX - mapY + 1; unknownInt < mapX + mapY; ++unknownInt)
            {
            	this.updateMapData_2(worldIn, viewer, data, mapX, mapY, mapZ, unknownInt, mapScale, mapCenterX, mapCenterZ, mapdata$mapinfo, flag);
            }
        }
    }


    private void updateMapData_2(World worldIn, Entity viewer, MapData data, int mapX, int mapY, int mapZ, int unknownInt, int mapScale, int mapCenterX, int mapCenterZ, MapData.MapInfo mapdata$mapinfo, boolean bUnknown)
    {
    	 if ((unknownInt & 15) == (mapdata$mapinfo.step & 15) || bUnknown)
         {
             bUnknown = false;
             double d0 = 0.0D;

             for (int unknownInt_2 = mapZ - mapY - 1; unknownInt_2 < mapZ + mapY; ++unknownInt_2)
             {
                 if (unknownInt >= 0 && unknownInt_2 >= -1 && unknownInt < 128 && unknownInt_2 < 128)
                 {
                     int i2 = unknownInt - mapX;
                     int j2 = unknownInt_2 - mapZ;

                     boolean flag1 = i2 * i2 + j2 * j2 > (mapY - 2) * (mapY - 2);

                     int k2 = (mapCenterX / mapScale + unknownInt - 64) * mapScale;
                     int l2 = (mapCenterZ / mapScale + unknownInt_2 - 64) * mapScale;

                     Multiset<MapColor> multiset = HashMultiset.<MapColor>create();
                     Chunk chunk = worldIn.getChunkFromBlockCoords(new BlockPos(k2, 0, l2));

                     // Inserting my stuff here
                     _Territory territory = TerritoryHandler.getFactionFromChunk(chunk);

                     if (!chunk.isEmpty())
                     {
                         int i3 = k2 & 15;
                         int j3 = l2 & 15;
                         int k3 = 0;
                         double dUnknown = 0.0D;

                         if (worldIn.provider.getHasNoSky())
                         {
                             int l3 = k2 + l2 * 231871;
                             l3 = l3 * l3 * 31287121 + l3 * 11;

                             if ((l3 >> 20 & 1) == 0)
                             {
                                 multiset.add(Blocks.DIRT.getDefaultState().withProperty(BlockDirt.VARIANT, BlockDirt.DirtType.DIRT).getMapColor(), 10);
                             }
                             else
                             {
                                 multiset.add(Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT, BlockStone.EnumType.STONE).getMapColor(), 100);
                             }

                             dUnknown = 100.0D;
                         }
                         else
                         {
                             BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

                             for (int i4 = 0; i4 < mapScale; ++i4)
                             {
                                 for (int j4 = 0; j4 < mapScale; ++j4)
                                 {
                                     int blockHeight = chunk.getHeightValue(i4 + i3, j4 + j3) + 1;
                                     IBlockState iblockstate = Blocks.AIR.getDefaultState();

                                     if (blockHeight > 1)
                                     {
                                         label542:
                                         {
                                             while (true)
                                             {
                                                 --blockHeight;
                                                 iblockstate = chunk.getBlockState(blockpos$mutableblockpos.setPos(i4 + i3, blockHeight, j4 + j3));

                                                 if (iblockstate.getMapColor() != MapColor.AIR || blockHeight <= 0)
                                                 {
                                                     break;
                                                 }
                                             }

                                             if (blockHeight > 0 && iblockstate.getMaterial().isLiquid())
                                             {
                                                 int l4 = blockHeight - 1;

                                                 while (true)
                                                 {
                                                     IBlockState iblockstate1 = chunk.getBlockState(i4 + i3, l4--, j4 + j3);
                                                     ++k3;

                                                     if (l4 <= 0 || !iblockstate1.getMaterial().isLiquid())
                                                     {
                                                         break label542;
                                                     }
                                                 }
                                             }
                                         }
                                     }

                                     dUnknown += blockHeight / (double)(mapScale * mapScale);
                                     //multiset.add(iblockstate.getMapColor());

                                     // Inserting here as well.
                                     if (territory != null)
                                     {
                                    	 multiset.add(territory.getMapColor());	// Overriding the actual color with a wool color. Each faction gets theirs randomly assigned
                                     }
                                     else
                                     {
                                    	 multiset.add(iblockstate.getMapColor());
                                     }
                                 }
                             }
                         }

                         k3 = k3 / (mapScale * mapScale);
                         double d2 = (dUnknown - d0) * 4.0D / (mapScale + 4) + ((unknownInt + unknownInt_2 & 1) - 0.5D) * 0.4D;
                         int i5 = 1;

                         if (d2 > 0.6D)
                         {
                             i5 = 2;
                         }

                         if (d2 < -0.6D)
                         {
                             i5 = 0;
                         }

                         MapColor mapcolor = Iterables.getFirst(Multisets.<MapColor>copyHighestCountFirst(multiset), MapColor.AIR);

                         if (mapcolor == MapColor.WATER)
                         {
                             d2 = k3 * 0.1D + (unknownInt + unknownInt_2 & 1) * 0.2D;
                             i5 = 1;

                             if (d2 < 0.5D)
                             {
                                 i5 = 2;
                             }

                             if (d2 > 0.9D)
                             {
                                 i5 = 0;
                             }
                         }

                         d0 = dUnknown;

                         if (unknownInt_2 >= 0 && i2 * i2 + j2 * j2 < mapY * mapY && (!flag1 || (unknownInt + unknownInt_2 & 1) != 0))
                         {
                             byte b0 = data.colors[unknownInt + unknownInt_2 * 128];
                             byte b1 = (byte)(mapcolor.colorIndex * 4 + i5);

                             if (b0 != b1)
                             {
                                 data.colors[unknownInt + unknownInt_2 * 128] = b1;
                                 data.updateMapData(unknownInt, unknownInt_2);
                                 bUnknown = true;
                             }
                         }
                     }
                 }
             }
         }
    }
}
