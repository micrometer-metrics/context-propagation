/**
 * Copyright 2024-2025 the original author or authors.
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
package io.micrometer.context.integration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.micrometer.context.ThreadLocalAccessor;
import org.slf4j.MDC;

/**
 * {@link ThreadLocalAccessor} for copying contents of the {@link MDC} across
 * {@link Thread} boundaries. It can work with selected keys only or copy the entire
 * contents of the {@link MDC}. <strong>It is recommended to use only when no other
 * {@link ThreadLocalAccessor} interacts with the MDC or the selected keys</strong>, for
 * instance tracing libraries.
 *
 * @author Dariusz Jędrzejczyk
 * @since 1.1.1
 */
public class Slf4jThreadLocalAccessor implements ThreadLocalAccessor<Map<String, String>> {

    /**
     * Key under which this accessor is registered in
     * {@link io.micrometer.context.ContextRegistry}.
     */
    public static final String KEY = "cp.slf4j";

    private final ThreadLocalAccessor<Map<String, String>> delegate;

    /**
     * Create an instance of {@link Slf4jThreadLocalAccessor}.
     * @param keys selected keys for which values from the {@link MDC} should be
     * propagated across {@link Thread} boundaries. If none provided, the entire contents
     * of the {@link MDC} are propagated.
     */
    public Slf4jThreadLocalAccessor(String... keys) {
        this.delegate = keys.length == 0 ? new GlobalMdcThreadLocalAccessor()
                : new SelectiveMdcThreadLocalAccessor(Arrays.asList(keys));
    }

    @Override
    public Object key() {
        return KEY;
    }

    @Override
    public Map<String, String> getValue() {
        return delegate.getValue();
    }

    @Override
    public void setValue(Map<String, String> value) {
        delegate.setValue(value);
    }

    @Override
    public void setValue() {
        delegate.setValue();
    }

    private static final class GlobalMdcThreadLocalAccessor implements ThreadLocalAccessor<Map<String, String>> {

        @Override
        public Object key() {
            return KEY;
        }

        @Override
        public Map<String, String> getValue() {
            return MDC.getCopyOfContextMap();
        }

        @Override
        public void setValue(Map<String, String> value) {
            MDC.setContextMap(value);
        }

        @Override
        public void setValue() {
            MDC.clear();
        }

    }

    private static final class SelectiveMdcThreadLocalAccessor implements ThreadLocalAccessor<Map<String, String>> {

        private final List<String> keys;

        SelectiveMdcThreadLocalAccessor(List<String> keys) {
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
                else {
                    MDC.remove(key);
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

}
