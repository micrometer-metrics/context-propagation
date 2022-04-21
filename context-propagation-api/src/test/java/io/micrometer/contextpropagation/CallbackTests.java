/*
 * Copyright 2002-2021 the original author or authors.
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

import java.util.Objects;

import io.micrometer.contextpropagation.ContextContainer.Scope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;

/**
 *
 * @author Tadaya Tsuyukubo
 */
class CallbackTests {
    
    @BeforeEach
    void reset() {
        ObservationThreadLocalHolder.holder.remove();
    }

    @Test
    void should_update_thread_local_via_callback() {
        ObservationThreadLocalHolder.holder.set("Hello");

        ContextContainer.ThreadLocalAccessorScopeCallback callback = new ContextContainer.ThreadLocalAccessorScopeCallback() {
            @Override
            public void beforeEachAccessor(ThreadLocalAccessor accessor, ContextContainer container) {
                // populate threadlocal
                accessor.captureValues(container);
            }

            @Override
            public void afterEachAccessor(ThreadLocalAccessor accessor, ContextContainer container) {
                // instead of clear, update with current threadlocal values. (Update scenario)
                accessor.captureValues(container);
            }
        };

        ContextContainer container = ContextContainer.create();
        try (Scope scope = container.withScope(callback)) {
            // some business logic
            String greeting = ObservationThreadLocalHolder.holder.get();
            then(greeting).isEqualTo("Hello");

            ObservationThreadLocalHolder.holder.set("Hi");  // update
        }

        String result = ObservationThreadLocalHolder.holder.get();
        then(result).isEqualTo("Hi");

        try (Scope scope = container.withScope(Objects::nonNull, callback)) {
            // some business logic
            String greeting = ObservationThreadLocalHolder.holder.get();
            then(greeting).isEqualTo("Hi");

            ObservationThreadLocalHolder.holder.set("Hi2");  // update
        }

        result = ObservationThreadLocalHolder.holder.get();
        then(result).isEqualTo("Hi2");
    }

    @Test
    void should_update_thread_local_via_runnable() {
        ObservationThreadLocalHolder.holder.set("Hello");

        // framework logic - describe how to handle the values
        ContextContainer.ThreadLocalAccessorScopeRunnable callback = (accessors, container, action) -> {
            // restore to threadlocal
            accessors.forEach(accessor -> accessor.restoreValues(container));

            // perform user's action
            action.run();

            // after user logic, capture the threadlocal rather than reset (Update)
            accessors.forEach(accessor -> accessor.captureValues(container));
        };

        // user defined action
        Runnable action = () -> {
            String greeting = ObservationThreadLocalHolder.holder.get();
            then(greeting).isEqualTo("Hello");

            ObservationThreadLocalHolder.holder.set("Hi");  // update
        };

        ContextContainer contextContainer = ContextContainer.create();
        contextContainer.tryScoped(callback, action);

        String greeting = ObservationThreadLocalHolder.holder.get();
        then(greeting).isEqualTo("Hi");
    }

    @Test
    void should_update_thread_local_via_runnable_with_predicate() {
        ObservationThreadLocalHolder.holder.set("Hello");

        // framework logic - describe how to handle the values
        ContextContainer.ThreadLocalAccessorScopeRunnable callback = (accessors, container, action) -> {
            // restore to threadlocal
            accessors.forEach(accessor -> accessor.restoreValues(container));

            // perform user's action
            action.run();

            // after user logic, capture the threadlocal rather than reset (Update)
            accessors.forEach(accessor -> accessor.captureValues(container));
        };

        // user defined action
        Runnable action = () -> {
            String greeting = ObservationThreadLocalHolder.holder.get();
            then(greeting).isEqualTo("Hello");

            ObservationThreadLocalHolder.holder.set("Hi");  // update
        };

        ContextContainer contextContainer = ContextContainer.create();
        contextContainer.tryScoped(Objects::nonNull, callback, action);

        String greeting = ObservationThreadLocalHolder.holder.get();
        then(greeting).isEqualTo("Hi");
    }

}
