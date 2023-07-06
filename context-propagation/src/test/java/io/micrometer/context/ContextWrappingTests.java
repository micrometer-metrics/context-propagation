/**
 * Copyright 2022 the original author or authors.
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
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Unit tests for the {@code "wrap"} methods in {@link ContextSnapshot}.
 */
class ContextWrappingTests {

    private final ContextRegistry registry = new ContextRegistry()
        .registerThreadLocalAccessor(new StringThreadLocalAccessor());

    private final ContextSnapshotFactory defaultSnapshotFactory = ContextSnapshotFactory.builder()
        .contextRegistry(registry)
        .clearMissing(false)
        .build();

    @AfterEach
    void clear() {
        StringThreadLocalHolder.reset();
    }

    @Test
    void should_instrument_runnable() throws InterruptedException, TimeoutException {
        StringThreadLocalHolder.setValue("hello");
        AtomicReference<String> valueInNewThread = new AtomicReference<>();
        Runnable runnable = runnable(valueInNewThread);
        runInNewThread(runnable);
        then(valueInNewThread.get()).as("By default thread local information should not be propagated").isNull();

        runInNewThread(defaultSnapshotFactory.captureAll().wrap(runnable));

        then(valueInNewThread.get()).as("With context container the thread local information should be propagated")
            .isEqualTo("hello");
    }

    @Test
    void should_instrument_callable() throws ExecutionException, InterruptedException, TimeoutException {
        StringThreadLocalHolder.setValue("hello");
        AtomicReference<String> valueInNewThread = new AtomicReference<>();
        Callable<String> callable = () -> {
            valueInNewThread.set(StringThreadLocalHolder.getValue());
            return "foo";
        };
        runInNewThread(callable);
        then(valueInNewThread.get()).as("By default thread local information should not be propagated").isNull();

        runInNewThread(defaultSnapshotFactory.captureAll().wrap(callable));

        then(valueInNewThread.get()).as("With context container the thread local information should be propagated")
            .isEqualTo("hello");
    }

    @Test
    void should_instrument_executor() throws InterruptedException, TimeoutException {
        StringThreadLocalHolder.setValue("hello");
        AtomicReference<String> valueInNewThread = new AtomicReference<>();
        Executor executor = command -> new Thread(command).start();
        runInNewThread(executor, valueInNewThread);
        then(valueInNewThread.get()).as("By default thread local information should not be propagated").isNull();

        runInNewThread(defaultSnapshotFactory.captureAll().wrapExecutor(executor), valueInNewThread);

        then(valueInNewThread.get()).as("With context container the thread local information should be propagated")
            .isEqualTo("hello");
    }

