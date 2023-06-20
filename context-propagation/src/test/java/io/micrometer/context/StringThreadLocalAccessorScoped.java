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

/**
 * Example of scoped {@link ThreadLocalAccessor} implementation.
 */
public class StringThreadLocalAccessorScoped implements ThreadLocalAccessor<String> {

    public static final String KEY = "string.threadlocal.scoped";

    @Override
    public Object key() {
        return KEY;
    }

    @Override
    public String getValue() {
        return StringThreadLocalHolder.getValue();
    }

    @Override
    public Scope setValueScoped(String value) {
        String previousValue = StringThreadLocalHolder.getValue();
        StringThreadLocalHolder.setValue(value);
        return () -> StringThreadLocalHolder.setValue(previousValue);
    }

    @Override
    public Scope setValueScoped() {
        String previousValue = StringThreadLocalHolder.getValue();
        StringThreadLocalHolder.reset();
        return () -> StringThreadLocalHolder.setValue(previousValue);
    }

    @Override
    public void setValue(String value) {
        throw new IllegalStateException("Not supported");
    }

}
