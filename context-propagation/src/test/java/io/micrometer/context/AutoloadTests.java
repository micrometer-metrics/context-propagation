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

import io.micrometer.context.integration.Slf4jThreadLocalAccessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for autoloading {@link ThreadLocalAccessor} and {@link ContextAccessor}
 * implementations using {@link ContextRegistry}.
 *
 * @author Jonatan Ivanov
 */
class AutoloadTests {

    private ClassLoader originalClassLoader;

    private URLClassLoader urlClassLoader;

    @BeforeEach
    void setUp() throws MalformedURLException {
        originalClassLoader = Thread.currentThread().getContextClassLoader();
        URL[] extraClasspathURLs = new URL[] { new File("src/test/resources/autoload-tests").toURI().toURL() };
        urlClassLoader = new URLClassLoader(extraClasspathURLs, originalClassLoader);
        Thread.currentThread().setContextClassLoader(urlClassLoader);
    }

    @AfterEach
    void tearDown() throws IOException {
        Thread.currentThread().setContextClassLoader(originalClassLoader);
        urlClassLoader.close();
    }

    @Test
    void predefinedThreadLocalAccessorsShouldBeRegistered() {
        ContextRegistry registry = new ContextRegistry().loadThreadLocalAccessors();
        assertThat(registry.getThreadLocalAccessors()).hasSize(3)
            .satisfiesExactlyInAnyOrder(accessor -> assertThat(accessor).isInstanceOf(Slf4jThreadLocalAccessor.class),
                    accessor -> assertThat(accessor).isInstanceOf(StringThreadLocalAccessor.class),
                    accessor -> assertThat(accessor).isInstanceOf(AutoLoadableTestThreadLocalAccessor.class));
    }

    @Test
    void predefinedContextAccessorsShouldBeRegistered() {
        ContextRegistry registry = new ContextRegistry().loadContextAccessors();
        assertThat(registry.getContextAccessors()).hasSize(1)
            .satisfiesExactly(accessor -> assertThat(accessor).isInstanceOf(AutoLoadableTestContextAccessor.class));
    }

    public static class AutoLoadableTestThreadLocalAccessor implements ThreadLocalAccessor<String> {

        @Override
        public Object key() {
            return "TEST_KEY";
        }

        @Override
        public String getValue() {
            return "test-value";
        }

        @Override
        public void setValue(String value) {
        }

    }

    public static class AutoLoadableTestContextAccessor implements ContextAccessor<String, String> {

        @Override
        public Class<? extends String> readableType() {
            return String.class;
        }

        @Override
        public void readValues(String sourceContext, Predicate<Object> keyPredicate, Map<Object, Object> readValues) {
        }

        @Override
        public <T> T readValue(String sourceContext, Object key) {
            return null;
        }

        @Override
        public Class<? extends String> writeableType() {
            return String.class;
        }

        @Override
        public String writeValues(Map<Object, Object> valuesToWrite, String targetContext) {
            return "test-value";
        }

    }

}
