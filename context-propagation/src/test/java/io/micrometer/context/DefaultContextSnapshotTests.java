/**
 * Copyright 2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micrometer.context;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.micrometer.context.ContextSnapshot.Scope;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.BDDAssertions.then;

/**
 * Unit tests for {@link DefaultContextSnapshot}.
 *
 * @author Rossen Stoyanchev
 * @author Dariusz Jędrzejczyk
 */
public class DefaultContextSnapshotTests {

    private final ContextRegistry registry = new ContextRegistry();

    @ParameterizedTest(name = "clearMissing={0}")
    @ValueSource(booleans = { true, false })
    void should_propagate_thread_local(boolean clearMissing) {
        ContextSnapshotFactory snapshotFactory = ContextSnapshotFactory.builder()
            .contextRegistry(registry)
            .clearMissing(clearMissing)
            .build();

        this.registry.registerThreadLocalAccessor(new StringThreadLocalAccessor());

        StringThreadLocalHolder.setValue("hello");
        ContextSnapshot snapshot = snapshotFactory.captureAll();

        StringThreadLocalHolder.setValue("hola");
        try {
            try (Scope scope = snapshot.setThreadLocals()) {
                then(StringThreadLocalHolder.getValue()).isEqualTo("hello");
            }
            then(StringThreadLocalHolder.getValue()).isEqualTo("hola");
        }
        finally {
            StringThreadLocalHolder.reset();
        }
    }

    @ParameterizedTest(name = "clearMissing={0}")
    @ValueSource(booleans = { true, false })
    void should_propagate_single_thread_local_value(boolean clearMissing) {
        ContextSnapshotFactory snapshotFactory = ContextSnapshotFactory.builder()
            .contextRegistry(registry)
            .clearMissing(clearMissing)
            .build();

        this.registry.registerContextAccessor(new TestContextAccessor());
        this.registry.registerThreadLocalAccessor(new StringThreadLocalAccessor());

        String key = StringThreadLocalAccessor.KEY;
        Map<String, String> sourceContext = Collections.singletonMap(key, "hello");

        StringThreadLocalHolder.setValue("hola");
        try {
            try (Scope scope = snapshotFactory.setThreadLocalsFrom(sourceContext, key)) {
                then(StringThreadLocalHolder.getValue()).isEqualTo("hello");
            }
            then(StringThreadLocalHolder.getValue()).isEqualTo("hola");
        }
        finally {
            StringThreadLocalHolder.reset();
        }
    }

    @ParameterizedTest(name = "clearMissing={0}")
    @ValueSource(booleans = { true, false })
    void should_override_context_values_when_many_contexts(boolean clearMissing) {
        ContextSnapshotFactory snapshotFactory = ContextSnapshotFactory.builder()
            .contextRegistry(registry)
            .clearMissing(clearMissing)
            .build();

        this.registry.registerContextAccessor(new TestContextAccessor());

        String key = StringThreadLocalAccessor.KEY;
        Map<String, String> firstContext = Collections.singletonMap(key, "hello");
        Map<String, String> secondContext = Collections.singletonMap(key, "override");
        try {
            ContextSnapshot contextSnapshot = snapshotFactory.captureFrom(firstContext, secondContext);
            contextSnapshot.wrap(() -> {
                then(StringThreadLocalHolder.getValue()).isEqualTo("override");
            });
        }
        finally {
            StringThreadLocalHolder.reset();
        }
    }

    @ParameterizedTest(name = "clearMissing={0}")
    @ValueSource(booleans = { true, false })
    void should_filter_thread_locals_on_capture(boolean clearMissing) {
        ContextSnapshotFactory snapshotFactory = ContextSnapshotFactory.builder()
            .captureKeyPredicate(key -> key.equals("foo"))
            .clearMissing(clearMissing)
            .contextRegistry(registry)
            .build();
        ThreadLocal<String> fooThreadLocal = new ThreadLocal<>();
        ThreadLocal<String> barThreadLocal = new ThreadLocal<>();

        this.registry.registerThreadLocalAccessor(new TestThreadLocalAccessor("foo", fooThreadLocal))
            .registerThreadLocalAccessor(new TestThreadLocalAccessor("bar", barThreadLocal));

        fooThreadLocal.set("fooValue");
        barThreadLocal.set("barValue");

        ContextSnapshot snapshot = snapshotFactory.captureAll();

        fooThreadLocal.remove();
        barThreadLocal.remove();

        try (Scope scope = snapshot.setThreadLocals()) {
            then(fooThreadLocal.get()).isEqualTo("fooValue");
            then(barThreadLocal.get()).isNull();
        }

        then(fooThreadLocal.get()).isNull();
        then(barThreadLocal.get()).isNull();
    }

