package io.micrometer.context;

import java.util.function.Predicate;

public class DefaultContextSnapshotFactory implements ContextSnapshot.Factory {

    public static final DefaultContextSnapshotFactory INSTANCE =
        new DefaultContextSnapshotFactory();

    private DefaultContextSnapshotFactory() {
        // no-op
    }

    @Override
    public ContextSnapshot captureAllUsing(Predicate<Object> keyPredicate, ContextRegistry registry,
        Object... contexts) {
        return DefaultContextSnapshot.captureAll(registry, keyPredicate, contexts);
    }

    @Override
    public ContextSnapshot captureFromContext(Object... contexts) {
        return DefaultContextSnapshot.captureFromContext(key -> true, ContextRegistry.getInstance(), null, contexts);
    }
    @Override
    public ContextSnapshot captureFromContext(ContextRegistry registry, Object... contexts) {
        return DefaultContextSnapshot.captureFromContext(key -> true, registry, null, contexts);
    }

    @Override
    public ContextSnapshot captureFromContext(Predicate<Object> keyPredicate, ContextRegistry registry,
        Object... contexts) {
        return DefaultContextSnapshot.captureFromContext(keyPredicate, registry, null, contexts);
    }

    @Override
    public ContextSnapshot.Scope setAllThreadLocalsFrom(Object sourceContext) {
        return DefaultContextSnapshot.setAllThreadLocalsFrom(sourceContext, ContextRegistry.getInstance());
    }

    @Override
    public ContextSnapshot.Scope setAllThreadLocalsFrom(Object sourceContext,
        ContextRegistry contextRegistry) {
        return DefaultContextSnapshot.setAllThreadLocalsFrom(sourceContext, contextRegistry);
    }

    @Override
    public ContextSnapshot.Scope setThreadLocalsFrom(Object sourceContext,
        ContextRegistry contextRegistry, String... keys) {
        return DefaultContextSnapshot.setThreadLocalsFrom(sourceContext, contextRegistry, keys);
    }
}
