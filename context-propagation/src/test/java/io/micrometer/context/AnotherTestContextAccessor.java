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

import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import io.micrometer.context.util.annotation.Nullable;

class AnotherTestContextAccessor implements ContextAccessor<Set, Set> {

    @Override
    public Class<Set> readableType() {
        return Set.class;
    }

    @Override
    public void readValues(Set sourceContext, Predicate<Object> keyPredicate, Map<Object, Object> readValues) {

    }

    @Nullable
    @Override
    public <T> T readValue(Set sourceContext, Object key) {
        return null;
    }

    @Override
    public Class<Set> writeableType() {
        return Set.class;
    }

    @Override
    public Set writeValues(Map<Object, Object> valuesToWrite, Set targetContext) {
        return null;
    }

}
