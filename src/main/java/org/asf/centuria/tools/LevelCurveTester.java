package org.asf.centuria.tools;

import java.util.Scanner;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

public class LevelCurveTester {

	public static void main(String[] args) {
		@SuppressWarnings("resource")
		Scanner sc = new Scanner(System.in);
		while (true) {
			System.out.print("Curve: ");
			String curve = sc.nextLine();

			// Evaluate
			int totalXp = 0;
			for (int i = 1; i < 101; i++) {
				// Evaluate the curve
				ExpressionBuilder builder = new ExpressionBuilder(curve);
				builder.variables("level", "lastlevel", "totalxp");
				Expression exp = builder.build();
				exp.setVariable("level", i);
				exp.setVariable("lastlevel", i - 1);
				exp.setVariable("totalxp", totalXp);
				int levelUpCount = (int) exp.evaluate();
				if (levelUpCount > 15000)
					levelUpCount = 15000;
				System.out.println("Level " + i + "-" + (i + 1) + ": " + levelUpCount);
				totalXp += levelUpCount;
			}
		}
	}

}
