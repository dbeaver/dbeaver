/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.rm;

import java.util.Date;

/**
 * Resource change
 */
public class RMResourceChange {
    private String changeId;
    private Date changeTime;
    private Date changeUser;

    public RMResourceChange() {
    }

    public RMResourceChange(String changeId, Date changeTime, Date changeUser) {
        this.changeId = changeId;
        this.changeTime = changeTime;
        this.changeUser = changeUser;
    }

    public String getChangeId() {
        return changeId;
    }

    public void setChangeId(String changeId) {
        this.changeId = changeId;
    }

    public Date getChangeTime() {
        return changeTime;
    }

    public void setChangeTime(Date changeTime) {
        this.changeTime = changeTime;
    }

    public Date getChangeUser() {
        return changeUser;
    }

    public void setChangeUser(Date changeUser) {
        this.changeUser = changeUser;
    }
}
