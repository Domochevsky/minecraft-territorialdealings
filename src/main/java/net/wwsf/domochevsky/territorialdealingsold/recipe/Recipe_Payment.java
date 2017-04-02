package net.wwsf.domochevsky.territorialdealingsold.recipe;

import java.util.List;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.wwsf.domochevsky.territorialdealingsold.Main;
import net.wwsf.domochevsky.territorialdealingsold.item.PaymentOrder;

public class Recipe_Payment extends ShapelessRecipes implements IRecipe
{
	private int valueAdd;	// The value this recipe will add to the payment


	public Recipe_Payment(ItemStack result, List components, int value)
	{
		super(result, components);

		this.valueAdd = value;
	}


	@Override
	public ItemStack getCraftingResult(InventoryCrafting matrix)
    {
		ItemStack stack = this.getRecipeOutput().copy();
		int amount = this.getDeedAmountFromMatrix(matrix);

		if (amount + this.valueAdd > Main.getMaxPaymentOrderAmount()) { return null; }	// Would be more than full, so not having that

		stack.setItemDamage(amount + this.valueAdd);	// Added value

        return stack;
    }


	private int getDeedAmountFromMatrix(InventoryCrafting matrix)
	{
		int counter = 0;

		while (counter < matrix.getSizeInventory())
		{
			if (matrix.getStackInSlot(counter) != null && matrix.getStackInSlot(counter).getItem() instanceof PaymentOrder)
			{
				return matrix.getStackInSlot(counter).getItemDamage();	// Found it
			}

			counter += 1;
		}

		return 0;	// Couldn't find it? How'd that happen
	}
}
