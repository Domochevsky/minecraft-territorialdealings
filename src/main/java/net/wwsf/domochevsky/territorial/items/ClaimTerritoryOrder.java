package net.wwsf.domochevsky.territorial.items;

import java.util.List;
import java.util.UUID;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.wwsf.domochevsky.territorial.ChatFormatting;
import net.wwsf.domochevsky.territorial.Main;
import net.wwsf.domochevsky.territorial.TerritoryHandler;
import net.wwsf.domochevsky.territorial.savedata._FactionSaveData;

public class ClaimTerritoryOrder extends _ItemBase
{
	public ClaimTerritoryOrder()
	{
		this.setMaxStackSize(64);	// Can hold a big stack of these
		this.setFull3D();
		this.setCreativeTab(CreativeTabs.TOOLS);

		this.registerWithName("claimorder");
	}
	
	
	@Override
	public String getItemStackDisplayName(ItemStack stack) 
	{ 
		String factionName = this.getNameOfFaction(stack);
		
		if (factionName == null)
		{
			return "Blank Claim Order"; 
		}
		else
		{
			return "Claim Order for " + factionName; 
		}
	}
	
	
	@SideOnly(Side.CLIENT)
	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean par4)
	{
		super.addInformation(stack, player, list, par4);

		list.add("'Go out and conquer!'");
		list.add("Use this to claim more");
		list.add("chunks for a faction.");
		
		String factionName = this.getNameOfFaction(stack);
		
		if (factionName == null)
		{
			// Blank
			list.add(" ");
			list.add(ChatFormatting.YELLOW + "A LEADER has to sign this first by USING it.");
			list.add(ChatFormatting.YELLOW + "They can sign a whole stack at once.");
		}
		else
		{
			list.add("Each claimed chunk increases");
			list.add("territory upkeep by " + Main.getUpkeepCost() + ".");
			list.add(" ");
			list.add(ChatFormatting.YELLOW + "Only chunks adjacent to this claim's");
			list.add(ChatFormatting.YELLOW + "territory can be claimed.");
			list.add(" ");
			list.add(ChatFormatting.RED + "Used up on successful claim.");
		}
		
		// Is this claimed by anyone?
		BlockPos pos = new BlockPos(player.posX, player.posY, player.posZ);
		Chunk chunk = player.worldObj.getChunkFromBlockCoords(pos);
		
		_FactionSaveData faction = TerritoryHandler.getFactionByChunkPos(player.worldObj.provider.getDimension(), chunk.getChunkCoordIntPair());
		
		if (faction != null)
		{
			list.add(" ");
			list.add(ChatFormatting.BLUE + "This chunk is claimed by " + faction.getName() + ".");
		}
    }
	
	
	@Override
	public ActionResult<ItemStack> onItemRightClick(ItemStack stack, World world, EntityPlayer player, EnumHand hand)
	{
		if (world.isRemote) { return new ActionResult(EnumActionResult.PASS, stack); }	// Not doing this on client side
		
		// Step 0, before we do anything here... is the cooldown expired?
		if (!this.isReady(stack))
		{
			// Not yet.
			return new ActionResult(EnumActionResult.FAIL, stack);
		}

		BlockPos pos = new BlockPos(player.posX, player.posY, player.posZ);
		Chunk chunk = world.getChunkFromBlockCoords(pos);
		
		// Start out as blank claim order, which has to be used (signed) by a leader to become usable. Afterwards anyone can use it to claim territory in their name
		
		String factionName = this.getNameOfFaction(stack);
		
		if (factionName == null)
		{
			// This thing is unsigned.
			
			// Step 1, are you capable of signing this?
			if (TerritoryHandler.isPlayerLeader(player))
			{
				// You are, so signing it in the name of your faction now.
				_FactionSaveData faction = TerritoryHandler.getFactionFromLeader(player);
				
				if (faction == null)
				{
					// Is a leader, but no faction exists? What?
					Main.sendMessageToPlayer(player, "Error: You seem to be the leader of a non-existent faction. How'd that happen?");
					return new ActionResult(EnumActionResult.FAIL, stack);
				}
				else
				{
					// Signing it now
					Main.console("Player " + player.getName() + " signed a CLAIM ORDER for faction " + faction.getName() + " (ID " + faction.getID() + ").");
					this.setFactionID(stack, faction.getID());
					
					return new ActionResult(EnumActionResult.PASS, stack);
				}
			}
			else
			{
				// Nope, so nothing to be done here.
				Main.sendMessageToPlayer(player, "Someone needs to sign this first.");
				return new ActionResult(EnumActionResult.FAIL, stack);
			}
		}
		else
		{
			// This thing is signed. What next?
			
			// Step 0, see if it's adjacent to owned territory. Cannot claim territory that isn't adjacent
			UUID factionID = this.getFactionID(stack);
			_FactionSaveData faction = TerritoryHandler.getFactionByChunkPos(world.provider.getDimension(), chunk.getChunkCoordIntPair());
			
			if (faction != null && faction.getID().equals(factionID))
			{
				// This is your own chunk
				Main.sendMessageToPlayer(player, "Your faction has already claimed this chunk.");
				return new ActionResult(EnumActionResult.FAIL, stack);
			}
			
			if (!TerritoryHandler.isChunkAdjacentToTerritory(world, factionID, chunk))
			{
				Main.sendMessageToPlayer(player, "You're too far away from this claim's territory.");
				return new ActionResult(EnumActionResult.FAIL, stack);
			}
			
			// Step 1, see if anyone holds this chunk.
			
			if (faction == null)
			{
				// Not owned by anyone. That makes this easy. Claiming it.
				
				// Claim~
				TerritoryHandler.claimChunkForFaction(chunk, factionID);
				
				stack.stackSize -= 1;	// Using one claim writ up.
				
				// SFX
				Main.startFireworks(player);
				
				// Done.
				return new ActionResult(EnumActionResult.PASS, stack);	
			}
			else
			{
				// Owned by someone. Who is it?
				if (TerritoryHandler.isChunkOwnedByFaction(factionID, chunk))
				{
					// Already owned by whoever signed this claim order, so nevermind.
					return new ActionResult(EnumActionResult.FAIL, stack);	
				}
				else
				{
					// Owned by someone, but not by who signed it. Attempting a hostile takeover now
					
					// See if enough players of that faction are online to be able to attack it.
					if (!TerritoryHandler.canAttackFaction(world, faction))
					{
						Main.sendMessageToPlayer(player, "Cannot attack this chunk right now.");
						
						return new ActionResult(EnumActionResult.FAIL, stack);
					}
					
					int controlStrength = faction.getControlStrengthFromChunkPos(chunk.getChunkCoordIntPair());
					
					// Bop. Maybe decide later if we want this to be reduced by more
					controlStrength -= 1;
					
					if (controlStrength <= 0)
					{
						// Control is broken, setting it free! (So it can be claimed naturally on the next attempt)
						faction.removeChunk(chunk.getChunkCoordIntPair());
						
						Main.sendMessageToFactionMembers(world, faction.getID(), "Chunk at x " + chunk.xPosition + " z" + chunk.zPosition + " lost!");
						
						// See if this was the last chunk of that faction and disband it, if it is. (And drop all the stored valuables)
						if (faction.getChunkList().size() <= 0)
						{
							// It is, so disbanding and scattering their valuables at my feet now
							Main.sendMessageToFactionMembers(world, factionID, "Last chunk lost!");
							TerritoryHandler.disbandFaction(world, faction.getID(), player);
						}
					}
					else
					{
						// Still got some in them, so nevermind.
						faction.setControlStrengthForChunkPos(chunk.getChunkCoordIntPair(), controlStrength);
					}
					
					// SFX (Letting everyone know)
					Main.startFireworks(player);
					
					// Letting them know, if need be
					this.informFactionMembers(world, faction, controlStrength + 1, controlStrength, Main.getChunkControlMax(), chunk.getChunkCoordIntPair());
					
					// Either way, we're done here
					return new ActionResult(EnumActionResult.PASS, stack);
				}
			}
		}
	}


	@Override
	public void onUpdate(ItemStack stack, World world, Entity entity, int animTick, boolean holdingItem) 	// Overhauled
	{
		if (world.isRemote) { return; } // Not doing this on client side
		
		this.tickCooldown(stack);
		
		if (TerritoryHandler.getSaveData().isDisplayReady()) 	
		{ 
			// Remember info about the chunk's control strength, so it can be displayed in the durability bar
			// Updating only once per second, for performance reasons (And because the item keeps flipping up and down, jittering, every time we update.)
			this.updateDisplayDurability(world, stack, entity);
		}	
	}
	
	
	@Override
	public boolean showDurabilityBar(ItemStack stack) { return true; }


	@Override
	public double getDurabilityForDisplay(ItemStack stack)
	{
		if (stack.hasTagCompound()) 
		{ 
			return stack.getTagCompound().getFloat("displayDurability"); 
		}
		// else, no info yet about any territories

		return 1.0f;	// Displaying 1.0 as "full damage", meaning an empty bar.
	}
	
	
	@Override
	public void registerRecipes() 
	{
		// One paper, one arrow, for one blank claim order. Nice 'n easy.
		GameRegistry.addShapelessRecipe(new ItemStack(this), 
				Items.PAPER, 
				Items.ARROW
		);
	}
	
	
	private String getNameOfFaction(ItemStack stack)
	{
		// We're storing the faction ID in the item after it has been signed by a leader
		if (stack.hasTagCompound())
		{
			String id = stack.getTagCompound().getString("factionID");
			
			if (id == null || id.isEmpty())
			{
				// ...eh?
				return null;
			}
			else
			{
				// What're you called...
				return TerritoryHandler.getNameOfFaction(UUID.fromString(id));
			}
		}
		else
		{
			// Has no faction ID tag, so cannot be signed
			return null;
		}
	}
	
	
	private UUID getFactionID(ItemStack stack)
	{
		if (stack.hasTagCompound())
		{
			String id = stack.getTagCompound().getString("factionID");
			
			if (id == null || id.isEmpty())
			{
				// ...eh?
				return null;
			}
			else
			{
				return UUID.fromString(id);
			}
		}
		else
		{
			return null;
		}
	}
	
	
	private void setFactionID(ItemStack stack, UUID id)
	{
		if (stack == null) { return; }
		if (id == null) { return; }
		
		if (!stack.hasTagCompound())
		{
			stack.setTagCompound(new NBTTagCompound());	// Init
		}
		
		stack.getTagCompound().setString("factionID", id.toString());	// Done
	}
	
	
	private boolean isReady(ItemStack stack)
	{
		if (stack == null) { return false; }
		
		if (stack.hasTagCompound())
		{
			int cooldown = stack.getTagCompound().getInteger("cooldown");
			
			return (cooldown <= 0);	// Is it?
		}
		else
		{
			// No tag, meaning this item is fresh
			return false;
		}
	}
	
	
	private void startCooldown(ItemStack stack)
	{
		if (stack == null) { return; }
		
		if (!stack.hasTagCompound())
		{
			stack.setTagCompound(new NBTTagCompound());	// Init
		}
		
		stack.getTagCompound().setInteger("cooldown", 20);	// 20 ticks = 1 sec, by default
	}
	
	
	private void tickCooldown(ItemStack stack)
	{
		if (stack == null) { return; }
		
		if (!stack.hasTagCompound())
		{
			stack.setTagCompound(new NBTTagCompound());	// Init
		}
		
		int cooldown = stack.getTagCompound().getInteger("cooldown");
		
		if (cooldown <= 0)
		{
			return;	// Nothing to be done.
		}
		
		stack.getTagCompound().setInteger("cooldown", cooldown - 1);	// One less.
	}
	
	
	/*private boolean isDisplayReady(ItemStack stack)
	{
		if (stack == null) { return false; }
		
		if (stack.hasTagCompound())
		{
			int cooldown = stack.getTagCompound().getInteger("cooldownDisplay");
			
			return (cooldown <= 0);	// Is it?
		}
		else
		{
			// No tag, meaning this item is fresh
			return false;
		}
	}
	
	
	private void startDisplayCooldown(ItemStack stack)
	{
		if (stack == null) { return; }
		
		if (!stack.hasTagCompound())
		{
			stack.setTagCompound(new NBTTagCompound());	// Init
		}
		
		stack.getTagCompound().setInteger("cooldownDisplay", 20);	// 20 ticks = 1 sec, by default
	}
	
	
	private void tickDisplayCooldown(ItemStack stack)
	{
		if (stack == null) { return; }
		
		if (!stack.hasTagCompound())
		{
			stack.setTagCompound(new NBTTagCompound());	// Init
		}
		
		int cooldown = stack.getTagCompound().getInteger("cooldownDisplay");
		
		stack.getTagCompound().setInteger("cooldownDisplay", cooldown - 1);	// One less.
	}*/
	
	
	private void updateDisplayDurability(World world, ItemStack stack, Entity player)
	{
		BlockPos pos = new BlockPos(player.posX, player.posY, player.posZ);
		Chunk chunk = world.getChunkFromBlockCoords(pos);
		
		if (!stack.hasTagCompound())
		{
			stack.setTagCompound(new NBTTagCompound());	// Init
		}
		
		// Is this chunk owned by anyone?
		_FactionSaveData faction = TerritoryHandler.getFactionByChunkPos(world.provider.getDimension(), chunk.getChunkCoordIntPair());		
		
		if (faction == null)
		{
			// No, so no durability
			stack.getTagCompound().setFloat("displayDurability", 1.0f);
		}
		else
		{
			// Yes, hand me that control strength
			int controlStrength = faction.getControlStrengthFromChunkPos(chunk.getChunkCoordIntPair());
			float displayStrength = 1.0f - (1.0f / Main.getChunkControlMax() * controlStrength);
			
			stack.getTagCompound().setFloat("displayDurability", displayStrength);
		}
	}
	
	
	private void informFactionMembers(World world, _FactionSaveData faction, int prevControlStrength, int controlStrength, int controlStrengthMax, ChunkPos pos) 
	{
		// Inform everyone of that faction that someone is nibbling on their territory. Do it at certain % strength values. Like 75%, 50%, 25%, 10%.
		
		int prevPercent = 100 / controlStrengthMax * prevControlStrength;
		int currentPercent = 100 / controlStrengthMax * controlStrength;
		
		if (prevPercent > 75 && currentPercent <= 75)
		{
			Main.sendMessageToFactionMembers(world, faction.getID(), "Warning! Chunk at x" + pos.chunkXPos + " z" + pos.chunkZPos + " at " + currentPercent + "% strength!");
		}
		else if (prevPercent > 50 && currentPercent <= 50)
		{
			Main.sendMessageToFactionMembers(world, faction.getID(), "Warning! Chunk at x" + pos.chunkXPos + " z" + pos.chunkZPos + " at " + currentPercent + "% strength!");
		}
		else if (prevPercent > 25 && currentPercent <= 25)
		{
			Main.sendMessageToFactionMembers(world, faction.getID(), "Warning! Chunk at x" + pos.chunkXPos + " z" + pos.chunkZPos + " at " + currentPercent + "% strength!");
		}
		else if (prevPercent > 10 && currentPercent <= 10)
		{
			Main.sendMessageToFactionMembers(world, faction.getID(), "Warning! Chunk at x" + pos.chunkXPos + " z" + pos.chunkZPos + " at " + currentPercent + "% strength!");
		}
	}
}
