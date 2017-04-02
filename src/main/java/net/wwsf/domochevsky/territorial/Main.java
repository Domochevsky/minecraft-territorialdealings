package net.wwsf.domochevsky.territorial;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityFireworkRocket;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.wwsf.domochevsky.territorial.items.ClaimTerritoryOrder;
import net.wwsf.domochevsky.territorial.items.FactionRenameOrder;
import net.wwsf.domochevsky.territorial.items.StartFactionDeed;
import net.wwsf.domochevsky.territorial.items._ItemBase;
import net.wwsf.domochevsky.territorial.savedata._SaveData;

@Mod(modid=Main.modID, name="Territorial Dealings", version="b18", acceptedMinecraftVersions="[1.9,1.11)")
public class Main 
{
	// Let's overhaul this... make it user-based instead, keeping track of said users and what factions they're part of AND what their role is in them.
	// Also separate save data from logic, for territories and chunks
	// Additionally, get away from leader-only items, since crafting them cannot be reliably checked on both server and client side.
	// Also, let items register and load their own models and recipes. (Likely be deriving them all from a base item)
	
	public static final String modID = "territorychevsky";	// For ease of access
	
	private static boolean isDebugEnabled = true;		// Relevant for putting text onto the console
	
	private static boolean checkInteraction = true;		// If this is true then opening chests/doors will be checked as well
	
	private static boolean allowAttacks = true;			// If this is false then claimed chunks cannot be attacked.
	private static boolean playerNeedsToBeOnline = true;// If this is true then hostile chunks can only be claimed when someone from them is actually online
	private static int playerAmountForOnline = 1;		// The minimum amount of players required if they need to be online, in percent
	
	private static boolean consumeUpkeep = true;		// If this is false then no upkeep will be consumed for chunk control at all
	private static int consumeUpkeepTick = 24000;		// When to check all chunks for upkeep costs. Once a day by default.
	private static int upkeepCost = 1;					// How much upkeep this costs per chunk.
	
	private static int chunkControlMax = 90;			// Control strength per chunk. Effectively translates to seconds taken to take over someone else's chunks.
	private static int controlRefreshTick = 24000;		// Once a day by default. Determines when to refresh chunk control strength.
	private static int controlStrengthRefresh = 15;		// How much gets restored each day. Can be considered to be in seconds, since the claim order can only try every 20 ticks (1 sec)
														// More claimants can make that happen faster
	
	private static int saveTickInterval = 24000;		// Autosave interval. Default once per ingame day.
	
	private static ArrayList<_ItemBase> items;
	
	private static String worldDir;			// The name of the world. Should be unique, if I saw this right
	private static String configDir;		// The directory we're saving our config to
	
	
	@EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
		configDir = event.getModConfigurationDirectory().getPath();	
		
		// TODO: Load and save the config here
		
		MinecraftForge.EVENT_BUS.register(new EventListener());
		
		this.registerItems();
		
		if (event.getSide().isClient())
		{
			// This is gonna crash if done on server-side, so let's not.
			this.registerModels();
		}
		
