package de.theoinf.rand;

import com.google.ortools.sat.CpSolverStatus;

import java.util.LinkedList;
import java.util.List;

public class LPSolverResult {
    public List<Double> xValues;
    public List<Double> zValues;
    public CpSolverStatus status;

    public LPSolverResult() {
        this.xValues = new LinkedList<>();
        this.zValues = new LinkedList<>();
    }
}
