/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.data;

import oracle.xdb.XMLType;
import org.eclipse.core.resources.IFile;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.StringContentStorage;
import org.jkiss.dbeaver.model.impl.TemporaryContentStorage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * XML content
 */
public class OracleContentXML extends OracleContentOpaque<XMLType> {
    public OracleContentXML(XMLType opaque)
    {
        super(opaque);
    }

    @Override
    protected String getOpaqueType()
    {
        return "XML";
    }

    @Override
    protected OracleContentOpaque createNewContent()
    {
        return new OracleContentXML(null);
    }

    @Override
    protected XMLType createNewOracleObject(Connection connection) throws DBCException, IOException, SQLException
    {
        final InputStream contentStream = storage.getContentStream();
        try {
            return XMLType.createXML(connection, contentStream);
        } finally {
            ContentUtils.close(contentStream);
        }
    }

    @Override
    protected DBDContentStorage makeStorageFromOpaque(DBRProgressMonitor monitor, XMLType opaque) throws DBCException
    {
        long contentLength = opaque.getLength();
        if (contentLength < 4000) {
            try {
                return new StringContentStorage(opaque.getStringVal());
            } catch (SQLException e) {
                throw new DBCException(e);
            }
        } else {
            // Create new local storage
            IFile tempFile;
            try {
                tempFile = ContentUtils.createTempContentFile(monitor, "opaque" + opaque.hashCode());
            }
            catch (IOException e) {
                throw new DBCException(e);
            }
            try {
                ContentUtils.copyReaderToFile(monitor, opaque.getClobVal().getCharacterStream(), contentLength, null, tempFile);
            } catch (Exception e) {
                ContentUtils.deleteTempFile(monitor, tempFile);
                throw new DBCException(e);
            }
            return new TemporaryContentStorage(tempFile);
        }
    }
}
