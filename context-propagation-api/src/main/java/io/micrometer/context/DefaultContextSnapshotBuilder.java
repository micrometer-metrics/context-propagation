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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;


/**
 * Default implementation of {@link ContextSnapshot.Builder}.
 *
 * @author Rossen Stoyanchev
 * @since 1.0.0
 */
final class DefaultContextSnapshotBuilder implements ContextSnapshot.Builder {

    private static final ContextSnapshot emptyContextSnapshot =
            new DefaultContextSnapshot(Collections.emptyMap(), ContextRegistry.getInstance());


    private final ContextRegistry accessorRegistry;

    @Nullable
    private List<Object> keys;

    @Nullable
    private Predicate<Object> keyPredicate;


    DefaultContextSnapshotBuilder(ContextRegistry accessorRegistry) {
        this.accessorRegistry = accessorRegistry;
    }


    @Override
    public ContextSnapshot.Builder include(Object... keys) {
        this.keys = (this.keys != null ? this.keys : new ArrayList<>());
        this.keys.addAll(Arrays.asList(keys));
        return this;
    }

    @Override
    public ContextSnapshot.Builder filter(Predicate<Object> keyPredicate) {
        this.keyPredicate = (this.keyPredicate != null ? this.keyPredicate.and(keyPredicate) : keyPredicate);
        return this;
    }

    @Override
    public ContextSnapshot build(Object... contexts) {
        Predicate<Object> predicate = getPredicate();
        Map<Object, Object> valueContainer = null;
        for (ThreadLocalAccessor<?> accessor : this.accessorRegistry.getThreadLocalAccessors()) {
            if (predicate.test(accessor.key())) {
                Object value = accessor.getValue();
                if (value != null) {
                    valueContainer = (valueContainer != null ? valueContainer : new HashMap<>());
                    valueContainer.put(accessor.key(), value);
                }
            }
        }
        for (Object context : contexts) {
            ContextAccessor<?, ?> accessor = this.accessorRegistry.getContextAccessorForRead(context);
            valueContainer = (valueContainer != null ? valueContainer : new HashMap<>());
            addContextValues(accessor, context, predicate, valueContainer);
        }
        return (valueContainer != null && !valueContainer.isEmpty() ?
                new DefaultContextSnapshot(valueContainer, this.accessorRegistry) : emptyContextSnapshot);
    }

    private Predicate<Object> getPredicate() {
        if ((this.keys == null || this.keys.isEmpty()) && this.keyPredicate == null) {
            return key -> true;
        }
        Predicate<Object> predicate = null;
        if (this.keys != null && !this.keys.isEmpty()) {
            predicate = key -> this.keys.contains(key);
        }
        if (this.keyPredicate != null) {
            predicate = (predicate != null ? predicate.and(this.keyPredicate) : this.keyPredicate);
        }
        return predicate;
    }

    @SuppressWarnings("unchecked")
    private <R> void addContextValues(
            ContextAccessor<?, ?> accessor, Object context, Predicate<Object> predicate,
            Map<Object, Object> valueContainer) {

        ((ContextAccessor<R, ?>) accessor).readValues((R) context, predicate, valueContainer);
    }

}
