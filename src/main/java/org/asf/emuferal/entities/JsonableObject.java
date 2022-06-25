package org.asf.emuferal.entities;

import java.lang.reflect.InvocationTargetException;

import com.google.gson.JsonObject;

public abstract class JsonableObject 
{

	public abstract JsonObject toJson();
	
	protected abstract JsonObject propagatePropertiesFromJson(JsonObject jsonObject);
	
	public JsonableObject CreateObjectFromJson(JsonObject jsonObject) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException
	{
		JsonableObject newObject = this.getClass().getDeclaredConstructor().newInstance();
		
		newObject.propagatePropertiesFromJson(jsonObject);
		
		return newObject;
	}
	
}
