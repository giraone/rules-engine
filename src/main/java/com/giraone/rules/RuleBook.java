package com.giraone.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

public class RuleBook<F, R> {

    private static final BiConsumer<String,Boolean> EMPTY_LOG = (d,r) -> {};

    private final List<Rule<F, R>> rules = new ArrayList<>();

    public RuleBook<F, R> addRule(Rule<F, R> rule) {
        rules.add(rule);
        return this;
    }

    public Outcome<F, R> applyOnFacts(F facts, R result) {

       return applyOnFacts(facts, result, EMPTY_LOG, EMPTY_LOG);
    }

    public Outcome<F, R> applyOnFacts(F facts, R result,
                                      BiConsumer<String,Boolean> logWhen,
                                      BiConsumer<String,Boolean> logThen) {

        final Outcome<F, R> outcome = new Outcome<>(facts, result);
        applyOnFacts(outcome, facts, result, logWhen, logThen);
        return outcome;
    }

    //------------------------------------------------------------------------------------------------------------------

    private void applyOnFacts(Outcome<F, R> outcome,
                              F facts, R result,
                              BiConsumer<String,Boolean> logWhen,
                              BiConsumer<String,Boolean> logThen) {

        final AtomicBoolean stopped = new AtomicBoolean(false);

        rules.stream()
            .filter(rule -> {
                if (stopped.get()) {
                    return false;
                }
                boolean whenResult = rule.whenFunction.test(facts);
                logWhen.accept(rule.whenDescription, whenResult);
                if (whenResult && rule.whenOutcomeFunction != null) {
                    whenResult = rule.whenOutcomeFunction.test(result);
                    logWhen.accept(rule.andWhenOutcomeDescription, whenResult);
                }
                return whenResult;
            })
            .forEach(rule -> {
                if (rule.groupedRules != null) {
                    final RuleBook<F,R> ruleBook = new RuleBook<>();
                    rule.groupedRules.accept(ruleBook);
                    ruleBook.applyOnFacts(outcome, facts, result, logWhen, logThen);
                } else {
                    final boolean mustStop = rule.thenFunction.test(outcome);
                    logThen.accept(rule.thenDescription, mustStop);
                    stopped.set(mustStop);
                }
            });
    }
}
