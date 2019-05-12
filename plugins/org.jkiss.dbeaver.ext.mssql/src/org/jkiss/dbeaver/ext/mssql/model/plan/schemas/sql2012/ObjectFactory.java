
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2012;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2012 package. 
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

    private final static QName _AssignType_sql2012ScalarOperator_QNAME = new QName("http://schemas.microsoft.com/sqlserver/2004/07/showplan", "ScalarOperator");
    private final static QName _AssignType_sql2012ColumnReference_QNAME = new QName("http://schemas.microsoft.com/sqlserver/2004/07/showplan", "ColumnReference");
    private final static QName _DefinedValuesListType_sql2012DefinedValue_sql2012ValueVector_QNAME = new QName("http://schemas.microsoft.com/sqlserver/2004/07/showplan", "ValueVector");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2012
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
     * Create an instance of {@link ConstantScanType_sql2012 }
     * 
     */
    public ConstantScanType_sql2012 createConstantScanType_sql2012() {
        return new ConstantScanType_sql2012();
    }

    /**
     * Create an instance of {@link CursorPlanType_sql2012 }
     * 
     */
    public CursorPlanType_sql2012 createCursorPlanType_sql2012() {
        return new CursorPlanType_sql2012();
    }

    /**
     * Create an instance of {@link RunTimeInformationType_sql2012 }
     * 
     */
    public RunTimeInformationType_sql2012 createRunTimeInformationType_sql2012() {
        return new RunTimeInformationType_sql2012();
    }

    /**
     * Create an instance of {@link ReceivePlanType_sql2012 }
     * 
     */
    public ReceivePlanType_sql2012 createReceivePlanType_sql2012() {
        return new ReceivePlanType_sql2012();
    }

    /**
     * Create an instance of {@link DefinedValuesListType_sql2012 }
     * 
     */
    public DefinedValuesListType_sql2012 createDefinedValuesListType_sql2012() {
        return new DefinedValuesListType_sql2012();
    }

    /**
     * Create an instance of {@link DefinedValuesListType_sql2012 .DefinedValue_sql2012 }
     * 
     */
    public DefinedValuesListType_sql2012 .DefinedValue_sql2012 createDefinedValuesListType_sql2012DefinedValue_sql2012() {
        return new DefinedValuesListType_sql2012 .DefinedValue_sql2012();
    }

    /**
     * Create an instance of {@link RunTimePartitionSummaryType_sql2012 }
     * 
     */
    public RunTimePartitionSummaryType_sql2012 createRunTimePartitionSummaryType_sql2012() {
        return new RunTimePartitionSummaryType_sql2012();
    }

    /**
     * Create an instance of {@link RunTimePartitionSummaryType_sql2012 .PartitionsAccessed_sql2012 }
     * 
     */
    public RunTimePartitionSummaryType_sql2012 .PartitionsAccessed_sql2012 createRunTimePartitionSummaryType_sql2012PartitionsAccessed_sql2012() {
        return new RunTimePartitionSummaryType_sql2012 .PartitionsAccessed_sql2012();
    }

    /**
     * Create an instance of {@link OrderByType_sql2012 }
     * 
     */
    public OrderByType_sql2012 createOrderByType_sql2012() {
        return new OrderByType_sql2012();
    }

    /**
     * Create an instance of {@link StmtCondType_sql2012 }
     * 
     */
    public StmtCondType_sql2012 createStmtCondType_sql2012() {
        return new StmtCondType_sql2012();
    }

    /**
     * Create an instance of {@link ShowPlanXML.BatchSequence_sql2012 }
     * 
     */
    public ShowPlanXML.BatchSequence_sql2012 createShowPlanXMLBatchSequence_sql2012() {
        return new ShowPlanXML.BatchSequence_sql2012();
    }

    /**
     * Create an instance of {@link UDFType_sql2012 }
     * 
     */
    public UDFType_sql2012 createUDFType_sql2012() {
        return new UDFType_sql2012();
    }

    /**
     * Create an instance of {@link ScalarExpressionListType_sql2012 }
     * 
     */
    public ScalarExpressionListType_sql2012 createScalarExpressionListType_sql2012() {
        return new ScalarExpressionListType_sql2012();
    }

    /**
     * Create an instance of {@link SeekPredicatesType_sql2012 }
     * 
     */
    public SeekPredicatesType_sql2012 createSeekPredicatesType_sql2012() {
        return new SeekPredicatesType_sql2012();
    }

    /**
     * Create an instance of {@link AssignType_sql2012 }
     * 
     */
    public AssignType_sql2012 createAssignType_sql2012() {
        return new AssignType_sql2012();
    }

    /**
     * Create an instance of {@link StmtSimpleType_sql2012 }
     * 
     */
    public StmtSimpleType_sql2012 createStmtSimpleType_sql2012() {
        return new StmtSimpleType_sql2012();
    }

    /**
     * Create an instance of {@link RollupInfoType_sql2012 }
     * 
     */
    public RollupInfoType_sql2012 createRollupInfoType_sql2012() {
        return new RollupInfoType_sql2012();
    }

    /**
     * Create an instance of {@link MergeType_sql2012 }
     * 
     */
    public MergeType_sql2012 createMergeType_sql2012() {
        return new MergeType_sql2012();
    }

    /**
     * Create an instance of {@link TableValuedFunctionType_sql2012 }
     * 
     */
    public TableValuedFunctionType_sql2012 createTableValuedFunctionType_sql2012() {
        return new TableValuedFunctionType_sql2012();
    }

    /**
     * Create an instance of {@link RemoteType_sql2012 }
     * 
     */
    public RemoteType_sql2012 createRemoteType_sql2012() {
        return new RemoteType_sql2012();
    }

    /**
     * Create an instance of {@link CollapseType_sql2012 }
     * 
     */
    public CollapseType_sql2012 createCollapseType_sql2012() {
        return new CollapseType_sql2012();
    }

    /**
     * Create an instance of {@link MultAssignType_sql2012 }
     * 
     */
    public MultAssignType_sql2012 createMultAssignType_sql2012() {
        return new MultAssignType_sql2012();
    }

    /**
     * Create an instance of {@link HashType_sql2012 }
     * 
     */
    public HashType_sql2012 createHashType_sql2012() {
        return new HashType_sql2012();
    }

    /**
     * Create an instance of {@link StmtCursorType_sql2012 }
     * 
     */
    public StmtCursorType_sql2012 createStmtCursorType_sql2012() {
        return new StmtCursorType_sql2012();
    }

    /**
     * Create an instance of {@link ColumnReferenceListType_sql2012 }
     * 
     */
    public ColumnReferenceListType_sql2012 createColumnReferenceListType_sql2012() {
        return new ColumnReferenceListType_sql2012();
    }

    /**
     * Create an instance of {@link ConcatType_sql2012 }
     * 
     */
    public ConcatType_sql2012 createConcatType_sql2012() {
        return new ConcatType_sql2012();
    }

    /**
     * Create an instance of {@link ArithmeticType_sql2012 }
     * 
     */
    public ArithmeticType_sql2012 createArithmeticType_sql2012() {
        return new ArithmeticType_sql2012();
    }

    /**
     * Create an instance of {@link MissingIndexGroupType_sql2012 }
     * 
     */
    public MissingIndexGroupType_sql2012 createMissingIndexGroupType_sql2012() {
        return new MissingIndexGroupType_sql2012();
    }

    /**
     * Create an instance of {@link UDTMethodType_sql2012 }
     * 
     */
    public UDTMethodType_sql2012 createUDTMethodType_sql2012() {
        return new UDTMethodType_sql2012();
    }

    /**
     * Create an instance of {@link SeekPredicateType_sql2012 }
     * 
     */
    public SeekPredicateType_sql2012 createSeekPredicateType_sql2012() {
        return new SeekPredicateType_sql2012();
    }

    /**
     * Create an instance of {@link StmtReceiveType_sql2012 }
     * 
     */
    public StmtReceiveType_sql2012 createStmtReceiveType_sql2012() {
        return new StmtReceiveType_sql2012();
    }

    /**
     * Create an instance of {@link TopType_sql2012 }
     * 
     */
    public TopType_sql2012 createTopType_sql2012() {
        return new TopType_sql2012();
    }

    /**
     * Create an instance of {@link OptimizerHardwareDependentPropertiesType_sql2012 }
     * 
     */
    public OptimizerHardwareDependentPropertiesType_sql2012 createOptimizerHardwareDependentPropertiesType_sql2012() {
        return new OptimizerHardwareDependentPropertiesType_sql2012();
    }

    /**
     * Create an instance of {@link CompareType_sql2012 }
     * 
     */
    public CompareType_sql2012 createCompareType_sql2012() {
        return new CompareType_sql2012();
    }

    /**
     * Create an instance of {@link StmtBlockType_sql2012 }
     * 
     */
    public StmtBlockType_sql2012 createStmtBlockType_sql2012() {
        return new StmtBlockType_sql2012();
    }

    /**
     * Create an instance of {@link SequenceType_sql2012 }
     * 
     */
    public SequenceType_sql2012 createSequenceType_sql2012() {
        return new SequenceType_sql2012();
    }

    /**
     * Create an instance of {@link MissingIndexesType_sql2012 }
     * 
     */
    public MissingIndexesType_sql2012 createMissingIndexesType_sql2012() {
        return new MissingIndexesType_sql2012();
    }

    /**
     * Create an instance of {@link ObjectType_sql2012 }
     * 
     */
    public ObjectType_sql2012 createObjectType_sql2012() {
        return new ObjectType_sql2012();
    }

    /**
     * Create an instance of {@link IndexedViewInfoType_sql2012 }
     * 
     */
    public IndexedViewInfoType_sql2012 createIndexedViewInfoType_sql2012() {
        return new IndexedViewInfoType_sql2012();
    }

    /**
     * Create an instance of {@link RemoteModifyType_sql2012 }
     * 
     */
    public RemoteModifyType_sql2012 createRemoteModifyType_sql2012() {
        return new RemoteModifyType_sql2012();
    }

    /**
     * Create an instance of {@link SingleColumnReferenceType_sql2012 }
     * 
     */
    public SingleColumnReferenceType_sql2012 createSingleColumnReferenceType_sql2012() {
        return new SingleColumnReferenceType_sql2012();
    }

    /**
     * Create an instance of {@link ConstType_sql2012 }
     * 
     */
    public ConstType_sql2012 createConstType_sql2012() {
        return new ConstType_sql2012();
    }

    /**
     * Create an instance of {@link ConditionalType_sql2012 }
     * 
     */
    public ConditionalType_sql2012 createConditionalType_sql2012() {
        return new ConditionalType_sql2012();
    }

    /**
     * Create an instance of {@link GenericType_sql2012 }
     * 
     */
    public GenericType_sql2012 createGenericType_sql2012() {
        return new GenericType_sql2012();
    }

    /**
     * Create an instance of {@link SetOptionsType_sql2012 }
     * 
     */
    public SetOptionsType_sql2012 createSetOptionsType_sql2012() {
        return new SetOptionsType_sql2012();
    }

    /**
     * Create an instance of {@link FunctionType_sql2012 }
     * 
     */
    public FunctionType_sql2012 createFunctionType_sql2012() {
        return new FunctionType_sql2012();
    }

    /**
     * Create an instance of {@link RelOpBaseType_sql2012 }
     * 
     */
    public RelOpBaseType_sql2012 createRelOpBaseType_sql2012() {
        return new RelOpBaseType_sql2012();
    }

    /**
     * Create an instance of {@link CreateIndexType_sql2012 }
     * 
     */
    public CreateIndexType_sql2012 createCreateIndexType_sql2012() {
        return new CreateIndexType_sql2012();
    }

    /**
     * Create an instance of {@link SegmentType_sql2012 }
     * 
     */
    public SegmentType_sql2012 createSegmentType_sql2012() {
        return new SegmentType_sql2012();
    }

    /**
     * Create an instance of {@link BatchHashTableBuildType_sql2012 }
     * 
     */
    public BatchHashTableBuildType_sql2012 createBatchHashTableBuildType_sql2012() {
        return new BatchHashTableBuildType_sql2012();
    }

    /**
     * Create an instance of {@link SimpleUpdateType_sql2012 }
     * 
     */
    public SimpleUpdateType_sql2012 createSimpleUpdateType_sql2012() {
        return new SimpleUpdateType_sql2012();
    }

    /**
     * Create an instance of {@link StarJoinInfoType_sql2012 }
     * 
     */
    public StarJoinInfoType_sql2012 createStarJoinInfoType_sql2012() {
        return new StarJoinInfoType_sql2012();
    }

    /**
     * Create an instance of {@link ColumnType_sql2012 }
     * 
     */
    public ColumnType_sql2012 createColumnType_sql2012() {
        return new ColumnType_sql2012();
    }

    /**
     * Create an instance of {@link ColumnGroupType_sql2012 }
     * 
     */
    public ColumnGroupType_sql2012 createColumnGroupType_sql2012() {
        return new ColumnGroupType_sql2012();
    }

    /**
     * Create an instance of {@link RemoteRangeType_sql2012 }
     * 
     */
    public RemoteRangeType_sql2012 createRemoteRangeType_sql2012() {
        return new RemoteRangeType_sql2012();
    }

    /**
     * Create an instance of {@link BaseStmtInfoType_sql2012 }
     * 
     */
    public BaseStmtInfoType_sql2012 createBaseStmtInfoType_sql2012() {
        return new BaseStmtInfoType_sql2012();
    }

    /**
     * Create an instance of {@link LogicalType_sql2012 }
     * 
     */
    public LogicalType_sql2012 createLogicalType_sql2012() {
        return new LogicalType_sql2012();
    }

    /**
     * Create an instance of {@link SubqueryType_sql2012 }
     * 
     */
    public SubqueryType_sql2012 createSubqueryType_sql2012() {
        return new SubqueryType_sql2012();
    }

    /**
     * Create an instance of {@link AggregateType_sql2012 }
     * 
     */
    public AggregateType_sql2012 createAggregateType_sql2012() {
        return new AggregateType_sql2012();
    }

    /**
     * Create an instance of {@link RelOpType_sql2012 }
     * 
     */
    public RelOpType_sql2012 createRelOpType_sql2012() {
        return new RelOpType_sql2012();
    }

    /**
     * Create an instance of {@link SeekPredicateNewType_sql2012 }
     * 
     */
    public SeekPredicateNewType_sql2012 createSeekPredicateNewType_sql2012() {
        return new SeekPredicateNewType_sql2012();
    }

    /**
     * Create an instance of {@link InternalInfoType_sql2012 }
     * 
     */
    public InternalInfoType_sql2012 createInternalInfoType_sql2012() {
        return new InternalInfoType_sql2012();
    }

    /**
     * Create an instance of {@link MemoryFractionsType_sql2012 }
     * 
     */
    public MemoryFractionsType_sql2012 createMemoryFractionsType_sql2012() {
        return new MemoryFractionsType_sql2012();
    }

    /**
     * Create an instance of {@link ConvertType_sql2012 }
     * 
     */
    public ConvertType_sql2012 createConvertType_sql2012() {
        return new ConvertType_sql2012();
    }

    /**
     * Create an instance of {@link SpillToTempDbType_sql2012 }
     * 
     */
    public SpillToTempDbType_sql2012 createSpillToTempDbType_sql2012() {
        return new SpillToTempDbType_sql2012();
    }

    /**
     * Create an instance of {@link IdentType_sql2012 }
     * 
     */
    public IdentType_sql2012 createIdentType_sql2012() {
        return new IdentType_sql2012();
    }

    /**
     * Create an instance of {@link UnmatchedIndexesType_sql2012 }
     * 
     */
    public UnmatchedIndexesType_sql2012 createUnmatchedIndexesType_sql2012() {
        return new UnmatchedIndexesType_sql2012();
    }

    /**
     * Create an instance of {@link RemoteFetchType_sql2012 }
     * 
     */
    public RemoteFetchType_sql2012 createRemoteFetchType_sql2012() {
        return new RemoteFetchType_sql2012();
    }

    /**
     * Create an instance of {@link CLRFunctionType_sql2012 }
     * 
     */
    public CLRFunctionType_sql2012 createCLRFunctionType_sql2012() {
        return new CLRFunctionType_sql2012();
    }

    /**
     * Create an instance of {@link RemoteQueryType_sql2012 }
     * 
     */
    public RemoteQueryType_sql2012 createRemoteQueryType_sql2012() {
        return new RemoteQueryType_sql2012();
    }

    /**
     * Create an instance of {@link SwitchType_sql2012 }
     * 
     */
    public SwitchType_sql2012 createSwitchType_sql2012() {
        return new SwitchType_sql2012();
    }

    /**
     * Create an instance of {@link IntrinsicType_sql2012 }
     * 
     */
    public IntrinsicType_sql2012 createIntrinsicType_sql2012() {
        return new IntrinsicType_sql2012();
    }

    /**
     * Create an instance of {@link FilterType_sql2012 }
     * 
     */
    public FilterType_sql2012 createFilterType_sql2012() {
        return new FilterType_sql2012();
    }

    /**
     * Create an instance of {@link ThreadStatType_sql2012 }
     * 
     */
    public ThreadStatType_sql2012 createThreadStatType_sql2012() {
        return new ThreadStatType_sql2012();
    }

    /**
     * Create an instance of {@link WaitWarningType_sql2012 }
     * 
     */
    public WaitWarningType_sql2012 createWaitWarningType_sql2012() {
        return new WaitWarningType_sql2012();
    }

    /**
     * Create an instance of {@link TableScanType_sql2012 }
     * 
     */
    public TableScanType_sql2012 createTableScanType_sql2012() {
        return new TableScanType_sql2012();
    }

    /**
     * Create an instance of {@link ThreadReservationType_sql2012 }
     * 
     */
    public ThreadReservationType_sql2012 createThreadReservationType_sql2012() {
        return new ThreadReservationType_sql2012();
    }

    /**
     * Create an instance of {@link ScalarInsertType_sql2012 }
     * 
     */
    public ScalarInsertType_sql2012 createScalarInsertType_sql2012() {
        return new ScalarInsertType_sql2012();
    }

    /**
     * Create an instance of {@link WindowType_sql2012 }
     * 
     */
    public WindowType_sql2012 createWindowType_sql2012() {
        return new WindowType_sql2012();
    }

    /**
     * Create an instance of {@link ColumnReferenceType_sql2012 }
     * 
     */
    public ColumnReferenceType_sql2012 createColumnReferenceType_sql2012() {
        return new ColumnReferenceType_sql2012();
    }

    /**
     * Create an instance of {@link BitmapType_sql2012 }
     * 
     */
    public BitmapType_sql2012 createBitmapType_sql2012() {
        return new BitmapType_sql2012();
    }

    /**
     * Create an instance of {@link SetPredicateElementType_sql2012 }
     * 
     */
    public SetPredicateElementType_sql2012 createSetPredicateElementType_sql2012() {
        return new SetPredicateElementType_sql2012();
    }

    /**
     * Create an instance of {@link SpoolType_sql2012 }
     * 
     */
    public SpoolType_sql2012 createSpoolType_sql2012() {
        return new SpoolType_sql2012();
    }

    /**
     * Create an instance of {@link ScalarType_sql2012 }
     * 
     */
    public ScalarType_sql2012 createScalarType_sql2012() {
        return new ScalarType_sql2012();
    }

    /**
     * Create an instance of {@link StreamAggregateType_sql2012 }
     * 
     */
    public StreamAggregateType_sql2012 createStreamAggregateType_sql2012() {
        return new StreamAggregateType_sql2012();
    }

    /**
     * Create an instance of {@link RollupLevelType_sql2012 }
     * 
     */
    public RollupLevelType_sql2012 createRollupLevelType_sql2012() {
        return new RollupLevelType_sql2012();
    }

    /**
     * Create an instance of {@link MemoryGrantType_sql2012 }
     * 
     */
    public MemoryGrantType_sql2012 createMemoryGrantType_sql2012() {
        return new MemoryGrantType_sql2012();
    }

    /**
     * Create an instance of {@link SimpleIteratorOneChildType_sql2012 }
     * 
     */
    public SimpleIteratorOneChildType_sql2012 createSimpleIteratorOneChildType_sql2012() {
        return new SimpleIteratorOneChildType_sql2012();
    }

    /**
     * Create an instance of {@link ComputeScalarType_sql2012 }
     * 
     */
    public ComputeScalarType_sql2012 createComputeScalarType_sql2012() {
        return new ComputeScalarType_sql2012();
    }

    /**
     * Create an instance of {@link NestedLoopsType_sql2012 }
     * 
     */
    public NestedLoopsType_sql2012 createNestedLoopsType_sql2012() {
        return new NestedLoopsType_sql2012();
    }

    /**
     * Create an instance of {@link ParameterizationType_sql2012 }
     * 
     */
    public ParameterizationType_sql2012 createParameterizationType_sql2012() {
        return new ParameterizationType_sql2012();
    }

    /**
     * Create an instance of {@link SplitType_sql2012 }
     * 
     */
    public SplitType_sql2012 createSplitType_sql2012() {
        return new SplitType_sql2012();
    }

    /**
     * Create an instance of {@link StmtUseDbType_sql2012 }
     * 
     */
    public StmtUseDbType_sql2012 createStmtUseDbType_sql2012() {
        return new StmtUseDbType_sql2012();
    }

    /**
     * Create an instance of {@link ParallelismType_sql2012 }
     * 
     */
    public ParallelismType_sql2012 createParallelismType_sql2012() {
        return new ParallelismType_sql2012();
    }

    /**
     * Create an instance of {@link MissingIndexType_sql2012 }
     * 
     */
    public MissingIndexType_sql2012 createMissingIndexType_sql2012() {
        return new MissingIndexType_sql2012();
    }

    /**
     * Create an instance of {@link ScalarSequenceType_sql2012 }
     * 
     */
    public ScalarSequenceType_sql2012 createScalarSequenceType_sql2012() {
        return new ScalarSequenceType_sql2012();
    }

    /**
     * Create an instance of {@link QueryPlanType_sql2012 }
     * 
     */
    public QueryPlanType_sql2012 createQueryPlanType_sql2012() {
        return new QueryPlanType_sql2012();
    }

    /**
     * Create an instance of {@link ScalarExpressionType_sql2012 }
     * 
     */
    public ScalarExpressionType_sql2012 createScalarExpressionType_sql2012() {
        return new ScalarExpressionType_sql2012();
    }

    /**
     * Create an instance of {@link RowsetType_sql2012 }
     * 
     */
    public RowsetType_sql2012 createRowsetType_sql2012() {
        return new RowsetType_sql2012();
    }

    /**
     * Create an instance of {@link UpdateType_sql2012 }
     * 
     */
    public UpdateType_sql2012 createUpdateType_sql2012() {
        return new UpdateType_sql2012();
    }

    /**
     * Create an instance of {@link AffectingConvertWarningType_sql2012 }
     * 
     */
    public AffectingConvertWarningType_sql2012 createAffectingConvertWarningType_sql2012() {
        return new AffectingConvertWarningType_sql2012();
    }

    /**
     * Create an instance of {@link SortType_sql2012 }
     * 
     */
    public SortType_sql2012 createSortType_sql2012() {
        return new SortType_sql2012();
    }

    /**
     * Create an instance of {@link ScanRangeType_sql2012 }
     * 
     */
    public ScanRangeType_sql2012 createScanRangeType_sql2012() {
        return new ScanRangeType_sql2012();
    }

    /**
     * Create an instance of {@link UDAggregateType_sql2012 }
     * 
     */
    public UDAggregateType_sql2012 createUDAggregateType_sql2012() {
        return new UDAggregateType_sql2012();
    }

    /**
     * Create an instance of {@link IndexScanType_sql2012 }
     * 
     */
    public IndexScanType_sql2012 createIndexScanType_sql2012() {
        return new IndexScanType_sql2012();
    }

    /**
     * Create an instance of {@link TopSortType_sql2012 }
     * 
     */
    public TopSortType_sql2012 createTopSortType_sql2012() {
        return new TopSortType_sql2012();
    }

    /**
     * Create an instance of {@link GuessedSelectivityType_sql2012 }
     * 
     */
    public GuessedSelectivityType_sql2012 createGuessedSelectivityType_sql2012() {
        return new GuessedSelectivityType_sql2012();
    }

    /**
     * Create an instance of {@link UDXType_sql2012 }
     * 
     */
    public UDXType_sql2012 createUDXType_sql2012() {
        return new UDXType_sql2012();
    }

    /**
     * Create an instance of {@link WarningsType_sql2012 }
     * 
     */
    public WarningsType_sql2012 createWarningsType_sql2012() {
        return new WarningsType_sql2012();
    }

    /**
     * Create an instance of {@link ConstantScanType_sql2012 .Values_sql2012 }
     * 
     */
    public ConstantScanType_sql2012 .Values_sql2012 createConstantScanType_sql2012Values_sql2012() {
        return new ConstantScanType_sql2012 .Values_sql2012();
    }

    /**
     * Create an instance of {@link CursorPlanType_sql2012 .Operation_sql2012 }
     * 
     */
    public CursorPlanType_sql2012 .Operation_sql2012 createCursorPlanType_sql2012Operation_sql2012() {
        return new CursorPlanType_sql2012 .Operation_sql2012();
    }

    /**
     * Create an instance of {@link RunTimeInformationType_sql2012 .RunTimeCountersPerThread_sql2012 }
     * 
     */
    public RunTimeInformationType_sql2012 .RunTimeCountersPerThread_sql2012 createRunTimeInformationType_sql2012RunTimeCountersPerThread_sql2012() {
        return new RunTimeInformationType_sql2012 .RunTimeCountersPerThread_sql2012();
    }

    /**
     * Create an instance of {@link ReceivePlanType_sql2012 .Operation_sql2012 }
     * 
     */
    public ReceivePlanType_sql2012 .Operation_sql2012 createReceivePlanType_sql2012Operation_sql2012() {
        return new ReceivePlanType_sql2012 .Operation_sql2012();
    }

    /**
     * Create an instance of {@link DefinedValuesListType_sql2012 .DefinedValue_sql2012 .ValueVector_sql2012 }
     * 
     */
    public DefinedValuesListType_sql2012 .DefinedValue_sql2012 .ValueVector_sql2012 createDefinedValuesListType_sql2012DefinedValue_sql2012ValueVector_sql2012() {
        return new DefinedValuesListType_sql2012 .DefinedValue_sql2012 .ValueVector_sql2012();
    }

    /**
     * Create an instance of {@link RunTimePartitionSummaryType_sql2012 .PartitionsAccessed_sql2012 .PartitionRange_sql2012 }
     * 
     */
    public RunTimePartitionSummaryType_sql2012 .PartitionsAccessed_sql2012 .PartitionRange_sql2012 createRunTimePartitionSummaryType_sql2012PartitionsAccessed_sql2012PartitionRange_sql2012() {
        return new RunTimePartitionSummaryType_sql2012 .PartitionsAccessed_sql2012 .PartitionRange_sql2012();
    }

    /**
     * Create an instance of {@link OrderByType_sql2012 .OrderByColumn_sql2012 }
     * 
     */
    public OrderByType_sql2012 .OrderByColumn_sql2012 createOrderByType_sql2012OrderByColumn_sql2012() {
        return new OrderByType_sql2012 .OrderByColumn_sql2012();
    }

    /**
     * Create an instance of {@link StmtCondType_sql2012 .Condition_sql2012 }
     * 
     */
    public StmtCondType_sql2012 .Condition_sql2012 createStmtCondType_sql2012Condition_sql2012() {
        return new StmtCondType_sql2012 .Condition_sql2012();
    }

    /**
     * Create an instance of {@link StmtCondType_sql2012 .Then_sql2012 }
     * 
     */
    public StmtCondType_sql2012 .Then_sql2012 createStmtCondType_sql2012Then_sql2012() {
        return new StmtCondType_sql2012 .Then_sql2012();
    }

    /**
     * Create an instance of {@link StmtCondType_sql2012 .Else_sql2012 }
     * 
     */
    public StmtCondType_sql2012 .Else_sql2012 createStmtCondType_sql2012Else_sql2012() {
        return new StmtCondType_sql2012 .Else_sql2012();
    }

    /**
     * Create an instance of {@link ShowPlanXML.BatchSequence_sql2012 .Batch_sql2012 }
     * 
     */
    public ShowPlanXML.BatchSequence_sql2012 .Batch_sql2012 createShowPlanXMLBatchSequence_sql2012Batch_sql2012() {
        return new ShowPlanXML.BatchSequence_sql2012 .Batch_sql2012();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ScalarType_sql2012 }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", name = "ScalarOperator", scope = AssignType_sql2012 .class)
    public JAXBElement<ScalarType_sql2012> createAssignType_sql2012ScalarOperator(ScalarType_sql2012 value) {
        return new JAXBElement<ScalarType_sql2012>(_AssignType_sql2012ScalarOperator_QNAME, ScalarType_sql2012 .class, AssignType_sql2012 .class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ColumnReferenceType_sql2012 }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", name = "ColumnReference", scope = AssignType_sql2012 .class)
    public JAXBElement<ColumnReferenceType_sql2012> createAssignType_sql2012ColumnReference(ColumnReferenceType_sql2012 value) {
        return new JAXBElement<ColumnReferenceType_sql2012>(_AssignType_sql2012ColumnReference_QNAME, ColumnReferenceType_sql2012 .class, AssignType_sql2012 .class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DefinedValuesListType_sql2012 .DefinedValue_sql2012 .ValueVector_sql2012 }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", name = "ValueVector", scope = DefinedValuesListType_sql2012 .DefinedValue_sql2012 .class)
    public JAXBElement<DefinedValuesListType_sql2012 .DefinedValue_sql2012 .ValueVector_sql2012> createDefinedValuesListType_sql2012DefinedValue_sql2012ValueVector(DefinedValuesListType_sql2012 .DefinedValue_sql2012 .ValueVector_sql2012 value) {
        return new JAXBElement<DefinedValuesListType_sql2012 .DefinedValue_sql2012 .ValueVector_sql2012>(_DefinedValuesListType_sql2012DefinedValue_sql2012ValueVector_QNAME, DefinedValuesListType_sql2012 .DefinedValue_sql2012 .ValueVector_sql2012 .class, DefinedValuesListType_sql2012 .DefinedValue_sql2012 .class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ScalarType_sql2012 }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", name = "ScalarOperator", scope = DefinedValuesListType_sql2012 .DefinedValue_sql2012 .class)
    public JAXBElement<ScalarType_sql2012> createDefinedValuesListType_sql2012DefinedValue_sql2012ScalarOperator(ScalarType_sql2012 value) {
        return new JAXBElement<ScalarType_sql2012>(_AssignType_sql2012ScalarOperator_QNAME, ScalarType_sql2012 .class, DefinedValuesListType_sql2012 .DefinedValue_sql2012 .class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ColumnReferenceType_sql2012 }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", name = "ColumnReference", scope = DefinedValuesListType_sql2012 .DefinedValue_sql2012 .class)
    public JAXBElement<ColumnReferenceType_sql2012> createDefinedValuesListType_sql2012DefinedValue_sql2012ColumnReference(ColumnReferenceType_sql2012 value) {
        return new JAXBElement<ColumnReferenceType_sql2012>(_AssignType_sql2012ColumnReference_QNAME, ColumnReferenceType_sql2012 .class, DefinedValuesListType_sql2012 .DefinedValue_sql2012 .class, value);
    }

}
