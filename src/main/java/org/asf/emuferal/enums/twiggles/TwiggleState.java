package org.asf.emuferal.enums.twiggles;

import java.util.HashMap;
import java.util.Map;

import org.asf.emuferal.enums.inventory.InventoryType;

public enum TwiggleState {
	None(0),
	WorkingSanctuary(1),
	WorkingOtherSanctuary(2),
	FinishedSanctuary(3),
	FinishedOtherSanctuary(4);
	
	public int value;

	TwiggleState(int value) {
		this.value = value;
	}
	
    // Reverse-lookup map 
    private static final Map<Integer, TwiggleState> lookup = new HashMap<Integer, TwiggleState>();

    static {
        for (TwiggleState d : TwiggleState.values()) {
            lookup.put(d.value, d);
        }
    }
    
    public static TwiggleState get(int state) {
        return lookup.get(state);
    }
}
