# KPar
[![Build Status](https://travis-ci.org/kikofernandez/kpar.svg?branch=master)](https://travis-ci.org/kikofernandez/kpar)

UNFINISHED WORK

Kpar is a parallel collection designed to create pipeline parallelism
(based on the [Encore Parallel combinators](http://link.springer.com/chapter/10.1007%2F978-3-319-18941-3_1)
paper).
 
## Features

The parallel collection is completely asynchronous and interaction is performed
via **parallel combinators**. The library provides a function to *spawn*
asynchronus computations and these can be *lifted* into the collection via
the `liftf` combinator. A task is an asynchronous computation that happens
in other working thread but synchronous computations can be added to the
parallel collection as well via the `liftv` combinator. A spawned task
returns a `CompletableFuture` value.

The following example spawns a task and returns a parallel collection:

```
(defn computation
  [x]
  (let [t (spawn (inc x))]
    (liftf t))
```

Combinators provide high-level constructs for low-level management of the
elements in the parallel collection:

- `|` (read *par*), creates a new parallel collection given a bunch
of parallel collections:

```
(let [x1 (liftv (inc 1))
      x2 (liftv (dec 23))]
      (| x1 x2))
```

- `>>` (read *sequence*), applies a function to each element in the
collection asynchronously.

```
(defn par-example
  [p]
  (p >> inc))
```

- `extract`, combinator that moves a parallel collection to the
sequential world, blocking until all the elements in the collection
have finished their computation and returning an array.

```
(defn extract-example
  []
  (let [x1 (liftv (inc 1))
        x2 (liftv (dec 100))
        p  (| x1 x2)]
    (extract p)))
```


## Usage

TODO: Examples

## Limitations

- Java 8

## License

Copyright © 2015 Kiko Fernandez Reyes

Distributed under the MIT License.
