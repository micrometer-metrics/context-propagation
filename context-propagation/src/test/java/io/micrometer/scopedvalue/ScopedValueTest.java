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

import io.micrometer.scopedvalue.Scope;
import io.micrometer.scopedvalue.ScopedValue;
import io.micrometer.scopedvalue.ScopeHolder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ScopedValue}.
 *
 * @author Dariusz Jędrzejczyk
 */
class ScopedValueTest {

    @Test
    void basicScopeWorks() {
        assertThat(ScopeHolder.currentValue()).isNull();

        ScopedValue scopedValue = ScopedValue.create("hello");
        try (Scope scope = Scope.open(scopedValue)) {
            assertThat(ScopeHolder.currentValue()).isEqualTo(scopedValue);
        }

        assertThat(ScopeHolder.currentValue()).isNull();
    }

    @Test
    void emptyScopeWorks() {
        assertThat(ScopeHolder.currentValue()).isNull();

        ScopedValue scopedValue = ScopedValue.create("hello");
        try (Scope scope = Scope.open(scopedValue)) {
            assertThat(ScopeHolder.currentValue()).isEqualTo(scopedValue);
            try (Scope emptyScope = Scope.open(ScopedValue.nullValue())) {
                assertThat(ScopeHolder.currentValue().get()).isNull();
            }
            assertThat(ScopeHolder.currentValue()).isEqualTo(scopedValue);
        }

        assertThat(ScopeHolder.currentValue()).isNull();
    }

    @Test
    void multiLevelScopesWithDifferentValues() {
        ScopedValue v1 = ScopedValue.create("val1");
        ScopedValue v2 = ScopedValue.create("val2");

        try (Scope v1scope1 = Scope.open(v1)) {
            try (Scope v1scope2 = Scope.open(v1)) {
                try (Scope v2scope1 = Scope.open(v2)) {
                    try (Scope v2scope2 = Scope.open(v2)) {
                        try (Scope v1scope3 = Scope.open(v1)) {
                            try (Scope nullScope = Scope.open(ScopedValue.nullValue())) {
                                assertThat(ScopeHolder.currentValue().get()).isNull();
                            }
                            assertThat(ScopeHolder.currentValue()).isEqualTo(v1);
                            assertThat(ScopeHolder.get()).isEqualTo(v1scope3);
                        }
                        assertThat(ScopeHolder.currentValue()).isEqualTo(v2);
                        assertThat(ScopeHolder.get()).isEqualTo(v2scope2);
                    }
                    assertThat(ScopeHolder.currentValue()).isEqualTo(v2);
                    assertThat(ScopeHolder.get()).isEqualTo(v2scope1);
                }
                assertThat(ScopeHolder.currentValue()).isEqualTo(v1);
                assertThat(ScopeHolder.get()).isEqualTo(v1scope2);
            }
            assertThat(ScopeHolder.currentValue()).isEqualTo(v1);
            assertThat(ScopeHolder.get()).isEqualTo(v1scope1);
        }

        assertThat(ScopeHolder.currentValue()).isNull();
    }

}
