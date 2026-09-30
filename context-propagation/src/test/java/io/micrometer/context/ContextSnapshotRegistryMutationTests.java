/**
 * Copyright 2026 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class ContextSnapshotRegistryMutationTests {

    @ParameterizedTest
    @EnumSource(ScopeSource.class)
    void should_not_apply_an_accessor_twice_when_registration_changes_during_setup(ScopeSource source) {
        ContextRegistry registry = new ContextRegistry().registerContextAccessor(new TestContextAccessor());
        ThreadLocal<String> firstValue = new ThreadLocal<>();
        firstValue.set("previous");
        AtomicBoolean registered = new AtomicBoolean();
        TestThreadLocalAccessor first = new TestThreadLocalAccessor("first", firstValue) {
            @Override
            public void setValue(String value) {
                super.setValue(value);
                if (registered.compareAndSet(false, true)) {
                    registry.registerThreadLocalAccessor(earlierAccessor());
                }
            }
        };
        registry.registerThreadLocalAccessor(first);
        Map<String, String> values = new HashMap<>();
        values.put("first", "captured");

        try (ContextSnapshot.Scope scope = source.open(registry, values)) {
            assertThat(firstValue.get()).isEqualTo("captured");
        }

        assertThat(firstValue.get()).isEqualTo("previous");
    }

    @ParameterizedTest
    @EnumSource(ScopeSource.class)
    void should_not_skip_restoration_when_registration_changes_during_close(ScopeSource source) {
        ContextRegistry registry = new ContextRegistry().registerContextAccessor(new TestContextAccessor());
        ThreadLocal<String> firstValue = new ThreadLocal<>();
        ThreadLocal<String> secondValue = new ThreadLocal<>();
        firstValue.set("previous");
        secondValue.set("previous");
        registry.registerThreadLocalAccessor(new TestThreadLocalAccessor("first", firstValue));
        registry.registerThreadLocalAccessor(new TestThreadLocalAccessor("second", secondValue) {
            @Override
            public void restore(String previousValue) {
                super.restore(previousValue);
                registry.registerThreadLocalAccessor(earlierAccessor());
            }
        });
        Map<String, String> values = new HashMap<>();
        values.put("first", "captured");
        values.put("second", "captured");

        try (ContextSnapshot.Scope scope = source.open(registry, values)) {
            assertThat(firstValue.get()).isEqualTo("captured");
            assertThat(secondValue.get()).isEqualTo("captured");
        }

        assertThat(firstValue.get()).isEqualTo("previous");
        assertThat(secondValue.get()).isEqualTo("previous");
    }

    @ParameterizedTest
    @EnumSource(ScopeSource.class)
    void should_restore_original_accessor_when_it_is_replaced_during_scope(ScopeSource source) {
        ContextRegistry registry = new ContextRegistry().registerContextAccessor(new TestContextAccessor());
        ThreadLocal<String> originalValue = new ThreadLocal<>();
        ThreadLocal<String> replacementValue = new ThreadLocal<>();
        originalValue.set("original");
        replacementValue.set("replacement");
        registry.registerThreadLocalAccessor(new TestThreadLocalAccessor("first", originalValue));
        Map<String, String> values = new HashMap<>();
        values.put("first", "captured");

        try (ContextSnapshot.Scope scope = source.open(registry, values)) {
            registry.removeThreadLocalAccessor("first");
            registry.registerThreadLocalAccessor(new TestThreadLocalAccessor("first", replacementValue));
        }

        assertThat(originalValue.get()).isEqualTo("original");
        assertThat(replacementValue.get()).isEqualTo("replacement");
    }

    private static ThreadLocalAccessor<String> earlierAccessor() {
        return new TestThreadLocalAccessor("added", new ThreadLocal<>()) {
            @Override
            public int getOrder() {
                return -1;
            }
        };
    }

    enum ScopeSource {

        SNAPSHOT {
            @Override
            ContextSnapshot.Scope open(ContextRegistry registry, Map<String, String> values) {
                DefaultContextSnapshot snapshot = new DefaultContextSnapshot(registry, false);
                snapshot.putAll(values);
                return snapshot.setThreadLocals();
            }
        },

        CONTEXT {
            @Override
            ContextSnapshot.Scope open(ContextRegistry registry, Map<String, String> values) {
                return ContextSnapshotFactory.builder().contextRegistry(registry).build().setThreadLocalsFrom(values);
            }
        },

        EXPLICIT_KEYS {
            @Override
            ContextSnapshot.Scope open(ContextRegistry registry, Map<String, String> values) {
                return ContextSnapshotFactory.builder()
                    .contextRegistry(registry)
                    .build()
                    .setThreadLocalsFrom(values, "first", "second");
            }
        };

        abstract ContextSnapshot.Scope open(ContextRegistry registry, Map<String, String> values);

    }

}
