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

import java.util.ServiceLoader;

/**
 * Abstraction to tell context propagation how to read from and write to
 * a given context. To register your own {@link ContextContainerPropagator}
 * you have to register it using the {@link ServiceLoader} mechanism.
 *
 * @param <READ> context from which we read
 * @param <WRITE> context to which we write
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
public interface ContextContainerPropagator<READ, WRITE> {

    @SuppressWarnings("rawtypes")
    ContextContainerPropagator NOOP = new ContextContainerPropagator() {
        @Override
        public Object set(Object ctx, ContextContainer value) {
            return ctx;
        }

        @Override
        public ContextContainer get(Object ctx) {
            return ContextContainer.NOOP;
        }

        @Override
        public Object remove(Object ctx) {
            return ctx;
        }

        @Override
        public boolean supportsContextForSet(Object context) {
            return false;
        }

        @Override
        public boolean supportsContextForGet(Object context) {
            return false;
        }
    };

    /**
     * Writes the {@link ContextContainer} to a context.
     *
     * @param ctx context to which we write
     * @param value value of the {@link ContextContainer} to write to the context
     * @return context
     */
    WRITE set(WRITE ctx, ContextContainer value);

    /**
     * Reads the {@link ContextContainer} from the context. If the container
     * is not there the implementation should return {@link ContextContainer#NOOP}.
     *
     * @param ctx context from which we want to retrieve the {@link ContextContainer}
     * @return container
     */
    ContextContainer get(READ ctx);

    /**
     * Removes the {@link ContextContainer} from the context.
     *
     * @param ctx context from which we are removing the {@link ContextContainer}
     * @return context
     */
    WRITE remove(WRITE ctx);

    /**
     * Checks if this implementation can work with the provided context for writing.
     *
     * @param context to write the {@link ContextContainer} to
     * @return {@code true} when this context is applicable for writing
     */
    boolean supportsContextForSet(Object context);

    /**
     * Checks if this implementation can work with the provided context for reading.
     *
     * @param context to retrieve the {@link ContextContainer} from
     * @return {@code true} when this context is applicable for reading
     */
    boolean supportsContextForGet(Object context);
}





