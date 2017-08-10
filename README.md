# ICFP Contest 2017

Team "A Storm of Minds"

- Christoph Breitkopf
- Jan Dreske

## Submissions

For convenience, our compiled submissions are available in the folder
`submissions`.

They have a small problem: The `punter` scripts will
only work in the currect directory. [The fix is trivial](https://github.com/jandreske/icfpc2017/commit/cbe50f19db854ce51b1f9145f56a4cd0855b6fe8).

Also, we forgot to disable logging in the lightning submission. When
run, it will create a file `server.log` in the current directory. This
can be safely ignored/deleted.
 
## Strategy

We write several punters during the contest, mostly being extensions
and refinements on the punters that came before them. We spent a lot
of time on futures and splurges, but actually got lower scores with
the code activated, so our final submission uses only options.

Our submitted punter tries two things:

1. connect two mines
2. claim the highest-scoring river connected to any we already own

## Tech 

Our punters are written in Java 8. We tried for functional style, but
a lack of experience with JSON mapping prevented that to some extent.
Also, we later rewrote some functions using streams to explicit loops
for performance.

The choice of Java proved less than ideal because of the huge JVM
  startup times and missing Hotspot optimizations in the 1-second
  runtime limit. Also, we had a some trouble respecting the runtime
  limit and had to sprinkle our code with `Thread.currentThread().isInterrupted()`queries.

Other:

- Groovy: offline game server. We couldn't get the OCaml version to compile
- Shell: tooling
- Haskell: another punter which was not used in the submissions

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


