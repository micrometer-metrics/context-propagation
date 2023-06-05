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
 * Factory for creating {@link ContextSnapshot} objects and restoring {@link ThreadLocal}
 * values using a context object for which a {@link ContextAccessor} exists in the
 * {@link ContextRegistry}.
 *
 * @author Dariusz Jędrzejczyk
 * @since 1.0.3
 */
public interface ContextSnapshotFactory {

    /**
     * Capture values from {@link ThreadLocal} and from other context objects using all
     * accessors from a {@link ContextRegistry} instance.
     * <p>
     * Values captured multiple times are overridden in the snapshot by the order of
     * contexts given as arguments.
     * @param contexts context objects to extract values from
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
     * Read the values specified by keys from the given source context, and if found, use
     * them to set {@link ThreadLocal} values. If no keys are provided, all keys are used.
     * Essentially, a shortcut that bypasses the need to create of {@link ContextSnapshot}
     * first via {@link #captureFrom(Object...)}, followed by
     * {@link ContextSnapshot#setThreadLocals()}.
     * @param sourceContext the source context to read values from
     * @param keys the keys of the values to read. If none provided, all keys are
     * considered.
     * @param <C> the type of the target context
     * @return an object that can be used to reset {@link ThreadLocal} values at the end
     * of the context scope, either removing them or restoring their previous values, if
     * any.
     */
    <C> ContextSnapshot.Scope setThreadLocalsFrom(Object sourceContext, String... keys);

    /**
     * Creates a builder for configuring the factory.
     * @return an instance that provides defaults, that can be configured to provide to
     * the created {@link ContextSnapshotFactory}.
     */
    static Builder builder() {
        return new DefaultContextSnapshotFactory.Builder();
    }

    /**
     * Builder for {@link ContextSnapshotFactory} instances.
     */
    interface Builder {

        /**
         * Creates a new instance of {@link ContextSnapshotFactory}.
         * @return an instance configured by the values set on the builder
         */
        ContextSnapshotFactory build();

        /**
         * Determines whether to clear existing {@link ThreadLocal} values at the start of
         * a scope, if there are no corresponding values in the source
         * {@link ContextSnapshot} or context object.
         * @param shouldClear if {@code true}, values not present in the context object or
         * snapshot will be cleared at the start of a scope and later restored
         * @return this builder instance
         */
        Builder clearMissing(boolean shouldClear);

        /**
         * Configures the {@link ContextRegistry} to use by the created factory.
         * @param contextRegistry the registry to use
         * @return this builder instance
         */
        Builder contextRegistry(ContextRegistry contextRegistry);

        /**
         * Instructs the factory to use the given predicate to select matching keys when
         * capturing {@link ThreadLocal} values
         * @param captureKeyPredicate predicate used to select matching keys
         * @return this builder instance
         */
        Builder captureKeyPredicate(Predicate<Object> captureKeyPredicate);

    }

}
