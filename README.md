# Context Propagation Library

[![Build Status](https://circleci.com/gh/micrometer-metrics/context-propagation.svg?style=shield)](https://circleci.com/gh/micrometer-metrics/context-propagation)
[![Apache 2.0](https://img.shields.io/github/license/micrometer-metrics/context-propagation.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/io.micrometer/micrometer-context-propagation.svg)](https://search.maven.org/artifact/io.micrometer/context-propagation)
[![Javadocs](https://www.javadoc.io/badge/io.micrometer/micrometer-context-propagation.svg)](https://www.javadoc.io/doc/io.micrometer/context-propagation)
[![Revved up by Gradle Enterprise](https://img.shields.io/badge/Revved%20up%20by-Gradle%20Enterprise-06A0CE?logo=Gradle&labelColor=02303A)](https://ge.micrometer.io/)

## Overview

A library that assists with context propagation across different types of context
mechanisms such as `ThreadLocal`, Reactor [Context](https://projectreactor.io/docs/core/release/reference/#context),
and others.

Abstractions:

* `ThreadLocalAccessor` - contract to assist with access to a `ThreadLocal` value. 
* `ContextAccessor` - contract to assist with access to a `Map`-like context.
* `ContextRegistry` - registry for instances of `ThreadLocalAccessor` and `ContextAccessor`. 
* `ContextSnapshot` - holder of contextual values, that provides methods to capture and to propagate.

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

Snapshots are published to `repo.spring.io` for every successful build on the `main` branch and maintenance branches.

```groovy
repositories {
    maven { url 'https://repo.spring.io/libs-snapshot' }
}

dependencies {
    implementation 'io.micrometer:context-propagation:latest.integration'
}
```

Milestone releases are published to https://repo.spring.io/milestone. Include that as a maven repository in your build
configuration to use milestone releases. Note that milestone releases are for testing purposes and are not intended for
production use.

These artifacts work with Java 8 or later.

## Contributing

See our [Contributing Guide](CONTRIBUTING.md) for information about contributing to Micrometer Tracing.

## Join the discussion

Join the [Micrometer Slack](https://slack.micrometer.io) (#context-propagation channel) to share your questions, concerns, and feature requests.


-------------------------------------
_Licensed under [Apache Software License 2.0](https://www.apache.org/licenses/LICENSE-2.0)_

_Sponsored by [VMware](https://tanzu.vmware.com)_
