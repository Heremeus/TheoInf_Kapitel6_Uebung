package de.theoinf.rand;

import com.google.ortools.sat.CpSolverStatus;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

@SuppressWarnings({"UnnecessaryLocalVariable", "SameParameterValue"})
public class Main {

    public static void main(String[] args) {
        // -------------------------------------------------------------------------------------
        // Generate (n,m)-equations in conjunctive normal form to solve

        // n = 5, m = 21, k = 3
        Integer[][] solvableEquation = getSolvableEquation();

        // n = 2, m = 4, k = 2
        // (x1 v x2) ^ (x1 v !x2) ^ (!x1 v x2) ^ (!x1 v !x2)
        Integer[][] unsolvableEquation = getUnsolvableEquation();

        // Generates an equation with 20 variables, 3000 clauses and 3 literals per clause
        var bigEquationK3 = generateEquation(20, 3000, 3);

        // Generates an equation with 20 variables, 3000 clauses and 2-4 literals per clause
        var bigEquationK2_4 = generateEquation(20, 3000, 2, 4);

        // -------------------------------------------------------------------------------------

        // Choose which equation to solve and how often to run the algorithms
        final Integer[][] equationToSolve = solvableEquation;
        final int repetitions = 1000;
        runAlgorithmsForEquation(equationToSolve, repetitions);
    }

    private static void runAlgorithmsForEquation(Integer[][] equationToUse, int repetitions) {
        // Calculate n, m and k for the chosen equation
        int n = Arrays.stream(equationToUse)
                .flatMap(Arrays::stream)
                .mapToInt(Math::abs)
                .max().orElseThrow();
        int m = equationToUse.length;
        int k = equationToUse[0].length;

        System.out.printf("MaxSAT randomised approximation for (n,m)-equation in conjunctive normal form with n=%d, m=%d, k=%d%n", n, m, k);
        // Solves the ILP and prints the result
        printOptimalSolution(equationToUse, n);
        // Solves the LP and prints superoptimal result
        printSuperoptimalSolution(equationToUse, n);


        //
        System.out.println("-----------------------------------------------------------------------------------------");
        runAlgorithm(RandomisedMaxSATApproximationAlgorithms::probabilisticRandomised, "Algorithm A",
                repetitions, equationToUse, n);

        System.out.println("-----------------------------------------------------------------------------------------");
        runAlgorithm(RandomisedMaxSATApproximationAlgorithms::randomisedRounding, "Algorithm B[pi(x)=x]",
                repetitions, equationToUse, n, Function.identity());

        System.out.println("-----------------------------------------------------------------------------------------");
        Function<Double, Double> pi = x -> x * 0.5 + 0.25; // == 1/2 * x + 1/4
        runAlgorithm(RandomisedMaxSATApproximationAlgorithms::randomisedRounding, "Algorithm B[pi(x)=1/2*x+1/4]",
                repetitions, equationToUse, n, pi);

        System.out.println("-----------------------------------------------------------------------------------------");
        runAlgorithm(RandomisedMaxSATApproximationAlgorithms::algorithmC_all, "Algorithm C_all[pi(x)=x]",
                repetitions, equationToUse, n, Function.identity());

        System.out.println("-----------------------------------------------------------------------------------------");
        runAlgorithm(RandomisedMaxSATApproximationAlgorithms::algorithmC_pa, "Algorithm C_1/2[pi(x)=x]",
                repetitions, equationToUse, n, Function.identity(), 0.5);
    }


    // -------------------------------------------------------------------------------------
    // Helper functions
    // -------------------------------------------------------------------------------------

