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
 * A {@code Namespace} is used to provide a <em>scope</em> for data saved by
 * extensions within a {@link Store}. The key should be dot-separated for a given
 * family of keys. E.g.
 *
 * <ul>
 *  <li>for Micrometer Observation you should use {@code micrometer.observation}</li>
 *  <li>for Spring Data you should use {@code spring.data}</li>
 *  <li>for Hibernate Transaction you should use {@code hibernate.transaction}</li>
 * </ul>
 *
 * Then you can filter out whole family of keys e.g. don't propagate any spring related context.
 *
 * Inspired by JUnit Extension Namespace.
 *
 * @since 1.0.0
 */
public interface Namespace {

    /**
     * Key under which this namespace is registered.
     *
     * @return namespace key
     */
    default String getKey() {
        return getClass().getName();
    }

    /**
     * Creates a new instance of a {@link Namespace}.
     *
     * @param key key under which this namespace is to be registered
     * @return new namespace
     */
    static Namespace of(String key) {
        return new SimpleNamespace(key);
    }
}
