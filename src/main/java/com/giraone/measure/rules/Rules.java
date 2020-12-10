package com.giraone.measure.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Rules<F, R> {

    private final List<Rule<F, R>> rules = new ArrayList<>();

    public Rules<F, R> addRule(Rule<F, R> rule) {
        rules.add(rule);
        return this;
    }

    public Outcome<F, R> applyOnFacts(F facts, R result) {

        final Outcome<F, R> outcome = new Outcome<>(facts, result);
        final AtomicBoolean stopped = new AtomicBoolean(false);

        rules.stream()
            .filter(rule -> !stopped.get() && rule.whenFunction.test(facts))
            .forEach(rule -> stopped.set(rule.thenFunction.test(outcome)));

        return outcome;
    }
}
