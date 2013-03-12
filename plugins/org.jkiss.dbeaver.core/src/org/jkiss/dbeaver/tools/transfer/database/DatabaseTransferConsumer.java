package org.jkiss.dbeaver.tools.transfer.database;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.tools.transfer.IDataTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProcessor;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
* Stream transfer consumer
*/
public class DatabaseTransferConsumer implements IDataTransferConsumer<DatabaseConsumerSettings, IDataTransferProcessor> {

    static final Log log = LogFactory.getLog(DatabaseTransferConsumer.class);

    private DBSDataContainer sourceObject;
    private DatabaseConsumerSettings settings;

    public DatabaseTransferConsumer()
    {
    }

    @Override
    public void fetchStart(DBCExecutionContext context, DBCResultSet resultSet) throws DBCException
    {
        initExporter(context);

    }

    @Override
    public void fetchRow(DBCExecutionContext context, DBCResultSet resultSet) throws DBCException
    {
    }

    @Override
    public void fetchEnd(DBCExecutionContext context) throws DBCException
    {
        closeExporter();
    }

    @Override
    public void close()
    {
    }

    private void initExporter(DBCExecutionContext context) throws DBCException
    {
    }

    private void closeExporter()
    {
    }

    @Override
    public void initTransfer(DBSObject sourceObject, DatabaseConsumerSettings settings, IDataTransferProcessor processor, Map<Object, Object> processorProperties)
    {
        this.sourceObject = (DBSDataContainer)sourceObject;
        this.settings = settings;
    }

    @Override
    public void startTransfer(DBRProgressMonitor monitor) throws DBException
    {
        // Create all necessary database objects
        DBSObjectContainer container = settings.getContainer();
        monitor.beginTask("Create necessary database objects", 1);
        try {
            boolean hasNewObjects = false;
            for (DatabaseMappingContainer containerMapping : settings.getDataMappings().values()) {
                switch (containerMapping.getMappingType()) {
                    case create:
                        createTargetTable(monitor, containerMapping);
                        hasNewObjects = true;
                        break;
                    case existing:
                        for (DatabaseMappingAttribute attr : containerMapping.getAttributeMappings(monitor)) {
                            if (attr.getMappingType() == DatabaseMappingType.create) {
                                createTargetAttribute(monitor, attr);
                                hasNewObjects = true;
                            }
                        }
                        break;
                }
            }
            if (hasNewObjects) {
                // Refresh node
                monitor.subTask("Refresh navigator model");
                settings.getContainerNode().refreshNode(monitor, this);

                // Reflect database changes in mappings
            }
        }
        finally {
            monitor.done();
        }
/*
        if (container != null) {
            try {
                DBEObjectManager<?> objectManager = DBeaverCore.getInstance().getEditorsRegistry().getObjectManager(container.getChildType(VoidProgressMonitor.INSTANCE));
                if (objectManager instanceof DBEObjectMaker<?, ?>) {
                    //((DBEObjectMaker) objectManager).createNewObject()
                }
            } catch (DBException e) {
                log.error(e);
            }
        }
*/

    }

    private void createTargetTable(DBRProgressMonitor monitor, DatabaseMappingContainer containerMapping) throws DBException
    {
        monitor.subTask("Create table " + containerMapping.getTargetName());
        StringBuilder sql = new StringBuilder(500);
        DBSObjectContainer schema = settings.getContainer();
        DBPDataSource targetDataSource = schema.getDataSource();

        String tableName = containerMapping.getTargetName();
        sql.append("CREATE TABLE ")
            .append(DBUtils.getQuotedIdentifier(schema))
            .append(targetDataSource.getInfo().getCatalogSeparator())
            .append(DBUtils.getQuotedIdentifier(targetDataSource, tableName))
            .append("(\n");
        Map<DBSAttributeBase, DatabaseMappingAttribute> mappedAttrs = new HashMap<DBSAttributeBase, DatabaseMappingAttribute>();
        for (DatabaseMappingAttribute attr : containerMapping.getAttributeMappings(monitor)) {
            if (attr.getMappingType() != DatabaseMappingType.create) {
                continue;
            }
            if (!mappedAttrs.isEmpty()) sql.append(",\n");
            appendAttributeClause(sql, attr);
            mappedAttrs.put(attr.getSource(), attr);
        }
        if (containerMapping.getSource() instanceof DBSEntity) {
            // Make primary key
            Collection<? extends DBSEntityAttribute> identifier = DBUtils.getBestTableIdentifier(monitor, (DBSEntity) containerMapping.getSource());
            if (!CommonUtils.isEmpty(identifier)) {
                boolean idMapped = true;
                for (DBSEntityAttribute idAttr : identifier) {
                    if (!mappedAttrs.containsKey(idAttr)) {
                        idMapped = false;
                        break;
                    }
                }
                if (idMapped) {
                    sql.append(",\nPRIMARY KEY (");
                    boolean hasAttr = false;
                    for (DBSEntityAttribute idAttr : identifier) {
                        DatabaseMappingAttribute mappedAttr = mappedAttrs.get(idAttr);
                        if (hasAttr) sql.append(",");
                        sql.append(mappedAttr.getTargetName());
                        hasAttr = true;
                    }
                    sql.append(")\n");
                }
            }
        }
        sql.append(")");
        executeSQL(monitor, targetDataSource, sql.toString());
    }

    private static void appendAttributeClause(StringBuilder sql, DatabaseMappingAttribute attr)
    {
        sql.append(attr.getTargetName()).append(" ").append(attr.getTargetType());
        if (attr.source.isRequired()) sql.append(" NOT NULL");
    }

    private void createTargetAttribute(DBRProgressMonitor monitor, DatabaseMappingAttribute attribute) throws DBCException
    {
        monitor.subTask("Create column " + DBUtils.getObjectFullName(attribute.getParent().getTarget()) + "." + attribute.getTargetName());
        StringBuilder sql = new StringBuilder(500);
        sql.append("ALTER TABLE ").append(DBUtils.getObjectFullName(attribute.getParent().getTarget()))
            .append(" ADD ");
        appendAttributeClause(sql, attribute);
        executeSQL(monitor, attribute.getParent().getTarget().getDataSource(), sql.toString());
    }

    private void executeSQL(DBRProgressMonitor monitor, DBPDataSource dataSource, String sql)
        throws DBCException
    {
        DBCExecutionContext context = dataSource.openContext(monitor, DBCExecutionPurpose.UTIL, "Create target metadata");
        try {
            DBCStatement dbStat = DBUtils.prepareStatement(context, sql);
            try {
                dbStat.executeStatement();
            } finally {
                dbStat.close();
            }
        }
        finally {
            context.close();
        }
    }

    @Override
    public void finishTransfer()
    {
    }

    @Override
    public String getTargetName()
    {
        DatabaseMappingContainer dataMapping = settings.getDataMapping(sourceObject);
        if (dataMapping == null) {
            return "?";
        }

        switch (dataMapping.getMappingType()) {
            case create: return dataMapping.getTargetName() + " [Create]";
            case existing: return dataMapping.getTargetName() + " [Insert]";
            case skip: return "[Skip]";
            default: return "?";
        }
    }

}
