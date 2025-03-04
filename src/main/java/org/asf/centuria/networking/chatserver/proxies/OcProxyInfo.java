package org.asf.centuria.networking.chatserver.proxies;

import java.util.ArrayList;

import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.accounts.PlayerInventory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * 
 * OC Proxying Information 
 * 
 * @author Sky Swimmer
 * 
 */
public class OcProxyInfo {

    public String displayName;

    public String triggerPrefix;
    public String triggerSuffix;

    public String characterPronouns;
    public String characterBio;

    public boolean publiclyVisible = true;

    /**
     * Finds OCs of users by name
     * 
     * @param user User to retrieve the oc of
     * @param ocName OC name
     * @return OcProxyInfo instance or null
     */
    public static OcProxyInfo ofUser(CenturiaAccount user, String ocName) {
        // Get save
        PlayerInventory data = user.getSaveSpecificInventory();
        if (data.containsItem("original-character-" + ocName.toLowerCase())) {
            // Load
            return new OcProxyInfo()
                    .loadJson(data.getItem("original-character-" + ocName.toLowerCase()).getAsJsonObject());
        }

        // Not found
        return null;
    }

    /**
     * Checks a OC exists
     * 
     * @param user User to retrieve the oc of
     * @param ocName OC name
     * @return True if the OC exists, false otherwise
     */
    public static boolean ocExists(CenturiaAccount user, String ocName) {
        // Get save
        PlayerInventory data = user.getSaveSpecificInventory();
        return data.containsItem("original-character-" + ocName.toLowerCase());
    }

    /**
     * Retrieves all OC names of a user
     * 
     * @param user User to retrieve all OCs of
     * @return Array of OcProxyInfo instances
     */
    public static String[] allOcNamesOfUser(CenturiaAccount user) {
        // Get save
        PlayerInventory data = user.getSaveSpecificInventory();
        if (data.containsItem("original-characters-list")) {
            // Load
            JsonArray arr = data.getItem("original-characters-list").getAsJsonArray();
            ArrayList<String> lst = new ArrayList<String>();
            for (JsonElement ele : arr)
                lst.add(ele.getAsString());
            return lst.toArray(t -> new String[t]);
        }

        // Not found
        return new String[0];
    }

    /**
     * Retrieves all OCs of a user
     * 
     * @param user User to retrieve all OCs of
     * @return Array of OcProxyInfo instances
     */
    public static OcProxyInfo[] allOfUser(CenturiaAccount user) {
        // Get save
        PlayerInventory data = user.getSaveSpecificInventory();
        if (data.containsItem("original-characters-list")) {
            // Load
            JsonArray arr = data.getItem("original-characters-list").getAsJsonArray();
            ArrayList<OcProxyInfo> lst = new ArrayList<OcProxyInfo>();
            for (JsonElement ele : arr)
                lst.add(ofUser(user, ele.getAsString()));
            return lst.toArray(t -> new OcProxyInfo[t]);
        }

        // Not found
        return new OcProxyInfo[0];
    }

    /**
     * Saves or creates original characters
     * 
     * @param user User to save the OC of
     * @param name OC name to save
     * @param triggerPrefix Trigger prefix of the new character
     * @return OcProxyInfo instance
     */
    public static OcProxyInfo saveOc(CenturiaAccount user, String name, String triggerPrefix, String triggerSuffix) {
        OcProxyInfo info = new OcProxyInfo();
        info.displayName = name;
        info.triggerPrefix = triggerPrefix;
        info.triggerSuffix = triggerSuffix;
        info.characterBio = "N/A";
        info.characterPronouns = "N/A";
        return saveOc(user, info);
    }

    /**
     * Saves or creates original characters
     * 
     * @param user User to save the OC of
     * @param oc OC to save
     * @return OcProxyInfo instance
     */
    public static OcProxyInfo saveOc(CenturiaAccount user, OcProxyInfo oc) {
        if (!oc.displayName.matches("^[A-Za-z0-9_\\-. ]+")) {
            // Invalid name
            throw new IllegalArgumentException("OC name contains invalid characters");
        }

        // Get save
        PlayerInventory data = user.getSaveSpecificInventory();

        // Check if existing
        if (!ocExists(user, oc.displayName)) {
            // Add to list
            JsonArray arr = new JsonArray();
            if (data.containsItem("original-characters-list"))
                arr = data.getItem("original-characters-list").getAsJsonArray();
            arr.add(oc.displayName);
            data.setItem("original-characters-list", arr);
        }

        // Save OC
        data.setItem("original-character-" + oc.displayName.toLowerCase(), oc.toJson());

        // Return
        return oc;
    }

    /**
     * Deletes OCs
     * 
     * @param user User to delete the oc of
     * @param ocName Name of the OC to delete
     * @return True if the OC was deleted, false otherwise
     */
    public static boolean deleteOc(CenturiaAccount user, String ocName) {
        // Get save
        PlayerInventory data = user.getSaveSpecificInventory();

        // Check if existing
        if (!ocExists(user, ocName))
            return false;

        // Delete
        data.deleteItem("original-character-" + ocName.toLowerCase());

        // Remove from list
        if (data.containsItem("original-characters-list")) {
            JsonArray arr = data.getItem("original-characters-list").getAsJsonArray();
            for (JsonElement ele : arr) {
                if (ele.getAsString().toLowerCase().equals(ocName.toLowerCase())) {
                    // Remove
                    arr.remove(ele);
                    data.setItem("original-characters-list", arr);
                    break;
                }
            }
        }

        // Return
        return true;
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("displayName", displayName);
        obj.addProperty("triggerPrefix", triggerPrefix);
        obj.addProperty("triggerSuffix", triggerSuffix);
        obj.addProperty("characterPronouns", characterPronouns);
        obj.addProperty("characterBio", characterBio);
        obj.addProperty("publiclyVisible", publiclyVisible);
        return obj;
    }

    public OcProxyInfo loadJson(JsonObject obj) {
        displayName = obj.get("displayName").getAsString();
        triggerPrefix = obj.get("triggerPrefix").getAsString();
        triggerSuffix = obj.get("triggerSuffix").getAsString();
        characterPronouns = obj.get("characterPronouns").getAsString();
        characterBio = obj.get("characterBio").getAsString();
        publiclyVisible = obj.get("publiclyVisible").getAsBoolean();
        return this;
    }

}