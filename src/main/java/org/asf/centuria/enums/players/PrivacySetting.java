package org.asf.centuria.enums.players;

import java.util.HashMap;
import java.util.Map;

public enum PrivacySetting
{
	Everyone(0), Followers(1), Nobody(2);
	
	public int value;

	PrivacySetting(int value) {
		this.value = value;
	}
	
    // Reverse-lookup map 
    private static final Map<Integer, PrivacySetting> lookup = new HashMap<Integer, PrivacySetting>();

    static {
        for (PrivacySetting d : PrivacySetting.values()) {
            lookup.put(d.value, d);
        }
    }
    
    public static PrivacySetting get(int value) {
        return lookup.get(value);
    }
}
