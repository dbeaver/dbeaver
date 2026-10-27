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
package org.jkiss.dbeaver.ui.datadam.internal;

import org.jkiss.dbeaver.model.DBIcon;

public class DDTrackingUIIcons {
    public static final DBIcon PROJECT_SYNC = new DBIcon("datadam-project-sync", "project_sync.svg");
    public static final DBIcon PROJECT_DISCONNECT = new DBIcon("datadam-project-disconnect", "project_disconnect.svg");
    public static final DBIcon STATUS_LOCAL_CHANGES = new DBIcon("datadam-status-local", "status_local_changes.svg");
    public static final DBIcon STATUS_SERVER_CHANGES = new DBIcon("datadam-status-server", "status_server_changes.svg");
    public static final DBIcon STATUS_CONFLICT = new DBIcon("datadam-status-conflict", "status_conflict.svg");
    public static final DBIcon STATUS_NOT_ON_DEVICE = new DBIcon("datadam-status-remote", "status_not_on_device.svg");

    static {
        DBIcon.loadIcons(DDTrackingUIIcons.class);
    }

    private DDTrackingUIIcons() {
    }
}
