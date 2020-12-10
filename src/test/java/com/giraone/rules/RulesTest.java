package com.giraone.rules;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class RulesTest {

    static class AnimalFacts {

        String animal;
        boolean mammal;
        int weightInKg;

        public AnimalFacts(String animal, boolean mammal, int weightInKg) {
            this.animal = animal;
            this.mammal = mammal;
            this.weightInKg = weightInKg;
        }
    }

    static class Result {

        String conclusion;
        String hint;

        @SuppressWarnings("UnusedReturnValue")
        public Result addConclusion(String conclusion) {
            if (this.conclusion == null) {
                this.conclusion = conclusion;
            } else {
                this.conclusion += " " + conclusion;
            }
            return this;
        }

        @SuppressWarnings("UnusedReturnValue")
        public Result setHint(String hint) {
            this.hint = hint;
            return this;
        }
    }

    @ParameterizedTest
    @CsvSource({
        "virus,true,0,A virus cannot be analyzed.,You must set a positive weight.",
        "bird,false,1,A bird does not produce milk.,",
        "cow,true,750,A cow cannot fly.,",
        "whale,true,100000,A whale must live in water. A whale cannot fly.,"
    })
    void applyOnFacts_basicChecksOnSimpleRuleSet(
        String animal, boolean mammal, int weightInKg, String expectedConclusion, String expectedHint) {

        // arrange
        AnimalFacts animalFacts = new AnimalFacts(animal, mammal, weightInKg);
        Result result = new Result();

        Rules<AnimalFacts, Result> rules = new Rules<AnimalFacts, Result>()
            .addRule(new Rule<AnimalFacts, Result>()
                .when(facts -> facts.weightInKg <= 0)
                .thenStopWith(outcome -> {
                    outcome.result.addConclusion("A " + outcome.facts.animal + " cannot be analyzed.");
                    outcome.result.setHint("You must set a positive weight.");
                })
            )
            .addRule(new Rule<AnimalFacts, Result>()
                .when(facts -> !facts.mammal)
                .thenStopWith(outcome -> outcome.result.addConclusion("A " + outcome.facts.animal + " does not produce milk."))
            )
            .addRule(new Rule<AnimalFacts, Result>()
                .when(facts -> facts.mammal && facts.weightInKg > 1000)
                .thenProceedWith(outcome -> outcome.result.addConclusion("A " + outcome.facts.animal + " must live in water."))
            )
            .addRule(new Rule<AnimalFacts, Result>()
                .when(facts -> facts.mammal && facts.weightInKg > 20)
                .thenProceedWith(outcome -> outcome.result.addConclusion("A " + outcome.facts.animal + " cannot fly."))
            );

        // act
        Outcome<AnimalFacts, Result> outcome = rules.applyOnFacts(animalFacts, result);

        // assert
        assertThat(outcome).isNotNull();
        assertThat(outcome.facts).isNotNull();
        assertThat(outcome.result).isNotNull();
        assertThat(outcome.result.conclusion).isEqualTo(expectedConclusion);
        assertThat(outcome.result.hint).isEqualTo(expectedHint);
    }
}