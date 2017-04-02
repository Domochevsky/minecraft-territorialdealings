package net.wwsf.domochevsky.territorialdealingsold;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityFireworkRocket;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.RecipeSorter;
import net.wwsf.domochevsky.territorialdealingsold.item.ClaimTerritoryOrder;
import net.wwsf.domochevsky.territorialdealingsold.item.FactionLedger;
import net.wwsf.domochevsky.territorialdealingsold.item.FactionOverviewCard;
import net.wwsf.domochevsky.territorialdealingsold.item.FactionRenameOrder;
import net.wwsf.domochevsky.territorialdealingsold.item.InheritanceDeed;
import net.wwsf.domochevsky.territorialdealingsold.item.PaymentOrder;
import net.wwsf.domochevsky.territorialdealingsold.item.PlayerIDCard;
import net.wwsf.domochevsky.territorialdealingsold.item.ReturnOrder;
import net.wwsf.domochevsky.territorialdealingsold.item.StartFactionDeed;
import net.wwsf.domochevsky.territorialdealingsold.item.TerritoryMap;
import net.wwsf.domochevsky.territorialdealingsold.net.PacketHandler;
import net.wwsf.domochevsky.territorialdealingsold.recipe.Recipe_LeaderRequired;
import net.wwsf.domochevsky.territorialdealingsold.recipe.Recipe_Payment;

//@Mod(modid="territorychevsky", name="Territorial Dealings", version="b17", acceptedMinecraftVersions="[1.9,1.11)")
public class Main
{
	private static boolean isDebugEnabled;			// Relevant for putting text onto the console

	private static boolean playerNeedsToBeOnline;	// If this is true then hostile chunks can only be claimed when someone from them is actually online
	private static int playerAmountForOnline;		// The minimum amount of players required if they need to be online

	private static String worldFolder;				// The name of the world. Should be unique, if I saw this right
	private static File configDir;					// The recommended config directory (/config/)

	private static Item startDeed;
	private static Item claimOrder;
	private static Item playerIDCard;
	private static Item paymentOrder;
	private static Item renameOrder;
	private static Item infoCard;
	private static Item inheritanceDeed;
	private static Item factionLedger;
	private static Item returnOrder;
	private static Item territoryMap;

	private static int maxControlStrength;		// Control strength per chunk. Effectively translates to second taken to take over someone else's chunks
	private static int controlStrengthRefresh;	// How much gets restored each day

	private static int controlRefreshTick;		// Once a day by default
	private static int consumeUpkeepTick;		// Consuming upkeep interval

	private static int saveTick;	// Autosave interval

	private static int upkeepMultiplier;		// Number of chunks times this much per chunk upkeep
	private static int maxPaymentOrderAmount;

	private static boolean allowPlayerInteract;	// If this is false then opening chests/doors is checked as well


	@EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
		Configuration config = new Configuration(event.getSuggestedConfigurationFile());	// Starting config

		config.load();	// And loading it up

			this.isDebugEnabled = config.get("generic", "Should I display a whole bunch of additional debug information on the server console? (default false)", false).getBoolean();
			this.saveTick = config.get("generic", "How often do I autosave, in addition to onServerStop? (default 24000 ticks, for once per ingame day.)", 24000).getInt();
			this.playerNeedsToBeOnline = config.get("generic", "Does someone from the target faction need to be online to start a hostile takeover? (default true)", true).getBoolean();
			this.playerAmountForOnline = config.get("generic", "How many members of the target faction need to be online for a hostile takeover? (Default 1% minimum)", 1).getInt();

			this.maxControlStrength = config.get("chunk control", "How long does it take to claim someone else's chunk? (default 90 sec)", 90).getInt();
			this.controlStrengthRefresh = config.get("chunk control", "How much lost chunk control strength gets restored every interval? (default +15 sec)", 15).getInt();
			this.controlRefreshTick = config.get("chunk control", "How long is each chunk control strength refresh interval? (default 24000 ticks, for once per ingame day.)", 24000).getInt();
			this.consumeUpkeepTick = config.get("chunk control", "How long is each upkeep consumption interval? (default 24000 ticks, for once per ingame day.)", 24000).getInt();
			this.upkeepMultiplier = config.get("chunk control", "How much upkeep does a chunk cost per upkeep consumption interval? (default 1 VALUABLE)", 1).getInt();

			this.maxPaymentOrderAmount = config.get("items", "How many valuables can the PAYMENT ORDER hold? (default 100 VALUABLES)", 100).getInt();

			ValueTable.setValueForDiamond(config.get("valuables", "How much value does a single diamond have? (default 2 VALUABLES)", 2).getInt());
			ValueTable.setValueForEmerald(config.get("valuables", "How much value does a single emerald have? (default 3 VALUABLES)", 3).getInt());
			ValueTable.setValueForGold(config.get("valuables", "How much value does a single gold ingot have? (default 1 VALUABLE)", 1).getInt());

			this.allowPlayerInteract = config.get("extra", "Are other players allowed to interact with chests/doors and such in protected chunks? (default false)", false).getBoolean();

