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
@SuppressWarnings({ "rawtypes", "unchecked" })
class SimpleContextContainer implements ContextContainer {

    private final Map<String, Object> values = new ConcurrentHashMap<>();

    private final List<ThreadLocalAccessor> threadLocalAccessors;

    SimpleContextContainer() {
        this.threadLocalAccessors = ThreadLocalAccessorLoader.getThreadLocalAccessors();
    }

    @Override
    public void captureContext(Object context) {
        List<ContextAccessor> contextAccessorsForSet = ContextAccessorLoader.getContextAccessorsForSet(context);
        for (ContextAccessor contextAccessor : contextAccessorsForSet) {
            contextAccessor.captureValues(context, this);
        }
    }

    @Override
    public <T> T restoreContext(T context) {
        T mergedContext = context;
        List<ContextAccessor> contextAccessorsForGet = ContextAccessorLoader.getContextAccessorsForGet(context);
        for (ContextAccessor contextAccessor : contextAccessorsForGet) {
            mergedContext = (T) contextAccessor.restoreValues(mergedContext, this);
        }
        return mergedContext;
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
    public <T> T save(T context) {
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
