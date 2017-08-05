# ICFP Contest 2017

Team "A Storm of Minds"

- Christoph Breitkopf <chbreitkopf@gmail.com>
- Jan Dreske

## Tech

Languages used:

- Java 8 (punter. We tried for functional style, but a lack of experiance with JSON mapping
  prevented that to a large part)
- Groovy (offline game server. We couldn't get the OCaml version to compile)
- Shell (tooling)
 
## Strategy

TO DO

## Compiling and running

The punter is built using gradle. Running

    ./gradlew build

in the project directory will download all dependencies, build, and run tests.

Start the punter with

    java -jar build/libs/icfpc2017.jar [solver]

where _solver_ can optionally specify the solver to use. If it's not present,
we use the one we've found to be best on average.

## Tools

We wrote our own offline game server. You need to have groovy installed to run it:

    groovy ops/OfflineServer.groovy mapFile punter1 punter2 ...


