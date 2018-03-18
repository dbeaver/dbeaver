/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.net.ssh;

import org.jkiss.code.NotNull;

/**
 * SSH implementation enum
 */
public enum SSHImplType  {

    JSCH("jsch", "JSch", SSHImplementationJsch.class),
    SSHJ("sshj", "SSHJ", SSHImplementationSshj.class);

    private String id;
    private String label;
    private Class<? extends SSHImplementation> implClass;

    SSHImplType(@NotNull String id, @NotNull String label, @NotNull Class<? extends SSHImplementation> implClass) {
        this.id = id;
        this.label = label;
        this.implClass = implClass;
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public String getLabel() {
        return label;
    }

    @NotNull
    public Class<? extends SSHImplementation> getImplClass() {
        return implClass;
    }

    @NotNull
    public static SSHImplType getById(String id) throws IllegalArgumentException {
        for (SSHImplType  it : values()) {
            if (it.getId().equals(id)) {
                return it;
            }
        }
        throw new IllegalArgumentException("Bad SSH impl: " + id);
    }
}