    @Test
    void should_instrument_executor_service() throws InterruptedException, ExecutionException, TimeoutException {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            StringThreadLocalHolder.setValue("hello");
            AtomicReference<String> valueInNewThread = new AtomicReference<>();
            runInNewThread(executorService, valueInNewThread,
                    atomic -> then(atomic.get()).as("By default thread local information should not be propagated")
                        .isNull());

            runInNewThread(ContextExecutorService.wrap(executorService, defaultSnapshotFactory::captureAll),
                    valueInNewThread,
                    atomic -> then(atomic.get())
                        .as("With context container the thread local information should be propagated")
                        .isEqualTo("hello"));
        }
        finally {
            executorService.shutdown();
        }
    }

    @Test
    void should_instrument_scheduled_executor_service()
            throws InterruptedException, ExecutionException, TimeoutException {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        try {
            StringThreadLocalHolder.setValue("hello at time of creation of the executor");
            AtomicReference<String> valueInNewThread = new AtomicReference<>();
            runInNewThread(executorService, valueInNewThread,
                    atomic -> then(atomic.get()).as("By default thread local information should not be propagated")
                        .isNull());

            StringThreadLocalHolder.setValue("hello at time of creation of the executor");
            runInNewThread(ContextExecutorService.wrap(executorService, defaultSnapshotFactory::captureAll),
                    valueInNewThread,
                    atomic -> then(atomic.get())
                        .as("With context container the thread local information should be propagated")
                        .isEqualTo("hello"));
        }
        finally {
            executorService.shutdown();
        }
    }

    private void runInNewThread(Runnable runnable) throws InterruptedException, TimeoutException {
        CountDownLatch latch = new CountDownLatch(1);
        Thread thread = new Thread(countDownWhenDone(runnable, latch));
        thread.start();

        throwIfTimesOut(latch);
    }

    private void runInNewThread(Callable<?> callable)
            throws InterruptedException, ExecutionException, TimeoutException {
        ExecutorService service = Executors.newSingleThreadExecutor();
        try {
            service.submit(callable).get(5, TimeUnit.MILLISECONDS);
        }
        finally {
            service.shutdown();
        }
    }

    private void runInNewThread(Executor executor, AtomicReference<String> valueInNewThread)
            throws InterruptedException, TimeoutException {
        CountDownLatch latch = new CountDownLatch(1);
        executor.execute(countDownWhenDone(runnable(valueInNewThread), latch));
        throwIfTimesOut(latch);
    }

    private Runnable countDownWhenDone(Runnable runnable, CountDownLatch latch) {
        return () -> {
            runnable.run();
            latch.countDown();
        };
    }

    private void throwIfTimesOut(CountDownLatch latch) throws InterruptedException, TimeoutException {
        if (!latch.await(5, TimeUnit.MILLISECONDS)) {
            throw new TimeoutException("Waiting for executed task timed out");
        }
    }

    private void runInNewThread(ExecutorService executor, AtomicReference<String> valueInNewThread,
            Consumer<AtomicReference<String>> assertion)
            throws InterruptedException, ExecutionException, TimeoutException {

        StringThreadLocalHolder.setValue("hello"); // IMPORTANT: We are setting the
                                                   // thread local value as late as
                                                   // possible
        executor.execute(runnable(valueInNewThread));
        Thread.sleep(5);
        assertion.accept(valueInNewThread);

        executor.submit(runnable(valueInNewThread)).get(5, TimeUnit.MILLISECONDS);
        assertion.accept(valueInNewThread);

        executor.submit(callable(valueInNewThread)).get(5, TimeUnit.MILLISECONDS);
        assertion.accept(valueInNewThread);

        executor.submit(runnable(valueInNewThread), "foo").get(5, TimeUnit.MILLISECONDS);
        assertion.accept(valueInNewThread);

        executor.invokeAll(Collections.singletonList(callable(valueInNewThread)));
        assertion.accept(valueInNewThread);

        executor.invokeAll(Collections.singletonList(callable(valueInNewThread)), 5, TimeUnit.MILLISECONDS);
        assertion.accept(valueInNewThread);

        executor.invokeAny(Collections.singletonList(callable(valueInNewThread)));
        assertion.accept(valueInNewThread);

        executor.invokeAny(Collections.singletonList(callable(valueInNewThread)), 5, TimeUnit.MILLISECONDS);
        assertion.accept(valueInNewThread);
    }

    private void runInNewThread(ScheduledExecutorService executor, AtomicReference<String> valueInNewThread,
            Consumer<AtomicReference<String>> assertion)
            throws InterruptedException, ExecutionException, TimeoutException {
        runInNewThread((ExecutorService) executor, valueInNewThread, assertion);

        executor.schedule(runnable(valueInNewThread), 0, TimeUnit.MILLISECONDS).get(5, TimeUnit.MILLISECONDS);
        assertion.accept(valueInNewThread);

        executor.schedule(callable(valueInNewThread), 0, TimeUnit.MILLISECONDS).get(5, TimeUnit.MILLISECONDS);
        ;
        assertion.accept(valueInNewThread);

        executor.scheduleAtFixedRate(runnable(valueInNewThread), 0, 1, TimeUnit.MILLISECONDS).cancel(true);
        assertion.accept(valueInNewThread);

        executor.scheduleWithFixedDelay(runnable(valueInNewThread), 0, 1, TimeUnit.MILLISECONDS).cancel(true);
        assertion.accept(valueInNewThread);
    }

    private Runnable runnable(AtomicReference<String> valueInNewThread) {
        return () -> valueInNewThread.set(StringThreadLocalHolder.getValue());
    }

    private Callable<Object> callable(AtomicReference<String> valueInNewThread) {
        return () -> {
            valueInNewThread.set(StringThreadLocalHolder.getValue());
            return "foo";
        };
    }

}
