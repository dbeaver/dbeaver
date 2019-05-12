
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2016;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for LogicalOpType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="LogicalOpType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="Aggregate"/>
 *     &lt;enumeration value="Anti Diff"/>
 *     &lt;enumeration value="Assert"/>
 *     &lt;enumeration value="Async Concat"/>
 *     &lt;enumeration value="Batch Hash Table Build"/>
 *     &lt;enumeration value="Bitmap Create"/>
 *     &lt;enumeration value="Clustered Index Scan"/>
 *     &lt;enumeration value="Clustered Index Seek"/>
 *     &lt;enumeration value="Clustered Update"/>
 *     &lt;enumeration value="Collapse"/>
 *     &lt;enumeration value="Compute Scalar"/>
 *     &lt;enumeration value="Concatenation"/>
 *     &lt;enumeration value="Constant Scan"/>
 *     &lt;enumeration value="Cross Join"/>
 *     &lt;enumeration value="Delete"/>
 *     &lt;enumeration value="Deleted Scan"/>
 *     &lt;enumeration value="Distinct Sort"/>
 *     &lt;enumeration value="Distinct"/>
 *     &lt;enumeration value="Distribute Streams"/>
 *     &lt;enumeration value="Eager Spool"/>
 *     &lt;enumeration value="Filter"/>
 *     &lt;enumeration value="Flow Distinct"/>
 *     &lt;enumeration value="Foreign Key References Check"/>
 *     &lt;enumeration value="Full Outer Join"/>
 *     &lt;enumeration value="Gather Streams"/>
 *     &lt;enumeration value="Generic"/>
 *     &lt;enumeration value="Index Scan"/>
 *     &lt;enumeration value="Index Seek"/>
 *     &lt;enumeration value="Inner Join"/>
 *     &lt;enumeration value="Insert"/>
 *     &lt;enumeration value="Inserted Scan"/>
 *     &lt;enumeration value="Intersect"/>
 *     &lt;enumeration value="Intersect All"/>
 *     &lt;enumeration value="Lazy Spool"/>
 *     &lt;enumeration value="Left Anti Semi Join"/>
 *     &lt;enumeration value="Left Diff"/>
 *     &lt;enumeration value="Left Diff All"/>
 *     &lt;enumeration value="Left Outer Join"/>
 *     &lt;enumeration value="Left Semi Join"/>
 *     &lt;enumeration value="Log Row Scan"/>
 *     &lt;enumeration value="Merge"/>
 *     &lt;enumeration value="Merge Interval"/>
 *     &lt;enumeration value="Merge Stats"/>
 *     &lt;enumeration value="Parameter Table Scan"/>
 *     &lt;enumeration value="Partial Aggregate"/>
 *     &lt;enumeration value="Print"/>
 *     &lt;enumeration value="Put"/>
 *     &lt;enumeration value="Rank"/>
 *     &lt;enumeration value="Remote Delete"/>
 *     &lt;enumeration value="Remote Index Scan"/>
 *     &lt;enumeration value="Remote Index Seek"/>
 *     &lt;enumeration value="Remote Insert"/>
 *     &lt;enumeration value="Remote Query"/>
 *     &lt;enumeration value="Remote Scan"/>
 *     &lt;enumeration value="Remote Update"/>
 *     &lt;enumeration value="Repartition Streams"/>
 *     &lt;enumeration value="RID Lookup"/>
 *     &lt;enumeration value="Right Anti Semi Join"/>
 *     &lt;enumeration value="Right Diff"/>
 *     &lt;enumeration value="Right Diff All"/>
 *     &lt;enumeration value="Right Outer Join"/>
 *     &lt;enumeration value="Right Semi Join"/>
 *     &lt;enumeration value="Segment"/>
 *     &lt;enumeration value="Sequence"/>
 *     &lt;enumeration value="Sort"/>
 *     &lt;enumeration value="Split"/>
 *     &lt;enumeration value="Switch"/>
 *     &lt;enumeration value="Table-valued function"/>
 *     &lt;enumeration value="Table Scan"/>
 *     &lt;enumeration value="Top"/>
 *     &lt;enumeration value="TopN Sort"/>
 *     &lt;enumeration value="UDX"/>
 *     &lt;enumeration value="Union"/>
 *     &lt;enumeration value="Update"/>
 *     &lt;enumeration value="Local Stats"/>
 *     &lt;enumeration value="Window Spool"/>
 *     &lt;enumeration value="Window Aggregate"/>
 *     &lt;enumeration value="Key Lookup"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "LogicalOpType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
@XmlEnum
public enum LogicalOpType_sql2016 {

