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

/**
 * Contract to assist with extracting and restoring context values to
 * and from a {@link ContextContainer}.
 *
 * @param <READ> type for context reading
 * @param <WRITE> type for context writing
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
public interface ContextAccessor<READ, WRITE> {

    /**
     * Capture values from a context and save them in the given container.
     *
     * @param view context from which we read
     * @param container container to which we put
     */
    void captureValues(READ view, ContextContainer container);

    /**
     * Restore context values from the given container.
     *
     * @param context context to which we write
     * @param container container from which we read
     * @return updated context
     */
    WRITE restoreValues(WRITE context, ContextContainer container);

    /**
     * Checks if this implementation can work with the provided context for writing.
     *
     * @return class type for which this propagator is applicable
     */
    Class<?> getSupportedContextClassForSet();

    /**
     * Checks if this implementation can work with the provided context for reading.
     *
     * @return class type for which this propagator is applicable
     */
    Class<?> getSupportedContextClassForGet();
}
