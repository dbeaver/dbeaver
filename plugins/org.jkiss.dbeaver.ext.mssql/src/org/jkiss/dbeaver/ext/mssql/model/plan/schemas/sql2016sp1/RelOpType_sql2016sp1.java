
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2016sp1;

import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for RelOpType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="RelOpType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="OutputList" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ColumnReferenceListType"/>
 *         &lt;element name="Warnings" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}WarningsType" minOccurs="0"/>
 *         &lt;element name="MemoryFractions" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}MemoryFractionsType" minOccurs="0"/>
 *         &lt;element name="RunTimeInformation" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RunTimeInformationType" minOccurs="0"/>
 *         &lt;element name="RunTimePartitionSummary" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RunTimePartitionSummaryType" minOccurs="0"/>
 *         &lt;element name="InternalInfo" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}InternalInfoType" minOccurs="0"/>
 *         &lt;choice>
 *           &lt;element name="Assert" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}FilterType"/>
 *           &lt;element name="BatchHashTableBuild" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}BatchHashTableBuildType"/>
 *           &lt;element name="Bitmap" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}BitmapType"/>
 *           &lt;element name="Collapse" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}CollapseType"/>
 *           &lt;element name="ComputeScalar" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ComputeScalarType"/>
 *           &lt;element name="Concat" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ConcatType"/>
 *           &lt;element name="ConstantScan" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ConstantScanType"/>
 *           &lt;element name="CreateIndex" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}CreateIndexType"/>
 *           &lt;element name="DeletedScan" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RowsetType"/>
 *           &lt;element name="Extension" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}UDXType"/>
 *           &lt;element name="Filter" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}FilterType"/>
 *           &lt;element name="ForeignKeyReferencesCheck" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ForeignKeyReferencesCheckType"/>
 *           &lt;element name="Generic" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}GenericType"/>
 *           &lt;element name="Hash" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}HashType"/>
 *           &lt;element name="IndexScan" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}IndexScanType"/>
 *           &lt;element name="InsertedScan" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RowsetType"/>
 *           &lt;element name="LogRowScan" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RelOpBaseType"/>
 *           &lt;element name="Merge" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}MergeType"/>
 *           &lt;element name="MergeInterval" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}SimpleIteratorOneChildType"/>
 *           &lt;element name="NestedLoops" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}NestedLoopsType"/>
 *           &lt;element name="OnlineIndex" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}CreateIndexType"/>
 *           &lt;element name="Parallelism" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ParallelismType"/>
 *           &lt;element name="ParameterTableScan" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RelOpBaseType"/>
 *           &lt;element name="PrintDataflow" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RelOpBaseType"/>
 *           &lt;element name="Put" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}PutType"/>
 *           &lt;element name="RemoteFetch" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RemoteFetchType"/>
 *           &lt;element name="RemoteModify" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RemoteModifyType"/>
 *           &lt;element name="RemoteQuery" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RemoteQueryType"/>
 *           &lt;element name="RemoteRange" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RemoteRangeType"/>
 *           &lt;element name="RemoteScan" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RemoteType"/>
 *           &lt;element name="RowCountSpool" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}SpoolType"/>
 *           &lt;element name="ScalarInsert" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ScalarInsertType"/>
 *           &lt;element name="Segment" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}SegmentType"/>
 *           &lt;element name="Sequence" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}SequenceType"/>
 *           &lt;element name="SequenceProject" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ComputeScalarType"/>
 *           &lt;element name="SimpleUpdate" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}SimpleUpdateType"/>
 *           &lt;element name="Sort" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}SortType"/>
 *           &lt;element name="Split" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}SplitType"/>
 *           &lt;element name="Spool" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}SpoolType"/>
 *           &lt;element name="StreamAggregate" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}StreamAggregateType"/>
 *           &lt;element name="Switch" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}SwitchType"/>
 *           &lt;element name="TableScan" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}TableScanType"/>
 *           &lt;element name="TableValuedFunction" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}TableValuedFunctionType"/>
 *           &lt;element name="Top" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}TopType"/>
 *           &lt;element name="TopSort" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}TopSortType"/>
 *           &lt;element name="Update" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}UpdateType"/>
 *           &lt;element name="WindowSpool" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}WindowType"/>
 *           &lt;element name="WindowAggregate" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}WindowAggregateType"/>
 *         &lt;/choice>
 *       &lt;/sequence>
 *       &lt;attribute name="AvgRowSize" use="required" type="{http://www.w3.org/2001/XMLSchema}double" />
 *       &lt;attribute name="EstimateCPU" use="required" type="{http://www.w3.org/2001/XMLSchema}double" />
 *       &lt;attribute name="EstimateIO" use="required" type="{http://www.w3.org/2001/XMLSchema}double" />
 *       &lt;attribute name="EstimateRebinds" use="required" type="{http://www.w3.org/2001/XMLSchema}double" />
 *       &lt;attribute name="EstimateRewinds" use="required" type="{http://www.w3.org/2001/XMLSchema}double" />
 *       &lt;attribute name="EstimatedExecutionMode" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ExecutionModeType" />
 *       &lt;attribute name="GroupExecuted" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="EstimateRows" use="required" type="{http://www.w3.org/2001/XMLSchema}double" />
 *       &lt;attribute name="EstimatedRowsRead" type="{http://www.w3.org/2001/XMLSchema}double" />
 *       &lt;attribute name="LogicalOp" use="required" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}LogicalOpType" />
 *       &lt;attribute name="NodeId" use="required" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="Parallel" use="required" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="RemoteDataAccess" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="Partitioned" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="PhysicalOp" use="required" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}PhysicalOpType" />
 *       &lt;attribute name="EstimatedTotalSubtreeCost" use="required" type="{http://www.w3.org/2001/XMLSchema}double" />
 *       &lt;attribute name="TableCardinality" type="{http://www.w3.org/2001/XMLSchema}double" />
 *       &lt;attribute name="StatsCollectionId" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RelOpType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "outputList",
    "warnings",
    "memoryFractions",
    "runTimeInformation",
    "runTimePartitionSummary",
    "internalInfo",
    "_assert",
    "batchHashTableBuild",
    "bitmap",
    "collapse",
    "computeScalar",
    "concat",
    "constantScan",
    "createIndex",
    "deletedScan",
    "extension",
    "filter",
    "foreignKeyReferencesCheck",
    "generic",
    "hash",
    "indexScan",
    "insertedScan",
    "logRowScan",
    "merge",
    "mergeInterval",
    "nestedLoops",
    "onlineIndex",
    "parallelism",
    "parameterTableScan",
    "printDataflow",
    "put",
    "remoteFetch",
    "remoteModify",
    "remoteQuery",
    "remoteRange",
    "remoteScan",
    "rowCountSpool",
    "scalarInsert",
    "segment",
    "sequence",
    "sequenceProject",
    "simpleUpdate",
    "sort",
    "split",
    "spool",
    "streamAggregate",
    "_switch",
    "tableScan",
    "tableValuedFunction",
    "top",
    "topSort",
    "update",
    "windowSpool",
    "windowAggregate"
})
public class RelOpType_sql2016sp1 {