    @ParameterizedTest(name = "clearMissing={0}")
    @ValueSource(booleans = { true, false })
    void should_filter_thread_locals_on_restore(boolean clearMissing) {
        ContextSnapshotFactory snapshotFactory = ContextSnapshotFactory.builder()
            .contextRegistry(registry)
            .clearMissing(clearMissing)
            .build();

        ThreadLocal<String> fooThreadLocal = new ThreadLocal<>();
        ThreadLocal<String> barThreadLocal = new ThreadLocal<>();

        this.registry.registerThreadLocalAccessor(new TestThreadLocalAccessor("foo", fooThreadLocal))
            .registerThreadLocalAccessor(new TestThreadLocalAccessor("bar", barThreadLocal));

        fooThreadLocal.set("fooValue");
        barThreadLocal.set("barValue");

        ContextSnapshot snapshot = snapshotFactory.captureAll();

        fooThreadLocal.remove();
        barThreadLocal.remove();

        try (Scope scope = snapshot.setThreadLocals(key -> key.equals("foo"))) {
            then(fooThreadLocal.get()).isEqualTo("fooValue");
            then(barThreadLocal.get()).isNull();
        }

        try (Scope scope = snapshot.setThreadLocals(key -> key.equals("bar"))) {
            then(fooThreadLocal.get()).isNull();
            then(barThreadLocal.get()).isEqualTo("barValue");
        }

        then(fooThreadLocal.get()).isNull();
        then(barThreadLocal.get()).isNull();
    }

    @ParameterizedTest(name = "clearMissing={0}")
    @ValueSource(booleans = { true, false })
    void should_not_fail_on_empty_thread_local(boolean clearMissing) {
        ContextSnapshotFactory snapshotFactory = ContextSnapshotFactory.builder()
            .contextRegistry(registry)
            .clearMissing(clearMissing)
            .build();
        this.registry.registerThreadLocalAccessor(new StringThreadLocalAccessor());

        then(StringThreadLocalHolder.getValue()).isNull();

        ContextSnapshot snapshot = snapshotFactory.captureAll();

        StringThreadLocalHolder.reset();
        then(StringThreadLocalHolder.getValue()).isNull();

        try (Scope scope = snapshot.setThreadLocals()) {
            then(StringThreadLocalHolder.getValue()).isNull();
        }

        then(StringThreadLocalHolder.getValue()).isNull();
    }

    @ParameterizedTest(name = "clearMissing={0}")
    @ValueSource(booleans = { true, false })
    @SuppressWarnings("unchecked")
    void should_ignore_null_value_in_source_context(boolean clearMissing) {
        ContextSnapshotFactory snapshotFactory = ContextSnapshotFactory.builder()
            .contextRegistry(registry)
            .clearMissing(clearMissing)
            .build();

        String key = "foo";
        ThreadLocal<String> fooThreadLocal = new ThreadLocal<>();
        TestThreadLocalAccessor fooThreadLocalAccessor = new TestThreadLocalAccessor(key, fooThreadLocal);

        this.registry.registerContextAccessor(new TestContextAccessor());
        this.registry.registerThreadLocalAccessor(fooThreadLocalAccessor);

        // We capture null from an uninitialized ThreadLocal:
        String emptyValue = fooThreadLocalAccessor.getValue();
        Map<String, String> sourceContext = Collections.singletonMap(key, emptyValue);

        ContextSnapshot snapshot = snapshotFactory.captureFrom(sourceContext);

        HashMap<Object, Object> snapshotStorage = (HashMap<Object, Object>) snapshot;
        assertThat(snapshotStorage).isEmpty();

        try (Scope scope = snapshot.setThreadLocals()) {
            assertThat(fooThreadLocalAccessor.getValue()).isEqualTo(emptyValue);
        }
        assertThat(fooThreadLocalAccessor.getValue()).isEqualTo(emptyValue);
    }

    @ParameterizedTest(name = "clearMissing={0}")
    @ValueSource(booleans = { true, false })
    void should_ignore_null_mapping_in_source_context_when_skipping_intermediate_snapshot(boolean clearMissing) {
        ContextSnapshotFactory snapshotFactory = ContextSnapshotFactory.builder()
            .contextRegistry(registry)
            .clearMissing(clearMissing)
            .build();

        String key = "foo";
        ThreadLocal<String> fooThreadLocal = new ThreadLocal<>();
        TestThreadLocalAccessor fooThreadLocalAccessor = new TestThreadLocalAccessor(key, fooThreadLocal);

        this.registry.registerContextAccessor(new TestContextAccessor());
        this.registry.registerThreadLocalAccessor(fooThreadLocalAccessor);

        // We capture null from an uninitialized ThreadLocal:
        String emptyValue = fooThreadLocalAccessor.getValue();
        Map<String, String> sourceContext = Collections.singletonMap(key, emptyValue);

        // Validate setting all values
        try (Scope scope = snapshotFactory.setThreadLocalsFrom(sourceContext)) {
            assertThat(fooThreadLocalAccessor.getValue()).isEqualTo(emptyValue);
        }
        assertThat(fooThreadLocalAccessor.getValue()).isEqualTo(emptyValue);

        // Validate setting a subset of values
        try (Scope scope = snapshotFactory.setThreadLocalsFrom(sourceContext, key)) {
            assertThat(fooThreadLocalAccessor.getValue()).isEqualTo(emptyValue);
        }
        assertThat(fooThreadLocalAccessor.getValue()).isEqualTo(emptyValue);
    }

