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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Holds context values to be propagated different context environments along
 * with the accessors required to propagate to and from those environments.
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class DefaultContextContainer implements ContextContainer {

    private final Map<String, Object> values = new ConcurrentHashMap<>();

    private final List<ContextAccessor<?, ?>> contextAccessors;

    private final List<ThreadLocalAccessor> threadLocalAccessors;

    private final Map<Class, List<ContextAccessor>> contextAccessorCache = new ConcurrentHashMap<>();

    private final List<Predicate<Namespace>> predicates = new CopyOnWriteArrayList<>();

    public DefaultContextContainer(
            List<ContextAccessor<?, ?>> contextAccessors, List<ThreadLocalAccessor> threadLocalAccessors) {

        this.contextAccessors = Collections.unmodifiableList(new ArrayList<>(contextAccessors));
        this.threadLocalAccessors = Collections.unmodifiableList(new ArrayList<>(threadLocalAccessors));
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
    public void captureValues(Object context) {
        for (ContextAccessor accessor : getContextAccessors(context, true)) {
            accessor.captureValues(context, this);
        }
    }

    @Override
    public <T> T restoreValues(T context) {
        T mergedContext = context;
        for (ContextAccessor accessor : getContextAccessors(context, false)) {
            mergedContext = (T) accessor.restoreValues(this, mergedContext);
        }
        return mergedContext;
    }

    private List<ContextAccessor> getContextAccessors(Object context, boolean capture) {
        List<ContextAccessor> accessors = this.contextAccessorCache.get(context.getClass());
        if (accessors == null) {
            accessors = this.contextAccessors.stream()
                    .filter(capture ?
                            accessor -> accessor.canCaptureFrom(context.getClass()) :
                            accessor -> accessor.canRestoreTo(context.getClass()))
                    .collect(Collectors.toList());
            this.contextAccessorCache.put(context.getClass(), accessors);
        }
        return accessors;
    }

    @Override
    public ContextContainer captureThreadLocalValues() {
        this.threadLocalAccessors.forEach(accessor -> accessor.captureValues(this));
        return this;
    }

    @Override
    public ContextContainer captureThreadLocalValues(Predicate<Namespace> predicate) {
        this.predicates.add(predicate);
        for (ThreadLocalAccessor accessor : this.threadLocalAccessors) {
            if (accessor.isApplicable(predicate)) {
                accessor.captureValues(this);
            }
        }
        return this;
    }

    @Override
    public ContextScope restoreThreadLocalValues() {
        List<ThreadLocalAccessor> accessors = new ArrayList<>();
        for (ThreadLocalAccessor accessor : this.threadLocalAccessors) {
            if (this.predicates.stream().allMatch(predicate -> predicate.test(accessor.getNamespace()))) {
                accessors.add(accessor);
            }
        }
        accessors.forEach(accessor -> accessor.restoreValues(this));
        return () -> {
            accessors.forEach(accessor -> accessor.resetValues(this));
            this.predicates.clear();
        };
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> T saveTo(T context) {
        ContextContainerAdapter adapter = ContextContainerAdapterLoader.getAdapterToWrite(context);
        return (T) adapter.save(this, context);
    }

    @Override
    public String toString() {
        return "ContextContainer{" +
                "values=" + values +
                ", threadLocalAccessors=" + threadLocalAccessors +
                '}';
    }
}
