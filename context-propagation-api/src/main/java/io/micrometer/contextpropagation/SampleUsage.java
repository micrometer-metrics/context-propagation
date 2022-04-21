package io.micrometer.contextpropagation;

import io.micrometer.contextpropagation.ContextContainer.Scope;
import io.micrometer.contextpropagation.SimpleContextContainer.AccessorOperations;
import io.micrometer.contextpropagation.SimpleContextContainer.ThreadLocalAccessorsOperation;

/**
 *
 * @author Tadaya Tsuyukubo
 */
public class SampleUsage {

	public void sample() {
		ThreadLocal<String> threadLocal = ThreadLocal.withInitial(() -> "Hello");

		ThreadLocalAccessorsOperation callback = new ThreadLocalAccessorsOperation() {
			@Override
			public void beforeEach(ThreadLocalAccessor accessors, ContextContainer container) {
				// populate threadlocal
				accessors.resetValues(container);
			}

			@Override
			public void afterEach(ThreadLocalAccessor accessors, ContextContainer container) {
				// instead of clear, update with current threadlocal values. (Update scenario)
				accessors.captureValues(container);
			}
		};


		SimpleContextContainer container = new SimpleContextContainer();
		try (Scope scope = container.withThreadLocalAccessors(callback)) {
			// some business logic
			String greeting = threadLocal.get();
			threadLocal.set("Hi");  // update
		}
	}

	public void sample2() {
		ThreadLocal<String> threadLocal = ThreadLocal.withInitial(() -> "Hello");

		// framework logic - describe how to handle the values
		AccessorOperations callback = (accessors, container, action) -> {
			// restore to threadlocal
			accessors.forEach(accessor -> accessor.restoreValues(container));

			// perform user's action
			action.run();

			// after user logic, capture the threadlocal rather than reset (Update)
			accessors.forEach(accessor -> accessor.captureValues(container));
		};

		// user defined action
		Runnable action = () -> {
			String greeting = threadLocal.get();
			threadLocal.set("Hi");  // update
		};

		SimpleContextContainer contextContainer = new SimpleContextContainer();
		contextContainer.withThreadLocalAccessorsAndAction(callback, action);
	}

}
