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
package org.jkiss.dbeaver.model.datadam.mcp;

import com.dbeaver.datadam.mcp.api.exception.DDMcpFunctionException;
import com.dbeaver.datadam.mcp.api.model.DDMcpFunctionCall;
import com.dbeaver.datadam.mcp.api.model.DDMcpFunctionDescriptor;
import com.dbeaver.datadam.mcp.api.model.DDMcpFunctionErrorCode;
import com.dbeaver.datadam.mcp.api.model.DDMcpFunctionResult;
import com.dbeaver.datadam.mcp.api.service.DDMcpFunctionService;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.datadam.mcp.function.DDMcpListDatasourcesFunction;
import org.jkiss.dbeaver.model.datadam.mcp.function.DDMcpListSchemaNamesFunction;

import java.util.List;

public class DDMcpDesktopFunctionService implements DDMcpFunctionService {
    private final DDMcpListDatasourcesFunction listDatasources = new DDMcpListDatasourcesFunction();
    private final DDMcpListSchemaNamesFunction listSchemaNames = new DDMcpListSchemaNamesFunction();

    @NotNull
    @Override
    public List<DDMcpFunctionDescriptor> getFunctions() {
        return List.of(listDatasources.getDescriptor(), listSchemaNames.getDescriptor());
    }

    @NotNull
    @Override
    public DDMcpFunctionResult callFunction(@NotNull DDMcpFunctionCall call) throws DDMcpFunctionException {
        if (listDatasources.getDescriptor().id().equals(call.functionId())) {
            return listDatasources.call();
        }
        if (listSchemaNames.getDescriptor().id().equals(call.functionId())) {
            return listSchemaNames.call(call);
        }
        throw new DDMcpFunctionException(DDMcpFunctionErrorCode.FUNCTION_NOT_FOUND, "Function not found", null);
    }
}
