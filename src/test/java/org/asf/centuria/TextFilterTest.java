package org.asf.centuria;

import java.util.Scanner;

import org.asf.centuria.textfilter.TextFilterService;

public class TextFilterTest {

	public static void main(String[] args) {
		Scanner sc = new Scanner(System.in);
		System.out.print("Input: ");
		String input = sc.nextLine();
		System.out.print("Strictmode: ");
		String mode = sc.nextLine();
		if (!mode.equalsIgnoreCase("true") && !mode.equalsIgnoreCase("false")) {
			System.err.println("Error: invalid mode");
			System.exit(1);
			sc.close();
			return;
		}
		boolean strict = mode.equalsIgnoreCase("true");
		System.out.print("Tags: ");
		String[] tags = sc.nextLine().replace(" ", "").split(",");
		TextFilterService.getInstance().initService();
		System.out.println(TextFilterService.getInstance().filterString(input, strict, tags));
		sc.close();
	}

}