    @ParameterizedTest(name = "clearMissing={0}")
    @ValueSource(booleans = { true, false })
    @SuppressWarnings("unchecked")
    void should_fail_assertion_if_null_value_makes_it_into_snapshot(boolean clearMissing) {
        ContextSnapshotFactory snapshotFactory = ContextSnapshotFactory.builder()
            .contextRegistry(registry)
            .clearMissing(clearMissing)
            .build();

        ThreadLocal<String> fooThreadLocal = new ThreadLocal<>();
        TestThreadLocalAccessor fooThreadLocalAccessor = new TestThreadLocalAccessor("foo", fooThreadLocal);
        this.registry.registerThreadLocalAccessor(fooThreadLocalAccessor);

        fooThreadLocal.set("present");

        ContextSnapshot snapshot = snapshotFactory.captureAll();
        fooThreadLocal.remove();

        HashMap<Object, Object> snapshotStorage = (HashMap<Object, Object>) snapshot;
        // Imitating a broken implementation that let mapping to null into the storage:
        snapshotStorage.put("foo", null);

        assertThatExceptionOfType(AssertionError.class).isThrownBy(snapshot::setThreadLocals)
            .withMessage("snapshot contains disallowed null mapping for key: foo");
    }

    @ParameterizedTest(name = "clearMissing={0}")
    @ValueSource(booleans = { true, false })
    void toString_should_include_values(boolean clearMissing) {
        ContextSnapshotFactory snapshotFactory = ContextSnapshotFactory.builder()
            .contextRegistry(registry)
            .clearMissing(clearMissing)
            .build();

        ThreadLocal<String> fooThreadLocal = new ThreadLocal<>();
        ThreadLocal<String> barThreadLocal = new ThreadLocal<>();

        this.registry.registerThreadLocalAccessor(new TestThreadLocalAccessor("foo", fooThreadLocal))
            .registerThreadLocalAccessor(new TestThreadLocalAccessor("bar", barThreadLocal));

        fooThreadLocal.set("fooValue");
        barThreadLocal.set("barValue");

        assertThat(snapshotFactory.captureAll().toString())
            .isEqualTo("DefaultContextSnapshot{bar=barValue, foo=fooValue}");

        fooThreadLocal.remove();
        barThreadLocal.remove();
    }

    @Nested
    class ClearingTests {

        private final ContextSnapshotFactory snapshotFactory = ContextSnapshotFactory.builder()
            .contextRegistry(registry)
            .clearMissing(true)
            .build();

        @Test
        void should_clear_missing_thread_local() {
            ThreadLocal<String> fooThreadLocal = new ThreadLocal<>();
            TestThreadLocalAccessor fooThreadLocalAccessor = new TestThreadLocalAccessor("foo", fooThreadLocal);
            registry.registerThreadLocalAccessor(fooThreadLocalAccessor);

            fooThreadLocal.set("present");

            ContextSnapshot snapshot = snapshotFactory.captureFrom();
            try (Scope scope = snapshot.setThreadLocals()) {
                assertThat(fooThreadLocal.get()).isNull();
            }
            assertThat(fooThreadLocal.get()).isEqualTo("present");
        }

        @Test
        void should_clear_only_selected_thread_locals_when_filter_in_set() {
            ThreadLocal<String> fooThreadLocal = new ThreadLocal<>();
            ThreadLocal<String> barThreadLocal = new ThreadLocal<>();
            TestThreadLocalAccessor fooThreadLocalAccessor = new TestThreadLocalAccessor("foo", fooThreadLocal);
            TestThreadLocalAccessor barThreadLocalAccessor = new TestThreadLocalAccessor("bar", barThreadLocal);
            registry.registerThreadLocalAccessor(fooThreadLocalAccessor)
                .registerThreadLocalAccessor(barThreadLocalAccessor);

            fooThreadLocal.set("present");
            barThreadLocal.set("present");

            ContextSnapshot snapshot = snapshotFactory.captureFrom();
            try (Scope scope = snapshot.setThreadLocals(key -> key.equals("foo"))) {
                assertThat(fooThreadLocal.get()).isNull();
                assertThat(barThreadLocal.get()).isEqualTo("present");
            }
            assertThat(fooThreadLocal.get()).isEqualTo("present");
            assertThat(barThreadLocal.get()).isEqualTo("present");
        }