    private static void printOptimalSolution(Integer[][] equationToUse, int n) {
        final double timeout = 3.0;
        var optimalResult = ILPSolver.solveCpSat(n, equationToUse, 1, timeout);
        if (optimalResult.status == CpSolverStatus.UNKNOWN) {
            System.out.printf("Failed to find optimal solution for the ILP in %f seconds. Problem size is too big.%n",
                    timeout);
        } else if (optimalResult.status == CpSolverStatus.OPTIMAL) {
            System.out.println("The optimal solution of the integer linear program has " +
                    Math.round(optimalResult.zValues.stream().mapToDouble(d -> d).sum()) + " true clauses.");
        } else {
            System.out.println("The optimal solution of the integer linear program has >= " +
                    Math.round(optimalResult.zValues.stream().mapToDouble(d -> d).sum()) + " true clauses. " +
                    "Timed out before finding the optimal solution.");
        }
    }


    private static void printSuperoptimalSolution(Integer[][] equationToUse, int n) {
        var lpResult = ILPSolver.solveGLOP(n, equationToUse, 5.0);
        System.out.println("The solution of the relaxed linear program has a (sum of Zj)=" +
                lpResult.zValues.stream().mapToDouble(d -> d).sum());
    }


    private static void runAlgorithm(
            Function<MaxSatParameters, MaxSatResult> algorithm,
            String algorithmName,
            int repetitions,
            Integer[][] equation,
            int n
    ) {
        runAlgorithm(algorithm, algorithmName, repetitions, equation, n, null, 0.0);
    }


    private static void runAlgorithm(
            Function<MaxSatParameters, MaxSatResult> algorithm,
            String algorithmName,
            int repetitions,
            Integer[][] equation,
            int n,
            Function<Double, Double> pi
    ) {
        runAlgorithm(algorithm, algorithmName, repetitions, equation, n, pi, 0.0);
    }


    private static void runAlgorithm(
            Function<MaxSatParameters, MaxSatResult> algorithm,
            String algorithmName,
            int repetitions,
            Integer[][] equation,
            int n,
            Function<Double, Double> pi,
            double pa
    ) {
        Random random = new Random(new Random(42).nextLong());
        long startTime = System.nanoTime();
        List<MaxSatResult> results = new LinkedList<>();
        for (int i = 0; i < repetitions; ++i) {
            results.add(algorithm.apply(new MaxSatParameters(equation, n, random.nextLong(), pi, pa)));
        }
        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1000000;  //divide by 1000000 to get milliseconds.
        System.out.printf("Ran %s for %d ms.%n", algorithmName, duration);

        MaxSatResult bestResult = results.stream().max(Comparator.comparing(MaxSatResult::getTrueClauses)).orElseThrow();
        double averageTrueClausesResult = results.stream().mapToInt(MaxSatResult::getTrueClauses).average().orElseThrow();
        System.out.printf("%s - best: %d; average: %f%n", algorithmName, bestResult.getTrueClauses(), averageTrueClausesResult);

        if (Arrays.stream(equation).mapToInt(clause -> clause.length).max().orElseThrow() <= 20) {
            System.out.print("Best variable assignment: ");
            for (int i = 0; i < bestResult.getXValues().length; ++i) {
                System.out.printf("x%d=%s ", i + 1, bestResult.getXValues()[i] ? "TRUE " : "FALSE");
            }
            System.out.printf("%n");
        }
    }


    private static Integer[][] getSolvableEquation() {
        return new Integer[][]{
                {1, 2, 3},
                {1, 2, 4},
                {1, 2, 5},
                {1, 3, 5},
                {1, 4, 5},
                {2, 3, 4},
                {3, 4, 5},
                // --------
                {-1, 2, 3},
                {1, -2, 4},
                {1, 2, -5},
                {-1, 3, 5},
                {1, -4, 5},
                {2, 3, -4},
                {-3, 4, 5},
                // --------
                {1, -2, 3},
                {1, 2, -4},
                {-1, 2, 5},
                {1, -3, 5},
                {1, 4, -5},
                {-2, 3, 4},
                {3, -4, 5}
        };
    }


    private static Integer[][] getUnsolvableEquation() {
        return new Integer[][]{
                {1, 2},
                {1, -2},
                {-1, 2},
                {-1, -2}
        };
    }


