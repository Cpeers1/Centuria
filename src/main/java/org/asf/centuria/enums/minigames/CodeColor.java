package org.asf.centuria.enums.minigames;

import java.util.HashMap;

public enum CodeColor {
	None(0),
	Red(1),
	Yellow(2),
	Blue(4),
	Green(8),
	Orange(16),
	Purple(32),
	Cyan(64),
	Pink(128),
	Brown(256);

    private int value;
    private static HashMap<Integer, CodeColor> map = new HashMap<>();

    private CodeColor(int value) {
        this.value = value;
    }

    static {
        for (CodeColor codeColor : CodeColor.values()) {
            map.put(codeColor.value, codeColor);
        }
    }

    public static CodeColor valueOf(int codeColor) {
        return (CodeColor) map.get(codeColor);
    }

    public int getValue() {
        return value;
    }
}
