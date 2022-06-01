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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Holds context values to be propagated different context environments along
 * with the accessors required to propagate to and from those environments.
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
class SimpleContextContainer implements ContextContainer {

    private final Map<String, Object> values = new ConcurrentHashMap<>();

    private final List<ThreadLocalAccessor> threadLocalAccessors;

    private final List<Predicate<Namespace>> predicates = new CopyOnWriteArrayList<>();

    SimpleContextContainer() {
        this.threadLocalAccessors = ThreadLocalAccessorLoader.getThreadLocalAccessors();
    }

    @Override
    public void captureValues(Object context) {
        List<ContextAccessor> contextAccessorsForSet = ContextAccessorLoader.getAccessorsToCapture(context);
        for (ContextAccessor contextAccessor : contextAccessorsForSet) {
            contextAccessor.captureValues(context, this);
        }
    }

    @Override
    public <T> T restoreValues(T context) {
        T mergedContext = context;
        List<ContextAccessor> contextAccessorsForGet = ContextAccessorLoader.getAccessorsToRestore(context);
        for (ContextAccessor contextAccessor : contextAccessorsForGet) {
            mergedContext = (T) contextAccessor.restoreValues(this, mergedContext);
        }
        return mergedContext;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> T saveTo(T context) {
        ContextContainerPropagator propagator = ContextContainerPropagatorLoader.getPropagatorToSave(context);
        return (T) propagator.save(this, context);
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
    public <T> T remove(String key) {
        return (T) this.values.remove(key);
    }

    @Override
    public ContextContainer captureThreadLocalValues() {
        this.threadLocalAccessors.forEach(accessor -> accessor.captureValues(this));
        return this;
    }

    @Override
    public ContextContainer captureThreadLocalValues(Predicate<Namespace> predicate) {
        this.predicates.add(predicate);
        this.threadLocalAccessors.stream().filter(t -> t.isApplicable(predicate)).forEach(accessor -> accessor.captureValues(this));
        return this;
    }

    @Override
    public Scope restoreThreadLocalValues() {
        List<ThreadLocalAccessor> accessors = this.threadLocalAccessors.stream().filter(t -> this.predicates.stream().allMatch(p -> p.test(t.getNamespace())))
                .collect(Collectors.toList());
        accessors.forEach(accessor -> accessor.restoreValues(this));
        return () -> {
            accessors.forEach(accessor -> accessor.resetValues(this));
            this.predicates.clear();
        };
    }

    @Override
    public boolean isNoOp() {
        return false;
    }

    /**
     * Restores the thread local values and runs the given action. No exception
     * catching takes place.
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
     * Restores the thread local values and runs the given action. No exception
     * catching takes place.
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

    @Override
    public String toString() {
        return "ContextContainer{" +
                "values=" + values +
                ", threadLocalAccessors=" + threadLocalAccessors +
                '}';
    }
}