        @Test
        void should_clear_other_thread_locals_when_filter_in_capture() {
            ContextSnapshotFactory snapshotFactory = ContextSnapshotFactory.builder()
                .contextRegistry(registry)
                .clearMissing(true)
                .captureKeyPredicate(key -> key.equals("foo"))
                .build();

            ThreadLocal<String> fooThreadLocal = new ThreadLocal<>();
            ThreadLocal<String> barThreadLocal = new ThreadLocal<>();
            TestThreadLocalAccessor fooThreadLocalAccessor = new TestThreadLocalAccessor("foo", fooThreadLocal);
            TestThreadLocalAccessor barThreadLocalAccessor = new TestThreadLocalAccessor("bar", barThreadLocal);
            registry.registerThreadLocalAccessor(fooThreadLocalAccessor)
                .registerThreadLocalAccessor(barThreadLocalAccessor);

            fooThreadLocal.set("present");
            barThreadLocal.set("present");

            ContextSnapshot snapshot = snapshotFactory.captureAll();

            try (Scope scope = snapshot.setThreadLocals()) {
                assertThat(fooThreadLocal.get()).isEqualTo("present");
                assertThat(barThreadLocal.get()).isNull();
            }
            assertThat(fooThreadLocal.get()).isEqualTo("present");
            assertThat(barThreadLocal.get()).isEqualTo("present");
        }

    }

    @Nested
    class DefaultTests {

        private final ContextSnapshotFactory snapshotFactory = ContextSnapshotFactory.builder()
            .contextRegistry(registry)
            .clearMissing(false)
            .build();

        @Test
        void should_not_touch_other_thread_locals_when_filter_in_set() {
            ThreadLocal<String> fooThreadLocal = new ThreadLocal<>();
            ThreadLocal<String> barThreadLocal = new ThreadLocal<>();
            TestThreadLocalAccessor fooThreadLocalAccessor = new TestThreadLocalAccessor("foo", fooThreadLocal);
            TestThreadLocalAccessor barThreadLocalAccessor = new TestThreadLocalAccessor("bar", barThreadLocal);
            registry.registerContextAccessor(new TestContextAccessor())
                .registerThreadLocalAccessor(fooThreadLocalAccessor)
                .registerThreadLocalAccessor(barThreadLocalAccessor);

            fooThreadLocal.set("foo_before");
            barThreadLocal.set("bar_before");

            ContextSnapshot snapshot = snapshotFactory.captureFrom(Collections.singletonMap("foo", "foo_after"));
            try (Scope scope = snapshot.setThreadLocals(key -> key.equals("foo"))) {
                assertThat(fooThreadLocal.get()).isEqualTo("foo_after");
                assertThat(barThreadLocal.get()).isEqualTo("bar_before");
            }
            assertThat(fooThreadLocal.get()).isEqualTo("foo_before");
            assertThat(barThreadLocal.get()).isEqualTo("bar_before");
        }

        @Test
        void should_not_touch_other_thread_locals_when_filter_capture() {

            ContextSnapshotFactory snapshotFactory = ContextSnapshotFactory.builder()
                .contextRegistry(registry)
                .clearMissing(true)
                .captureKeyPredicate(key -> key.equals("foo"))
                .build();

            ThreadLocal<String> fooThreadLocal = new ThreadLocal<>();
            ThreadLocal<String> barThreadLocal = new ThreadLocal<>();
            TestThreadLocalAccessor fooThreadLocalAccessor = new TestThreadLocalAccessor("foo", fooThreadLocal);
            TestThreadLocalAccessor barThreadLocalAccessor = new TestThreadLocalAccessor("bar", barThreadLocal);
            registry.registerThreadLocalAccessor(fooThreadLocalAccessor)
                .registerThreadLocalAccessor(barThreadLocalAccessor);

            fooThreadLocal.set("present");
            barThreadLocal.set("present");

            ContextSnapshot snapshot = snapshotFactory.captureAll();

            fooThreadLocal.remove();
            barThreadLocal.remove();

            try (Scope scope = snapshot.setThreadLocals()) {
                assertThat(fooThreadLocal.get()).isEqualTo("present");
                assertThat(barThreadLocal.get()).isNull();
            }
            assertThat(fooThreadLocal.get()).isNull();
            assertThat(barThreadLocal.get()).isNull();
        }

    }

}
