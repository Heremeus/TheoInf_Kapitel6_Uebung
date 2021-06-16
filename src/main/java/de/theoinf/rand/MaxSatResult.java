package de.theoinf.rand;

public class MaxSatResult {
    private final int trueClauses;
    private final boolean[] xValues;


    /**
     *
     * @param trueClauses the number of true clauses resulting from this variable assignment
     * @param xValues the variable assignment for x1...xn
     */
    public MaxSatResult(int trueClauses, boolean[] xValues) {
        this.trueClauses = trueClauses;
        this.xValues = xValues;
    }

    public int getTrueClauses() {
        return trueClauses;
    }

    public boolean[] getXValues() {
        return xValues;
    }
}
