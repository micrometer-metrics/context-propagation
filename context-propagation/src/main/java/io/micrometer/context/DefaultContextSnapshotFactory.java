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

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class DefaultContextSnapshotFactory implements ContextSnapshotFactory {

    private final ContextRegistry defaultRegistry;

    private final boolean clearMissing;

    private final Predicate<Object> keyPredicate;

    public static final DefaultContextSnapshotFactory INSTANCE = new DefaultContextSnapshotFactory(
            ContextRegistry.getInstance(), false, key -> true);

    public DefaultContextSnapshotFactory(ContextRegistry contextRegistry, boolean clearMissing,
            Predicate<Object> predicate) {
        this.defaultRegistry = contextRegistry;
        this.clearMissing = clearMissing;
        this.keyPredicate = predicate;
    }

    @Override
    public ContextSnapshot captureAll(Object... contexts) {
        return DefaultContextSnapshot.captureAll(defaultRegistry, keyPredicate, clearMissing, contexts);
    }

    @Override
    public ContextSnapshot captureFromContext(Object... contexts) {
        return DefaultContextSnapshot.captureFromContext(keyPredicate, clearMissing, defaultRegistry, null, contexts);
    }

    @Override
    public <C> ContextSnapshot.Scope setThreadLocalsFrom(Object sourceContext, String... keys) {
        if (keys == null || keys.length == 0) {
            return setAllThreadLocalsFrom(sourceContext, defaultRegistry, clearMissing);
        }
        else {
            return setThreadLocalsFrom(sourceContext, defaultRegistry, clearMissing, keys);
        }
    }

    @SuppressWarnings("unchecked")
    static <C> ContextSnapshot.Scope setAllThreadLocalsFrom(Object sourceContext, ContextRegistry contextRegistry,
            boolean clearMissing) {
        ContextAccessor<?, ?> contextAccessor = contextRegistry.getContextAccessorForRead(sourceContext);
        Map<Object, Object> previousValues = null;
        for (ThreadLocalAccessor<?> threadLocalAccessor : contextRegistry.getThreadLocalAccessors()) {
            Object key = threadLocalAccessor.key();
            Object value = ((ContextAccessor<C, ?>) contextAccessor).readValue((C) sourceContext, key);
            if (value != null) {
                previousValues = DefaultContextSnapshot.setThreadLocal(key, value, threadLocalAccessor, previousValues);
            }
            else if (clearMissing) {
                previousValues = DefaultContextSnapshot.clearThreadLocal(key, threadLocalAccessor, previousValues);
            }
        }
        return DefaultContextSnapshot.DefaultScope.from(previousValues, contextRegistry);
    }

    @SuppressWarnings("unchecked")
    static <C> ContextSnapshot.Scope setThreadLocalsFrom(Object sourceContext, ContextRegistry contextRegistry,
            boolean clearMissing, String... keys) {
        if (keys == null || keys.length == 0) {
            throw new IllegalArgumentException("You must provide at least one key when setting thread locals");
        }
        ContextAccessor<?, ?> contextAccessor = contextRegistry.getContextAccessorForRead(sourceContext);
        Map<Object, Object> previousValues = null;
        List<ThreadLocalAccessor<?>> accessors = contextRegistry.getThreadLocalAccessors();
        for (String key : keys) {
            Object value = ((ContextAccessor<C, ?>) contextAccessor).readValue((C) sourceContext, key);
            if (value != null) {
                for (ThreadLocalAccessor<?> threadLocalAccessor : accessors) {
                    if (key.equals(threadLocalAccessor.key())) {
                        previousValues = DefaultContextSnapshot.setThreadLocal(key, value, threadLocalAccessor,
                                previousValues);
                        break;
                    }
                }
            }
            else if (clearMissing) {
                for (ThreadLocalAccessor<?> threadLocalAccessor : accessors) {
                    if (key.equals(threadLocalAccessor.key())) {
                        previousValues = DefaultContextSnapshot.clearThreadLocal(key, threadLocalAccessor,
                                previousValues);
                        break;
                    }
                }
            }
        }
        return DefaultContextSnapshot.DefaultScope.from(previousValues, contextRegistry);
    }

}
