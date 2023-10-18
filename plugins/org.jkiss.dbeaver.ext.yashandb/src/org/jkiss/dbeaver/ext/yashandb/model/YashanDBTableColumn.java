package org.jkiss.dbeaver.ext.yashandb.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableColumn;
import org.jkiss.dbeaver.model.meta.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableColumn;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @Author: donghy
 * @Date: 2022/08
 * @Description:
 */
public class YashanDBTableColumn extends JDBCTableColumn<YashanDBTableBase> implements
        DBSTableColumn, DBSTypedObjectEx, DBSTypedObjectExt3, DBPHiddenObject, DBPNamedObject2,
        DBSTypedObjectExt4<YashanDBDataType>, DBPObjectWithLazyDescription {
    private static final Log log = Log.getLog(YashanDBTableColumn.class);

    private YashanDBDataType type;
    private YashanDBDataTypeModifier typeMod;

    private String comment;
    //    private boolean hidden;
//    private Integer scale;

    public YashanDBTableColumn(YashanDBTableBase table) {
        super(table, false);
    }

    public YashanDBTableColumn(DBRProgressMonitor monitor, YashanDBTableBase table,@NotNull ResultSet dbResult) throws DBException {
        super(table, true);
        setDefaultValue(JDBCUtils.safeGetString(dbResult, "DATA_DEFAULT"));
        setName(JDBCUtils.safeGetString(dbResult, "COLUMN_NAME"));
        setOrdinalPosition(JDBCUtils.safeGetInt(dbResult, "COLUMN_ID"));
        this.typeName = JDBCUtils.safeGetString(dbResult, "DATA_TYPE");
        this.type = YashanDBDataType.resolveDataType(
                monitor,
                getDataSource(),
                JDBCUtils.safeGetString(dbResult, "DATA_TYPE_OWNER"),
                this.typeName);
        this.typeMod = YashanDBDataTypeModifier.resolveTypeModifier(JDBCUtils.safeGetString(dbResult, "DATA_TYPE_MOD"));
        if (this.type != null) {
            this.typeName = type.getFullyQualifiedName(DBPEvaluationContext.DDL);
            this.valueType = type.getTypeID();
        }
        if (typeMod == YashanDBDataTypeModifier.REF) {
            this.valueType = Types.REF;
        }
        setMaxLength(JDBCUtils.safeGetLong(dbResult, "DATA_LENGTH"));
        setRequired(!"Y".equals(JDBCUtils.safeGetString(dbResult, "NULLABLE")));

//        this.scale = JDBCUtils.safeGetInteger(dbResult, "DATA_SCALE");
//        if (this.scale == null || this.scale < 0) {
//            // Scale can be null in case when type was declared without parameters (examples: NUMBER, NUMBER(*), FLOAT)
//            if (this.type != null && this.type.getScale() != null) {
//                this.scale = this.type.getScale();
//            }
//        }

        Integer scale = JDBCUtils.safeGetInteger(dbResult, "DATA_SCALE");
        if (scale == null) {
            // Scale can be null in case when type was declared without parameters (examples: NUMBER, NUMBER(*), FLOAT)
            if (this.type != null && this.type.getScale() != null) {
                scale = this.type.getScale();
            }
        }
        setScale(scale);

        // if typename is bit, use data length as data precision.
        if(typeName.equals("BIT")) {
            setPrecision(CommonUtils.toInt(JDBCUtils.safeGetLong(dbResult, "DATA_LENGTH")));
        }else {
            setPrecision(JDBCUtils.safeGetInteger(dbResult, "DATA_PRECISION"));
        }
        this.typeMod = YashanDBDataTypeModifier.resolveTypeModifier(JDBCUtils.safeGetString(dbResult, "DATA_TYPE_MOD"));
        if (this.type != null) {
            this.typeName = type.getFullyQualifiedName(DBPEvaluationContext.DDL);
            this.valueType = type.getTypeID();
        }
        if (typeMod == YashanDBDataTypeModifier.REF) {
            this.valueType = Types.REF;
        }

//        if (this.scale == null || this.scale < 0) {
//            // Scale can be null in case when type was declared without parameters (examples: NUMBER, NUMBER(*), FLOAT)
//            if (this.type != null && this.type.getScale() != null) {
//                this.scale = this.type.getScale();
//            }
//        }

    }

    @Override
    public YashanDBDataSource getDataSource() {
        return getTable().getDataSource();
    }

    @Override
    public boolean isHidden() {
        return false;
    }

    @Override
    public String getDescription(DBRProgressMonitor monitor) {
        return getComment(monitor);
    }

    @Nullable
    @Override
    public String getDescription() {
        return comment;
    }

    public static class CommentLoadValidator implements IPropertyCacheValidator<YashanDBTableColumn> {
        @Override
        public boolean isPropertyCached(YashanDBTableColumn object, Object propertyId) {
            return object.comment != null;
        }
    }


    /**
     * get Comments.
     */
    @Property(viewable = true, editable = true, updatable = true, length = PropertyLength.MULTILINE, order = 100)
    @LazyProperty(cacheValidator = CommentLoadValidator.class)
    public String getComment(DBRProgressMonitor monitor) {
        if (isPersisted() && comment == null) {
            // Load comments for all table columns
            getTable().loadColumnComments(monitor);
        }
        return comment;
    }


    public void setComment(String comment) {
        this.comment = comment;
    }

    void cacheComment() {
        if (this.comment == null) {
            this.comment = "";
        }
    }


    /**
     * delete valueRenderer which limits the value of ordinalPosition(more than 0).
     */
    // @Property(viewable = true, order = 15, valueRenderer = DBPositiveNumberTransformer.class)
    @Property(viewable = true, order = 15)
    public int getOrdinalPosition() {
        return ordinalPosition;
    }

    public void setOrdinalPosition(int ordinalPosition) {
        this.ordinalPosition = ordinalPosition;
    }

    /**
     * get the list of datatype name of column.
     */
    //@Property(name = "Data Type", viewable = true, editable = true, updatable = true, order = 20, listProvider = ColumnTypeNameListProvider.class)
    @Override
    public String getTypeName() {
        return super.getTypeName();
    }

    @Property(viewable = true, editable = true, updatable = true, order = 20, listProvider = ColumnTypeNameListProvider.class)
    @Override
    public String getFullTypeName() {
        return DBUtils.getFullTypeName(this);
    }

    public void setDataType(YashanDBDataType type) {
        this.type = type;
        this.typeName = type == null ? "" : type.getFullyQualifiedName(DBPEvaluationContext.DDL);
    }

    @Nullable
    @Override
//    @Property(viewable = true, editableExpr = "!object.table.view", updatableExpr = "!object.table.view", order = 21,
//            listProvider = ColumnDataTypeListProvider.class)
    public YashanDBDataType getDataType() {
        return type;
    }

//    public static class ColumnDataTypeListProvider implements IPropertyValueListProvider<YashanDBTableColumn> {
//
//        @Override
//        public boolean allowCustomValue() {
//            return false;
//        }
//
//        @Override
//        public Object[] getPossibleValues(YashanDBTableColumn column) {
//            List<DBSObject> dataTypes = new ArrayList<DBSObject>(column.getTable().getDataSource().getLocalDataTypes());
//            if (!dataTypes.contains(column.getDataType())) {
//                dataTypes.add(column.getDataType());
//            }
//            Collections.sort(dataTypes, DBUtils.nameComparator());
//            return dataTypes.toArray(new DBSDataType[dataTypes.size()]);
//        }
//    }

    @Property(viewable = false, editableExpr = "!object.table.view", updatableExpr = "!object.table.view", order = 40)
    @Override
    public long getMaxLength() {
        return super.getMaxLength();
    }

    /**
     * when changing BIT data type's length, change it.
     */
    @Override
    @Property(viewable = false, editableExpr = "!object.table.view", updatableExpr = "!object.table.view", order = 41)
    public Integer getPrecision() {
        return super.getPrecision();
    }

    @Override
    @Property(viewable = false, editableExpr = "!object.table.view", updatableExpr = "!object.table.view", order = 42)
    public Integer getScale() {
        return super.getScale();
    }

//    @Override
//    public void setScale(Integer scale) {
//        this.scale = scale;
//    }

    /**
     * Not null and AutoIncrement box will be showed in right UI.
     */
    @Property(viewable = true, editableExpr = "!object.table.view", updatableExpr = "!object.table.view", order = 50)
    @Override
    public boolean isRequired() {
        return super.isRequired();
    }

    @Property(viewable = true, editableExpr = "!object.table.view", updatableExpr = "!object.table.view", order = 70)
    @Override
    public String getDefaultValue() {
        return super.getDefaultValue();
    }

    /**
     * Make AutoGenerated item can not be showed in right UI.
     */
    @Override
    public boolean isAutoGenerated() {
        return false;
    }
}
