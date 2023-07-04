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

    @Override
    public Object key() {
        return KEY;
    }

    @Override
    public ScopedValue getValue() {
        return ScopedValue.VALUE_IN_SCOPE.get();
    }

    @Override
    public void setValue(ScopedValue value) {
        value.open();
    }

    @Override
    public void setValue() {
        ScopedValue.nullValue().open();
    }

    @Override
    public void restore(ScopedValue previousValue) {
        ScopedValue current = ScopedValue.VALUE_IN_SCOPE.get();
        if (current != null) {
            ScopedValue.Scope currentScope = current.currentScope();
            if (currentScope == null || currentScope.parentScope == null
                    || currentScope.parentScope.scopedValue != previousValue) {
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
        ScopedValue current = ScopedValue.VALUE_IN_SCOPE.get();
        if (current != null) {
            ScopedValue.Scope currentScope = current.currentScope();
            if (currentScope == null) {
                throw new RuntimeException("Can't close current scope, as it is missing");
            }
            currentScope.close();
        }
        else {
            throw new RuntimeException("Restoring to previous scope, but current is missing.");
        }
    }

}
