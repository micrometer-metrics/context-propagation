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
     * Variant of {@link #captureAll(Object...)} with a predicate to filter context keys
     * and with a specific {@link ContextRegistry} instance.
     * @param keyPredicate predicate for context value keys
     * @param contexts one more context objects to extract values from
     * @return a snapshot with saved context values
     */
    ContextSnapshot captureAllUsing(Predicate<Object> keyPredicate, Object... contexts);

    /**
     * Create a {@link ContextSnapshot} by reading values from the given context objects.
     * <p>
     * Values captured multiple times are overridden in the snapshot by the order of
     * contexts given as arguments.
     * @param contexts the contexts to read values from
     * @return the created {@link ContextSnapshot}
     */
    ContextSnapshot captureFromContext(Object... contexts);

    /**
     * Create a {@link ContextSnapshot} by reading values from the given context objects.
     * <p>
     * Values captured multiple times are overridden in the snapshot by the order of
     * contexts given as arguments.
     * @param keyPredicate predicate for context value keys
     * @param contexts the contexts to read values from
     * @return the created {@link ContextSnapshot}
     */
    ContextSnapshot captureFromContext(Predicate<Object> keyPredicate, Object... contexts);

    /**
     * Variant of {@link #setThreadLocalsFrom(Object, String...)} that sets all
     * {@link ThreadLocal} values for which there is a value in the given source context.
     * @param sourceContext the source context to read values from
     * @return an object that can be used to reset {@link ThreadLocal} values at the end
     * of the context scope, either removing them or restoring their previous values, if
     * any.
     */
    <C> ContextSnapshot.Scope setAllThreadLocalsFrom(Object sourceContext);

    /**
     * Read the values specified by from the given source context, and if found, use them
     * to set {@link ThreadLocal} values. Essentially, a shortcut that bypasses the need
     * to create of {@link ContextSnapshot} first via {@link #captureAll(Object...)},
     * followed by {@link ContextSnapshot#setThreadLocals()}.
     * @param sourceContext the source context to read values from
     * @param keys the keys of the values to read (at least one key must be passed)
     * @return an object that can be used to reset {@link ThreadLocal} values at the end
     * of the context scope, either removing them or restoring their previous values, if
     * any.
     */
    <C> ContextSnapshot.Scope setThreadLocalsFrom(Object sourceContext, String... keys);

    class Builder {

        private boolean clearMissing = false;

        private ContextRegistry defaultRegistry = ContextRegistry.getInstance();

        public Builder() {

        }

        public ContextSnapshotFactory build() {
            return new DefaultContextSnapshotFactory(defaultRegistry, clearMissing);
        }

        public Builder clearMissing(boolean shouldClear) {
            this.clearMissing = shouldClear;
            return this;
        }

        public Builder defaultRegistry(ContextRegistry contextRegistry) {
            this.defaultRegistry = contextRegistry;
            return this;
        }

    }

}
