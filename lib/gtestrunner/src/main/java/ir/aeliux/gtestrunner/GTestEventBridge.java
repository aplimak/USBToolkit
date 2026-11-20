package ir.aeliux.gtestrunner;

import org.junit.AssumptionViolatedException;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.TestTimedOutException;

import java.util.Map;

public final class GTestEventBridge {

    private final RunNotifier notifier;
    private final Map<String, Description> descriptions;

    private int failureCount;

    public GTestEventBridge(
            RunNotifier notifier,
            Map<String, Description> descriptions) {
        this.notifier = notifier;
        this.descriptions = descriptions;
    }

    private Description getDescription(String name) {
        Description description = descriptions.get(name);

        if (description == null) {
            throw new IllegalArgumentException(
                    "Unknown GoogleTest case: " + name
            );
        }

        return description;
    }

    public void onTestStarted(String name) {
        notifier.fireTestStarted(getDescription(name));
    }

    public void onTestFailure(String name, String message) {
        failureCount++;

        notifier.fireTestFailure(
                new Failure(
                        getDescription(name),
                        new AssertionError(message)
                )
        );
    }

    public void onTestFinished(String name) {
        notifier.fireTestFinished(getDescription(name));
    }

    public void onTestSkipped(String name, String message) {
        notifier.fireTestAssumptionFailed(
                new Failure(
                        getDescription(name),
                        new AssumptionViolatedException(message)
                )
        );

        notifier.fireTestFinished(getDescription(name));
    }

    public int getFailureCount() {
        return failureCount;
    }
}
