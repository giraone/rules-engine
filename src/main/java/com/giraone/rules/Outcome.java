package com.giraone.rules;

/**
 * Outcome is the tuple of input facts and outcome results.
 *
 * @param <F> The input facts class. This can be any Java Pojo.
 * @param <R> The outcome results class. This can be any Java Pojo.
 */
public class Outcome<F, R> {

    public final F facts;
    public final R result;

    public Outcome(F facts, R result) {
        this.facts = facts;
        this.result = result;
    }
}
