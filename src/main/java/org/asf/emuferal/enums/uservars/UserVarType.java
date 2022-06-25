package org.asf.emuferal.enums.uservars;

public enum UserVarType
{
	Any(0, "UserVarCustom"),
	Counter(1, "UserVarCounter"), //TODO: Confirm name of this.
	Highest(2, "UserVarHighest"),
	Lowest(3, "UserVarLowest"), //TODO: Confirm name of this.
	Bit(4, "UserVarBit"), //TODO: Confirm name of this.
	BitOnOnly(5, "UserVarBitOnOnly"); //TODO: Confirm name of this.
	
	public int val;
	public String componentName;
	
	UserVarType(int val, String componentName)
	{
		this.val = val;
		this.componentName = componentName;
	}	
}