    /**
     * Generates an (n,m)-equation in conjunctive normal form (e.g. (x1 or x2) and (!x1 or !x2)).
     * The function guarantees that every variable xi appears at least once not-negated and once negated.
     * The function also guarantees that a variable does not appear twice in a single clause.
     * The random number generator is seeded and will provide the same equation for the same input arguments
     *
     * @param n the number of variables xi
     * @param m the number of clauses Cj
     * @param k the number of literals in each clause
     * @return an (n,m)-equation in KNF/CNF
     */
    private static Integer[][] generateEquation(int n, int m, int k) {
        if (k > n) {
            throw new IllegalArgumentException("Can't generate a valid equation for k > n");
        }

        Integer[][] equation = new Integer[m][k];
        Set<Integer> verifyEquationSet = new HashSet<>();

        Random random = new Random(42);
        int attemptCounter = 0;
        while (verifyEquationSet.size() != 2 * n) {
            verifyEquationSet = new HashSet<>();
            for (int j = 0; j < m; ++j) {
                for (int i = 0; i < k; ++i) {
                    int xi;
                    for (; ; ) {
                        xi = (random.nextInt(n) + 1) * (random.nextBoolean() ? 1 : -1);
                        boolean isDuplicate = false;
                        for (int i2 = 0; i2 < i; ++i2) {
                            if (Math.abs(xi) == Math.abs(equation[j][i2])) {
                                isDuplicate = true;
                                break;
                            }
                        }
                        if (!isDuplicate) {
                            break;
                        }
                    }
                    equation[j][i] = xi;
                    verifyEquationSet.add(xi);
                }
            }
            if (++attemptCounter > 1000) {
                throw new IllegalArgumentException(
                        String.format("Failed to generate a valid equation for n: %d, m: %d, k: %d", n, m, k));
            }
        }

        return equation;
    }


    /**
     * Generates an (n,m)-equation in conjunctive normal form (e.g. (x1 or x2) and (!x1 or !x2)).
     * This overload allows a random number of literals between kMin and kMax in each clause.
     * The function guarantees that every variable xi appears at least once not-negated and once negated.
     * The function also guarantees that a variable does not appear twice in a single clause.
     * The random number generator is seeded and will provide the same equation for the same input arguments
     *
     * @param n    the number of variables xi
     * @param m    the number of clauses Cj
     * @param kMin the minimum number of literals in each clause (inclusive)
     * @param kMax the maximum number of literals in each clause (inclusive)
     * @return an (n,m)-equation in KNF/CNF
     */
    private static Integer[][] generateEquation(int n, int m, int kMin, int kMax) {
        if (kMax > n) {
            throw new IllegalArgumentException("Can't generate a valid equation for kMax > n");
        } else if (kMax < kMin) {
            throw new IllegalArgumentException("Can't generate a valid equation for kMax < kMin");
        }

        Random random = new Random(42);
        Integer[][] equation = new Integer[m][];
        for (int i = 0; i < m; ++i) {
            equation[i] = new Integer[kMin + random.nextInt(kMax + 1 - kMin)];
        }
        Set<Integer> verifyEquationSet = new HashSet<>();

        int attemptCounter = 0;
        while (verifyEquationSet.size() != 2 * n) {
            verifyEquationSet = new HashSet<>();
            for (int j = 0; j < m; ++j) {
                for (int i = 0; i < equation[j].length; ++i) {
                    int tmp;
                    for (; ; ) {
                        tmp = (random.nextInt(n) + 1) * (random.nextBoolean() ? 1 : -1);
                        boolean isDuplicate = false;
                        for (int i2 = 0; i2 < i; ++i2) {
                            if (Math.abs(tmp) == Math.abs(equation[j][i2])) {
                                isDuplicate = true;
                                break;
                            }
                        }
                        if (!isDuplicate) {
                            break;
                        }
                    }
                    equation[j][i] = tmp;
                    verifyEquationSet.add(tmp);
                }
            }
            if (++attemptCounter > 1000) {
                throw new IllegalArgumentException(String.format(
                        "Failed to generate a valid equation for n: %d, m: %d, kMin: %d, kMax: %d", n, m, kMin, kMax));
            }
        }

        return equation;
    }
}
