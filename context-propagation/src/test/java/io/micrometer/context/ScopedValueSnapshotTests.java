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

import io.micrometer.context.test.ScopedValue;
import io.micrometer.context.test.ScopedValueThreadLocalAccessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ContextSnapshotFactory} when used in scoped scenarios.
 *
 * @author Dariusz Jędrzejczyk
 */
public class ScopedValueSnapshotTests {

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
        ScopedValue.VALUE_IN_SCOPE.remove();
        registry.removeThreadLocalAccessor(ScopedValueThreadLocalAccessor.KEY);
    }

    @Test
    void scopeWorksInAnotherThreadWhenWrapping() throws Exception {
        AtomicReference<ScopedValue> valueInNewThread = new AtomicReference<>();
        ScopedValue scopedValue = ScopedValue.create("hello");

        assertThat(ScopedValue.getCurrent()).isNull();

        try (ScopedValue.Scope scope = scopedValue.open()) {
            assertThat(ScopedValue.getCurrent()).isEqualTo(scopedValue);
            Runnable wrapped = snapshotFactory.captureAll().wrap(() -> valueInNewThread.set(ScopedValue.getCurrent()));
            Thread t = new Thread(wrapped);
            t.start();
            t.join();
        }

        assertThat(valueInNewThread.get()).isEqualTo(scopedValue);
        assertThat(ScopedValue.getCurrent()).isNull();
    }

    @Test
    void nestedScopeWorksInAnotherThreadWhenWrapping() throws Exception {
        AtomicReference<ScopedValue> value1InNewThreadBefore = new AtomicReference<>();
        AtomicReference<ScopedValue> value1InNewThreadAfter = new AtomicReference<>();
        AtomicReference<ScopedValue> value2InNewThread = new AtomicReference<>();

        ScopedValue v1 = ScopedValue.create("val1");
        ScopedValue v2 = ScopedValue.create("val2");

        assertThat(ScopedValue.getCurrent()).isNull();

        Thread t;

        try (ScopedValue.Scope v1Scope = v1.open()) {
            assertThat(ScopedValue.getCurrent()).isEqualTo(v1);
            try (ScopedValue.Scope v2scope1T1 = v2.open()) {
                assertThat(ScopedValue.getCurrent()).isEqualTo(v2);
                try (ScopedValue.Scope v2scope2T1 = v2.open()) {
                    assertThat(ScopedValue.getCurrent()).isEqualTo(v2);
                    Runnable runnable = () -> {
                        value1InNewThreadBefore.set(ScopedValue.getCurrent());
                        try (ScopedValue.Scope v2scopeT2 = v2.open()) {
                            value2InNewThread.set(ScopedValue.getCurrent());
                        }
                        value1InNewThreadAfter.set(ScopedValue.getCurrent());
                    };

                    Runnable wrapped = snapshotFactory.captureAll().wrap(runnable);
                    t = new Thread(wrapped);
                    t.start();

                    assertThat(ScopedValue.getCurrent()).isEqualTo(v2);
                    assertThat(ScopedValue.getCurrent().currentScope()).isEqualTo(v2scope2T1);
                }
                assertThat(ScopedValue.getCurrent()).isEqualTo(v2);
                assertThat(ScopedValue.getCurrent().currentScope()).isEqualTo(v2scope1T1);
            }

            assertThat(ScopedValue.getCurrent()).isEqualTo(v1);

            try (ScopedValue.Scope childScope3 = v2.open()) {
                assertThat(ScopedValue.getCurrent()).isEqualTo(v2);
                assertThat(ScopedValue.getCurrent().currentScope()).isEqualTo(childScope3);
            }

            t.join();
            assertThat(ScopedValue.getCurrent()).isEqualTo(v1);
        }

        assertThat(value1InNewThreadBefore.get()).isEqualTo(v2);
        assertThat(value1InNewThreadAfter.get()).isEqualTo(v2);
        assertThat(value2InNewThread.get()).isEqualTo(v2);
        assertThat(ScopedValue.getCurrent()).isNull();
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

        assertThat(ScopedValue.getCurrent()).isNull();

        Map<String, ScopedValue> sourceContext = Collections.singletonMap(ScopedValueThreadLocalAccessor.KEY, value);

        try (ContextSnapshot.Scope outer = snapshotFactory.setThreadLocalsFrom(sourceContext)) {
            assertThat(ScopedValue.getCurrent()).isEqualTo(value);
            try (ContextSnapshot.Scope inner = snapshotFactory.setThreadLocalsFrom(Collections.emptyMap())) {
                assertThat(ScopedValue.getCurrent().get()).isNull();
            }
            assertThat(ScopedValue.getCurrent()).isEqualTo(value);
        }
        assertThat(ScopedValue.getCurrent()).isNull();

        registry.removeContextAccessor(accessor);
    }

}
