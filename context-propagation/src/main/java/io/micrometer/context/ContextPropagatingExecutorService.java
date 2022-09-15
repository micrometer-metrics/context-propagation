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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;


/**
 * Wrap and delegate to an {@link ExecutorService} in order to instrument all
 * tasks executed through it.
 *
 * @author Marcin Grzejszczak
 * @author Rossen Stoyanchev
 * @since 1.0.0
 */
class ContextPropagatingExecutorService<EXECUTOR extends ExecutorService> implements ExecutorService {

    private final ContextSnapshot contextSnapshot;

    private final EXECUTOR executorService;


    /**
     * Create an instance
     * @param executorService the {@code ExecutorService} to delegate to
     * @param contextSnapshot the {@code ContextSnapshot} with values to propagate
     */
    ContextPropagatingExecutorService(EXECUTOR executorService, ContextSnapshot contextSnapshot) {
        this.contextSnapshot = contextSnapshot;
        this.executorService = executorService;
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
    public boolean isShutdown() {
        return this.executorService.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return this.executorService.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return this.executorService.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return this.executorService.submit(this.contextSnapshot.wrap(task));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return this.executorService.submit(this.contextSnapshot.wrap(task), result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return this.executorService.submit(this.contextSnapshot.wrap(task));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) 
            throws InterruptedException {

        List<Callable<T>> instrumentedTasks = tasks.stream()
                .map(this.contextSnapshot::wrap)
                .collect(Collectors.toList());

        return this.executorService.invokeAll(instrumentedTasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(
            Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) 
            throws InterruptedException {

        List<Callable<T>> instrumentedTasks = tasks.stream()
                .map(this.contextSnapshot::wrap)
                .collect(Collectors.toList());

        return this.executorService.invokeAll(instrumentedTasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) 
            throws InterruptedException, ExecutionException {

        List<Callable<T>> instrumentedTasks = tasks.stream()
                .map(this.contextSnapshot::wrap)
                .collect(Collectors.toList());

        return this.executorService.invokeAny(instrumentedTasks);
    }

    @Override
    public <T> T invokeAny(
            Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {

        List<Callable<T>> instrumentedTasks = tasks.stream()
                .map(this.contextSnapshot::wrap)
                .collect(Collectors.toList());

        return this.executorService.invokeAny(instrumentedTasks, timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        this.executorService.execute(this.contextSnapshot.wrap(command));
    }

    ContextSnapshot getContextSnapshot() {
        return this.contextSnapshot;
    }

    EXECUTOR getExecutorService() {
        return this.executorService;
    }
}
