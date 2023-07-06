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
import io.micrometer.scopedvalue.ScopeHolder;
import io.micrometer.scopedvalue.ScopedValue;
import io.micrometer.scopedvalue.ScopedValueThreadLocalAccessor;
import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.BDDAssertions.then;

/**
 * Unit tests for {@link DefaultContextSnapshot}.
 *
 * @author Rossen Stoyanchev
 */
@SuppressWarnings("deprecation")
public class DefaultContextSnapshotDepreactionTests {

    private final ContextRegistry registry = new ContextRegistry();

    @Test
    void should_propagate_thread_local() {
        this.registry.registerThreadLocalAccessor(new StringThreadLocalAccessor());

        StringThreadLocalHolder.setValue("hello");
        ContextSnapshot snapshot = ContextSnapshot.captureAllUsing(key -> true, this.registry);

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

    @Test
    void should_propagate_single_thread_local_value() {
        this.registry.registerContextAccessor(new TestContextAccessor());
        this.registry.registerThreadLocalAccessor(new StringThreadLocalAccessor());

        String key = StringThreadLocalAccessor.KEY;
        Map<String, String> sourceContext = Collections.singletonMap(key, "hello");

        StringThreadLocalHolder.setValue("hola");
        try {
            try (Scope scope = ContextSnapshot.setThreadLocalsFrom(sourceContext, this.registry, key)) {
                then(StringThreadLocalHolder.getValue()).isEqualTo("hello");
            }
            then(StringThreadLocalHolder.getValue()).isEqualTo("hola");
        }
        finally {
            StringThreadLocalHolder.reset();
        }
    }

    @Test
    void should_propagate_all_single_thread_local_value() {
        this.registry.registerContextAccessor(new TestContextAccessor());
        this.registry.registerThreadLocalAccessor(new StringThreadLocalAccessor());

        String key = StringThreadLocalAccessor.KEY;
        Map<String, String> sourceContext = Collections.singletonMap(key, "hello");

        StringThreadLocalHolder.setValue("hola");
        try {
            try (Scope scope = ContextSnapshot.setAllThreadLocalsFrom(sourceContext, this.registry)) {
                then(StringThreadLocalHolder.getValue()).isEqualTo("hello");
            }
            then(StringThreadLocalHolder.getValue()).isEqualTo("hola");
        }
        finally {
            StringThreadLocalHolder.reset();
        }
    }

    @Test
    void should_throw_an_exception_when_no_keys_are_passed() {
        this.registry.registerContextAccessor(new TestContextAccessor());
        this.registry.registerThreadLocalAccessor(new StringThreadLocalAccessor());

        Map<String, String> sourceContext = Collections.singletonMap("foo", "hello");

        BDDAssertions.thenThrownBy(() -> ContextSnapshot.setThreadLocalsFrom(sourceContext, this.registry))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("You must provide at least one key when setting thread locals");
    }

    @Test
    void should_throw_an_exception_when_no_keys_are_passed_for_version_with_no_registry() {
        this.registry.registerContextAccessor(new TestContextAccessor());
        this.registry.registerThreadLocalAccessor(new StringThreadLocalAccessor());

        Map<String, String> sourceContext = Collections.singletonMap("foo", "hello");

        BDDAssertions.thenThrownBy(() -> ContextSnapshot.setThreadLocalsFrom(sourceContext))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("You must provide at least one key when setting thread locals");
    }

    @Test
    void should_filter_thread_locals_on_capture() {
        ThreadLocal<String> fooThreadLocal = new ThreadLocal<>();
        ThreadLocal<String> barThreadLocal = new ThreadLocal<>();

        this.registry.registerThreadLocalAccessor(new TestThreadLocalAccessor("foo", fooThreadLocal))
            .registerThreadLocalAccessor(new TestThreadLocalAccessor("bar", barThreadLocal));

        fooThreadLocal.set("fooValue");
        barThreadLocal.set("barValue");

        ContextSnapshot snapshot = ContextSnapshot.captureAllUsing(key -> key.equals("foo"), this.registry);

        fooThreadLocal.remove();
        barThreadLocal.remove();

        try (Scope scope = snapshot.setThreadLocals()) {
            then(fooThreadLocal.get()).isEqualTo("fooValue");
            then(barThreadLocal.get()).isNull();
        }

        then(fooThreadLocal.get()).isNull();
        then(barThreadLocal.get()).isNull();
    }

    @Test
    void should_filter_thread_locals_on_restore() {
        ThreadLocal<String> fooThreadLocal = new ThreadLocal<>();
        ThreadLocal<String> barThreadLocal = new ThreadLocal<>();

        this.registry.registerThreadLocalAccessor(new TestThreadLocalAccessor("foo", fooThreadLocal))
            .registerThreadLocalAccessor(new TestThreadLocalAccessor("bar", barThreadLocal));

        fooThreadLocal.set("fooValue");
        barThreadLocal.set("barValue");

        ContextSnapshot snapshot = ContextSnapshot.captureAllUsing(key -> true, this.registry);

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

    @Test
    void should_not_fail_on_empty_thread_local() {
        this.registry.registerThreadLocalAccessor(new StringThreadLocalAccessor());

        then(StringThreadLocalHolder.getValue()).isNull();

        ContextSnapshot snapshot = ContextSnapshot.captureAll(this.registry);

        StringThreadLocalHolder.reset();
        then(StringThreadLocalHolder.getValue()).isNull();

        try (Scope scope = snapshot.setThreadLocals()) {
            then(StringThreadLocalHolder.getValue()).isNull();
        }

        then(StringThreadLocalHolder.getValue()).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_ignore_null_value_in_source_context() {
        String key = "foo";
        ThreadLocal<String> fooThreadLocal = new ThreadLocal<>();
        TestThreadLocalAccessor fooThreadLocalAccessor = new TestThreadLocalAccessor(key, fooThreadLocal);

        this.registry.registerContextAccessor(new TestContextAccessor());
        this.registry.registerThreadLocalAccessor(fooThreadLocalAccessor);

        // We capture null from an uninitialized ThreadLocal:
        String emptyValue = fooThreadLocalAccessor.getValue();
        Map<String, String> sourceContext = Collections.singletonMap(key, emptyValue);

        ContextSnapshot snapshot = ContextSnapshot.captureFrom(sourceContext, this.registry);

        HashMap<Object, Object> snapshotStorage = (HashMap<Object, Object>) snapshot;
        assertThat(snapshotStorage).isEmpty();

        try (Scope scope = snapshot.setThreadLocals()) {
            assertThat(fooThreadLocalAccessor.getValue()).isEqualTo(emptyValue);
        }
        assertThat(fooThreadLocalAccessor.getValue()).isEqualTo(emptyValue);
    }

    @Test
    void should_ignore_null_mapping_in_source_context_when_skipping_intermediate_snapshot() {
        String key = "foo";
        ThreadLocal<String> fooThreadLocal = new ThreadLocal<>();
        TestThreadLocalAccessor fooThreadLocalAccessor = new TestThreadLocalAccessor(key, fooThreadLocal);

        this.registry.registerContextAccessor(new TestContextAccessor());
        this.registry.registerThreadLocalAccessor(fooThreadLocalAccessor);

        // We capture null from an uninitialized ThreadLocal:
        String emptyValue = fooThreadLocalAccessor.getValue();
        Map<String, String> sourceContext = Collections.singletonMap(key, emptyValue);

        // Validate setting all values
        try (Scope scope = ContextSnapshot.setAllThreadLocalsFrom(sourceContext, this.registry)) {
            assertThat(fooThreadLocalAccessor.getValue()).isEqualTo(emptyValue);
        }
        assertThat(fooThreadLocalAccessor.getValue()).isEqualTo(emptyValue);

        // Validate setting a subset of values
        try (Scope scope = ContextSnapshot.setThreadLocalsFrom(sourceContext, this.registry, key)) {
            assertThat(fooThreadLocalAccessor.getValue()).isEqualTo(emptyValue);
        }
        assertThat(fooThreadLocalAccessor.getValue()).isEqualTo(emptyValue);
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_fail_assertion_if_null_value_makes_it_into_snapshot() {
        ThreadLocal<String> fooThreadLocal = new ThreadLocal<>();
        TestThreadLocalAccessor fooThreadLocalAccessor = new TestThreadLocalAccessor("foo", fooThreadLocal);
        this.registry.registerThreadLocalAccessor(fooThreadLocalAccessor);

        fooThreadLocal.set("present");

        ContextSnapshot snapshot = ContextSnapshot.captureAll(this.registry);
        fooThreadLocal.remove();

        HashMap<Object, Object> snapshotStorage = (HashMap<Object, Object>) snapshot;
        // Imitating a broken implementation that let mapping to null into the storage:
        snapshotStorage.put("foo", null);

        assertThatExceptionOfType(AssertionError.class).isThrownBy(snapshot::setThreadLocals)
            .withMessage("snapshot contains disallowed null mapping for key: foo");
    }

    @Test
    void toString_should_include_values() {
        ThreadLocal<String> fooThreadLocal = new ThreadLocal<>();
        ThreadLocal<String> barThreadLocal = new ThreadLocal<>();

        this.registry.registerThreadLocalAccessor(new TestThreadLocalAccessor("foo", fooThreadLocal))
            .registerThreadLocalAccessor(new TestThreadLocalAccessor("bar", barThreadLocal));

        fooThreadLocal.set("fooValue");
        barThreadLocal.set("barValue");

        assertThat(ContextSnapshot.captureAllUsing(key -> true, this.registry).toString())
            .isEqualTo("DefaultContextSnapshot{bar=barValue, foo=fooValue}");

        fooThreadLocal.remove();
        barThreadLocal.remove();
    }

    @Test
    void should_work_with_scope_based_thread_local_accessor() {
        TestContextAccessor accessor = new TestContextAccessor();
        this.registry.registerContextAccessor(accessor);
        this.registry.registerThreadLocalAccessor(new ScopedValueThreadLocalAccessor());

        ScopedValue value = ScopedValue.create("value");

        assertThat(ScopeHolder.currentValue()).isNull();

        Map<String, ScopedValue> sourceContext = Collections.singletonMap(ScopedValueThreadLocalAccessor.KEY, value);

        try (ContextSnapshot.Scope outer = ContextSnapshot.setAllThreadLocalsFrom(sourceContext, this.registry)) {
            assertThat(ScopeHolder.currentValue()).isEqualTo(value);
            try (ContextSnapshot.Scope inner = ContextSnapshot.setAllThreadLocalsFrom(Collections.emptyMap(),
                    this.registry)) {
                assertThat(ScopeHolder.currentValue()).isEqualTo(value);
                // The new API allows the following to happen when clearing behavior is
                // specified on ContextSnapshotFactory
                // assertThat(ScopeHolder.currentValue().get()).isNull();
            }
            assertThat(ScopeHolder.currentValue()).isEqualTo(value);
        }
        assertThat(ScopeHolder.currentValue()).isNull();

        this.registry.removeContextAccessor(accessor);
        this.registry.removeThreadLocalAccessor(ScopedValueThreadLocalAccessor.KEY);
    }

}
