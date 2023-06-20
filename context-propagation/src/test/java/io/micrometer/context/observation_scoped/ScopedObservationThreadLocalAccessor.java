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
package io.micrometer.context.observation_scoped;

import io.micrometer.context.ThreadLocalAccessor;

/**
 * Example {@link ThreadLocalAccessor} implementation.
 */
public class ScopedObservationThreadLocalAccessor implements ThreadLocalAccessor<ScopedObservation> {

    public static final String KEY = "micrometer.observation_scoped";

    @Override
    public Object key() {
        return KEY;
    }

    @Override
    public ScopedObservation getValue() {
        return ScopedObservationScopeThreadLocalHolder.getCurrentObservation();
    }

    @Override
    public void setValue(ScopedObservation value) {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public Scope setValueScoped(ScopedObservation value) {
        io.micrometer.context.observation_scoped.Scope scope = value.openScope();
        return scope::close;
    }

    @Override
    public Scope setValueScoped() {
        io.micrometer.context.observation_scoped.Scope scope = new ScopedNullObservation().openScope();
        return scope::close;
    }

}
