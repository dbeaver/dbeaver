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
package org.jkiss.dbeaver.runtime;

import org.jkiss.code.NotNull;

/**
 * Responsible for environment-specific path transformation during file browse discovery
 * See NavigatorHandlerShowInExplorer 
 */
public interface IEnvironmentPathMapper {
    
    /**
     * Checks if path transformation should be applied
     */
    boolean isApplicable(@NotNull String localEnvPath);
   
    /**
     * Applies environment-specific path transformation
     */
    @NotNull
    String map(@NotNull String localEnvPath);
}
