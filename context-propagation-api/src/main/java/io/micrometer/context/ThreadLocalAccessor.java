/*
 * Copyright 2002-2021 the original author or authors.
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

import java.util.ServiceLoader;
import java.util.function.Predicate;

/**
 * Contract to assist with extracting and restoring ThreadLocal values to and
 * from a {@link ContextContainer}. To register your own {@link ThreadLocalAccessor}
 * you have to register it using the {@link ServiceLoader} mechanism.
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
public interface ThreadLocalAccessor {

    /**
     * Capture ThreadLocal values and add them to the given container, so they
     * can be saved and subsequently {@link #restoreValues(ContextContainer)
     * restored} on a different thread.
     *
     * @param container {@link ContextContainer} to which we put the thread locals
     */
    void captureValues(ContextContainer container);

    /**
     * Restore ThreadLocal values from the given container.
     *
     * @param container {@link ContextContainer} for which we restore thread locals
     */
    void restoreValues(ContextContainer container);

    /**
     * Reset ThreadLocal values holders.
     *
     * @param container {@link ContextContainer} for which we reset thread locals
     */
    void resetValues(ContextContainer container);

    /**
     * Corresponding {@link Namespace}.
     *
     * @return corresponding {@link Namespace} or {@code null} if entries should go to the global namespace
     */
    default Namespace getNamespace() {
        return null;
    }

    /**
     * Used to filter out {@link ThreadLocalAccessor} by {@link Namespace}.
     *
     * @param predicate conditional logic for this accessor
     * @return {@code true} if this {@link ThreadLocalAccessor} should be applied
     */
    default boolean isApplicable(Predicate<Namespace> predicate) {
        if (getNamespace() == null) {
            return true;
        }
        return predicate.test(getNamespace());
    }

}
