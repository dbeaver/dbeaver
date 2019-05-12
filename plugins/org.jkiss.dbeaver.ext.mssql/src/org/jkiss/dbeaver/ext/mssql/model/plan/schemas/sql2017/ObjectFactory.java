
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2017;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2017 package. 
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

    private final static QName _AssignType_sql2017ScalarOperator_QNAME = new QName("http://schemas.microsoft.com/sqlserver/2004/07/showplan", "ScalarOperator");
    private final static QName _AssignType_sql2017ColumnReference_QNAME = new QName("http://schemas.microsoft.com/sqlserver/2004/07/showplan", "ColumnReference");
    private final static QName _DefinedValuesListType_sql2017DefinedValue_sql2017ValueVector_QNAME = new QName("http://schemas.microsoft.com/sqlserver/2004/07/showplan", "ValueVector");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2017
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
     * Create an instance of {@link ParallelismType_sql2017 }
     * 
     */
    public ParallelismType_sql2017 createParallelismType_sql2017() {
        return new ParallelismType_sql2017();
    }

    /**
     * Create an instance of {@link ConstantScanType_sql2017 }
     * 
     */
    public ConstantScanType_sql2017 createConstantScanType_sql2017() {
        return new ConstantScanType_sql2017();
    }

    /**
     * Create an instance of {@link CursorPlanType_sql2017 }
     * 
     */
    public CursorPlanType_sql2017 createCursorPlanType_sql2017() {
        return new CursorPlanType_sql2017();
    }

    /**
     * Create an instance of {@link RunTimeInformationType_sql2017 }
     * 
     */
    public RunTimeInformationType_sql2017 createRunTimeInformationType_sql2017() {
        return new RunTimeInformationType_sql2017();
    }

    /**
     * Create an instance of {@link ReceivePlanType_sql2017 }
     * 
     */
    public ReceivePlanType_sql2017 createReceivePlanType_sql2017() {
        return new ReceivePlanType_sql2017();
    }

    /**
     * Create an instance of {@link DefinedValuesListType_sql2017 }
     * 
     */
    public DefinedValuesListType_sql2017 createDefinedValuesListType_sql2017() {
        return new DefinedValuesListType_sql2017();
    }

    /**
     * Create an instance of {@link DefinedValuesListType_sql2017 .DefinedValue_sql2017 }
     * 
     */
    public DefinedValuesListType_sql2017 .DefinedValue_sql2017 createDefinedValuesListType_sql2017DefinedValue_sql2017() {
        return new DefinedValuesListType_sql2017 .DefinedValue_sql2017();
    }

    /**
     * Create an instance of {@link RunTimePartitionSummaryType_sql2017 }
     * 
     */
    public RunTimePartitionSummaryType_sql2017 createRunTimePartitionSummaryType_sql2017() {
        return new RunTimePartitionSummaryType_sql2017();
    }

    /**
     * Create an instance of {@link RunTimePartitionSummaryType_sql2017 .PartitionsAccessed_sql2017 }
     * 
     */
    public RunTimePartitionSummaryType_sql2017 .PartitionsAccessed_sql2017 createRunTimePartitionSummaryType_sql2017PartitionsAccessed_sql2017() {
        return new RunTimePartitionSummaryType_sql2017 .PartitionsAccessed_sql2017();
    }

    /**
     * Create an instance of {@link OrderByType_sql2017 }
     * 
     */
    public OrderByType_sql2017 createOrderByType_sql2017() {
        return new OrderByType_sql2017();
    }

    /**
     * Create an instance of {@link StmtCondType_sql2017 }
     * 
     */
    public StmtCondType_sql2017 createStmtCondType_sql2017() {
        return new StmtCondType_sql2017();
    }

    /**
     * Create an instance of {@link ShowPlanXML.BatchSequence_sql2017 }
     * 
     */
    public ShowPlanXML.BatchSequence_sql2017 createShowPlanXMLBatchSequence_sql2017() {
        return new ShowPlanXML.BatchSequence_sql2017();
    }

    /**
     * Create an instance of {@link SeekPredicatePartType_sql2017 }
     * 
     */
    public SeekPredicatePartType_sql2017 createSeekPredicatePartType_sql2017() {
        return new SeekPredicatePartType_sql2017();
    }

    /**
     * Create an instance of {@link UDFType_sql2017 }
     * 
     */
    public UDFType_sql2017 createUDFType_sql2017() {
        return new UDFType_sql2017();
    }

    /**
     * Create an instance of {@link ScalarExpressionListType_sql2017 }
     * 
     */
    public ScalarExpressionListType_sql2017 createScalarExpressionListType_sql2017() {
        return new ScalarExpressionListType_sql2017();
    }

    /**
     * Create an instance of {@link SeekPredicatesType_sql2017 }
     * 
     */
    public SeekPredicatesType_sql2017 createSeekPredicatesType_sql2017() {
        return new SeekPredicatesType_sql2017();
    }

    /**
     * Create an instance of {@link QueryExecTimeType_sql2017 }
     * 
     */
    public QueryExecTimeType_sql2017 createQueryExecTimeType_sql2017() {
        return new QueryExecTimeType_sql2017();
    }

    /**
     * Create an instance of {@link AssignType_sql2017 }
     * 
     */
    public AssignType_sql2017 createAssignType_sql2017() {
        return new AssignType_sql2017();
    }

    /**
     * Create an instance of {@link StmtSimpleType_sql2017 }
     * 
     */
    public StmtSimpleType_sql2017 createStmtSimpleType_sql2017() {
        return new StmtSimpleType_sql2017();
    }

    /**
     * Create an instance of {@link RollupInfoType_sql2017 }
     * 
     */
    public RollupInfoType_sql2017 createRollupInfoType_sql2017() {
        return new RollupInfoType_sql2017();
    }

    /**
     * Create an instance of {@link MergeType_sql2017 }
     * 
     */
    public MergeType_sql2017 createMergeType_sql2017() {
        return new MergeType_sql2017();
    }

    /**
     * Create an instance of {@link TableValuedFunctionType_sql2017 }
     * 
     */
    public TableValuedFunctionType_sql2017 createTableValuedFunctionType_sql2017() {
        return new TableValuedFunctionType_sql2017();
    }

    /**
     * Create an instance of {@link RemoteType_sql2017 }
     * 
     */
    public RemoteType_sql2017 createRemoteType_sql2017() {
        return new RemoteType_sql2017();
    }

    /**
     * Create an instance of {@link CollapseType_sql2017 }
     * 
     */
    public CollapseType_sql2017 createCollapseType_sql2017() {
        return new CollapseType_sql2017();
    }

    /**
     * Create an instance of {@link MultAssignType_sql2017 }
     * 
     */
    public MultAssignType_sql2017 createMultAssignType_sql2017() {
        return new MultAssignType_sql2017();
    }

    /**
     * Create an instance of {@link HashType_sql2017 }
     * 
     */
    public HashType_sql2017 createHashType_sql2017() {
        return new HashType_sql2017();
    }

    /**
     * Create an instance of {@link StmtCursorType_sql2017 }
     * 
     */
    public StmtCursorType_sql2017 createStmtCursorType_sql2017() {
        return new StmtCursorType_sql2017();
    }

    /**
     * Create an instance of {@link ColumnReferenceListType_sql2017 }
     * 
     */
    public ColumnReferenceListType_sql2017 createColumnReferenceListType_sql2017() {
        return new ColumnReferenceListType_sql2017();
    }

    /**
     * Create an instance of {@link ConcatType_sql2017 }
     * 
     */
    public ConcatType_sql2017 createConcatType_sql2017() {
        return new ConcatType_sql2017();
    }

    /**
     * Create an instance of {@link ArithmeticType_sql2017 }
     * 
     */
    public ArithmeticType_sql2017 createArithmeticType_sql2017() {
        return new ArithmeticType_sql2017();
    }

    /**
     * Create an instance of {@link MissingIndexGroupType_sql2017 }
     * 
     */
    public MissingIndexGroupType_sql2017 createMissingIndexGroupType_sql2017() {
        return new MissingIndexGroupType_sql2017();
    }

    /**
     * Create an instance of {@link UDTMethodType_sql2017 }
     * 
     */
    public UDTMethodType_sql2017 createUDTMethodType_sql2017() {
        return new UDTMethodType_sql2017();
    }

    /**
     * Create an instance of {@link SeekPredicateType_sql2017 }
     * 
     */
    public SeekPredicateType_sql2017 createSeekPredicateType_sql2017() {
        return new SeekPredicateType_sql2017();
    }

    /**
     * Create an instance of {@link TraceFlagType_sql2017 }
     * 
     */
    public TraceFlagType_sql2017 createTraceFlagType_sql2017() {
        return new TraceFlagType_sql2017();
    }

    /**
     * Create an instance of {@link StmtReceiveType_sql2017 }
     * 
     */
    public StmtReceiveType_sql2017 createStmtReceiveType_sql2017() {
        return new StmtReceiveType_sql2017();
    }

    /**
     * Create an instance of {@link HashSpillDetailsType_sql2017 }
     * 
     */
    public HashSpillDetailsType_sql2017 createHashSpillDetailsType_sql2017() {
        return new HashSpillDetailsType_sql2017();
    }

    /**
     * Create an instance of {@link TopType_sql2017 }
     * 
     */
    public TopType_sql2017 createTopType_sql2017() {
        return new TopType_sql2017();
    }

    /**
     * Create an instance of {@link OptimizerHardwareDependentPropertiesType_sql2017 }
     * 
     */
    public OptimizerHardwareDependentPropertiesType_sql2017 createOptimizerHardwareDependentPropertiesType_sql2017() {
        return new OptimizerHardwareDependentPropertiesType_sql2017();
    }

    /**
     * Create an instance of {@link CompareType_sql2017 }
     * 
     */
    public CompareType_sql2017 createCompareType_sql2017() {
        return new CompareType_sql2017();
    }

    /**
     * Create an instance of {@link StmtBlockType_sql2017 }
     * 
     */
    public StmtBlockType_sql2017 createStmtBlockType_sql2017() {
        return new StmtBlockType_sql2017();
    }

    /**
     * Create an instance of {@link SequenceType_sql2017 }
     * 
     */
    public SequenceType_sql2017 createSequenceType_sql2017() {
        return new SequenceType_sql2017();
    }

    /**
     * Create an instance of {@link MissingIndexesType_sql2017 }
     * 
     */
    public MissingIndexesType_sql2017 createMissingIndexesType_sql2017() {
        return new MissingIndexesType_sql2017();
    }

    /**
     * Create an instance of {@link ObjectType_sql2017 }
     * 
     */
    public ObjectType_sql2017 createObjectType_sql2017() {
        return new ObjectType_sql2017();
    }

    /**
     * Create an instance of {@link IndexedViewInfoType_sql2017 }
     * 
     */
    public IndexedViewInfoType_sql2017 createIndexedViewInfoType_sql2017() {
        return new IndexedViewInfoType_sql2017();
    }

    /**
     * Create an instance of {@link RemoteModifyType_sql2017 }
     * 
     */
    public RemoteModifyType_sql2017 createRemoteModifyType_sql2017() {
        return new RemoteModifyType_sql2017();
    }

    /**
     * Create an instance of {@link SingleColumnReferenceType_sql2017 }
     * 
     */
    public SingleColumnReferenceType_sql2017 createSingleColumnReferenceType_sql2017() {
        return new SingleColumnReferenceType_sql2017();
    }

    /**
     * Create an instance of {@link ConstType_sql2017 }
     * 
     */
    public ConstType_sql2017 createConstType_sql2017() {
        return new ConstType_sql2017();
    }

    /**
     * Create an instance of {@link ConditionalType_sql2017 }
     * 
     */
    public ConditionalType_sql2017 createConditionalType_sql2017() {
        return new ConditionalType_sql2017();
    }

    /**
     * Create an instance of {@link GenericType_sql2017 }
     * 
     */
    public GenericType_sql2017 createGenericType_sql2017() {
        return new GenericType_sql2017();
    }

    /**
     * Create an instance of {@link SetOptionsType_sql2017 }
     * 
     */
    public SetOptionsType_sql2017 createSetOptionsType_sql2017() {
        return new SetOptionsType_sql2017();
    }

    /**
     * Create an instance of {@link FunctionType_sql2017 }
     * 
     */
    public FunctionType_sql2017 createFunctionType_sql2017() {
        return new FunctionType_sql2017();
    }

    /**
     * Create an instance of {@link RelOpBaseType_sql2017 }
     * 
     */
    public RelOpBaseType_sql2017 createRelOpBaseType_sql2017() {
        return new RelOpBaseType_sql2017();
    }

    /**
     * Create an instance of {@link CreateIndexType_sql2017 }
     * 
     */
    public CreateIndexType_sql2017 createCreateIndexType_sql2017() {
        return new CreateIndexType_sql2017();
    }

    /**
     * Create an instance of {@link SegmentType_sql2017 }
     * 
     */
    public SegmentType_sql2017 createSegmentType_sql2017() {
        return new SegmentType_sql2017();
    }

    /**
     * Create an instance of {@link BatchHashTableBuildType_sql2017 }
     * 
     */
    public BatchHashTableBuildType_sql2017 createBatchHashTableBuildType_sql2017() {
        return new BatchHashTableBuildType_sql2017();
    }

    /**
     * Create an instance of {@link SimpleUpdateType_sql2017 }
     * 
     */
    public SimpleUpdateType_sql2017 createSimpleUpdateType_sql2017() {
        return new SimpleUpdateType_sql2017();
    }

    /**
     * Create an instance of {@link StarJoinInfoType_sql2017 }
     * 
     */
    public StarJoinInfoType_sql2017 createStarJoinInfoType_sql2017() {
        return new StarJoinInfoType_sql2017();
    }

    /**
     * Create an instance of {@link ColumnType_sql2017 }
     * 
     */
    public ColumnType_sql2017 createColumnType_sql2017() {
        return new ColumnType_sql2017();
    }

    /**
     * Create an instance of {@link ColumnGroupType_sql2017 }
     * 
     */
    public ColumnGroupType_sql2017 createColumnGroupType_sql2017() {
        return new ColumnGroupType_sql2017();
    }

    /**
     * Create an instance of {@link RemoteRangeType_sql2017 }
     * 
     */
    public RemoteRangeType_sql2017 createRemoteRangeType_sql2017() {
        return new RemoteRangeType_sql2017();
    }

    /**
     * Create an instance of {@link BaseStmtInfoType_sql2017 }
     * 
     */
    public BaseStmtInfoType_sql2017 createBaseStmtInfoType_sql2017() {
        return new BaseStmtInfoType_sql2017();
    }

    /**
     * Create an instance of {@link LogicalType_sql2017 }
     * 
     */
    public LogicalType_sql2017 createLogicalType_sql2017() {
        return new LogicalType_sql2017();
    }

    /**
     * Create an instance of {@link WaitStatListType_sql2017 }
     * 
     */
    public WaitStatListType_sql2017 createWaitStatListType_sql2017() {
        return new WaitStatListType_sql2017();
    }

    /**
     * Create an instance of {@link SubqueryType_sql2017 }
     * 
     */
    public SubqueryType_sql2017 createSubqueryType_sql2017() {
        return new SubqueryType_sql2017();
    }

    /**
     * Create an instance of {@link AggregateType_sql2017 }
     * 
     */
    public AggregateType_sql2017 createAggregateType_sql2017() {
        return new AggregateType_sql2017();
    }

    /**
     * Create an instance of {@link TraceFlagListType_sql2017 }
     * 
     */
    public TraceFlagListType_sql2017 createTraceFlagListType_sql2017() {
        return new TraceFlagListType_sql2017();
    }

    /**
     * Create an instance of {@link WindowAggregateType_sql2017 }
     * 
     */
    public WindowAggregateType_sql2017 createWindowAggregateType_sql2017() {
        return new WindowAggregateType_sql2017();
    }

    /**
     * Create an instance of {@link RelOpType_sql2017 }
     * 
     */
    public RelOpType_sql2017 createRelOpType_sql2017() {
        return new RelOpType_sql2017();
    }

    /**
     * Create an instance of {@link SeekPredicateNewType_sql2017 }
     * 
     */
    public SeekPredicateNewType_sql2017 createSeekPredicateNewType_sql2017() {
        return new SeekPredicateNewType_sql2017();
    }

    /**
     * Create an instance of {@link InternalInfoType_sql2017 }
     * 
     */
    public InternalInfoType_sql2017 createInternalInfoType_sql2017() {
        return new InternalInfoType_sql2017();
    }

    /**
     * Create an instance of {@link MemoryFractionsType_sql2017 }
     * 
     */
    public MemoryFractionsType_sql2017 createMemoryFractionsType_sql2017() {
        return new MemoryFractionsType_sql2017();
    }

    /**
     * Create an instance of {@link ConvertType_sql2017 }
     * 
     */
    public ConvertType_sql2017 createConvertType_sql2017() {
        return new ConvertType_sql2017();
    }

    /**
     * Create an instance of {@link SpillToTempDbType_sql2017 }
     * 
     */
    public SpillToTempDbType_sql2017 createSpillToTempDbType_sql2017() {
        return new SpillToTempDbType_sql2017();
    }

    /**
     * Create an instance of {@link IdentType_sql2017 }
     * 
     */
    public IdentType_sql2017 createIdentType_sql2017() {
        return new IdentType_sql2017();
    }

    /**
     * Create an instance of {@link UnmatchedIndexesType_sql2017 }
     * 
     */
    public UnmatchedIndexesType_sql2017 createUnmatchedIndexesType_sql2017() {
        return new UnmatchedIndexesType_sql2017();
    }

    /**
     * Create an instance of {@link RemoteFetchType_sql2017 }
     * 
     */
    public RemoteFetchType_sql2017 createRemoteFetchType_sql2017() {
        return new RemoteFetchType_sql2017();
    }

    /**
     * Create an instance of {@link CLRFunctionType_sql2017 }
     * 
     */
    public CLRFunctionType_sql2017 createCLRFunctionType_sql2017() {
        return new CLRFunctionType_sql2017();
    }

    /**
     * Create an instance of {@link SortSpillDetailsType_sql2017 }
     * 
     */
    public SortSpillDetailsType_sql2017 createSortSpillDetailsType_sql2017() {
        return new SortSpillDetailsType_sql2017();
    }

    /**
     * Create an instance of {@link RemoteQueryType_sql2017 }
     * 
     */
    public RemoteQueryType_sql2017 createRemoteQueryType_sql2017() {
        return new RemoteQueryType_sql2017();
    }

    /**
     * Create an instance of {@link SwitchType_sql2017 }
     * 
     */
    public SwitchType_sql2017 createSwitchType_sql2017() {
        return new SwitchType_sql2017();
    }

    /**
     * Create an instance of {@link IntrinsicType_sql2017 }
     * 
     */
    public IntrinsicType_sql2017 createIntrinsicType_sql2017() {
        return new IntrinsicType_sql2017();
    }

    /**
     * Create an instance of {@link FilterType_sql2017 }
     * 
     */
    public FilterType_sql2017 createFilterType_sql2017() {
        return new FilterType_sql2017();
    }

    /**
     * Create an instance of {@link ThreadStatType_sql2017 }
     * 
     */
    public ThreadStatType_sql2017 createThreadStatType_sql2017() {
        return new ThreadStatType_sql2017();
    }

    /**
     * Create an instance of {@link WaitWarningType_sql2017 }
     * 
     */
    public WaitWarningType_sql2017 createWaitWarningType_sql2017() {
        return new WaitWarningType_sql2017();
    }

    /**
     * Create an instance of {@link AdaptiveJoinType_sql2017 }
     * 
     */
    public AdaptiveJoinType_sql2017 createAdaptiveJoinType_sql2017() {
        return new AdaptiveJoinType_sql2017();
    }

    /**
     * Create an instance of {@link TableScanType_sql2017 }
     * 
     */
    public TableScanType_sql2017 createTableScanType_sql2017() {
        return new TableScanType_sql2017();
    }

    /**
     * Create an instance of {@link ThreadReservationType_sql2017 }
     * 
     */
    public ThreadReservationType_sql2017 createThreadReservationType_sql2017() {
        return new ThreadReservationType_sql2017();
    }

    /**
     * Create an instance of {@link ForeignKeyReferencesCheckType_sql2017 }
     * 
     */
    public ForeignKeyReferencesCheckType_sql2017 createForeignKeyReferencesCheckType_sql2017() {
        return new ForeignKeyReferencesCheckType_sql2017();
    }

    /**
     * Create an instance of {@link ScalarInsertType_sql2017 }
     * 
     */
    public ScalarInsertType_sql2017 createScalarInsertType_sql2017() {
        return new ScalarInsertType_sql2017();
    }

    /**
     * Create an instance of {@link StatsInfoType_sql2017 }
     * 
     */
    public StatsInfoType_sql2017 createStatsInfoType_sql2017() {
        return new StatsInfoType_sql2017();
    }

    /**
     * Create an instance of {@link MemoryGrantWarningInfo_sql2017 }
     * 
     */
    public MemoryGrantWarningInfo_sql2017 createMemoryGrantWarningInfo_sql2017() {
        return new MemoryGrantWarningInfo_sql2017();
    }

    /**
     * Create an instance of {@link WindowType_sql2017 }
     * 
     */
    public WindowType_sql2017 createWindowType_sql2017() {
        return new WindowType_sql2017();
    }

    /**
     * Create an instance of {@link ColumnReferenceType_sql2017 }
     * 
     */
    public ColumnReferenceType_sql2017 createColumnReferenceType_sql2017() {
        return new ColumnReferenceType_sql2017();
    }

    /**
     * Create an instance of {@link BitmapType_sql2017 }
     * 
     */
    public BitmapType_sql2017 createBitmapType_sql2017() {
        return new BitmapType_sql2017();
    }

    /**
     * Create an instance of {@link SetPredicateElementType_sql2017 }
     * 
     */
    public SetPredicateElementType_sql2017 createSetPredicateElementType_sql2017() {
        return new SetPredicateElementType_sql2017();
    }

    /**
     * Create an instance of {@link SpoolType_sql2017 }
     * 
     */
    public SpoolType_sql2017 createSpoolType_sql2017() {
        return new SpoolType_sql2017();
    }

    /**
     * Create an instance of {@link ScalarType_sql2017 }
     * 
     */
    public ScalarType_sql2017 createScalarType_sql2017() {
        return new ScalarType_sql2017();
    }

    /**
     * Create an instance of {@link StreamAggregateType_sql2017 }
     * 
     */
    public StreamAggregateType_sql2017 createStreamAggregateType_sql2017() {
        return new StreamAggregateType_sql2017();
    }

    /**
     * Create an instance of {@link RollupLevelType_sql2017 }
     * 
     */
    public RollupLevelType_sql2017 createRollupLevelType_sql2017() {
        return new RollupLevelType_sql2017();
    }

    /**
     * Create an instance of {@link MemoryGrantType_sql2017 }
     * 
     */
    public MemoryGrantType_sql2017 createMemoryGrantType_sql2017() {
        return new MemoryGrantType_sql2017();
    }

    /**
     * Create an instance of {@link SimpleIteratorOneChildType_sql2017 }
     * 
     */
    public SimpleIteratorOneChildType_sql2017 createSimpleIteratorOneChildType_sql2017() {
        return new SimpleIteratorOneChildType_sql2017();
    }

    /**
     * Create an instance of {@link ComputeScalarType_sql2017 }
     * 
     */
    public ComputeScalarType_sql2017 createComputeScalarType_sql2017() {
        return new ComputeScalarType_sql2017();
    }

    /**
     * Create an instance of {@link NestedLoopsType_sql2017 }
     * 
     */
    public NestedLoopsType_sql2017 createNestedLoopsType_sql2017() {
        return new NestedLoopsType_sql2017();
    }

    /**
     * Create an instance of {@link ParameterizationType_sql2017 }
     * 
     */
    public ParameterizationType_sql2017 createParameterizationType_sql2017() {
        return new ParameterizationType_sql2017();
    }

    /**
     * Create an instance of {@link SplitType_sql2017 }
     * 
     */
    public SplitType_sql2017 createSplitType_sql2017() {
        return new SplitType_sql2017();
    }

    /**
     * Create an instance of {@link StmtUseDbType_sql2017 }
     * 
     */
    public StmtUseDbType_sql2017 createStmtUseDbType_sql2017() {
        return new StmtUseDbType_sql2017();
    }

    /**
     * Create an instance of {@link MissingIndexType_sql2017 }
     * 
     */
    public MissingIndexType_sql2017 createMissingIndexType_sql2017() {
        return new MissingIndexType_sql2017();
    }

    /**
     * Create an instance of {@link ScalarSequenceType_sql2017 }
     * 
     */
    public ScalarSequenceType_sql2017 createScalarSequenceType_sql2017() {
        return new ScalarSequenceType_sql2017();
    }

    /**
     * Create an instance of {@link QueryPlanType_sql2017 }
     * 
     */
    public QueryPlanType_sql2017 createQueryPlanType_sql2017() {
        return new QueryPlanType_sql2017();
    }

    /**
     * Create an instance of {@link ScalarExpressionType_sql2017 }
     * 
     */
    public ScalarExpressionType_sql2017 createScalarExpressionType_sql2017() {
        return new ScalarExpressionType_sql2017();
    }

    /**
     * Create an instance of {@link RowsetType_sql2017 }
     * 
     */
    public RowsetType_sql2017 createRowsetType_sql2017() {
        return new RowsetType_sql2017();
    }

    /**
     * Create an instance of {@link PutType_sql2017 }
     * 
     */
    public PutType_sql2017 createPutType_sql2017() {
        return new PutType_sql2017();
    }

    /**
     * Create an instance of {@link UpdateType_sql2017 }
     * 
     */
    public UpdateType_sql2017 createUpdateType_sql2017() {
        return new UpdateType_sql2017();
    }

    /**
     * Create an instance of {@link WaitStatType_sql2017 }
     * 
     */
    public WaitStatType_sql2017 createWaitStatType_sql2017() {
        return new WaitStatType_sql2017();
    }

    /**
     * Create an instance of {@link AffectingConvertWarningType_sql2017 }
     * 
     */
    public AffectingConvertWarningType_sql2017 createAffectingConvertWarningType_sql2017() {
        return new AffectingConvertWarningType_sql2017();
    }

    /**
     * Create an instance of {@link SortType_sql2017 }
     * 
     */
    public SortType_sql2017 createSortType_sql2017() {
        return new SortType_sql2017();
    }

    /**
     * Create an instance of {@link OptimizerStatsUsageType_sql2017 }
     * 
     */
    public OptimizerStatsUsageType_sql2017 createOptimizerStatsUsageType_sql2017() {
        return new OptimizerStatsUsageType_sql2017();
    }

    /**
     * Create an instance of {@link ScanRangeType_sql2017 }
     * 
     */
    public ScanRangeType_sql2017 createScanRangeType_sql2017() {
        return new ScanRangeType_sql2017();
    }

    /**
     * Create an instance of {@link UDAggregateType_sql2017 }
     * 
     */
    public UDAggregateType_sql2017 createUDAggregateType_sql2017() {
        return new UDAggregateType_sql2017();
    }

    /**
     * Create an instance of {@link IndexScanType_sql2017 }
     * 
     */
    public IndexScanType_sql2017 createIndexScanType_sql2017() {
        return new IndexScanType_sql2017();
    }

    /**
     * Create an instance of {@link TopSortType_sql2017 }
     * 
     */
    public TopSortType_sql2017 createTopSortType_sql2017() {
        return new TopSortType_sql2017();
    }

    /**
     * Create an instance of {@link GuessedSelectivityType_sql2017 }
     * 
     */
    public GuessedSelectivityType_sql2017 createGuessedSelectivityType_sql2017() {
        return new GuessedSelectivityType_sql2017();
    }

    /**
     * Create an instance of {@link UDXType_sql2017 }
     * 
     */
    public UDXType_sql2017 createUDXType_sql2017() {
        return new UDXType_sql2017();
    }

    /**
     * Create an instance of {@link ForeignKeyReferenceCheckType_sql2017 }
     * 
     */
    public ForeignKeyReferenceCheckType_sql2017 createForeignKeyReferenceCheckType_sql2017() {
        return new ForeignKeyReferenceCheckType_sql2017();
    }

    /**
     * Create an instance of {@link WarningsType_sql2017 }
     * 
     */
    public WarningsType_sql2017 createWarningsType_sql2017() {
        return new WarningsType_sql2017();
    }

    /**
     * Create an instance of {@link ParallelismType_sql2017 .Activation_sql2017 }
     * 
     */
    public ParallelismType_sql2017 .Activation_sql2017 createParallelismType_sql2017Activation_sql2017() {
        return new ParallelismType_sql2017 .Activation_sql2017();
    }

    /**
     * Create an instance of {@link ParallelismType_sql2017 .BrickRouting_sql2017 }
     * 
     */
    public ParallelismType_sql2017 .BrickRouting_sql2017 createParallelismType_sql2017BrickRouting_sql2017() {
        return new ParallelismType_sql2017 .BrickRouting_sql2017();
    }

    /**
     * Create an instance of {@link ConstantScanType_sql2017 .Values_sql2017 }
     * 
     */
    public ConstantScanType_sql2017 .Values_sql2017 createConstantScanType_sql2017Values_sql2017() {
        return new ConstantScanType_sql2017 .Values_sql2017();
    }

    /**
     * Create an instance of {@link CursorPlanType_sql2017 .Operation_sql2017 }
     * 
     */
    public CursorPlanType_sql2017 .Operation_sql2017 createCursorPlanType_sql2017Operation_sql2017() {
        return new CursorPlanType_sql2017 .Operation_sql2017();
    }

    /**
     * Create an instance of {@link RunTimeInformationType_sql2017 .RunTimeCountersPerThread_sql2017 }
     * 
     */
    public RunTimeInformationType_sql2017 .RunTimeCountersPerThread_sql2017 createRunTimeInformationType_sql2017RunTimeCountersPerThread_sql2017() {
        return new RunTimeInformationType_sql2017 .RunTimeCountersPerThread_sql2017();
    }

    /**
     * Create an instance of {@link ReceivePlanType_sql2017 .Operation_sql2017 }
     * 
     */
    public ReceivePlanType_sql2017 .Operation_sql2017 createReceivePlanType_sql2017Operation_sql2017() {
        return new ReceivePlanType_sql2017 .Operation_sql2017();
    }

    /**
     * Create an instance of {@link DefinedValuesListType_sql2017 .DefinedValue_sql2017 .ValueVector_sql2017 }
     * 
     */
    public DefinedValuesListType_sql2017 .DefinedValue_sql2017 .ValueVector_sql2017 createDefinedValuesListType_sql2017DefinedValue_sql2017ValueVector_sql2017() {
        return new DefinedValuesListType_sql2017 .DefinedValue_sql2017 .ValueVector_sql2017();
    }

    /**
     * Create an instance of {@link RunTimePartitionSummaryType_sql2017 .PartitionsAccessed_sql2017 .PartitionRange_sql2017 }
     * 
     */
    public RunTimePartitionSummaryType_sql2017 .PartitionsAccessed_sql2017 .PartitionRange_sql2017 createRunTimePartitionSummaryType_sql2017PartitionsAccessed_sql2017PartitionRange_sql2017() {
        return new RunTimePartitionSummaryType_sql2017 .PartitionsAccessed_sql2017 .PartitionRange_sql2017();
    }

    /**
     * Create an instance of {@link OrderByType_sql2017 .OrderByColumn_sql2017 }
     * 
     */
    public OrderByType_sql2017 .OrderByColumn_sql2017 createOrderByType_sql2017OrderByColumn_sql2017() {
        return new OrderByType_sql2017 .OrderByColumn_sql2017();
    }

    /**
     * Create an instance of {@link StmtCondType_sql2017 .Condition_sql2017 }
     * 
     */
    public StmtCondType_sql2017 .Condition_sql2017 createStmtCondType_sql2017Condition_sql2017() {
        return new StmtCondType_sql2017 .Condition_sql2017();
    }

    /**
     * Create an instance of {@link StmtCondType_sql2017 .Then_sql2017 }
     * 
     */
    public StmtCondType_sql2017 .Then_sql2017 createStmtCondType_sql2017Then_sql2017() {
        return new StmtCondType_sql2017 .Then_sql2017();
    }

    /**
     * Create an instance of {@link StmtCondType_sql2017 .Else_sql2017 }
     * 
     */
    public StmtCondType_sql2017 .Else_sql2017 createStmtCondType_sql2017Else_sql2017() {
        return new StmtCondType_sql2017 .Else_sql2017();
    }

    /**
     * Create an instance of {@link ShowPlanXML.BatchSequence_sql2017 .Batch_sql2017 }
     * 
     */
    public ShowPlanXML.BatchSequence_sql2017 .Batch_sql2017 createShowPlanXMLBatchSequence_sql2017Batch_sql2017() {
        return new ShowPlanXML.BatchSequence_sql2017 .Batch_sql2017();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ScalarType_sql2017 }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", name = "ScalarOperator", scope = AssignType_sql2017 .class)
    public JAXBElement<ScalarType_sql2017> createAssignType_sql2017ScalarOperator(ScalarType_sql2017 value) {
        return new JAXBElement<ScalarType_sql2017>(_AssignType_sql2017ScalarOperator_QNAME, ScalarType_sql2017 .class, AssignType_sql2017 .class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ColumnReferenceType_sql2017 }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", name = "ColumnReference", scope = AssignType_sql2017 .class)
    public JAXBElement<ColumnReferenceType_sql2017> createAssignType_sql2017ColumnReference(ColumnReferenceType_sql2017 value) {
        return new JAXBElement<ColumnReferenceType_sql2017>(_AssignType_sql2017ColumnReference_QNAME, ColumnReferenceType_sql2017 .class, AssignType_sql2017 .class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DefinedValuesListType_sql2017 .DefinedValue_sql2017 .ValueVector_sql2017 }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", name = "ValueVector", scope = DefinedValuesListType_sql2017 .DefinedValue_sql2017 .class)
    public JAXBElement<DefinedValuesListType_sql2017 .DefinedValue_sql2017 .ValueVector_sql2017> createDefinedValuesListType_sql2017DefinedValue_sql2017ValueVector(DefinedValuesListType_sql2017 .DefinedValue_sql2017 .ValueVector_sql2017 value) {
        return new JAXBElement<DefinedValuesListType_sql2017 .DefinedValue_sql2017 .ValueVector_sql2017>(_DefinedValuesListType_sql2017DefinedValue_sql2017ValueVector_QNAME, DefinedValuesListType_sql2017 .DefinedValue_sql2017 .ValueVector_sql2017 .class, DefinedValuesListType_sql2017 .DefinedValue_sql2017 .class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ScalarType_sql2017 }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", name = "ScalarOperator", scope = DefinedValuesListType_sql2017 .DefinedValue_sql2017 .class)
    public JAXBElement<ScalarType_sql2017> createDefinedValuesListType_sql2017DefinedValue_sql2017ScalarOperator(ScalarType_sql2017 value) {
        return new JAXBElement<ScalarType_sql2017>(_AssignType_sql2017ScalarOperator_QNAME, ScalarType_sql2017 .class, DefinedValuesListType_sql2017 .DefinedValue_sql2017 .class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ColumnReferenceType_sql2017 }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", name = "ColumnReference", scope = DefinedValuesListType_sql2017 .DefinedValue_sql2017 .class)
    public JAXBElement<ColumnReferenceType_sql2017> createDefinedValuesListType_sql2017DefinedValue_sql2017ColumnReference(ColumnReferenceType_sql2017 value) {
        return new JAXBElement<ColumnReferenceType_sql2017>(_AssignType_sql2017ColumnReference_QNAME, ColumnReferenceType_sql2017 .class, DefinedValuesListType_sql2017 .DefinedValue_sql2017 .class, value);
    }

}
