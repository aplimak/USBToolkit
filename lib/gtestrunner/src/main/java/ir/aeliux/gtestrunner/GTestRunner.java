package ir.aeliux.gtestrunner;

import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.Filterable;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;

import java.util.*;

public final class GTestRunner extends Runner implements Filterable {
    private final Class<?> testClass;

    private final List<String> allTests;
    private final List<String> selectedTests;

    private final Map<String, Description> descriptions =
            new HashMap<>();

    public GTestRunner(Class<?> testClass)
            throws InitializationError {

        this.testClass = testClass;

        GTestLibrary config =
                testClass.getAnnotation(GTestLibrary.class);

        if (config == null) {
            throw new InitializationError(
                    "Missing @GTestLibrary on " + testClass.getName()
            );
        }

        try {
            System.loadLibrary(config.value());

            allTests = Arrays.asList(listTests());

        } catch (LinkageError | RuntimeException e) {
            throw new InitializationError(e);
        }

        if (allTests.isEmpty()) {
            throw new InitializationError("No GoogleTest cases found");
        }

        selectedTests = new ArrayList<>(allTests);

        for (String name : allTests) {
            descriptions.put(
                    name,
                    Description.createTestDescription(testClass, name)
            );
        }
    }

    @Override
    public Description getDescription() {
        Description suite =
                Description.createSuiteDescription(testClass);

        for (String name : selectedTests) {
            suite.addChild(descriptions.get(name));
        }

        return suite;
    }

    @Override
    public void filter(Filter filter) throws NoTestsRemainException {
        List<String> filtered = new ArrayList<>();

        for (String name : allTests) {
            if (filter.shouldRun(descriptions.get(name))) {
                filtered.add(name);
            }
        }

        if (filtered.isEmpty()) {
            throw new NoTestsRemainException();
        }

        selectedTests.clear();
        selectedTests.addAll(filtered);
    }

    @Override
    public void run(RunNotifier notifier) {
        Map<String, Result> results;

        try {
            String[] raw = runAll(
                    selectedTests.toArray(new String[0])
            );

            if (raw == null || raw.length % 3 != 0) {
                throw new IllegalStateException(
                        "Invalid native test result"
                );
            }

            results = new HashMap<>();

            for (int i = 0; i < raw.length; i += 3) {
                results.put(
                        raw[i],
                        new Result(raw[i + 1], raw[i + 2])
                );
            }

        } catch (Throwable error) {
            reportExecutionFailure(notifier, error);
            return;
        }

        for (String name : selectedTests) {
            Description description = descriptions.get(name);
            Result result = results.get(name);

            if (result == null) {
                reportFailure(
                        notifier,
                        description,
                        new AssertionError("Native test returned no result")
                );
                continue;
            }

            if ("S".equals(result.status)) {
                notifier.fireTestIgnored(description);
                continue;
            }

            notifier.fireTestStarted(description);

            if ("F".equals(result.status)) {
                notifier.fireTestFailure(
                        new Failure(
                                description,
                                new AssertionError(result.message)
                        )
                );
            }

            notifier.fireTestFinished(description);
        }
    }

    private void reportExecutionFailure(
            RunNotifier notifier,
            Throwable error) {

        for (String name : selectedTests) {
            reportFailure(notifier, descriptions.get(name), error);
        }
    }

    private void reportFailure(
            RunNotifier notifier,
            Description description,
            Throwable error) {

        notifier.fireTestStarted(description);
        notifier.fireTestFailure(new Failure(description, error));
        notifier.fireTestFinished(description);
    }

    private static final class Result {
        final String status;
        final String message;

        Result(String status, String message) {
            this.status = status;
            this.message = message;
        }
    }

    private static native String[] listTests();
    private static native String[] runAll(String[] selectedTests);
}
