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
package io.micrometer.context;

import java.util.Objects;

/**
 * ThreadLocalAccessor for testing purposes with a given key and {@link ThreadLocal}
 * instance.
 *
 * @author Rossen Stoyanchev
 */
class TestThreadLocalAccessor implements ThreadLocalAccessor<String> {

    private final String key;

    // Normally this wouldn't be a field in the accessor but ok for testing purposes
    private final ThreadLocal<String> threadLocal;

    TestThreadLocalAccessor(String key, ThreadLocal<String> threadLocal) {
        this.key = key;
        this.threadLocal = threadLocal;
    }

    @Override
    public Object key() {
        return this.key;
    }

    @Nullable
    @Override
    public String getValue() {
        return this.threadLocal.get();
    }

    @Override
    public void setValue(String value) {
        // ThreadLocalAccessor API is @NonNullApi by default
        // so we don't expect null here
        Objects.requireNonNull(value);
        this.threadLocal.set(value);
    }

    @Override
    public void setValue() {
        this.threadLocal.remove();
    }

}
