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
    public boolean canReadFrom(Class<?> contextType) {
        return Map.class.isAssignableFrom(contextType);
    }

    @Override
    public void readValues(Map<?, ?> sourceContext, Predicate<Object> keyPredicate, Map<Object, Object> readValues) {
        readValues.putAll(sourceContext);
    }

    @Override
    public boolean canWriteTo(Class<?> contextType) {
        return Map.class.isAssignableFrom(contextType);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<?, ?> writeValues(Map<Object, Object> valuesToWrite, Map<?, ?> targetContext) {
        ((Map<Object, Object>) targetContext).putAll(valuesToWrite);
        return targetContext;
    }

}
