/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micrometer.context;


import java.util.Collections;
import java.util.Map;

import io.micrometer.context.ContextSnapshot.Scope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.then;

/**
 * Unit tests for {@link DefaultContextSnapshot}.
 *
 * @author Rossen Stoyanchev
 */
public class DefaultContextSnapshotTests {

    private final ContextRegistry registry = new ContextRegistry();


    @Test
    void should_propagate_thread_local() {
        this.registry.registerThreadLocalAccessor(new ObservationThreadLocalAccessor());

        ObservationThreadLocalHolder.setValue("hello");
        ContextSnapshot snapshot = ContextSnapshot.captureUsing(this.registry, key -> true);

        ObservationThreadLocalHolder.setValue("hola");
        try {
            try (Scope scope = snapshot.setThreadLocalValues()) {
                then(ObservationThreadLocalHolder.getValue()).isEqualTo("hello");
            }
            then(ObservationThreadLocalHolder.getValue()).isEqualTo("hola");
        }
        finally {
            ObservationThreadLocalHolder.reset();
        }
    }

    @Test
    void should_propagate_single_thread_local_value() {
        this.registry.registerContextAccessor(new TestContextAccessor());
        this.registry.registerThreadLocalAccessor(new ObservationThreadLocalAccessor());

        String key = ObservationThreadLocalAccessor.KEY;
        Map<String, String> sourceContext = Collections.singletonMap(key, "hello");

        ObservationThreadLocalHolder.setValue("hola");
        try {
            try (Scope scope = ContextSnapshot.setThreadLocalsFrom(sourceContext, this.registry, key)) {
                then(ObservationThreadLocalHolder.getValue()).isEqualTo("hello");
            }
            then(ObservationThreadLocalHolder.getValue()).isEqualTo("hola");
        }
        finally {
            ObservationThreadLocalHolder.reset();
        }
    }

    @Test
    void should_not_fail_on_empty_thread_local() {
        this.registry.registerThreadLocalAccessor(new ObservationThreadLocalAccessor());

        then(ObservationThreadLocalHolder.getValue()).isNull();

        ContextSnapshot snapshot = ContextSnapshot.captureUsing(this.registry, key -> true);

        ObservationThreadLocalHolder.reset();
        then(ObservationThreadLocalHolder.getValue()).isNull();

        try (Scope scope = snapshot.setThreadLocalValues()) {
            then(ObservationThreadLocalHolder.getValue()).isNull();
        }

        then(ObservationThreadLocalHolder.getValue()).isNull();
    }

    @Test
    void should_filter_thread_locals_on_capture() {
        ThreadLocal<String> fooThreadLocal = new ThreadLocal<>();
        ThreadLocal<String> barThreadLocal = new ThreadLocal<>();

        this.registry
                .registerThreadLocalAccessor(new TestThreadLocalAccessor("foo", fooThreadLocal))
                .registerThreadLocalAccessor(new TestThreadLocalAccessor("bar", barThreadLocal));

        fooThreadLocal.set("fooValue");
        barThreadLocal.set("barValue");

        ContextSnapshot snapshot = ContextSnapshot.captureUsing(this.registry, key -> key.equals("foo"));

        fooThreadLocal.remove();
        barThreadLocal.remove();

        try (Scope scope = snapshot.setThreadLocalValues()) {
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

        this.registry
                .registerThreadLocalAccessor(new TestThreadLocalAccessor("foo", fooThreadLocal))
                .registerThreadLocalAccessor(new TestThreadLocalAccessor("bar", barThreadLocal));

        fooThreadLocal.set("fooValue");
        barThreadLocal.set("barValue");

        ContextSnapshot snapshot = ContextSnapshot.captureUsing(this.registry, key -> true);

        fooThreadLocal.remove();
        barThreadLocal.remove();

        try (Scope scope = snapshot.setThreadLocalValues(key -> key.equals("foo"))) {
            then(fooThreadLocal.get()).isEqualTo("fooValue");
            then(barThreadLocal.get()).isNull();
        }

        try (Scope scope = snapshot.setThreadLocalValues(key -> key.equals("bar"))) {
            then(fooThreadLocal.get()).isNull();
            then(barThreadLocal.get()).isEqualTo("barValue");
        }

        then(fooThreadLocal.get()).isNull();
        then(barThreadLocal.get()).isNull();
    }

    @Test
    void toString_should_include_values() {
        ThreadLocal<String> fooThreadLocal = new ThreadLocal<>();
        ThreadLocal<String> barThreadLocal = new ThreadLocal<>();

        this.registry
                .registerThreadLocalAccessor(new TestThreadLocalAccessor("foo", fooThreadLocal))
                .registerThreadLocalAccessor(new TestThreadLocalAccessor("bar", barThreadLocal));

        fooThreadLocal.set("fooValue");
        barThreadLocal.set("barValue");

        assertThat(ContextSnapshot.captureUsing(this.registry, key -> true).toString())
                .isEqualTo("DefaultContextSnapshot{bar=barValue, foo=fooValue}");

        fooThreadLocal.remove();
        barThreadLocal.remove();
    }

}
