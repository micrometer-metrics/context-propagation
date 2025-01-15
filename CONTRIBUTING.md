# Contributing Guide

This Contributing Guide is intended for those that would like to contribute to Micrometer Context Propagation.

If you would like to use any of the published Micrometer Context Propagation modules as a library in your project, you can instead
include the Micrometer Context Propagation artifacts from the Maven Central repository using your build tool of choice.

## Code of Conduct

See [our Contributor Code of Conduct](https://github.com/micrometer-metrics/.github/blob/main/CODE_OF_CONDUCT.md).

## Contributions

Contributions come in various forms and are not limited to code changes. The Micrometer Context Propagation community benefits from
contributions in all forms.

For example, those with Micrometer Context Propagation knowledge and experience can contribute by:

* TODO: [Contributing documentation]()
* Answering [Stackoverflow questions](https://stackoverflow.com/tags/micrometer)
* Answering questions on the [Micrometer slack](https://slack.micrometer.io)
* Share Micrometer Context Propagation knowledge in other ways (e.g. presentations, blogs)

The remainder of this document will focus on guidance for contributing code changes. It will help contributors to build,
modify, or test the Micrometer Context Propagation source code.

## Include a Signed Off By Trailer

All commits must include a *Signed-off-by* trailer at the end of each commit message to indicate that the contributor agrees to the [Developer Certificate of Origin](https://developercertificate.org).
For additional details, please refer to the blog post [Hello DCO, Goodbye CLA: Simplifying Contributions to Spring](https://spring.io/blog/2025/01/06/hello-dco-goodbye-cla-simplifying-contributions-to-spring).

## Getting the source

The Micrometer Context Propagation source code is hosted on GitHub at https://github.com/micrometer-metrics/context-propagation. You can use a
Git client to clone the source code to your local machine.

## Building

Micrometer Context Propagation targets Java 8 but requires JDK 11 or later to build.

The Gradle wrapper is provided and should be used for building with a consistent version of Gradle.

The wrapper can be used with a command, for example, `./gradlew check` to build the project and check conventions.

## Importing into an IDE

This repository should be imported as a Gradle project into your IDE of choice.

## Testing changes locally

Specific modules or a test class can be run from your IDE for convenience.

The Gradle `check` task depends on the `test` task, and so tests will be run as part of a build as described previously.

### Publishing local snapshots

Run `./gradlew pTML` to publish a Maven-style snapshot to your Maven local repo. The build automatically calculates
the "next" version for you when publishing snapshots.

These local snapshots can be used in another project to test the changes. For example:

```groovy
repositories {
    mavenLocal()
}

dependencies {
    implementation 'io.micrometer:context-propagation:latest.integration'
}
```
