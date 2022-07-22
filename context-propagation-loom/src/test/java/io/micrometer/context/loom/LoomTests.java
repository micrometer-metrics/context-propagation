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
package io.micrometer.context.loom;

import java.time.Duration;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import io.micrometer.context.ContextRegistry;
import io.micrometer.context.ContextSnapshot;
import io.micrometer.context.ThreadLocalAccessor;
import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoomTests {

    MyThreadLocalAccessor accessor = new MyThreadLocalAccessor();
    ContextRegistry contextRegistry = ContextRegistry.getInstance();

    @BeforeEach
    void setup() {
        accessor.reset();
        contextRegistry.registerThreadLocalAccessor(accessor);
    }

    @Test
    void threadLocalsManually() throws InterruptedException {
        var queue = new SynchronousQueue<String>();
        ContextRegistry contextRegistry = ContextRegistry.getInstance();
        contextRegistry.registerThreadLocalAccessor(accessor);
        accessor.setValue("foo");
        ContextSnapshot capture = ContextSnapshot.capture();

        Thread.ofVirtual().start(() -> {
            try {
                Thread.sleep(Duration.ofMillis(5));
                try (ContextSnapshot.Scope scope = capture.setThreadLocalValues()) {
                    String s = accessor.getValue();
                    if (s != null) {
                        queue.put(s);
                    }
                }
            }
            catch (InterruptedException e) {
            }
        });

        String msg = queue.poll(100, TimeUnit.MILLISECONDS);

        BDDAssertions.then(msg).isEqualTo("foo");
    }

    @Test
    void threadLocals() throws InterruptedException {
        var queue = new SynchronousQueue<String>();
        accessor.setValue("foo");
        ContextSnapshot capture = ContextSnapshot.capture();

        Thread.ofVirtual().start(capture.wrap(() -> {
            try {
                Thread.sleep(Duration.ofMillis(5));
                String s = accessor.getValue();
                if (s != null) {
                    queue.put(s);
                }
            }
            catch (InterruptedException e) {
            }
        }));

        String msg = queue.poll(100, TimeUnit.MILLISECONDS);

        BDDAssertions.then(msg).isEqualTo("foo");
    }

    
    static class MyThreadLocalAccessor implements ThreadLocalAccessor<String> {

        private static ThreadLocal<String> TL = new ThreadLocal<>();

        @Override
        public Object key() {
            return "key";
        }

        @Override
        public String getValue() {
            return TL.get();
        }

        @Override
        public void setValue(String s) {
            TL.set(s);
        }

        @Override
        public void reset() {
            TL.remove();
        }
    }
}
