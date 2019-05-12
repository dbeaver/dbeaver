
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2008;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for PhysicalOpType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="PhysicalOpType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="Assert"/>
 *     &lt;enumeration value="Bitmap"/>
 *     &lt;enumeration value="Clustered Index Delete"/>
 *     &lt;enumeration value="Clustered Index Insert"/>
 *     &lt;enumeration value="Clustered Index Scan"/>
 *     &lt;enumeration value="Clustered Index Seek"/>
 *     &lt;enumeration value="Clustered Index Update"/>
 *     &lt;enumeration value="Clustered Index Merge"/>
 *     &lt;enumeration value="Clustered Update"/>
 *     &lt;enumeration value="Collapse"/>
 *     &lt;enumeration value="Compute Scalar"/>
 *     &lt;enumeration value="Concatenation"/>
 *     &lt;enumeration value="Constant Scan"/>
 *     &lt;enumeration value="Deleted Scan"/>
 *     &lt;enumeration value="Filter"/>
 *     &lt;enumeration value="Generic"/>
 *     &lt;enumeration value="Hash Match"/>
 *     &lt;enumeration value="Index Delete"/>
 *     &lt;enumeration value="Index Insert"/>
 *     &lt;enumeration value="Index Scan"/>
 *     &lt;enumeration value="Index Seek"/>
 *     &lt;enumeration value="Index Spool"/>
 *     &lt;enumeration value="Index Update"/>
 *     &lt;enumeration value="Inserted Scan"/>
 *     &lt;enumeration value="Log Row Scan"/>
 *     &lt;enumeration value="Merge Interval"/>
 *     &lt;enumeration value="Merge Join"/>
 *     &lt;enumeration value="Nested Loops"/>
 *     &lt;enumeration value="Online Index Insert"/>
 *     &lt;enumeration value="Parallelism"/>
 *     &lt;enumeration value="Parameter Table Scan"/>
 *     &lt;enumeration value="Print"/>
 *     &lt;enumeration value="Remote Delete"/>
 *     &lt;enumeration value="Remote Index Scan"/>
 *     &lt;enumeration value="Remote Index Seek"/>
 *     &lt;enumeration value="Remote Insert"/>
 *     &lt;enumeration value="Remote Query"/>
 *     &lt;enumeration value="Remote Scan"/>
 *     &lt;enumeration value="Remote Update"/>
 *     &lt;enumeration value="RID Lookup"/>
 *     &lt;enumeration value="Row Count Spool"/>
 *     &lt;enumeration value="Segment"/>
 *     &lt;enumeration value="Sequence"/>
 *     &lt;enumeration value="Sequence Project"/>
 *     &lt;enumeration value="Sort"/>
 *     &lt;enumeration value="Split"/>
 *     &lt;enumeration value="Stream Aggregate"/>
 *     &lt;enumeration value="Switch"/>
 *     &lt;enumeration value="Table-valued function"/>
 *     &lt;enumeration value="Table Delete"/>
 *     &lt;enumeration value="Table Insert"/>
 *     &lt;enumeration value="Table Scan"/>
 *     &lt;enumeration value="Table Spool"/>
 *     &lt;enumeration value="Table Update"/>
 *     &lt;enumeration value="Table Merge"/>
 *     &lt;enumeration value="Top"/>
 *     &lt;enumeration value="UDX"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "PhysicalOpType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
@XmlEnum
public enum PhysicalOpType_sql2008 {

