Circuity - A minimal Circuit Breaker for Clojure
====

A [Circuit Breaker][1] wraps a function call, mostly a costly one
e.g. an http request, a database operation, or alike. Circuit breaker
monitors the call for failures and timeout, once the number of
failures reaches a threshold, the circuit breaker will trip (or in a
state of open), and further calls will return error right away without
actually execute the call. Thus it can prevent costly operations, upon
frequent failures, from taking too much resources like threads,
connections etc.

Circuity is an exercise implementation in Clojure.

Install
----

Add this dependency to `project.clj`:

```clj
[circuity/circuity "0.1.0"]
```

Usage
----

```clj
(require '[circuity.core :as cc])

(cc/defcommand http-get
  "HTTP get request"
  {:timeout 5000
   :trip_threshold 5
   :reset_window 50000}
  [url & params]
  ...)

;; invoking it like a regular function
(http-get "https://api.twitter.com/1.1/search/tweets.json"
          {:q "clojure"})
```

`:timeout` specifies how long to wait for function execution before it
timeouts, and throws an Exception.

`:trip_threshold` is a number of failures when reached, will trip the
circuit breaker, and throws an Exception right away without executing
function.

`:reset_window` specifies a failure time window that will keep the
circuit open, not executing call in the window. And after window, the
circuit breaker will attempt to heal by letting a call pass
through. It recovers to normal if the *attempting-call* succeeds, or
remains open until another time window ends.

References
----

* Martin Fowler on [Circuit Breaker][1].
* Netflix [Hystrix][2]: a scalable and reliable implementation of
  circuit breaker in Java, that offers full of metrics for monitoring
  system. It also comes with a [clojure wrapper][3].

Development
----

There are a few tests that could be run with:

```
lein test
```

Feedback and bugs report are welcome.

License
----

Copyright © 2016 Juvenn Woo

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

[1]: http://martinfowler.com/bliki/CircuitBreaker.html "Circuit Breaker"
[2]: https://github.com/Netflix/Hystrix/ "Hystrix"
[3]: https://github.com/Netflix/Hystrix/tree/master/hystrix-contrib/hystrix-clj
