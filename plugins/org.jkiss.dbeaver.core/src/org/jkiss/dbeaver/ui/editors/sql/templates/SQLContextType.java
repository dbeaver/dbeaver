/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sebastian Davids: sdavids@gmx.de - see bug 25376
 *******************************************************************************/
package org.jkiss.dbeaver.ui.editors.sql.templates;


import org.eclipse.jface.text.templates.GlobalTemplateVariables;
import org.eclipse.jface.text.templates.TemplateContextType;

public class SQLContextType extends TemplateContextType {

	/**
	 * The context type id for templates working on all Java code locations
	 */
	public static final String ID_SQL= "sql"; //$NON-NLS-1$

    public SQLContextType()
    {
        super("sql", "SQL");
        addGlobalResolvers();
        addDatabaseProposals();
    }

    private void addGlobalResolvers() {
        //addResolver(new GlobalTemplateVariables.Cursor());
        addResolver(new GlobalTemplateVariables.WordSelection());
        addResolver(new GlobalTemplateVariables.LineSelection());
        addResolver(new GlobalTemplateVariables.Dollar());
        addResolver(new GlobalTemplateVariables.Date());
        addResolver(new GlobalTemplateVariables.Year());
        addResolver(new GlobalTemplateVariables.Time());
        addResolver(new GlobalTemplateVariables.User());
    }

    private void addDatabaseProposals()
    {
        addResolver(new SQLEntityResolver());
    }

}
