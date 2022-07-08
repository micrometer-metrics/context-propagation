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


import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ContextRegistry}.
 *
 * @author Rossen Stoyanchev
 */
public class ContextRegistryTests {

    private final ContextRegistry registry = new ContextRegistry();


    @Test
    void should_remove_existing_context_accessors_of_same_type() {
        TestContextAccessor contextAccessor1 = new TestContextAccessor();
        TestContextAccessor contextAccessor2 = new TestContextAccessor();

        this.registry.registerContextAccessor(contextAccessor1);
        assertThat(this.registry.getContextAccessors()).containsExactly(contextAccessor1);

        this.registry.registerContextAccessor(contextAccessor2);
        assertThat(this.registry.getContextAccessors()).containsExactly(contextAccessor2);
    }

    @Test
    void should_remove_existing_thread_local_accessors_for_same_key() {
        TestThreadLocalAccessor accessor1 = new TestThreadLocalAccessor("foo", new ThreadLocal<>());
        TestThreadLocalAccessor accessor2 = new TestThreadLocalAccessor("foo", new ThreadLocal<>());
        TestThreadLocalAccessor accessor3 = new TestThreadLocalAccessor("bar", new ThreadLocal<>());

        this.registry.registerThreadLocalAccessor(accessor1);
        assertThat(this.registry.getThreadLocalAccessors()).containsExactly(accessor1);

        this.registry.registerThreadLocalAccessor(accessor2);
        assertThat(this.registry.getThreadLocalAccessors()).containsExactly(accessor2);

        this.registry.registerThreadLocalAccessor(accessor3);
        assertThat(this.registry.getThreadLocalAccessors()).containsExactly(accessor2, accessor3);
    }

}
