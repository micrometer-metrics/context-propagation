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
 * @author Dariusz Jędrzejczyk
 * @since 1.0.0
 */
final class DefaultContextSnapshot extends HashMap<Object, Object> implements ContextSnapshot {

    private final ContextRegistry contextRegistry;

    private final boolean clearMissing;

    DefaultContextSnapshot(ContextRegistry contextRegistry, boolean clearMissing) {
        this.contextRegistry = contextRegistry;
        this.clearMissing = clearMissing;
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
            if (keyPredicate.test(key)) {
                if (this.containsKey(key)) {
                    Object value = get(key);
                    assert value != null : "snapshot contains disallowed null mapping for key: " + key;
                    previousValues = setThreadLocal(key, value, accessor, previousValues);
                }
                else if (clearMissing) {
                    previousValues = clearThreadLocal(key, accessor, previousValues);
                }
            }
        }
        return DefaultScope.from(previousValues, this.contextRegistry);
    }

    @SuppressWarnings("unchecked")
    static <V> Map<Object, Object> setThreadLocal(Object key, V value, ThreadLocalAccessor<?> accessor,
            @Nullable Map<Object, Object> previousValues) {

        previousValues = (previousValues != null ? previousValues : new HashMap<>());
        previousValues.put(key, accessor.getValue());
        ((ThreadLocalAccessor<V>) accessor).setValue(value);
        return previousValues;
    }

    @SuppressWarnings("unchecked")
    static <V> Map<Object, Object> clearThreadLocal(Object key, ThreadLocalAccessor<?> accessor,
            @Nullable Map<Object, Object> previousValues) {
        previousValues = (previousValues != null ? previousValues : new HashMap<>());
        previousValues.put(key, accessor.getValue());
        accessor.setValue();
        return previousValues;
    }

    @Override
    public String toString() {
        return "DefaultContextSnapshot" + super.toString();
    }

    /**
     * Default implementation of {@link Scope}.
     */
    static class DefaultScope implements Scope {

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
                accessor.restore();
            }
        }

        public static Scope from(@Nullable Map<Object, Object> previousValues, ContextRegistry registry) {
            return (previousValues != null ? new DefaultScope(previousValues, registry) : () -> {
            });
        }

    }

}
