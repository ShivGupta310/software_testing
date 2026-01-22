package ilpREST.ilp_submission_1.services;

import ilpREST.ilp_submission_1.dto.Position;
import org.junit.jupiter.api.Test;

public class ConversionFormulaTest {

    @Test
    void deriveCorrectConversionFormula() {
        System.out.println("=== DERIVING THE CONVERSION ===\n");

        // Navigation bearings (clockwise from North)
        // Math angles (counter-clockwise from East)

        double[][] testCases = {
                {0, 90},      // North (nav) = 90° (math) - pointing up
                {90, 0},      // East (nav) = 0° (math) - pointing right
                {180, 270},   // South (nav) = 270° (math) - pointing down
                {270, 180},   // West (nav) = 180° (math) - pointing left
                {45, 45},     // NE (nav) = 45° (math)
                {135, 315},   // SE (nav) = 315° (math)
                {225, 225},   // SW (nav) = 225° (math)
                {315, 135}    // NW (nav) = 135° (math)
        };

        System.out.println("Nav° -> Math° (expected)");
        for (double[] tc : testCases) {
            System.out.printf("%3.0f° -> %3.0f°\n", tc[0], tc[1]);
        }

        System.out.println("\n=== TESTING FORMULAS ===\n");

        String[] formulas = {
                "90 - nav",
                "-(nav - 90)",
                "450 - nav",
                "(90 - nav + 360) % 360"
        };

        for (String formula : formulas) {
            System.out.println("Testing: mathAngle = " + formula);
            boolean allMatch = true;

            for (double[] tc : testCases) {
                double nav = tc[0];
                double expectedMath = tc[1];
                double computedMath = 0;

                switch (formula) {
                    case "90 - nav":
                        computedMath = 90 - nav;
                        break;
                    case "-(nav - 90)":
                        computedMath = -(nav - 90);
                        break;
                    case "450 - nav":
                        computedMath = 450 - nav;
                        break;
                    case "(90 - nav + 360) % 360":
                        computedMath = (90 - nav + 360) % 360;
                        break;
                }

                // Normalize to 0-360
                computedMath = ((computedMath % 360) + 360) % 360;

                boolean match = Math.abs(computedMath - expectedMath) < 0.01;
                if (!match) {
                    System.out.printf("  ❌ %3.0f° -> got %.0f°, expected %.0f°\n",
                            nav, computedMath, expectedMath);
                    allMatch = false;
                }
            }

            if (allMatch) {
                System.out.println("  ✓✓✓ ALL MATCH! ✓✓✓");
            }
            System.out.println();
        }

        // Now test with the actual bearing
        double navBearing = 257.84450079492376;
        double expectedMath = 186.87917690899542;

        System.out.println("=== ACTUAL CASE ===");
        System.out.println("Navigation bearing: " + navBearing + "°");
        System.out.println("Expected math angle: " + expectedMath + "°");

        double result1 = ((90 - navBearing) + 360) % 360;
        double result2 = ((450 - navBearing) % 360);

        System.out.println("\nUsing (90 - nav + 360) % 360 = " + result1);
        System.out.println("Using (450 - nav) % 360 = " + result2);
        System.out.println("\nWhich is closer to " + expectedMath + "?");
        System.out.println("Formula 1 diff: " + Math.abs(result1 - expectedMath));
        System.out.println("Formula 2 diff: " + Math.abs(result2 - expectedMath));
    }
}