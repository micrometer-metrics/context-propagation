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
package io.micrometer.context.observation;

import java.util.logging.Level;
import java.util.logging.Logger;

import io.micrometer.context.ThreadLocalAccessor;

/**
 * Example {@link ThreadLocalAccessor} implementation.
 */
public class ObservationThreadLocalAccessor implements ThreadLocalAccessor<Observation> {

    private static final Logger log = Logger.getLogger(ObservationThreadLocalAccessor.class.getName());

    public static final String KEY = "micrometer.observation";

    @Override
    public Object key() {
        return KEY;
    }

    @Override
    public Observation getValue() {
        return ObservationScopeThreadLocalHolder.getCurrentObservation();
    }

    @Override
    public void setValue(Observation value) {
        value.openScope();
    }

    @Override
    public void resetToSetValue() {
        new NullObservation().openScope();
    }

    @Override
    public void restore(Observation value) {
        Scope scope = ObservationScopeThreadLocalHolder.getValue();
        if (scope == null) {
            String msg = "There is no current scope in thread local. This situation should not happen";
            log.log(Level.WARNING, msg);
            assert false : msg;
        }
        Scope previousObservationScope = scope.getPreviousObservationScope();
        if (previousObservationScope == null || value != previousObservationScope.getCurrentObservation()) {
            Observation previousObservation = previousObservationScope != null
                    ? previousObservationScope.getCurrentObservation() : null;
            String msg = "Observation <" + value
                    + "> to which we're restoring is not the same as the one set as this scope's parent observation <"
                    + previousObservation
                    + "> . Most likely a manually created Observation has a scope opened that was never closed. This may lead to thread polluting and memory leaks";
            log.log(Level.WARNING, msg);
            assert false : msg;
        }
        reset();
    }

    @Override
    public void resetToRestore() {
        reset();
    }

    @Override
    public void reset() {
        Scope scope = ObservationScopeThreadLocalHolder.getValue();
        if (scope != null) {
            scope.close();
        }
    }

}
