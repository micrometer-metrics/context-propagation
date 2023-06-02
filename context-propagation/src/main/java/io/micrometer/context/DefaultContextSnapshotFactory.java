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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Default implementation of {@link ContextSnapshotFactory}.
 *
 * @author Dariusz Jędrzejczyk
 * @since 1.0.4
 */
final class DefaultContextSnapshotFactory implements ContextSnapshotFactory {

    private static final DefaultContextSnapshot emptyContextSnapshot = new DefaultContextSnapshot(new ContextRegistry(),
            false);

    private static final Function<ContextRegistry, DefaultContextSnapshot> clearingEmptySnapshot = contextRegistry -> new DefaultContextSnapshot(
            contextRegistry, true);

    private final ContextRegistry contextRegistry;

    private final boolean clearMissing;

    private final Predicate<Object> captureKeyPredicate;

    public DefaultContextSnapshotFactory(ContextRegistry contextRegistry, boolean clearMissing,
            Predicate<Object> captureKeyPredicate) {
        this.contextRegistry = contextRegistry;
        this.clearMissing = clearMissing;
        this.captureKeyPredicate = captureKeyPredicate;
    }

    @Override
    public ContextSnapshot captureAll(Object... contexts) {
        return captureAll(contextRegistry, captureKeyPredicate, clearMissing, contexts);
    }

    static ContextSnapshot captureAll(ContextRegistry contextRegistry, Predicate<Object> keyPredicate,
            boolean clearMissing, Object... contexts) {

        DefaultContextSnapshot snapshot = captureFromThreadLocals(keyPredicate, clearMissing, contextRegistry);
        for (Object context : contexts) {
            snapshot = captureFromContext(keyPredicate, clearMissing, contextRegistry, snapshot, context);
        }
        return (snapshot != null ? snapshot
                : (clearMissing ? clearingEmptySnapshot.apply(contextRegistry) : emptyContextSnapshot));
    }

    @Nullable
    private static DefaultContextSnapshot captureFromThreadLocals(Predicate<Object> keyPredicate, boolean clearMissing,
            ContextRegistry contextRegistry) {

        DefaultContextSnapshot snapshot = null;
        for (ThreadLocalAccessor<?> accessor : contextRegistry.getThreadLocalAccessors()) {
            if (keyPredicate.test(accessor.key())) {
                Object value = accessor.getValue();
                if (value != null) {
                    snapshot = (snapshot != null ? snapshot
                            : new DefaultContextSnapshot(contextRegistry, clearMissing));
                    snapshot.put(accessor.key(), value);
                }
            }
        }
        return snapshot;
    }

    @Override
    public ContextSnapshot captureFrom(Object... contexts) {
        return captureFromContext(captureKeyPredicate, clearMissing, contextRegistry, null, contexts);
    }

    @SuppressWarnings("unchecked")
    static DefaultContextSnapshot captureFromContext(Predicate<Object> keyPredicate, boolean clearMissing,
            ContextRegistry contextRegistry, @Nullable DefaultContextSnapshot snapshot, Object... contexts) {

        for (Object context : contexts) {
            ContextAccessor<?, ?> accessor = contextRegistry.getContextAccessorForRead(context);
            snapshot = (snapshot != null ? snapshot : new DefaultContextSnapshot(contextRegistry, clearMissing));
            ((ContextAccessor<Object, ?>) accessor).readValues(context, keyPredicate, snapshot);
        }
        if (snapshot != null) {
            snapshot.values().removeIf(Objects::isNull);
        }
        return (snapshot != null ? snapshot
                : (clearMissing ? clearingEmptySnapshot.apply(contextRegistry) : emptyContextSnapshot));
    }

    @Override
    public <C> ContextSnapshot.Scope setThreadLocalsFrom(Object sourceContext, String... keys) {
        if (keys == null || keys.length == 0) {
            return setAllThreadLocalsFrom(sourceContext, contextRegistry, clearMissing);
        }
        else {
            return setThreadLocalsFrom(sourceContext, contextRegistry, clearMissing, keys);
        }
    }

    @SuppressWarnings("unchecked")
    static <C> ContextSnapshot.Scope setAllThreadLocalsFrom(Object sourceContext, ContextRegistry contextRegistry,
            boolean clearMissing) {
        ContextAccessor<?, ?> contextAccessor = contextRegistry.getContextAccessorForRead(sourceContext);
        Map<Object, Object> previousValues = null;
        for (ThreadLocalAccessor<?> threadLocalAccessor : contextRegistry.getThreadLocalAccessors()) {
            Object key = threadLocalAccessor.key();
            Object value = ((ContextAccessor<C, ?>) contextAccessor).readValue((C) sourceContext, key);
            if (value != null) {
                previousValues = DefaultContextSnapshot.setThreadLocal(key, value, threadLocalAccessor, previousValues);
            }
            else if (clearMissing) {
                previousValues = DefaultContextSnapshot.clearThreadLocal(key, threadLocalAccessor, previousValues);
            }
        }
        return DefaultContextSnapshot.DefaultScope.from(previousValues, contextRegistry);
    }

    @SuppressWarnings("unchecked")
    static <C> ContextSnapshot.Scope setThreadLocalsFrom(Object sourceContext, ContextRegistry contextRegistry,
            boolean clearMissing, String... keys) {
        if (keys == null || keys.length == 0) {
            throw new IllegalArgumentException("You must provide at least one key when setting thread locals");
        }
        ContextAccessor<?, ?> contextAccessor = contextRegistry.getContextAccessorForRead(sourceContext);
        Map<Object, Object> previousValues = null;
        List<ThreadLocalAccessor<?>> accessors = contextRegistry.getThreadLocalAccessors();
        for (String key : keys) {
            Object value = ((ContextAccessor<C, ?>) contextAccessor).readValue((C) sourceContext, key);
            if (value != null) {
                for (ThreadLocalAccessor<?> threadLocalAccessor : accessors) {
                    if (key.equals(threadLocalAccessor.key())) {
                        previousValues = DefaultContextSnapshot.setThreadLocal(key, value, threadLocalAccessor,
                                previousValues);
                        break;
                    }
                }
            }
            else if (clearMissing) {
                for (ThreadLocalAccessor<?> threadLocalAccessor : accessors) {
                    if (key.equals(threadLocalAccessor.key())) {
                        previousValues = DefaultContextSnapshot.clearThreadLocal(key, threadLocalAccessor,
                                previousValues);
                        break;
                    }
                }
            }
        }
        return DefaultContextSnapshot.DefaultScope.from(previousValues, contextRegistry);
    }

    static final class Builder implements ContextSnapshotFactory.Builder {

        private boolean clearMissing = false;

        private ContextRegistry contextRegistry = ContextRegistry.getInstance();

        private Predicate<Object> captureKeyPredicate = key -> true;

        public Builder() {
        }

        @Override
        public ContextSnapshotFactory build() {
            return new DefaultContextSnapshotFactory(contextRegistry, clearMissing, captureKeyPredicate);
        }

        @Override
        public ContextSnapshotFactory.Builder clearMissing(boolean shouldClear) {
            this.clearMissing = shouldClear;
            return this;
        }

        @Override
        public ContextSnapshotFactory.Builder contextRegistry(ContextRegistry contextRegistry) {
            this.contextRegistry = contextRegistry;
            return this;
        }

        @Override
        public ContextSnapshotFactory.Builder captureKeyPredicate(Predicate<Object> captureKeyPredicate) {
            this.captureKeyPredicate = captureKeyPredicate;
            return this;
        }

    }

}
