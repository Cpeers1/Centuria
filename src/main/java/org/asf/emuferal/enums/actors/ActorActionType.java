package org.asf.emuferal.enums.actors;

public enum ActorActionType {

	None(0), IdleBreak01(1), IdleBreak02(2), IdleBreak03(3), Jump_Loop(10), Fall_Loop(11), Glide_Loop(12), Dig_Loop(20),
	Dive(30), Sleep_Loop(40), Tired(41), Dizzy(50), Sit_Loop(60), Sit01_Loop(61), Sit02_Loop(62), Mad(70), Excited(80),
	Surprised(90), Talk(100), Yes(110), No(120), GetAttention(130), SendAway(140), Laugh(150), Dance(160), Eat(170),
	Sad(180), Scared(190), Strut(200), Play(210), Throw(220), Ride_Loop(230), Harvest(240), Respawn(250), Swim(1000),
	Decal(2000), BreakLoop(99999), Movement(100000);

	public int value;

	ActorActionType(int value) {
		this.value = value;
	}

}