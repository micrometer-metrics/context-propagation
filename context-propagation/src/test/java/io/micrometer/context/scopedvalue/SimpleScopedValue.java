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
 * Implementation of {@link ScopedValue} which maintains the hierarchy between parent
 * scope and an opened scope for this value.
 */
class SimpleScopedValue implements ScopedValue {

    private static final Logger log = Logger.getLogger(SimpleScopedValue.class.getName());

    private final String value;

    ThreadLocal<Scope> currentScope = new ThreadLocal<>();

    SimpleScopedValue(String value) {
        this.value = value;
    }

    @Override
    public String get() {
        return value;
    }

    @Override
    public Scope currentScope() {
        return currentScope.get();
    }

    @Override
    public Scope open() {
        Scope scope = new Scope(this);
        this.currentScope.set(scope);
        return scope;
    }

    @Override
    public void updateCurrentScope(Scope scope) {
        log.log(INFO, () -> String.format("%s update scope[%s] -> scope[%s]", value, currentScope.get().hashCode(),
                scope.hashCode()));
        this.currentScope.set(scope);
    }

}
