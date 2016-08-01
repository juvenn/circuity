# Circuity

A minimal Circuit Breaker implemented for Clojure, it supports customizing
circuit timeout, number of failures to trip circuit, as well as a reset window
to half open circuit.

## Todo

* Throw explicit exceptions?
* Fallback fn when circuit open

## Usage

```
(require '[circuity.core :as cc])

(cc/defcommand http-get
  "Issue a get request"
  {:timeout 5000
   :trip_threshold 5
   :reset_window 50000}
  [url & params]
  ...)
```

The code above defines a circuit command, which has timeout of
5000ms. The circuit will trip if command failed more than 4 times. The
circuit will attempt to half-open if 50,000ms since last failure
passed.

## License

Copyright © 2016 Juvenn Woo

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
