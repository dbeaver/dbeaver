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
package org.jkiss.dbeaver.model;

import java.util.Map;

/**
 * Abstract context with attributes.
 * All attributes will be cleared after context close.
 * Attributes are valid only during implementor Java object live time.
 */
public interface DBPContextWithAttributes {

    /**
     * Returns copy of all context attributes
     */
    Map<String, ?> getContextAttributes();

    /**
     * Returns attribute value by name.
     */
    <T> T getContextAttribute(String attributeName);

    /**
     * Sets context attribute
     */
    <T> void setContextAttribute(String attributeName, T attributeValue);

    /**
     * Removes context attribute
     */
    void removeContextAttribute(String attributeName);

}
