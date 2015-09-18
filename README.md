# clj-bucket

A low-level implementation of the [token bucket](https://en.wikipedia.org/wiki/Token_bucket) algorithm in Clojure.

Function calls are throttled explicitly with the bucket per function call. Buckets are implemented as core.async
channels and are returned to the called in verbatim; this allows the caller to close the bucket when needed.

The [throttler](https://github.com/brunoV/throttler) library served as an inspiration for this library. One disadvantage of the library is that the throttled function keeps three core.async channels open internally which cannot be explicitly closed. This can lead to a channel leak in situations where the throttling needs to frequently change. By exposing the throttling channel this library allows the caller to close it and avoid the leak.

## Usage

Add clj-bucket as a dependency to your `project.clj`:

```clj
[clj-bucket 0.2.0]
```

Then in your namespace, import `clj-bucket.core`:

```clj
(require '[clj-bucket.core :refer [bucket throttle]))
```

To create a token bucket:

```clj
(bucket 5 1 :second)
```

This gives you back a bucket that allows 1 function call per second,
with a burstiness (capacity) of 5 tokens in total. Tokens that exceed the capacity
of the bucket are discarded.

Function calls are throttled explicitly per function call with the bucket:

```clj
(throttle bucket + 1 1)
;=> 2
```

If there is a token present in the bucket it will be removed and the function executed.
If the bucket is empty the call will block until a token becomes available.

## License

Copyright © 2015 Shkodran Gerguri

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
