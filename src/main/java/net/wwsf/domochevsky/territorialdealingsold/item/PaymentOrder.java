package net.wwsf.domochevsky.territorialdealingsold.item;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.wwsf.domochevsky.territorialdealingsold.Main;
import net.wwsf.domochevsky.territorialdealingsold.TerritoryHandler;
import net.wwsf.domochevsky.territorialdealingsold._Territory;

public class PaymentOrder extends LeaderRequiredItem
{
	public PaymentOrder()
	{
		this.setMaxStackSize(1);	// only one per
		this.setFull3D();
		this.setCreativeTab(CreativeTabs.TOOLS);

		this.setRegistryName("paymentorder");
		this.setUnlocalizedName("territorychevsky_paymentorder");
	}


	@Override
	public String getItemStackDisplayName(ItemStack stack) { return "Payment Order"; }


	@Override
	public ActionResult<ItemStack> onItemRightClick(ItemStack stack, World world, EntityPlayer player, EnumHand hand)
	{
		if (world.isRemote) { return new ActionResult(EnumActionResult.PASS, stack); }	// Not doing this on client side

		// TODO: Change from damage to nbt, since damage directly affects the "model" apparently

		int amount = this.getDamage(stack);

		if (this.addUpkeepToFaction(amount, player))
		{
			stack.stackSize -= 1;	// Successful, so consuming it now
			//world.playAuxSFXAtEntity(player, "random.pop", 0.5f, 2.0f);	// SFX
		}

		return new ActionResult(EnumActionResult.PASS, stack);
	}


	private boolean addUpkeepToFaction(int amount, EntityPlayer player)
	{
		if (player == null) { return false; }	// ...wha?
		if (amount <= 0) { return false; }		// Adding nothing?

		_Territory faction = TerritoryHandler.getFactionPlayerIsLeaderOf(player);

		if (faction == null) { return false; }	// Not the leader of any faction

		Main.console("[TD - Payment Order] Player is leader of faction " + faction.getFactionName() + ". Adding " + amount + " valuables upkeep.");

		faction.addUpkeep(player, amount);	// Cha-ching

		return true;	// Done
	}


	@SideOnly(Side.CLIENT)
	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean dbg)
	{
		super.addInformation(stack, player, list, dbg);

		list.add("'Dues where they're due.'");
		list.add("Use this to add valuables");
		list.add("to your faction's vault.");

		list.add("§9VALUABLES: " + stack.getItemDamage() + " / " + Main.getMaxPaymentOrderAmount());

		list.add("§eCraft with emerald, diamond");
		list.add("§eor gold to add valuables.");

		list.add("§cOnly usable by the LEADER.");
    }


	@Override
	public boolean showDurabilityBar(ItemStack stack) { return false; }
}
