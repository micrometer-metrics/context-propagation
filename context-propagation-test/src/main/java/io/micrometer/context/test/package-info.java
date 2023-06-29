/**
 * Copyright 2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
/**
 * This package provides means to validate your use of
 * {@link io.micrometer.context.ContextSnapshot} and
 * {@link io.micrometer.context.ContextSnapshotFactory}.
 * <ul>
 * <li>{@link io.micrometer.context.test.ScopedValue} allows accessing currently set
 * {@link String} value in Thread-local
 * {@link io.micrometer.context.test.ScopedValue.Scope scope}.
 * <li>{@link io.micrometer.context.test.ScopedValueThreadLocalAccessor} can be registered
 * in {@link io.micrometer.context.ContextRegistry} to allow capturing and restoring the
 * values with regards to their scope hierarchy.
 * </ul>
 */
@NonNullApi
package io.micrometer.context.test;

import io.micrometer.context.util.annotation.NonNullApi;
