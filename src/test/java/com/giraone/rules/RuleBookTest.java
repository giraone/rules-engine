package com.giraone.rules;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBookTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RuleBookTest.class);

    static class AnimalFacts {

        String animalName;
        boolean mammal;
        int weightInKg;

        public AnimalFacts(String animalName, boolean mammal, int weightInKg) {
            this.animalName = animalName;
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
        public Result setConclusion(String conclusion) {

            this.conclusion = conclusion;
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
        "false,virus,true,0,A virus cannot be analyzed.,You must set a positive weight.",
        "false,sea hawk,false,1,A sea hawk does not produce milk.,",
        "false,cow,true,750,A cow cannot fly.,",
        "false,whale,true,200000,A whale must live in water. A whale cannot fly.,",
        "true,virus,true,0,A virus cannot be analyzed.,You must set a positive weight.",
        "true,sea hawk,false,1,A sea hawk does not produce milk.,",
        "true,cow,true,750,A cow cannot fly.,",
        "true,whale,true,200000,A whale must live in water. A whale cannot fly.,"
    })
    void applyOnFacts_basicChecksOnSimpleRuleSet(
        boolean withLog, String animal, boolean mammal, int weightInKg, String expectedConclusion, String expectedHint) {

        // arrange
        AnimalFacts animalFacts = new AnimalFacts(animal, mammal, weightInKg);
        Result result = new Result();

        RuleBook<AnimalFacts, Result> ruleBook = new RuleBook<AnimalFacts, Result>()
            .addRule(new Rule<AnimalFacts, Result>()
                .whenFacts("If there is no weight given?")
                .whenFacts(facts -> facts.weightInKg <= 0)
                .thenStopWith("Stop processing and give a hint to set the weight.")
                .thenStopWith(outcome -> {
                    outcome.result.addConclusion("A " + outcome.facts.animalName + " cannot be analyzed.");
                    outcome.result.setHint("You must set a positive weight.");
                })
            )
            .addRule(new Rule<AnimalFacts, Result>()
                .whenFacts("If animal is no mammal?")
                .whenFacts(facts -> !facts.mammal)
                .thenStopWith("Stop processing and conclude, that the animal does not give milk.")
                .thenStopWith(outcome -> outcome.result.addConclusion("A " + outcome.facts.animalName + " does not produce milk."))
            )
            .addRule(new Rule<AnimalFacts, Result>()
                .whenFacts("If animal weights over 100 tons?")
                .whenFacts(facts -> facts.mammal && facts.weightInKg > 100000)
                .thenProceedWith("Conclude, that the animal must live in water, because otherwise its weight would crush it.")
                .thenProceedWith(outcome -> outcome.result.addConclusion("A " + outcome.facts.animalName + " must live in water."))
            )
            .addRule(new Rule<AnimalFacts, Result>()
                .whenFacts("If animal is mammal and weights more than 2kg?")
                .whenFacts(facts -> facts.mammal && facts.weightInKg > 2)
                .thenProceedWith("Conclude, that the animal cannot fly, because the largest flying mammals are \"flying foxes\" and the largest species of them has less than 1.6kg.")
                .thenProceedWith(outcome -> outcome.result.addConclusion("A " + outcome.facts.animalName + " cannot fly."))
            );

        BiConsumer<String, Boolean> logWhen = (description, value) ->
            LOGGER.debug("{} WHEN \"{}\" was {}\u001b[0m", value ? "\u001b[32m" : "\u001b[33m", description, value);
        BiConsumer<String, Boolean> logThen = (description, value) ->
            { if (value) LOGGER.debug("\u001b[31m THEN \"{}\" STOPPED\u001b[0m", description); };

        // act
        Outcome<AnimalFacts, Result> outcome = withLog
            ? ruleBook.applyOnFacts(animalFacts, result, logWhen, logThen)
            : ruleBook.applyOnFacts(animalFacts, result);

        // assert
        assertThat(outcome).isNotNull();
        assertThat(outcome.facts).isNotNull();
        assertThat(outcome.result).isNotNull();
        assertThat(outcome.result.conclusion).isEqualTo(expectedConclusion);
        assertThat(outcome.result.hint).isEqualTo(expectedHint);
    }

    @ParameterizedTest
    @CsvSource({
        "virus,true,0,A virus cannot be analyzed.,You must set a positive weight.",
        "sea hawk,false,1,A sea hawk does not produce milk.,",
        "cow,true,750,A cow cannot fly.,",
        "whale,true,200000,A whale must live in water. A whale cannot fly.,"
    })
    void applyOnFacts_basicChecksOnGroupedRuleSet(
        String animal, boolean mammal, int weightInKg, String expectedConclusion, String expectedHint) {

        // arrange
        AnimalFacts animalFacts = new AnimalFacts(animal, mammal, weightInKg);
        Result result = new Result();

        RuleBook<AnimalFacts, Result> ruleBook = new RuleBook<AnimalFacts, Result>()
            .addRule(new Rule<AnimalFacts, Result>()
                .whenFacts("If there is no weight given?")
                .whenFacts(facts -> facts.weightInKg <= 0)
                .thenStopWith("Stop processing and give a hint to set the weight.")
                .thenStopWith(outcome -> {
                    outcome.result.addConclusion("A " + outcome.facts.animalName + " cannot be analyzed.");
                    outcome.result.setHint("You must set a positive weight.");
                })
            )
            .addRule(new Rule<AnimalFacts, Result>()
                .whenFacts("If animal is a mammal?")
                .whenFacts(facts -> facts.mammal)
                .thenGroupRules(group -> group
                    .addRule(new Rule<AnimalFacts, Result>()
                        .whenFacts("If mammal weights over 100 tons?")
                        .whenFacts(facts -> facts.weightInKg > 100000)
                        .thenStopWith("Conclude, that the animal must live in water, because otherwise its weight would crush it.")
                        .thenProceedWith(outcome -> outcome.result.addConclusion("A " + outcome.facts.animalName + " must live in water."))
                    )
                    .addRule(new Rule<AnimalFacts, Result>()
                        .whenFacts("If mammal weights more than 2kg?")
                        .whenFacts(facts -> facts.weightInKg > 2)
                        .thenStopWith("Conclude, that the animal cannot fly, because the largest flying mammals are \"flying foxes\" and the largest species of them has less than 1.6kg.")
                        .thenProceedWith(outcome -> outcome.result.addConclusion("A " + outcome.facts.animalName + " cannot fly."))
                    ))
            )
            .addRule(new Rule<AnimalFacts, Result>()
                .whenFacts("If animal is no mammal?")
                .whenFacts(facts -> !facts.mammal)
                .thenGroupRules(group -> group
                    .addRule(new Rule<AnimalFacts, Result>()
                        .whenFacts("true")
                        .whenFacts(facts -> true)
                        .thenStopWith("Stop processing and conclude, that the animal does not give milk.")
                        .thenStopWith(outcome -> outcome.result.addConclusion("A " + outcome.facts.animalName + " does not produce milk."))
                    ))
            );

        BiConsumer<String,Boolean> logWhen = (description, r) -> LOGGER.debug("WHEN \"{}\" was {}", description, r);
        BiConsumer<String,Boolean> logThen = (description, r) -> LOGGER.debug("THEN \"{}\" stop {}", description, r);

        // act
        Outcome<AnimalFacts, Result> outcome = ruleBook.applyOnFacts(animalFacts, result, logWhen, logThen);

        // assert
        assertThat(outcome).isNotNull();
        assertThat(outcome.facts).isNotNull();
        assertThat(outcome.result).isNotNull();
        assertThat(outcome.result.conclusion).isEqualTo(expectedConclusion);
        assertThat(outcome.result.hint).isEqualTo(expectedHint);
    }

    @ParameterizedTest
    @CsvSource({
        "whale,true,200000,A whale is not a fish.,super-heavy",
        "whale shark,false,200000,The weight for whale shark is wrong!,super-heavy"
    })
    void applyOnFacts_basicChecksOnOutcomeConditions(
        String animal, boolean mammal, int weightInKg, String expectedConclusion, String expectedHint) {

        // arrange
        AnimalFacts animalFacts = new AnimalFacts(animal, mammal, weightInKg);
        Result result = new Result();

        RuleBook<AnimalFacts, Result> ruleBook = new RuleBook<AnimalFacts, Result>()
            .addRule(new Rule<AnimalFacts, Result>()
                .whenFacts("If animal weights more than 20 tons?")
                .whenFacts(facts -> facts.weightInKg > 20000)
                .thenProceedWith(outcome -> {
                    outcome.result.addConclusion("A " + outcome.facts.animalName + " is not a fish.");
                    outcome.result.setHint("super-heavy");
                })
            )
            .addRule(new Rule<AnimalFacts, Result>()
                .whenFacts("If animal is not a mammal?")
                .whenFacts(facts -> !facts.mammal)
                .whenOutcome("and if it is super heavy, like whales only")
                .whenOutcome(outcome -> "super-heavy".equals(outcome.hint))
                .thenStopWith("then something with the data is wrong!")
                .thenStopWith(outcome -> outcome.result.setConclusion("The weight for " + outcome.facts.animalName + " is wrong!"))
            );

        // act
        Outcome<AnimalFacts, Result> outcome = ruleBook.applyOnFacts(animalFacts, result);

        // assert
        assertThat(outcome).isNotNull();
        assertThat(outcome.facts).isNotNull();
        assertThat(outcome.result).isNotNull();
        assertThat(outcome.result.conclusion).isEqualTo(expectedConclusion);
        assertThat(outcome.result.hint).isEqualTo(expectedHint);
    }
}