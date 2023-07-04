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
 * Contract to assist with setting and clearing a {@link ThreadLocal}.
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
     * The key to associate with the ThreadLocal value when saved within a
     * {@link ContextSnapshot}.
     */
    Object key();

    /**
     * Return the current {@link ThreadLocal} value.
     * <p>
     * This method is called in two scenarios:
     * <ul>
     * <li>When capturing a {@link ContextSnapshot}. A {@code null} value is ignored and
     * the {@link #key()} will not be present in the snapshot.</li>
     * <li>When setting {@link ThreadLocal} values from a {@link ContextSnapshot} or from
     * a Context object (through a {@link ContextAccessor}) to save existing values in
     * order to {@link #restore(Object)} them at the end of the scope. A {@code null}
     * value means the {@link ThreadLocal} should not be set and upon closing a
     * {@link io.micrometer.context.ContextSnapshot.Scope}, the {@link #restore()} variant
     * is called.</li>
     * </ul>
     */
    @Nullable
    V getValue();

    /**
     * Set the {@link ThreadLocal} at the start of a
     * {@link io.micrometer.context.ContextSnapshot.Scope} to a value obtained from a
     * {@link ContextSnapshot} or from a different type of context (through a
     * {@link ContextAccessor}).
     * <p>
     * @param value the value to set, never {@code null} when called from a
     * {@link ContextSnapshot} implementation, which is not allowed to store mappings to
     * {@code null}.
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
     * Restore the {@link ThreadLocal} at the end of a
     * {@link io.micrometer.context.ContextSnapshot.Scope} to the previous value it had
     * before the start of the scope.
     * <p>
     * @param previousValue previous value to set, never {@code null} when called from a
     * {@link ContextSnapshot} * implementation, which is not allowed to store mappings to
     * {@code null}.
     * @since 1.0.1
     */
    default void restore(V previousValue) {
        setValue(previousValue);
    }

    /**
     * Called instead of {@link #restore(Object)} when there was no {@link ThreadLocal}
     * value existing at the start of the scope.
     * @see #getValue()
     * @since 1.0.3
     */
    default void restore() {
        setValue();
    }

}
