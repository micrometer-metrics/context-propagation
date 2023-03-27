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
     * Return the current {@link ThreadLocal} value, or {@code null} if not set.
     */
    @Nullable
    V getValue();

    /**
     * Return the {@link ThreadLocal} value, or {@code null} if not set to which we should
     * revert after the whole processing has been finished (e.g. in imperative block of code
     * you had value X, then you switch to reactive where you can have mulitple other values.
     * Finally, after the reactive block has finished, you want to go back to X).
     */
    @Nullable
    default V getInitialValue() {
        return getValue();
    }

    /**
     * Set the {@link ThreadLocal} value.
     * @param value the value to set
     */
    void setValue(V value);

    /**
     * Remove the {@link ThreadLocal} value.
     */
    void reset();

    /**
     * Remove the current {@link ThreadLocal} value and set the previously stored one.
     * @param previousValue previous value to set
     * @since 1.0.1
     */
    default void restore(V previousValue) {
        setValue(previousValue);
    }

    /**
     * Remove the current {@link ThreadLocal} value and set the one initially stored one.
     * @param initialValue initial value to set
     * @since 1.0.4
     */
    default void restoreInitial(V initialValue) {
        setValue(initialValue);
    }

}
