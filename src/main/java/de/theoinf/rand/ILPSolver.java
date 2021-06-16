package de.theoinf.rand;

import com.google.ortools.Loader;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.CpSolverStatus;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearExpr;

import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;

import java.util.Arrays;
import java.util.stream.Collectors;

public class ILPSolver {

    /**
     * We us google or-tools for solving this ILP (integer linear program).
     * The problem is only an LP (linear program) but we can translate it to an ILP by multiplying the value
     * in the interval [0,1] by a big integer value (e.g. mapping it to the range [0,1000]) and dividing the result
     * by the same integer value.
     *
     * @param n        the number of variables in the equation
     * @param equation an (m,n) equation in KNF
     * @param accuracy The integer accuracy of this algorithm. Results are in 1/accuracy steps.
     *                 A value of 1000 means the result values are in steps of 0.001.
     * @param timeout  timeout in seconds
     */
    public static LPSolverResult solveCpSat(int n, Integer[][] equation, long accuracy, double timeout) {
        Loader.loadNativeLibraries();
        CpModel model = new CpModel();

        // array of IntVars
        IntVar[] xVars = new IntVar[n];
        IntVar[] oXVars = new IntVar[n];
        for (int i = 0; i < n; ++i) {
            xVars[i] = model.newIntVar(0, accuracy, "x" + (i + 1));
            oXVars[i] = model.newIntVar(0, accuracy, "ox" + (i + 1));
            model.addEquality(LinearExpr.sum(new IntVar[]{xVars[i], oXVars[i]}), accuracy);
        }

        IntVar[] zVars = new IntVar[equation.length];
        for (int i = 0; i < equation.length; ++i) {
            zVars[i] = model.newIntVar(0, accuracy, "Z" + (i + 1));
        }

        for (int j = 0; j < equation.length; ++j) {
            var clause = equation[j];
            IntVar[] literals = new IntVar[clause.length];
            for (int i = 0; i < clause.length; ++i) {
                var literal = clause[i];
                boolean negated = literal < 0;
                int index = Math.abs(literal) - 1;
                literals[i] = negated ? oXVars[index] : xVars[index];
            }

            LinearExpr sumVars = LinearExpr.sum(literals);
            LinearExpr zVar = LinearExpr.term(zVars[j], 1);
            model.addGreaterOrEqual(sumVars, zVar);
        }

        model.maximize(LinearExpr.sum(zVars));

        CpSolver solver = new CpSolver();
        solver.getParameters().setMaxTimeInSeconds(timeout);
        LPSolverResult result = new LPSolverResult();
        result.status = solver.solve(model);
        if(result.status == CpSolverStatus.UNKNOWN)
        {
            // Failed to solve problem
            return result;
        }
        for (var zVar : zVars) {
            result.zValues.add((double) solver.value(zVar) / accuracy);
        }
        for (var xVar : xVars) {
            result.xValues.add((double) solver.value(xVar) / accuracy);
        }

        return result;
    }

    /**
     * We us google or-tools for solving this LP (linear program).
     * @param n        the number of variables in the equation
     * @param equation an (m,n) equation in KNF
     * @param timeout  timeout in seconds
     */
    public static LPSolverResult solveGLOP(int n, Integer[][] equation, double timeout) {
        Loader.loadNativeLibraries();
        MPSolver solver = MPSolver.createSolver("GLOP");

        double infinity = java.lang.Double.POSITIVE_INFINITY;

        // array of IntVars
        MPVariable[] xVars = new MPVariable[n];
        MPVariable[] oXVars = new MPVariable[n];
        for (int i = 0; i < n; ++i) {
            xVars[i] = solver.makeNumVar(0.0, 1.0, "x" + (i + 1));
            oXVars[i] = solver.makeNumVar(0.0, 1.0, "x" + (i + 1));
            MPConstraint constraint = solver.makeConstraint(1.0, 1.0);
            constraint.setCoefficient(xVars[i], 1.0);
            constraint.setCoefficient(oXVars[i], 1.0);
        }

        MPVariable[] zVars = new MPVariable[equation.length];
        for (int i = 0; i < equation.length; ++i) {
            zVars[i] = solver.makeNumVar(0.0, 1.0, "Z" + (i + 1));
        }

        for (int j = 0; j < equation.length; ++j) {
            var clause = equation[j];

            MPConstraint constraint = solver.makeConstraint(0.0, infinity);
            for (Integer literal : clause) {
                boolean negated = literal < 0;
                int index = Math.abs(literal) - 1;
                constraint.setCoefficient(negated ? oXVars[index] : xVars[index], 1.0);
            }

            constraint.setCoefficient(zVars[j], -1.0);
        }

        // Maximize sum(Z)
        MPObjective objective = solver.objective();
        for(var zVar : zVars) {
            objective.setCoefficient(zVar, 1);
        }
        objective.setMaximization();

        solver.setTimeLimit(Math.round(timeout * 1000));
        final MPSolver.ResultStatus resultStatus = solver.solve();

        var result = new LPSolverResult();
        result.xValues = Arrays.stream(xVars)
                .mapToDouble(MPVariable::solutionValue)
                .boxed()
                .collect(Collectors.toList());
        result.zValues = Arrays.stream(zVars)
                .mapToDouble(MPVariable::solutionValue)
                .boxed()
                .collect(Collectors.toList());

        switch (resultStatus)
        {
            case OPTIMAL:
                result.status = CpSolverStatus.OPTIMAL;
                break;
            case FEASIBLE:
                result.status = CpSolverStatus.FEASIBLE;
                break;
            case INFEASIBLE:
                result.status = CpSolverStatus.INFEASIBLE;
                break;
            default:
                result.status = CpSolverStatus.UNKNOWN;
                break;
        }

        return result;
    }
}