		config.save();	// Done with config, saving it

		PacketHandler.initPackets();

		this.registerItems();

		if (event.getSide().isClient())
		{
			this.loadModels();
		}

		configDir = event.getModConfigurationDirectory();	// The directory we're saving our config to

		// Event listener
    	MinecraftForge.EVENT_BUS.register(new EventListener());

    	RecipeSorter.register("territorychevsky:recipehandler_payment", Recipe_Payment.class, RecipeSorter.Category.SHAPELESS, "after:minecraft:shapeless");
    	RecipeSorter.register("territorychevsky:recipehandler_leader", Recipe_LeaderRequired.class, RecipeSorter.Category.SHAPELESS, "after:minecraft:shapeless");

    	this.registerItemRecipes();
    	this.registerPaymentRecipes();
    }


	@EventHandler
    public void serverLoad(FMLServerStartingEvent event)
    {
		this.worldFolder = event.getServer().getFolderName();	// Hand me that world name

		SaveHandler.loadFactionsFromFile();
    }


	@EventHandler
    public void serverStopping(FMLServerStoppingEvent event)
    {
		SaveHandler.saveFactionsToFile();
    }


	private void registerItems()
	{
		this.startDeed = new StartFactionDeed();
		GameRegistry.register(this.startDeed);

		this.claimOrder = new ClaimTerritoryOrder();
		GameRegistry.register(this.claimOrder);

		this.paymentOrder = new PaymentOrder();
		GameRegistry.register(this.paymentOrder);

		this.renameOrder = new FactionRenameOrder();
		GameRegistry.register(this.renameOrder);

		this.playerIDCard = new PlayerIDCard();
		GameRegistry.register(this.playerIDCard);

		this.infoCard = new FactionOverviewCard();
		GameRegistry.register(this.infoCard);

		this.inheritanceDeed = new InheritanceDeed();
		GameRegistry.register(this.inheritanceDeed);

		this.factionLedger = new FactionLedger();
		GameRegistry.register(this.factionLedger);

		this.returnOrder = new ReturnOrder();
		GameRegistry.register(this.returnOrder);

		this.territoryMap = new TerritoryMap();
		GameRegistry.register(this.territoryMap);
	}


	private void loadModels()
	{
		// Registering custom models, based on metadata. JSON, man. What the fuck.
		ModelLoader.setCustomModelResourceLocation(this.startDeed, 0, new ModelResourceLocation("territorychevsky:startfactiondeed"));

		ModelLoader.setCustomModelResourceLocation(this.claimOrder, 0, new ModelResourceLocation("territorychevsky:claimorder"));

		ModelLoader.setCustomModelResourceLocation(this.paymentOrder, 0, new ModelResourceLocation("territorychevsky:paymentorder"));

		ModelLoader.setCustomModelResourceLocation(this.renameOrder, 0, new ModelResourceLocation("territorychevsky:factionnameorder"));

		ModelLoader.setCustomModelResourceLocation(this.playerIDCard, 0, new ModelResourceLocation("territorychevsky:playerid_blank"));
		ModelLoader.setCustomModelResourceLocation(this.playerIDCard, 1, new ModelResourceLocation("territorychevsky:playerid"));

		ModelLoader.setCustomModelResourceLocation(this.infoCard, 0, new ModelResourceLocation("territorychevsky:overviewcard"));

		ModelLoader.setCustomModelResourceLocation(this.inheritanceDeed, 0, new ModelResourceLocation("territorychevsky:inheritancedeed_blank"));
		ModelLoader.setCustomModelResourceLocation(this.inheritanceDeed, 1, new ModelResourceLocation("territorychevsky:inheritancedeed"));

		ModelLoader.setCustomModelResourceLocation(this.factionLedger, 0, new ModelResourceLocation("territorychevsky:factionledger_blank"));
		ModelLoader.setCustomModelResourceLocation(this.factionLedger, 1, new ModelResourceLocation("territorychevsky:factionledger"));

		ModelLoader.setCustomModelResourceLocation(this.returnOrder, 0, new ModelResourceLocation("territorychevsky:returnorder"));

		ModelLoader.setCustomModelResourceLocation(this.territoryMap, 0, new ModelResourceLocation("territorychevsky:territorymap"));
	}


	private void registerItemRecipes()
	{
		this.registerLeaderRecipes(new ItemStack(this.claimOrder), Items.PAPER, Items.ARROW);
		this.registerLeaderRecipes(new ItemStack(this.paymentOrder), Items.PAPER, Items.BOWL);
		this.registerLeaderRecipes(new ItemStack(this.renameOrder), Items.PAPER, Items.BLAZE_POWDER);
		this.registerLeaderRecipes(new ItemStack(this.inheritanceDeed), Items.PAPER, Items.GOLD_INGOT);
		this.registerLeaderRecipes(new ItemStack(this.factionLedger), Items.PAPER, Items.BOOK);
		this.registerLeaderRecipes(new ItemStack(this.returnOrder), Items.PAPER, Items.MAP);

		GameRegistry.addShapelessRecipe(new ItemStack(this.playerIDCard), new ItemStack(Items.PAPER), new ItemStack(Items.PAPER), new ItemStack(Items.WOODEN_SWORD));

		GameRegistry.addRecipe(new ItemStack(this.territoryMap), " o ", "oco", " o ",
                'o', new ItemStack(this.claimOrder),
                'c', Items.COMPASS
        );
	}


	// Recipes that can only be crafted if you are the leader of a faction
	// TODO: Extend this maybe later to allow for elevated privileges within a faction
	private void registerLeaderRecipes(ItemStack result, Item comp1, Item comp2)
	{
		if (result == null) { return; }
		List<ItemStack> list = new ArrayList<ItemStack>();

		if (comp1 != null) { list.add(new ItemStack(comp1)); }
		if (comp2 != null) { list.add(new ItemStack(comp2)); }

		if (!list.isEmpty())
		{
			GameRegistry.addRecipe(new Recipe_LeaderRequired(result, list));
		}
	}


	private void registerPaymentRecipes()
	{
		// Items
		this.registerPayment(new ItemStack(Items.EMERALD), ValueTable.getValueFromItem(Items.EMERALD));
		this.registerPayment(new ItemStack(Items.DIAMOND), ValueTable.getValueFromItem(Items.DIAMOND));
		this.registerPayment(new ItemStack(Items.GOLD_INGOT), ValueTable.getValueFromItem(Items.GOLD_INGOT));

		// Blocks
		this.registerPayment(new ItemStack(Item.getItemFromBlock(Blocks.EMERALD_BLOCK)), ValueTable.getValueFromBlock(Blocks.EMERALD_BLOCK));
		this.registerPayment(new ItemStack(Item.getItemFromBlock(Blocks.DIAMOND_BLOCK)), ValueTable.getValueFromBlock(Blocks.DIAMOND_BLOCK));
		this.registerPayment(new ItemStack(Item.getItemFromBlock(Blocks.GOLD_BLOCK)), ValueTable.getValueFromBlock(Blocks.GOLD_BLOCK));
	}


	// For payment recipes only
	private void registerPayment(ItemStack component, int value)
	{
		ArrayList list = new ArrayList();

		ItemStack resultStack = new ItemStack(this.paymentOrder, 1, OreDictionary.WILDCARD_VALUE);

		list.add(resultStack);
		list.add(component);

		GameRegistry.addRecipe(new Recipe_Payment(new ItemStack(this.paymentOrder), list, value));
	}


	// Just for convenience
	public static void console(String text)
	{
		if (text == null) { return; }		// Nothing to say?
		if (!isDebugEnabled) { return; }	// Not putting anything out

		System.out.println(text);
	}


	public static void sendMessageToPlayer(EntityPlayer player, String msg)
	{
		player.addChatMessage(new TextComponentString(msg));
	}


	public static void startFireworks(Entity entity)
	{
		//ItemStack stack = new ItemStack(Items.firework_charge);	// Used to give fireworks something to blow up with, it seems

		//stack.setTagCompound(new NBTTagCompound());

		//NBTTagCompound nbttagcompound = stack.getTagCompound();
		//nbttagcompound.setTag("Fireworks", new NBTTagCompound());

		//nbttagcompound.getCompoundTag("Fireworks").setByte("Flight", (byte) 15);

		// SFX
		EntityFireworkRocket entityfireworkrocket = new EntityFireworkRocket(entity.worldObj, entity.posX, entity.posY, entity.posZ, null);
		entity.worldObj.spawnEntityInWorld(entityfireworkrocket);

		// Sound
		entity.worldObj.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK, entity.posX, entity.posY - 0.3D, entity.posZ, new Random().nextGaussian() * 0.05D, -entity.motionY * 0.5D, new Random().nextGaussian() * 0.05D, new int[0]);
	}


	public static int getControlRefreshTick() { return controlRefreshTick; }


	public static int getSaveTick() { return saveTick; }


	public static int getConsumeUpkeepTick() { return consumeUpkeepTick; }


	public static int getStrengthRefreshAmount() { return controlStrengthRefresh; }


	public static int getMaxControlStrength() { return maxControlStrength; }


	public static File getConfigDir() { return configDir; }
	public static String getConfigDirPath() { return configDir.getPath(); }	// Relative path
	public static String getWorldFolder() { return worldFolder; }


	public static boolean doesPlayerNeedToBeOnline() { return playerNeedsToBeOnline; }


	public static int getUpkeepMultiplier() { return upkeepMultiplier; }


	public static double getRequiredPlayerAmount() { return playerAmountForOnline; }


	public static boolean isDebugEnabled() { return isDebugEnabled; }


	public static Item getPaymentOrder() { return paymentOrder; }


	public static int getMaxPaymentOrderAmount() { return maxPaymentOrderAmount; }


	public static boolean isPlayerInteractionAllowed() { return allowPlayerInteract; }
}
