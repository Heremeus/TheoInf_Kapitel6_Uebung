package de.theoinf.rand;

import java.util.function.Function;

public class MaxSatParameters {
    private final Integer[][] equation;
    private final int n;
    private final long seed;
    private final Function<Double, Double> pi;
    private final double pa;

    /**
     *
     * @param equation the (n,m)-equation in conjunctive normal form
     * @param n the number of variables x1...xn
     * @param seed a seed for the random number generator to get deterministic random results
     */
    public MaxSatParameters(Integer[][] equation, int n, long seed) {
        this.equation = equation;
        this.n = n;
        this.seed = seed;
        pi = null;
        pa = 0.0;
    }

    /**
     *
     * @param equation the (n,m)-equation in conjunctive normal form
     * @param n the number of variables x1...xn
     * @param seed a seed for the random number generator to get deterministic random results
     * @param pi the function pi for Algorithm B
     */
    public MaxSatParameters(Integer[][] equation, int n, long seed, Function<Double, Double> pi) {
        this.equation = equation;
        this.n = n;
        this.pi = pi;
        this.seed = seed;
        pa = 0.0;
    }

    /**
     *
     * @param equation the (n,m)-equation in conjunctive normal form
     * @param n the number of variables x1...xn
     * @param seed a seed for the random number generator to get deterministic random results
     * @param pi the function pi for Algorithm B
     * @param pa the probability pa for Algorithm C_pa
     */
    public MaxSatParameters(Integer[][] equation, int n, long seed, Function<Double, Double> pi, double pa) {
        this.equation = equation;
        this.n = n;
        this.pi = pi;
        this.seed = seed;
        this.pa = pa;
    }

    public Integer[][] getEquation() {
        return equation;
    }

    public int getN() {
        return n;
    }

    public long getSeed() {
        return seed;
    }

    public Function<Double, Double> getPi() {
        return pi;
    }

    public double getPa() {
        return pa;
    }
}
