package io.micrometer.contextpropagation;

import java.util.Collections;
import java.util.List;
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
			return () -> { };
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

		@Override
		public <A> void setAccessors(String key, List<A> accessors) {

		}

		@Override
		public <A> List<A> getAccessors(String key) {
			return Collections.emptyList();
		}
	};

	@SuppressWarnings("unchecked")
	static <T> ContextContainer restoreContainer(T bag) {
		ContextContainerPropagator contextContainerPropagator = PropagatorLoader.getPropagatorForGet(bag);
		return contextContainerPropagator.get(bag);
	}

	@SuppressWarnings("unchecked")
	static <T> T resetContainer(T bag) {
		ContextContainerPropagator contextContainerPropagator = PropagatorLoader.getPropagatorForGet(bag);
		return (T) contextContainerPropagator.remove(bag);
	}

	/**
	 * Create an instance with the registered {@link ThreadLocalAccessor} to use.
	 */
	static ContextContainer create() {
		return new SimpleContextContainer(AccessorLoader.getThreadLocalAccessors());
	}

	@SuppressWarnings("unchecked")
	<T> T get(String key);

	boolean containsKey(String key);

	@SuppressWarnings("unchecked")
	<T> T put(String key, T value);

	Object remove(String key);

	ContextContainer captureThreadLocalValues();

	Scope restoreThreadLocalValues();

	@SuppressWarnings("unchecked")
	<T> T saveContainer(T context);

	boolean isNoOp();

	void tryScoped(Runnable action);

	<T> T tryScoped(Supplier<T> action);

	<A> void setAccessors(String key, List<A> accessors);

	@SuppressWarnings("unchecked")
	<A> List<A> getAccessors(String key);

	/**
	 * Demarcates the scope of restored ThreadLocal values.
	 */
	interface Scope extends AutoCloseable {

		@Override
		void close();

	}
}
