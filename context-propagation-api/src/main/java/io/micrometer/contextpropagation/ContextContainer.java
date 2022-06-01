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
package io.micrometer.contextpropagation;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * {@link ContextContainer} is a holder of values that are being propagated through
 * various contexts. A context can be e.g. a Reactor Context, Reactor Netty Channel etc.
 * The values can be e.g. MDC entries, Micrometer Observation etc.
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
public interface ContextContainer {

    /**
     * Gets an element from the container.
     *
     * @param key key under which the element is stored
     * @param <T> type of the element
     * @return the element or {@code null} when it's not present
     */
    <T> T get(String key);

    /**
     * Checks if an element is available under the given key.
     *
     * @param key key under which the element is stored
     * @return {@code true} when element is stored
     */
    boolean containsKey(String key);

    /**
     * Puts the element under the given key.
     *
     * @param key key under which the element will be available
     * @param value element to store
     * @param <T> type of the value
     * @return previously stored element under this key or {@code null} if there was none
     */
    <T> T put(String key, T value);

    /**
     * Removes the element registered under the given key,
     *
     * @param key key under which the element is stored
     * @param <T> type of the removed element
     * @return the removed element or {@code null} if it wasn't there
     */
    <T> T remove(String key);

    /**
     * Capture values from a context and save them in the given container.
     *
     * @param context context to process
     */
    void captureValues(Object context);

    /**
     * Restore context values from the given container.
     *
     * @param context context to process
     * @param <T> type of the context
     * @return context with elements previously stored in the container
     * @see ContextContainer#captureValues(Object)
     */
    <T> T restoreValues(T context);

    /**
     * Captures the current thread local values and stores them in the container.
     * @return this for chaining
     */
    ContextContainer captureThreadLocalValues();

    /**
     * Captures the current thread local values and stores them in the container.
     * @param predicate condition to check namespaces against
     * @return this for chaining
     */
    ContextContainer captureThreadLocalValues(Predicate<Namespace> predicate);

    /**
     * Restores the previously captured thread local values and puts them in thread local
     * @return scope within which the thread local values are available
     * @see Scope
     */
    Scope restoreThreadLocalValues();

    /**
     * Saves this container in the provided context.
     *
     * @param context context in which we want to store this container
     * @param <T> type of the context
     * @return the context with the stored container
     * @throws IllegalStateException if a {@link ContextContainerAdapter}
     * that supports the given external context is not available
     */
    <T> T saveTo(T context);

    /**
     * Restores the {@link ContextContainer} from the provided external context.
     *
     * @param context context from which we want to retrieve from {@link ContextContainer}
     * @param <T> type of the context
     * @return the container retrieved from the given context
     * @throws IllegalStateException if a {@link ContextContainerAdapter}
     * that supports the given external context is not available, or if the
     * external context does not have a {@link ContextContainer} saved
     * @see #restoreIfPresentFrom(Object)
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static <T> ContextContainer restoreFrom(T context) {
        ContextContainerAdapter adapter = ContextContainerAdapterLoader.getAdapterToRead(context);
        return adapter.restore(context);
    }

    /**
     * Restores the {@link ContextContainer} from the provided external context.
     *
     * @param context context from which we want to retrieve from {@link ContextContainer}
     * @param <T> type of the context
     * @return the container retrieved from the external context or {@code null}
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static <T> ContextContainer restoreIfPresentFrom(T context) {
        ContextContainerAdapter adapter = ContextContainerAdapterLoader.getAdapterToRead(context);
        return adapter.restoreIfPresent(context);
    }

    /**
     * Removes the {@link ContextContainer} from the given context.
     *
     * @param context context from which we want to remove the {@link ContextContainer}
     * @param <T> type of the context
     * @return context with {@link ContextContainer} removed
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static <T> T removeFrom(T context) {
        ContextContainerAdapter adapter = ContextContainerAdapterLoader.getAdapterToWrite(context);
        return (T) adapter.remove(context);
    }

    /**
     * Takes the runnable and runs it with thread local values available.
     * Clears the thread local values when the runnable has been finished.
     *
     * @param action runnable to run
     */
    void tryScoped(Runnable action);

    /**
     * Takes the supplier and runs it with thread local values available.
     * Clears the thread local values when the supplier has been finished.
     *
     * @param action supplier to run
     * @param <T> type that the supplier returns
     * @return value returned by the supplier
     */
    <T> T tryScoped(Supplier<T> action);


    /**
     * Propagates the context over the {@link Runnable}.
     *
     * @param action action through which context should be propagated
     * @return wrapped action
     */
    default Runnable wrap(Runnable action) {
        ContextContainer container = captureThreadLocalValues();
        return () -> container.tryScoped(action);
    }

    /**
     * Propagates the context over the {@link Callable}.
     *
     * @param action action through which context should be propagated
     * @return wrapped action
     */
    default <T> Callable<T> wrap(Callable<T> action) {
        ContextContainer container = captureThreadLocalValues();
        return () -> {
            try (Scope scope = container.restoreThreadLocalValues()) {
                return action.call();
            }
        };
    }

    /**
     * Propagates the context over the {@link Executor}.
     *
     * @param executor executor through which context should be propagated
     * @return wrapped executor
     */
    default Executor wrap(Executor executor) {
        return command -> executor.execute(wrap(command));
    }

    /**
     * Propagates the context over the {@link ExecutorService}.
     *
     * @param executorService executor service through which context should be propagated
     * @return wrapped executor
     */
    default ExecutorService wrap(ExecutorService executorService) {
        return new WrappedExecutorService(this, executorService);
    }

    /**
     * Demarcates the scope of restored ThreadLocal values.
     */
    interface Scope extends AutoCloseable {
        @Override
        void close();
    }

}
