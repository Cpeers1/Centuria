package org.asf.centuria.entities.inventoryitems;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.MarkerManager;
import org.asf.centuria.Centuria;
import org.asf.centuria.enums.inventory.InventoryType;
import org.reflections.Reflections;

/**
 * This class is responsible for holding a type register of all items.
 * It retrieves the items's class type from its inventory type. 
 * @author Owenvii
 *
 */
public final class InventoryItemManager {
	
	private static final String INVENTORY_ITEM_PACKAGE_NAME = "org.asf.centuria.entities.inventoryitems";
	
	private static Map<InventoryType, Class<InventoryItem>> itemRegister = new HashMap<InventoryType, Class<InventoryItem>>();
	
	/**
	 * This method registers all items (Items marked with the Item Annotation) into the register.
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public static void registerAllItems() throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException
	{
		var classes = new Reflections(INVENTORY_ITEM_PACKAGE_NAME).getTypesAnnotatedWith(Item.class);
		
		for(var classObject : classes)
		{
			@SuppressWarnings("unchecked")
			var itemClass = (Class<InventoryItem>)classObject;
			
			var itemType = itemClass.getDeclaredConstructor().newInstance().getInventoryType();
			
			Centuria.logger.debug(MarkerManager.getMarker("ITEMS"), "Loading item into item register: " + itemType.toString());			
			itemRegister.put(itemType, itemClass);	
		}
	}
	
	public static Class<InventoryItem> getItemTypeFromInvType(InventoryType invType)
	{
		return itemRegister.get(invType);
	}
}
