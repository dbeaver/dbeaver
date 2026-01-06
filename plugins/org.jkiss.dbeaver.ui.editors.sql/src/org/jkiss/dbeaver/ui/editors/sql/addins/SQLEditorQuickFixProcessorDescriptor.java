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
package org.jkiss.dbeaver.ui.editors.sql.addins;

import org.eclipse.core.expressions.Expression;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class SQLEditorQuickFixProcessorDescriptor extends AbstractDescriptor {

    private static final Log log = Log.getLog(SQLEditorQuickAssistProcessor.class);

    @NotNull
    private final String id;
    @NotNull
    private final ObjectType implClass;
    @Nullable
    private final Expression enabledWhen;
    @Nullable
    private final Set<String> handledMarkerTypes;

    SQLEditorQuickFixProcessorDescriptor(IConfigurationElement config) {
        super(config);
        this.id = config.getAttribute("id");
        this.implClass = new ObjectType(config.getAttribute("class"));
        this.enabledWhen = getEnablementExpression(config);

        IConfigurationElement[] handledMarkerTypes = config.getChildren("handledMarkerTypes");
        this.handledMarkerTypes = ArrayUtils.isEmpty(handledMarkerTypes)
            ? null
            : Arrays.stream(handledMarkerTypes)
                .map(e -> e.getAttribute("id"))
                .filter(CommonUtils::isNotEmpty)
                .collect(Collectors.toUnmodifiableSet());
    }

    @NotNull
    public String getId() {
        return id;
    }

    public boolean isEnabled(@NotNull IWorkbenchSite site) {
        return isExpressionTrue(this.enabledWhen, site);
    }

    @Nullable
    public Set<String> getHandledMarkerTypes() {
        return this.handledMarkerTypes;
    }

    @NotNull
    public IQuickAssistProcessor createInstance() throws DBException {
        return implClass.createInstance(IQuickAssistProcessor.class);
    }

    @Override
    public String toString() {
        return "SQLEditorQuickFixProcessorDescriptor[id: " + id + ", class: " + implClass.getImplName() + "]"; //$NON-NLS-1$
    }

    public boolean handlesAnnotation(@NotNull Annotation annotation) {
        if (this.handledMarkerTypes == null) {
            return true;
        } else if (annotation instanceof MarkerAnnotation markerAnnotation) {
            try {
                IMarker marker = markerAnnotation.getMarker();
                if (this.handledMarkerTypes.contains(marker.getType())) {
                    return true;
                }
                return this.handledMarkerTypes.stream().anyMatch(t -> {
                    try {
                        return marker.isSubtypeOf(t);
                    } catch (CoreException e) {
                        log.error(e);
                        return false;
                    }
                });
            } catch (CoreException e) {
                log.error(e);
                return false;
            }
        }
        return false;
    }
}
