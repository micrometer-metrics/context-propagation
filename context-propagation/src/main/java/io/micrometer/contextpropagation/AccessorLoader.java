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

import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Loads {@link ThreadLocalAccessor} and {@link ReactorContextAccessor}.
 */
final class AccessorLoader {

	private static final ServiceLoader<ThreadLocalAccessor> threadLocalAccessors = ServiceLoader.load(ThreadLocalAccessor.class);

	private static final ServiceLoader<ReactorContextAccessor> reactorAccessors = ServiceLoader.load(ReactorContextAccessor.class);

	static List<ThreadLocalAccessor> getThreadLocalAccessors() {
		return StreamSupport.stream(threadLocalAccessors.spliterator(), false).collect(Collectors.toList());
	}

	static List<ReactorContextAccessor> getReactorContextAccessor() {
		return StreamSupport.stream(reactorAccessors.spliterator(), false).collect(Collectors.toList());
	}

}
