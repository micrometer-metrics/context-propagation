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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Wraps a {@code ScheduledExecutorService} in order to capture context via
 * {@link ContextSnapshot} when a task is submitted, and propagate context to the task
 * when it is executed.
 *
 * @author Marcin Grzejszczak
 * @author Rossen Stoyanchev
 * @since 1.0.0
 */
public final class ContextScheduledExecutorService extends ContextExecutorService<ScheduledExecutorService>
        implements ScheduledExecutorService {

    /**
     * Create an instance
     * @param service the {@code ScheduledExecutorService} to delegate to
     * @param snapshotSupplier supplier of the {@link ContextSnapshot} - instruction on
     * who to retrieve {@link ContextSnapshot} when tasks are scheduled
     */
    private ContextScheduledExecutorService(ScheduledExecutorService service,
            Supplier<ContextSnapshot> snapshotSupplier) {
        super(service, snapshotSupplier);
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return getExecutorService().schedule(capture().wrap(command), delay, unit);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return getExecutorService().schedule(capture().wrap(callable), delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return getExecutorService().scheduleAtFixedRate(capture().wrap(command), initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return getExecutorService().scheduleWithFixedDelay(capture().wrap(command), initialDelay, delay, unit);
    }

    /**
     * Wrap the given {@code ScheduledExecutorService} in order to propagate context to
     * any executed task through the given {@link ContextSnapshot} supplier.
     * @param service the executorService to wrap
     * @param supplier supplier for capturing a {@link ContextSnapshot} at the point when
     * tasks are scheduled
     */
    public static ScheduledExecutorService wrap(ScheduledExecutorService service, Supplier<ContextSnapshot> supplier) {
        return new ContextScheduledExecutorService(service, supplier);
    }

    /**
     * Variant of {@link #wrap(ScheduledExecutorService, Supplier)} that uses
     * {@link ContextSnapshot#captureAll(Object...)} to create the context snapshot.
     * @param service the executorService to wrap
     * @deprecated use {@link #wrap(ScheduledExecutorService, Supplier)}
     */
    @Deprecated
    public static ScheduledExecutorService wrap(ScheduledExecutorService service) {
        return wrap(service,
                () -> DefaultContextSnapshotFactory.captureAll(ContextRegistry.getInstance(), key -> true, false));
    }

}
