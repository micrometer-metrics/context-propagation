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
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import io.micrometer.scopedvalue.Scope;
import io.micrometer.scopedvalue.ScopedValue;
import io.micrometer.scopedvalue.ScopeHolder;
import io.micrometer.scopedvalue.ScopedValueThreadLocalAccessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ContextSnapshotFactory} when used in scoped scenarios.
 *
 * @author Dariusz Jędrzejczyk
 */
class ScopedValueSnapshotTests {

    private final ContextRegistry registry = new ContextRegistry();

    private final ContextSnapshotFactory snapshotFactory = ContextSnapshotFactory.builder()
        .contextRegistry(registry)
        .build();

    @BeforeEach
    void initializeThreadLocalAccessors() {
        registry.registerThreadLocalAccessor(new ScopedValueThreadLocalAccessor());
    }

    @AfterEach
    void cleanupThreadLocals() {
        ScopeHolder.remove();
        registry.removeThreadLocalAccessor(ScopedValueThreadLocalAccessor.KEY);
    }

    @Test
    void scopeWorksInAnotherThreadWhenWrapping() throws Exception {
        AtomicReference<ScopedValue> valueInNewThread = new AtomicReference<>();
        ScopedValue scopedValue = ScopedValue.create("hello");

        assertThat(ScopeHolder.currentValue()).isNull();

        try (Scope scope = Scope.open(scopedValue)) {
            assertThat(ScopeHolder.currentValue()).isEqualTo(scopedValue);
            Runnable wrapped = snapshotFactory.captureAll()
                .wrap(() -> valueInNewThread.set(ScopeHolder.currentValue()));
            Thread t = new Thread(wrapped);
            t.start();
            t.join();
        }

        assertThat(valueInNewThread.get()).isEqualTo(scopedValue);
        assertThat(ScopeHolder.currentValue()).isNull();
    }

    @Test
    void nestedScopeWorksInAnotherThreadWhenWrapping() throws Exception {
        AtomicReference<ScopedValue> value1InNewThreadBefore = new AtomicReference<>();
        AtomicReference<ScopedValue> value1InNewThreadAfter = new AtomicReference<>();
        AtomicReference<ScopedValue> value2InNewThread = new AtomicReference<>();

        ScopedValue v1 = ScopedValue.create("val1");
        ScopedValue v2 = ScopedValue.create("val2");

        assertThat(ScopeHolder.currentValue()).isNull();

        Thread t;

        try (Scope v1Scope = Scope.open(v1)) {
            assertThat(ScopeHolder.currentValue()).isEqualTo(v1);
            try (Scope v2scope1T1 = Scope.open(v2)) {
                assertThat(ScopeHolder.currentValue()).isEqualTo(v2);
                try (Scope v2scope2T1 = Scope.open(v2)) {
                    assertThat(ScopeHolder.currentValue()).isEqualTo(v2);
                    Runnable runnable = () -> {
                        value1InNewThreadBefore.set(ScopeHolder.currentValue());
                        try (Scope v2scopeT2 = Scope.open(v2)) {
                            value2InNewThread.set(ScopeHolder.currentValue());
                        }
                        value1InNewThreadAfter.set(ScopeHolder.currentValue());
                    };

                    Runnable wrapped = snapshotFactory.captureAll().wrap(runnable);
                    t = new Thread(wrapped);
                    t.start();

                    assertThat(ScopeHolder.currentValue()).isEqualTo(v2);
                    assertThat(ScopeHolder.get()).isEqualTo(v2scope2T1);
                }
                assertThat(ScopeHolder.currentValue()).isEqualTo(v2);
                assertThat(ScopeHolder.get()).isEqualTo(v2scope1T1);
            }

            assertThat(ScopeHolder.currentValue()).isEqualTo(v1);

            try (Scope childScope3 = Scope.open(v2)) {
                assertThat(ScopeHolder.currentValue()).isEqualTo(v2);
                assertThat(ScopeHolder.get()).isEqualTo(childScope3);
            }

            t.join();
            assertThat(ScopeHolder.currentValue()).isEqualTo(v1);
        }

        assertThat(value1InNewThreadBefore.get()).isEqualTo(v2);
        assertThat(value1InNewThreadAfter.get()).isEqualTo(v2);
        assertThat(value2InNewThread.get()).isEqualTo(v2);
        assertThat(ScopeHolder.currentValue()).isNull();
    }

    @Test
    void shouldProperlyClearInNestedScope() {
        TestContextAccessor accessor = new TestContextAccessor();
        ContextSnapshotFactory snapshotFactory = ContextSnapshotFactory.builder()
            .contextRegistry(registry)
            .clearMissing(true)
            .build();
        registry.registerContextAccessor(accessor);
        ScopedValue value = ScopedValue.create("value");

        assertThat(ScopeHolder.currentValue()).isNull();

        Map<String, ScopedValue> sourceContext = Collections.singletonMap(ScopedValueThreadLocalAccessor.KEY, value);

        try (ContextSnapshot.Scope outer = snapshotFactory.setThreadLocalsFrom(sourceContext)) {
            assertThat(ScopeHolder.currentValue()).isEqualTo(value);
            try (ContextSnapshot.Scope inner = snapshotFactory.setThreadLocalsFrom(Collections.emptyMap())) {
                assertThat(ScopeHolder.currentValue().get()).isNull();
            }
            assertThat(ScopeHolder.currentValue()).isEqualTo(value);
        }
        assertThat(ScopeHolder.currentValue()).isNull();

        registry.removeContextAccessor(accessor);
    }

    @Test
    void duplicateThreadLocalAccessorsForSameThreadLocalHaveReverseOrderUponClose() {
        registry.registerThreadLocalAccessor(new ScopedValueThreadLocalAccessor("other"));

        ScopedValue value = ScopedValue.create("value");

        ContextSnapshot snapshot;
        try (Scope scope = Scope.open(value)) {
            snapshot = snapshotFactory.captureAll();
        }

        try (ContextSnapshot.Scope scope = snapshot.setThreadLocals()) {
            assertThat(ScopeHolder.currentValue()).isEqualTo(value);
        }

        assertThat(ScopeHolder.currentValue()).isNull();

        registry.removeThreadLocalAccessor("other");
    }

}
