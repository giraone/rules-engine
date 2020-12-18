package com.giraone.rules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

/**
 * A rule book applied on input facts of type F with an output result of type R.
 * @param <F> The type of the input facts.
 * @param <R> The type of the output result.
 */
public class RuleBook<F, R> {

    private static final BiConsumer<String,Boolean> EMPTY_LOG = (d,r) -> {};

    private final List<Rule<F, R>> rules = new ArrayList<>();

    /**
     * Add a new rule to the end of the rule book.
     * @param rule  The rule to add.
     * @return The rule book itself to be able to use "fluent" style.
     */
    public RuleBook<F, R> addRule(Rule<F, R> rule) {
        rules.add(rule);
        return this;
    }

    /**
     * Add a new rule to the end of the rule book.
     * @param rules  A collection of rules to add. The rules are added at the end of the rules list in the order they have in the collection
     * @return The rule book itself to be able to use "fluent" style.
     */
    public RuleBook<F, R> addRules(Collection<Rule<F, R>> rules) {
        rules.forEach(this::addRule);
        return this;
    }

    /**
     * Apply all rules on given facts and define the result
     * @param facts The input facts.
     * @param result The output result object, that is changed by the rules.
     * @return The tupel of input facts and output result.
     */
    public Outcome<F, R> applyOnFacts(F facts, R result) {
       return applyOnFacts(facts, result, EMPTY_LOG, EMPTY_LOG);
    }

    /**
     * Apply all rules on given facts and define the result
     * @param facts The input facts.
     * @param result The output result object, that is changed by the rules.
     * @param logWhen A logging function that is called with the description of the WHEN clause and the resulting value (true/false) of the WHEN clause.
     * @param logThen A logging function that is called with the description of the THEN clause and the resulting value (true/false) of the THEN clause.
     * @return The tupel of input facts and output result.
     */
    public Outcome<F, R> applyOnFacts(F facts, R result,
                                      BiConsumer<String,Boolean> logWhen,
                                      BiConsumer<String,Boolean> logThen) {

        final Outcome<F, R> outcome = new Outcome<>(facts, result); // outcome is used globally
        final AtomicBoolean stopped = new AtomicBoolean(false); // stopped work globally
        applyOnFacts(outcome, stopped, "", facts, result, logWhen, logThen);
        return outcome;
    }

    //------------------------------------------------------------------------------------------------------------------

    private void applyOnFacts(Outcome<F, R> outcome,
                              AtomicBoolean stopped,
                              String parentWhenDescription,
                              F facts, R result,
                              BiConsumer<String,Boolean> logWhen,
                              BiConsumer<String,Boolean> logThen) {

        rules.stream()
            .filter(rule -> {
                if (stopped.get()) {
                    return false;
                }
                boolean whenResult = rule.whenFactsFunction.test(facts);
                logWhen.accept(parentWhenDescription + rule.whenFactsDescription, whenResult);
                if (whenResult && rule.whenOutcomeFunction != null) {
                    whenResult = rule.whenOutcomeFunction.test(result);
                    logWhen.accept(rule.whenOutcomeDescription, whenResult);
                }
                return whenResult;
            })
            .forEach(rule -> {
                if (rule.groupedRules != null) {
                    final RuleBook<F,R> ruleBook = new RuleBook<>();
                    rule.groupedRules.accept(ruleBook);
                    ruleBook.applyOnFacts(outcome, stopped, rule.whenFactsDescription + " AND ", facts, result, logWhen, logThen);
                } else {
                    final boolean mustStop = rule.thenFunction.test(outcome);
                    logThen.accept(rule.thenDescription, mustStop);
                    stopped.set(mustStop);
                }
            });
    }
}
