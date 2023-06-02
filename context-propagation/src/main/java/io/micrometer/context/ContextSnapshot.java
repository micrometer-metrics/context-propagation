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

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Holds values extracted from {@link ThreadLocal} and other types of context and exposes
 * methods to propagate those values.
 * <p>
 * Use {@link ContextSnapshotFactory#builder()} to configure a factory to work with
 * snapshots.
 * <p>
 * Implementations are disallowed to store {@code null} values. If a {@link ThreadLocal}
 * is not set, or it's value is {@code null}, there is no way of distinguishing one from
 * the other. In such a case, the {@link ContextSnapshot} simply must not contain a
 * capture for the particular {@link ThreadLocal}. Implementations should filter out any
 * {@code null} values after reading into the storage also obtained by calling
 * {@link ContextAccessor#readValues(Object, Predicate, Map)}, and should likewise ignore
 * {@code null} values from {@link ContextAccessor#readValue(Object, Object)}.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Dariusz Jędrzejczyk
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
     * key.
     * @param keyPredicate selects keys for use when setting {@link ThreadLocal} values
     * @return an object that can be used to reset {@link ThreadLocal} values at the end
     * of the context scope, either removing them or restoring their previous values, if
     * any.
     */
    Scope setThreadLocals(Predicate<Object> keyPredicate);

    /**
     * Return a new {@code Runnable} that sets {@code ThreadLocal} values from the
     * snapshot around the invocation of the given {@code Runnable}.
     * @param runnable the runnable to instrument
     * @return wrapped instance
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
     * @return wrapped instance
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
     * @return wrapped instance
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
     * @return wrapped instance
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
     * @deprecated use {@link ContextSnapshotFactory#captureAll(Object...)} on a factory
     * obtained via a {@link ContextSnapshotFactory#builder()}.
     */
    @Deprecated
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
     * @deprecated use {@link ContextSnapshotFactory#captureAll(Object...)} on a factory
     * obtained via a {@link ContextSnapshotFactory#builder()} combined with
     * {@link ContextSnapshotFactory.Builder#contextRegistry(ContextRegistry)}.
     */
    @Deprecated
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
     * @deprecated use {@link ContextSnapshotFactory#captureAll(Object...)} on a factory
     * obtained via a {@link ContextSnapshotFactory#builder()} and configure
     * {@link ContextSnapshotFactory.Builder#captureKeyPredicate(Predicate)} and
     * {@link ContextSnapshotFactory.Builder#contextRegistry(ContextRegistry)}.
     */
    @Deprecated
    static ContextSnapshot captureAllUsing(Predicate<Object> keyPredicate, ContextRegistry registry,
            Object... contexts) {
        return DefaultContextSnapshotFactory.captureAll(registry, keyPredicate, false, contexts);
    }

    /**
     * Create a {@link ContextSnapshot} by reading values from the given context object.
     * @param context the context to read values from
     * @return the created {@link ContextSnapshot}
     * @deprecated use {@link ContextSnapshotFactory#captureFrom(Object...)} on a factory
     * obtained via a {@link ContextSnapshotFactory#builder()}.
     */
    @Deprecated
    static ContextSnapshot captureFrom(Object context) {
        return captureFrom(context, ContextRegistry.getInstance());
    }

    /**
     * Create a {@link ContextSnapshot} by reading values from the given context object.
     * @param context the context to read values from
     * @param registry the registry to use
     * @return the created {@link ContextSnapshot}
     * @deprecated use {@link ContextSnapshotFactory#captureFrom(Object...)} on a factory
     * obtained via a {@link ContextSnapshotFactory#builder()} combined with
     * {@link ContextSnapshotFactory.Builder#contextRegistry(ContextRegistry)}.
     */
    @Deprecated
    static ContextSnapshot captureFrom(Object context, ContextRegistry registry) {
        return DefaultContextSnapshotFactory.captureFromContext(key -> true, false, registry, null, context);
    }

    /**
     * Create a {@link ContextSnapshot} by reading values from the given context object.
     * @param context the context to read values from
     * @param keyPredicate predicate for context value keys
     * @param registry the registry to use
     * @return the created {@link ContextSnapshot}
     * @deprecated use {@link ContextSnapshotFactory#captureFrom(Object...)} on a factory
     * obtained via a {@link ContextSnapshotFactory#builder()} and configure
     * {@link ContextSnapshotFactory.Builder#captureKeyPredicate(Predicate)} and
     * {@link ContextSnapshotFactory.Builder#contextRegistry(ContextRegistry)}.
     */
    @Deprecated
    static ContextSnapshot captureFrom(Object context, Predicate<Object> keyPredicate, ContextRegistry registry) {
        return DefaultContextSnapshotFactory.captureFromContext(keyPredicate, false, registry, null, context);
    }

    /**
     * Variant of {@link #setThreadLocalsFrom(Object, String...)} that sets all
     * {@link ThreadLocal} values for which there is a value in the given source context.
     * @param sourceContext the source context to read values from
     * @return an object that can be used to reset {@link ThreadLocal} values at the end
     * of the context scope, either removing them or restoring their previous values, if
     * any.
     * @deprecated use
     * {@link ContextSnapshotFactory#setThreadLocalsFrom(Object, String...)} with no keys
     * on a factory obtained via a {@link ContextSnapshotFactory#builder()}.
     */
    @Deprecated
    static Scope setAllThreadLocalsFrom(Object sourceContext) {
        return DefaultContextSnapshotFactory.setAllThreadLocalsFrom(sourceContext, ContextRegistry.getInstance(),
                false);
    }

    /**
     * Variant of {@link #setThreadLocalsFrom(Object, String...)} that sets all
     * {@link ThreadLocal} values for which there is a value in the given source context.
     * @param sourceContext the source context to read values from
     * @param contextRegistry the registry with the accessors to use
     * @return an object that can be used to reset {@link ThreadLocal} values at the end
     * of the context scope, either removing them or restoring their previous values, if
     * any.
     * @deprecated use
     * {@link ContextSnapshotFactory#setThreadLocalsFrom(Object, String...)} with no keys
     * on a factory obtained via a {@link ContextSnapshotFactory#builder()} combined with
     * {@link ContextSnapshotFactory.Builder#contextRegistry(ContextRegistry)}.
     */
    @Deprecated
    static Scope setAllThreadLocalsFrom(Object sourceContext, ContextRegistry contextRegistry) {
        return DefaultContextSnapshotFactory.setAllThreadLocalsFrom(sourceContext, contextRegistry, false);
    }

    /**
     * Read the values specified by from the given source context, and if found, use them
     * to set {@link ThreadLocal} values. Essentially, a shortcut that bypasses the need
     * to create of {@link ContextSnapshot} first via {@link #captureAll(Object...)},
     * followed by {@link #setThreadLocals()}.
     * @param sourceContext the source context to read values from
     * @param keys the keys of the values to read (at least one key must be passed)
     * @return an object that can be used to reset {@link ThreadLocal} values at the end
     * of the context scope, either removing them or restoring their previous values, if
     * any.
     * @deprecated use
     * {@link ContextSnapshotFactory#setThreadLocalsFrom(Object, String...)} on a factory
     * obtained via a {@link ContextSnapshotFactory#builder()}.
     */
    @Deprecated
    static Scope setThreadLocalsFrom(Object sourceContext, String... keys) {
        return setThreadLocalsFrom(sourceContext, ContextRegistry.getInstance(), keys);
    }

    /**
     * Variant of {@link #setThreadLocalsFrom(Object, String...)} with a specific
     * {@link ContextRegistry} instead of the global instance.
     * @param sourceContext the source context to read values from
     * @param contextRegistry the registry with the accessors to use
     * @param keys the keys of the values to read (at least one key must be passed)
     * @return an object that can be used to reset {@link ThreadLocal} values at the end
     * of the context scope, either removing them or restoring their previous values, if
     * any.
     * @deprecated use
     * {@link ContextSnapshotFactory#setThreadLocalsFrom(Object, String...)} on a factory
     * obtained via a {@link ContextSnapshotFactory#builder()} combined with
     * {@link ContextSnapshotFactory.Builder#contextRegistry(ContextRegistry)}.
     */
    @Deprecated
    static Scope setThreadLocalsFrom(Object sourceContext, ContextRegistry contextRegistry, String... keys) {
        return DefaultContextSnapshotFactory.setThreadLocalsFrom(sourceContext, contextRegistry, false, keys);
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
