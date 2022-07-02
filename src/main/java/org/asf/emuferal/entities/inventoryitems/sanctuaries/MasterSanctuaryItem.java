package org.asf.emuferal.entities.inventoryitems.sanctuaries;

import java.lang.reflect.InvocationTargetException;

import org.asf.emuferal.entities.components.generic.NameComponent;
import org.asf.emuferal.entities.components.generic.TimeStampComponent;
import org.asf.emuferal.entities.components.sanctuaries.PrimaryLookComponent;
import org.asf.emuferal.entities.components.sanctuaries.SanctuaryLookComponent;
import org.asf.emuferal.entities.inventoryitems.InventoryItem;

import com.google.gson.JsonObject;

public class MasterSanctuaryItem extends InventoryItem {

	public final static int InvType = 201;
	
	public MasterSanctuaryItem(int defId, String uuid) {
		super(defId, uuid, InvType);
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * Converts this item to a jsonObject with the correct format to be in an inventory.
	 */
	public JsonObject toJsonObject() {
		return super.toJsonObject();
	}

	/**
	 * Populates this item with properties from an inventory jsonObject of the same type.
	 */
	public void fromJsonObject(JsonObject object) throws InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		super.fromJsonObject(object);
	}
	
	public PrimaryLookComponent getPrimaryLookComponent()
	{
		return (PrimaryLookComponent)this.getComponent(PrimaryLookComponent.COMPONENT_NAME);
	}
	
	public SanctuaryLookComponent getSanctuaryLookComponent()
	{
		return (SanctuaryLookComponent)this.getComponent(SanctuaryLookComponent.COMPONENT_NAME);
	}
	
	public TimeStampComponent getTimeStampComponent()
	{
		return (TimeStampComponent)this.getComponent(TimeStampComponent.COMPONENT_NAME);
	}

	public NameComponent getNameComponent()
	{
		return (NameComponent)this.getComponent(NameComponent.COMPONENT_NAME);
	}
	
	public void setPrimaryLookComponent(PrimaryLookComponent component)
	{
		this.SetComponent(component);
	}
	
	public void setNameComponentComponent(NameComponent component)
	{
		this.SetComponent(component);
	}
	
	public void setTimeStampComponent(TimeStampComponent component)
	{
		this.SetComponent(component);
	}
	
	public void setNameComponent(NameComponent component)
	{
		this.SetComponent(component);
	}

	
	
}
