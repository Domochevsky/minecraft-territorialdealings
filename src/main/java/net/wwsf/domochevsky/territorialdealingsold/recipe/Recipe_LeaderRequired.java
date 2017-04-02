package net.wwsf.domochevsky.territorialdealingsold.recipe;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;
import net.wwsf.domochevsky.territorialdealingsold.Main;
import net.wwsf.domochevsky.territorialdealingsold.item.PaymentOrder;
import net.wwsf.domochevsky.territorialdealingsold.net.ClientHelper;

public class Recipe_LeaderRequired extends ShapelessRecipes implements IRecipe
{
	public Recipe_LeaderRequired(ItemStack stack, List list)
	{
		super(stack, list);
	}


	@Override
	public boolean matches(InventoryCrafting matrix, World world)
    {
        ArrayList arraylist = new ArrayList(this.recipeItems);

        for (int column = 0; column < 3; ++column)
        {
            for (int row = 0; row < 3; ++row)
            {
                ItemStack itemstack = matrix.getStackInRowAndColumn(row, column);

                if (itemstack != null)
                {
                    boolean flag = false;
                    Iterator<ItemStack> iterator = arraylist.iterator();

                    while (iterator.hasNext())
                    {
                        ItemStack requiredItemStack = iterator.next();

                        if (itemstack.getItem() == requiredItemStack.getItem() && (requiredItemStack.getItemDamage() == OreDictionary.WILDCARD_VALUE || itemstack.getItemDamage() == requiredItemStack.getItemDamage()))
                        {
                            flag = true;
                            arraylist.remove(requiredItemStack);
                            break;
                        }
                    }

                    if (!flag)
                    {
                        return false;
                    }
                }
                // else, there's nothing in that slot
            }
        }

        if (arraylist.isEmpty() && world.isRemote)
        {
        	if (this.getRecipeOutput().getItem() instanceof PaymentOrder && this.hasPaymentOrderInMatrix(matrix))
			{
            	return true;	// Only leaders can craft this item, but refilling is free
			}
        	else
        	{
        		// All items were found and we're on client side. Asking the server now if you're a leader (Can't craft this if you aren't)
        		return ClientHelper.isPlayerLeader(Minecraft.getMinecraft().thePlayer.dimension, Minecraft.getMinecraft().thePlayer.getEntityId());
        		//return false;
        	}
        }
        else if (!world.isRemote)
        {
        	// Server side. How do I get the player instance?
        	// Answer: I don't. :|
        	// I could get the player list and see which one has a container open and THEN seeing which one has the items in the matrix listed here?
        	Main.console("[Territorial Dealings - Server] Can't check if this player is allowed to craft this item, since I don't know who asked for this recipe on this side. :/");
        }

        return arraylist.isEmpty();
    }


	private boolean hasPaymentOrderInMatrix(IInventory matrix)
	{
		int counter = 0;

		while (counter < matrix.getSizeInventory())
		{
			if (matrix.getStackInSlot(counter) != null && matrix.getStackInSlot(counter).getItem() instanceof PaymentOrder)
			{
				return true;	// Found it
			}

			counter += 1;
		}

		return false;
	}
}
