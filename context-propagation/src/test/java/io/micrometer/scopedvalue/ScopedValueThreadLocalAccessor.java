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
package io.micrometer.scopedvalue;

import io.micrometer.context.ThreadLocalAccessor;

/**
 * Accessor for {@link ScopedValue}.
 *
 * @author Dariusz Jędrzejczyk
 */
public class ScopedValueThreadLocalAccessor implements ThreadLocalAccessor<ScopedValue> {

    /**
     * The key used for registrations in {@link io.micrometer.context.ContextRegistry}.
     */
    public static final String KEY = "svtla";

    private final String key;

    public ScopedValueThreadLocalAccessor() {
        this.key = KEY;
    }

    public ScopedValueThreadLocalAccessor(String key) {
        this.key = key;
    }

    @Override
    public Object key() {
        return this.key;
    }

    @Override
    public ScopedValue getValue() {
        return ScopeHolder.currentValue();
    }

    @Override
    public void setValue(ScopedValue value) {
        Scope.open(value);
    }

    @Override
    public void setValue() {
        Scope.open(ScopedValue.nullValue());
    }

    @Override
    public void restore(ScopedValue previousValue) {
        Scope currentScope = ScopeHolder.get();
        if (currentScope != null) {
            if (currentScope.parentScope == null || currentScope.parentScope.scopedValue != previousValue) {
                throw new RuntimeException("Restoring to a different previous scope than expected!");
            }
            currentScope.close();
        }
        else {
            throw new RuntimeException("Restoring to previous scope, but current is missing.");
        }
    }

    @Override
    public void restore() {
        Scope currentScope = ScopeHolder.get();
        if (currentScope != null) {
            currentScope.close();
        }
        else {
            throw new RuntimeException("Restoring to previous scope, but current is missing.");
        }
    }

}
