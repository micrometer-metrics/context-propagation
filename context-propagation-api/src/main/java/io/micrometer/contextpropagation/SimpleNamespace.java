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
package io.micrometer.contextpropagation;

import java.util.Objects;

/**
 * A {@code Namespace} is used to provide a <em>scope</em> for data saved by
 * extensions within a {@link Store}.
 *
 * <p>Storing data in custom namespaces allows extensions to avoid accidentally
 * mixing data between extensions or across different invocations within the
 * lifecycle of a single extension.
 *
 * Inspired by JUnit Extension Namespace and Store.
 *
 * @since 1.0.0
 */
class SimpleNamespace implements Namespace {

    private final String key;

    SimpleNamespace(String key) {
        this.key = key;
    }

    @Override
    public String getKey() {
        return this.key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Namespace)) {
            return false;
        }
        Namespace that = (Namespace) o;
        return Objects.equals(this.key, that.getKey());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.key);
    }

    @Override
    public String toString() {
        return this.key;
    }
}
