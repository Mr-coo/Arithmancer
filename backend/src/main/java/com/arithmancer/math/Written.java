package com.arithmancer.math;

// How numbers and terms are written in questions and answers.
public final class Written {

	private static final String SUPERSCRIPTS = "⁰¹²³⁴⁵⁶⁷⁸⁹";
	private static final String SUBSCRIPTS = "₀₁₂₃₄₅₆₇₈₉";

	private Written() {
	}

	// n/d in lowest terms, or a whole number when it divides out, with the sign in front.
	public static String fraction(int n, int d) {
		int divisor = gcd(Math.abs(n), Math.abs(d));
		int top = n / divisor * Integer.signum(d);
		int bottom = Math.abs(d) / divisor;
		return bottom == 1 ? String.valueOf(top) : top + "/" + bottom;
	}

	// coefficient × xⁿ: 3x², x, -x³, 7. A coefficient of 1 is left out, as is x⁰.
	public static String term(int coefficient, int exponent) {
		if (exponent == 0) {
			return String.valueOf(coefficient);
		}
		String number = coefficient == 1 ? "" : coefficient == -1 ? "-" : String.valueOf(coefficient);
		return number + "x" + (exponent == 1 ? "" : superscript(exponent));
	}

	// n written small and raised, as an exponent: ³ for 3.
	public static String superscript(int n) {
		return digits(n, SUPERSCRIPTS);
	}

	// n written small and lowered, as a logarithm's base: ₂ for 2.
	public static String subscript(int n) {
		return digits(n, SUBSCRIPTS);
	}

	// Writes n, at least 0, with these characters for 0 to 9.
	private static String digits(int n, String characters) {
		StringBuilder written = new StringBuilder();
		for (char digit : String.valueOf(n).toCharArray()) {
			written.append(characters.charAt(digit - '0'));
		}
		return written.toString();
	}

	private static int gcd(int a, int b) {
		return b == 0 ? Math.max(a, 1) : gcd(b, a % b);
	}

}
