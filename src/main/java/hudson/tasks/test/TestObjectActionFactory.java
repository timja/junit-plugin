/*
 * The MIT License
 *
 * Copyright (c) 2026, Jenkins contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.tasks.test;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Action;
import java.util.Collection;

/**
 * Contributes actions to the app bar of test result pages: the overall test report, packages, classes,
 * test cases and their history.
 *
 * <p>This is the test result counterpart of {@link jenkins.model.TransientActionFactory}, which cannot be used
 * here because test objects are not {@link hudson.model.Actionable}. Actions are placed using the core menu API:
 * {@link Action#getGroup()} decides whether an action is shown as a button in the app bar
 * ({@link jenkins.model.menu.Group#IN_APP_BAR} and friends) or in its overflow menu (the default),
 * and {@link Action#getEvent()} decides what happens when it is clicked.
 * Actions without an icon are not shown.
 *
 * <p>An action whose {@link Action#getUrlName()} is a relative name is also bound to that URL below the test object,
 * like a {@link hudson.tasks.junit.TestAction}.
 *
 * <p>Implementations are called on every page render, so they should be cheap.
 *
 * @param <T> the type of test object to contribute actions to
 * @since TODO
 */
public abstract class TestObjectActionFactory<T extends TestObject> implements ExtensionPoint {

    /**
     * The type of test object this factory contributes actions to, for example {@link TestResult} for every
     * test result page, or {@link hudson.tasks.junit.CaseResult} for test cases only.
     */
    public abstract Class<T> type();

    /**
     * Creates the actions for the given test object.
     *
     * @param target the test object being displayed
     * @return the actions to show, possibly empty
     */
    @NonNull
    public abstract Collection<? extends Action> createFor(@NonNull T target);

    /**
     * All registered factories.
     */
    @SuppressWarnings("rawtypes")
    public static ExtensionList<TestObjectActionFactory> all() {
        return ExtensionList.lookup(TestObjectActionFactory.class);
    }
}
