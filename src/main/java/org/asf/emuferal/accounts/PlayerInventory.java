package org.asf.emuferal.accounts;

import com.google.gson.JsonElement;

import org.asf.emuferal.accounts.highlevel.AvatarAccessor;
import org.asf.emuferal.accounts.highlevel.ClothingItemAccessor;
import org.asf.emuferal.accounts.highlevel.CurrencyAccessor;
import org.asf.emuferal.accounts.highlevel.DyeAccessor;
import org.asf.emuferal.accounts.highlevel.FurnitureItemAccessor;
import org.asf.emuferal.accounts.highlevel.InteractionMemoryAccessor;
import org.asf.emuferal.accounts.highlevel.InventoryAccessor;
import org.asf.emuferal.accounts.highlevel.ItemAccessor;
import org.asf.emuferal.accounts.highlevel.SanctuaryAccessor;
import org.asf.emuferal.accounts.highlevel.impl.AvatarAccessorImpl;
import org.asf.emuferal.accounts.highlevel.impl.ClothingItemAccessorImpl;
import org.asf.emuferal.accounts.highlevel.impl.DyeAccessorImpl;
import org.asf.emuferal.accounts.highlevel.impl.FurnitureItemAccessorImpl;
import org.asf.emuferal.accounts.highlevel.impl.InteractionMemoryAccessorImpl;
import org.asf.emuferal.accounts.highlevel.impl.SanctuaryAccessorImpl;
import org.asf.emuferal.players.Player;

public abstract class PlayerInventory {

	protected InventoryAccessor accessor = new InventoryAccessor(this);
	protected InteractionMemoryAccessor iAccessor = new InteractionMemoryAccessorImpl(this);
	protected ClothingItemAccessor clAccessor = new ClothingItemAccessorImpl(this);
	protected FurnitureItemAccessor fAccessor = new FurnitureItemAccessorImpl(this);
	protected SanctuaryAccessor sAccessor = new SanctuaryAccessorImpl(this);
	protected CurrencyAccessor cAccessor = new CurrencyAccessor(this);
	protected AvatarAccessor aAccessor = new AvatarAccessorImpl(this);
	protected DyeAccessor dAccessor = new DyeAccessorImpl(this);

	/**
	 * Retrieves the high-level inventory accessor
	 * 
	 * @return Accessor instance
	 */
	public InventoryAccessor getAccessor() {
		return accessor;
	}

	/**
	 * Retrieves the high-level interaction memory accessor
	 * 
	 * @return InteractionMemoryAccessor instance
	 */
	public InteractionMemoryAccessor getInteractionMemory() {
		return iAccessor;
	}

	/**
	 * Retrieves the high-level avatar accessor
	 * 
	 * @return AvatarAccessor instance
	 */
	public AvatarAccessor getAvatarAccessor() {
		return aAccessor;
	}

	/**
	 * Retrieves the high-level dye item accessor
	 * 
	 * @return DyeAccessor instance
	 */
	public DyeAccessor getDyeAccessor() {
		return dAccessor;
	}

	/**
	 * Retrieves the high-level sanctuary accessor
	 * 
	 * @return SanctuaryAccessor instance
	 */
	public SanctuaryAccessor getSanctuaryAccessor() {
		return sAccessor;
	}

	/**
	 * Retrieves the high-level clothing item accessor
	 * 
	 * @return ClothingItemAccessor instance
	 */
	public ClothingItemAccessor getClothingAccessor() {
		return clAccessor;
	}

	/**
	 * Retrieves the high-level furniture item accessor
	 * 
	 * @return FurnitureItemAccessor instance
	 */
	public FurnitureItemAccessor getFurnitureAccessor() {
		return fAccessor;
	}

	/**
	 * Retrieves the high-level currency accessor
	 * 
	 * @return CurrencyAccessor instance
	 */
	public CurrencyAccessor getCurrencyAccessor() {
		return cAccessor;
	}

	/**
	 * Retrieves the high-level item accessor
	 * 
	 * @param player Player for which to retrieve the item accessor for
	 * @return ItemAccessor instance
	 */
	public ItemAccessor getItemAccessor(Player player) {
		return new ItemAccessor(this, player);
	}

	/**
	 * Retrieves a item from the player's inventory
	 * 
	 * @param itemID Inventory item ID
	 * @return JsonElement instance or null
	 */
	public abstract JsonElement getItem(String itemID);

	/**
	 * Saves a item to the player inventory
	 * 
	 * @param itemID   Inventory item ID
	 * @param itemData Item data
	 */
	public abstract void setItem(String itemID, JsonElement itemData);

	/**
	 * Deletes a item from the player inventory
	 * 
	 * @param itemID Inventory item ID
	 */
	public abstract void deleteItem(String itemID);

	/**
	 * Checks if a inventory item is present
	 * 
	 * @param itemID Inventory item ID
	 * @return True if the item is present, false otherwise
	 */
	public abstract boolean containsItem(String itemID);
}
