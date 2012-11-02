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


import org.eclipse.jface.text.templates.TemplateContextType;

public class SQLContextType extends TemplateContextType {

	/**
	 * The context type id for templates working on all Java code locations
	 */
	public static final String ID_SQL= "sql"; //$NON-NLS-1$

    public SQLContextType()
    {
        super("sql", "SQL");
    }

/*
    public CompilationUnitContext createContext(IDocument document, int offset, int length, ICompilationUnit compilationUnit) {
        JavaContext javaContext= new JavaContext(this, document, offset, length, compilationUnit);
        initializeContext(javaContext);
        return javaContext;
    }

    */
/*
      * @see org.eclipse.jdt.internal.corext.template.java.CompilationUnitContextType#createContext(org.eclipse.jface.text.IDocument, org.eclipse.jface.text.Position, org.eclipse.jdt.core.ICompilationUnit)
      *//*

    @Override
    public CompilationUnitContext createContext(IDocument document, Position completionPosition, ICompilationUnit compilationUnit) {
        JavaContext javaContext= new JavaContext(this, document, completionPosition, compilationUnit);
        initializeContext(javaContext);
        return javaContext;
    }
*/

}
