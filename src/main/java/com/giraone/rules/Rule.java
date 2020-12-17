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

    String whenDescription;
    String andWhenOutcomeDescription;
    String thenDescription;
    Predicate<F> whenFunction;
    Predicate<R> whenOutcomeFunction;
    Predicate<Outcome<F, R>> thenFunction;
    Consumer<RuleBook<F, R>> groupedRules;

    /**
     * Give the when clause of the rule a description, that is used for debugging.
     * @param description  Description of when clause
     * @return The rule object
     */
    public Rule<F, R> whenDescription(String description) {
        this.whenDescription = description;
        return this;
    }

    /**
     * Give the and-when-outcome clause of the rule a description, that is used for debugging.
     * @param description  Description of and-when-outcome clause
     * @return The rule object
     */
    public Rule<F, R> andWhenOutcomeDescription(String description) {
        this.andWhenOutcomeDescription = description;
        return this;
    }

    /**
     * Give the when clause of the rule a description, that is used for debugging.
     * @param description  Description of when clause
     * @return The rule object
     */
    public Rule<F, R> thenDescription(String description) {
        this.thenDescription = description;
        return this;
    }

    /**
     * Define the facts condition under which the rule is applied.
     * @param whenFunction  Condition as a function with the input facts as the only parameter
     * @return The rule object
     */
    public Rule<F, R> when(Predicate<F> whenFunction) {

        this.whenFunction = whenFunction;
        if (this.whenDescription == null) {
            this.whenDescription = whenFunction.toString();
        }
        return this;
    }

    /**
     * Define and optional outcome condition under which the rule is applied.
     * @param whenOutcomeFunction  Condition as a function with the outcome as the only parameter
     * @return The rule object
     */
    public Rule<F, R> andWhenOutcome(Predicate<R> whenOutcomeFunction) {

        this.whenOutcomeFunction = whenOutcomeFunction;
        if (this.andWhenOutcomeDescription == null) {
            this.andWhenOutcomeDescription = whenOutcomeFunction.toString();
        }
        return this;
    }

    /**
     * Define the function that is applied, when the condition is true.
     * Do NOT stop with rule processing after the function is applied.
     * @param consumer  The rule's function with input facts and outcome result as the two parameters
     * @return The rule object
     */
    public Rule<F, R> thenProceedWith(Consumer<Outcome<F, R>> consumer) {
        if (this.groupedRules != null) {
            throw new IllegalStateException("thenProceedWith cannot be used, when group rules are defined");
        }
        this.thenFunction = f -> {
            consumer.accept(f);
            return false;
        };
        if (this.thenDescription == null) {
            this.thenDescription = consumer.toString();
        }
        return this;
    }

    /**
     * Define the function that is applied, when the condition is true.
     * Stop with rule processing after the function is applied.
     * @param consumer  The rule's function with input facts and outcome result as the two parameters
     * @return The rule object
     */
    public Rule<F, R> thenStopWith(Consumer<Outcome<F, R>> consumer) {
        if (this.groupedRules != null) {
            throw new IllegalStateException("thenStopWith cannot be used, when group rules are defined");
        }
        this.thenFunction = f -> {
            consumer.accept(f);
            return true;
        };
        if (this.thenDescription == null) {
            this.thenDescription = consumer.toString();
        }
        return this;
    }

    /**
     * Define grouping of rules, with the same when clause.
     * @param groupedRules  A consumer function where rules of the group can be added.
     * @return The rule object
     */
    public Rule<F, R> thenGroupRules(Consumer<RuleBook<F, R>> groupedRules) {

        if (this.thenFunction != null) {
            throw new IllegalStateException("thenGroupRules cannot be used, when thenStopWith/thenProceedWith is already defined");
        }
        this.groupedRules = groupedRules;
        if (this.thenDescription == null) {
            this.thenDescription = groupedRules.toString();
        }
        return this;
    }
}
