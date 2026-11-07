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

import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;

/**
 * Wraps a {@link ThreadFactory} in order to capture context via {@link ContextSnapshot}
 * when a new thread is created, and propagate context to the thread when it is executed.
 * <p>
 * Intended to be used with short-lived threads dedicated to a single task, rather than
 * threads in a thread pool to which multiple tasks are delegated. Typically, the wrapped
 * {@link ThreadFactory} should create Virtual Threads.
 * </p>
 *
 * @author Phil Clay
 * @since 1.2.2
 */
public class ContextThreadFactory implements ThreadFactory {

    /**
     * The {@link ThreadFactory} to which to delegate {@link Thread} creation.
     */
    private final ThreadFactory threadFactory;

    /**
     * Supplies a {@link ContextSnapshot} to be propagated to the spawned threads.
     */
    private final Supplier<ContextSnapshot> contextSnapshotSupplier;

    private ContextThreadFactory(ThreadFactory threadFactory, Supplier<ContextSnapshot> contextSnapshotSupplier) {
        this.threadFactory = Objects.requireNonNull(threadFactory, "threadFactory must not be null");
        this.contextSnapshotSupplier = Objects.requireNonNull(contextSnapshotSupplier,
                "contextSnapshotSupplier must not be null");
    }

    @Override
    public Thread newThread(Runnable runnable) {
        return threadFactory.newThread(capture().wrap(runnable));
    }

    private ContextSnapshot capture() {
        return this.contextSnapshotSupplier.get();
    }

    /**
     * Wrap the given {@code ThreadFactory} in order to propagate context to any executed
     * runnable through the given {@link ContextSnapshotFactory}.
     * <p>
     * This method only captures ThreadLocal value. To work with other types of contexts,
     * use {@link #wrap(ThreadFactory, Supplier)}.
     * </p>
     * @param factory the threadFactory to wrap
     * @param contextSnapshotFactory {@link ContextSnapshotFactory} for capturing a
     * {@link ContextSnapshot} at the point when a new Thread is created from a Runnable
     * @return {@code ThreadFactory} wrapper
     */
    public static ThreadFactory wrap(ThreadFactory factory, ContextSnapshotFactory contextSnapshotFactory) {
        return wrap(factory, contextSnapshotFactory::captureAll);
    }

    /**
     * Wrap the given {@code ThreadFactory} in order to propagate context to any executed
     * runnable through the given {@link ContextSnapshot} supplier.
     * <p>
     * Typically, a {@link ContextSnapshotFactory} can be used to supply the snapshot. In
     * the case that only ThreadLocal values are to be captured, the
     * {@link #wrap(ThreadFactory, ContextSnapshotFactory)} variant can be used.
     * </p>
     * @param service the threadFactory to wrap
     * @param contextSnapshotSupplier supplier for capturing a {@link ContextSnapshot} at
     * the point when a new Thread is created from a Runnable
     * @return {@code ThreadFactory} wrapper
     */
    public static ThreadFactory wrap(ThreadFactory service, Supplier<ContextSnapshot> contextSnapshotSupplier) {
        return new ContextThreadFactory(service, contextSnapshotSupplier);
    }

}
