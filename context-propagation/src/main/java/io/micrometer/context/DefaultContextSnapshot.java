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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Default implementation of {@link ContextSnapshot}.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 1.0.0
 */
final class DefaultContextSnapshot extends HashMap<Object, Object> implements ContextSnapshot {

    private static final ContextSnapshot emptyContextSnapshot = new DefaultContextSnapshot(new ContextRegistry());

    private final ContextRegistry contextRegistry;

    DefaultContextSnapshot(ContextRegistry contextRegistry) {
        this.contextRegistry = contextRegistry;
    }

    @Override
    public <C> C updateContext(C context) {
        return updateContextInternal(context, this);
    }

    @Override
    public <C> C updateContext(C context, Predicate<Object> keyPredicate) {
        if (!isEmpty()) {
            Map<Object, Object> valuesToWrite = new HashMap<>();
            this.forEach((key, value) -> {
                if (keyPredicate.test(key)) {
                    valuesToWrite.put(key, value);
                }
            });
            context = updateContextInternal(context, valuesToWrite);
        }
        return context;
    }

    @SuppressWarnings("unchecked")
    private <C> C updateContextInternal(C context, Map<Object, Object> valueContainer) {
        if (!isEmpty()) {
            ContextAccessor<?, ?> accessor = this.contextRegistry.getContextAccessorForWrite(context);
            context = ((ContextAccessor<?, C>) accessor).writeValues(valueContainer, context);
        }
        return context;
    }

    @Override
    public Scope setThreadLocals() {
        return setThreadLocals(key -> true);
    }

    @Override
    public Scope setThreadLocals(Predicate<Object> keyPredicate) {
        Map<Object, Object> previousValues = null;
        for (ThreadLocalAccessor<?> accessor : this.contextRegistry.getThreadLocalAccessors()) {
            Object key = accessor.key();
            if (keyPredicate.test(key) && this.containsKey(key)) {
                previousValues = setThreadLocal(key, get(key), accessor, previousValues);
            }
        }
        return DefaultScope.from(previousValues, this.contextRegistry);
    }

    @SuppressWarnings("unchecked")
    private static <V> Map<Object, Object> setThreadLocal(Object key, V value, ThreadLocalAccessor<?> accessor,
            @Nullable Map<Object, Object> previousValues) {

        previousValues = (previousValues != null ? previousValues : new HashMap<>());
        previousValues.put(key, accessor.getValue());
        ((ThreadLocalAccessor<V>) accessor).setValue(value);
        return previousValues;
    }

    @SuppressWarnings("unchecked")
    static <C> Scope setAllThreadLocalsFrom(Object context, ContextRegistry registry) {
        ContextAccessor<?, ?> contextAccessor = registry.getContextAccessorForRead(context);
        Map<Object, Object> previousValues = null;
        for (ThreadLocalAccessor<?> threadLocalAccessor : registry.getThreadLocalAccessors()) {
            Object key = threadLocalAccessor.key();
            Object value = ((ContextAccessor<C, ?>) contextAccessor).readValue((C) context, key);
            if (value != null) {
                previousValues = setThreadLocal(key, value, threadLocalAccessor, previousValues);
            }
        }
        return DefaultScope.from(previousValues, registry);
    }

    @SuppressWarnings("unchecked")
    static <C> Scope setThreadLocalsFrom(Object context, ContextRegistry registry, String... keys) {
        if (keys == null || keys.length == 0) {
            throw new IllegalArgumentException("You must provide at least one key when setting thread locals");
        }
        ContextAccessor<?, ?> contextAccessor = registry.getContextAccessorForRead(context);
        Map<Object, Object> previousValues = null;
        for (String key : keys) {
            Object value = ((ContextAccessor<C, ?>) contextAccessor).readValue((C) context, key);
            if (value != null) {
                for (ThreadLocalAccessor<?> threadLocalAccessor : registry.getThreadLocalAccessors()) {
                    if (key.equals(threadLocalAccessor.key())) {
                        previousValues = setThreadLocal(key, value, threadLocalAccessor, previousValues);
                    }
                }
            }
        }
        return DefaultScope.from(previousValues, registry);
    }

    static ContextSnapshot captureAll(ContextRegistry contextRegistry, Predicate<Object> keyPredicate,
            Object... contexts) {

        DefaultContextSnapshot snapshot = captureFromThreadLocals(keyPredicate, contextRegistry);
        for (Object context : contexts) {
            snapshot = captureFromContext(keyPredicate, contextRegistry, context, snapshot);
        }
        return (snapshot != null ? snapshot : emptyContextSnapshot);
    }

    @Nullable
    private static DefaultContextSnapshot captureFromThreadLocals(Predicate<Object> keyPredicate,
            ContextRegistry contextRegistry) {

        DefaultContextSnapshot snapshot = null;
        for (ThreadLocalAccessor<?> accessor : contextRegistry.getThreadLocalAccessors()) {
            if (keyPredicate.test(accessor.key())) {
                Object value = accessor.getValue();
                if (value != null) {
                    snapshot = (snapshot != null ? snapshot : new DefaultContextSnapshot(contextRegistry));
                    snapshot.put(accessor.key(), value);
                }
            }
        }
        return snapshot;
    }

    static ContextSnapshot captureFromMany(Predicate<Object> keyPredicate, ContextRegistry contextRegistry,
            Object... contexts) {
        DefaultContextSnapshot snapshot = null;
        for (Object context : contexts) {
            snapshot = captureFromContext(keyPredicate, contextRegistry, context, snapshot);
        }
        return (snapshot != null ? snapshot : emptyContextSnapshot);
    }

    @SuppressWarnings("unchecked")
    static DefaultContextSnapshot captureFromContext(Predicate<Object> keyPredicate, ContextRegistry contextRegistry,
            Object context, @Nullable DefaultContextSnapshot snapshot) {

        ContextAccessor<?, ?> accessor = contextRegistry.getContextAccessorForRead(context);
        snapshot = (snapshot != null ? snapshot : new DefaultContextSnapshot(contextRegistry));
        ((ContextAccessor<Object, ?>) accessor).readValues(context, keyPredicate, snapshot);
        return snapshot;
    }

    @Override
    public String toString() {
        return "DefaultContextSnapshot" + super.toString();
    }

    /**
     * Default implementation of {@link Scope}.
     */
    private static class DefaultScope implements Scope {

        private final Map<Object, Object> previousValues;

        private final ContextRegistry contextRegistry;

        private DefaultScope(Map<Object, Object> previousValues, ContextRegistry contextRegistry) {
            this.previousValues = previousValues;
            this.contextRegistry = contextRegistry;
        }

        @Override
        public void close() {
            for (ThreadLocalAccessor<?> accessor : this.contextRegistry.getThreadLocalAccessors()) {
                if (this.previousValues.containsKey(accessor.key())) {
                    Object previousValue = this.previousValues.get(accessor.key());
                    resetThreadLocalValue(accessor, previousValue);
                }
            }
        }

        @SuppressWarnings("unchecked")
        private <V> void resetThreadLocalValue(ThreadLocalAccessor<?> accessor, @Nullable V previousValue) {
            if (previousValue != null) {
                ((ThreadLocalAccessor<V>) accessor).restore(previousValue);
            }
            else {
                accessor.reset();
            }
        }

        public static Scope from(@Nullable Map<Object, Object> previousValues, ContextRegistry registry) {
            return (previousValues != null ? new DefaultScope(previousValues, registry) : () -> {
            });
        }

    }

}
