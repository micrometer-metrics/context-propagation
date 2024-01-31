/**
 * Copyright 2024 the original author or authors.
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
package io.micrometer.context.logging.slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.micrometer.context.ThreadLocalAccessor;
import org.slf4j.MDC;

/**
 * {@link ThreadLocalAccessor} for copying values for selected keys from the {@link MDC}
 * across {@link Thread} boundaries. <strong>For use only when no other
 * {@link ThreadLocalAccessor} interacts with the MDC</strong>, for instance tracing
 * libraries.
 */
public class SelectiveMdcThreadLocalAccessor implements ThreadLocalAccessor<Map<String, String>> {

    /**
     * Key under which this accessor is registered in
     * {@link io.micrometer.context.ContextRegistry}.
     */
    public static final String KEY = "cp.slf4j.selective";

    private final List<String> keys;

    /**
     * Create an instance of {@link SelectiveMdcThreadLocalAccessor}.
     * @param keys selected keys for which values from the {@link MDC} should be
     * propagated across {@link Thread} boundaries.
     * @since 1.1.1
     */
    public SelectiveMdcThreadLocalAccessor(List<String> keys) {
        this.keys = keys;
    }

    @Override
    public Object key() {
        return KEY;
    }

    @Override
    public Map<String, String> getValue() {
        Map<String, String> values = new HashMap<>(this.keys.size());
        for (String key : this.keys) {
            values.put(key, MDC.get(key));
        }
        return values;
    }

    @Override
    public void setValue(Map<String, String> value) {
        for (String key : this.keys) {
            String mdcValue = value.get(key);
            if (mdcValue != null) {
                MDC.put(key, mdcValue);
            }
        }
    }

    @Override
    public void setValue() {
        for (String key : keys) {
            MDC.remove(key);
        }
    }

}
