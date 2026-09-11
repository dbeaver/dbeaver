/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.model.runtime;

import org.eclipse.core.runtime.IProgressMonitor;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
public class DefaultProgressMonitorTest {
    @Test
    public void doneDoesNotRestartCompletedTask() {
        IProgressMonitor nested = Mockito.mock(IProgressMonitor.class);
        DefaultProgressMonitor monitor = new DefaultProgressMonitor(nested);

        monitor.beginTask("Download", 100);
        monitor.worked(100);
        monitor.done();

        InOrder calls = Mockito.inOrder(nested);
        calls.verify(nested).beginTask("Download", 100);
        calls.verify(nested).worked(100);
        calls.verify(nested).done();
        calls.verifyNoMoreInteractions();
    }

    @Test
    public void doneRestoresParentTask() {
        IProgressMonitor nested = Mockito.mock(IProgressMonitor.class);
        DefaultProgressMonitor monitor = new DefaultProgressMonitor(nested);

        monitor.beginTask("Parent", 10);
        monitor.worked(2);
        monitor.beginTask("Child", 5);
        monitor.worked(5);
        monitor.done();

        InOrder calls = Mockito.inOrder(nested);
        calls.verify(nested).beginTask("Parent", 10);
        calls.verify(nested).worked(2);
        calls.verify(nested).beginTask("Child", 5);
        calls.verify(nested).worked(5);
        calls.verify(nested).done();
        calls.verify(nested).beginTask("Parent", 10);
        calls.verify(nested).worked(2);
        calls.verifyNoMoreInteractions();
    }
}
