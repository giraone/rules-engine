# Rules Engine

A simple Java based rules engine library based on
- generics and POJOs for input facts and outcome results
- Java 8 streams
- functional Java programming with predicates, consumers and functions
- Java based fluent style rules definitions

The core objects of rules are
- `<F>` The POJO input facts class.
- `<R>` The POJO outcome result class.

The core implementation is nothing more than these lines:

```java
    public Outcome<F,R> applyOnFacts(F facts, R result) {

      final Outcome<F,R> outcome = new Outcome<>(facts, result);
      final AtomicBoolean stopped = new AtomicBoolean(false);

      rules.stream()
      .filter(rule -> !stopped.get() && rule.whenFunction.test(facts))
      .forEach(rule -> stopped.set(rule.thenFunction.test(outcome)));

      return outcome;
}
```

A single rule itself is a *when* / *then* pair, where
- *when* is a `Predicate`
- *then* is a `Consumer` that can set an outcome, either
  - with stopping the whole rules processing using *thenStopWith()* or
  - it can only change the outcome without stopping using *thenProceedWith()*, so following rules can also
    change the outcome

```java
/**
 * A rule based on facts F with a result outcome R
 * @param <F>   The input facts class.
 * @param <R>   The outcome result class.
 */
public class Rule<F,R> {

  Predicate<F> whenFunction;
  Predicate<Outcome<F,R>> thenFunction;

  Rule<F,R> when(Predicate<F> whenFunction) {
    this.whenFunction = whenFunction;
    return this;
  }

  Rule<F,R> thenProceedWith(Consumer<Outcome<F,R>> consumer) {
    this.thenFunction = f -> { consumer.accept(f); return false; };
    return this;
  }

  Rule<F,R> thenStopWith(Consumer<Outcome<F,R>> consumer) {
    this.thenFunction = f -> { consumer.accept(f); return true; };
    return this;
  }
}
```

The full functionality and the rule definition, can be seen in the following test class:

```java
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
    
    public Result addConclusion(String conclusion) {
      if (this.conclusion == null) {
        this.conclusion = conclusion;
      } else {
        this.conclusion += " " + conclusion;
      }
      return this;
    }
    
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
```

## Build

- Use JDK 11
- `mvn package`

## Release Notes

- 1.0.0 (2020-12-12)
    - Initial version
