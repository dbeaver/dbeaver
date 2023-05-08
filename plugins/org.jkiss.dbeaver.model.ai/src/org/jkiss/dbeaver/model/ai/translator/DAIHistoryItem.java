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

package org.jkiss.dbeaver.model.ai.translator;

import org.jkiss.dbeaver.model.data.json.JSONUtils;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Natural language translator item
 */
public class DAIHistoryItem {

    private String id;
    private String naturalText;
    private String completionText;
    private Date time;

    public DAIHistoryItem() {
    }

    public DAIHistoryItem(String naturalText, String completionText) {
        this.naturalText = naturalText;
        this.completionText = completionText;
    }

    public DAIHistoryItem(Map<String, Object> map) {
        this.id = JSONUtils.getString(map, "id");
        this.naturalText = JSONUtils.getString(map, "naturalText");
        this.completionText = JSONUtils.getString(map, "completionText");
        this.time = new Date(JSONUtils.getLong(map, "time", 0));
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNaturalText() {
        return naturalText;
    }

    public void setNaturalText(String naturalText) {
        this.naturalText = naturalText;
    }

    public String getCompletionText() {
        return completionText;
    }

    public void setCompletionText(String completionText) {
        this.completionText = completionText;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    /**
     * Converts item to map
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", this.id);
        map.put("naturalText", this.naturalText);
        map.put("completionText", this.completionText);
        map.put("time", this.time == null ? 0L : this.time.getTime());
        return map;
    }
}
