package org.asf.emuferal.enums.inventory;

import java.util.HashMap;
import java.util.Map;

public enum InventoryType {

	AvatarSpeciesList(1, InventoryStorageType.SINGLE_ITEM), 
	ModsAndWings(2, InventoryStorageType.SINGLE_ITEM), 
	Houses(5, InventoryStorageType.OBJECT_BASED),
	Islands(6, InventoryStorageType.OBJECT_BASED),
	Enigmas(7, InventoryStorageType.OBJECT_BASED),
	Inspirations(8, InventoryStorageType.SINGLE_ITEM),
	Emotes(9),
	SanctuaryClasses(10, InventoryStorageType.SINGLE_ITEM),
	Clothes(100, InventoryStorageType.OBJECT_BASED),
	Furniture(102, InventoryStorageType.OBJECT_BASED),
	Resources(103, InventoryStorageType.QUANTITY_BASED),
	Currency(104, InventoryStorageType.QUANTITY_BASED),
	Twiggles(110),
	Dyes(111, InventoryStorageType.QUANTITY_BASED),
	Looks(200),
	SanctuaryLooks(201),
	Settings(303),
	HarvestingAndDailys(304),
	Quests(311),
	Unlocks(400);
	
	public int invTypeId;
	public InventoryStorageType storageType;
	
    // Reverse-lookup map 
    private static final Map<Integer, InventoryType> lookup = new HashMap<Integer, InventoryType>();

    static {
        for (InventoryType d : InventoryType.values()) {
            lookup.put(d.invTypeId, d);
        }
    }

	InventoryType(int value, InventoryStorageType storageType) {
		this.invTypeId = value;
		this.storageType = storageType;
	}
	
	InventoryType(int value) {
		this.invTypeId = value;
		this.storageType = InventoryStorageType.OBJECT_BASED;
	}
	
    public static InventoryType get(int type) {
        return lookup.get(type);
    }
}
