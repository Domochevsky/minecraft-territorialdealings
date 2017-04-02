package net.wwsf.domochevsky.territorialdealingsold.item;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.wwsf.domochevsky.territorialdealingsold.Main;
import net.wwsf.domochevsky.territorialdealingsold.TerritoryHandler;
import net.wwsf.domochevsky.territorialdealingsold._Territory;

import com.mojang.realmsclient.gui.ChatFormatting;

public class ClaimTerritoryOrder extends LeaderRequiredItem
{
	public ClaimTerritoryOrder()
	{
		this.setMaxStackSize(64);	// Can hold a big stack of these
		this.setFull3D();
		this.setCreativeTab(CreativeTabs.TOOLS);

		this.setRegistryName("claimorder");
		this.setUnlocalizedName("territorychevsky_claimorder");
	}


	@Override
	public String getItemStackDisplayName(ItemStack stack) { return "Claim Order"; }


	@Override
	public ActionResult<ItemStack> onItemRightClick(ItemStack stack, World world, EntityPlayer player, EnumHand hand)
    {
		if (world.isRemote) {  return new ActionResult(EnumActionResult.PASS, stack); }	// Not doing this on client side

		if (stack.hasTagCompound() && stack.getTagCompound().getInteger("cooldown") > 0) { return new ActionResult(EnumActionResult.PASS, stack); }	// Not yet

		if (TerritoryHandler.addChunkToFaction(player)) { stack.stackSize -= 1; }
		else	// Failed, so adding a cooldown
		{
			if (!stack.hasTagCompound()) { stack.setTagCompound(new NBTTagCompound()); }	// Init

			stack.getTagCompound().setInteger("cooldown", 20);	// Can only try once a second
		}

        return new ActionResult(EnumActionResult.PASS, stack);
    }


	@Override
	public void onUpdate(ItemStack stack, World world, Entity entity, int animTick, boolean holdingItem) 	// Overhauled
	{
		if (world.isRemote) { return; } // Not doing this on client side

		if (!stack.hasTagCompound()) { stack.setTagCompound(new NBTTagCompound()); }

		if (stack.getTagCompound().getInteger("cooldown") > 0)
		{
			stack.getTagCompound().setInteger("cooldown", stack.getTagCompound().getInteger("cooldown") - 1);	// Ticking down
		}

		if (stack.getTagCompound().getInteger("updateInfo") > 0) // Refreshing info
		{
			stack.getTagCompound().setInteger("updateInfo", stack.getTagCompound().getInteger("updateInfo") - 1);	// Ticking down
		}
		else	// Done ticking down
		{
			stack.getTagCompound().setInteger("updateInfo", 40);	// Time until we do this again

			Chunk chunk = world.getChunkFromBlockCoords(new BlockPos(entity));

			if (chunk != null)
			{
				// Does this chunk belong to anyone?
				_Territory faction = TerritoryHandler.getFactionFromChunk(chunk);

				if (faction == null)	// Not owned by anyone, so no resistance
				{
					stack.getTagCompound().setFloat("displayDurability", 1.0f);
					stack.getTagCompound().setString("factionName", "NONE");
				}
				else	// Owned by someone, so displaying this chunk's resistance in our durability bar
				{
					int control = faction.getChunkControlStrength(chunk);

					stack.getTagCompound().setFloat("displayDurability", (float) (1.0 - (1.0 / Main.getMaxControlStrength() * control)));

					stack.getTagCompound().setString("factionName", faction.getFactionName());	// Also keeping track of whose territory this is
				}
			}
			// else, no chunk found at those coords, hm?
		}
	}


	@SideOnly(Side.CLIENT)
	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean par4)
	{
		super.addInformation(stack, player, list, par4);

		list.add("'Go out and conquer!'");
		list.add("Use this to claim more");
		list.add("chunks for your faction.");

		list.add(ChatFormatting.BLUE + "Each claimed chunk increases");
		list.add(ChatFormatting.BLUE + "territory upkeep by " + Main.getUpkeepMultiplier() + ".");

		list.add(ChatFormatting.RED + "Used up on successful claim.");

		if (stack.hasTagCompound())
		{
			list.add(ChatFormatting.YELLOW + "Chunk is claimed by " + stack.getTagCompound().getString("factionName") + ".");
		}
    }


	@Override
	public boolean showDurabilityBar(ItemStack stack) { return true; }


	@Override
	public double getDurabilityForDisplay(ItemStack stack)
	{
		if (stack.hasTagCompound()) { return stack.getTagCompound().getFloat("displayDurability"); }

		return 1.0f;
	}
}
