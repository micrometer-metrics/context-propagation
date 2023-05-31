/**
 * Copyright 2023 the original author or authors.
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

import java.util.function.Predicate;

/**
 * @since 1.0.4
 */
public interface ContextSnapshotFactory {

    /**
     * Capture values from {@link ThreadLocal} and from other context objects using all
     * accessors from the {@link ContextRegistry#getInstance() global} ContextRegistry
     * instance.
     * @param contexts one more context objects to extract values from
     * @return a snapshot with saved context values
     */
    ContextSnapshot captureAll(Object... contexts);

    /**
     * Create a {@link ContextSnapshot} by reading values from the given context objects.
     * <p>
     * Values captured multiple times are overridden in the snapshot by the order of
     * contexts given as arguments.
     * @param contexts the contexts to read values from
     * @return the created {@link ContextSnapshot}
     */
    ContextSnapshot captureFrom(Object... contexts);

    /**
     * Read the values specified by from the given source context, and if found, use them
     * to set {@link ThreadLocal} values. Essentially, a shortcut that bypasses the need
     * to create of {@link ContextSnapshot} first via {@link #captureAll(Object...)},
     * followed by {@link ContextSnapshot#setThreadLocals()}.
     * @param sourceContext the source context to read values from
     * @param keys the keys of the values to read. If none provided, all keys are
     * considered.
     * @return an object that can be used to reset {@link ThreadLocal} values at the end
     * of the context scope, either removing them or restoring their previous values, if
     * any.
     */
    <C> ContextSnapshot.Scope setThreadLocalsFrom(Object sourceContext, String... keys);

    static Builder builder() {
        return new DefaultContextSnapshotFactory.Builder();
    }

    interface Builder {

        ContextSnapshotFactory build();

        Builder clearMissing(boolean shouldClear);

        Builder contextRegistry(ContextRegistry contextRegistry);

        Builder captureKeyPredicate(Predicate<Object> captureKeyPredicate);

    }

}
