package io.micrometer.context;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class ClearingContextSnapshot extends HashMap<Object, Object>
    implements ContextSnapshot {

    private static final ClearingContextSnapshot emptyContextSnapshot = new ClearingContextSnapshot(
        new ContextRegistry());

    private final ContextRegistry contextRegistry;

    ClearingContextSnapshot(ContextRegistry contextRegistry) {
        this.contextRegistry = contextRegistry;
    }

    @Override
    public <C> C updateContext(C context) {
        return updateContextInternal(context, this);
    }

    @Override
    public <C> C updateContext(C context, Predicate<Object> keyPredicate) {
        if (!isEmpty()) {
            Map<Object, Object> valuesToWrite = new HashMap<>();
            this.forEach((key, value) -> {
                if (keyPredicate.test(key)) {
                    valuesToWrite.put(key, value);
                }
            });
            context = updateContextInternal(context, valuesToWrite);
        }
        return context;
    }

    @SuppressWarnings("unchecked")
    private <C> C updateContextInternal(C context, Map<Object, Object> valueContainer) {
        if (!isEmpty()) {
            ContextAccessor<?, ?> accessor = this.contextRegistry.getContextAccessorForWrite(context);
            context = ((ContextAccessor<?, C>) accessor).writeValues(valueContainer, context);
        }
        return context;
    }

    @Override
    public Scope setThreadLocals() {
        return setThreadLocals(key -> true);
    }

    @Override
    public Scope setThreadLocals(Predicate<Object> keyPredicate) {
        Map<Object, Object> previousValues = null;
        for (ThreadLocalAccessor<?> accessor : this.contextRegistry.getThreadLocalAccessors()) {
            Object key = accessor.key();
            if (keyPredicate.test(key) && this.containsKey(key)) {
                previousValues = setThreadLocal(key, get(key), accessor, previousValues);
            }
        }
        return DefaultScope.from(previousValues, this.contextRegistry);
    }

    @SuppressWarnings("unchecked")
    private static <V> Map<Object, Object> setThreadLocal(Object key, @Nullable V value,
        ThreadLocalAccessor<?> accessor,
        @Nullable Map<Object, Object> previousValues) {

        previousValues = (previousValues != null ? previousValues : new HashMap<>());
        previousValues.put(key, accessor.getValue());
        if (value != null) {
            ((ThreadLocalAccessor<V>) accessor).setValue(value);
        }
        else {
            accessor.reset();
        }

        return previousValues;
    }

    @SuppressWarnings("unchecked")
    static <C> Scope setAllThreadLocalsFrom(Object context, ContextRegistry registry) {
        ContextAccessor<?, ?> contextAccessor = registry.getContextAccessorForRead(context);
        Map<Object, Object> previousValues = null;
        for (ThreadLocalAccessor<?> threadLocalAccessor : registry.getThreadLocalAccessors()) {
            Object key = threadLocalAccessor.key();
            Object value = ((ContextAccessor<C, ?>) contextAccessor).readValue((C) context, key);
            previousValues = setThreadLocal(key, value, threadLocalAccessor, previousValues);
        }
        return DefaultScope.from(previousValues, registry);
    }

    @SuppressWarnings("unchecked")
    static <C> Scope setThreadLocalsFrom(Object context, ContextRegistry registry, String... keys) {
        if (keys == null || keys.length == 0) {
            throw new IllegalArgumentException("You must provide at least one key when setting thread locals");
        }
        ContextAccessor<?, ?> contextAccessor = registry.getContextAccessorForRead(context);
        Map<Object, Object> previousValues = null;
        for (String key : keys) {
            Object value = ((ContextAccessor<C, ?>) contextAccessor).readValue((C) context, key);
            if (value != null) {
                for (ThreadLocalAccessor<?> threadLocalAccessor : registry.getThreadLocalAccessors()) {
                    if (key.equals(threadLocalAccessor.key())) {
                        previousValues = setThreadLocal(key, value, threadLocalAccessor, previousValues);
                    }
                }
            }
        }
        return DefaultScope.from(previousValues, registry);
    }

    static ContextSnapshot captureAll(ContextRegistry contextRegistry, Predicate<Object> keyPredicate,
        Object... contexts) {

        ClearingContextSnapshot snapshot = captureFromThreadLocals(keyPredicate, contextRegistry);
        for (Object context : contexts) {
            snapshot = captureFromContext(keyPredicate, contextRegistry, snapshot, context);
        }
        return (snapshot != null ? snapshot : emptyContextSnapshot);
    }

    @Nullable
    private static ClearingContextSnapshot captureFromThreadLocals(Predicate<Object> keyPredicate,
        ContextRegistry contextRegistry) {

        ClearingContextSnapshot snapshot = null;
        for (ThreadLocalAccessor<?> accessor : contextRegistry.getThreadLocalAccessors()) {
            if (keyPredicate.test(accessor.key())) {
                Object value = accessor.getValue();
                if (value != null) {
                    snapshot = (snapshot != null ? snapshot : new ClearingContextSnapshot(contextRegistry));
                    snapshot.put(accessor.key(), value);
                }
            }
        }
        return snapshot;
    }

    @SuppressWarnings("unchecked")
    static ClearingContextSnapshot captureFromContext(Predicate<Object> keyPredicate,
        ContextRegistry contextRegistry,
        @Nullable ClearingContextSnapshot snapshot, Object... contexts) {

        for (Object context : contexts) {
            ContextAccessor<?, ?> accessor = contextRegistry.getContextAccessorForRead(context);
            snapshot = (snapshot != null ? snapshot : new ClearingContextSnapshot(contextRegistry));
            ((ContextAccessor<Object, ?>) accessor).readValues(context, keyPredicate, snapshot);
        }
        return (snapshot != null ? snapshot : emptyContextSnapshot);
    }

    @Override
    public String toString() {
        return "ClearingContextSnapshot" + super.toString();
    }

}
