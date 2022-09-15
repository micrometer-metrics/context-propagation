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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


/**
 * Wrap and delegate to an {@link ExecutorService} in order to instrument all
 * tasks executed through it.
 *
 * @author Marcin Grzejszczak
 * @author Rossen Stoyanchev
 * @since 1.0.0
 */
final class ContextPropagatingScheduledExecutorService extends ContextPropagatingExecutorService<ScheduledExecutorService> implements ScheduledExecutorService {

    /**
     * Create an instance
     * @param executorService the {@code ScheduledExecutorService} to delegate to
     * @param contextSnapshot the {@code ContextSnapshot} with values to propagate
     */
    ContextPropagatingScheduledExecutorService(ScheduledExecutorService executorService, ContextSnapshot contextSnapshot) {
        super(executorService, contextSnapshot);
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return getExecutorService().schedule(getContextSnapshot().wrap(command), delay, unit);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return getExecutorService().schedule(getContextSnapshot().wrap(callable), delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return getExecutorService().scheduleAtFixedRate(getContextSnapshot().wrap(command), initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return getExecutorService().scheduleWithFixedDelay(getContextSnapshot().wrap(command), initialDelay, delay, unit);
    }
}
