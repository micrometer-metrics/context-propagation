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
     * Capture values from an external context holder and put them in the given
     * {@link ContextContainer}.
     *
     * @param view the external context to capture values from
     * @param container the container to put the values in
     */
    void captureValues(READ view, ContextContainer container);

    /**
     * Restore values from the given {@link ContextContainer} and put them in
     * the given external context holder.
     *
     * @param container the container to obtain values from
     * @param context the external context to put values in
     * @return the updated external context
     */
    WRITE restoreValues(ContextContainer container, WRITE context);

    /**
     * Whether this accessor can capture values from the given external context type.
     *
     * @param contextType the type of external context
     */
    boolean canCaptureFrom(Class<?> contextType);

    /**
     * Whether this accessor can restore values to the given external context type.
     *
     * @param contextType the type of external context
     */
    boolean canRestoreTo(Class<?> contextType);

}
