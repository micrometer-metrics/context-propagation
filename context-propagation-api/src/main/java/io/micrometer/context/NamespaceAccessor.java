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

import java.util.function.Predicate;

/**
 * An accessor allowing to mutate the {@link ContextContainer} to
 * put and read the {@link Store} from it.
 *
 * @since 1.0.0
 */
public class NamespaceAccessor<T extends Store> {

    private final Namespace namespace;

    /**
     * Creates a new instance of {@link NamespaceAccessor}.
     *
     * @param namespace namespace to access
     */
    public NamespaceAccessor(Namespace namespace) {
        this.namespace = namespace;
    }

    /**
     * Puts store to the container.
     *
     * @param container container to which the namespace store should be placed
     * @param store store object to be put to the container
     */
    public void putStore(ContextContainer container, T store) {
        container.put(getNamespace().getKey(), store);
    }

    /**
     * Checks if namespace's {@link Store} is present in the container.
     *
     * @param container container in which the namespace {@link Store} might be stored
     * @return {@code true} when {@link Store} is in the container
     */
    public boolean isPresent(ContextContainer container) {
        return container.containsKey(getNamespace().getKey());
    }

    /**
     * Retrieves the {@link Store} from the container.
     *
     * @param container container where {@link Store} can be stored
     * @return corresponding {@link Store} or {@code null} when missing
     */
    public T getStore(ContextContainer container) {
        return container.get(getNamespace().getKey());
    }

    /**
     * Retrieves the {@link Store} from the container or throws an exception if there's no {@link Store}.
     *
     * @param container container where {@link Store} can be stored
     * @return corresponding {@link Store} or throws exception if missing
     */
    public T getRequiredStore(ContextContainer container) {
        T store = getStore(container);
        if (store == null) {
            throw new IllegalStateException("Store must be set");
        }
        return store;
    }

    /**
     * Checks whether this {@link Namespace} should be applied.
     *
     * @param namespacePredicate predicate to check whether this namespace should be applied
     * @return {@code} true when for the given predicate this namespace is applicable
     */
    public boolean isApplicable(Predicate<Namespace> namespacePredicate) {
        return namespacePredicate.test(getNamespace());
    }

    /**
     * Namespace to access.
     *
     * @return namespace
     */
    public Namespace getNamespace() {
        return this.namespace;
    }
}
