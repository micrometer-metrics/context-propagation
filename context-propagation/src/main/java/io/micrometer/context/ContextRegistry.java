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

import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Registry that provides access to, instances of {@link ContextAccessor} and
 * {@link ThreadLocalAccessor}.
 *
 * <p>
 * A static instance is available via {@link #getInstance()}. It is intended to be
 * initialized on startup, and to be aware of all available accessors, as many as
 * possible. The means to control what context gets propagated is in
 * {@link ContextSnapshot}, which filters context values by key.
 *
 * @author Rossen Stoyanchev
 * @since 1.0.0
 */
public class ContextRegistry {

    private static final ContextRegistry instance = new ContextRegistry().loadContextAccessors()
        .loadThreadLocalAccessors();

    private final List<ContextAccessor<?, ?>> contextAccessors = new CopyOnWriteArrayList<>();

    private final List<ThreadLocalAccessor<?>> threadLocalAccessors = new CopyOnWriteArrayList<>();

    private final List<ContextAccessor<?, ?>> readOnlyContextAccessors = Collections
        .unmodifiableList(this.contextAccessors);

    private final List<ThreadLocalAccessor<?>> readOnlyThreadLocalAccessors = Collections
        .unmodifiableList(this.threadLocalAccessors);

    /**
     * Register a {@link ContextAccessor}. If there is an existing registration of another
     * {@code ContextAccessor} that can work with its declared types, an exception is
     * thrown.
     */
    public ContextRegistry registerContextAccessor(ContextAccessor<?, ?> accessor) {
        for (ContextAccessor<?, ?> existing : this.contextAccessors) {
            if (existing.readableType().isAssignableFrom(accessor.readableType())
                    || accessor.readableType().isAssignableFrom(existing.readableType())) {
                throw new IllegalArgumentException(
                        "Found an already registered accessor (" + existing.getClass().getCanonicalName() + ") reading "
                                + existing.readableType().getCanonicalName() + " when trying to add accessor ("
                                + accessor.getClass().getCanonicalName() + ") reading "
                                + accessor.readableType().getCanonicalName());
            }
            if (existing.writeableType().isAssignableFrom(accessor.writeableType())
                    || accessor.writeableType().isAssignableFrom(existing.writeableType())) {
                throw new IllegalArgumentException(
                        "Found an already registered accessor (" + existing.getClass().getCanonicalName() + ") writing "
                                + existing.writeableType().getCanonicalName() + " when trying to add accessor ("
                                + accessor.getClass().getCanonicalName() + ") writing "
                                + accessor.writeableType().getCanonicalName());
            }
        }
        this.contextAccessors.add(accessor);
        return this;
    }

    /**
     * Register a {@link ThreadLocalAccessor} for the given {@link ThreadLocal}.
     * @param key the {@link ThreadLocalAccessor#key() key} to associate with the
     * ThreadLocal value
     * @param threadLocal the underlying {@code ThreadLocal}
     * @return the same registry instance
     * @param <V> the type of value stored in the ThreadLocal
     */
    public <V> ContextRegistry registerThreadLocalAccessor(String key, ThreadLocal<V> threadLocal) {
        return registerThreadLocalAccessor(key, threadLocal::get, threadLocal::set, threadLocal::remove);
    }

    /**
     * Register a {@link ThreadLocalAccessor} from callbacks.
     * @param key the {@link ThreadLocalAccessor#key() key} to associate with the
     * ThreadLocal value
     * @param getSupplier callback to use for getting the value
     * @param setConsumer callback to use for setting the value
     * @param resetTask callback to use for resetting the value
     * @return the same registry instance
     * @param <V> the type of value stored in the ThreadLocal
     */
    public <V extends @Nullable Object> ContextRegistry registerThreadLocalAccessor(String key, Supplier<V> getSupplier,
            Consumer<V> setConsumer, Runnable resetTask) {

        return registerThreadLocalAccessor(new ThreadLocalAccessor<V>() {

            @Override
            public Object key() {
                return key;
            }

            @Override
            public V getValue() {
                return getSupplier.get();
            }

            @Override
            public void setValue(V value) {
                setConsumer.accept(value);
            }

            @Override
            public void setValue() {
                resetTask.run();
            }
        });
    }

    /**
     * Register a {@link ThreadLocalAccessor}. If there is an existing registration with
     * the same {@link ThreadLocalAccessor#key() key}, it is removed first.
     * @param accessor the accessor to register
     * @return the same registry instance
     */
    public ContextRegistry registerThreadLocalAccessor(ThreadLocalAccessor<?> accessor) {
        for (ThreadLocalAccessor<?> existing : this.threadLocalAccessors) {
            if (existing.key().equals(accessor.key())) {
                this.threadLocalAccessors.remove(existing);
                break;
            }
        }
        this.threadLocalAccessors.add(accessor);
        return this;
    }

    /**
     * Removes a {@link ThreadLocalAccessor}.
     * @param key under which the accessor got registered
     * @return {@code true} when accessor got successfully removed
     */
    public boolean removeThreadLocalAccessor(String key) {
        return removeThreadLocalAccessor((Object) key);
    }

    /**
     * Removes a {@link ThreadLocalAccessor}.
     * @param key under which the accessor got registered
     * @return {@code true} when accessor got successfully removed
     * @since 1.1.4
     */
    public boolean removeThreadLocalAccessor(Object key) {
        for (ThreadLocalAccessor<?> existing : this.threadLocalAccessors) {
            if (existing.key().equals(key)) {
                return this.threadLocalAccessors.remove(existing);
            }
        }
        return false;
    }

    /**
     * Removes a registered {@link ContextAccessor}.
     * @param accessorToRemove accessor instance to remove
     * @return {@code true} when accessor got successfully removed
     */
    public boolean removeContextAccessor(ContextAccessor<?, ?> accessorToRemove) {
        return this.contextAccessors.remove(accessorToRemove);
    }

    /**
     * Load {@link ContextAccessor} implementations through the {@link ServiceLoader}
     * mechanism.
     * <p>
     * Note that existing registrations of the same {@code ContextAccessor} type, if any,
     * are removed first.
     */
    public ContextRegistry loadContextAccessors() {
        ServiceLoader.load(ContextAccessor.class).forEach(this::registerContextAccessor);
        return this;
    }

    /**
     * Load {@link ThreadLocalAccessor} implementations through the {@link ServiceLoader}
     * mechanism.
     * <p>
     * Note that existing registrations with the same {@link ThreadLocalAccessor#key()
     * key}, if any, are removed first.
     */
    public ContextRegistry loadThreadLocalAccessors() {
        ServiceLoader.load(ThreadLocalAccessor.class).forEach(this::registerThreadLocalAccessor);
        return this;
    }

    /**
     * Find a {@link ContextAccessor} that can read the given context.
     * @param context the context to read from
     * @throws IllegalStateException if no match is found
     */
    public ContextAccessor<?, ?> getContextAccessorForRead(Object context) {
        for (ContextAccessor<?, ?> accessor : this.contextAccessors) {
            if (accessor.readableType().isAssignableFrom(context.getClass())) {
                return accessor;
            }
        }
        throw new IllegalStateException("No ContextAccessor for contextType: " + context.getClass());
    }

    /**
     * Return a {@link ContextAccessor} that can write the given context.
     * @param context the context to write to
     * @throws IllegalStateException if no match is found
     */
    public ContextAccessor<?, ?> getContextAccessorForWrite(Object context) {
        for (ContextAccessor<?, ?> accessor : this.contextAccessors) {
            if (accessor.writeableType().isAssignableFrom(context.getClass())) {
                return accessor;
            }
        }
        throw new IllegalStateException("No ContextAccessor for contextType: " + context.getClass());
    }

    /**
     * Return a read-only list of registered {@link ContextAccessor}'s.
     */
    public List<ContextAccessor<?, ?>> getContextAccessors() {
        return this.readOnlyContextAccessors;
    }

    /**
     * Return a read-only list of registered {@link ThreadLocalAccessor}'s.
     */
    public List<ThreadLocalAccessor<?>> getThreadLocalAccessors() {
        return this.readOnlyThreadLocalAccessors;
    }

    @Override
    public String toString() {
        return "ContextRegistry{" + "contextAccessors=" + this.contextAccessors + ", " + "threadLocalAccessors="
                + this.threadLocalAccessors + "}";
    }

    /**
     * Return a global {@link ContextRegistry} instance.
     * <p>
     * <strong>Note:</strong> The global instance should be initialized on startup to
     * ensure it has the ability to propagate to and from different types of context
     * throughout the application. The registry itself is not intended to as a mechanism
     * to control what gets propagated. It is in {@link ContextSnapshot} where more
     * fine-grained decisions can be made about which context values to propagate.
     */
    public static ContextRegistry getInstance() {
        return instance;
    }

}
