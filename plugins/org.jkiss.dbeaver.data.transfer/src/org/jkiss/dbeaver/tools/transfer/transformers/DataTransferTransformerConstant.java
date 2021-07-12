/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.tools.transfer.transformers;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.tools.transfer.IDataTransferAttributeTransformer;

import java.util.Map;

/**
 * Constant attribute transformer
 */
public class DataTransferTransformerConstant implements IDataTransferAttributeTransformer {

    @Override
    public Object transformAttribute(@NotNull DBCSession session, @NotNull DBDAttributeBinding[] dataAttributes, @NotNull Object[] dataRow, @NotNull DBDAttributeBinding attribute, Object attrValue, @NotNull Map<String, Object> options) throws DBException {
        return options.get("constant");
    }

}
