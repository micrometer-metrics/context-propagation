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

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Holds values extracted from {@link ThreadLocal} and other types of context and
 * exposes methods to propagate those values.
 *
 * <p>Use {@link #builder()} to create a snapshot.
 *
 * @author Rossen Stoyanchev
 * @since 1.0.0
 */
public interface ContextSnapshot {

    /**
     * Update the given context with all snapshot values.
     * @param context the context to write to
     * @return a context, possibly a new instance, with the written values
     * @param <C> the type of the target context
     */
    <C> C updateContext(C context);

    /**
     * Variant of {@link #updateContext(Object)} to update the given context
     * with a subset of snapshot values.
     * @param context the context to write to
     * @param keyPredicate predicate for context value keys
     * @return a context, possibly a new instance, with the written values
     * @param <C> the type of the target context
     */
    <C> C updateContext(C context, Predicate<Object> keyPredicate);

    /**
     * Set {@link ThreadLocal} values from the snapshot.
     * @return an object that can be used to reset {@link ThreadLocal} values
     * at the end of the context scope, either removing them or restoring their
     * previous values, if any.
     */
    Scope setThreadLocalValues();

    /**
     * Variant of {@link #setThreadLocalValues()} with a predicate to select
     * context values by key.
     * @return an object that can be used to reset {@link ThreadLocal} values
     * at the end of the context scope, either removing them or restoring their
     * previous values, if any.
     */
    Scope setThreadLocalValues(Predicate<Object> keyPredicate);

    /**
     * Return a new {@code Runnable} that sets {@code ThreadLocal} values from
     * the snapshot around the invocation of the given {@code Runnable}.
     * @param runnable the runnable to instrument
     */
    default Runnable instrumentRunnable(Runnable runnable) {
        return () -> {
            try (Scope scope = setThreadLocalValues()) {
                runnable.run();
            }
        };
    }

    /**
     * Return a new {@code Callable} that sets {@code ThreadLocal} values from
     * the snapshot around the invocation of the given {@code Callable}.
     * @param callable the callable to instrument
     * @param <T> the type of value produced by the {@code Callable}
     */
    default <T> Callable<T> instrumentCallable(Callable<T> callable) {
        return () -> {
            try (Scope scope = setThreadLocalValues()) {
                return callable.call();
            }
        };
    }

    /**
     * Return a new {@code Consumer} that sets {@code ThreadLocal} values from
     * the snapshot around the invocation of the given {@code Consumer}.
     * @param consumer the callable to instrument
     * @param <T> the type of value produced by the {@code Callable}
     */
    default <T> Consumer<T> instrumentConsumer(Consumer<T> consumer) {
        return value -> {
            try (Scope scope = setThreadLocalValues()) {
                consumer.accept(value);
            }
        };
    }

    /**
     * Return a new {@code Executor} that sets {@code ThreadLocal} values from
     * the snapshot around the invocation of any executed, {@code Runnable}.
     * @param executor the executor to instrument
     */
    default Executor instrumentExecutor(Executor executor) {
        return runnable -> {
            Runnable instrumentedRunnable = instrumentRunnable(runnable);
            executor.execute(instrumentedRunnable);
        };
    }

    /**
     * Return a new {@code ExecutorService} that sets {@code ThreadLocal} values from
     * the snapshot around the invocation of any executed task.
     * @param executorService the executorService to instrument
     */
    default ExecutorService instrumentExecutorService(ExecutorService executorService) {
        return new InstrumentedExecutorService(executorService, this);
    }


    /**
     * Return a {@link Builder} to create a snapshot that uses accessors from
     * the global {@link ContextRegistry}.
     */
    static Builder builder() {
        return builder(ContextRegistry.getInstance());
    }

    /**
     * Variant of {@link #builder()} with the {@link ContextRegistry} to use.
     */
    static Builder builder(ContextRegistry registry) {
        return new DefaultContextSnapshotBuilder(registry);
    }


    /**
     * Builder to create a {@link ContextSnapshot} with.
     */
    interface Builder {

        /**
         * Specify keys for context values of interest to extract and save.
         * <p>By default, this is not set. If neither this nor
         * {@link #filter(Predicate)} is set, then all context values are saved.
         * @param keys the keys of interest
         * @return the same builder instance
         */
        Builder include(Object... keys);

        /**
         * Configure a filter to decide which context values to save. Supports
         * multiple invocations in which case filters are chained.
         * <p>By default, this is not set. If neither this nor
         * {@link #include(Object...)} is set, then all context values are saved.
         * @param keyPredicate predicate for context value keys
         * @return the same builder instance
         */
        Builder filter(Predicate<Object> keyPredicate);

        /**
         * Build a snapshot by extracting ThreadLocal values as well as values
         * from the given context objects.
         * @param contexts one more context objects to extract values from
         * @return a snapshot with saved context values
         */
        ContextSnapshot build(Object... contexts);

    }


    /**
     * An object to use to reset {@link ThreadLocal} values at the end of a
     * context scope.
     */
    interface Scope extends AutoCloseable {

        /**
         * Reset {@link ThreadLocal} values, either removing them or restoring
         * their previous values, if any.
         */
        @Override
        void close();

    }

}
