/**
 * Copyright 2023 the original author or authors.
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
package io.micrometer.context.scopedvalue;

import org.assertj.core.util.VisibleForTesting;

/**
 * Thread-local storage for the current value in scope for the current Thread.
 */
public class ScopedValueHolder {

    private static final ThreadLocal<ScopedValue> VALUE_IN_SCOPE = new ThreadLocal<>();

    public static ScopedValue get() {
        return VALUE_IN_SCOPE.get();
    }

    static void set(ScopedValue value) {
        VALUE_IN_SCOPE.set(value);
    }

    @VisibleForTesting
    public static void remove() {
        VALUE_IN_SCOPE.remove();
    }

}
