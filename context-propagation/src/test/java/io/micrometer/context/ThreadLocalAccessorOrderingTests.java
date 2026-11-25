/**
 * Copyright 2026 the original author or authors.
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

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ThreadLocalAccessorOrderingTests {

    @Test
    void should_set_dependencies_first_and_restore_them_last() {
        List<String> events = new ArrayList<>();
        RecordingAccessor first = new RecordingAccessor("first", 0, events);
        RecordingAccessor second = new RecordingAccessor("second", 1, events) {
            @Override
            public void setValue(String value) {
                assertThat(first.getValue()).isEqualTo(value);
                super.setValue(value);
            }
        };
        ContextRegistry registry = new ContextRegistry().registerThreadLocalAccessor(second)
            .registerThreadLocalAccessor(first);
        DefaultContextSnapshot snapshot = new DefaultContextSnapshot(registry, false);
        snapshot.put("first", "captured");
        snapshot.put("second", "captured");

        try (ContextSnapshot.Scope scope = snapshot.setThreadLocals()) {
            assertThat(second.getValue()).isEqualTo("captured");
        }

        assertThat(events).containsExactly("set:first", "set:second", "restore:second", "restore:first");
        assertThat(first.getValue()).isNull();
        assertThat(second.getValue()).isNull();
    }

    @Test
    void should_preserve_registration_order_for_equal_and_default_orders() {
        ContextRegistry registry = new ContextRegistry();
        TestThreadLocalAccessor defaultOrder = new TestThreadLocalAccessor("default", new ThreadLocal<>());
        RecordingAccessor first = accessor("first", 0);
        RecordingAccessor second = accessor("second", 0);
        RecordingAccessor lowest = accessor("lowest", Integer.MIN_VALUE);
        RecordingAccessor highest = accessor("highest", Integer.MAX_VALUE);

        registry.registerThreadLocalAccessor(first)
            .registerThreadLocalAccessor(highest)
            .registerThreadLocalAccessor(defaultOrder)
            .registerThreadLocalAccessor(lowest)
            .registerThreadLocalAccessor(second);

        assertThat(registry.getThreadLocalAccessors()).containsExactly(lowest, first, defaultOrder, second, highest);
    }

    @Test
    void should_order_replacements_and_reregistered_accessors() {
        ContextRegistry registry = new ContextRegistry();
        RecordingAccessor first = accessor("first", 0);
        RecordingAccessor second = accessor("second", 1);
        RecordingAccessor replacement = accessor("second", -1);
        registry.registerThreadLocalAccessor(second).registerThreadLocalAccessor(first);
        registry.registerThreadLocalAccessor(replacement);
        assertThat(registry.getThreadLocalAccessors()).containsExactly(replacement, first);

        assertThat(registry.removeThreadLocalAccessor("second")).isTrue();
        registry.registerThreadLocalAccessor(second);
        assertThat(registry.getThreadLocalAccessors()).containsExactly(first, second);

        registry.registerThreadLocalAccessor(accessor("third", 0)).registerThreadLocalAccessor(first);
        assertThat(registry.getThreadLocalAccessors()).extracting(ThreadLocalAccessor::key)
            .containsExactly("third", "first", "second");
    }

    @Test
    void should_restore_nested_scopes_in_reverse_order() {
        List<String> events = new ArrayList<>();
        RecordingAccessor first = new RecordingAccessor("first", 0, events);
        RecordingAccessor second = new RecordingAccessor("second", 1, events);
        ContextRegistry registry = new ContextRegistry().registerThreadLocalAccessor(second)
            .registerThreadLocalAccessor(first);
        DefaultContextSnapshot outer = new DefaultContextSnapshot(registry, false);
        outer.put("first", "outer");
        outer.put("second", "outer");
        DefaultContextSnapshot inner = new DefaultContextSnapshot(registry, false);
        inner.put("first", "inner");
        inner.put("second", "inner");

        try (ContextSnapshot.Scope scope = outer.setThreadLocals()) {
            try (ContextSnapshot.Scope nestedScope = inner.setThreadLocals()) {
                assertThat(first.getValue()).isEqualTo("inner");
                assertThat(second.getValue()).isEqualTo("inner");
            }
            assertThat(first.getValue()).isEqualTo("outer");
            assertThat(second.getValue()).isEqualTo("outer");
        }

        assertThat(events).containsExactly("set:first", "set:second", "set:first", "set:second", "restore:second",
                "restore:first", "restore:second", "restore:first");
        assertThat(first.getValue()).isNull();
        assertThat(second.getValue()).isNull();
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void should_preserve_order_when_values_are_missing(boolean clearMissing) {
        List<String> events = new ArrayList<>();
        RecordingAccessor first = new RecordingAccessor("first", 0, events);
        RecordingAccessor second = new RecordingAccessor("second", 1, events);
        ContextRegistry registry = new ContextRegistry().registerThreadLocalAccessor(second)
            .registerThreadLocalAccessor(first);
        first.setValue("previous");
        second.setValue("previous");
        events.clear();
        DefaultContextSnapshot snapshot = new DefaultContextSnapshot(registry, clearMissing);
        snapshot.put("second", "captured");

        try (ContextSnapshot.Scope scope = snapshot.setThreadLocals()) {
            assertThat(first.getValue()).isEqualTo(clearMissing ? null : "previous");
            assertThat(second.getValue()).isEqualTo("captured");
        }

        assertThat(first.getValue()).isEqualTo("previous");
        assertThat(second.getValue()).isEqualTo("previous");
        if (clearMissing) {
            assertThat(events).containsExactly("clear:first", "set:second", "restore:second", "restore:first");
        }
        else {
            assertThat(events).containsExactly("set:second", "restore:second");
        }
    }

    @Test
    void should_order_service_loaded_accessors(@TempDir Path directory) throws Exception {
        Path services = directory.resolve("META-INF/services/" + ThreadLocalAccessor.class.getName());
        Files.createDirectories(services.getParent());
        Files.write(services, Arrays.asList(SecondAccessor.class.getName(), FirstAccessor.class.getName()),
                StandardCharsets.UTF_8);
        ContextRegistry registry = new ContextRegistry();
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader loader = new URLClassLoader(new URL[] { directory.toUri().toURL() }, previous)) {
            Thread.currentThread().setContextClassLoader(loader);
            registry.loadThreadLocalAccessors();
            assertThat(registry.getThreadLocalAccessors()).extracting(ThreadLocalAccessor::key)
                .containsExactly("first", "second");
        }
        finally {
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    @Test
    void should_apply_registry_order_when_setting_all_values_from_a_context() {
        List<String> events = new ArrayList<>();
        RecordingAccessor first = new RecordingAccessor("first", 0, events);
        RecordingAccessor second = new RecordingAccessor("second", 1, events);
        ContextRegistry registry = new ContextRegistry().registerThreadLocalAccessor(second)
            .registerThreadLocalAccessor(first)
            .registerContextAccessor(new TestContextAccessor());
        ContextSnapshotFactory factory = ContextSnapshotFactory.builder().contextRegistry(registry).build();
        Map<String, String> context = new HashMap<>();
        context.put("first", "captured");
        context.put("second", "captured");

        try (ContextSnapshot.Scope scope = factory.setThreadLocalsFrom(context)) {
            assertThat(first.getValue()).isEqualTo("captured");
            assertThat(second.getValue()).isEqualTo("captured");
        }

        assertThat(events).containsExactly("set:first", "set:second", "restore:second", "restore:first");
    }

    @Test
    void should_preserve_explicit_key_order_when_setting_values_from_a_context() {
        List<String> events = new ArrayList<>();
        RecordingAccessor first = new RecordingAccessor("first", 0, events);
        RecordingAccessor second = new RecordingAccessor("second", 1, events);
        ContextRegistry registry = new ContextRegistry().registerThreadLocalAccessor(second)
            .registerThreadLocalAccessor(first)
            .registerContextAccessor(new TestContextAccessor());
        ContextSnapshotFactory factory = ContextSnapshotFactory.builder().contextRegistry(registry).build();
        Map<String, String> context = new HashMap<>();
        context.put("first", "captured");
        context.put("second", "captured");

        try (ContextSnapshot.Scope scope = factory.setThreadLocalsFrom(context, "second", "first")) {
            assertThat(first.getValue()).isEqualTo("captured");
            assertThat(second.getValue()).isEqualTo("captured");
        }

        assertThat(events).containsExactly("set:second", "set:first", "restore:second", "restore:first");
    }

    @Test
    void should_preserve_order_during_concurrent_registration() throws Exception {
        ContextRegistry registry = new ContextRegistry();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<?>> registrations = new ArrayList<>();
            for (int thread = 0; thread < 2; thread++) {
                int group = thread;
                registrations.add(executor.submit(() -> {
                    start.await();
                    for (int i = 50; i >= 0; i--) {
                        registry.registerThreadLocalAccessor(accessor(group + ":" + i, i));
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> registration : registrations) {
                registration.get(10, TimeUnit.SECONDS);
            }
            assertThat(registry.getThreadLocalAccessors()).hasSize(102);
            assertThat(registry.getThreadLocalAccessors()).extracting(ThreadLocalAccessor::getOrder).isSorted();
        }
        finally {
            executor.shutdownNow();
        }
    }

    private static RecordingAccessor accessor(String key, int order) {
        return new RecordingAccessor(key, order, new ArrayList<>());
    }

    public static class FirstAccessor extends RecordingAccessor {

        public FirstAccessor() {
            super("first", 0, Collections.emptyList());
        }

    }

    public static class SecondAccessor extends RecordingAccessor {

        public SecondAccessor() {
            super("second", 1, Collections.emptyList());
        }

    }

    private static class RecordingAccessor extends TestThreadLocalAccessor {

        private final int order;

        private final List<String> events;

        RecordingAccessor(String key, int order, List<String> events) {
            super(key, new ThreadLocal<>());
            this.order = order;
            this.events = events;
        }

        @Override
        public int getOrder() {
            return this.order;
        }

        @Override
        public void setValue(String value) {
            this.events.add("set:" + key());
            super.setValue(value);
        }

        @Override
        public void setValue() {
            this.events.add("clear:" + key());
            super.setValue();
        }

        @Override
        public void restore(String previousValue) {
            this.events.add("restore:" + key());
            super.setValue(previousValue);
        }

        @Override
        public void restore() {
            this.events.add("restore:" + key());
            super.setValue();
        }

    }

}
