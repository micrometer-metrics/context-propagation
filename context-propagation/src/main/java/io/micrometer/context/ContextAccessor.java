/**
 * Copyright 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
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
 * Contract to assist with access to an external, map-like context, such as the Project
 * Reactor {@code Context}, including the ability to read values from it a {@link Map} and
 * to write values to it from a {@link Map}.
 *
 * <p>
 * Implementations are disallowed to read {@code null} values into the given storage. This
 * requirement comes from the nature of {@link ThreadLocal}: if a value was not set, or
 * it's value is {@code null}, there is no way of distinguishing one from the other. In
 * such a case, the {@link ContextSnapshot} simply doesn't contain a capture fpr the
 * particular {@link ThreadLocal}. Due to this limitation, {@link ContextAccessor} should
 * not operate on {@code null} values. On the writing side, the implementations are safe
 * to assume the provided mappings do not contain mapping to {@code null}.
 *
 * @param <READ> type of context for reading
 * @param <WRITE> type of context for writing
 * @author Marcin Grzejszczak
 * @author Rossen Stoyanchev
 * @since 1.0.0
 */
public interface ContextAccessor<READ, WRITE> {

    /**
     * {@link Class} representing the type of context this accessor is capable of reading
     * values from.
     */
    Class<? extends READ> readableType();

    /**
     * Read values from a source context into a {@link Map}.
     * @param sourceContext the context to read from; the context type should be
     * {@link Class#isAssignableFrom(Class) assignable} from the type returned by
     * {@link #readableType()}.
     * <p>
     * It is forbidden to store {@code} values in the provided {@link Map}.
     * @param keyPredicate a predicate to decide which keys to read
     * @param readValues a map where to put read values
     */
    void readValues(READ sourceContext, Predicate<Object> keyPredicate, Map<Object, Object> readValues);

    /**
     * Read a single value from the source context.
     * @param sourceContext the context to read from; the context type should be
     * {@link Class#isAssignableFrom(Class) assignable} from the type returned by
     * {@link #readableType()}.
     * @param key the key to use to look up the context value
     * @return the value, if present, or {@code null} when not
     */
    @Nullable
    <T> T readValue(READ sourceContext, Object key);

    /**
     * {@link Class} representing the type of context this accessor can restore values to.
     */
    Class<? extends WRITE> writeableType();

    /**
     * Write values from a {@link Map} to a target context.
     * @param valuesToWrite the values to write to the target context. Implementations can
     * assume there is no mapping to {@code null}.
     * @param targetContext the context to write to; the context type should be
     * {@link Class#isAssignableFrom(Class) assignable} from the type returned by
     * {@link #writeableType()}.
     * @return a context with the written values
     */
    WRITE writeValues(Map<Object, Object> valuesToWrite, WRITE targetContext);

}
