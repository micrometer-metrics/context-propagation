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

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Contract to assist with access to a {@link ThreadLocal} including the ability to get,
 * set, and reset it.
 *
 * @author Rossen Stoyanchev
 * @author Marcin Grzejszczak
 * @author Dariusz Jędrzejczyk
 * @since 1.0.0
 * @see ContextRegistry#registerThreadLocalAccessor(ThreadLocalAccessor)
 * @see ContextRegistry#registerThreadLocalAccessor(String, Supplier, Consumer, Runnable)
 */
public interface ThreadLocalAccessor<V> {

    /**
     * The key to associate with the ThreadLocal value. This is the key under which the
     * value is saved within a {@link ContextSnapshot} and the key under which it is
     * looked up.
     */
    Object key();

    /**
     * Return the current {@link ThreadLocal} value, or {@code null} if not set. This
     * method is called in two scenarios:
     * <ul>
     * <li>When capturing a {@link ContextSnapshot}. A {@code null} value would not end up
     * in the snapshot and would mean the snapshot is missing a mapping for this
     * accessor's {@link #key()}.</li>
     * <li>When setting {@link ThreadLocal} values from a {@link ContextSnapshot} or a
     * Context object (operated upon by {@link ContextAccessor}) to check for existing
     * values: {@code null} means the {@link ThreadLocal} is not set and upon closing a
     * {@link io.micrometer.context.ContextSnapshot.Scope}, the {@link #restore()} variant
     * with no argument would be called.</li>
     * </ul>
     */
    @Nullable
    V getValue();

    /**
     * Set the {@link ThreadLocal} value.
     * <p>
     * The argument will not be {@code null} when called from {@link ContextSnapshot}
     * implementations, which are disallowed to store mappings to {@code null}.
     * @param value the value to set
     */
    void setValue(V value);

    /**
     * Called instead of {@link #setValue(Object)} in order to remove the current
     * {@link ThreadLocal} value at the start of a
     * {@link io.micrometer.context.ContextSnapshot.Scope}.
     *
     * @since 1.0.3
     */
    default void setValue() {
        reset();
    }

    /**
     * Remove the {@link ThreadLocal} value when setting {@link ThreadLocal} values in
     * case of missing mapping for a {@link #key()} from a {@link ContextSnapshot}, or a
     * Context object (operated upon by {@link ContextAccessor}).
     * @deprecated To be replaced by calls to {@link #setValue()} (and/or
     * {@link #restore()}), which needs to be implemented when this implementation is
     * removed.
     */
    @Deprecated
    default void reset() {
        throw new IllegalStateException(this.getClass().getName() + "#reset() should "
                + "not be called. Please implement #setValue() method when removing the " + "#reset() implementation.");
    }

    /**
     * Remove the current {@link ThreadLocal} value and set the previously stored one.
     * <p>
     * The argument will not be {@code null} when called from {@link ContextSnapshot}
     * implementations, which are disallowed to store mappings to {@code null}.
     * @param previousValue previous value to set
     * @since 1.0.1
     */
    default void restore(V previousValue) {
        setValue(previousValue);
    }

    /**
     * Remove the current {@link ThreadLocal} value when restoring values after a
     * {@link io.micrometer.context.ContextSnapshot.Scope} closes but there was no
     * {@link ThreadLocal} value present prior to the closed scope.
     * @see #getValue()
     * @since 1.0.3
     */
    default void restore() {
        setValue();
    }

}
