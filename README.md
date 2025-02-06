# Context Propagation Library

[![Build Status](https://circleci.com/gh/micrometer-metrics/context-propagation.svg?style=shield)](https://circleci.com/gh/micrometer-metrics/context-propagation)
[![Apache 2.0](https://img.shields.io/github/license/micrometer-metrics/context-propagation.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/io.micrometer/context-propagation.svg)](https://search.maven.org/artifact/io.micrometer/context-propagation)
[![Javadocs](https://www.javadoc.io/badge/io.micrometer/context-propagation.svg)](https://www.javadoc.io/doc/io.micrometer/context-propagation)
[![Revved up by Develocity](https://img.shields.io/badge/Revved%20up%20by-Develocity-06A0CE?logo=Gradle&labelColor=02303A)](https://ge.micrometer.io/)

## Overview

A library that assists with context propagation across different types of context
mechanisms such as `ThreadLocal`, Reactor [Context](https://projectreactor.io/docs/core/release/reference/#context),
and others.

Abstractions:

* `ThreadLocalAccessor` - contract to assist with access to a `ThreadLocal` value. 
* `ContextAccessor` - contract to assist with access to a `Map`-like context.
* `ContextRegistry` - registry for instances of `ThreadLocalAccessor` and `ContextAccessor`. 
* `ContextSnapshot` - holder of contextual values, that provides methods to capture and to propagate.

You can read the full [reference documentation here](https://docs.micrometer.io/context-propagation/reference/).

Example Scenarios:

* In imperative code, e.g. Spring MVC controller, capture `ThreadLocal` values into a
`ContextSnapshot`. After that use the snapshot to populate a Reactor `Context` with the
captured values, or to wrap a task (e.g. `Runnable`, `Callable`, etc) or an `Executor`
with a decorator that restores `ThreadLocal` values when the task executes.
* In reactive code, e.g. Spring WebFlux controller, create a `ContextSnapshot` from
Reactor `Context` values. After that use the snapshot to restore `ThreadLocal` values 
within a specific stage (operator) of the reactive chain.

Context values can originate from any context mechanism and propagate to any other, any
number of times. For example, a value in a `Reactor` context may originate as a
`ThreadLocal`, and may yet become a `ThreadLocal` again, and so on.  

Generally, imperative code should interact with `ThreadLocal` values as usual, and
likewise Reactor code should interact with the Reactor `Context` as usual. The Context
Propagation library is not intended to replace those, but to assist with propagation when
crossing from one type of context to another, e.g. when imperative code invokes a Reactor
chain, or when a Reactor chain invokes an imperative component that expects
`ThreadLocal` values.

The library is not limited to context propagation from imperative to reactive. It can
assist in asynchronous scenarios to propagate `ThreadLocal` values from one thread to
another. It can also propagate to any other type of context for which there is a
registered `ContextAccesor` instance.

## Artifacts

The published artifacts work with Java 8 or later.

### Snapshot builds

Snapshots are published to `repo.spring.io` for every successful build on the `main` branch and maintenance branches.

```groovy
repositories {
    maven { url 'https://repo.spring.io/snapshot' }
}

dependencies {
    implementation 'io.micrometer:context-propagation:latest.integration'
}
```

### Milestone releases

Starting with the 1.2.0-M1 release, milestone releases and release candidates will be published to Maven Central. Note that milestone releases are for testing purposes and are not intended for production use. Earlier milestone releases were published to https://repo.spring.io/milestone.

## Contributing

See our [Contributing Guide](CONTRIBUTING.md) for information about contributing to Micrometer Context Propagation.

## Code formatting

The [spring-javaformat plugin](https://github.com/spring-io/spring-javaformat) is configured to check and apply consistent formatting in the codebase through the build.
The `checkFormat` task checks the formatting as part of the `check` task.
Apply formatting with the `format` task.
You should rely on the formatting the `format` task applies instead of your IDE's configured formatting.

## Join the discussion

Join the [Micrometer Slack](https://slack.micrometer.io) (#context-propagation channel) to share your questions, concerns, and feature requests.


-------------------------------------
_Licensed under [Apache Software License 2.0](https://www.apache.org/licenses/LICENSE-2.0)_

_Sponsored by [VMware](https://tanzu.vmware.com)_
