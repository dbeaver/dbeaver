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
package org.jkiss.dbeaver.model.datadam.transport;

import com.dbeaver.datadam.dam.remote.api.model.DDRemoteDamSessionInfo;
import com.dbeaver.datadam.dam.remote.api.transport.DDRemoteDamChannelState;
import com.dbeaver.datadam.dam.remote.api.transport.DDRemoteDamChannelStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DDConnectionMonitorTest {
    @Test
    void retainsFinalConnectionStatisticsUntilTheNextSocketOpens() {
        var monitor = new DDConnectionMonitor();
        monitor.opened();
        monitor.sent(321);
        monitor.received(654);
        monitor.failed("Connection lost");
        monitor.disconnected();
        var finalSnapshot = monitor.snapshot();
        Assertions.assertNotNull(finalSnapshot.connectedAt());
        Assertions.assertEquals(321, finalSnapshot.bytesSent());
        Assertions.assertEquals(654, finalSnapshot.bytesReceived());
        Assertions.assertEquals(finalSnapshot.duration(), monitor.snapshot().duration());
        monitor.clearEvents();
        Assertions.assertEquals(finalSnapshot, monitor.snapshot());

        monitor.opened();
        Assertions.assertEquals(0, monitor.snapshot().bytesSent());
        Assertions.assertEquals(0, monitor.snapshot().bytesReceived());
        Assertions.assertEquals("Connection lost", monitor.snapshot().error());
        monitor.stateChanged(new DDRemoteDamChannelStatus(DDRemoteDamChannelState.CONNECTED,
            new DDRemoteDamSessionInfo("deployment", "instance", "session")));
        Assertions.assertNull(monitor.snapshot().error());
    }

    @Test
    void boundsEventsAndDetailsAndReturnsImmutableSnapshots() {
        var monitor = new DDConnectionMonitor();
        monitor.record(DDConnectionMonitor.EventType.SIGNED_IN, "oldest");
        var original = monitor.getEvents();
        for (int i = 0; i < 1_000; i++) {
            monitor.record(DDConnectionMonitor.EventType.ERROR, "x".repeat(8_192));
        }
        var events = monitor.getEvents();
        Assertions.assertEquals(1_000, events.size());
        Assertions.assertEquals(4_096, events.getFirst().details().length());
        Assertions.assertEquals("oldest", original.getFirst().details());
        Assertions.assertThrows(UnsupportedOperationException.class, events::clear);
        monitor.clearEvents();
        Assertions.assertTrue(monitor.getEvents().isEmpty());
        Assertions.assertEquals(1_000, events.size());
    }
}
