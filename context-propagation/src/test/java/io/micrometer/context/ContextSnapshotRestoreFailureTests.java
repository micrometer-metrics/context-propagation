/*
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

import io.micrometer.context.ContextSnapshot.Scope;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class ContextSnapshotRestoreFailureTests {

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void restores_other_thread_locals_when_one_restore_fails(boolean previousValuePresent) {
        Accessor first = new Accessor("first");
        Accessor second = new Accessor("second");
        Scope scope = scope(previousValuePresent, first, second);
        RuntimeException failure = new IllegalStateException("restore failed");
        second.failure = failure;
        try {
            Throwable thrown = catchThrowable(scope::close);
            assertThat(thrown).isSameAs(failure);
            assertRestored(previousValuePresent, first, second);
        }
        finally {
            clear(first, second);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void preserves_distinct_restore_failures_in_reverse_order(boolean previousValuePresent) {
        Accessor first = new Accessor("first");
        Accessor second = new Accessor("second");
        Accessor third = new Accessor("third");
        Scope scope = scope(previousValuePresent, first, second, third);
        RuntimeException firstFailure = new IllegalStateException("third restore failed");
        RuntimeException laterFailure = new IllegalArgumentException("second restore failed");
        third.failure = firstFailure;
        second.failure = laterFailure;
        try {
            Throwable thrown = catchThrowable(scope::close);
            assertThat(thrown).isSameAs(firstFailure);
            assertThat(thrown.getSuppressed()).containsExactly(laterFailure);
            assertRestored(previousValuePresent, first, second, third);
        }
        finally {
            clear(first, second, third);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void repeated_exception_does_not_interrupt_remaining_restores(boolean previousValuePresent) {
        Accessor first = new Accessor("first");
        Accessor second = new Accessor("second");
        Accessor third = new Accessor("third");
        Scope scope = scope(previousValuePresent, first, second, third);
        RuntimeException failure = new IllegalStateException("shared restore failure");
        second.failure = failure;
        third.failure = failure;
        try {
            Throwable thrown = catchThrowable(scope::close);
            assertThat(thrown).isSameAs(failure);
            assertThat(thrown.getSuppressed()).isEmpty();
            assertRestored(previousValuePresent, first, second, third);
        }
        finally {
            clear(first, second, third);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void wrapped_task_keeps_its_exception_and_restores_remaining_context(boolean previousValuePresent) {
        Accessor first = new Accessor("first");
        Accessor second = new Accessor("second");
        ContextSnapshot snapshot = snapshot(first, second);
        setPrevious(previousValuePresent, first, second);
        RuntimeException taskFailure = new IllegalArgumentException("task failed");
        RuntimeException restoreFailure = new IllegalStateException("restore failed");
        second.failure = restoreFailure;
        Runnable task = () -> {
            throw taskFailure;
        };
        Runnable wrapped = snapshot.wrap(task);
        try {
            Throwable thrown = catchThrowable(wrapped::run);
            assertThat(thrown).isSameAs(taskFailure);
            assertThat(thrown.getSuppressed()).containsExactly(restoreFailure);
            assertRestored(previousValuePresent, first, second);
        }
        finally {
            clear(first, second);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void failed_inner_close_restores_outer_context(boolean previousValuePresent) {
        Accessor first = new Accessor("first");
        Accessor second = new Accessor("second");
        ContextSnapshot outerSnapshot = snapshot(first, second);
        setPrevious(previousValuePresent, first, second);
        try (Scope outerScope = outerSnapshot.setThreadLocals()) {
            first.setValue("inner");
            second.setValue("inner");
            ContextSnapshot innerSnapshot = ContextSnapshotFactory.builder()
                .contextRegistry(
                        new ContextRegistry().registerThreadLocalAccessor(first).registerThreadLocalAccessor(second))
                .build()
                .captureAll();
            first.setValue("task");
            second.setValue("task");
            Scope innerScope = innerSnapshot.setThreadLocals();
            RuntimeException failure = new IllegalStateException("inner restore failed");
            second.failure = failure;
            try {
                assertThat(catchThrowable(innerScope::close)).isSameAs(failure);
                assertThat(first.getValue()).isEqualTo("task");
                assertThat(second.getValue()).isEqualTo("task");
            }
            finally {
                second.failure = null;
            }
        }
        finally {
            // Both outer values should have been restored even after inner close failed.
            try {
                assertRestored(previousValuePresent, first, second);
            }
            finally {
                clear(first, second);
            }
        }
    }

    private static Scope scope(boolean previousValuePresent, Accessor... accessors) {
        ContextSnapshot snapshot = snapshot(accessors);
        setPrevious(previousValuePresent, accessors);
        return snapshot.setThreadLocals();
    }

    private static ContextSnapshot snapshot(Accessor... accessors) {
        ContextRegistry registry = new ContextRegistry();
        for (Accessor accessor : accessors) {
            registry.registerThreadLocalAccessor(accessor);
            accessor.setValue("task");
        }
        return ContextSnapshotFactory.builder().contextRegistry(registry).build().captureAll();
    }

    private static void setPrevious(boolean present, Accessor... accessors) {
        for (Accessor accessor : accessors) {
            if (present) {
                accessor.setValue("previous");
            }
            else {
                accessor.setValue();
            }
        }
    }

    private static void assertRestored(boolean present, Accessor... accessors) {
        for (Accessor accessor : accessors) {
            assertThat(accessor.getValue()).isEqualTo(present ? "previous" : null);
        }
    }

    private static void clear(Accessor... accessors) {
        for (Accessor accessor : accessors) {
            accessor.setValue();
        }
    }

    private static final class Accessor implements ThreadLocalAccessor<String> {

        private final String key;

        private final ThreadLocal<String> value = new ThreadLocal<>();

        @org.jspecify.annotations.Nullable
        private RuntimeException failure;

        private Accessor(String key) {
            this.key = key;
        }

        @Override
        public Object key() {
            return this.key;
        }

        @Override
        public @org.jspecify.annotations.Nullable String getValue() {
            return this.value.get();
        }

        @Override
        public void setValue(String value) {
            this.value.set(value);
        }

        @Override
        public void setValue() {
            this.value.remove();
        }

        @Override
        public void restore(String previousValue) {
            setValue(previousValue);
            failIfConfigured();
        }

        @Override
        public void restore() {
            setValue();
            failIfConfigured();
        }

        private void failIfConfigured() {
            if (this.failure != null) {
                throw this.failure;
            }
        }

    }

}
