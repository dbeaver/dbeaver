
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2014sp2;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2014sp2 package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _DefinedValuesListType_sql2014sp2DefinedValue_sql2014sp2ValueVector_QNAME = new QName("http://schemas.microsoft.com/sqlserver/2004/07/showplan", "ValueVector");
    private final static QName _DefinedValuesListType_sql2014sp2DefinedValue_sql2014sp2ScalarOperator_QNAME = new QName("http://schemas.microsoft.com/sqlserver/2004/07/showplan", "ScalarOperator");
    private final static QName _DefinedValuesListType_sql2014sp2DefinedValue_sql2014sp2ColumnReference_QNAME = new QName("http://schemas.microsoft.com/sqlserver/2004/07/showplan", "ColumnReference");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2014sp2
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link ShowPlanXML }
     * 
     */
    public ShowPlanXML createShowPlanXML() {
        return new ShowPlanXML();
    }

    /**
     * Create an instance of {@link ParallelismType_sql2014sp2 }
     * 
     */
    public ParallelismType_sql2014sp2 createParallelismType_sql2014sp2() {
        return new ParallelismType_sql2014sp2();
    }

    /**
     * Create an instance of {@link ConstantScanType_sql2014sp2 }
     * 
     */
    public ConstantScanType_sql2014sp2 createConstantScanType_sql2014sp2() {
        return new ConstantScanType_sql2014sp2();
    }

    /**
     * Create an instance of {@link CursorPlanType_sql2014sp2 }
     * 
     */
    public CursorPlanType_sql2014sp2 createCursorPlanType_sql2014sp2() {
        return new CursorPlanType_sql2014sp2();
    }

    /**
     * Create an instance of {@link RunTimeInformationType_sql2014sp2 }
     * 
     */
    public RunTimeInformationType_sql2014sp2 createRunTimeInformationType_sql2014sp2() {
        return new RunTimeInformationType_sql2014sp2();
    }

    /**
     * Create an instance of {@link ReceivePlanType_sql2014sp2 }
     * 
     */
    public ReceivePlanType_sql2014sp2 createReceivePlanType_sql2014sp2() {
        return new ReceivePlanType_sql2014sp2();
    }

    /**
     * Create an instance of {@link DefinedValuesListType_sql2014sp2 }
     * 
     */
    public DefinedValuesListType_sql2014sp2 createDefinedValuesListType_sql2014sp2() {
        return new DefinedValuesListType_sql2014sp2();
    }

    /**
     * Create an instance of {@link DefinedValuesListType_sql2014sp2 .DefinedValue_sql2014sp2 }
     * 
     */
    public DefinedValuesListType_sql2014sp2 .DefinedValue_sql2014sp2 createDefinedValuesListType_sql2014sp2DefinedValue_sql2014sp2() {
        return new DefinedValuesListType_sql2014sp2 .DefinedValue_sql2014sp2();
    }

    /**
     * Create an instance of {@link RunTimePartitionSummaryType_sql2014sp2 }
     * 
     */
    public RunTimePartitionSummaryType_sql2014sp2 createRunTimePartitionSummaryType_sql2014sp2() {
        return new RunTimePartitionSummaryType_sql2014sp2();
    }

    /**
     * Create an instance of {@link RunTimePartitionSummaryType_sql2014sp2 .PartitionsAccessed_sql2014sp2 }
     * 
     */
    public RunTimePartitionSummaryType_sql2014sp2 .PartitionsAccessed_sql2014sp2 createRunTimePartitionSummaryType_sql2014sp2PartitionsAccessed_sql2014sp2() {
        return new RunTimePartitionSummaryType_sql2014sp2 .PartitionsAccessed_sql2014sp2();
    }

    /**
     * Create an instance of {@link OrderByType_sql2014sp2 }
     * 
     */
    public OrderByType_sql2014sp2 createOrderByType_sql2014sp2() {
        return new OrderByType_sql2014sp2();
    }

    /**
     * Create an instance of {@link StmtCondType_sql2014sp2 }
     * 
     */
    public StmtCondType_sql2014sp2 createStmtCondType_sql2014sp2() {
        return new StmtCondType_sql2014sp2();
    }

    /**
     * Create an instance of {@link ShowPlanXML.BatchSequence_sql2014sp2 }
     * 
     */
    public ShowPlanXML.BatchSequence_sql2014sp2 createShowPlanXMLBatchSequence_sql2014sp2() {
        return new ShowPlanXML.BatchSequence_sql2014sp2();
    }

    /**
     * Create an instance of {@link UDFType_sql2014sp2 }
     * 
     */
    public UDFType_sql2014sp2 createUDFType_sql2014sp2() {
        return new UDFType_sql2014sp2();
    }

    /**
     * Create an instance of {@link ScalarExpressionListType_sql2014sp2 }
     * 
     */
    public ScalarExpressionListType_sql2014sp2 createScalarExpressionListType_sql2014sp2() {
        return new ScalarExpressionListType_sql2014sp2();
    }

    /**
     * Create an instance of {@link SeekPredicatesType_sql2014sp2 }
     * 
     */
    public SeekPredicatesType_sql2014sp2 createSeekPredicatesType_sql2014sp2() {
        return new SeekPredicatesType_sql2014sp2();
    }

    /**
     * Create an instance of {@link AssignType_sql2014sp2 }
     * 
     */
    public AssignType_sql2014sp2 createAssignType_sql2014sp2() {
        return new AssignType_sql2014sp2();
    }

    /**
     * Create an instance of {@link StmtSimpleType_sql2014sp2 }
     * 
     */
    public StmtSimpleType_sql2014sp2 createStmtSimpleType_sql2014sp2() {
        return new StmtSimpleType_sql2014sp2();
    }

    /**
     * Create an instance of {@link RollupInfoType_sql2014sp2 }
     * 
     */
    public RollupInfoType_sql2014sp2 createRollupInfoType_sql2014sp2() {
        return new RollupInfoType_sql2014sp2();
    }

    /**
     * Create an instance of {@link MergeType_sql2014sp2 }
     * 
     */
    public MergeType_sql2014sp2 createMergeType_sql2014sp2() {
        return new MergeType_sql2014sp2();
    }

    /**
     * Create an instance of {@link TableValuedFunctionType_sql2014sp2 }
     * 
     */
    public TableValuedFunctionType_sql2014sp2 createTableValuedFunctionType_sql2014sp2() {
        return new TableValuedFunctionType_sql2014sp2();
    }

    /**
     * Create an instance of {@link RemoteType_sql2014sp2 }
     * 
     */
    public RemoteType_sql2014sp2 createRemoteType_sql2014sp2() {
        return new RemoteType_sql2014sp2();
    }

    /**
     * Create an instance of {@link CollapseType_sql2014sp2 }
     * 
     */
    public CollapseType_sql2014sp2 createCollapseType_sql2014sp2() {
        return new CollapseType_sql2014sp2();
    }

    /**
     * Create an instance of {@link MultAssignType_sql2014sp2 }
     * 
     */
    public MultAssignType_sql2014sp2 createMultAssignType_sql2014sp2() {
        return new MultAssignType_sql2014sp2();
    }

    /**
     * Create an instance of {@link HashType_sql2014sp2 }
     * 
     */
    public HashType_sql2014sp2 createHashType_sql2014sp2() {
        return new HashType_sql2014sp2();
    }

    /**
     * Create an instance of {@link StmtCursorType_sql2014sp2 }
     * 
     */
    public StmtCursorType_sql2014sp2 createStmtCursorType_sql2014sp2() {
        return new StmtCursorType_sql2014sp2();
    }

    /**
     * Create an instance of {@link ColumnReferenceListType_sql2014sp2 }
     * 
     */
    public ColumnReferenceListType_sql2014sp2 createColumnReferenceListType_sql2014sp2() {
        return new ColumnReferenceListType_sql2014sp2();
    }

    /**
     * Create an instance of {@link ConcatType_sql2014sp2 }
     * 
     */
    public ConcatType_sql2014sp2 createConcatType_sql2014sp2() {
        return new ConcatType_sql2014sp2();
    }

    /**
     * Create an instance of {@link ArithmeticType_sql2014sp2 }
     * 
     */
    public ArithmeticType_sql2014sp2 createArithmeticType_sql2014sp2() {
        return new ArithmeticType_sql2014sp2();
    }

    /**
     * Create an instance of {@link MissingIndexGroupType_sql2014sp2 }
     * 
     */
    public MissingIndexGroupType_sql2014sp2 createMissingIndexGroupType_sql2014sp2() {
        return new MissingIndexGroupType_sql2014sp2();
    }

    /**
     * Create an instance of {@link UDTMethodType_sql2014sp2 }
     * 
     */
    public UDTMethodType_sql2014sp2 createUDTMethodType_sql2014sp2() {
        return new UDTMethodType_sql2014sp2();
    }

    /**
     * Create an instance of {@link SeekPredicateType_sql2014sp2 }
     * 
     */
    public SeekPredicateType_sql2014sp2 createSeekPredicateType_sql2014sp2() {
        return new SeekPredicateType_sql2014sp2();
    }

    /**
     * Create an instance of {@link TraceFlagType_sql2014sp2 }
     * 
     */
    public TraceFlagType_sql2014sp2 createTraceFlagType_sql2014sp2() {
        return new TraceFlagType_sql2014sp2();
    }

    /**
     * Create an instance of {@link StmtReceiveType_sql2014sp2 }
     * 
     */
    public StmtReceiveType_sql2014sp2 createStmtReceiveType_sql2014sp2() {
        return new StmtReceiveType_sql2014sp2();
    }

    /**
     * Create an instance of {@link HashSpillDetailsType_sql2014sp2 }
     * 
     */
    public HashSpillDetailsType_sql2014sp2 createHashSpillDetailsType_sql2014sp2() {
        return new HashSpillDetailsType_sql2014sp2();
    }

    /**
     * Create an instance of {@link TopType_sql2014sp2 }
     * 
     */
    public TopType_sql2014sp2 createTopType_sql2014sp2() {
        return new TopType_sql2014sp2();
    }

    /**
     * Create an instance of {@link OptimizerHardwareDependentPropertiesType_sql2014sp2 }
     * 
     */
    public OptimizerHardwareDependentPropertiesType_sql2014sp2 createOptimizerHardwareDependentPropertiesType_sql2014sp2() {
        return new OptimizerHardwareDependentPropertiesType_sql2014sp2();
    }

    /**
     * Create an instance of {@link CompareType_sql2014sp2 }
     * 
     */
    public CompareType_sql2014sp2 createCompareType_sql2014sp2() {
        return new CompareType_sql2014sp2();
    }

    /**
     * Create an instance of {@link StmtBlockType_sql2014sp2 }
     * 
     */
    public StmtBlockType_sql2014sp2 createStmtBlockType_sql2014sp2() {
        return new StmtBlockType_sql2014sp2();
    }

    /**
     * Create an instance of {@link SequenceType_sql2014sp2 }
     * 
     */
    public SequenceType_sql2014sp2 createSequenceType_sql2014sp2() {
        return new SequenceType_sql2014sp2();
    }

    /**
     * Create an instance of {@link MissingIndexesType_sql2014sp2 }
     * 
     */
    public MissingIndexesType_sql2014sp2 createMissingIndexesType_sql2014sp2() {
        return new MissingIndexesType_sql2014sp2();
    }

    /**
     * Create an instance of {@link ObjectType_sql2014sp2 }
     * 
     */
    public ObjectType_sql2014sp2 createObjectType_sql2014sp2() {
        return new ObjectType_sql2014sp2();
    }

    /**
     * Create an instance of {@link IndexedViewInfoType_sql2014sp2 }
     * 
     */
    public IndexedViewInfoType_sql2014sp2 createIndexedViewInfoType_sql2014sp2() {
        return new IndexedViewInfoType_sql2014sp2();
    }

    /**
     * Create an instance of {@link RemoteModifyType_sql2014sp2 }
     * 
     */
    public RemoteModifyType_sql2014sp2 createRemoteModifyType_sql2014sp2() {
        return new RemoteModifyType_sql2014sp2();
    }

    /**
     * Create an instance of {@link SingleColumnReferenceType_sql2014sp2 }
     * 
     */
    public SingleColumnReferenceType_sql2014sp2 createSingleColumnReferenceType_sql2014sp2() {
        return new SingleColumnReferenceType_sql2014sp2();
    }

    /**
     * Create an instance of {@link ConstType_sql2014sp2 }
     * 
     */
    public ConstType_sql2014sp2 createConstType_sql2014sp2() {
        return new ConstType_sql2014sp2();
    }

    /**
     * Create an instance of {@link ConditionalType_sql2014sp2 }
     * 
     */
    public ConditionalType_sql2014sp2 createConditionalType_sql2014sp2() {
        return new ConditionalType_sql2014sp2();
    }

    /**
     * Create an instance of {@link GenericType_sql2014sp2 }
     * 
     */
    public GenericType_sql2014sp2 createGenericType_sql2014sp2() {
        return new GenericType_sql2014sp2();
    }

    /**
     * Create an instance of {@link SetOptionsType_sql2014sp2 }
     * 
     */
    public SetOptionsType_sql2014sp2 createSetOptionsType_sql2014sp2() {
        return new SetOptionsType_sql2014sp2();
    }

    /**
     * Create an instance of {@link FunctionType_sql2014sp2 }
     * 
     */
    public FunctionType_sql2014sp2 createFunctionType_sql2014sp2() {
        return new FunctionType_sql2014sp2();
    }

    /**
     * Create an instance of {@link RelOpBaseType_sql2014sp2 }
     * 
     */
    public RelOpBaseType_sql2014sp2 createRelOpBaseType_sql2014sp2() {
        return new RelOpBaseType_sql2014sp2();
    }

    /**
     * Create an instance of {@link CreateIndexType_sql2014sp2 }
     * 
     */
    public CreateIndexType_sql2014sp2 createCreateIndexType_sql2014sp2() {
        return new CreateIndexType_sql2014sp2();
    }

    /**
     * Create an instance of {@link SegmentType_sql2014sp2 }
     * 
     */
    public SegmentType_sql2014sp2 createSegmentType_sql2014sp2() {
        return new SegmentType_sql2014sp2();
    }

    /**
     * Create an instance of {@link BatchHashTableBuildType_sql2014sp2 }
     * 
     */
    public BatchHashTableBuildType_sql2014sp2 createBatchHashTableBuildType_sql2014sp2() {
        return new BatchHashTableBuildType_sql2014sp2();
    }

    /**
     * Create an instance of {@link SimpleUpdateType_sql2014sp2 }
     * 
     */
    public SimpleUpdateType_sql2014sp2 createSimpleUpdateType_sql2014sp2() {
        return new SimpleUpdateType_sql2014sp2();
    }

    /**
     * Create an instance of {@link StarJoinInfoType_sql2014sp2 }
     * 
     */
    public StarJoinInfoType_sql2014sp2 createStarJoinInfoType_sql2014sp2() {
        return new StarJoinInfoType_sql2014sp2();
    }

    /**
     * Create an instance of {@link ColumnType_sql2014sp2 }
     * 
     */
    public ColumnType_sql2014sp2 createColumnType_sql2014sp2() {
        return new ColumnType_sql2014sp2();
    }

    /**
     * Create an instance of {@link ColumnGroupType_sql2014sp2 }
     * 
     */
    public ColumnGroupType_sql2014sp2 createColumnGroupType_sql2014sp2() {
        return new ColumnGroupType_sql2014sp2();
    }

    /**
     * Create an instance of {@link RemoteRangeType_sql2014sp2 }
     * 
     */
    public RemoteRangeType_sql2014sp2 createRemoteRangeType_sql2014sp2() {
        return new RemoteRangeType_sql2014sp2();
    }

    /**
     * Create an instance of {@link BaseStmtInfoType_sql2014sp2 }
     * 
     */
    public BaseStmtInfoType_sql2014sp2 createBaseStmtInfoType_sql2014sp2() {
        return new BaseStmtInfoType_sql2014sp2();
    }

    /**
     * Create an instance of {@link LogicalType_sql2014sp2 }
     * 
     */
    public LogicalType_sql2014sp2 createLogicalType_sql2014sp2() {
        return new LogicalType_sql2014sp2();
    }

    /**
     * Create an instance of {@link SubqueryType_sql2014sp2 }
     * 
     */
    public SubqueryType_sql2014sp2 createSubqueryType_sql2014sp2() {
        return new SubqueryType_sql2014sp2();
    }

    /**
     * Create an instance of {@link AggregateType_sql2014sp2 }
     * 
     */
    public AggregateType_sql2014sp2 createAggregateType_sql2014sp2() {
        return new AggregateType_sql2014sp2();
    }

    /**
     * Create an instance of {@link TraceFlagListType_sql2014sp2 }
     * 
     */
    public TraceFlagListType_sql2014sp2 createTraceFlagListType_sql2014sp2() {
        return new TraceFlagListType_sql2014sp2();
    }

    /**
     * Create an instance of {@link RelOpType_sql2014sp2 }
     * 
     */
    public RelOpType_sql2014sp2 createRelOpType_sql2014sp2() {
        return new RelOpType_sql2014sp2();
    }

    /**
     * Create an instance of {@link SeekPredicateNewType_sql2014sp2 }
     * 
     */
    public SeekPredicateNewType_sql2014sp2 createSeekPredicateNewType_sql2014sp2() {
        return new SeekPredicateNewType_sql2014sp2();
    }

    /**
     * Create an instance of {@link InternalInfoType_sql2014sp2 }
     * 
     */
    public InternalInfoType_sql2014sp2 createInternalInfoType_sql2014sp2() {
        return new InternalInfoType_sql2014sp2();
    }

    /**
     * Create an instance of {@link MemoryFractionsType_sql2014sp2 }
     * 
     */
    public MemoryFractionsType_sql2014sp2 createMemoryFractionsType_sql2014sp2() {
        return new MemoryFractionsType_sql2014sp2();
    }

    /**
     * Create an instance of {@link ConvertType_sql2014sp2 }
     * 
     */
    public ConvertType_sql2014sp2 createConvertType_sql2014sp2() {
        return new ConvertType_sql2014sp2();
    }

    /**
     * Create an instance of {@link SpillToTempDbType_sql2014sp2 }
     * 
     */
    public SpillToTempDbType_sql2014sp2 createSpillToTempDbType_sql2014sp2() {
        return new SpillToTempDbType_sql2014sp2();
    }

    /**
     * Create an instance of {@link IdentType_sql2014sp2 }
     * 
     */
    public IdentType_sql2014sp2 createIdentType_sql2014sp2() {
        return new IdentType_sql2014sp2();
    }

    /**
     * Create an instance of {@link UnmatchedIndexesType_sql2014sp2 }
     * 
     */
    public UnmatchedIndexesType_sql2014sp2 createUnmatchedIndexesType_sql2014sp2() {
        return new UnmatchedIndexesType_sql2014sp2();
    }

    /**
     * Create an instance of {@link RemoteFetchType_sql2014sp2 }
     * 
     */
    public RemoteFetchType_sql2014sp2 createRemoteFetchType_sql2014sp2() {
        return new RemoteFetchType_sql2014sp2();
    }

    /**
     * Create an instance of {@link CLRFunctionType_sql2014sp2 }
     * 
     */
    public CLRFunctionType_sql2014sp2 createCLRFunctionType_sql2014sp2() {
        return new CLRFunctionType_sql2014sp2();
    }

    /**
     * Create an instance of {@link SortSpillDetailsType_sql2014sp2 }
     * 
     */
    public SortSpillDetailsType_sql2014sp2 createSortSpillDetailsType_sql2014sp2() {
        return new SortSpillDetailsType_sql2014sp2();
    }

    /**
     * Create an instance of {@link RemoteQueryType_sql2014sp2 }
     * 
     */
    public RemoteQueryType_sql2014sp2 createRemoteQueryType_sql2014sp2() {
        return new RemoteQueryType_sql2014sp2();
    }

    /**
     * Create an instance of {@link SwitchType_sql2014sp2 }
     * 
     */
    public SwitchType_sql2014sp2 createSwitchType_sql2014sp2() {
        return new SwitchType_sql2014sp2();
    }

    /**
     * Create an instance of {@link IntrinsicType_sql2014sp2 }
     * 
     */
    public IntrinsicType_sql2014sp2 createIntrinsicType_sql2014sp2() {
        return new IntrinsicType_sql2014sp2();
    }

    /**
     * Create an instance of {@link FilterType_sql2014sp2 }
     * 
     */
    public FilterType_sql2014sp2 createFilterType_sql2014sp2() {
        return new FilterType_sql2014sp2();
    }

    /**
     * Create an instance of {@link ThreadStatType_sql2014sp2 }
     * 
     */
    public ThreadStatType_sql2014sp2 createThreadStatType_sql2014sp2() {
        return new ThreadStatType_sql2014sp2();
    }

    /**
     * Create an instance of {@link WaitWarningType_sql2014sp2 }
     * 
     */
    public WaitWarningType_sql2014sp2 createWaitWarningType_sql2014sp2() {
        return new WaitWarningType_sql2014sp2();
    }

    /**
     * Create an instance of {@link TableScanType_sql2014sp2 }
     * 
     */
    public TableScanType_sql2014sp2 createTableScanType_sql2014sp2() {
        return new TableScanType_sql2014sp2();
    }

    /**
     * Create an instance of {@link ThreadReservationType_sql2014sp2 }
     * 
     */
    public ThreadReservationType_sql2014sp2 createThreadReservationType_sql2014sp2() {
        return new ThreadReservationType_sql2014sp2();
    }

    /**
     * Create an instance of {@link ScalarInsertType_sql2014sp2 }
     * 
     */
    public ScalarInsertType_sql2014sp2 createScalarInsertType_sql2014sp2() {
        return new ScalarInsertType_sql2014sp2();
    }

    /**
     * Create an instance of {@link MemoryGrantWarningInfo_sql2014sp2 }
     * 
     */
    public MemoryGrantWarningInfo_sql2014sp2 createMemoryGrantWarningInfo_sql2014sp2() {
        return new MemoryGrantWarningInfo_sql2014sp2();
    }

    /**
     * Create an instance of {@link WindowType_sql2014sp2 }
     * 
     */
    public WindowType_sql2014sp2 createWindowType_sql2014sp2() {
        return new WindowType_sql2014sp2();
    }

    /**
     * Create an instance of {@link ColumnReferenceType_sql2014sp2 }
     * 
     */
    public ColumnReferenceType_sql2014sp2 createColumnReferenceType_sql2014sp2() {
        return new ColumnReferenceType_sql2014sp2();
    }

    /**
     * Create an instance of {@link BitmapType_sql2014sp2 }
     * 
     */
    public BitmapType_sql2014sp2 createBitmapType_sql2014sp2() {
        return new BitmapType_sql2014sp2();
    }

    /**
     * Create an instance of {@link SetPredicateElementType_sql2014sp2 }
     * 
     */
    public SetPredicateElementType_sql2014sp2 createSetPredicateElementType_sql2014sp2() {
        return new SetPredicateElementType_sql2014sp2();
    }

    /**
     * Create an instance of {@link SpoolType_sql2014sp2 }
     * 
     */
    public SpoolType_sql2014sp2 createSpoolType_sql2014sp2() {
        return new SpoolType_sql2014sp2();
    }

    /**
     * Create an instance of {@link ScalarType_sql2014sp2 }
     * 
     */
    public ScalarType_sql2014sp2 createScalarType_sql2014sp2() {
        return new ScalarType_sql2014sp2();
    }

    /**
     * Create an instance of {@link StreamAggregateType_sql2014sp2 }
     * 
     */
    public StreamAggregateType_sql2014sp2 createStreamAggregateType_sql2014sp2() {
        return new StreamAggregateType_sql2014sp2();
    }

    /**
     * Create an instance of {@link RollupLevelType_sql2014sp2 }
     * 
     */
    public RollupLevelType_sql2014sp2 createRollupLevelType_sql2014sp2() {
        return new RollupLevelType_sql2014sp2();
    }

    /**
     * Create an instance of {@link MemoryGrantType_sql2014sp2 }
     * 
     */
    public MemoryGrantType_sql2014sp2 createMemoryGrantType_sql2014sp2() {
        return new MemoryGrantType_sql2014sp2();
    }

    /**
     * Create an instance of {@link SimpleIteratorOneChildType_sql2014sp2 }
     * 
     */
    public SimpleIteratorOneChildType_sql2014sp2 createSimpleIteratorOneChildType_sql2014sp2() {
        return new SimpleIteratorOneChildType_sql2014sp2();
    }

    /**
     * Create an instance of {@link ComputeScalarType_sql2014sp2 }
     * 
     */
    public ComputeScalarType_sql2014sp2 createComputeScalarType_sql2014sp2() {
        return new ComputeScalarType_sql2014sp2();
    }

    /**
     * Create an instance of {@link NestedLoopsType_sql2014sp2 }
     * 
     */
    public NestedLoopsType_sql2014sp2 createNestedLoopsType_sql2014sp2() {
        return new NestedLoopsType_sql2014sp2();
    }

    /**
     * Create an instance of {@link ParameterizationType_sql2014sp2 }
     * 
     */
    public ParameterizationType_sql2014sp2 createParameterizationType_sql2014sp2() {
        return new ParameterizationType_sql2014sp2();
    }

    /**
     * Create an instance of {@link SplitType_sql2014sp2 }
     * 
     */
    public SplitType_sql2014sp2 createSplitType_sql2014sp2() {
        return new SplitType_sql2014sp2();
    }

    /**
     * Create an instance of {@link StmtUseDbType_sql2014sp2 }
     * 
     */
    public StmtUseDbType_sql2014sp2 createStmtUseDbType_sql2014sp2() {
        return new StmtUseDbType_sql2014sp2();
    }

    /**
     * Create an instance of {@link MissingIndexType_sql2014sp2 }
     * 
     */
    public MissingIndexType_sql2014sp2 createMissingIndexType_sql2014sp2() {
        return new MissingIndexType_sql2014sp2();
    }

    /**
     * Create an instance of {@link ScalarSequenceType_sql2014sp2 }
     * 
     */
    public ScalarSequenceType_sql2014sp2 createScalarSequenceType_sql2014sp2() {
        return new ScalarSequenceType_sql2014sp2();
    }

    /**
     * Create an instance of {@link QueryPlanType_sql2014sp2 }
     * 
     */
    public QueryPlanType_sql2014sp2 createQueryPlanType_sql2014sp2() {
        return new QueryPlanType_sql2014sp2();
    }

    /**
     * Create an instance of {@link ScalarExpressionType_sql2014sp2 }
     * 
     */
    public ScalarExpressionType_sql2014sp2 createScalarExpressionType_sql2014sp2() {
        return new ScalarExpressionType_sql2014sp2();
    }

    /**
     * Create an instance of {@link RowsetType_sql2014sp2 }
     * 
     */
    public RowsetType_sql2014sp2 createRowsetType_sql2014sp2() {
        return new RowsetType_sql2014sp2();
    }

    /**
     * Create an instance of {@link UpdateType_sql2014sp2 }
     * 
     */
    public UpdateType_sql2014sp2 createUpdateType_sql2014sp2() {
        return new UpdateType_sql2014sp2();
    }

    /**
     * Create an instance of {@link AffectingConvertWarningType_sql2014sp2 }
     * 
     */
    public AffectingConvertWarningType_sql2014sp2 createAffectingConvertWarningType_sql2014sp2() {
        return new AffectingConvertWarningType_sql2014sp2();
    }

    /**
     * Create an instance of {@link SortType_sql2014sp2 }
     * 
     */
    public SortType_sql2014sp2 createSortType_sql2014sp2() {
        return new SortType_sql2014sp2();
    }

    /**
     * Create an instance of {@link ScanRangeType_sql2014sp2 }
     * 
     */
    public ScanRangeType_sql2014sp2 createScanRangeType_sql2014sp2() {
        return new ScanRangeType_sql2014sp2();
    }

    /**
     * Create an instance of {@link UDAggregateType_sql2014sp2 }
     * 
     */
    public UDAggregateType_sql2014sp2 createUDAggregateType_sql2014sp2() {
        return new UDAggregateType_sql2014sp2();
    }

    /**
     * Create an instance of {@link IndexScanType_sql2014sp2 }
     * 
     */
    public IndexScanType_sql2014sp2 createIndexScanType_sql2014sp2() {
        return new IndexScanType_sql2014sp2();
    }

    /**
     * Create an instance of {@link TopSortType_sql2014sp2 }
     * 
     */
    public TopSortType_sql2014sp2 createTopSortType_sql2014sp2() {
        return new TopSortType_sql2014sp2();
    }

    /**
     * Create an instance of {@link GuessedSelectivityType_sql2014sp2 }
     * 
     */
    public GuessedSelectivityType_sql2014sp2 createGuessedSelectivityType_sql2014sp2() {
        return new GuessedSelectivityType_sql2014sp2();
    }

    /**
     * Create an instance of {@link UDXType_sql2014sp2 }
     * 
     */
    public UDXType_sql2014sp2 createUDXType_sql2014sp2() {
        return new UDXType_sql2014sp2();
    }

    /**
     * Create an instance of {@link WarningsType_sql2014sp2 }
     * 
     */
    public WarningsType_sql2014sp2 createWarningsType_sql2014sp2() {
        return new WarningsType_sql2014sp2();
    }

    /**
     * Create an instance of {@link ParallelismType_sql2014sp2 .Activation_sql2014sp2 }
     * 
     */
    public ParallelismType_sql2014sp2 .Activation_sql2014sp2 createParallelismType_sql2014sp2Activation_sql2014sp2() {
        return new ParallelismType_sql2014sp2 .Activation_sql2014sp2();
    }

    /**
     * Create an instance of {@link ParallelismType_sql2014sp2 .BrickRouting_sql2014sp2 }
     * 
     */
    public ParallelismType_sql2014sp2 .BrickRouting_sql2014sp2 createParallelismType_sql2014sp2BrickRouting_sql2014sp2() {
        return new ParallelismType_sql2014sp2 .BrickRouting_sql2014sp2();
    }

    /**
     * Create an instance of {@link ConstantScanType_sql2014sp2 .Values_sql2014sp2 }
     * 
     */
    public ConstantScanType_sql2014sp2 .Values_sql2014sp2 createConstantScanType_sql2014sp2Values_sql2014sp2() {
        return new ConstantScanType_sql2014sp2 .Values_sql2014sp2();
    }

    /**
     * Create an instance of {@link CursorPlanType_sql2014sp2 .Operation_sql2014sp2 }
     * 
     */
    public CursorPlanType_sql2014sp2 .Operation_sql2014sp2 createCursorPlanType_sql2014sp2Operation_sql2014sp2() {
        return new CursorPlanType_sql2014sp2 .Operation_sql2014sp2();
    }

    /**
     * Create an instance of {@link RunTimeInformationType_sql2014sp2 .RunTimeCountersPerThread_sql2014sp2 }
     * 
     */
    public RunTimeInformationType_sql2014sp2 .RunTimeCountersPerThread_sql2014sp2 createRunTimeInformationType_sql2014sp2RunTimeCountersPerThread_sql2014sp2() {
        return new RunTimeInformationType_sql2014sp2 .RunTimeCountersPerThread_sql2014sp2();
    }

    /**
     * Create an instance of {@link ReceivePlanType_sql2014sp2 .Operation_sql2014sp2 }
     * 
     */
    public ReceivePlanType_sql2014sp2 .Operation_sql2014sp2 createReceivePlanType_sql2014sp2Operation_sql2014sp2() {
        return new ReceivePlanType_sql2014sp2 .Operation_sql2014sp2();
    }

    /**
     * Create an instance of {@link DefinedValuesListType_sql2014sp2 .DefinedValue_sql2014sp2 .ValueVector_sql2014sp2 }
     * 
     */
    public DefinedValuesListType_sql2014sp2 .DefinedValue_sql2014sp2 .ValueVector_sql2014sp2 createDefinedValuesListType_sql2014sp2DefinedValue_sql2014sp2ValueVector_sql2014sp2() {
        return new DefinedValuesListType_sql2014sp2 .DefinedValue_sql2014sp2 .ValueVector_sql2014sp2();
    }

    /**
     * Create an instance of {@link RunTimePartitionSummaryType_sql2014sp2 .PartitionsAccessed_sql2014sp2 .PartitionRange_sql2014sp2 }
     * 
     */
    public RunTimePartitionSummaryType_sql2014sp2 .PartitionsAccessed_sql2014sp2 .PartitionRange_sql2014sp2 createRunTimePartitionSummaryType_sql2014sp2PartitionsAccessed_sql2014sp2PartitionRange_sql2014sp2() {
        return new RunTimePartitionSummaryType_sql2014sp2 .PartitionsAccessed_sql2014sp2 .PartitionRange_sql2014sp2();
    }

    /**
     * Create an instance of {@link OrderByType_sql2014sp2 .OrderByColumn_sql2014sp2 }
     * 
     */
    public OrderByType_sql2014sp2 .OrderByColumn_sql2014sp2 createOrderByType_sql2014sp2OrderByColumn_sql2014sp2() {
        return new OrderByType_sql2014sp2 .OrderByColumn_sql2014sp2();
    }

    /**
     * Create an instance of {@link StmtCondType_sql2014sp2 .Condition_sql2014sp2 }
     * 
     */
    public StmtCondType_sql2014sp2 .Condition_sql2014sp2 createStmtCondType_sql2014sp2Condition_sql2014sp2() {
        return new StmtCondType_sql2014sp2 .Condition_sql2014sp2();
    }

    /**
     * Create an instance of {@link StmtCondType_sql2014sp2 .Then_sql2014sp2 }
     * 
     */
    public StmtCondType_sql2014sp2 .Then_sql2014sp2 createStmtCondType_sql2014sp2Then_sql2014sp2() {
        return new StmtCondType_sql2014sp2 .Then_sql2014sp2();
    }

    /**
     * Create an instance of {@link StmtCondType_sql2014sp2 .Else_sql2014sp2 }
     * 
     */
    public StmtCondType_sql2014sp2 .Else_sql2014sp2 createStmtCondType_sql2014sp2Else_sql2014sp2() {
        return new StmtCondType_sql2014sp2 .Else_sql2014sp2();
    }

    /**
     * Create an instance of {@link ShowPlanXML.BatchSequence_sql2014sp2 .Batch_sql2014sp2 }
     * 
     */
    public ShowPlanXML.BatchSequence_sql2014sp2 .Batch_sql2014sp2 createShowPlanXMLBatchSequence_sql2014sp2Batch_sql2014sp2() {
        return new ShowPlanXML.BatchSequence_sql2014sp2 .Batch_sql2014sp2();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DefinedValuesListType_sql2014sp2 .DefinedValue_sql2014sp2 .ValueVector_sql2014sp2 }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", name = "ValueVector", scope = DefinedValuesListType_sql2014sp2 .DefinedValue_sql2014sp2 .class)
    public JAXBElement<DefinedValuesListType_sql2014sp2 .DefinedValue_sql2014sp2 .ValueVector_sql2014sp2> createDefinedValuesListType_sql2014sp2DefinedValue_sql2014sp2ValueVector(DefinedValuesListType_sql2014sp2 .DefinedValue_sql2014sp2 .ValueVector_sql2014sp2 value) {
        return new JAXBElement<DefinedValuesListType_sql2014sp2 .DefinedValue_sql2014sp2 .ValueVector_sql2014sp2>(_DefinedValuesListType_sql2014sp2DefinedValue_sql2014sp2ValueVector_QNAME, DefinedValuesListType_sql2014sp2 .DefinedValue_sql2014sp2 .ValueVector_sql2014sp2 .class, DefinedValuesListType_sql2014sp2 .DefinedValue_sql2014sp2 .class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ScalarType_sql2014sp2 }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", name = "ScalarOperator", scope = DefinedValuesListType_sql2014sp2 .DefinedValue_sql2014sp2 .class)
    public JAXBElement<ScalarType_sql2014sp2> createDefinedValuesListType_sql2014sp2DefinedValue_sql2014sp2ScalarOperator(ScalarType_sql2014sp2 value) {
        return new JAXBElement<ScalarType_sql2014sp2>(_DefinedValuesListType_sql2014sp2DefinedValue_sql2014sp2ScalarOperator_QNAME, ScalarType_sql2014sp2 .class, DefinedValuesListType_sql2014sp2 .DefinedValue_sql2014sp2 .class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ColumnReferenceType_sql2014sp2 }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", name = "ColumnReference", scope = DefinedValuesListType_sql2014sp2 .DefinedValue_sql2014sp2 .class)
    public JAXBElement<ColumnReferenceType_sql2014sp2> createDefinedValuesListType_sql2014sp2DefinedValue_sql2014sp2ColumnReference(ColumnReferenceType_sql2014sp2 value) {
        return new JAXBElement<ColumnReferenceType_sql2014sp2>(_DefinedValuesListType_sql2014sp2DefinedValue_sql2014sp2ColumnReference_QNAME, ColumnReferenceType_sql2014sp2 .class, DefinedValuesListType_sql2014sp2 .DefinedValue_sql2014sp2 .class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ScalarType_sql2014sp2 }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", name = "ScalarOperator", scope = AssignType_sql2014sp2 .class)
    public JAXBElement<ScalarType_sql2014sp2> createAssignType_sql2014sp2ScalarOperator(ScalarType_sql2014sp2 value) {
        return new JAXBElement<ScalarType_sql2014sp2>(_DefinedValuesListType_sql2014sp2DefinedValue_sql2014sp2ScalarOperator_QNAME, ScalarType_sql2014sp2 .class, AssignType_sql2014sp2 .class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ColumnReferenceType_sql2014sp2 }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", name = "ColumnReference", scope = AssignType_sql2014sp2 .class)
    public JAXBElement<ColumnReferenceType_sql2014sp2> createAssignType_sql2014sp2ColumnReference(ColumnReferenceType_sql2014sp2 value) {
        return new JAXBElement<ColumnReferenceType_sql2014sp2>(_DefinedValuesListType_sql2014sp2DefinedValue_sql2014sp2ColumnReference_QNAME, ColumnReferenceType_sql2014sp2 .class, AssignType_sql2014sp2 .class, value);
    }

}