		this.registerRecipes();
    }
	
	
	@EventHandler
    public void serverLoad(FMLServerStartingEvent event)
    {
		this.worldDir = event.getServer().getFolderName();	// Hand me that world name

		_SaveData save = SaveHandler.loadFactionsFromFile(Main.configDir, Main.worldDir);
		
		if (save == null)
		{
			save = new _SaveData();	// Init
		}
		
		TerritoryHandler.setSaveData(save);
    }


	@EventHandler
    public void serverStopping(FMLServerStoppingEvent event)
    {
		SaveHandler.saveFactionsToFile(Main.configDir, Main.worldDir, TerritoryHandler.getSaveData());
    }
	
	
	private void registerItems()
	{
		items = new ArrayList<_ItemBase>();	// Fresh
		
		this.registerItems_2(new StartFactionDeed());		// Creates a new faction.
		this.registerItems_2(new ClaimTerritoryOrder());	// Claims territories in the name of a particular faction.
		this.registerItems_2(new FactionRenameOrder());		// Renames the faction you are the leader of
		// TODO - Register more item
	}
	
	
	private void registerItems_2(_ItemBase item)
	{
		GameRegistry.register(item);
		items.add(item);
	}
	
	
	private void registerModels()
	{
		Iterator<_ItemBase> it = items.iterator();
		
		while (it.hasNext())
		{
			it.next().registerModel();	// Register your own damn model!
		}
	}
	
	
	private void registerRecipes()
	{
		Iterator<_ItemBase> it = items.iterator();
		
		while (it.hasNext())
		{
			it.next().registerRecipes();	// Register your own damn model!
		}
	}
	
	
	// Just for convenience
	public static void console(String text)
	{
		if (!isDebugEnabled) { return; }					// Not putting anything out
		if (text == null || text.isEmpty()) { return; }		// Nothing to say?

		System.out.println("[TERRITORIAL DEALINGS] " + text);
	}
	
	
	public static void sendMessageToPlayer(EntityPlayer player, String msg)
	{
		player.addChatMessage(new TextComponentString(msg));
	}
	
	
	public static void sendMessageToFactionMembers(World world, UUID factionID, String msg)
	{
		if (factionID == null) { return; }
		if (msg == null || msg.isEmpty()) { return; }
		
		Iterator<EntityPlayer> it = world.playerEntities.iterator();
		
		while (it.hasNext())
		{
			EntityPlayer player = it.next();
			
			if (TerritoryHandler.isPlayerMemberOfFaction(factionID, player, false))
			{
				// Not informing creative mode players about this every time.
				sendMessageToPlayer(player, msg);
			}
		}
	}
	
	
	public static void startFireworks(Entity entity)
	{
		// SFX
		EntityFireworkRocket entityfireworkrocket = new EntityFireworkRocket(entity.worldObj, entity.posX, entity.posY, entity.posZ, null);
		entity.worldObj.spawnEntityInWorld(entityfireworkrocket);

		// Sound
		entity.worldObj.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK, entity.posX, entity.posY - 0.3D, entity.posZ, new Random().nextGaussian() * 0.05D, 
				-entity.motionY * 0.5D, new Random().nextGaussian() * 0.05D, 
				new int[0]);
	}
	
	
	static void checkAutoSave()
	{
		int saveTick = TerritoryHandler.getSaveData().getSaveTick();
		int maxTick = saveTickInterval;
		
		if (saveTick < maxTick)
		{
			// Not yet
			TerritoryHandler.getSaveData().setSaveTick(saveTick + 1);
			
			return;
		}
		else
		{
			// Reset
			TerritoryHandler.getSaveData().setSaveTick(0);
		}
		
		console("Autosaving...");
		
		SaveHandler.saveFactionsToFile(configDir, worldDir, TerritoryHandler.getSaveData());
	}
	
	
	static boolean shouldCheckInteraction()	{ return checkInteraction; }
	
	
	static boolean shouldCheckUpkeep() { return consumeUpkeep; }
	public static int getUpkeepCost() { return upkeepCost; }
	public static int getChunkControlMax() { return chunkControlMax; }
	public static int getUpkeepTickMax() { return consumeUpkeepTick; }
	
	public static int getChunkTickMax() { return controlRefreshTick; }
	public static int getChunkControlRefreshValue() { return controlStrengthRefresh; }	
	
	public static boolean allowAttacks() { return allowAttacks; }
	public static boolean doesPlayerNeedToBeOnline() { return playerNeedsToBeOnline; }
	public static int getRequiredPlayerAmount() { return playerAmountForOnline; }


	static void checkDisplayTick() 
	{
		int currentTick = TerritoryHandler.getSaveData().getDisplayTick();
		
		if (currentTick > 0)
		{
			// Ticking down
			TerritoryHandler.getSaveData().setDisplayTick(currentTick - 1);
		}
		else
		{
			// At 0, so resetting back to max
			TerritoryHandler.getSaveData().setDisplayTick(20);	// Once per second
		}
	}
}
