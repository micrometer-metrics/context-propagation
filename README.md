# Micrometer Context Propagation

[![Build Status](https://circleci.com/gh/micrometer-metrics/context-propagation.svg?style=shield)](https://circleci.com/gh/micrometer-metrics/context-propagation)
[![Apache 2.0](https://img.shields.io/github/license/micrometer-metrics/context-propagation.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/io.micrometer/micrometer-context-propagation.svg)](https://search.maven.org/artifact/io.micrometer/micrometer-context-propagation)
[![Javadocs](https://www.javadoc.io/badge/io.micrometer/micrometer-context-propagation.svg)](https://www.javadoc.io/doc/io.micrometer/micrometer-core)
[![Revved up by Gradle Enterprise](https://img.shields.io/badge/Revved%20up%20by-Gradle%20Enterprise-06A0CE?logo=Gradle&labelColor=02303A)](https://ge.micrometer.io/)

A application context propagation facade.

These artifacts work with Java 8 or later.

## Join the discussion

Join the [Micrometer Slack](https://slack.micrometer.io) to share your questions, concerns, and feature requests.

## Snapshot builds

Snapshots are published to `repo.spring.io` for every successful build on the `main` branch and maintenance branches.

To use:

```groovy
repositories {
    maven { url 'https://repo.spring.io/libs-snapshot' }
}

dependencies {
    implementation 'io.micrometer:context-propagation:latest.integration'
}
```

## Milestone releases

Milestone releases are published to https://repo.spring.io/milestone. Include that as a maven repository in your build
configuration to use milestone releases. Note that milestone releases are for testing purposes and are not intended for
production use.

## Documentation

Glossary
* `ContextContainer` - our bag, we want it to be propagated over various means of communication
* `Context` - mean of communication. That can be e.g. Reactor `Context`, Reactor Netty `Channel` etc.
* `ContextAccessor` - what to take from the Context and put it to the container and vice versa (e.g. if there's a Reactor Context I want to take out and put back a value for key `foo`)
* `ThreadLocalAccessor` - how to take from thread local and put it to the context container and vice versa. (e.g. `ObservationThreadLocalAccessor` will take the value of the current `Observation`)
* `ContextContainerAdapter` - how to write the `ContextContainer` to a Context and from it (e.g. `ReactorContextContainerAdapter` - how to read and write the container to Reactor Context)
* `Namespace` logical grouping for `ThreadLocalAccessor`s. Can be used for filtering out thread local accessors.

## Contributing

See our [Contributing Guide](CONTRIBUTING.md) for information about contributing to Micrometer Tracing.

-------------------------------------
_Licensed under [Apache Software License 2.0](https://www.apache.org/licenses/LICENSE-2.0)_

_Sponsored by [VMware](https://tanzu.vmware.com)_
