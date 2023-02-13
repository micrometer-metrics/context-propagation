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

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Holds values extracted from {@link ThreadLocal} and other types of context and exposes
 * methods to propagate those values.
 *
 * <p>
 * Use static factory methods on this interface to create a snapshot.
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
     * Variant of {@link #updateContext(Object)} to update the given context with a subset
     * of snapshot values.
     * @param context the context to write to
     * @param keyPredicate predicate for context value keys
     * @return a context, possibly a new instance, with the written values
     * @param <C> the type of the target context
     */
    <C> C updateContext(C context, Predicate<Object> keyPredicate);

    /**
     * Set {@link ThreadLocal} values from the snapshot.
     * @return an object that can be used to reset {@link ThreadLocal} values at the end
     * of the context scope, either removing them or restoring their previous values, if
     * any.
     */
    Scope setThreadLocals();

    /**
     * Variant of {@link #setThreadLocals()} with a predicate to select context values by
     * key. All currently present {@link ThreadLocal} values, for which the
     * {@code sourceContext} has no key defined, will be {@link ThreadLocalAccessor#reset
     * reset} and {@link ThreadLocalAccessor#restore(Object) restored} at the end of the
     * scope.
     * @return an object that can be used to reset {@link ThreadLocal} values at the end
     * of the context scope, either removing them or restoring their previous values, if
     * any.
     */
    Scope setThreadLocals(Predicate<Object> keyPredicate);

    /**
     * Return a new {@code Runnable} that sets {@code ThreadLocal} values from the
     * snapshot around the invocation of the given {@code Runnable}.
     * @param runnable the runnable to instrument
     */
    default Runnable wrap(Runnable runnable) {
        return () -> {
            try (Scope scope = setThreadLocals()) {
                runnable.run();
            }
        };
    }

    /**
     * Return a new {@code Callable} that sets {@code ThreadLocal} values from the
     * snapshot around the invocation of the given {@code Callable}.
     * @param callable the callable to instrument
     * @param <T> the type of value produced by the {@code Callable}
     */
    default <T> Callable<T> wrap(Callable<T> callable) {
        return () -> {
            try (Scope scope = setThreadLocals()) {
                return callable.call();
            }
        };
    }

    /**
     * Return a new {@code Consumer} that sets {@code ThreadLocal} values from the
     * snapshot around the invocation of the given {@code Consumer}.
     * @param consumer the callable to instrument
     * @param <T> the type of value produced by the {@code Callable}
     */
    default <T> Consumer<T> wrap(Consumer<T> consumer) {
        return value -> {
            try (Scope scope = setThreadLocals()) {
                consumer.accept(value);
            }
        };
    }

    /**
     * Return a new {@code Executor} that sets {@code ThreadLocal} values from the
     * snapshot around the invocation of any executed, {@code Runnable}.
     * @param executor the executor to instrument
     * @see ContextExecutorService
     * @see ContextScheduledExecutorService
     */
    default Executor wrapExecutor(Executor executor) {
        return runnable -> {
            Runnable instrumentedRunnable = wrap(runnable);
            executor.execute(instrumentedRunnable);
        };
    }

    /**
     * Capture values from {@link ThreadLocal} and from other context objects using all
     * accessors from the {@link ContextRegistry#getInstance() global} ContextRegistry
     * instance.
     * @param contexts one more context objects to extract values from
     * @return a snapshot with saved context values
     */
    static ContextSnapshot captureAll(Object... contexts) {
        return captureAll(ContextRegistry.getInstance(), contexts);
    }

    /**
     * Capture values from {@link ThreadLocal} and from other context objects using all
     * accessors from the {@link ContextRegistry#getInstance() global} ContextRegistry
     * instance.
     * @param registry the registry to use
     * @param contexts one more context objects to extract values from
     * @return a snapshot with saved context values
     */
    static ContextSnapshot captureAll(ContextRegistry registry, Object... contexts) {
        return captureAllUsing(key -> true, registry, contexts);
    }

    /**
     * Variant of {@link #captureAll(Object...)} with a predicate to filter context keys
     * and with a specific {@link ContextRegistry} instance.
     * @param keyPredicate predicate for context value keys
     * @param registry the registry with the accessors to use
     * @param contexts one more context objects to extract values from
     * @return a snapshot with saved context values
     */
    static ContextSnapshot captureAllUsing(Predicate<Object> keyPredicate, ContextRegistry registry,
            Object... contexts) {

        return DefaultContextSnapshot.captureAll(registry, keyPredicate, contexts);
    }

    /**
     * Create a {@link ContextSnapshot} by reading values from the given context object.
     * @param context the context to read values from
     * @return the created {@link ContextSnapshot}
     */
    static ContextSnapshot captureFrom(Object context) {
        return captureFrom(context, ContextRegistry.getInstance());
    }

    /**
     * Create a {@link ContextSnapshot} by reading values from the given context object.
     * @param context the context to read values from
     * @param registry the registry to use
     * @return the created {@link ContextSnapshot}
     */
    static ContextSnapshot captureFrom(Object context, ContextRegistry registry) {
        return captureFrom(context, key -> true, registry);
    }

    /**
     * Create a {@link ContextSnapshot} by reading values from the given context object.
     * @param context the context to read values from
     * @return the created {@link ContextSnapshot}
     */
    static ContextSnapshot captureFrom(Object context, Predicate<Object> keyPredicate, ContextRegistry registry) {
        return DefaultContextSnapshot.captureFromContext(keyPredicate, registry, context, null);
    }

    /**
     * Sets all {@link ThreadLocal} values for which there is a value in the given source
     * context. All currently present {@link ThreadLocal} values, for which the
     * {@code sourceContext} has no key defined are not affected by this method. Use
     * {@link #setThreadLocals(Object)} to {@link ThreadLocalAccessor#reset() reset}
     * {@link ThreadLocal} values not present in the provided {@code sourceContext}.
     * @param sourceContext the source context to read values from
     * @return an object that can be used to reset {@link ThreadLocal} values at the end
     * of the context scope, either removing them or restoring their previous values, if
     * any.
     * @see #setThreadLocals(Object)
     */
    static Scope setAllThreadLocalsFrom(Object sourceContext) {
        return DefaultContextSnapshot.setAllThreadLocalsFrom(sourceContext, ContextRegistry.getInstance());
    }

    /**
     * Variant of {@link #setAllThreadLocalsFrom(Object)} with a specific
     * {@link ContextRegistry} instead of the global instance.
     * @param contextRegistry the registry with the accessors to use
     * @return an object that can be used to reset {@link ThreadLocal} values at the end
     * of the context scope, either removing them or restoring their previous values, if
     * any.
     * @see #setThreadLocals(Object, ContextRegistry)
     */
    static Scope setAllThreadLocalsFrom(Object sourceContext, ContextRegistry contextRegistry) {
        return DefaultContextSnapshot.setAllThreadLocalsFrom(sourceContext, contextRegistry);
    }

    /**
     * Sets all {@link ThreadLocal} values for which there is a value in the given source
     * context. All currently present {@link ThreadLocal} values, for which the
     * {@code sourceContext} has no key defined, will be {@link ThreadLocalAccessor#reset
     * reset} and {@link ThreadLocalAccessor#restore(Object) restored} at the end of the
     * scope.
     * @param sourceContext the source context to read values from
     * @return an object that can be used to reset {@link ThreadLocal} values at the end
     * of the context scope, either removing them or restoring their previous values, if
     * any.
     * @since 1.0.2
     */
    static Scope setThreadLocals(Object sourceContext) {
        return DefaultContextSnapshot.setThreadLocals(sourceContext, ContextRegistry.getInstance());
    }

    /**
     * Variant of {@link #setThreadLocals(Object)} with a specific {@link ContextRegistry}
     * instead of the global instance.
     * @param sourceContext the source context to read values from
     * @param contextRegistry the registry with the accessors to use
     * @return an object that can be used to reset {@link ThreadLocal} values at the end
     * of the context scope, either removing them or restoring their previous values, if
     * any.
     * @since 1.0.2
     */
    static Scope setThreadLocals(Object sourceContext, ContextRegistry contextRegistry) {
        return DefaultContextSnapshot.setThreadLocals(sourceContext, contextRegistry);
    }

    /**
     * Read the values specified by keys from the given source context, and if found, use
     * them to set {@link ThreadLocal} values. Currently present {@link ThreadLocal}
     * values for provided {@code keys}, for which the {@code sourceContext} has no key
     * defined, will be {@link ThreadLocalAccessor#reset} and restored at the end of the
     * scope. To create a union with currently set
     * {@link ThreadLocal} values,
     * {@link #captureAllUsing(Predicate, ContextRegistry, Object...)} can be used and
     * combined with {@link Scope#setThreadLocals(Predicate)} to delay the
     * {@link ThreadLocal} restoration.
     * @param sourceContext the source context to read values from
     * @param keys the keys of the values to read. If empty, all mappings from
     * {@code sourceContext} are considered.
     * @return an object that can be used to reset {@link ThreadLocal} values at the end
     * of the context scope, either removing them or restoring their previous values, if
     * any.
     */
    static Scope setThreadLocalsFrom(Object sourceContext, String... keys) {
        return setThreadLocalsFrom(sourceContext, ContextRegistry.getInstance(), keys);
    }

    /**
     * Variant of {@link #setThreadLocalsFrom(Object, String...)} with a specific
     * {@link ContextRegistry} instead of the global instance.
     * @param sourceContext the source context to read values from
     * @param contextRegistry the registry with the accessors to use
     * @param keys the keys of the values to read. If empty, all mappings from
     * {@code sourceContext} are considered.
     * @return an object that can be used to reset {@link ThreadLocal} values at the end
     * of the context scope, either removing them or restoring their previous values, if
     * any.
     */
    static Scope setThreadLocalsFrom(Object sourceContext, ContextRegistry contextRegistry, String... keys) {
        return DefaultContextSnapshot.setThreadLocalsFrom(sourceContext, contextRegistry, keys);
    }

    /**
     * An object to use to reset {@link ThreadLocal} values at the end of a context scope.
     */
    interface Scope extends AutoCloseable {

        /**
         * Reset {@link ThreadLocal} values, either removing them or restoring their
         * previous values, if any.
         */
        @Override
        void close();

    }

}
