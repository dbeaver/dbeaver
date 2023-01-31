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
package org.jkiss.dbeaver.tools.transfer.registry;

import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlExpression;
import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.navigator.DBNUtils;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamWriter;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

public class DataTransferStreamWriterDescriptor extends AbstractDescriptor {
    private static final Log log = Log.getLog(DataTransferStreamWriterDescriptor.class);

    private final String id;
    private final String label;
    private final String description;
    private final int order;
    private final DBPImage icon;
    private final ObjectType type;
    private JexlExpression visibleIf;

    protected DataTransferStreamWriterDescriptor(@NotNull IConfigurationElement config) {
        super(config);

        this.id = config.getAttribute("id");
        this.label = config.getAttribute("label");
        this.description = config.getAttribute("description");
        this.order = CommonUtils.toInt(config.getAttribute("order"));
        this.icon = iconToImage(config.getAttribute("icon"), DBIcon.TYPE_UNKNOWN);
        this.type = new ObjectType(config.getAttribute("class"));

        final String visibleIf = CommonUtils.toString(config.getAttribute("if"));
        if (CommonUtils.isNotEmpty(visibleIf)) {
            try {
                this.visibleIf = parseExpression(visibleIf);
            } catch (DBException e) {
                log.debug("Error parsing expression '" + visibleIf + "': " + GeneralUtils.getExpressionParseMessage(e));
            }
        }
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
    public String getDescription() {
        return description;
    }

    public int getOrder() {
        return order;
    }

    @NotNull
    public DBPImage getIcon() {
        return icon;
    }

    @NotNull
    public ObjectType getType() {
        return type;
    }

    public boolean isVisible(@NotNull DataTransferProcessorDescriptor processor) {
        try {
            return visibleIf == null || Boolean.TRUE.equals(visibleIf.evaluate(AbstractDescriptor.makeContext(processor, null)));
        } catch (JexlException e) {
            log.debug("Error evaluating expression '" + visibleIf.getSourceText() + "': " + GeneralUtils.getExpressionParseMessage(e));
            return false;
        }
    }

    @NotNull
    public IStreamWriter create() throws DBException {
        type.checkObjectClass(IStreamWriter.class);
        try {
            return type
                .getObjectClass(IStreamWriter.class)
                .getDeclaredConstructor()
                .newInstance();
        } catch (Throwable e) {
            throw new DBException("Can't create stream writer", e);
        }
    }
}
