package org.asf.emuferal.entities;

public class Factory<T> {

	public interface InventoryItemComponentFactory<T>
	{
		T create();
	}	
}
