package org.asf.emuferal.entities.sanctuaries;

import com.google.gson.JsonObject;

public class RoomInfoObject {

	public int roomIndex;
	public double brightness;
	public String color;
	public double rotation;
	public String roomName;
	
	public JsonObject toJson()
	{
		var jsonObject = new JsonObject();
		jsonObject.addProperty("roomIndex", roomIndex);
		jsonObject.addProperty("brightness", brightness);
		jsonObject.addProperty("color", color);
		jsonObject.addProperty("rotation", rotation);
		jsonObject.addProperty("roomName", roomName);
		
		return jsonObject;
	}
	
	public RoomInfoObject(JsonObject originalObject)
	{
		this.roomIndex = originalObject.get("roomIndex").getAsInt();
		this.brightness = originalObject.get("brightness").getAsDouble();
		this.color = originalObject.get("color").getAsString();
		this.rotation = originalObject.get("rotation").getAsDouble();
		this.roomName = originalObject.get("roomName").getAsString();
	}
}
