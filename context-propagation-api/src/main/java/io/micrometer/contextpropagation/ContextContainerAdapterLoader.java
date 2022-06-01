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

import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads {@link ThreadLocalAccessor} and {@link ContextAccessor}.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
final class ContextContainerAdapterLoader {

    private static final ServiceLoader<ContextContainerAdapter> adapters = ServiceLoader.load(ContextContainerAdapter.class);

    private static final Map<Class, ContextContainerAdapter> cache = new ConcurrentHashMap<>();

    static ContextContainerAdapter getAdapterToWrite(Object context) {
        return cache.computeIfAbsent(context.getClass(), aClass -> {
            for (ContextContainerAdapter contextContainerAdapter : adapters) {
                if (contextContainerAdapter.supportsSaveTo(aClass)) {
                    return contextContainerAdapter;
                }
            }
            throw new IllegalStateException(
                    "No ContextContainerAdapter for context type: " + context.getClass());
        });
    }

    static ContextContainerAdapter getAdapterToRead(Object context) {
        return cache.computeIfAbsent(context.getClass(), aClass -> {
            for (ContextContainerAdapter contextContainerAdapter : adapters) {
                if (contextContainerAdapter.supportsRestoreFrom(aClass)) {
                    return contextContainerAdapter;
                }
            }
            throw new IllegalStateException(
                    "No ContextContainerAdapter for context type: " + context.getClass());
        });
    }
}
