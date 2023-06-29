/**
 * Copyright 2022 the original author or authors.
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
/**
 * This package contains abstractions and implementations for context propagation.
 * <ul>
 * <li>{@link io.micrometer.context.ThreadLocalAccessor} and
 * {@link io.micrometer.context.ContextAccessor} allow applications and frameworks to plug
 * in support for {@link java.lang.ThreadLocal} and other Map-like types of context such
 * as the Project Reactor Contexts.
 * <li>{@link io.micrometer.context.ContextRegistry} provides a static instance with
 * global access to all known accessors that should be registered on startup.
 * <li>{@link io.micrometer.context.ContextSnapshot} uses the {@code ContextRegistry} and
 * is used to capture context values, and then propagate them from one type of context to
 * another or from one thread to another.
 * </ul>
 */
@NonNullApi
@NonNullFields
package io.micrometer.context;

import io.micrometer.context.util.annotation.NonNullApi;
import io.micrometer.context.util.annotation.NonNullFields;
