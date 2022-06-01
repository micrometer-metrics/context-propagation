/*
 * Copyright 2019 VMware, Inc.
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
package io.micrometer.contextpropagation;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;

class SimpleThreadLocalAccessorTests {

    ContextContainer container = new DefaultContextContainer(
            Collections.emptyList(), Collections.singletonList(new ObservationThreadLocalAccessor()));

    @Test
    void should_capture_thread_local_values() {
        then(ObservationThreadLocalHolder.holder.get()).isNull();
        ObservationThreadLocalHolder.holder.set("hello");

        ContextContainer container = this.container.captureThreadLocalValues();

        ObservationThreadLocalHolder.holder.remove();
        then(ObservationThreadLocalHolder.holder.get()).isNull();

        try (ContextContainer.Scope scope = container.restoreThreadLocalValues()) {
            then(ObservationThreadLocalHolder.holder.get()).isEqualTo("hello");
        }

        then(ObservationThreadLocalHolder.holder.get()).isNull();
    }

    @Test
    void should_not_fail_when_capturing_empty_thread_local_values() {
        then(ObservationThreadLocalHolder.holder.get()).isNull();

        ContextContainer container = this.container.captureThreadLocalValues();

        ObservationThreadLocalHolder.holder.remove();
        then(ObservationThreadLocalHolder.holder.get()).isNull();

        try (ContextContainer.Scope scope = container.restoreThreadLocalValues()) {
            then(ObservationThreadLocalHolder.holder.get()).isNull();
        }

        then(ObservationThreadLocalHolder.holder.get()).isNull();
    }

    @Test
    void should_filter_out_thread_local_values() {
        then(ObservationThreadLocalHolder.holder.get()).isNull();
        ObservationThreadLocalHolder.holder.set("hello");

        ContextContainer container = this.container.captureThreadLocalValues(namespace -> !namespace.getKey().startsWith("micrometer.observation"));

        ObservationThreadLocalHolder.holder.remove();
        then(ObservationThreadLocalHolder.holder.get()).isNull();

        try (ContextContainer.Scope scope = container.restoreThreadLocalValues()) {
            then(ObservationThreadLocalHolder.holder.get()).isNull();
        }

        then(ObservationThreadLocalHolder.holder.get()).isNull();

    }

}
