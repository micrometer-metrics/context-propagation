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
package io.micrometer.contextpropagation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Holds context values to be propagated different context environments along
 * with the accessors required to propagate to and from those environments.
 *
 * @since 1.0.0
 */
class SimpleContextContainer implements ContextContainer {

    private final Map<String, Object> values = new ConcurrentHashMap<>();

    private final List<ThreadLocalAccessor> threadLocalAccessors;

    private final Map<String, List<?>> accessors = new ConcurrentHashMap<>(1);

    SimpleContextContainer(List<ThreadLocalAccessor> accessors) {
        this.threadLocalAccessors = new ArrayList<>(accessors);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) this.values.get(key);
    }

    @Override
    public boolean containsKey(String key) {
        return this.values.containsKey(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T put(String key, T value) {
        return (T) this.values.put(key, value);
    }

    @Override
    public Object remove(String key) {
        return this.values.remove(key);
    }

    @Override
    public <A> void setAccessors(String key, List<A> accessors) {
        this.accessors.put(key, accessors);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <A> List<A> getAccessors(String key) {
        return (List<A>) this.accessors.getOrDefault(key, Collections.emptyList());
    }

    @Override
    public ContextContainer captureThreadLocalValues() {
        this.threadLocalAccessors.forEach(accessor -> accessor.captureValues(this));
        return this;
    }

    @Override
    public Scope restoreThreadLocalValues() {
        this.threadLocalAccessors.forEach(accessor -> accessor.restoreValues(this));
        return () -> this.threadLocalAccessors.forEach(accessor -> accessor.resetValues(this));
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> T saveContainer(T context) {
        ContextContainerPropagator contextContainerPropagator = PropagatorLoader.getPropagatorForSet(context);
        return (T) contextContainerPropagator.set(context, this);
    }

    @Override
    public boolean isNoOp() {
        return false;
    }

    /**
     * Tries to run the action against an Observation. If the
     * Observation is null, we just run the action, otherwise
     * we run the action in scope.
     *
     * @param action action to run
     */
    @Override
    @SuppressWarnings("unused")
    public void tryScoped(Runnable action) {
        try (Scope scope = restoreThreadLocalValues()) {
            action.run();
        }
    }

    /**
     * Tries to run the action against an Observation. If the
     * Observation is null, we just run the action, otherwise
     * we run the action in scope.
     *
     * @param action action to run
     * @return result of the action
     */
    @Override
    @SuppressWarnings("unused")
    public <T> T tryScoped(Supplier<T> action) {
        try (Scope scope = restoreThreadLocalValues()) {
            return action.get();
        }
    }

}
