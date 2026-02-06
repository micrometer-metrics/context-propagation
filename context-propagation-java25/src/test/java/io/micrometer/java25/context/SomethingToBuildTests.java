/**
 * Copyright 2026 the original author or authors.
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

package io.micrometer.java25.context;

import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static io.micrometer.java25.context.SomethingToBuild.NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link SomethingToBuild}. Delete this as well as {@link SomethingToBuild}.
 */
class SomethingToBuildTests {

    @Test
    void boundScopedValueWithoutConcurrency() {
        ScopedValue.where(NAME, "42").run(() -> assertThat(NAME.get()).isEqualTo("42"));
        assertThatThrownBy(() -> NAME.get()).isInstanceOf(NoSuchElementException.class)
            .hasNoCause()
            .hasMessageContaining("ScopedValue not bound");
    }

}
