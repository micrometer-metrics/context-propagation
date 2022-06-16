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

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * Registry that provides access to, instances of {@link ContextAccessor} and
 * {@link ThreadLocalAccessor}.
 *
 * <p>A static instance is available via {@link #getInstance()}. It is intended
 * to be initialized on startup, and to be aware of all available accessors, as
 * many as possible. The means to control what context gets propagated is in
 * {@link ContextSnapshot}, which filters context values by key.
 *
 * @author Rossen Stoyanchev
 * @since 1.0.0
 */
public class ContextRegistry {

    private static final ContextRegistry instance = new ContextRegistry();


    private final List<ContextAccessor<?, ?>> contextAccessors = new CopyOnWriteArrayList<>();

    private final List<ThreadLocalAccessor<?>> threadLocalAccessors = new CopyOnWriteArrayList<>();


    private final List<ContextAccessor<?, ?>> readOnlyContextAccessors =
            Collections.unmodifiableList(this.contextAccessors);

    private final List<ThreadLocalAccessor<?>> readOnlyThreadLocalAccessors =
            Collections.unmodifiableList(this.threadLocalAccessors);


    /**
     * Register a {@link ContextAccessor}.
     */
    public ContextRegistry registerContextAccessor(ContextAccessor<?, ?> accessor) {
        this.contextAccessors.add(accessor);
        return this;
    }

    /**
     * Register a {@link ThreadLocalAccessor}.
     */
    public ContextRegistry registerThreadLocalAccessor(ThreadLocalAccessor<?> accessor) {
        this.threadLocalAccessors.add(accessor);
        return this;
    }

    /**
     * Find a {@link ContextAccessor} that can read the given context.
     * @param context the context to read from
     * @throws IllegalStateException if no match is found
     */
    public ContextAccessor<?, ?> getContextAccessorForRead(Object context) {
        for (ContextAccessor<?, ?> accessor : this.contextAccessors) {
            if (accessor.canReadFrom(context.getClass())) {
                return accessor;
            }
        }
        throw new IllegalStateException("No ContextAccessor for contextType: " + context.getClass());
    }

    /**
     * Return a {@link ContextAccessor} that can write the given context.
     * @param context the context to write to
     * @throws IllegalStateException if no match is found
     */
    public ContextAccessor<?, ?> getContextAccessorForWrite(Object context) {
        for (ContextAccessor<?, ?> accessor : this.contextAccessors) {
            if (accessor.canWriteTo(context.getClass())) {
                return accessor;
            }
        }
        throw new IllegalStateException("No ContextAccessor for contextType: " + context.getClass());
    }

    /**
     * Return a read-only list of registered {@link ContextAccessor}'s.
     */
    public List<ContextAccessor<?, ?>> getContextAccessors() {
        return this.readOnlyContextAccessors;
    }

    /**
     * Return a read-only list of registered {@link ThreadLocalAccessor}'s.
     */
    public List<ThreadLocalAccessor<?>> getThreadLocalAccessors() {
        return this.readOnlyThreadLocalAccessors;
    }

    @Override
    public String toString() {
        return "ContextRegistry{" +
                "contextAccessors=" + this.contextAccessors + ", " +
                "threadLocalAccessors=" + this.threadLocalAccessors + "}";
    }


    /**
     * Return a global {@link ContextRegistry} instance.
     * <p><strong>Note:</strong> The global instance should be initialized on
     * startup to ensure it has the ability to propagate to and from different
     * types of context throughout the application. The registry itself is not
     * intended to as a mechanism to control what gets propagated. It is in
     * {@link ContextSnapshot} where more fine-grained decisions can be made
     * about which context values to propagate.
     */
    public static ContextRegistry getInstance() {
        return instance;
    }

}
