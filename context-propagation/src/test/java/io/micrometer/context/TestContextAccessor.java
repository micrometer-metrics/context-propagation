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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * ThreadLocalAccessor for testing purposes with a given key and
 * {@link ThreadLocal} instance.
 *
 * @author Rossen Stoyanchev
 */
class TestContextAccessor implements ContextAccessor<Map<?, ?>, Map<?, ?>> {

    @Override
    public Class<?> readableType() {
        return Map.class;
    }

    @Override
    public void readValues(Map<?, ?> sourceContext, Predicate<Object> keyPredicate, Map<Object, Object> readValues) {
        readValues.putAll(sourceContext);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T> T readValue(Map<?, ?> sourceContext, Object key) {
        return (T) sourceContext.get(key);
    }

    @Override
    public Class<?> writeableType() {
        return Map.class;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<?, ?> writeValues(Map<Object, Object> valuesToWrite, Map<?, ?> targetContext) {
        ((Map<Object, Object>) targetContext).putAll(valuesToWrite);
        return targetContext;
    }
}

class HashMapReaderAccessor implements ContextAccessor<HashMap<?, ?>, Map<?, ?>> {

    @Override
    public Class<?> readableType() {
        return HashMap.class;
    }

    @Override
    public void readValues(HashMap<?, ?> sourceContext,
            Predicate<Object> keyPredicate,
            Map<Object, Object> readValues) {
        readValues.putAll(sourceContext);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T readValue(HashMap<?, ?> sourceContext, Object key) {
        return (T) sourceContext.get(key);
    }

    @Override
    public Class<?> writeableType() {
        return Map.class;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<?, ?> writeValues(Map<Object, Object> valuesToWrite,
            Map<?, ?> targetContext) {
        ((Map<Object, Object>) targetContext).putAll(valuesToWrite);
        return targetContext;
    }
}

class HashMapWriterAccessor implements ContextAccessor<Map<?, ?>, HashMap<?, ?>> {

    @Override
    public Class<?> readableType() {
        return Map.class;
    }

    @Override
    public void readValues(Map<?, ?> sourceContext,
            Predicate<Object> keyPredicate,
            Map<Object, Object> readValues) {
        readValues.putAll(sourceContext);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T readValue(Map<?, ?> sourceContext, Object key) {
        return (T) sourceContext.get(key);
    }

    @Override
    public Class<?> writeableType() {
        return HashMap.class;
    }

    @SuppressWarnings("unchecked")
    @Override
    public HashMap<?, ?> writeValues(Map<Object, Object> valuesToWrite,
            HashMap<?, ?> targetContext) {
        ((HashMap<Object, Object>) targetContext).putAll(valuesToWrite);
        return targetContext;
    }
}

class FixedReadHashMapWriterAccessor implements ContextAccessor<String, HashMap<?, ?>> {

    @Override
    public Class<?> readableType() {
        return String.class;
    }

    @Override
    public void readValues(String sourceContext,
            Predicate<Object> keyPredicate,
            Map<Object, Object> readValues) {
        readValues.put("DUMMY", sourceContext);
    }

    @Override
    public <T> T readValue(String sourceContext, Object key) {
        return null;
    }

    @Override
    public Class<?> writeableType() {
        return HashMap.class;
    }

    @SuppressWarnings("unchecked")
    @Override
    public HashMap<?, ?> writeValues(Map<Object, Object> valuesToWrite,
            HashMap<?, ?> targetContext) {
        ((HashMap<Object, Object>) targetContext).putAll(valuesToWrite);
        return targetContext;
    }
}

class HashMapReaderFixedWriterAccessor implements ContextAccessor<HashMap<?, ?>, String> {

    @Override
    public Class<?> readableType() {
        return HashMap.class;
    }

    @Override
    public void readValues(HashMap<?, ?> sourceContext,
            Predicate<Object> keyPredicate,
            Map<Object, Object> readValues) {
        readValues.putAll(sourceContext);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T readValue(HashMap<?, ?> sourceContext, Object key) {
        return (T) sourceContext.get(key);
    }

    @Override
    public Class<?> writeableType() {
        return String.class;
    }

    @Override
    public String writeValues(Map<Object, Object> valuesToWrite, String targetContext) {
        return "DUMMY";
    }
}
