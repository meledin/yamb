Abstract Message Broker (amb)
===

The Abstract Message Broker is a shim over the message broker paradigm, much like *slf4j* abstracts away the underlying logging API. Use *amb* when a message broker is needed, but the *exact implementation* is less than interesting. Change the dependency at compile or runtime to use a different message broker.

Why change? When testing, it is useful to have many things embedded; with amb, it is possible to use an embedded broker, like *tmb*, for smaller unit tests. For integration tests, or deployment, scale up to the full power of a larger, distributed broker without any recompilation or code changes.