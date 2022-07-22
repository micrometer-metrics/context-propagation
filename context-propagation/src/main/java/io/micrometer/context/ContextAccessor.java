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

import java.util.Map;
import java.util.function.Predicate;

/**
 * Contract to assist with access to an external, map-like context, such as the
 * Project Reactor {@code Context}, including the ability to read values from it
 * a {@link Map} and to write values to it from a {@link Map}.
 *
 * @param <READ> type of context for reading
 * @param <WRITE> type of context for writing
 *
 * @author Marcin Grzejszczak
 * @author Rossen Stoyanchev
 * @since 1.0.0
 */
public interface ContextAccessor<READ, WRITE> {

    /**
     * Whether this accessor can capture values from the given type of context.
     * @param contextType the type of external context
     */
    boolean canReadFrom(Class<?> contextType);

    /**
     * Read values from a source context into a {@link Map}.
     * @param sourceContext the context to read from; the context type should be
     * checked with {@link #canReadFrom(Class)} before this method is called
     * @param keyPredicate a predicate to decide which keys to read
     * @param readValues a map where to put read values
     */
    void readValues(READ sourceContext, Predicate<Object> keyPredicate, Map<Object, Object> readValues);

    /**
     * Read a single value from the source context.
     * @param sourceContext the context to read from; the context type should be
     * checked with {@link #canReadFrom(Class)} before this method is called
     * @param key the key to use to look up the context value
     * @return the value, if any
     */
    @Nullable
    <T> T readValue(READ sourceContext, Object key);

    /**
     * Whether this accessor can restore values to the given type of context.
     * @param contextType the type of external context
     */
    boolean canWriteTo(Class<?> contextType);

    /**
     * Write values from a {@link Map} to a target context.
     * @param valuesToWrite the values to write to the target context
     * @param targetContext the context to write to; the context type should be
     * checked with {@link #canWriteTo(Class)}  before this method is called
     * @return a context with the written values
     */
    WRITE writeValues(Map<Object, Object> valuesToWrite, WRITE targetContext);

}
