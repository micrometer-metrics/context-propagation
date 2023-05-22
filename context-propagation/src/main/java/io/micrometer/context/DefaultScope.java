package io.micrometer.context;

import java.util.Map;

/**
 * Default implementation of {@link ContextSnapshot.Scope}.
 */
class DefaultScope implements ContextSnapshot.Scope {

    private final Map<Object, Object> previousValues;

    private final ContextRegistry contextRegistry;

    private DefaultScope(Map<Object, Object> previousValues,
        ContextRegistry contextRegistry) {
        this.previousValues = previousValues;
        this.contextRegistry = contextRegistry;
    }

    @Override
    public void close() {
        for (ThreadLocalAccessor<?> accessor : this.contextRegistry.getThreadLocalAccessors()) {
            if (this.previousValues.containsKey(accessor.key())) {
                Object previousValue = this.previousValues.get(accessor.key());
                resetThreadLocalValue(accessor, previousValue);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <V> void resetThreadLocalValue(ThreadLocalAccessor<?> accessor,
        @Nullable V previousValue) {
        if (previousValue != null) {
            ((ThreadLocalAccessor<V>) accessor).restore(previousValue);
        }
        else {
            accessor.reset();
        }
    }

    public static ContextSnapshot.Scope from(@Nullable Map<Object, Object> previousValues,
        ContextRegistry registry) {
        return (previousValues != null ? new DefaultScope(previousValues, registry) :
            () -> {
            });
    }

}
