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
 * <p>Use static factory methods on this interface to create a snapshot.
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
    default Runnable wrap(Runnable runnable) {
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
    default <T> Callable<T> wrap(Callable<T> callable) {
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
    default <T> Consumer<T> wrap(Consumer<T> consumer) {
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
    default Executor wrapExecutor(Executor executor) {
        return runnable -> {
            Runnable instrumentedRunnable = wrap(runnable);
            executor.execute(instrumentedRunnable);
        };
    }

    /**
     * Return a new {@code ExecutorService} that sets {@code ThreadLocal} values from
     * the snapshot around the invocation of any executed task.
     * @param executorService the executorService to instrument
     */
    default ExecutorService wrapExecutorService(ExecutorService executorService) {
        return new ContextExecutorService(executorService, this);
    }


    /**
     * Capture values from {@link ThreadLocal} and from other context objects
     * using all accessors from the {@link ContextRegistry#getInstance() global}
     * ContextRegistry instance.
     * @param contexts one more context objects to extract values from
     * @return a snapshot with saved context values
     */
    static ContextSnapshot captureAll(Object... contexts) {
        return captureAllUsing(ContextRegistry.getInstance(), key -> true, contexts);
    }

    /**
     * Variant of {@link #captureAll(Object...)} that uses a
     * {@link Predicate} to decide which context values to capture.
     * @param keyPredicate predicate for context value keys
     * @param contexts one more context objects to extract values from
     * @return a snapshot with saved context values
     */
    static ContextSnapshot captureAllUsing(Predicate<Object> keyPredicate, Object... contexts) {
        return captureAllUsing(ContextRegistry.getInstance(), keyPredicate, contexts);
    }

    /**
     * Variant of {@link #captureAllUsing(Predicate, Object...)} with a specific
     * {@link ContextRegistry} instead of the global instance.
     * @param contextRegistry the registry with the accessors to use
     * @param keyPredicate predicate for context value keys
     * @param contexts one more context objects to extract values from
     * @return a snapshot with saved context values
     */
    static ContextSnapshot captureAllUsing(
            ContextRegistry contextRegistry, Predicate<Object> keyPredicate, Object... contexts) {

        return DefaultContextSnapshot.captureAll(contextRegistry, keyPredicate, contexts);
    }

    /**
     * Create a {@link ContextSnapshot} by reading values from the given context object.
     * @param context the context to read values from
     * @return the created {@link ContextSnapshot}
     */
    static ContextSnapshot captureFrom(Object context) {
        return DefaultContextSnapshot.captureFromContext(ContextRegistry.getInstance(), key -> true, context, null);
    }

    /**
     * Read the values specified by from the given source context, and if found,
     * use them to set {@link ThreadLocal} values. Essentially, a shortcut that
     * bypasses the need to create of {@link ContextSnapshot} first via
     * {@link #captureAll(Object...)}, followed by {@link #setThreadLocalValues()}.
     * @param sourceContext the source context to read values from
     * @param keys the keys of the values to read
     * @return an object that can be used to reset {@link ThreadLocal} values
     * at the end of the context scope, either removing them or restoring their
     * previous values, if any.
     */
    static Scope setThreadLocalsFrom(Object sourceContext, String... keys) {
        return setThreadLocalsFrom(sourceContext, ContextRegistry.getInstance(), keys);
    }

    /**
     * Variant of {@link #setThreadLocalsFrom(Object, String...)} with a specific
     * {@link ContextRegistry} instead of the global instance.
     * @param sourceContext the source context to read values from
     * @param contextRegistry the registry with the accessors to use
     * @param keys the keys of the values to read
     * @return an object that can be used to reset {@link ThreadLocal} values
     * at the end of the context scope, either removing them or restoring their
     * previous values, if any.
     */
    static Scope setThreadLocalsFrom(Object sourceContext, ContextRegistry contextRegistry, String... keys) {
        return DefaultContextSnapshot.setThreadLocalsFrom(sourceContext, contextRegistry, keys);
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
