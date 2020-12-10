package com.giraone.rules;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A rule based on facts F with a result outcome R
 *
 * @param <F> The input facts class.
 * @param <R> The outcome result class.
 */
public class Rule<F, R> {

    Predicate<F> whenFunction;
    Predicate<Outcome<F, R>> thenFunction;

    Rule<F, R> when(Predicate<F> whenFunction) {
        this.whenFunction = whenFunction;
        return this;
    }

    Rule<F, R> thenProceedWith(Consumer<Outcome<F, R>> consumer) {
        this.thenFunction = f -> {
            consumer.accept(f);
            return false;
        };
        return this;
    }

    Rule<F, R> thenStopWith(Consumer<Outcome<F, R>> consumer) {
        this.thenFunction = f -> {
            consumer.accept(f);
            return true;
        };
        return this;
    }
}
