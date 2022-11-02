# Rules Engine

A simple Java based rules engine library based on
- generics and POJOs for input facts and output results
- Java 8 streams
- functional Java programming with predicates, consumers and functions
- Java based fluent style rules definitions

The core objects of a rule book (ordered List of rules) are
- `<F>` The POJO input facts class.
- `<R>` The POJO output result class.

The core implementation is nothing more than these lines (**logging and grouping of rules is left out**):

```java
public class RuleBook<F, R> {
    
  private final List<Rule<F, R>> rules = new ArrayList<>();

  public Outcome<F, R> applyOnFacts(F facts, R result) {

    final Outcome<F, R> outcome = new Outcome<>(facts, result);
    final AtomicBoolean stopped = new AtomicBoolean(false);

    rules.stream()
      .filter(rule -> !stopped.get() && rule.whenFunction.test(facts))
      .forEach(rule -> stopped.set(rule.thenFunction.test(outcome)));

    return outcome;
  }
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
 *
 * @param <F> The input facts class.
 * @param <R> The output result class.
 */
public class Rule<F, R> {

  Predicate<F> whenFunction;
  Predicate<Outcome<F, R>> thenFunction;

  public Rule<F, R> whenFacts(Predicate<F> whenFunction) {
    this.whenFunction = whenFunction;
    return this;
  }

  public Rule<F, R> thenProceedWith(Consumer<Outcome<F, R>> consumer) {
    this.thenFunction = f -> {
      consumer.accept(f);
      return false;
    };
    return this;
  }

  public Rule<F, R> thenStopWith(Consumer<Outcome<F, R>> consumer) {
    this.thenFunction = f -> {
      consumer.accept(f);
      return true;
    };
    return this;
  }
}
```

A complete rule book is an ordered list of single rules.

The full functionality and the rule definition, can be seen in the following test class:

```java
class RuleBookTest {

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
          "sea hawk,false,9,A sea hawk does not produce milk.,",
          "cow,true,750,A cow cannot fly.,",
          "whale,true,200000,A whale must live in water. A whale cannot fly.,"
  })
  void applyOnFacts_basicChecksOnSimpleRuleSet(
          String animalName, boolean mammal, int weightInKg, String expectedConclusion, String expectedHint) {

    // arrange
    AnimalFacts animalFacts = new AnimalFacts(animalName, mammal, weightInKg);
    Result result = new Result();

    RuleBook<AnimalFacts, Result> ruleBook = new RuleBook<AnimalFacts, Result>()
            .addRule(new Rule<AnimalFacts, Result>()
                    .whenFacts(facts -> facts.weightInKg <= 0)
                    .thenStopWith(outcome -> {
                      outcome.result.addConclusion("A " + outcome.facts.animalName + " cannot be analyzed.");
                      outcome.result.setHint("You must set a positive weight.");
                    })
            )
            .addRule(new Rule<AnimalFacts, Result>()
                    .whenFacts(facts -> !facts.mammal)
                    .thenStopWith(outcome -> outcome.result.addConclusion("A " + outcome.facts.animalName + " does not produce milk."))
            )
            .addRule(new Rule<AnimalFacts, Result>()
                    .whenFacts(facts -> facts.mammal && facts.weightInKg > 100000)
                    .thenProceedWith(outcome -> outcome.result.addConclusion("A " + outcome.facts.animalName + " must live in water."))
            )
            .addRule(new Rule<AnimalFacts, Result>()
                    .whenFacts(facts -> facts.mammal && facts.weightInKg > 2)
                    .thenProceedWith(outcome -> outcome.result.addConclusion("A " + outcome.facts.animalName + " cannot fly."))
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
```

## More features

### Grouping

`when` clauses with the same condition can be defined once and re-used using groups. A group is a list This is as group definition example, where
the condition `facts -> facts.mammal` "forms" the group:

```java
.addRule(new Rule<AnimalFacts, Result>()
    .whenFacts(facts -> facts.mammal)
    .thenGroupRules(group -> group
        .addRule(new Rule<AnimalFacts, Result>()
            .whenFacts(facts -> facts.weightInKg > 100000)
            .thenProceedWith(outcome -> outcome.result.addConclusion("A " + outcome.facts.animalName + " must live in water."))
        )
        .addRule(new Rule<AnimalFacts, Result>()
            .whenFacts(facts -> facts.weightInKg > 2)
            .thenProceedWith(outcome -> outcome.result.addConclusion("A " + outcome.facts.animalName + " cannot fly."))
        )
    )
);
```

### Conditions on outcome

Sometimes the simple *lambda* `outcome = f(facts)` would lead to complex rule books, because conditions on `outcome` are also needed.
To help in these situations a *lamdba* `outcome = f(facts, outcome)` can be defined, where the already defined outcome can be used
as a condition:

```java
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
  .whenOutcome(outcome -> "super-heavy".equals(outcome.hint))
  .thenStopWith(outcome -> outcome.result.setConclusion("The weight for " + outcome.facts.animalName + " is wrong!"))
);
```

whenOutcome

### Descriptions

For each `then`, `when` and `whenOutcome` a descriptive text can be added:

```java
.addRule(new Rule<AnimalFacts, Result>()
    .whenFacts("If mammal weights more than 2kg?")
    .whenFacts(facts -> facts.weightInKg > 2)
    .thenProceedWith("Conclude, that the animal cannot fly, because the largest flying mammals are \"flying foxes\" and the largest species of them has less than 1.6kg.")
    .thenProceedWith(outcome -> outcome.result.addConclusion("A " + outcome.facts.animalName + " cannot fly."))
);
```

### Logging

The descriptive text can be used also, when the single steps should be logged. To enable logging a log function for `when` and `then` can
be defined and passed to `applyOnFacts()`:

```java
final Result result = new Result();
if (log.isDebugEnabled()) {
    BiConsumer<String, Boolean> logWhen = (description, value) -> log.debug("WHEN \"{}\" was {}", description, value);
    BiConsumer<String, Boolean> logThen = (description, value) -> log.debug("THEN \"{}\" stop {}", description, value);
    ruleBook.applyOnFacts(inputFacts, result, logWhen, logThen);
} else {
    ruleBook.applyOnFacts(inputFacts, result);
}
log.debug("applyOnFacts result={}", result);
```

During development, it can be also useful to use ANSI color codes in the log functions.

```java
BiConsumer<String, Boolean> logWhen = (description, value) ->
    log.debug("{} WHEN \"{}\" was {}\u001b[0m", value ? "\u001b[33m" : "\u001b[32m", description, value);
BiConsumer<String, Boolean> logThen = (description, value) ->
    { if (value) log.debug("\u001b[31m THEN \"{}\" STOPPED\u001b[0m", description); };
```

This will output sth. like

```diff
+ WHEN "If there is no weight given?" was false
! WHEN "If animal is no mammal?" was true
- THEN "Stop processing and conclude, that the animal does not give milk.
```

---

## Build

- Use JDK 8+
- `mvn package`

## Release Notes

- 1.2.2 (2022-11-02)
  - maven versions plugin added
  - Upgrade of all dependencies to the latest versions
- 1.2.1 (2021-01-03)
  - First release published to Maven Central
- 1.2.0 (2020-12-18)
  - "Description" and "when" methods renamed
- 1.1.0 (2020-12-17)
  - Version with groupedRules
  - Rules class renamed to RuleBook
- 1.0.0 (2020-12-18)
  - Initial version
