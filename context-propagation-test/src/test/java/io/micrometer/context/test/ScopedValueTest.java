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
package io.micrometer.context.test;

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
        assertThat(ScopedValue.getCurrent()).isNull();

        ScopedValue scopedValue = ScopedValue.create("hello");
        try (ScopedValue.Scope scope = scopedValue.open()) {
            assertThat(ScopedValue.getCurrent()).isEqualTo(scopedValue);
        }

        assertThat(ScopedValue.getCurrent()).isNull();
    }

    @Test
    void emptyScopeWorks() {
        assertThat(ScopedValue.getCurrent()).isNull();

        ScopedValue scopedValue = ScopedValue.create("hello");
        try (ScopedValue.Scope scope = scopedValue.open()) {
            assertThat(ScopedValue.getCurrent()).isEqualTo(scopedValue);
            try (ScopedValue.Scope emptyScope = ScopedValue.nullValue().open()) {
                assertThat(ScopedValue.getCurrent().get()).isNull();
            }
            assertThat(ScopedValue.getCurrent()).isEqualTo(scopedValue);
        }

        assertThat(ScopedValue.getCurrent()).isNull();
    }

    @Test
    void multiLevelScopesWithDifferentValues() {
        ScopedValue v1 = ScopedValue.create("val1");
        ScopedValue v2 = ScopedValue.create("val2");

        try (ScopedValue.Scope v1scope1 = v1.open()) {
            try (ScopedValue.Scope v1scope2 = v1.open()) {
                try (ScopedValue.Scope v2scope1 = v2.open()) {
                    try (ScopedValue.Scope v2scope2 = v2.open()) {
                        try (ScopedValue.Scope v1scope3 = v1.open()) {
                            try (ScopedValue.Scope nullScope = ScopedValue.nullValue().open()) {
                                assertThat(ScopedValue.getCurrent().get()).isNull();
                            }
                            assertThat(ScopedValue.getCurrent()).isEqualTo(v1);
                            assertThat(ScopedValue.getCurrent().currentScope()).isEqualTo(v1scope3);
                        }
                        assertThat(ScopedValue.getCurrent()).isEqualTo(v2);
                        assertThat(ScopedValue.getCurrent().currentScope()).isEqualTo(v2scope2);
                    }
                    assertThat(ScopedValue.getCurrent()).isEqualTo(v2);
                    assertThat(ScopedValue.getCurrent().currentScope()).isEqualTo(v2scope1);
                }
                assertThat(ScopedValue.getCurrent()).isEqualTo(v1);
                assertThat(ScopedValue.getCurrent().currentScope()).isEqualTo(v1scope2);
            }
            assertThat(ScopedValue.getCurrent()).isEqualTo(v1);
            assertThat(ScopedValue.getCurrent().currentScope()).isEqualTo(v1scope1);
        }

        assertThat(ScopedValue.getCurrent()).isNull();
    }

}