    @XmlElement(name = "OutputList", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
    protected ColumnReferenceListType_sql2016sp1 outputList;
    @XmlElement(name = "Warnings", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected WarningsType_sql2016sp1 warnings;
    @XmlElement(name = "MemoryFractions", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected MemoryFractionsType_sql2016sp1 memoryFractions;
    @XmlElement(name = "RunTimeInformation", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected RunTimeInformationType_sql2016sp1 runTimeInformation;
    @XmlElement(name = "RunTimePartitionSummary", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected RunTimePartitionSummaryType_sql2016sp1 runTimePartitionSummary;
    @XmlElement(name = "InternalInfo", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected InternalInfoType_sql2016sp1 internalInfo;
    @XmlElement(name = "Assert", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected FilterType_sql2016sp1 _assert;
    @XmlElement(name = "BatchHashTableBuild", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected BatchHashTableBuildType_sql2016sp1 batchHashTableBuild;
    @XmlElement(name = "Bitmap", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected BitmapType_sql2016sp1 bitmap;
    @XmlElement(name = "Collapse", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected CollapseType_sql2016sp1 collapse;
    @XmlElement(name = "ComputeScalar", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ComputeScalarType_sql2016sp1 computeScalar;
    @XmlElement(name = "Concat", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ConcatType_sql2016sp1 concat;
    @XmlElement(name = "ConstantScan", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ConstantScanType_sql2016sp1 constantScan;
    @XmlElement(name = "CreateIndex", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected CreateIndexType_sql2016sp1 createIndex;
    @XmlElement(name = "DeletedScan", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected RowsetType_sql2016sp1 deletedScan;
    @XmlElement(name = "Extension", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected UDXType_sql2016sp1 extension;
    @XmlElement(name = "Filter", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected FilterType_sql2016sp1 filter;
    @XmlElement(name = "ForeignKeyReferencesCheck", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ForeignKeyReferencesCheckType_sql2016sp1 foreignKeyReferencesCheck;
    @XmlElement(name = "Generic", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected GenericType_sql2016sp1 generic;
    @XmlElement(name = "Hash", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected HashType_sql2016sp1 hash;
    @XmlElement(name = "IndexScan", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected IndexScanType_sql2016sp1 indexScan;
    @XmlElement(name = "InsertedScan", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected RowsetType_sql2016sp1 insertedScan;
    @XmlElement(name = "LogRowScan", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected RelOpBaseType_sql2016sp1 logRowScan;
    @XmlElement(name = "Merge", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected MergeType_sql2016sp1 merge;
    @XmlElement(name = "MergeInterval", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected SimpleIteratorOneChildType_sql2016sp1 mergeInterval;
    @XmlElement(name = "NestedLoops", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected NestedLoopsType_sql2016sp1 nestedLoops;
    @XmlElement(name = "OnlineIndex", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected CreateIndexType_sql2016sp1 onlineIndex;
    @XmlElement(name = "Parallelism", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ParallelismType_sql2016sp1 parallelism;
    @XmlElement(name = "ParameterTableScan", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected RelOpBaseType_sql2016sp1 parameterTableScan;
    @XmlElement(name = "PrintDataflow", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected RelOpBaseType_sql2016sp1 printDataflow;
    @XmlElement(name = "Put", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected PutType_sql2016sp1 put;
    @XmlElement(name = "RemoteFetch", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected RemoteFetchType_sql2016sp1 remoteFetch;
    @XmlElement(name = "RemoteModify", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected RemoteModifyType_sql2016sp1 remoteModify;
    @XmlElement(name = "RemoteQuery", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected RemoteQueryType_sql2016sp1 remoteQuery;
    @XmlElement(name = "RemoteRange", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected RemoteRangeType_sql2016sp1 remoteRange;
    @XmlElement(name = "RemoteScan", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected RemoteType_sql2016sp1 remoteScan;
    @XmlElement(name = "RowCountSpool", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected SpoolType_sql2016sp1 rowCountSpool;
    @XmlElement(name = "ScalarInsert", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ScalarInsertType_sql2016sp1 scalarInsert;
    @XmlElement(name = "Segment", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected SegmentType_sql2016sp1 segment;
    @XmlElement(name = "Sequence", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected SequenceType_sql2016sp1 sequence;
    @XmlElement(name = "SequenceProject", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ComputeScalarType_sql2016sp1 sequenceProject;
    @XmlElement(name = "SimpleUpdate", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected SimpleUpdateType_sql2016sp1 simpleUpdate;
    @XmlElement(name = "Sort", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected SortType_sql2016sp1 sort;
    @XmlElement(name = "Split", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected SplitType_sql2016sp1 split;
    @XmlElement(name = "Spool", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected SpoolType_sql2016sp1 spool;
    @XmlElement(name = "StreamAggregate", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected StreamAggregateType_sql2016sp1 streamAggregate;
    @XmlElement(name = "Switch", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected SwitchType_sql2016sp1 _switch;
    @XmlElement(name = "TableScan", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected TableScanType_sql2016sp1 tableScan;
    @XmlElement(name = "TableValuedFunction", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected TableValuedFunctionType_sql2016sp1 tableValuedFunction;
    @XmlElement(name = "Top", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected TopType_sql2016sp1 top;
    @XmlElement(name = "TopSort", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected TopSortType_sql2016sp1 topSort;
    @XmlElement(name = "Update", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected UpdateType_sql2016sp1 update;
    @XmlElement(name = "WindowSpool", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected WindowType_sql2016sp1 windowSpool;
    @XmlElement(name = "WindowAggregate", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected WindowAggregateType_sql2016sp1 windowAggregate;
    @XmlAttribute(name = "AvgRowSize", required = true)
    protected double avgRowSize;
    @XmlAttribute(name = "EstimateCPU", required = true)
    protected double estimateCPU;
    @XmlAttribute(name = "EstimateIO", required = true)
    protected double estimateIO;
    @XmlAttribute(name = "EstimateRebinds", required = true)
    protected double estimateRebinds;
    @XmlAttribute(name = "EstimateRewinds", required = true)
    protected double estimateRewinds;
    @XmlAttribute(name = "EstimatedExecutionMode")
    protected ExecutionModeType_sql2016sp1 estimatedExecutionMode;
    @XmlAttribute(name = "GroupExecuted")
    protected Boolean groupExecuted;
    @XmlAttribute(name = "EstimateRows", required = true)
    protected double estimateRows;
    @XmlAttribute(name = "EstimatedRowsRead")
    protected Double estimatedRowsRead;
    @XmlAttribute(name = "LogicalOp", required = true)
    protected LogicalOpType_sql2016sp1 logicalOp;
    @XmlAttribute(name = "NodeId", required = true)
    protected int nodeId;
    @XmlAttribute(name = "Parallel", required = true)
    protected boolean parallel;
    @XmlAttribute(name = "RemoteDataAccess")
    protected Boolean remoteDataAccess;
    @XmlAttribute(name = "Partitioned")
    protected Boolean partitioned;
    @XmlAttribute(name = "PhysicalOp", required = true)
    protected PhysicalOpType_sql2016sp1 physicalOp;
    @XmlAttribute(name = "EstimatedTotalSubtreeCost", required = true)
    protected double estimatedTotalSubtreeCost;
    @XmlAttribute(name = "TableCardinality")
    protected Double tableCardinality;
    @XmlAttribute(name = "StatsCollectionId")
    @XmlSchemaType(name = "unsignedLong")
    protected BigInteger statsCollectionId;

    /**
     * Gets the value of the outputList property.
     * 
     * @return
     *     possible object is
     *     {@link ColumnReferenceListType_sql2016sp1 }
     *     
     */
    public ColumnReferenceListType_sql2016sp1 getOutputList() {
        return outputList;
    }

    /**
     * Sets the value of the outputList property.
     * 
     * @param value
     *     allowed object is
     *     {@link ColumnReferenceListType_sql2016sp1 }
     *     
     */
    public void setOutputList(ColumnReferenceListType_sql2016sp1 value) {
        this.outputList = value;
    }

    /**
     * Gets the value of the warnings property.
     * 
     * @return
     *     possible object is
     *     {@link WarningsType_sql2016sp1 }
     *     
     */
    public WarningsType_sql2016sp1 getWarnings() {
        return warnings;
    }

    /**
     * Sets the value of the warnings property.
     * 
     * @param value
     *     allowed object is
     *     {@link WarningsType_sql2016sp1 }
     *     
     */
    public void setWarnings(WarningsType_sql2016sp1 value) {
        this.warnings = value;
    }

    /**
     * Gets the value of the memoryFractions property.
     * 
     * @return
     *     possible object is
     *     {@link MemoryFractionsType_sql2016sp1 }
     *     
     */
    public MemoryFractionsType_sql2016sp1 getMemoryFractions() {
        return memoryFractions;
    }

    /**
     * Sets the value of the memoryFractions property.
     * 
     * @param value
     *     allowed object is
     *     {@link MemoryFractionsType_sql2016sp1 }
     *     
     */
    public void setMemoryFractions(MemoryFractionsType_sql2016sp1 value) {
        this.memoryFractions = value;
    }

    /**
     * Gets the value of the runTimeInformation property.
     * 
     * @return
     *     possible object is
     *     {@link RunTimeInformationType_sql2016sp1 }
     *     
     */
    public RunTimeInformationType_sql2016sp1 getRunTimeInformation() {
        return runTimeInformation;
    }

    /**
     * Sets the value of the runTimeInformation property.
     * 
     * @param value
     *     allowed object is
     *     {@link RunTimeInformationType_sql2016sp1 }
     *     
     */
    public void setRunTimeInformation(RunTimeInformationType_sql2016sp1 value) {
        this.runTimeInformation = value;
    }

    /**
     * Gets the value of the runTimePartitionSummary property.
     * 
     * @return
     *     possible object is
     *     {@link RunTimePartitionSummaryType_sql2016sp1 }
     *     
     */
    public RunTimePartitionSummaryType_sql2016sp1 getRunTimePartitionSummary() {
        return runTimePartitionSummary;
    }

    /**
     * Sets the value of the runTimePartitionSummary property.
     * 
     * @param value
     *     allowed object is
     *     {@link RunTimePartitionSummaryType_sql2016sp1 }
     *     
     */
    public void setRunTimePartitionSummary(RunTimePartitionSummaryType_sql2016sp1 value) {
        this.runTimePartitionSummary = value;
    }

    /**
     * Gets the value of the internalInfo property.
     * 
     * @return
     *     possible object is
     *     {@link InternalInfoType_sql2016sp1 }
     *     
     */
    public InternalInfoType_sql2016sp1 getInternalInfo() {
        return internalInfo;
    }

    /**
     * Sets the value of the internalInfo property.
     * 
     * @param value
     *     allowed object is
     *     {@link InternalInfoType_sql2016sp1 }
     *     
     */
    public void setInternalInfo(InternalInfoType_sql2016sp1 value) {
        this.internalInfo = value;
    }

    /**
     * Gets the value of the assert property.
     * 
     * @return
     *     possible object is
     *     {@link FilterType_sql2016sp1 }
     *     
     */
    public FilterType_sql2016sp1 getAssert() {
        return _assert;
    }

    /**
     * Sets the value of the assert property.
     * 
     * @param value
     *     allowed object is
     *     {@link FilterType_sql2016sp1 }
     *     
     */
    public void setAssert(FilterType_sql2016sp1 value) {
        this._assert = value;
    }

    /**
     * Gets the value of the batchHashTableBuild property.
     * 
     * @return
     *     possible object is
     *     {@link BatchHashTableBuildType_sql2016sp1 }
     *     
     */
    public BatchHashTableBuildType_sql2016sp1 getBatchHashTableBuild() {
        return batchHashTableBuild;
    }

    /**
     * Sets the value of the batchHashTableBuild property.
     * 
     * @param value
     *     allowed object is
     *     {@link BatchHashTableBuildType_sql2016sp1 }
     *     
     */
    public void setBatchHashTableBuild(BatchHashTableBuildType_sql2016sp1 value) {
        this.batchHashTableBuild = value;
    }

    /**
     * Gets the value of the bitmap property.
     * 
     * @return
     *     possible object is
     *     {@link BitmapType_sql2016sp1 }
     *     
     */
    public BitmapType_sql2016sp1 getBitmap() {
        return bitmap;
    }

    /**
     * Sets the value of the bitmap property.
     * 
     * @param value
     *     allowed object is
     *     {@link BitmapType_sql2016sp1 }
     *     
     */
    public void setBitmap(BitmapType_sql2016sp1 value) {
        this.bitmap = value;
    }

    /**
     * Gets the value of the collapse property.
     * 
     * @return
     *     possible object is
     *     {@link CollapseType_sql2016sp1 }
     *     
     */
    public CollapseType_sql2016sp1 getCollapse() {
        return collapse;
    }

    /**
     * Sets the value of the collapse property.
     * 
     * @param value
     *     allowed object is
     *     {@link CollapseType_sql2016sp1 }
     *     
     */
    public void setCollapse(CollapseType_sql2016sp1 value) {
        this.collapse = value;
    }

    /**
     * Gets the value of the computeScalar property.
     * 
     * @return
     *     possible object is
     *     {@link ComputeScalarType_sql2016sp1 }
     *     
     */
    public ComputeScalarType_sql2016sp1 getComputeScalar() {
        return computeScalar;
    }

    /**
     * Sets the value of the computeScalar property.
     * 
     * @param value
     *     allowed object is
     *     {@link ComputeScalarType_sql2016sp1 }
     *     
     */
    public void setComputeScalar(ComputeScalarType_sql2016sp1 value) {
        this.computeScalar = value;
    }

    /**
     * Gets the value of the concat property.
     * 
     * @return
     *     possible object is
     *     {@link ConcatType_sql2016sp1 }
     *     
     */
    public ConcatType_sql2016sp1 getConcat() {
        return concat;
    }

    /**
     * Sets the value of the concat property.
     * 
     * @param value
     *     allowed object is
     *     {@link ConcatType_sql2016sp1 }
     *     
     */
    public void setConcat(ConcatType_sql2016sp1 value) {
        this.concat = value;
    }

    /**
     * Gets the value of the constantScan property.
     * 
     * @return
     *     possible object is
     *     {@link ConstantScanType_sql2016sp1 }
     *     
     */
    public ConstantScanType_sql2016sp1 getConstantScan() {
        return constantScan;
    }

    /**
     * Sets the value of the constantScan property.
     * 
     * @param value
     *     allowed object is
     *     {@link ConstantScanType_sql2016sp1 }
     *     
     */
    public void setConstantScan(ConstantScanType_sql2016sp1 value) {
        this.constantScan = value;
    }

    /**
     * Gets the value of the createIndex property.
     * 
     * @return
     *     possible object is
     *     {@link CreateIndexType_sql2016sp1 }
     *     
     */
    public CreateIndexType_sql2016sp1 getCreateIndex() {
        return createIndex;
    }

    /**
     * Sets the value of the createIndex property.
     * 
     * @param value
     *     allowed object is
     *     {@link CreateIndexType_sql2016sp1 }
     *     
     */
    public void setCreateIndex(CreateIndexType_sql2016sp1 value) {
        this.createIndex = value;
    }

    /**
     * Gets the value of the deletedScan property.
     * 
     * @return
     *     possible object is
     *     {@link RowsetType_sql2016sp1 }
     *     
     */
    public RowsetType_sql2016sp1 getDeletedScan() {
        return deletedScan;
    }

    /**
     * Sets the value of the deletedScan property.
     * 
     * @param value
     *     allowed object is
     *     {@link RowsetType_sql2016sp1 }
     *     
     */
    public void setDeletedScan(RowsetType_sql2016sp1 value) {
        this.deletedScan = value;
    }

    /**
     * Gets the value of the extension property.
     * 
     * @return
     *     possible object is
     *     {@link UDXType_sql2016sp1 }
     *     
     */
    public UDXType_sql2016sp1 getExtension() {
        return extension;
    }

    /**
     * Sets the value of the extension property.
     * 
     * @param value
     *     allowed object is
     *     {@link UDXType_sql2016sp1 }
     *     
     */
    public void setExtension(UDXType_sql2016sp1 value) {
        this.extension = value;
    }

    /**
     * Gets the value of the filter property.
     * 
     * @return
     *     possible object is
     *     {@link FilterType_sql2016sp1 }
     *     
     */
    public FilterType_sql2016sp1 getFilter() {
        return filter;
    }

    /**
     * Sets the value of the filter property.
     * 
     * @param value
     *     allowed object is
     *     {@link FilterType_sql2016sp1 }
     *     
     */
    public void setFilter(FilterType_sql2016sp1 value) {
        this.filter = value;
    }

    /**
     * Gets the value of the foreignKeyReferencesCheck property.
     * 
     * @return
     *     possible object is
     *     {@link ForeignKeyReferencesCheckType_sql2016sp1 }
     *     
     */
    public ForeignKeyReferencesCheckType_sql2016sp1 getForeignKeyReferencesCheck() {
        return foreignKeyReferencesCheck;
    }

    /**
     * Sets the value of the foreignKeyReferencesCheck property.
     * 
     * @param value
     *     allowed object is
     *     {@link ForeignKeyReferencesCheckType_sql2016sp1 }
     *     
     */
    public void setForeignKeyReferencesCheck(ForeignKeyReferencesCheckType_sql2016sp1 value) {
        this.foreignKeyReferencesCheck = value;
    }

    /**
     * Gets the value of the generic property.
     * 
     * @return
     *     possible object is
     *     {@link GenericType_sql2016sp1 }
     *     
     */
    public GenericType_sql2016sp1 getGeneric() {
        return generic;
    }

    /**
     * Sets the value of the generic property.
     * 
     * @param value
     *     allowed object is
     *     {@link GenericType_sql2016sp1 }
     *     
     */
    public void setGeneric(GenericType_sql2016sp1 value) {
        this.generic = value;
    }

    /**
     * Gets the value of the hash property.
     * 
     * @return
     *     possible object is
     *     {@link HashType_sql2016sp1 }
     *     
     */
    public HashType_sql2016sp1 getHash() {
        return hash;
    }

    /**
     * Sets the value of the hash property.
     * 
     * @param value
     *     allowed object is
     *     {@link HashType_sql2016sp1 }
     *     
     */
    public void setHash(HashType_sql2016sp1 value) {
        this.hash = value;
    }

    /**
     * Gets the value of the indexScan property.
     * 
     * @return
     *     possible object is
     *     {@link IndexScanType_sql2016sp1 }
     *     
     */
    public IndexScanType_sql2016sp1 getIndexScan() {
        return indexScan;
    }

    /**
     * Sets the value of the indexScan property.
     * 
     * @param value
     *     allowed object is
     *     {@link IndexScanType_sql2016sp1 }
     *     
     */
    public void setIndexScan(IndexScanType_sql2016sp1 value) {
        this.indexScan = value;
    }

    /**
     * Gets the value of the insertedScan property.
     * 
     * @return
     *     possible object is
     *     {@link RowsetType_sql2016sp1 }
     *     
     */
    public RowsetType_sql2016sp1 getInsertedScan() {
        return insertedScan;
    }

    /**
     * Sets the value of the insertedScan property.
     * 
     * @param value
     *     allowed object is
     *     {@link RowsetType_sql2016sp1 }
     *     
     */
    public void setInsertedScan(RowsetType_sql2016sp1 value) {
        this.insertedScan = value;
    }

    /**
     * Gets the value of the logRowScan property.
     * 
     * @return
     *     possible object is
     *     {@link RelOpBaseType_sql2016sp1 }
     *     
     */
    public RelOpBaseType_sql2016sp1 getLogRowScan() {
        return logRowScan;
    }

    /**
     * Sets the value of the logRowScan property.
     * 
     * @param value
     *     allowed object is
     *     {@link RelOpBaseType_sql2016sp1 }
     *     
     */
    public void setLogRowScan(RelOpBaseType_sql2016sp1 value) {
        this.logRowScan = value;
    }

    /**
     * Gets the value of the merge property.
     * 
     * @return
     *     possible object is
     *     {@link MergeType_sql2016sp1 }
     *     
     */
    public MergeType_sql2016sp1 getMerge() {
        return merge;
    }

    /**
     * Sets the value of the merge property.
     * 
     * @param value
     *     allowed object is
     *     {@link MergeType_sql2016sp1 }
     *     
     */
    public void setMerge(MergeType_sql2016sp1 value) {
        this.merge = value;
    }

    /**
     * Gets the value of the mergeInterval property.
     * 
     * @return
     *     possible object is
     *     {@link SimpleIteratorOneChildType_sql2016sp1 }
     *     
     */
    public SimpleIteratorOneChildType_sql2016sp1 getMergeInterval() {
        return mergeInterval;
    }

    /**
     * Sets the value of the mergeInterval property.
     * 
     * @param value
     *     allowed object is
     *     {@link SimpleIteratorOneChildType_sql2016sp1 }
     *     
     */
    public void setMergeInterval(SimpleIteratorOneChildType_sql2016sp1 value) {
        this.mergeInterval = value;
    }

    /**
     * Gets the value of the nestedLoops property.
     * 
     * @return
     *     possible object is
     *     {@link NestedLoopsType_sql2016sp1 }
     *     
     */
    public NestedLoopsType_sql2016sp1 getNestedLoops() {
        return nestedLoops;
    }

    /**
     * Sets the value of the nestedLoops property.
     * 
     * @param value
     *     allowed object is
     *     {@link NestedLoopsType_sql2016sp1 }
     *     
     */
    public void setNestedLoops(NestedLoopsType_sql2016sp1 value) {
        this.nestedLoops = value;
    }

    /**
     * Gets the value of the onlineIndex property.
     * 
     * @return
     *     possible object is
     *     {@link CreateIndexType_sql2016sp1 }
     *     
     */
    public CreateIndexType_sql2016sp1 getOnlineIndex() {
        return onlineIndex;
    }

    /**
     * Sets the value of the onlineIndex property.
     * 
     * @param value
     *     allowed object is
     *     {@link CreateIndexType_sql2016sp1 }
     *     
     */
    public void setOnlineIndex(CreateIndexType_sql2016sp1 value) {
        this.onlineIndex = value;
    }

    /**
     * Gets the value of the parallelism property.
     * 
     * @return
     *     possible object is
     *     {@link ParallelismType_sql2016sp1 }
     *     
     */
    public ParallelismType_sql2016sp1 getParallelism() {
        return parallelism;
    }

    /**
     * Sets the value of the parallelism property.
     * 
     * @param value
     *     allowed object is
     *     {@link ParallelismType_sql2016sp1 }
     *     
     */
    public void setParallelism(ParallelismType_sql2016sp1 value) {
        this.parallelism = value;
    }

    /**
     * Gets the value of the parameterTableScan property.
     * 
     * @return
     *     possible object is
     *     {@link RelOpBaseType_sql2016sp1 }
     *     
     */
    public RelOpBaseType_sql2016sp1 getParameterTableScan() {
        return parameterTableScan;
    }

    /**
     * Sets the value of the parameterTableScan property.
     * 
     * @param value
     *     allowed object is
     *     {@link RelOpBaseType_sql2016sp1 }
     *     
     */
    public void setParameterTableScan(RelOpBaseType_sql2016sp1 value) {
        this.parameterTableScan = value;
    }

    /**
     * Gets the value of the printDataflow property.
     * 
     * @return
     *     possible object is
     *     {@link RelOpBaseType_sql2016sp1 }
     *     
     */
    public RelOpBaseType_sql2016sp1 getPrintDataflow() {
        return printDataflow;
    }

    /**
     * Sets the value of the printDataflow property.
     * 
     * @param value
     *     allowed object is
     *     {@link RelOpBaseType_sql2016sp1 }
     *     
     */
    public void setPrintDataflow(RelOpBaseType_sql2016sp1 value) {
        this.printDataflow = value;
    }

    /**
     * Gets the value of the put property.
     * 
     * @return
     *     possible object is
     *     {@link PutType_sql2016sp1 }
     *     
     */
    public PutType_sql2016sp1 getPut() {
        return put;
    }

    /**
     * Sets the value of the put property.
     * 
     * @param value
     *     allowed object is
     *     {@link PutType_sql2016sp1 }
     *     
     */
    public void setPut(PutType_sql2016sp1 value) {
        this.put = value;
    }

    /**
     * Gets the value of the remoteFetch property.
     * 
     * @return
     *     possible object is
     *     {@link RemoteFetchType_sql2016sp1 }
     *     
     */
    public RemoteFetchType_sql2016sp1 getRemoteFetch() {
        return remoteFetch;
    }

    /**
     * Sets the value of the remoteFetch property.
     * 
     * @param value
     *     allowed object is
     *     {@link RemoteFetchType_sql2016sp1 }
     *     
     */
    public void setRemoteFetch(RemoteFetchType_sql2016sp1 value) {
        this.remoteFetch = value;
    }

    /**
     * Gets the value of the remoteModify property.
     * 
     * @return
     *     possible object is
     *     {@link RemoteModifyType_sql2016sp1 }
     *     
     */
    public RemoteModifyType_sql2016sp1 getRemoteModify() {
        return remoteModify;
    }

    /**
     * Sets the value of the remoteModify property.
     * 
     * @param value
     *     allowed object is
     *     {@link RemoteModifyType_sql2016sp1 }
     *     
     */
    public void setRemoteModify(RemoteModifyType_sql2016sp1 value) {
        this.remoteModify = value;
    }

    /**
     * Gets the value of the remoteQuery property.
     * 
     * @return
     *     possible object is
     *     {@link RemoteQueryType_sql2016sp1 }
     *     
     */
    public RemoteQueryType_sql2016sp1 getRemoteQuery() {
        return remoteQuery;
    }

    /**
     * Sets the value of the remoteQuery property.
     * 
     * @param value
     *     allowed object is
     *     {@link RemoteQueryType_sql2016sp1 }
     *     
     */
    public void setRemoteQuery(RemoteQueryType_sql2016sp1 value) {
        this.remoteQuery = value;
    }

    /**
     * Gets the value of the remoteRange property.
     * 
     * @return
     *     possible object is
     *     {@link RemoteRangeType_sql2016sp1 }
     *     
     */
    public RemoteRangeType_sql2016sp1 getRemoteRange() {
        return remoteRange;
    }

    /**
     * Sets the value of the remoteRange property.
     * 
     * @param value
     *     allowed object is
     *     {@link RemoteRangeType_sql2016sp1 }
     *     
     */
    public void setRemoteRange(RemoteRangeType_sql2016sp1 value) {
        this.remoteRange = value;
    }

    /**
     * Gets the value of the remoteScan property.
     * 
     * @return
     *     possible object is
     *     {@link RemoteType_sql2016sp1 }
     *     
     */
    public RemoteType_sql2016sp1 getRemoteScan() {
        return remoteScan;
    }

    /**
     * Sets the value of the remoteScan property.
     * 
     * @param value
     *     allowed object is
     *     {@link RemoteType_sql2016sp1 }
     *     
     */
    public void setRemoteScan(RemoteType_sql2016sp1 value) {
        this.remoteScan = value;
    }

    /**
     * Gets the value of the rowCountSpool property.
     * 
     * @return
     *     possible object is
     *     {@link SpoolType_sql2016sp1 }
     *     
     */
    public SpoolType_sql2016sp1 getRowCountSpool() {
        return rowCountSpool;
    }

    /**
     * Sets the value of the rowCountSpool property.
     * 
     * @param value
     *     allowed object is
     *     {@link SpoolType_sql2016sp1 }
     *     
     */
    public void setRowCountSpool(SpoolType_sql2016sp1 value) {
        this.rowCountSpool = value;
    }

    /**
     * Gets the value of the scalarInsert property.
     * 
     * @return
     *     possible object is
     *     {@link ScalarInsertType_sql2016sp1 }
     *     
     */
    public ScalarInsertType_sql2016sp1 getScalarInsert() {
        return scalarInsert;
    }

    /**
     * Sets the value of the scalarInsert property.
     * 
     * @param value
     *     allowed object is
     *     {@link ScalarInsertType_sql2016sp1 }
     *     
     */
    public void setScalarInsert(ScalarInsertType_sql2016sp1 value) {
        this.scalarInsert = value;
    }

    /**
     * Gets the value of the segment property.
     * 
     * @return
     *     possible object is
     *     {@link SegmentType_sql2016sp1 }
     *     
     */
    public SegmentType_sql2016sp1 getSegment() {
        return segment;
    }

    /**
     * Sets the value of the segment property.
     * 
     * @param value
     *     allowed object is
     *     {@link SegmentType_sql2016sp1 }
     *     
     */
    public void setSegment(SegmentType_sql2016sp1 value) {
        this.segment = value;
    }

    /**
     * Gets the value of the sequence property.
     * 
     * @return
     *     possible object is
     *     {@link SequenceType_sql2016sp1 }
     *     
     */
    public SequenceType_sql2016sp1 getSequence() {
        return sequence;
    }

    /**
     * Sets the value of the sequence property.
     * 
     * @param value
     *     allowed object is
     *     {@link SequenceType_sql2016sp1 }
     *     
     */
    public void setSequence(SequenceType_sql2016sp1 value) {
        this.sequence = value;
    }

    /**
     * Gets the value of the sequenceProject property.
     * 
     * @return
     *     possible object is
     *     {@link ComputeScalarType_sql2016sp1 }
     *     
     */
    public ComputeScalarType_sql2016sp1 getSequenceProject() {
        return sequenceProject;
    }

    /**
     * Sets the value of the sequenceProject property.
     * 
     * @param value
     *     allowed object is
     *     {@link ComputeScalarType_sql2016sp1 }
     *     
     */
    public void setSequenceProject(ComputeScalarType_sql2016sp1 value) {
        this.sequenceProject = value;
    }

    /**
     * Gets the value of the simpleUpdate property.
     * 
     * @return
     *     possible object is
     *     {@link SimpleUpdateType_sql2016sp1 }
     *     
     */
    public SimpleUpdateType_sql2016sp1 getSimpleUpdate() {
        return simpleUpdate;
    }

    /**
     * Sets the value of the simpleUpdate property.
     * 
     * @param value
     *     allowed object is
     *     {@link SimpleUpdateType_sql2016sp1 }
     *     
     */
    public void setSimpleUpdate(SimpleUpdateType_sql2016sp1 value) {
        this.simpleUpdate = value;
    }

    /**
     * Gets the value of the sort property.
     * 
     * @return
     *     possible object is
     *     {@link SortType_sql2016sp1 }
     *     
     */
    public SortType_sql2016sp1 getSort() {
        return sort;
    }

    /**
     * Sets the value of the sort property.
     * 
     * @param value
     *     allowed object is
     *     {@link SortType_sql2016sp1 }
     *     
     */
    public void setSort(SortType_sql2016sp1 value) {
        this.sort = value;
    }

    /**
     * Gets the value of the split property.
     * 
     * @return
     *     possible object is
     *     {@link SplitType_sql2016sp1 }
     *     
     */
    public SplitType_sql2016sp1 getSplit() {
        return split;
    }

    /**
     * Sets the value of the split property.
     * 
     * @param value
     *     allowed object is
     *     {@link SplitType_sql2016sp1 }
     *     
     */
    public void setSplit(SplitType_sql2016sp1 value) {
        this.split = value;
    }

    /**
     * Gets the value of the spool property.
     * 
     * @return
     *     possible object is
     *     {@link SpoolType_sql2016sp1 }
     *     
     */
    public SpoolType_sql2016sp1 getSpool() {
        return spool;
    }

    /**
     * Sets the value of the spool property.
     * 
     * @param value
     *     allowed object is
     *     {@link SpoolType_sql2016sp1 }
     *     
     */
    public void setSpool(SpoolType_sql2016sp1 value) {
        this.spool = value;
    }

    /**
     * Gets the value of the streamAggregate property.
     * 
     * @return
     *     possible object is
     *     {@link StreamAggregateType_sql2016sp1 }
     *     
     */
    public StreamAggregateType_sql2016sp1 getStreamAggregate() {
        return streamAggregate;
    }

    /**
     * Sets the value of the streamAggregate property.
     * 
     * @param value
     *     allowed object is
     *     {@link StreamAggregateType_sql2016sp1 }
     *     
     */
    public void setStreamAggregate(StreamAggregateType_sql2016sp1 value) {
        this.streamAggregate = value;
    }

    /**
     * Gets the value of the switch property.
     * 
     * @return
     *     possible object is
     *     {@link SwitchType_sql2016sp1 }
     *     
     */
    public SwitchType_sql2016sp1 getSwitch() {
        return _switch;
    }

    /**
     * Sets the value of the switch property.
     * 
     * @param value
     *     allowed object is
     *     {@link SwitchType_sql2016sp1 }
     *     
     */
    public void setSwitch(SwitchType_sql2016sp1 value) {
        this._switch = value;
    }

    /**
     * Gets the value of the tableScan property.
     * 
     * @return
     *     possible object is
     *     {@link TableScanType_sql2016sp1 }
     *     
     */
    public TableScanType_sql2016sp1 getTableScan() {
        return tableScan;
    }

    /**
     * Sets the value of the tableScan property.
     * 
     * @param value
     *     allowed object is
     *     {@link TableScanType_sql2016sp1 }
     *     
     */
    public void setTableScan(TableScanType_sql2016sp1 value) {
        this.tableScan = value;
    }

    /**
     * Gets the value of the tableValuedFunction property.
     * 
     * @return
     *     possible object is
     *     {@link TableValuedFunctionType_sql2016sp1 }
     *     
     */
    public TableValuedFunctionType_sql2016sp1 getTableValuedFunction() {
        return tableValuedFunction;
    }

    /**
     * Sets the value of the tableValuedFunction property.
     * 
     * @param value
     *     allowed object is
     *     {@link TableValuedFunctionType_sql2016sp1 }
     *     
     */
    public void setTableValuedFunction(TableValuedFunctionType_sql2016sp1 value) {
        this.tableValuedFunction = value;
    }

    /**
     * Gets the value of the top property.
     * 
     * @return
     *     possible object is
     *     {@link TopType_sql2016sp1 }
     *     
     */
    public TopType_sql2016sp1 getTop() {
        return top;
    }

    /**
     * Sets the value of the top property.
     * 
     * @param value
     *     allowed object is
     *     {@link TopType_sql2016sp1 }
     *     
     */
    public void setTop(TopType_sql2016sp1 value) {
        this.top = value;
    }

    /**
     * Gets the value of the topSort property.
     * 
     * @return
     *     possible object is
     *     {@link TopSortType_sql2016sp1 }
     *     
     */
    public TopSortType_sql2016sp1 getTopSort() {
        return topSort;
    }

    /**
     * Sets the value of the topSort property.
     * 
     * @param value
     *     allowed object is
     *     {@link TopSortType_sql2016sp1 }
     *     
     */
    public void setTopSort(TopSortType_sql2016sp1 value) {
        this.topSort = value;
    }

    /**
     * Gets the value of the update property.
     * 
     * @return
     *     possible object is
     *     {@link UpdateType_sql2016sp1 }
     *     
     */
    public UpdateType_sql2016sp1 getUpdate() {
        return update;
    }

    /**
     * Sets the value of the update property.
     * 
     * @param value
     *     allowed object is
     *     {@link UpdateType_sql2016sp1 }
     *     
     */
    public void setUpdate(UpdateType_sql2016sp1 value) {
        this.update = value;
    }

    /**
     * Gets the value of the windowSpool property.
     * 
     * @return
     *     possible object is
     *     {@link WindowType_sql2016sp1 }
     *     
     */
    public WindowType_sql2016sp1 getWindowSpool() {
        return windowSpool;
    }

    /**
     * Sets the value of the windowSpool property.
     * 
     * @param value
     *     allowed object is
     *     {@link WindowType_sql2016sp1 }
     *     
     */
    public void setWindowSpool(WindowType_sql2016sp1 value) {
        this.windowSpool = value;
    }

    /**
     * Gets the value of the windowAggregate property.
     * 
     * @return
     *     possible object is
     *     {@link WindowAggregateType_sql2016sp1 }
     *     
     */
    public WindowAggregateType_sql2016sp1 getWindowAggregate() {
        return windowAggregate;
    }

    /**
     * Sets the value of the windowAggregate property.
     * 
     * @param value
     *     allowed object is
     *     {@link WindowAggregateType_sql2016sp1 }
     *     
     */
    public void setWindowAggregate(WindowAggregateType_sql2016sp1 value) {
        this.windowAggregate = value;
    }

    /**
     * Gets the value of the avgRowSize property.
     * 
     */
    public double getAvgRowSize() {
        return avgRowSize;
    }

    /**
     * Sets the value of the avgRowSize property.
     * 
     */
    public void setAvgRowSize(double value) {
        this.avgRowSize = value;
    }

    /**
     * Gets the value of the estimateCPU property.
     * 
     */
    public double getEstimateCPU() {
        return estimateCPU;
    }

    /**
     * Sets the value of the estimateCPU property.
     * 
     */
    public void setEstimateCPU(double value) {
        this.estimateCPU = value;
    }

    /**
     * Gets the value of the estimateIO property.
     * 
     */
    public double getEstimateIO() {
        return estimateIO;
    }

    /**
     * Sets the value of the estimateIO property.
     * 
     */
    public void setEstimateIO(double value) {
        this.estimateIO = value;
    }

    /**
     * Gets the value of the estimateRebinds property.
     * 
     */
    public double getEstimateRebinds() {
        return estimateRebinds;
    }

    /**
     * Sets the value of the estimateRebinds property.
     * 
     */
    public void setEstimateRebinds(double value) {
        this.estimateRebinds = value;
    }

    /**
     * Gets the value of the estimateRewinds property.
     * 
     */
    public double getEstimateRewinds() {
        return estimateRewinds;
    }

    /**
     * Sets the value of the estimateRewinds property.
     * 
     */
    public void setEstimateRewinds(double value) {
        this.estimateRewinds = value;
    }

    /**
     * Gets the value of the estimatedExecutionMode property.
     * 
     * @return
     *     possible object is
     *     {@link ExecutionModeType_sql2016sp1 }
     *     
     */
    public ExecutionModeType_sql2016sp1 getEstimatedExecutionMode() {
        return estimatedExecutionMode;
    }

    /**
     * Sets the value of the estimatedExecutionMode property.
     * 
     * @param value
     *     allowed object is
     *     {@link ExecutionModeType_sql2016sp1 }
     *     
     */
    public void setEstimatedExecutionMode(ExecutionModeType_sql2016sp1 value) {
        this.estimatedExecutionMode = value;
    }

    /**
     * Gets the value of the groupExecuted property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean getGroupExecuted() {
        return groupExecuted;
    }

    /**
     * Sets the value of the groupExecuted property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setGroupExecuted(Boolean value) {
        this.groupExecuted = value;
    }

    /**
     * Gets the value of the estimateRows property.
     * 
     */
    public double getEstimateRows() {
        return estimateRows;
    }

    /**
     * Sets the value of the estimateRows property.
     * 
     */
    public void setEstimateRows(double value) {
        this.estimateRows = value;
    }

    /**
     * Gets the value of the estimatedRowsRead property.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public Double getEstimatedRowsRead() {
        return estimatedRowsRead;
    }

    /**
     * Sets the value of the estimatedRowsRead property.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setEstimatedRowsRead(Double value) {
        this.estimatedRowsRead = value;
    }

    /**
     * Gets the value of the logicalOp property.
     * 
     * @return
     *     possible object is
     *     {@link LogicalOpType_sql2016sp1 }
     *     
     */
    public LogicalOpType_sql2016sp1 getLogicalOp() {
        return logicalOp;
    }

    /**
     * Sets the value of the logicalOp property.
     * 
     * @param value
     *     allowed object is
     *     {@link LogicalOpType_sql2016sp1 }
     *     
     */
    public void setLogicalOp(LogicalOpType_sql2016sp1 value) {
        this.logicalOp = value;
    }

    /**
     * Gets the value of the nodeId property.
     * 
     */
    public int getNodeId() {
        return nodeId;
    }

    /**
     * Sets the value of the nodeId property.
     * 
     */
    public void setNodeId(int value) {
        this.nodeId = value;
    }

    /**
     * Gets the value of the parallel property.
     * 
     */
    public boolean isParallel() {
        return parallel;
    }

    /**
     * Sets the value of the parallel property.
     * 
     */
    public void setParallel(boolean value) {
        this.parallel = value;
    }

    /**
     * Gets the value of the remoteDataAccess property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean getRemoteDataAccess() {
        return remoteDataAccess;
    }

    /**
     * Sets the value of the remoteDataAccess property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setRemoteDataAccess(Boolean value) {
        this.remoteDataAccess = value;
    }

    /**
     * Gets the value of the partitioned property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean getPartitioned() {
        return partitioned;
    }

    /**
     * Sets the value of the partitioned property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setPartitioned(Boolean value) {
        this.partitioned = value;
    }

    /**
     * Gets the value of the physicalOp property.
     * 
     * @return
     *     possible object is
     *     {@link PhysicalOpType_sql2016sp1 }
     *     
     */
    public PhysicalOpType_sql2016sp1 getPhysicalOp() {
        return physicalOp;
    }

    /**
     * Sets the value of the physicalOp property.
     * 
     * @param value
     *     allowed object is
     *     {@link PhysicalOpType_sql2016sp1 }
     *     
     */
    public void setPhysicalOp(PhysicalOpType_sql2016sp1 value) {
        this.physicalOp = value;
    }

    /**
     * Gets the value of the estimatedTotalSubtreeCost property.
     * 
     */
    public double getEstimatedTotalSubtreeCost() {
        return estimatedTotalSubtreeCost;
    }

    /**
     * Sets the value of the estimatedTotalSubtreeCost property.
     * 
     */
    public void setEstimatedTotalSubtreeCost(double value) {
        this.estimatedTotalSubtreeCost = value;
    }

    /**
     * Gets the value of the tableCardinality property.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public Double getTableCardinality() {
        return tableCardinality;
    }

    /**
     * Sets the value of the tableCardinality property.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setTableCardinality(Double value) {
        this.tableCardinality = value;
    }

    /**
     * Gets the value of the statsCollectionId property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getStatsCollectionId() {
        return statsCollectionId;
    }

    /**
     * Sets the value of the statsCollectionId property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setStatsCollectionId(BigInteger value) {
        this.statsCollectionId = value;
    }

}
