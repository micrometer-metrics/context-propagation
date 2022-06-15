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
 * Contract to assist with access to a {@link ThreadLocal} including the ability
 * to get, set, and reset it.
 *
 * @author Rossen Stoyanchev
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
public interface ThreadLocalAccessor<V> {

    /**
     * The key to associate with the ThreadLocal value. This is the key under
     * which the value is saved within a {@link ContextSnapshot} and the key
     * under which it is looked up.
     */
    Object key();

    /**
     * Return the {@link ThreadLocal} value, or {@code null} if not set.
     */
    V getValue();

    /**
     * Set the {@link ThreadLocal} value.
     * @param value the value to set
     */
    void setValue(V value);

    /**
     * Remove the {@link ThreadLocal} value.
     */
    void reset();

}
