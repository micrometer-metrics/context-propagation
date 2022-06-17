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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Default implementation of {@link ContextSnapshot}.
 *
 * @author Rossen Stoyanchev
 * @since 1.0.0
 */
final class DefaultContextSnapshot implements ContextSnapshot {

    private final Map<Object, Object> values;

    private final ContextRegistry accessorRegistry;


    DefaultContextSnapshot(Map<Object, Object> values, ContextRegistry accessorRegistry) {
        this.values = values;
        this.accessorRegistry = accessorRegistry;
    }


    @Override
    public <C> C updateContext(C context) {
        return updateContextInternal(context, this.values);
    }

    @Override
    public <C> C updateContext(C context, Predicate<Object> keyPredicate) {
        if (!this.values.isEmpty()) {
            Map<Object, Object> valuesToWrite = new HashMap<>();
            this.values.forEach((key, value) -> {
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
        if (!this.values.isEmpty()) {
            ContextAccessor<?, ?> accessor = this.accessorRegistry.getContextAccessorForWrite(context);
            context = ((ContextAccessor<?, C>) accessor).writeValues(valueContainer, context);
        }
        return context;
    }

    @Override
    public Scope setThreadLocalValues() {
        return setThreadLocalValues(key -> true);
    }

    @Override
    public Scope setThreadLocalValues(Predicate<Object> keyPredicate) {
        Set<Object> keys = null;
        Map<Object, Object> previousValues = null;
        for (ThreadLocalAccessor<?> accessor : this.accessorRegistry.getThreadLocalAccessors()) {
            Object key = accessor.key();
            if (this.values.containsKey(key)) {
                keys = (keys != null ? keys : new HashSet<>());
                keys.add(key);

                Object previousValue = accessor.getValue();
                previousValues = (previousValues != null ? previousValues : new HashMap<>());
                previousValues.put(key, previousValue);

                setThreadLocalValue(key, accessor);
            }
        }
        return (keys != null ? new DefaultScope(keys, previousValues) : () -> { });
    }

    @SuppressWarnings("unchecked")
    private <V> void setThreadLocalValue(Object key, ThreadLocalAccessor<?> accessor) {
        ((ThreadLocalAccessor<V>) accessor).setValue((V) this.values.get(key));
    }

    @Override
    public String toString() {
        return "DefaultContextSnapshot" + this.values;
    }


    /**
     * Default implementation of {@link Scope}.
     */
    private class DefaultScope implements Scope {

        private final Set<Object> keys;

        private final Map<Object, Object> previousValues;

        private DefaultScope(Set<Object> keys, Map<Object, Object> previousValues) {
            this.keys = keys;
            this.previousValues = previousValues;
        }

        @Override
        public void close() {
            for (ThreadLocalAccessor<?> accessor : accessorRegistry.getThreadLocalAccessors()) {
                if (this.keys.contains(accessor.key())) {
                    Object previousValue = this.previousValues.get(accessor.key());
                    resetThreadLocalValue(accessor, previousValue);
                }
            }
        }

        @SuppressWarnings("unchecked")
        private <V> void resetThreadLocalValue(ThreadLocalAccessor<?> accessor, @Nullable V previousValue) {
            if (previousValue != null) {
                ((ThreadLocalAccessor<V>) accessor).setValue(previousValue);
            }
            else {
                accessor.reset();
            }
        }
    }

}
