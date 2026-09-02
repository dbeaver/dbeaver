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
package org.jkiss.dbeaver.model.ai.engine;

import com.google.gson.annotations.SerializedName;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.ai.utils.AIUtils;
import org.jkiss.dbeaver.model.meta.Property;

/**
 * Base class for AI engine properties.
 */
public abstract class BaseAIEngineProperties implements AIEngineProperties {

    private boolean global = true;

    @SerializedName(value = "gpt.model.temperature", alternate = {"anthropic.temperature", "aws.temperature"})
    protected double temperature;

    @Nullable
    @SerializedName(value = "gpt.log.query", alternate = {"anthropic.log.query", "aws.log.query"})
    protected Boolean loggingEnabled;

    @Nullable
    @SerializedName(value = "gpt.timeout")
    private Integer timeout;

    @Override
    @Property(order = 0, name = "Global credentials")
    public boolean isGlobal() {
        return global;
    }

    @Override
    public void setGlobal(boolean global) {
        this.global = global;
    }

    public void setTemperature(double temperature) {
        this.temperature = AIUtils.normalizeTemperature(temperature);
    }

    @Override
    @Property(order = 1000)
    public boolean isLoggingEnabled() {
        return loggingEnabled != null && loggingEnabled;
    }

    @Override
    public void setLoggingEnabled(boolean loggingEnabled) {
        this.loggingEnabled = loggingEnabled;
    }

    @Override
    @Property(order = 1001)
    public int getTimeout() {
        return timeout != null && timeout > 0 ? timeout : DEFAULT_TIMEOUT;
    }

    @Override
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}
