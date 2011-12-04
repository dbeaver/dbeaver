/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.editors;

import org.jkiss.dbeaver.ext.IDatabaseEditor;
import org.jkiss.dbeaver.ext.oracle.model.OracleProcedureStandalone;

/**
 * OracleProcedureBodySection
 */
public class OracleProcedureBodySection extends OracleSourceViewSection {

    private OracleProcedureStandalone procedure;

    public OracleProcedureBodySection(IDatabaseEditor editor)
    {
        super(editor, false);
        this.procedure = (OracleProcedureStandalone) editor.getEditorInput().getDatabaseObject();
    }

    @Override
    protected boolean isReadOnly()
    {
        return false;
    }

}