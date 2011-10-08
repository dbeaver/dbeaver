/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.edit;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.oracle.model.OracleDataType;
import org.jkiss.dbeaver.ext.oracle.model.OracleSchema;
import org.jkiss.dbeaver.ext.oracle.model.OracleUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCObjectEditor;
import org.jkiss.dbeaver.ui.dialogs.struct.CreateEntityDialog;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * OracleDataTypeManager
 */
public class OracleDataTypeManager extends JDBCObjectEditor<OracleDataType, OracleSchema> {

    @Override
    protected OracleDataType createDatabaseObject(IWorkbenchWindow workbenchWindow, IEditorPart activeEditor, DBECommandContext context, OracleSchema parent, Object copyFrom)
    {
        CreateEntityDialog dialog = new CreateEntityDialog(workbenchWindow.getShell(), parent.getDataSource(), "Package");
        if (dialog.open() != IDialogConstants.OK_ID) {
            return null;
        }
        OracleDataType dataType = new OracleDataType(
            parent,
            dialog.getEntityName(),
            false);
        dataType.setSourceDeclaration("TYPE " + dataType.getName() + " AS OBJECT\n" +
            "(\n" +
            ")");
        return dataType;
    }

    @Override
    protected IDatabasePersistAction[] makeObjectCreateActions(ObjectCreateCommand objectCreateCommand)
    {
        return createOrReplaceProcedureQuery(objectCreateCommand.getObject());
    }

    @Override
    protected IDatabasePersistAction[] makeObjectDeleteActions(ObjectDeleteCommand objectDeleteCommand)
    {
        final OracleDataType object = objectDeleteCommand.getObject();
        return new IDatabasePersistAction[] {
            new AbstractDatabasePersistAction("Drop data type",
                "DROP TYPE " + object.getFullQualifiedName())
        };
    }

    @Override
    protected IDatabasePersistAction[] makeObjectModifyActions(ObjectChangeCommand objectChangeCommand)
    {
        return createOrReplaceProcedureQuery(objectChangeCommand.getObject());
    }

    public long getMakerOptions()
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    private IDatabasePersistAction[] createOrReplaceProcedureQuery(OracleDataType dataType)
    {
        List<IDatabasePersistAction> actions = new ArrayList<IDatabasePersistAction>();
        String header = OracleUtils.normalizeSourceName(dataType, false);
        if (!CommonUtils.isEmpty(header)) {
            actions.add(
                new AbstractDatabasePersistAction(
                    "Create type header",
                    "CREATE OR REPLACE " + header));
        }
        String body = OracleUtils.normalizeSourceName(dataType, true);
        if (!CommonUtils.isEmpty(body)) {
            actions.add(
                new AbstractDatabasePersistAction(
                    "Create type body",
                    "CREATE OR REPLACE " + body));
        }
        return actions.toArray(new IDatabasePersistAction[actions.size()]);
    }

/*
    public ITabDescriptor[] getTabDescriptors(IWorkbenchWindow workbenchWindow, final IDatabaseNodeEditor activeEditor, final OracleDataType object)
    {
        List<ITabDescriptor> tabs = new ArrayList<ITabDescriptor>();
        if (!object.isPredefined() || object.hasAttributes() || object.hasMethods()) {
            tabs.add(
                new PropertyTabDescriptor(
                    PropertiesContributor.CATEGORY_INFO,
                    "type.declaration",
                    "Declaration",
                    DBIcon.SOURCES.getImage(),
                    new SectionDescriptor("default", "Declaration") {
                        public ISection getSectionClass()
                        {
                            return new OracleSourceViewSection(activeEditor, false);
                        }
                    }));
        }

        if (object.hasMethods()) {
            tabs.add(new PropertyTabDescriptor(
                PropertiesContributor.CATEGORY_INFO,
                "type.definition",
                "Definition",
                DBIcon.SOURCES.getImage(),
                new SectionDescriptor("default", "Definition") {
                    public ISection getSectionClass()
                    {
                        return new OracleSourceViewSection(activeEditor, true);
                    }
                }));
        }
        return tabs.toArray(new ITabDescriptor[tabs.size()]);
    }
*/
}

