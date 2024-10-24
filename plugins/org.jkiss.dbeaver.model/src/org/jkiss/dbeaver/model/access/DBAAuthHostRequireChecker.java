/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.access;

import org.jkiss.code.Nullable;

import java.util.Map;

/**
 * The interface serve as marker for auth models that have specific logic
 * about the requirements of host.
 * Interface was introduced for IAM (AWS) auth model case, where host isn't necessary.
 */
public interface DBAAuthHostRequireChecker {

    /**
     * Getting necessity of the hostname field
     *
     * @param authProperties
     * @return necessity of the hostname field
     */
    boolean isHostRequired(@Nullable Map<String, String> authProperties);
}
