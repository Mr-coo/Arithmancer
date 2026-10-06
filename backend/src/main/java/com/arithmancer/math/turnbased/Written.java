package com.arithmancer.math.turnbased;

// How numbers and terms are written in turn-based problems and answers.
final class Written {

	private static final String SUPERSCRIPTS = "⁰¹²³⁴⁵⁶⁷⁸⁹";

	private Written() {
	}

	// n/d in lowest terms, or a whole number when it divides out, with the sign in front.
	static String fraction(int n, int d) {
		int divisor = gcd(Math.abs(n), Math.abs(d));
		int top = n / divisor * Integer.signum(d);
		int bottom = Math.abs(d) / divisor;
		return bottom == 1 ? String.valueOf(top) : top + "/" + bottom;
	}

	// coefficient × xⁿ: 3x², x, -x³, 7. A coefficient of 1 is left out, as is x⁰.
	static String term(int coefficient, int exponent) {
		if (exponent == 0) {
			return String.valueOf(coefficient);
		}
		String number = coefficient == 1 ? "" : coefficient == -1 ? "-" : String.valueOf(coefficient);
		return number + "x" + (exponent == 1 ? "" : superscript(exponent));
	}

	static String superscript(int n) {
		StringBuilder written = new StringBuilder();
		for (char digit : String.valueOf(n).toCharArray()) {
			written.append(SUPERSCRIPTS.charAt(digit - '0'));
		}
		return written.toString();
	}

	private static int gcd(int a, int b) {
		return b == 0 ? Math.max(a, 1) : gcd(b, a % b);
	}

}
