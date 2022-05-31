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
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
public interface ContextContainerPropagator<READ, WRITE> {

    /**
     * No-Op instance of the {@link ContextContainerPropagator}. Does nothing.
     */
    @SuppressWarnings("rawtypes")
    ContextContainerPropagator NOOP = new ContextContainerPropagator<Object, Object>() {
        @Override
        public Object save(ContextContainer contextContainer, Object context) {
            return context;
        }

        @Override
        public ContextContainer restore(Object context) {
            return ContextContainer.NOOP;
        }

        @Override
        public Object remove(Object ctx) {
            return ctx;
        }

        @Override
        public boolean supportsSaveTo(Class<?> contextType) {
            return false;
        }

        @Override
        public boolean supportsRestoreFrom(Class<?> contextType) {
            return false;
        }

    };

    /**
     * Save the {@link ContextContainer} to the external context.
     *
     * @param contextContainer value of the {@link ContextContainer} to write to the context
     * @param context   context to which we write
     * @return context
     */
    WRITE save(ContextContainer contextContainer, WRITE context);

    /**
     * Restore the {@link ContextContainer} from the external context. If the container
     * is not there the implementation should return {@link ContextContainer#NOOP}.
     *
     * @param context context from which we want to retrieve the {@link ContextContainer}
     * @return container
     */
    ContextContainer restore(READ context);

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
     * @return class type for which this propagator is applicable
     */
    boolean supportsSaveTo(Class<?> contextType);

    /**
     * Checks if this implementation can work with the provided context for reading.
     *
     * @return class type for which this propagator is applicable
     */
    boolean supportsRestoreFrom(Class<?> contextType);
}





