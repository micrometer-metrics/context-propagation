/**
 * Copyright 2024-2025 the original author or authors.
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
package io.micrometer.context.integration;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.micrometer.context.ContextRegistry;
import io.micrometer.context.ContextSnapshot;
import io.micrometer.context.ContextSnapshotFactory;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

class Slf4jThreadLocalAccessorTests {

    @Test
    void shouldCopyEntireMdcContentsToNewThread() throws InterruptedException {
        ContextRegistry registry = new ContextRegistry().registerThreadLocalAccessor(new Slf4jThreadLocalAccessor());

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> value1InOtherThread = new AtomicReference<>();
        AtomicReference<String> value2InOtherThread = new AtomicReference<>();

        MDC.put("key1", "value1");
        MDC.put("key2", "value2");

        ContextSnapshot snapshot = ContextSnapshotFactory.builder()
            .contextRegistry(registry)
            .clearMissing(true)
            .build()
            .captureAll();

        executorService.submit(() -> {
            try (ContextSnapshot.Scope scope = snapshot.setThreadLocals()) {
                value1InOtherThread.set(MDC.get("key1"));
                value2InOtherThread.set(MDC.get("key2"));
            }
            latch.countDown();
        });

        latch.await(100, TimeUnit.MILLISECONDS);

        assertThat(value1InOtherThread.get()).isEqualTo("value1");
        assertThat(value2InOtherThread.get()).isEqualTo("value2");

        executorService.shutdown();
    }

    @Test
    void shouldCopySelectedMdcContentsToNewThread() throws InterruptedException {
        ContextRegistry registry = new ContextRegistry()
            .registerThreadLocalAccessor(new Slf4jThreadLocalAccessor("key1", "key2"));

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> value1InOtherThread = new AtomicReference<>();
        AtomicReference<String> value2InOtherThread = new AtomicReference<>();
        AtomicReference<String> value3InOtherThread = new AtomicReference<>();

        MDC.put("key1", "value1");
        MDC.put("key2", "value2");
        MDC.put("key3", "value3");

        ContextSnapshot snapshot = ContextSnapshotFactory.builder()
            .contextRegistry(registry)
            .clearMissing(true)
            .build()
            .captureAll();

        executorService.submit(() -> {
            try (ContextSnapshot.Scope scope = snapshot.setThreadLocals()) {
                value1InOtherThread.set(MDC.get("key1"));
                value2InOtherThread.set(MDC.get("key2"));
                value3InOtherThread.set(MDC.get("key3"));
            }
            latch.countDown();
        });

        latch.await(100, TimeUnit.MILLISECONDS);

        assertThat(value1InOtherThread.get()).isEqualTo("value1");
        assertThat(value2InOtherThread.get()).isEqualTo("value2");
        assertThat(value3InOtherThread.get()).isNull();

        executorService.shutdown();
    }

    @Test
    void shouldDealWithEmptySelectedValues() throws InterruptedException {
        ContextRegistry registry = new ContextRegistry()
            .registerThreadLocalAccessor(new Slf4jThreadLocalAccessor("key1", "key2"));

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> value1InOtherThread = new AtomicReference<>();
        AtomicReference<String> value2InOtherThread = new AtomicReference<>();
        AtomicReference<String> value3InOtherThread = new AtomicReference<>();

        MDC.put("key1", "value1_1");
        MDC.put("key3", "value3_1");

        ContextSnapshot snapshot = ContextSnapshotFactory.builder()
            .contextRegistry(registry)
            .clearMissing(true)
            .build()
            .captureAll();

        executorService.submit(() -> {
            try (ContextSnapshot.Scope scope = snapshot.setThreadLocals()) {
                value1InOtherThread.set(MDC.get("key1"));
                value2InOtherThread.set(MDC.get("key2"));
                value3InOtherThread.set(MDC.get("key3"));
            }
            latch.countDown();
        });

        latch.await(100, TimeUnit.MILLISECONDS);

        assertThat(value1InOtherThread.get()).isEqualTo("value1_1");
        assertThat(value2InOtherThread.get()).isNull();
        assertThat(value3InOtherThread.get()).isNull();

        CountDownLatch latch2 = new CountDownLatch(1);

        MDC.remove("key1");
        MDC.put("key2", "value2_2");

        ContextSnapshot snapshot2 = ContextSnapshotFactory.builder()
            .contextRegistry(registry)
            .clearMissing(true)
            .build()
            .captureAll();
        executorService.submit(() -> {
            try (ContextSnapshot.Scope scope = snapshot2.setThreadLocals()) {
                value1InOtherThread.set(MDC.get("key1"));
                value2InOtherThread.set(MDC.get("key2"));
                value3InOtherThread.set(MDC.get("key3"));
            }
            latch2.countDown();
        });

        latch2.await(100, TimeUnit.MILLISECONDS);

        assertThat(value1InOtherThread.get()).isNull();
        assertThat(value2InOtherThread.get()).isEqualTo("value2_2");
        assertThat(value3InOtherThread.get()).isNull();
        executorService.shutdown();
    }

}
