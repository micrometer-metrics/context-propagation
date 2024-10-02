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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Wraps an {@code ExecutorService} in order to capture context via
 * {@link ContextSnapshot} when a task is submitted, and propagate context to the task
 * when it is executed.
 *
 * @author Marcin Grzejszczak
 * @author Rossen Stoyanchev
 * @since 1.0.0
 */
public class ContextExecutorService<EXECUTOR extends ExecutorService> implements ExecutorService {

    private final EXECUTOR executorService;

    private final Supplier<ContextSnapshot> contextSnapshot;

    /**
     * Create an instance of {@link ContextScheduledExecutorService}.
     * @param executorService the {@code ExecutorService} to delegate to
     * @param contextSnapshot supplier of the {@link ContextSnapshot} - instruction on who
     * to retrieve {@link ContextSnapshot} when tasks are scheduled
     */
    protected ContextExecutorService(EXECUTOR executorService, Supplier<ContextSnapshot> contextSnapshot) {
        this.executorService = executorService;
        this.contextSnapshot = contextSnapshot;
    }

    protected EXECUTOR getExecutorService() {
        return this.executorService;
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return this.executorService.submit(capture().wrap(task));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return this.executorService.submit(capture().wrap(task), result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return this.executorService.submit(capture().wrap(task));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {

        List<Callable<T>> instrumentedTasks = tasks.stream().map(capture()::wrap).collect(Collectors.toList());

        return this.executorService.invokeAll(instrumentedTasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException {

        List<Callable<T>> instrumentedTasks = tasks.stream().map(capture()::wrap).collect(Collectors.toList());

        return this.executorService.invokeAll(instrumentedTasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {

        List<Callable<T>> instrumentedTasks = tasks.stream().map(capture()::wrap).collect(Collectors.toList());

        return this.executorService.invokeAny(instrumentedTasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {

        List<Callable<T>> instrumentedTasks = tasks.stream().map(capture()::wrap).collect(Collectors.toList());

        return this.executorService.invokeAny(instrumentedTasks, timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        this.executorService.execute(capture().wrap(command));
    }

    @Override
    public boolean isShutdown() {
        return this.executorService.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return this.executorService.isTerminated();
    }

    @Override
    public void shutdown() {
        this.executorService.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return this.executorService.shutdownNow();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return this.executorService.awaitTermination(timeout, unit);
    }

    protected ContextSnapshot capture() {
        return this.contextSnapshot.get();
    }

    /**
     * Wrap the given {@code ExecutorService} in order to propagate context to any
     * executed task through the given {@link ContextSnapshotFactory}.
     * <p>
     * This method only captures ThreadLocal value. To work with other types of
     * contexts, use {@link #wrap(ExecutorService, Supplier)}.
     * </p>
     * @param service the executorService to wrap
     * @param contextSnapshotFactory {@link ContextSnapshotFactory} for capturing a
     * {@link ContextSnapshot} at the point when tasks are scheduled
     */
    public static ExecutorService wrap(ExecutorService service, ContextSnapshotFactory contextSnapshotFactory) {
        return new ContextExecutorService<>(service, contextSnapshotFactory::captureAll);
    }

    /**
     * Wrap the given {@code ExecutorService} in order to propagate context to any
     * executed task through the given {@link ContextSnapshot} supplier.
     * <p>
     * Typically, a {@link ContextSnapshotFactory} can be used to supply the snapshot. In
     * the case that only ThreadLocal values are to be captured, the
     * {@link #wrap(ExecutorService, ContextSnapshotFactory)} variant can be used.
     * </p>
     * @param service the executorService to wrap
     * @param snapshotSupplier supplier for capturing a {@link ContextSnapshot} at the
     * point when tasks are scheduled
     */
    public static ExecutorService wrap(ExecutorService service, Supplier<ContextSnapshot> snapshotSupplier) {
        return new ContextExecutorService<>(service, snapshotSupplier);
    }

    /**
     * Variant of {@link #wrap(ExecutorService, Supplier)} that uses
     * {@link ContextSnapshot#captureAll(Object...)} to create the context snapshot.
     * @param service the executorService to wrap
     * @deprecated use {@link #wrap(ExecutorService, Supplier)}
     */
    @Deprecated
    public static ExecutorService wrap(ExecutorService service) {
        return wrap(service,
                () -> DefaultContextSnapshotFactory.captureAll(ContextRegistry.getInstance(), key -> true, false));
    }

}
