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

import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * 
 */
class InstrumentationTests {

    private final ContextRegistry registry = new ContextRegistry()
            .registerThreadLocalAccessor(new ObservationThreadLocalAccessor());


    @AfterEach
    void clear() {
        ObservationThreadLocalHolder.reset();
    }

    @Test
    void should_instrument_runnable() throws InterruptedException {
        ObservationThreadLocalHolder.setValue("hello");
        AtomicReference<String> valueInNewThread = new AtomicReference<>();
        Runnable runnable = runnable(valueInNewThread);
        runInNewThread(runnable);
        then(valueInNewThread.get())
                .as("By default thread local information should not be propagated")
                .isNull();

        runInNewThread(ContextSnapshot.capture(this.registry, key -> true).instrumentRunnable(runnable));

        then(valueInNewThread.get())
                .as("With context container the thread local information should be propagated")
                .isEqualTo("hello");
    }

    @Test
    void should_instrument_callable() throws ExecutionException, InterruptedException, TimeoutException {
        ObservationThreadLocalHolder.setValue("hello");
        AtomicReference<String> valueInNewThread = new AtomicReference<>();
        Callable<String> callable = () -> {
            valueInNewThread.set(ObservationThreadLocalHolder.getValue());
            return "foo";
        };
        runInNewThread(callable);
        then(valueInNewThread.get())
                .as("By default thread local information should not be propagated")
                .isNull();

        runInNewThread(ContextSnapshot.capture(this.registry, key -> true).instrumentCallable(callable));

        then(valueInNewThread.get())
                .as("With context container the thread local information should be propagated")
                .isEqualTo("hello");
    }

    @Test
    void should_instrument_executor() throws InterruptedException {
        ObservationThreadLocalHolder.setValue("hello");
        AtomicReference<String> valueInNewThread = new AtomicReference<>();
        Executor executor = command -> new Thread(command).start();
        runInNewThread(executor, valueInNewThread);
        then(valueInNewThread.get())
                .as("By default thread local information should not be propagated")
                .isNull();

        runInNewThread(
                ContextSnapshot.capture(this.registry, key -> true).instrumentExecutor(executor),
                valueInNewThread);

        then(valueInNewThread.get())
                .as("With context container the thread local information should be propagated")
                .isEqualTo("hello");
    }

    @Test
    void should_instrument_executor_service() throws InterruptedException, ExecutionException, TimeoutException {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            ObservationThreadLocalHolder.setValue("hello");
            AtomicReference<String> valueInNewThread = new AtomicReference<>();
            runInNewThread(executorService, valueInNewThread,
                    atomic -> then(atomic.get())
                            .as("By default thread local information should not be propagated")
                            .isNull());

            runInNewThread(
                    ContextSnapshot.capture(this.registry, key -> true).instrumentExecutorService(executorService),
                    valueInNewThread,
                    atomic -> then(atomic.get())
                            .as("With context container the thread local information should be propagated")
                            .isEqualTo("hello"));
        }
        finally {
            executorService.shutdown();
        }
    }

    private void runInNewThread(Runnable runnable) throws InterruptedException {
        Thread thread = new Thread(runnable);
        thread.start();
        Thread.sleep(5);
    }

    private void runInNewThread(Callable<?> callable) throws InterruptedException, ExecutionException, TimeoutException {
        ExecutorService service = Executors.newSingleThreadExecutor();
        try {
            service.submit(callable).get(5, TimeUnit.MILLISECONDS);
        }
        finally {
            service.shutdown();
        }
    }

    private void runInNewThread(Executor executor, AtomicReference<String> valueInNewThread) throws InterruptedException {
        executor.execute(runnable(valueInNewThread));
        Thread.sleep(5);
    }

    private void runInNewThread(
            ExecutorService executor, AtomicReference<String> valueInNewThread,
            Consumer<AtomicReference<String>> assertion)
            throws InterruptedException, ExecutionException, TimeoutException {

        executor.execute(runnable(valueInNewThread));
        Thread.sleep(5);
        assertion.accept(valueInNewThread);

        executor.submit(runnable(valueInNewThread)).get(5, TimeUnit.MILLISECONDS);
        assertion.accept(valueInNewThread);

        executor.submit(callable(valueInNewThread)).get(5, TimeUnit.MILLISECONDS);
        assertion.accept(valueInNewThread);

        executor.submit(runnable(valueInNewThread), "foo").get(5, TimeUnit.MILLISECONDS);
        assertion.accept(valueInNewThread);

        executor.invokeAll(Collections.singletonList(callable(valueInNewThread)));
        assertion.accept(valueInNewThread);

        executor.invokeAll(Collections.singletonList(callable(valueInNewThread)), 5, TimeUnit.MILLISECONDS);
        assertion.accept(valueInNewThread);

        executor.invokeAny(Collections.singletonList(callable(valueInNewThread)));
        assertion.accept(valueInNewThread);

        executor.invokeAny(Collections.singletonList(callable(valueInNewThread)), 5, TimeUnit.MILLISECONDS);
        assertion.accept(valueInNewThread);
    }

    private Runnable runnable(AtomicReference<String> valueInNewThread) {
        return () -> valueInNewThread.set(ObservationThreadLocalHolder.getValue());
    }

    private Callable<Object> callable(AtomicReference<String> valueInNewThread) {
        return () -> {
            valueInNewThread.set(ObservationThreadLocalHolder.getValue());
            return "foo";
        };
    }

}