    @XmlEnumValue("Aggregate")
    AGGREGATE("Aggregate"),
    @XmlEnumValue("Anti Diff")
    ANTI_DIFF("Anti Diff"),
    @XmlEnumValue("Assert")
    ASSERT("Assert"),
    @XmlEnumValue("Async Concat")
    ASYNC_CONCAT("Async Concat"),
    @XmlEnumValue("Batch Hash Table Build")
    BATCH_HASH_TABLE_BUILD("Batch Hash Table Build"),
    @XmlEnumValue("Bitmap Create")
    BITMAP_CREATE("Bitmap Create"),
    @XmlEnumValue("Clustered Index Scan")
    CLUSTERED_INDEX_SCAN("Clustered Index Scan"),
    @XmlEnumValue("Clustered Index Seek")
    CLUSTERED_INDEX_SEEK("Clustered Index Seek"),
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
    @XmlEnumValue("Cross Join")
    CROSS_JOIN("Cross Join"),
    @XmlEnumValue("Delete")
    DELETE("Delete"),
    @XmlEnumValue("Deleted Scan")
    DELETED_SCAN("Deleted Scan"),
    @XmlEnumValue("Distinct Sort")
    DISTINCT_SORT("Distinct Sort"),
    @XmlEnumValue("Distinct")
    DISTINCT("Distinct"),
    @XmlEnumValue("Distribute Streams")
    DISTRIBUTE_STREAMS("Distribute Streams"),
    @XmlEnumValue("Eager Spool")
    EAGER_SPOOL("Eager Spool"),
    @XmlEnumValue("Filter")
    FILTER("Filter"),
    @XmlEnumValue("Flow Distinct")
    FLOW_DISTINCT("Flow Distinct"),
    @XmlEnumValue("Foreign Key References Check")
    FOREIGN_KEY_REFERENCES_CHECK("Foreign Key References Check"),
    @XmlEnumValue("Full Outer Join")
    FULL_OUTER_JOIN("Full Outer Join"),
    @XmlEnumValue("Gather Streams")
    GATHER_STREAMS("Gather Streams"),
    @XmlEnumValue("Generic")
    GENERIC("Generic"),
    @XmlEnumValue("Index Scan")
    INDEX_SCAN("Index Scan"),
    @XmlEnumValue("Index Seek")
    INDEX_SEEK("Index Seek"),
    @XmlEnumValue("Inner Join")
    INNER_JOIN("Inner Join"),
    @XmlEnumValue("Insert")
    INSERT("Insert"),
    @XmlEnumValue("Inserted Scan")
    INSERTED_SCAN("Inserted Scan"),
    @XmlEnumValue("Intersect")
    INTERSECT("Intersect"),
    @XmlEnumValue("Intersect All")
    INTERSECT_ALL("Intersect All"),
    @XmlEnumValue("Lazy Spool")
    LAZY_SPOOL("Lazy Spool"),
    @XmlEnumValue("Left Anti Semi Join")
    LEFT_ANTI_SEMI_JOIN("Left Anti Semi Join"),
    @XmlEnumValue("Left Diff")
    LEFT_DIFF("Left Diff"),
    @XmlEnumValue("Left Diff All")
    LEFT_DIFF_ALL("Left Diff All"),
    @XmlEnumValue("Left Outer Join")
    LEFT_OUTER_JOIN("Left Outer Join"),
    @XmlEnumValue("Left Semi Join")
    LEFT_SEMI_JOIN("Left Semi Join"),
    @XmlEnumValue("Log Row Scan")
    LOG_ROW_SCAN("Log Row Scan"),
    @XmlEnumValue("Merge")
    MERGE("Merge"),
    @XmlEnumValue("Merge Interval")
    MERGE_INTERVAL("Merge Interval"),
    @XmlEnumValue("Merge Stats")
    MERGE_STATS("Merge Stats"),
    @XmlEnumValue("Parameter Table Scan")
    PARAMETER_TABLE_SCAN("Parameter Table Scan"),
    @XmlEnumValue("Partial Aggregate")
    PARTIAL_AGGREGATE("Partial Aggregate"),
    @XmlEnumValue("Print")
    PRINT("Print"),
    @XmlEnumValue("Put")
    PUT("Put"),
    @XmlEnumValue("Rank")
    RANK("Rank"),
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
    @XmlEnumValue("Repartition Streams")
    REPARTITION_STREAMS("Repartition Streams"),
    @XmlEnumValue("RID Lookup")
    RID_LOOKUP("RID Lookup"),
    @XmlEnumValue("Right Anti Semi Join")
    RIGHT_ANTI_SEMI_JOIN("Right Anti Semi Join"),
    @XmlEnumValue("Right Diff")
    RIGHT_DIFF("Right Diff"),
    @XmlEnumValue("Right Diff All")
    RIGHT_DIFF_ALL("Right Diff All"),
    @XmlEnumValue("Right Outer Join")
    RIGHT_OUTER_JOIN("Right Outer Join"),
    @XmlEnumValue("Right Semi Join")
    RIGHT_SEMI_JOIN("Right Semi Join"),
    @XmlEnumValue("Segment")
    SEGMENT("Segment"),
    @XmlEnumValue("Sequence")
    SEQUENCE("Sequence"),
    @XmlEnumValue("Sort")
    SORT("Sort"),
    @XmlEnumValue("Split")
    SPLIT("Split"),
    @XmlEnumValue("Switch")
    SWITCH("Switch"),
    @XmlEnumValue("Table-valued function")
    TABLE_VALUED_FUNCTION("Table-valued function"),
    @XmlEnumValue("Table Scan")
    TABLE_SCAN("Table Scan"),
    @XmlEnumValue("Top")
    TOP("Top"),
    @XmlEnumValue("TopN Sort")
    TOP_N_SORT("TopN Sort"),
    UDX("UDX"),
    @XmlEnumValue("Union")
    UNION("Union"),
    @XmlEnumValue("Update")
    UPDATE("Update"),
    @XmlEnumValue("Local Stats")
    LOCAL_STATS("Local Stats"),
    @XmlEnumValue("Window Spool")
    WINDOW_SPOOL("Window Spool"),
    @XmlEnumValue("Window Aggregate")
    WINDOW_AGGREGATE("Window Aggregate"),
    @XmlEnumValue("Key Lookup")
    KEY_LOOKUP("Key Lookup");
    private final String value;

    LogicalOpType_sql2016(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static LogicalOpType_sql2016 fromValue(String v) {
        for (LogicalOpType_sql2016 c: LogicalOpType_sql2016 .values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
