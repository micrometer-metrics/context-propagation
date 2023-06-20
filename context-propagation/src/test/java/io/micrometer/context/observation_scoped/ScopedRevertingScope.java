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

class ScopedRevertingScope implements Scope {

    private final Scope previousScope;

    private final ScopedObservation observation;

    ScopedRevertingScope(ScopedObservation observation) {
        this.previousScope = ScopedObservationScopeThreadLocalHolder.getValue();
        this.observation = observation;
        ScopedObservationScopeThreadLocalHolder.setValue(this);
    }

    @Override
    public void close() {
        ScopedObservationScopeThreadLocalHolder.setValue(this.previousScope);
    }

    @Override
    public Scope getPreviousObservationScope() {
        return this.previousScope;
    }

    @Override
    public ScopedObservation getCurrentObservation() {
        return this.observation;
    }

}
