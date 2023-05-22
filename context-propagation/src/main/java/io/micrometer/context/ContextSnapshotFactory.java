package io.micrometer.context;

import java.util.function.Predicate;

/**
 * @since 1.0.4
 */
public interface ContextSnapshotFactory {

    /**
     * Capture values from {@link ThreadLocal} and from other context objects using all
     * accessors from the {@link ContextRegistry#getInstance() global} ContextRegistry
     * instance.
     *
     * @param contexts one more context objects to extract values from
     * @return a snapshot with saved context values
     */
    ContextSnapshot captureAll(Object... contexts);

    /**
     * Capture values from {@link ThreadLocal} and from other context objects using all
     * accessors from the {@link ContextRegistry#getInstance() global} ContextRegistry
     * instance.
     *
     * @param registry the registry to use
     * @param contexts one more context objects to extract values from
     * @return a snapshot with saved context values
     */
    default ContextSnapshot captureAll(ContextRegistry registry, Object... contexts) {
        return captureAllUsing(key -> true, registry, contexts);
    }

    /**
     * Variant of {@link #captureAll(Object...)} with a predicate to filter context keys
     * and with a specific {@link ContextRegistry} instance.
     *
     * @param keyPredicate predicate for context value keys
     * @param registry     the registry with the accessors to use
     * @param contexts     one more context objects to extract values from
     * @return a snapshot with saved context values
     */
    ContextSnapshot captureAllUsing(Predicate<Object> keyPredicate,
        ContextRegistry registry,
        Object... contexts);

    /**
     * Create a {@link ContextSnapshot} by reading values from the given context objects.
     * <p>
     * Values captured multiple times are overridden in the snapshot by the order of
     * contexts given as arguments.
     *
     * @param contexts the contexts to read values from
     * @return the created {@link ContextSnapshot}
     */
    ContextSnapshot captureFromContext(Object... contexts);

    /**
     * Create a {@link ContextSnapshot} by reading values from the given context objects.
     * <p>
     * Values captured multiple times are overridden in the snapshot by the order of
     * contexts given as arguments.
     *
     * @param registry the registry to use
     * @param contexts the contexts to read values from
     * @return the created {@link ContextSnapshot}
     * @since 1.0.3
     */
    ContextSnapshot captureFromContext(ContextRegistry registry, Object... contexts);

    /**
     * Create a {@link ContextSnapshot} by reading values from the given context objects.
     * <p>
     * Values captured multiple times are overridden in the snapshot by the order of
     * contexts given as arguments.
     *
     * @param keyPredicate predicate for context value keys
     * @param registry     the registry to use
     * @param contexts     the contexts to read values from
     * @return the created {@link ContextSnapshot}
     */
    ContextSnapshot captureFromContext(Predicate<Object> keyPredicate,
        ContextRegistry registry,
        Object... contexts);

    /**
     * Variant of {@link #setThreadLocalsFrom(Object, String...)} that sets all
     * {@link ThreadLocal} values for which there is a value in the given source context.
     *
     * @param sourceContext the source context to read values from
     * @return an object that can be used to reset {@link ThreadLocal} values at the end
     * of the context scope, either removing them or restoring their previous values, if
     * any.
     */
    ContextSnapshot.Scope setAllThreadLocalsFrom(Object sourceContext);

    /**
     * Variant of {@link #setThreadLocalsFrom(Object, String...)} that sets all
     * {@link ThreadLocal} values for which there is a value in the given source context.
     *
     * @param sourceContext   the source context to read values from
     * @param contextRegistry the registry with the accessors to use
     * @return an object that can be used to reset {@link ThreadLocal} values at the end
     * of the context scope, either removing them or restoring their previous values, if
     * any.
     */
    ContextSnapshot.Scope setAllThreadLocalsFrom(Object sourceContext,
        ContextRegistry contextRegistry);

    /**
     * Read the values specified by from the given source context, and if found, use them
     * to set {@link ThreadLocal} values. Essentially, a shortcut that bypasses the need
     * to create of {@link ContextSnapshot} first via {@link #captureAll(Object...)},
     * followed by {@link #setThreadLocals()}.
     *
     * @param sourceContext the source context to read values from
     * @param keys          the keys of the values to read (at least one key must be passed)
     * @return an object that can be used to reset {@link ThreadLocal} values at the end
     * of the context scope, either removing them or restoring their previous values, if
     * any.
     */
    ContextSnapshot.Scope setThreadLocalsFrom(Object sourceContext, String... keys);

    /**
     * Variant of {@link #setThreadLocalsFrom(Object, String...)} with a specific
     * {@link ContextRegistry} instead of the global instance.
     *
     * @param sourceContext   the source context to read values from
     * @param contextRegistry the registry with the accessors to use
     * @param keys            the keys of the values to read (at least one key must be passed)
     * @return an object that can be used to reset {@link ThreadLocal} values at the end
     * of the context scope, either removing them or restoring their previous values, if
     * any.
     */
    ContextSnapshot.Scope setThreadLocalsFrom(Object sourceContext,
        ContextRegistry contextRegistry,
        String... keys);

    class Builder {

        private boolean clearWhenMissing = false;
        private ContextRegistry defaultRegistry = ContextRegistry.getInstance();

        public Builder() {

        }

        public ContextSnapshotFactory build() {
            if (clearWhenMissing) {
                return new ClearingContextSnapshot.Factory(defaultRegistry);
            } else {
                return new DefaultContextSnapshotFactory(defaultRegistry);
            }
        }

        public Builder clearWhenMissing(boolean shouldClear) {
            this.clearWhenMissing = shouldClear;
            return this;
        }

        public Builder defaultRegistry(ContextRegistry contextRegistry) {
            this.defaultRegistry = contextRegistry;
            return this;
        }
    }
}
