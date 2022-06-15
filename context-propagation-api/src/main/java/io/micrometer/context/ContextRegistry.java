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


/**
 * Registry that provides access to, instances of {@link ContextAccessor} and
 * {@link ThreadLocalAccessor}.
 *
 * <p>A static instance is available via {@link #getInstance()} and intended to
 * expose all known accessors. A {@link ContextSnapshot} can narrow down which
 * accessors to use at a particular point.
 *
 * @author Rossen Stoyanchev
 * @since 1.0.0
 */
public class ContextRegistry {

    private static final ContextRegistry instance = new ContextRegistry();


    private final List<ContextAccessor<?, ?>> contextAccessors = new ArrayList<>();

    private final List<ThreadLocalAccessor<?>> threadLocalAccessors = new ArrayList<>();


    private final List<ContextAccessor<?, ?>> readOnlyContextAccessors =
            Collections.unmodifiableList(this.contextAccessors);

    private final List<ThreadLocalAccessor<?>> readOnlyThreadLocalAccessors =
            Collections.unmodifiableList(this.threadLocalAccessors);


    /**
     * Return a {@link ContextAccessor} that can read the given context.
     * @param context the context to read from
     */
    public ContextAccessor<?, ?> getContextAccessorForRead(Object context) {
        for (ContextAccessor<?, ?> accessor : this.contextAccessors) {
            if (accessor.canReadFrom(context.getClass())) {
                return accessor;
            }
        }
        throw new IllegalStateException("No ContextAccessor for context type: " + context.getClass());
    }

    /**
     * Return a {@link ContextAccessor} that can write the given context.
     * @param context the context to write to
     */
    public ContextAccessor<?, ?> getContextAccessorForWrite(Object context) {
        for (ContextAccessor<?, ?> accessor : this.contextAccessors) {
            if (accessor.canWriteTo(context.getClass())) {
                return accessor;
            }
        }
        throw new IllegalStateException("No ContextAccessor for context type: " + context.getClass());
    }

    /**
     * Return a read-only list of registered {@link ContextAccessor}s.
     */
    public List<ContextAccessor<?, ?>> getContextAccessors() {
        return this.readOnlyContextAccessors;
    }

    /**
     * Return a read-only list of registered {@link ThreadLocalAccessor}s.
     */
    public List<ThreadLocalAccessor<?>> getThreadLocalAccessors() {
        return this.readOnlyThreadLocalAccessors;
    }

    /**
     * Register a {@link ContextAccessor}.
     */
    public void registerContextAccessor(ContextAccessor<?, ?> accessor) {
        this.contextAccessors.add(accessor);
    }

    /**
     * Register a {@link ThreadLocalAccessor}.
     */
    public void registerThreadLocalAccessor(ThreadLocalAccessor<?> accessor) {
        this.threadLocalAccessors.add(accessor);
    }


    /**
     * Return a global {@link ContextRegistry} instance.
     */
    public static ContextRegistry getInstance() {
        return instance;
    }

}
