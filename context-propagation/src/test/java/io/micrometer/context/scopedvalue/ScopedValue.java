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
package io.micrometer.context.scopedvalue;

import java.util.logging.Logger;

import static java.util.logging.Level.INFO;

/**
 * Serves as an abstraction of a value currently in scope.
 *
 * @author Dariusz Jędrzejczyk
 */
public interface ScopedValue {

    /**
     * Thread-local storage for the current value in scope for the current Thread.
     */
    ThreadLocal<ScopedValue> VALUE_IN_SCOPE = new ThreadLocal<>();

    /**
     * Shorthand for accessing the value in scope.
     * @return current {@link ScopedValue} set for this Thread.
     */
    static ScopedValue getCurrent() {
        return VALUE_IN_SCOPE.get();
    }

    /**
     * Creates a new instance, which can be set in scope via {@link #open()}.
     * @param value {@code String} value associated with created {@link ScopedValue}
     * @return new instance
     */
    static ScopedValue create(String value) {
        return new SimpleScopedValue(value);
    }

    /**
     * Creates a dummy instance used for nested scopes, in which the value should be
     * virtually absent, but allows reverting to the previous value in scope.
     * @return new instance representing an empty scope
     */
    static ScopedValue nullValue() {
        return new NullScopedValue();
    }

    /**
     * {@code String} value associated with this instance.
     * @return associated value
     */
    String get();

    /**
     * If this value was set in scope, returns the current {@link Scope} for the accessing
     * Thread. If not set, returns {@code null}.
     * @return current {@link Scope}
     */
    Scope currentScope();

    /**
     * Associates the value with the current scope. It allows re-using the same value in
     * multiple nested scopes.
     * @param scope current scope in which this value is set
     */
    void updateCurrentScope(Scope scope);

    /**
     * Create a new scope and set the value for this Thread.
     * @return newly created {@link Scope}
     */
    Scope open();

    /**
     * Represents a scope in which a {@link ScopedValue} is set for a particular Thread.
     */
    class Scope implements AutoCloseable {

        private static final Logger log = Logger.getLogger(Scope.class.getName());

        final ScopedValue scopedValue;

        final Scope parentScope;

        Scope(ScopedValue scopedValue) {
            log.log(INFO, () -> String.format("%s: open scope[%s]", scopedValue.get(), hashCode()));
            this.scopedValue = scopedValue;

            ScopedValue currentValue = ScopedValue.VALUE_IN_SCOPE.get();
            this.parentScope = currentValue != null ? currentValue.currentScope() : null;

            ScopedValue.VALUE_IN_SCOPE.set(scopedValue);
        }

        @Override
        public void close() {
            if (parentScope == null) {
                log.log(INFO, () -> String.format("%s: remove scope[%s]", scopedValue.get(), hashCode()));
                ScopedValue.VALUE_IN_SCOPE.remove();
            }
            else {
                log.log(INFO, () -> String.format("%s: close scope[%s] -> restore %s scope[%s]", scopedValue.get(),
                        hashCode(), parentScope.scopedValue.get(), parentScope.hashCode()));
                parentScope.scopedValue.updateCurrentScope(parentScope);
                ScopedValue.VALUE_IN_SCOPE.set(parentScope.scopedValue);
            }
        }

    }

}
