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
package io.micrometer.contextpropagation;

import java.util.Collections;
import java.util.function.Supplier;

/**
 * {@link ContextContainer} is a holder of values that are being propagated through
 * various contexts. A context can be e.g. a Reactor Context, Reactor Netty Channel etc.
 * The values can be e.g. MDC entries, Micrometer Observation etc.
 *
 * @since 1.0.0
 */
public interface ContextContainer {

    /**
     * A No-Op instance that does nothing. To be used instead of {@code null}.
     */
    ContextContainer NOOP = new ContextContainer() {
        @Override
        public void captureContext(Object context) {

        }

        @Override
        public <T> T restoreContext(T context) {
            return context;
        }

        @Override
        public <T> T get(String key) {
            return null;
        }

        @Override
        public boolean containsKey(String key) {
            return false;
        }

        @Override
        public <T> T put(String key, T value) {
            return value;
        }

        @Override
        public Object remove(String key) {
            return null;
        }

        @Override
        public ContextContainer captureThreadLocalValues() {
            return this;
        }

        @Override
        public Scope restoreThreadLocalValues() {
            return () -> {
            };
        }

        @Override
        public <T> T saveContainer(T context) {
            return context;
        }

        @Override
        public boolean isNoOp() {
            return true;
        }

        @Override
        public void tryScoped(Runnable action) {
            action.run();
        }

        @Override
        public <T> T tryScoped(Supplier<T> action) {
            return action.get();
        }
    };

    void captureContext(Object context);

    <T> T restoreContext(T context);

    @SuppressWarnings({"unchecked", "rawtypes"})
    static <T> ContextContainer restoreContainer(T bag) {
        ContextContainerPropagator contextContainerPropagator = PropagatorLoader.getPropagatorForGet(bag);
        return contextContainerPropagator.get(bag);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static <T> T resetContainer(T bag) {
        ContextContainerPropagator contextContainerPropagator = PropagatorLoader.getPropagatorForGet(bag);
        return (T) contextContainerPropagator.remove(bag);
    }

    /**
     * Create an instance with the registered {@link ThreadLocalAccessor} to use.
     */
    static ContextContainer create() {
        return new SimpleContextContainer();
    }

    <T> T get(String key);

    boolean containsKey(String key);

    <T> T put(String key, T value);

    Object remove(String key);

    ContextContainer captureThreadLocalValues();

    Scope restoreThreadLocalValues();

    <T> T saveContainer(T context);

    boolean isNoOp();

    void tryScoped(Runnable action);

    <T> T tryScoped(Supplier<T> action);

    /**
     * Demarcates the scope of restored ThreadLocal values.
     */
    interface Scope extends AutoCloseable {
        @Override
        void close();
    }
}
