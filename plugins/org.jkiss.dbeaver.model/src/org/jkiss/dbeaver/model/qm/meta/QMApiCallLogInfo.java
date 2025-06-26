/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.qm.meta;

import java.time.LocalDateTime;
import java.util.Map;

public class QMApiCallLogInfo extends QMMObject {

    private String userName;
    private String qmSessionId;
    private QMApiCallType requestType;
    private String endpoint;
    private String httpMethod;
    private Boolean isSuccessful;
    private LocalDateTime requestTime;
    private Map<String, Object> parameters;

    public QMApiCallLogInfo(
        QMMetaObjectType type,
        String endpoint,
        String httpMethod,
        Boolean isSuccessful,
        Map<String, Object> parameters,
        String qmSessionId,
        LocalDateTime requestTime,
        QMApiCallType requestType,
        String userName
    ) {
        super(type);
        this.endpoint = endpoint;
        this.httpMethod = httpMethod;
        this.isSuccessful = isSuccessful;
        this.parameters = parameters;
        this.qmSessionId = qmSessionId;
        this.requestTime = requestTime;
        this.requestType = requestType;
        this.userName = userName;
    }

    @Override
    public QMMConnectionInfo getConnection() {
        return null;
    }

    public String getQmSessionId() {
        return qmSessionId;
    }

    @Override
    public boolean isClosed() {
        return true;
    }

    @Override
    public String getText() {
        return "";
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public Boolean isSuccessful() {
        return isSuccessful;
    }

    public LocalDateTime getRequestTime() {
        return requestTime;
    }

    public String getUserName() {
        return userName;
    }

    public static QMActivityLogInfoBuilder builder() {
        return new QMActivityLogInfoBuilder();
    }

    public QMApiCallType getRequestType() {
        return requestType;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public static class QMActivityLogInfoBuilder {
        private QMMetaObjectType type;
        private String endpoint;
        private String httpMethod;
        private Boolean isSuccessful;
        private Map<String, Object> parameters;
        private String qmSessionId;
        private LocalDateTime requestTime;
        private QMApiCallType requestType;
        private String userName;

        public QMActivityLogInfoBuilder type(QMMetaObjectType type) {
            this.type = type;
            return this;
        }
        public QMActivityLogInfoBuilder endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }
        public QMActivityLogInfoBuilder httpMethod(String httpMethod) {
            this.httpMethod = httpMethod;
            return this;
        }
        public QMActivityLogInfoBuilder isSuccessful(Boolean isSuccessful) {
            this.isSuccessful = isSuccessful;
            return this;
        }
        public QMActivityLogInfoBuilder parameters(Map<String, Object> parameters) {
            this.parameters = parameters;
            return this;
        }
        public QMActivityLogInfoBuilder qmSessionId(String qmSessionId) {
            this.qmSessionId = qmSessionId;
            return this;
        }
        public QMActivityLogInfoBuilder requestTime(LocalDateTime requestTime) {
            this.requestTime = requestTime;
            return this;
        }
        public QMActivityLogInfoBuilder requestType(QMApiCallType requestType) {
            this.requestType = requestType;
            return this;
        }
        public QMActivityLogInfoBuilder userName(String userName) {
            this.userName = userName;
            return this;
        }
        public QMApiCallLogInfo build() {
            return new QMApiCallLogInfo(
                type,
                endpoint,
                httpMethod,
                isSuccessful,
                parameters,
                qmSessionId,
                requestTime,
                requestType,
                userName
            );
        }
    }
}
