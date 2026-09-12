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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ContextThreadFactory}.
 */
class ContextThreadFactoryTests {

    private final ContextRegistry registry = new ContextRegistry()
        .registerThreadLocalAccessor(new StringThreadLocalAccessor());

    private final ContextSnapshotFactory snapshotFactory = ContextSnapshotFactory.builder()
        .contextRegistry(registry)
        .clearMissing(false)
        .build();

    @AfterEach
    void clear() {
        StringThreadLocalHolder.reset();
    }

    @Test
    void should_propagate_context_to_new_thread_using_snapshot_factory() throws InterruptedException {
        StringThreadLocalHolder.setValue("hello");
        AtomicReference<@Nullable String> valueInNewThread = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        ThreadFactory factory = ContextThreadFactory.wrap(Executors.defaultThreadFactory(), snapshotFactory);
        Thread thread = factory.newThread(() -> {
            valueInNewThread.set(StringThreadLocalHolder.getValue());
            latch.countDown();
        });
        thread.start();
        awaitAndJoin(thread, latch);

        assertThat(valueInNewThread.get()).isEqualTo("hello");
    }

    @Test
    void should_propagate_context_to_new_thread_using_snapshot_supplier() throws InterruptedException {
        StringThreadLocalHolder.setValue("world");
        AtomicReference<@Nullable String> valueInNewThread = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        ThreadFactory factory = ContextThreadFactory.wrap(Executors.defaultThreadFactory(),
                snapshotFactory::captureAll);
        Thread thread = factory.newThread(() -> {
            valueInNewThread.set(StringThreadLocalHolder.getValue());
            latch.countDown();
        });
        thread.start();
        awaitAndJoin(thread, latch);

        assertThat(valueInNewThread.get()).isEqualTo("world");
    }

    @Test
    void should_capture_context_at_thread_creation_time_not_at_execution_time() throws InterruptedException {
        StringThreadLocalHolder.setValue("captured-value");
        AtomicReference<@Nullable String> valueInNewThread = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        ThreadFactory factory = ContextThreadFactory.wrap(Executors.defaultThreadFactory(), snapshotFactory);
        // Create the thread while context is set
        Thread thread = factory.newThread(() -> {
            valueInNewThread.set(StringThreadLocalHolder.getValue());
            latch.countDown();
        });

        // Change context after thread creation
        StringThreadLocalHolder.setValue("changed-value");

        thread.start();
        awaitAndJoin(thread, latch);

        assertThat(valueInNewThread.get()).as("Context should be captured at thread creation time, not execution time")
            .isEqualTo("captured-value");
    }

    @Test
    void should_delegate_thread_creation_to_wrapped_factory() {
        AtomicReference<@Nullable Thread> createdThread = new AtomicReference<>();
        ThreadFactory delegateFactory = r -> {
            Thread t = new Thread(r, "custom-thread-name");
            createdThread.set(t);
            return t;
        };

        ThreadFactory factory = ContextThreadFactory.wrap(delegateFactory, snapshotFactory);
        Thread thread = factory.newThread(() -> {
        });

        assertThat(thread).isSameAs(createdThread.get());
        assertThat(thread.getName()).isEqualTo("custom-thread-name");
    }

    @Test
    void should_use_thread_created_by_delegate_factory() throws Exception {
        StringThreadLocalHolder.setValue("delegate-value");
        AtomicReference<@Nullable String> valueInNewThread = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        ThreadFactory delegateFactory = r -> new Thread(r, "delegate-thread");
        ThreadFactory factory = ContextThreadFactory.wrap(delegateFactory, snapshotFactory);
        Thread thread = factory.newThread(() -> {
            valueInNewThread.set(StringThreadLocalHolder.getValue());
            latch.countDown();
        });
        thread.start();
        awaitAndJoin(thread, latch);

        assertThat(thread.getName()).isEqualTo("delegate-thread");
        assertThat(valueInNewThread.get()).isEqualTo("delegate-value");
    }

    @Test
    @SuppressWarnings("NullAway")
    void should_throw_on_null_thread_factory() {
        assertThatThrownBy(() -> ContextThreadFactory.wrap(null, snapshotFactory))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("threadFactory must not be null");
    }

    @Test
    @SuppressWarnings("NullAway")
    void should_throw_on_null_snapshot_factory() {
        assertThatThrownBy(
                () -> ContextThreadFactory.wrap(Executors.defaultThreadFactory(), (ContextSnapshotFactory) null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    @SuppressWarnings("NullAway")
    void should_throw_on_null_snapshot_supplier() {
        assertThatThrownBy(
                () -> ContextThreadFactory.wrap(Executors.defaultThreadFactory(), (Supplier<ContextSnapshot>) null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("contextSnapshotSupplier must not be null");
    }

    private void awaitAndJoin(Thread thread, CountDownLatch latch) throws InterruptedException {
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        thread.join(TimeUnit.SECONDS.toMillis(5));
    }

}
