package org.asf.centuria.enums.inventory;

import java.util.HashMap;
import java.util.Map;

public enum InventoryType {

	AvatarClass(1, InventoryStorageType.SINGLE_ITEM), 
	BodyPart(2, InventoryStorageType.SINGLE_ITEM),
	Decal(3),
	Eye(4), 
	House(5, InventoryStorageType.OBJECT_BASED),
	Land(6, InventoryStorageType.OBJECT_BASED),
	Enigma(7, InventoryStorageType.OBJECT_BASED),
	Inspiration(8, InventoryStorageType.SINGLE_ITEM),
	AvatarAction(9),
	SanctuaryClass(10, InventoryStorageType.SINGLE_ITEM),
	SanctuaryLight(11),
	SeasonPass(12),
	Clothing(100, InventoryStorageType.OBJECT_BASED),
	Furniture(102, InventoryStorageType.OBJECT_BASED),
	Ingredient(103, InventoryStorageType.QUANTITY_BASED),
	Currency(104, InventoryStorageType.QUANTITY_BASED),
	Glamour(105),
	Charm(106),
	Recipe(107),
	Spell(108),
	Pet(109),
	Twiggle(110),
	Dye(111, InventoryStorageType.QUANTITY_BASED),
	Time(112),
	Seed(113),
	AvatarLook(200),
	SanctuaryLook(201),
	Quest(300),
	Challenge(301),
	Gift(302),
	UserVar(303),
	RoomScript(304),
	Objective(305),
	Task(310),
	LinearQuestCompletion(311),
	BundlePack(314),
	ExtraLookSlot(315),
	Retry(316),
	Unlock(400);
	
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
