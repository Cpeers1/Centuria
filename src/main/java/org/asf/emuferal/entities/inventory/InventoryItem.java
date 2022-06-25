package org.asf.emuferal.entities.inventory;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.asf.emuferal.entities.inventory.components.ComponentManager;
import org.asf.emuferal.entities.inventory.components.InventoryItemComponent;

import com.google.gson.JsonObject;

/**
 * The base class for all inventory items.
 * The idea behind this is you want to code abstract methods to add the required components to the item.
 * This class takes care of converting back and forth from JsonObjects.
 * @author Owenvii
 */
public class InventoryItem 
{
	//json constants
	private static String defIdPropertyName = "defId";
	private static String uuidPropertyName = "id";
	private static String invTypePropertyName = "type";
	private static String componentsPropertyName = "components";
	
	//object variables
	public int defId;
	public String uuid;
	public int invType;
	private List<InventoryItemComponent> components = new ArrayList<InventoryItemComponent>();
	
	/**
	 * Converts a jsonObject to an inventory item.
	 * This method is meant to be used by a extending class.
	 * Call the super method to populate default properties.
	 * @param object The jsonObject to retrieve properties from.
	 * @return The inventory item after populating the default values.
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	protected static InventoryItem fromJsonObject(JsonObject object) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException
	{
		//this continues adding the fields from the object.
		
		var defId = object.get(defIdPropertyName).getAsInt();
		var uuid = object.get(uuidPropertyName).getAsString();
		var invType = object.get(invTypePropertyName).getAsInt();
		InventoryItem item = new InventoryItem(defId, uuid, invType);
		
		//get components
		
		JsonObject componentLevel = object.get("components").getAsJsonObject();
		
		for(var componentJson : componentLevel.entrySet())
		{
			var type = ComponentManager.getComponentTypeFromComponentName(componentJson.getKey());
			item.components.add(InventoryItemComponent.fromJson(type, componentJson.getValue().getAsJsonObject()));
		}
	
		//return new item.
		return item;
	}
	
	/**
	 * Base constructor for all inventory items.
	 * @param defId The defId of the inventory item.
	 * @param uuid The unique identifier of the item.
	 * @param invType The inventory number/type this item belongs in.
	 */
	public InventoryItem(int defId, String uuid, int invType)
	{
		this.defId = defId;
		this.uuid = uuid;
		this.invType = invType;
	}

	/**
	 * Converts this instance of the inventory object to a json object.
	 * This method is meant to be used by a extending class.
	 * Call the super method to populate default properties.
	 * @return The jsonObject after populating the default values.
	 */
	public JsonObject toJsonObject()
	{
		//this continues adding the fields to the object.
		var newObject = new JsonObject();
		newObject.addProperty(defIdPropertyName, defId);
		newObject.addProperty(uuidPropertyName, uuid);
		newObject.addProperty(invTypePropertyName, invType);
		
		var componentObject = new JsonObject();
		
		for(var component : components )
		{
			var componentChildObject = component.toJson();
			componentObject.add(component.getComponentName(), componentChildObject);
		}
		
		//add component object
		newObject.add(componentsPropertyName, componentObject);
		
		return newObject;
	}
	
	/**
	 * This method is used to add components to the item.
	 * This is meant for INTERNAL USE!
	 * Write your own item wrapper and use this method to add components to it.
	 * @param component
	 */
	protected void AddComponent(InventoryItemComponent component)
	{
		//adds the component..
		components.add(component);
	}
	
	/**
	 * Gets a component give its component name.
	 * @param componentName The name of the component.
	 * @return
	 */
	protected InventoryItemComponent GetComponent(String componentName)
	{
		InventoryItemComponent result = null;
		
		for(var component : components)
		{
			if(component.getComponentName() == componentName)
			{
				result = component;
				break;
			}
		}
		
		return result;
	}

}
