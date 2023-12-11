package org.asf.centuria.accounts.tags;

import com.google.gson.JsonObject;

/**
 * 
 * Player account tag
 * 
 * @author Sky Swimmer
 * 
 */
public abstract class AccountTag {

    /**
     * Retrieves the tag ID
     * 
     * @return Tag ID string
     */
    public abstract String getTagID();

    /**
     * Retrieves the tag value
     * 
     * @return Tag value JSON object
     */
    public abstract JsonObject getTagValue();

    /**
     * Updates the tag value
     * 
     * @param value New tag value
     */
    public abstract void setTagValue(JsonObject value);
    
    /**
     * Deletes tags
     */
    public abstract void deleteTag();

}