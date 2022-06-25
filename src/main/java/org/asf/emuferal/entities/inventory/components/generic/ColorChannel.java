package org.asf.emuferal.entities.inventory.components.generic;

import com.google.gson.JsonObject;

/**
 * Class for working with a color (HSV) channel in the inventory.
 * @author Owenvii
 *
 */
public class ColorChannel {
	
	public static String hsvPropertyName = "_hsv";

	public String hsv;
	
	public JsonObject ToJson()
	{
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty(hsvPropertyName, hsv);
		
		return jsonObject;
	}
	
	public static ColorChannel fromJson(JsonObject json)
	{
		return new ColorChannel(json.get(hsvPropertyName).getAsString());
	}
	
	public ColorChannel(String hsv)
	{
		this.hsv = hsv;
	}
	
	//TODO: individual modifiers for h, s, and v.
}
