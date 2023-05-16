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
 * Example {@link ThreadLocalAccessor} implementation.
 */
public class ObservationThreadLocalAccessor implements ThreadLocalAccessor<String> {

    public static final String KEY = "micrometer.observation";

    @Override
    public Object key() {
        return KEY;
    }

    @Override
    public String getValue() {
        return ObservationThreadLocalHolder.getValue();
    }

    @Override
    public void setValue(String value) {
        // ThreadLocalAccessor API is @NonNullApi by default
        // so we don't expect null here
        Objects.requireNonNull(value);
        ObservationThreadLocalHolder.setValue(value);
    }

    @Override
    public void reset() {
        ObservationThreadLocalHolder.reset();
    }

    @Override
    public void restore(String previousValue) {
        // ThreadLocalAccessor API is @NonNullApi by default
        // so we don't expect null here
        Objects.requireNonNull(previousValue);
        setValue(previousValue);
    }
}
