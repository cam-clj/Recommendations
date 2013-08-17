# recommendations

Project outline for the Cambridge Clojure User Group hands-on Clojure
event where we implement a movie recommender using collaborative
filtering algorithms.

## The Algorithm

Wikipedia has a good overview of [collaborative
filtering](http://en.wikipedia.org/wiki/Collaborative_filtering).
We'll implemenent a
[memory-based](http://en.wikipedia.org/wiki/Collaborative_filtering#Memory-based)
algorithm.

For more details and background reading, check out Chapter 2 of
[Programming Collective
Intelligence](http://shop.oreilly.com/product/9780596529321.do).

## Setting Up Your Environment

If you're new to Clojure, check out the [Getting Started
Guide](http://dev.clojure.org/display/doc/Getting+Started). It would
be useful to setup your development environment and install
[Leiningen](http://leiningen.org/) before you come along.

Clone (or fork) this git repository, and run `lein deps` to pull in
some useful libraries and test data. 

Don't panic if you can't get this far: there should be someone at the
event who can help.

## The MovieLens data set

We'll work with a dataset of users and film ratings from MovieLens,
see [MovieLens Data Sets](http://www.grouplens.org/node/73) for
download links. We need the MovieLens 100k dataset:

    mkdir -p resources/data
    cd resources/data
    wget http://www.grouplens.org/system/files/ml-100k.zip
    unzip ml-100k.zip

If possible, please download the test data before the Meetup, in case
the pub's wi-fi is not up to the job.

## License

Copyright © 2013 Ray Miller <ray@1729.org.uk>.

Distributed under the Eclipse Public License, the same as Clojure.