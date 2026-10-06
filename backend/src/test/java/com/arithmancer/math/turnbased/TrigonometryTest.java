package com.arithmancer.math.turnbased;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TrigonometryTest {

	@Test
	void exactValuesMatchTheFunctions() {
		for (int degrees = 0; degrees < 360; degrees += 15) {
			if (degrees % 30 != 0 && degrees % 45 != 0) {
				continue;
			}
			double radians = Math.toRadians(degrees);
			assertEquals(Math.sin(radians), parse(Trigonometry.value("sin", degrees)), 1e-9, "sin " + degrees);
			assertEquals(Math.cos(radians), parse(Trigonometry.value("cos", degrees)), 1e-9, "cos " + degrees);
			String tangent = Trigonometry.value("tan", degrees);
			if (degrees % 180 == 90) {
				assertNull(tangent, "tan " + degrees);
			} else {
				assertTrue(tangent != null, "tan " + degrees);
				assertEquals(Math.tan(radians), parse(tangent), 1e-9, "tan " + degrees);
			}
		}
	}

	// Reads values such as 0, -1, 1/2, √3, -√2/2.
	private static double parse(String value) {
		double sign = value.startsWith("-") ? -1 : 1;
		String[] parts = value.replace("-", "").split("/");
		double top = parts[0].startsWith("√") ? Math.sqrt(Double.parseDouble(parts[0].substring(1)))
				: Double.parseDouble(parts[0]);
		return sign * top / (parts.length > 1 ? Double.parseDouble(parts[1]) : 1);
	}

}
