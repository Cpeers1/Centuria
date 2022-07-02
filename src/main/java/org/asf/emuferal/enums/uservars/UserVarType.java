package org.asf.emuferal.enums.uservars;

public enum UserVarType {
	Any(0, "UserVarCustom"), Counter(1, "UserVarCounter"), Highest(2, "UserVarHighest"), Lowest(3, "UserVarLowest"),
	Bit(4, "UserVarBit"), BitOnOnly(5, "UserVarBitOnOnly");

	public int val;
	public String componentName;

	UserVarType(int val, String componentName) {
		this.val = val;
		this.componentName = componentName;
	}
}