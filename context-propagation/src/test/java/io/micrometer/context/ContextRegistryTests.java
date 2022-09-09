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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Unit tests for {@link ContextRegistry}.
 *
 * @author Rossen Stoyanchev
 */
public class ContextRegistryTests {

    private final ContextRegistry registry = new ContextRegistry();


    @Test
    void should_reject_accessors_reading_and_writing_same_or_child_types() {
        TestContextAccessor contextAccessor = new TestContextAccessor();
        TestContextAccessor sameTypeContextAccessor = new TestContextAccessor();
        FixedReadHashMapWriterAccessor childTypeWriterAccessor =
                new FixedReadHashMapWriterAccessor();
        HashMapReaderFixedWriterAccessor childTypeReaderAccessor =
                new HashMapReaderFixedWriterAccessor();

        this.registry.registerContextAccessor(contextAccessor);
        assertThat(this.registry.getContextAccessors()).containsExactly(contextAccessor);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> this.registry.registerContextAccessor(sameTypeContextAccessor));

        assertThat(this.registry.getContextAccessors()).containsExactly(contextAccessor);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> this.registry.registerContextAccessor(childTypeWriterAccessor));

        assertThat(this.registry.getContextAccessors()).containsExactly(contextAccessor);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> this.registry.registerContextAccessor(childTypeReaderAccessor));

        assertThat(this.registry.getContextAccessors()).containsExactly(contextAccessor);
    }

    @Test
    void should_reject_accessors_reading_child_type_already_read_by_existing() {
        TestContextAccessor contextAccessor = new TestContextAccessor();
        HashMapReaderAccessor readChildTypeAccessor = new HashMapReaderAccessor();

        this.registry.registerContextAccessor(contextAccessor);
        assertThat(this.registry.getContextAccessors()).containsExactly(contextAccessor);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> this.registry.registerContextAccessor(readChildTypeAccessor));

        assertThat(this.registry.getContextAccessors()).containsExactly(contextAccessor);
    }

    @Test
    void should_reject_accessor_reading_parent_type_of_type_read_by_existing() {
        HashMapReaderAccessor contextAccessor = new HashMapReaderAccessor();
        TestContextAccessor readParentTypeAccessor = new TestContextAccessor();

        this.registry.registerContextAccessor(contextAccessor);
        assertThat(this.registry.getContextAccessors()).containsExactly(contextAccessor);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> this.registry.registerContextAccessor(readParentTypeAccessor));

        assertThat(this.registry.getContextAccessors()).containsExactly(contextAccessor);
    }

    @Test
    void should_reject_accessors_writing_child_type_already_read_by_existing() {
        TestContextAccessor contextAccessor = new TestContextAccessor();
        HashMapWriterAccessor writeChildTypeAccessor = new HashMapWriterAccessor();

        this.registry.registerContextAccessor(contextAccessor);
        assertThat(this.registry.getContextAccessors()).containsExactly(contextAccessor);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> this.registry.registerContextAccessor(writeChildTypeAccessor));

        assertThat(this.registry.getContextAccessors()).containsExactly(contextAccessor);
    }

    @Test
    void should_reject_accessor_writing_parent_type_of_type_read_by_existing() {
        HashMapWriterAccessor contextAccessor = new HashMapWriterAccessor();
        TestContextAccessor writeParentTypeAccessor = new TestContextAccessor();

        this.registry.registerContextAccessor(contextAccessor);
        assertThat(this.registry.getContextAccessors()).containsExactly(contextAccessor);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> this.registry.registerContextAccessor(writeParentTypeAccessor));

        assertThat(this.registry.getContextAccessors()).containsExactly(contextAccessor);
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
