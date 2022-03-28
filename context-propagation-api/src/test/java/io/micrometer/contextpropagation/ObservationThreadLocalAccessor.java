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

public class ObservationThreadLocalAccessor implements ThreadLocalAccessor {

    public static final String KEY = "micrometer.observation";

    public static final Namespace OBSERVATION = Namespace.create(KEY);

    private final NamespaceAccessor<ObservationStore> namespaceAccessor = new NamespaceAccessor<>(OBSERVATION);

    @Override
    public void captureValues(ContextContainer container) {
        String value = ObservationThreadLocalHolder.holder.get();
        if (value != null) {
            namespaceAccessor.putStore(container, new ObservationStore(value));
        }
    }

    @Override
    public void restoreValues(ContextContainer container) {
        if (namespaceAccessor.isPresent(container)) {
            ObservationStore store = namespaceAccessor.getStore(container);
            String observation = store.getObservation();
            ObservationThreadLocalHolder.holder.set(observation);
            System.out.println("Observation [" + observation + "]");
        }
    }

    @Override
    public void resetValues(ContextContainer container) {
        namespaceAccessor.getRequiredStore(container).close();
    }

    @Override
    public Namespace getNamespace() {
        return OBSERVATION;
    }

}

class ObservationStore implements Store, AutoCloseable {
    final String observation;

    ObservationStore(String observation) {
        this.observation = observation;
    }

    String getObservation() {
        return this.observation;
    }

    @Override
    public void close() {
        ObservationThreadLocalHolder.holder.remove();
    }
}

