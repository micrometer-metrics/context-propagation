/*
 * Copyright 2002-2022 the original author or authors.
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
package io.micrometer.context;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Tests using a {@link ContextSnapshot} to propagate {@link ThreadLocal} values.
 *
 * @author Rossen Stoyanchev
 */
public class ContextSnapshotThreadLocalTests {

    private ContextSnapshot.Builder snapshotBuilder;

    
    @BeforeEach
    void setUp() {
        ContextRegistry registry = new ContextRegistry();
        registry.registerThreadLocalAccessor(new ObservationThreadLocalAccessor());
        this.snapshotBuilder = ContextSnapshot.builder(registry);
    }

    @AfterEach
    void tearDown() {
        ObservationThreadLocalHolder.reset();
    }


    @Test
    void should_propagate_thread_local_values() {
        then(ObservationThreadLocalHolder.getValue()).isNull();
        ObservationThreadLocalHolder.setValue("hello");

        ContextSnapshot snapshot = this.snapshotBuilder.build();

        ObservationThreadLocalHolder.reset();
        then(ObservationThreadLocalHolder.getValue()).isNull();

        try (ContextSnapshot.Scope scope = snapshot.setThreadLocalValues()) {
            then(ObservationThreadLocalHolder.getValue()).isEqualTo("hello");
        }

        then(ObservationThreadLocalHolder.getValue()).isNull();
    }

    @Test
    void should_not_fail_when_propagating_empty_thread_local_values() {
        then(ObservationThreadLocalHolder.getValue()).isNull();

        ContextSnapshot snapshot = this.snapshotBuilder.build();

        ObservationThreadLocalHolder.reset();
        then(ObservationThreadLocalHolder.getValue()).isNull();

        try (ContextSnapshot.Scope scope = snapshot.setThreadLocalValues()) {
            then(ObservationThreadLocalHolder.getValue()).isNull();
        }

        then(ObservationThreadLocalHolder.getValue()).isNull();
    }

    @Test
    void should_filter_out_thread_local_values() {
        then(ObservationThreadLocalHolder.getValue()).isNull();
        ObservationThreadLocalHolder.setValue("hello");

        ContextSnapshot snapshot = this.snapshotBuilder
                .filter(key -> !((String) key).startsWith("micrometer.observation"))
                .build();

        ObservationThreadLocalHolder.reset();
        then(ObservationThreadLocalHolder.getValue()).isNull();

        try (ContextSnapshot.Scope scope = snapshot.setThreadLocalValues()) {
            then(ObservationThreadLocalHolder.getValue()).isNull();
        }

        then(ObservationThreadLocalHolder.getValue()).isNull();

    }


}
