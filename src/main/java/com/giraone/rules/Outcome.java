package com.giraone.rules;

/**
 * Outcome is the tuple of input facts and output result.
 *
 * @param <F> The input facts class. This can be any Java Pojo.
 * @param <R> The output result class. This can be any Java Pojo.
 */
public class Outcome<F, R> {

    public final F facts;
    public final R result;

    public Outcome(F facts, R result) {
        this.facts = facts;
        this.result = result;
    }
}
