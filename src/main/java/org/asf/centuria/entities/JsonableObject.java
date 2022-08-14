package org.asf.centuria.entities;

import java.lang.reflect.InvocationTargetException;

import com.google.gson.JsonObject;

public abstract class JsonableObject 
{

	public abstract JsonObject toJson();
	
	protected abstract void propagatePropertiesFromJson(JsonObject jsonObject);
	
	public JsonableObject CreateObjectFromJson(JsonObject jsonObject) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException
	{
		JsonableObject newObject = this.getClass().getDeclaredConstructor().newInstance();
		
		newObject.propagatePropertiesFromJson(jsonObject);
		
		return newObject;
	}
	
}
