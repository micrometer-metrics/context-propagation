/*
 * Copyright 2019 VMware, Inc.
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
package io.micrometer.contextpropagation;

import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;

class InstrumentationTests {

    ContextContainer container = ContextContainer.create();

    @AfterEach
    void clear() {
        ObservationThreadLocalHolder.holder.remove();
    }

    @Test
    void should_instrument_runnable() throws InterruptedException {
        ObservationThreadLocalHolder.holder.set("hello");
        AtomicReference<String> valueInNewThread = new AtomicReference<>();
        Runnable runnable = runnable(valueInNewThread);
        runInNewThread(runnable);
        then(valueInNewThread.get()).as("By default thread local information should not be propagated").isNull();

        runInNewThread(container.wrap(runnable));

        then(valueInNewThread.get()).as("With context container the thread local information should be propagated").isEqualTo("hello");
    }

    @Test
    void should_instrument_callable() throws ExecutionException, InterruptedException, TimeoutException {
        ObservationThreadLocalHolder.holder.set("hello");
        AtomicReference<String> valueInNewThread = new AtomicReference<>();
        Callable<String> callable = () -> {
            valueInNewThread.set(ObservationThreadLocalHolder.holder.get());
            return "foo";
        };
        runInNewThread(callable);
        then(valueInNewThread.get()).as("By default thread local information should not be propagated").isNull();

        runInNewThread(container.wrap(callable));

        then(valueInNewThread.get()).as("With context container the thread local information should be propagated").isEqualTo("hello");
    }

    @Test
    void should_instrument_executor() throws InterruptedException {
        ObservationThreadLocalHolder.holder.set("hello");
        AtomicReference<String> valueInNewThread = new AtomicReference<>();
        Executor executor = command -> new Thread(command).start();
        runInNewThread(executor, valueInNewThread);
        then(valueInNewThread.get()).as("By default thread local information should not be propagated").isNull();

        runInNewThread(container.wrap(executor), valueInNewThread);

        then(valueInNewThread.get()).as("With context container the thread local information should be propagated").isEqualTo("hello");
    }

    @Test
    void should_instrument_executor_service() throws InterruptedException, ExecutionException, TimeoutException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            ObservationThreadLocalHolder.holder.set("hello");
            AtomicReference<String> valueInNewThread = new AtomicReference<>();
            runInNewThread(executor, valueInNewThread, atomic -> then(atomic.get()).as("By default thread local information should not be propagated").isNull());

            runInNewThread(container.wrap(executor), valueInNewThread, atomic -> then(atomic.get()).as("With context container the thread local information should be propagated").isEqualTo("hello"));
        } finally {
            executor.shutdown();
        }
    }

    private void runInNewThread(Runnable runnable) throws InterruptedException {
        Thread thread = new Thread(runnable);
        thread.start();
        Thread.sleep(5);
    }

    private void runInNewThread(Callable callable) throws InterruptedException, ExecutionException, TimeoutException {
        ExecutorService service = Executors.newSingleThreadExecutor();
        try {
            service.submit(callable).get(5, TimeUnit.MILLISECONDS);
        } finally {
            service.shutdown();
        }
    }

    private void runInNewThread(Executor executor, AtomicReference<String> valueInNewThread) throws InterruptedException {
        executor.execute(runnable(valueInNewThread));
        Thread.sleep(5);
    }

    private void runInNewThread(ExecutorService executor, AtomicReference<String> valueInNewThread, Consumer<AtomicReference<String>> assertion) throws InterruptedException, ExecutionException, TimeoutException {
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

    private Runnable runnable(AtomicReference<String> valueInNewThread) {
        return () -> valueInNewThread.set(ObservationThreadLocalHolder.holder.get());
    }

    private Callable<Object> callable(AtomicReference<String> valueInNewThread) {
        return () -> {
            valueInNewThread.set(ObservationThreadLocalHolder.holder.get());
            return "foo";
        };
    }

}
