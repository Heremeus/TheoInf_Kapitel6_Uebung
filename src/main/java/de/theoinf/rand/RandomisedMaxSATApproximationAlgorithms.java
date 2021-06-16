package de.theoinf.rand;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class RandomisedMaxSATApproximationAlgorithms {
    public static Map<Long, LPSolverResult> solvedEquations = new HashMap<>();

    /**
     * Algorithm A - Randomly sets variables xi to TRUE or FALSE with a 50% probability each
     *
     * @param parameters contains the equation and the random seed for running Algorithm A
     * @return a MaxSatResult with the number of true clauses and the variable assignment
     */
    public static MaxSatResult probabilisticRandomised(MaxSatParameters parameters) {
        boolean[] variables = new boolean[parameters.getN()];
        Integer[][] equation = parameters.getEquation();
        // Seeded random instance to get the same results when running the function twice with the same seed
        Random random = new Random(parameters.getSeed());

        // TODO: Implement Algorithm A

        return new MaxSatResult(countTrueClauses(parameters.getEquation(), variables), variables);
    }

    /**
     * Algorithm B - Solves the relaxed linear program to find the probabilities for setting a variable to TRUE
     *
     * @param parameters contains the equation, the function pi and the random seed for running Algorithm B
     * @return a MaxSatResult with the number of true clauses and the variable assignment
     */
    public static MaxSatResult randomisedRounding(MaxSatParameters parameters) {
        boolean[] variables = new boolean[parameters.getN()];
        Integer[][] equation = parameters.getEquation();
        // Seeded random instance to get the same results when running the function twice with the same seed
        Random random = new Random(parameters.getSeed());

        // Solve relaxed linear program
        LPSolverResult solverResult = solveLP(parameters.getEquation(), parameters.getN());
        // The optimised x variables with 0 <= value <= 1
        List<Double> solverVariableAssignment = solverResult.xValues;

        // TODO: Implement Algorithm B

        int trueClauses = countTrueClauses(parameters.getEquation(), variables);
        return new MaxSatResult(trueClauses, variables);
    }

    /**
     * Algorithm C_all - Runs Algorithm A and B and returns the better result
     *
     * @param parameters the parameters for running Algorithm A and B
     * @return the better of the two results of Algorithm A and B
     */
    public static MaxSatResult algorithmC_all(MaxSatParameters parameters) {
        // TODO: Implement Algorithm C_all

        return null;
    }

    /**
     * Algorithm C_pa - Runs Algorithm A with a probability pa, otherwise runs Algorithm B
     *
     * @param parameters the parameters for running Algorithm A and B
     * @return the result of Algorithm A with probability pa, otherwise the result of Algorithm B
     */
    public static MaxSatResult algorithmC_pa(MaxSatParameters parameters) {
        double pa = parameters.getPa();

        // TODO: Implement Algorithm C_1/2

        return null;
    }


    // -------------------------------------------------------------------------------------
    // Helper functions
    // -------------------------------------------------------------------------------------

    /**
     * Counts the number of true clauses in a (n,m)-equation in conjunctive normal form for a given variable assignment
     *
     * @param equation  the equation to check
     * @param variables the variable assignment
     * @return the number of true clauses
     */
    private static int countTrueClauses(Integer[][] equation, boolean[] variables) {
        int numSatisfiedClauses = 0;
        for (var clause : equation) {
            for (var literal : clause) {
                int index = Math.abs(literal) - 1;
                boolean expectedValue = literal > 0; // x == true, -x == false
                if (variables[index] == expectedValue) {
                    ++numSatisfiedClauses;
                    break;
                }
            }
        }
        return numSatisfiedClauses;
    }

    /**
     * Solves the relaxed linear program
     * @param equation the equation to solve
     * @param n the number of variable
     * @return the solver result
     */
    private static LPSolverResult solveLP(Integer[][] equation, int n) {
        long hash = Arrays.hashCode(Arrays.stream(equation).map(Arrays::hashCode).toArray());
        // Check if this program has been solved before to save performance
        if (!solvedEquations.containsKey(hash)) {
            solvedEquations.put(hash, ILPSolver.solveGLOP(n, equation, 5.0));
        }
        return solvedEquations.get(hash);
    }
}