    @XmlEnumValue("Assert")
    ASSERT("Assert"),
    @XmlEnumValue("Bitmap")
    BITMAP("Bitmap"),
    @XmlEnumValue("Clustered Index Delete")
    CLUSTERED_INDEX_DELETE("Clustered Index Delete"),
    @XmlEnumValue("Clustered Index Insert")
    CLUSTERED_INDEX_INSERT("Clustered Index Insert"),
    @XmlEnumValue("Clustered Index Scan")
    CLUSTERED_INDEX_SCAN("Clustered Index Scan"),
    @XmlEnumValue("Clustered Index Seek")
    CLUSTERED_INDEX_SEEK("Clustered Index Seek"),
    @XmlEnumValue("Clustered Index Update")
    CLUSTERED_INDEX_UPDATE("Clustered Index Update"),
    @XmlEnumValue("Clustered Index Merge")
    CLUSTERED_INDEX_MERGE("Clustered Index Merge"),
    @XmlEnumValue("Clustered Update")
    CLUSTERED_UPDATE("Clustered Update"),
    @XmlEnumValue("Collapse")
    COLLAPSE("Collapse"),
    @XmlEnumValue("Compute Scalar")
    COMPUTE_SCALAR("Compute Scalar"),
    @XmlEnumValue("Concatenation")
    CONCATENATION("Concatenation"),
    @XmlEnumValue("Constant Scan")
    CONSTANT_SCAN("Constant Scan"),
    @XmlEnumValue("Deleted Scan")
    DELETED_SCAN("Deleted Scan"),
    @XmlEnumValue("Filter")
    FILTER("Filter"),
    @XmlEnumValue("Generic")
    GENERIC("Generic"),
    @XmlEnumValue("Hash Match")
    HASH_MATCH("Hash Match"),
    @XmlEnumValue("Index Delete")
    INDEX_DELETE("Index Delete"),
    @XmlEnumValue("Index Insert")
    INDEX_INSERT("Index Insert"),
    @XmlEnumValue("Index Scan")
    INDEX_SCAN("Index Scan"),
    @XmlEnumValue("Index Seek")
    INDEX_SEEK("Index Seek"),
    @XmlEnumValue("Index Spool")
    INDEX_SPOOL("Index Spool"),
    @XmlEnumValue("Index Update")
    INDEX_UPDATE("Index Update"),
    @XmlEnumValue("Inserted Scan")
    INSERTED_SCAN("Inserted Scan"),
    @XmlEnumValue("Log Row Scan")
    LOG_ROW_SCAN("Log Row Scan"),
    @XmlEnumValue("Merge Interval")
    MERGE_INTERVAL("Merge Interval"),
    @XmlEnumValue("Merge Join")
    MERGE_JOIN("Merge Join"),
    @XmlEnumValue("Nested Loops")
    NESTED_LOOPS("Nested Loops"),
    @XmlEnumValue("Online Index Insert")
    ONLINE_INDEX_INSERT("Online Index Insert"),
    @XmlEnumValue("Parallelism")
    PARALLELISM("Parallelism"),
    @XmlEnumValue("Parameter Table Scan")
    PARAMETER_TABLE_SCAN("Parameter Table Scan"),
    @XmlEnumValue("Print")
    PRINT("Print"),
    @XmlEnumValue("Remote Delete")
    REMOTE_DELETE("Remote Delete"),
    @XmlEnumValue("Remote Index Scan")
    REMOTE_INDEX_SCAN("Remote Index Scan"),
    @XmlEnumValue("Remote Index Seek")
    REMOTE_INDEX_SEEK("Remote Index Seek"),
    @XmlEnumValue("Remote Insert")
    REMOTE_INSERT("Remote Insert"),
    @XmlEnumValue("Remote Query")
    REMOTE_QUERY("Remote Query"),
    @XmlEnumValue("Remote Scan")
    REMOTE_SCAN("Remote Scan"),
    @XmlEnumValue("Remote Update")
    REMOTE_UPDATE("Remote Update"),
    @XmlEnumValue("RID Lookup")
    RID_LOOKUP("RID Lookup"),
    @XmlEnumValue("Row Count Spool")
    ROW_COUNT_SPOOL("Row Count Spool"),
    @XmlEnumValue("Segment")
    SEGMENT("Segment"),
    @XmlEnumValue("Sequence")
    SEQUENCE("Sequence"),
    @XmlEnumValue("Sequence Project")
    SEQUENCE_PROJECT("Sequence Project"),
    @XmlEnumValue("Sort")
    SORT("Sort"),
    @XmlEnumValue("Split")
    SPLIT("Split"),
    @XmlEnumValue("Stream Aggregate")
    STREAM_AGGREGATE("Stream Aggregate"),
    @XmlEnumValue("Switch")
    SWITCH("Switch"),
    @XmlEnumValue("Table-valued function")
    TABLE_VALUED_FUNCTION("Table-valued function"),
    @XmlEnumValue("Table Delete")
    TABLE_DELETE("Table Delete"),
    @XmlEnumValue("Table Insert")
    TABLE_INSERT("Table Insert"),
    @XmlEnumValue("Table Scan")
    TABLE_SCAN("Table Scan"),
    @XmlEnumValue("Table Spool")
    TABLE_SPOOL("Table Spool"),
    @XmlEnumValue("Table Update")
    TABLE_UPDATE("Table Update"),
    @XmlEnumValue("Table Merge")
    TABLE_MERGE("Table Merge"),
    @XmlEnumValue("Top")
    TOP("Top"),
    UDX("UDX");
    private final String value;

    PhysicalOpType_sql2008(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static PhysicalOpType_sql2008 fromValue(String v) {
        for (PhysicalOpType_sql2008 c: PhysicalOpType_sql2008 .values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
