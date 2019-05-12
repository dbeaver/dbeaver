
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2016;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2016 package. 
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

    private final static QName _AssignType_sql2016ScalarOperator_QNAME = new QName("http://schemas.microsoft.com/sqlserver/2004/07/showplan", "ScalarOperator");
    private final static QName _AssignType_sql2016ColumnReference_QNAME = new QName("http://schemas.microsoft.com/sqlserver/2004/07/showplan", "ColumnReference");
    private final static QName _DefinedValuesListType_sql2016DefinedValue_sql2016ValueVector_QNAME = new QName("http://schemas.microsoft.com/sqlserver/2004/07/showplan", "ValueVector");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2016
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
     * Create an instance of {@link ParallelismType_sql2016 }
     * 
     */
    public ParallelismType_sql2016 createParallelismType_sql2016() {
        return new ParallelismType_sql2016();
    }

    /**
     * Create an instance of {@link ConstantScanType_sql2016 }
     * 
     */
    public ConstantScanType_sql2016 createConstantScanType_sql2016() {
        return new ConstantScanType_sql2016();
    }

    /**
     * Create an instance of {@link CursorPlanType_sql2016 }
     * 
     */
    public CursorPlanType_sql2016 createCursorPlanType_sql2016() {
        return new CursorPlanType_sql2016();
    }

    /**
     * Create an instance of {@link RunTimeInformationType_sql2016 }
     * 
     */
    public RunTimeInformationType_sql2016 createRunTimeInformationType_sql2016() {
        return new RunTimeInformationType_sql2016();
    }

    /**
     * Create an instance of {@link ReceivePlanType_sql2016 }
     * 
     */
    public ReceivePlanType_sql2016 createReceivePlanType_sql2016() {
        return new ReceivePlanType_sql2016();
    }

    /**
     * Create an instance of {@link DefinedValuesListType_sql2016 }
     * 
     */
    public DefinedValuesListType_sql2016 createDefinedValuesListType_sql2016() {
        return new DefinedValuesListType_sql2016();
    }

    /**
     * Create an instance of {@link DefinedValuesListType_sql2016 .DefinedValue_sql2016 }
     * 
     */
    public DefinedValuesListType_sql2016 .DefinedValue_sql2016 createDefinedValuesListType_sql2016DefinedValue_sql2016() {
        return new DefinedValuesListType_sql2016 .DefinedValue_sql2016();
    }

    /**
     * Create an instance of {@link RunTimePartitionSummaryType_sql2016 }
     * 
     */
    public RunTimePartitionSummaryType_sql2016 createRunTimePartitionSummaryType_sql2016() {
        return new RunTimePartitionSummaryType_sql2016();
    }

    /**
     * Create an instance of {@link RunTimePartitionSummaryType_sql2016 .PartitionsAccessed_sql2016 }
     * 
     */
    public RunTimePartitionSummaryType_sql2016 .PartitionsAccessed_sql2016 createRunTimePartitionSummaryType_sql2016PartitionsAccessed_sql2016() {
        return new RunTimePartitionSummaryType_sql2016 .PartitionsAccessed_sql2016();
    }

    /**
     * Create an instance of {@link OrderByType_sql2016 }
     * 
     */
    public OrderByType_sql2016 createOrderByType_sql2016() {
        return new OrderByType_sql2016();
    }

    /**
     * Create an instance of {@link StmtCondType_sql2016 }
     * 
     */
    public StmtCondType_sql2016 createStmtCondType_sql2016() {
        return new StmtCondType_sql2016();
    }

    /**
     * Create an instance of {@link ShowPlanXML.BatchSequence_sql2016 }
     * 
     */
    public ShowPlanXML.BatchSequence_sql2016 createShowPlanXMLBatchSequence_sql2016() {
        return new ShowPlanXML.BatchSequence_sql2016();
    }

    /**
     * Create an instance of {@link SeekPredicatePartType_sql2016 }
     * 
     */
    public SeekPredicatePartType_sql2016 createSeekPredicatePartType_sql2016() {
        return new SeekPredicatePartType_sql2016();
    }

    /**
     * Create an instance of {@link UDFType_sql2016 }
     * 
     */
    public UDFType_sql2016 createUDFType_sql2016() {
        return new UDFType_sql2016();
    }

    /**
     * Create an instance of {@link ScalarExpressionListType_sql2016 }
     * 
     */
    public ScalarExpressionListType_sql2016 createScalarExpressionListType_sql2016() {
        return new ScalarExpressionListType_sql2016();
    }

    /**
     * Create an instance of {@link SeekPredicatesType_sql2016 }
     * 
     */
    public SeekPredicatesType_sql2016 createSeekPredicatesType_sql2016() {
        return new SeekPredicatesType_sql2016();
    }

    /**
     * Create an instance of {@link AssignType_sql2016 }
     * 
     */
    public AssignType_sql2016 createAssignType_sql2016() {
        return new AssignType_sql2016();
    }

    /**
     * Create an instance of {@link StmtSimpleType_sql2016 }
     * 
     */
    public StmtSimpleType_sql2016 createStmtSimpleType_sql2016() {
        return new StmtSimpleType_sql2016();
    }

    /**
     * Create an instance of {@link RollupInfoType_sql2016 }
     * 
     */
    public RollupInfoType_sql2016 createRollupInfoType_sql2016() {
        return new RollupInfoType_sql2016();
    }

    /**
     * Create an instance of {@link MergeType_sql2016 }
     * 
     */
    public MergeType_sql2016 createMergeType_sql2016() {
        return new MergeType_sql2016();
    }

    /**
     * Create an instance of {@link TableValuedFunctionType_sql2016 }
     * 
     */
    public TableValuedFunctionType_sql2016 createTableValuedFunctionType_sql2016() {
        return new TableValuedFunctionType_sql2016();
    }

    /**
     * Create an instance of {@link RemoteType_sql2016 }
     * 
     */
    public RemoteType_sql2016 createRemoteType_sql2016() {
        return new RemoteType_sql2016();
    }

    /**
     * Create an instance of {@link CollapseType_sql2016 }
     * 
     */
    public CollapseType_sql2016 createCollapseType_sql2016() {
        return new CollapseType_sql2016();
    }

    /**
     * Create an instance of {@link MultAssignType_sql2016 }
     * 
     */
    public MultAssignType_sql2016 createMultAssignType_sql2016() {
        return new MultAssignType_sql2016();
    }

    /**
     * Create an instance of {@link HashType_sql2016 }
     * 
     */
    public HashType_sql2016 createHashType_sql2016() {
        return new HashType_sql2016();
    }

    /**
     * Create an instance of {@link StmtCursorType_sql2016 }
     * 
     */
    public StmtCursorType_sql2016 createStmtCursorType_sql2016() {
        return new StmtCursorType_sql2016();
    }

    /**
     * Create an instance of {@link ColumnReferenceListType_sql2016 }
     * 
     */
    public ColumnReferenceListType_sql2016 createColumnReferenceListType_sql2016() {
        return new ColumnReferenceListType_sql2016();
    }

    /**
     * Create an instance of {@link ConcatType_sql2016 }
     * 
     */
    public ConcatType_sql2016 createConcatType_sql2016() {
        return new ConcatType_sql2016();
    }

    /**
     * Create an instance of {@link ArithmeticType_sql2016 }
     * 
     */
    public ArithmeticType_sql2016 createArithmeticType_sql2016() {
        return new ArithmeticType_sql2016();
    }

    /**
     * Create an instance of {@link MissingIndexGroupType_sql2016 }
     * 
     */
    public MissingIndexGroupType_sql2016 createMissingIndexGroupType_sql2016() {
        return new MissingIndexGroupType_sql2016();
    }

    /**
     * Create an instance of {@link UDTMethodType_sql2016 }
     * 
     */
    public UDTMethodType_sql2016 createUDTMethodType_sql2016() {
        return new UDTMethodType_sql2016();
    }

    /**
     * Create an instance of {@link SeekPredicateType_sql2016 }
     * 
     */
    public SeekPredicateType_sql2016 createSeekPredicateType_sql2016() {
        return new SeekPredicateType_sql2016();
    }

    /**
     * Create an instance of {@link StmtReceiveType_sql2016 }
     * 
     */
    public StmtReceiveType_sql2016 createStmtReceiveType_sql2016() {
        return new StmtReceiveType_sql2016();
    }

    /**
     * Create an instance of {@link HashSpillDetailsType_sql2016 }
     * 
     */
    public HashSpillDetailsType_sql2016 createHashSpillDetailsType_sql2016() {
        return new HashSpillDetailsType_sql2016();
    }

    /**
     * Create an instance of {@link TopType_sql2016 }
     * 
     */
    public TopType_sql2016 createTopType_sql2016() {
        return new TopType_sql2016();
    }

    /**
     * Create an instance of {@link OptimizerHardwareDependentPropertiesType_sql2016 }
     * 
     */
    public OptimizerHardwareDependentPropertiesType_sql2016 createOptimizerHardwareDependentPropertiesType_sql2016() {
        return new OptimizerHardwareDependentPropertiesType_sql2016();
    }

    /**
     * Create an instance of {@link CompareType_sql2016 }
     * 
     */
    public CompareType_sql2016 createCompareType_sql2016() {
        return new CompareType_sql2016();
    }

    /**
     * Create an instance of {@link StmtBlockType_sql2016 }
     * 
     */
    public StmtBlockType_sql2016 createStmtBlockType_sql2016() {
        return new StmtBlockType_sql2016();
    }

    /**
     * Create an instance of {@link SequenceType_sql2016 }
     * 
     */
    public SequenceType_sql2016 createSequenceType_sql2016() {
        return new SequenceType_sql2016();
    }

    /**
     * Create an instance of {@link MissingIndexesType_sql2016 }
     * 
     */
    public MissingIndexesType_sql2016 createMissingIndexesType_sql2016() {
        return new MissingIndexesType_sql2016();
    }

    /**
     * Create an instance of {@link ObjectType_sql2016 }
     * 
     */
    public ObjectType_sql2016 createObjectType_sql2016() {
        return new ObjectType_sql2016();
    }

    /**
     * Create an instance of {@link IndexedViewInfoType_sql2016 }
     * 
     */
    public IndexedViewInfoType_sql2016 createIndexedViewInfoType_sql2016() {
        return new IndexedViewInfoType_sql2016();
    }

    /**
     * Create an instance of {@link RemoteModifyType_sql2016 }
     * 
     */
    public RemoteModifyType_sql2016 createRemoteModifyType_sql2016() {
        return new RemoteModifyType_sql2016();
    }

    /**
     * Create an instance of {@link SingleColumnReferenceType_sql2016 }
     * 
     */
    public SingleColumnReferenceType_sql2016 createSingleColumnReferenceType_sql2016() {
        return new SingleColumnReferenceType_sql2016();
    }

    /**
     * Create an instance of {@link ConstType_sql2016 }
     * 
     */
    public ConstType_sql2016 createConstType_sql2016() {
        return new ConstType_sql2016();
    }

    /**
     * Create an instance of {@link ConditionalType_sql2016 }
     * 
     */
    public ConditionalType_sql2016 createConditionalType_sql2016() {
        return new ConditionalType_sql2016();
    }

    /**
     * Create an instance of {@link GenericType_sql2016 }
     * 
     */
    public GenericType_sql2016 createGenericType_sql2016() {
        return new GenericType_sql2016();
    }

    /**
     * Create an instance of {@link SetOptionsType_sql2016 }
     * 
     */
    public SetOptionsType_sql2016 createSetOptionsType_sql2016() {
        return new SetOptionsType_sql2016();
    }

    /**
     * Create an instance of {@link FunctionType_sql2016 }
     * 
     */
    public FunctionType_sql2016 createFunctionType_sql2016() {
        return new FunctionType_sql2016();
    }

    /**
     * Create an instance of {@link RelOpBaseType_sql2016 }
     * 
     */
    public RelOpBaseType_sql2016 createRelOpBaseType_sql2016() {
        return new RelOpBaseType_sql2016();
    }

    /**
     * Create an instance of {@link CreateIndexType_sql2016 }
     * 
     */
    public CreateIndexType_sql2016 createCreateIndexType_sql2016() {
        return new CreateIndexType_sql2016();
    }

    /**
     * Create an instance of {@link SegmentType_sql2016 }
     * 
     */
    public SegmentType_sql2016 createSegmentType_sql2016() {
        return new SegmentType_sql2016();
    }

    /**
     * Create an instance of {@link BatchHashTableBuildType_sql2016 }
     * 
     */
    public BatchHashTableBuildType_sql2016 createBatchHashTableBuildType_sql2016() {
        return new BatchHashTableBuildType_sql2016();
    }

    /**
     * Create an instance of {@link SimpleUpdateType_sql2016 }
     * 
     */
    public SimpleUpdateType_sql2016 createSimpleUpdateType_sql2016() {
        return new SimpleUpdateType_sql2016();
    }

    /**
     * Create an instance of {@link StarJoinInfoType_sql2016 }
     * 
     */
    public StarJoinInfoType_sql2016 createStarJoinInfoType_sql2016() {
        return new StarJoinInfoType_sql2016();
    }

    /**
     * Create an instance of {@link ColumnType_sql2016 }
     * 
     */
    public ColumnType_sql2016 createColumnType_sql2016() {
        return new ColumnType_sql2016();
    }

    /**
     * Create an instance of {@link ColumnGroupType_sql2016 }
     * 
     */
    public ColumnGroupType_sql2016 createColumnGroupType_sql2016() {
        return new ColumnGroupType_sql2016();
    }

    /**
     * Create an instance of {@link RemoteRangeType_sql2016 }
     * 
     */
    public RemoteRangeType_sql2016 createRemoteRangeType_sql2016() {
        return new RemoteRangeType_sql2016();
    }

    /**
     * Create an instance of {@link BaseStmtInfoType_sql2016 }
     * 
     */
    public BaseStmtInfoType_sql2016 createBaseStmtInfoType_sql2016() {
        return new BaseStmtInfoType_sql2016();
    }

    /**
     * Create an instance of {@link LogicalType_sql2016 }
     * 
     */
    public LogicalType_sql2016 createLogicalType_sql2016() {
        return new LogicalType_sql2016();
    }

    /**
     * Create an instance of {@link SubqueryType_sql2016 }
     * 
     */
    public SubqueryType_sql2016 createSubqueryType_sql2016() {
        return new SubqueryType_sql2016();
    }

    /**
     * Create an instance of {@link AggregateType_sql2016 }
     * 
     */
    public AggregateType_sql2016 createAggregateType_sql2016() {
        return new AggregateType_sql2016();
    }

    /**
     * Create an instance of {@link WindowAggregateType_sql2016 }
     * 
     */
    public WindowAggregateType_sql2016 createWindowAggregateType_sql2016() {
        return new WindowAggregateType_sql2016();
    }

    /**
     * Create an instance of {@link RelOpType_sql2016 }
     * 
     */
    public RelOpType_sql2016 createRelOpType_sql2016() {
        return new RelOpType_sql2016();
    }

    /**
     * Create an instance of {@link SeekPredicateNewType_sql2016 }
     * 
     */
    public SeekPredicateNewType_sql2016 createSeekPredicateNewType_sql2016() {
        return new SeekPredicateNewType_sql2016();
    }

    /**
     * Create an instance of {@link InternalInfoType_sql2016 }
     * 
     */
    public InternalInfoType_sql2016 createInternalInfoType_sql2016() {
        return new InternalInfoType_sql2016();
    }

    /**
     * Create an instance of {@link MemoryFractionsType_sql2016 }
     * 
     */
    public MemoryFractionsType_sql2016 createMemoryFractionsType_sql2016() {
        return new MemoryFractionsType_sql2016();
    }

    /**
     * Create an instance of {@link ConvertType_sql2016 }
     * 
     */
    public ConvertType_sql2016 createConvertType_sql2016() {
        return new ConvertType_sql2016();
    }

    /**
     * Create an instance of {@link SpillToTempDbType_sql2016 }
     * 
     */
    public SpillToTempDbType_sql2016 createSpillToTempDbType_sql2016() {
        return new SpillToTempDbType_sql2016();
    }

    /**
     * Create an instance of {@link IdentType_sql2016 }
     * 
     */
    public IdentType_sql2016 createIdentType_sql2016() {
        return new IdentType_sql2016();
    }

    /**
     * Create an instance of {@link UnmatchedIndexesType_sql2016 }
     * 
     */
    public UnmatchedIndexesType_sql2016 createUnmatchedIndexesType_sql2016() {
        return new UnmatchedIndexesType_sql2016();
    }

    /**
     * Create an instance of {@link RemoteFetchType_sql2016 }
     * 
     */
    public RemoteFetchType_sql2016 createRemoteFetchType_sql2016() {
        return new RemoteFetchType_sql2016();
    }

    /**
     * Create an instance of {@link CLRFunctionType_sql2016 }
     * 
     */
    public CLRFunctionType_sql2016 createCLRFunctionType_sql2016() {
        return new CLRFunctionType_sql2016();
    }

    /**
     * Create an instance of {@link SortSpillDetailsType_sql2016 }
     * 
     */
    public SortSpillDetailsType_sql2016 createSortSpillDetailsType_sql2016() {
        return new SortSpillDetailsType_sql2016();
    }

    /**
     * Create an instance of {@link RemoteQueryType_sql2016 }
     * 
     */
    public RemoteQueryType_sql2016 createRemoteQueryType_sql2016() {
        return new RemoteQueryType_sql2016();
    }

    /**
     * Create an instance of {@link SwitchType_sql2016 }
     * 
     */
    public SwitchType_sql2016 createSwitchType_sql2016() {
        return new SwitchType_sql2016();
    }

    /**
     * Create an instance of {@link IntrinsicType_sql2016 }
     * 
     */
    public IntrinsicType_sql2016 createIntrinsicType_sql2016() {
        return new IntrinsicType_sql2016();
    }

    /**
     * Create an instance of {@link FilterType_sql2016 }
     * 
     */
    public FilterType_sql2016 createFilterType_sql2016() {
        return new FilterType_sql2016();
    }

    /**
     * Create an instance of {@link ThreadStatType_sql2016 }
     * 
     */
    public ThreadStatType_sql2016 createThreadStatType_sql2016() {
        return new ThreadStatType_sql2016();
    }

    /**
     * Create an instance of {@link WaitWarningType_sql2016 }
     * 
     */
    public WaitWarningType_sql2016 createWaitWarningType_sql2016() {
        return new WaitWarningType_sql2016();
    }

    /**
     * Create an instance of {@link TableScanType_sql2016 }
     * 
     */
    public TableScanType_sql2016 createTableScanType_sql2016() {
        return new TableScanType_sql2016();
    }

    /**
     * Create an instance of {@link ThreadReservationType_sql2016 }
     * 
     */
    public ThreadReservationType_sql2016 createThreadReservationType_sql2016() {
        return new ThreadReservationType_sql2016();
    }

    /**
     * Create an instance of {@link ForeignKeyReferencesCheckType_sql2016 }
     * 
     */
    public ForeignKeyReferencesCheckType_sql2016 createForeignKeyReferencesCheckType_sql2016() {
        return new ForeignKeyReferencesCheckType_sql2016();
    }

    /**
     * Create an instance of {@link ScalarInsertType_sql2016 }
     * 
     */
    public ScalarInsertType_sql2016 createScalarInsertType_sql2016() {
        return new ScalarInsertType_sql2016();
    }

    /**
     * Create an instance of {@link WindowType_sql2016 }
     * 
     */
    public WindowType_sql2016 createWindowType_sql2016() {
        return new WindowType_sql2016();
    }

    /**
     * Create an instance of {@link ColumnReferenceType_sql2016 }
     * 
     */
    public ColumnReferenceType_sql2016 createColumnReferenceType_sql2016() {
        return new ColumnReferenceType_sql2016();
    }

    /**
     * Create an instance of {@link BitmapType_sql2016 }
     * 
     */
    public BitmapType_sql2016 createBitmapType_sql2016() {
        return new BitmapType_sql2016();
    }

    /**
     * Create an instance of {@link SetPredicateElementType_sql2016 }
     * 
     */
    public SetPredicateElementType_sql2016 createSetPredicateElementType_sql2016() {
        return new SetPredicateElementType_sql2016();
    }

    /**
     * Create an instance of {@link SpoolType_sql2016 }
     * 
     */
    public SpoolType_sql2016 createSpoolType_sql2016() {
        return new SpoolType_sql2016();
    }

    /**
     * Create an instance of {@link ScalarType_sql2016 }
     * 
     */
    public ScalarType_sql2016 createScalarType_sql2016() {
        return new ScalarType_sql2016();
    }

    /**
     * Create an instance of {@link StreamAggregateType_sql2016 }
     * 
     */
    public StreamAggregateType_sql2016 createStreamAggregateType_sql2016() {
        return new StreamAggregateType_sql2016();
    }

    /**
     * Create an instance of {@link RollupLevelType_sql2016 }
     * 
     */
    public RollupLevelType_sql2016 createRollupLevelType_sql2016() {
        return new RollupLevelType_sql2016();
    }

    /**
     * Create an instance of {@link MemoryGrantType_sql2016 }
     * 
     */
    public MemoryGrantType_sql2016 createMemoryGrantType_sql2016() {
        return new MemoryGrantType_sql2016();
    }

    /**
     * Create an instance of {@link SimpleIteratorOneChildType_sql2016 }
     * 
     */
    public SimpleIteratorOneChildType_sql2016 createSimpleIteratorOneChildType_sql2016() {
        return new SimpleIteratorOneChildType_sql2016();
    }

    /**
     * Create an instance of {@link ComputeScalarType_sql2016 }
     * 
     */
    public ComputeScalarType_sql2016 createComputeScalarType_sql2016() {
        return new ComputeScalarType_sql2016();
    }

    /**
     * Create an instance of {@link NestedLoopsType_sql2016 }
     * 
     */
    public NestedLoopsType_sql2016 createNestedLoopsType_sql2016() {
        return new NestedLoopsType_sql2016();
    }

    /**
     * Create an instance of {@link ParameterizationType_sql2016 }
     * 
     */
    public ParameterizationType_sql2016 createParameterizationType_sql2016() {
        return new ParameterizationType_sql2016();
    }

    /**
     * Create an instance of {@link SplitType_sql2016 }
     * 
     */
    public SplitType_sql2016 createSplitType_sql2016() {
        return new SplitType_sql2016();
    }

    /**
     * Create an instance of {@link StmtUseDbType_sql2016 }
     * 
     */
    public StmtUseDbType_sql2016 createStmtUseDbType_sql2016() {
        return new StmtUseDbType_sql2016();
    }

    /**
     * Create an instance of {@link MissingIndexType_sql2016 }
     * 
     */
    public MissingIndexType_sql2016 createMissingIndexType_sql2016() {
        return new MissingIndexType_sql2016();
    }

    /**
     * Create an instance of {@link ScalarSequenceType_sql2016 }
     * 
     */
    public ScalarSequenceType_sql2016 createScalarSequenceType_sql2016() {
        return new ScalarSequenceType_sql2016();
    }

    /**
     * Create an instance of {@link QueryPlanType_sql2016 }
     * 
     */
    public QueryPlanType_sql2016 createQueryPlanType_sql2016() {
        return new QueryPlanType_sql2016();
    }

    /**
     * Create an instance of {@link ScalarExpressionType_sql2016 }
     * 
     */
    public ScalarExpressionType_sql2016 createScalarExpressionType_sql2016() {
        return new ScalarExpressionType_sql2016();
    }

    /**
     * Create an instance of {@link RowsetType_sql2016 }
     * 
     */
    public RowsetType_sql2016 createRowsetType_sql2016() {
        return new RowsetType_sql2016();
    }

    /**
     * Create an instance of {@link PutType_sql2016 }
     * 
     */
    public PutType_sql2016 createPutType_sql2016() {
        return new PutType_sql2016();
    }

    /**
     * Create an instance of {@link UpdateType_sql2016 }
     * 
     */
    public UpdateType_sql2016 createUpdateType_sql2016() {
        return new UpdateType_sql2016();
    }

    /**
     * Create an instance of {@link AffectingConvertWarningType_sql2016 }
     * 
     */
    public AffectingConvertWarningType_sql2016 createAffectingConvertWarningType_sql2016() {
        return new AffectingConvertWarningType_sql2016();
    }

    /**
     * Create an instance of {@link SortType_sql2016 }
     * 
     */
    public SortType_sql2016 createSortType_sql2016() {
        return new SortType_sql2016();
    }

    /**
     * Create an instance of {@link ScanRangeType_sql2016 }
     * 
     */
    public ScanRangeType_sql2016 createScanRangeType_sql2016() {
        return new ScanRangeType_sql2016();
    }

    /**
     * Create an instance of {@link UDAggregateType_sql2016 }
     * 
     */
    public UDAggregateType_sql2016 createUDAggregateType_sql2016() {
        return new UDAggregateType_sql2016();
    }

    /**
     * Create an instance of {@link IndexScanType_sql2016 }
     * 
     */
    public IndexScanType_sql2016 createIndexScanType_sql2016() {
        return new IndexScanType_sql2016();
    }

    /**
     * Create an instance of {@link TopSortType_sql2016 }
     * 
     */
    public TopSortType_sql2016 createTopSortType_sql2016() {
        return new TopSortType_sql2016();
    }

    /**
     * Create an instance of {@link GuessedSelectivityType_sql2016 }
     * 
     */
    public GuessedSelectivityType_sql2016 createGuessedSelectivityType_sql2016() {
        return new GuessedSelectivityType_sql2016();
    }

    /**
     * Create an instance of {@link UDXType_sql2016 }
     * 
     */
    public UDXType_sql2016 createUDXType_sql2016() {
        return new UDXType_sql2016();
    }

    /**
     * Create an instance of {@link ForeignKeyReferenceCheckType_sql2016 }
     * 
     */
    public ForeignKeyReferenceCheckType_sql2016 createForeignKeyReferenceCheckType_sql2016() {
        return new ForeignKeyReferenceCheckType_sql2016();
    }

    /**
     * Create an instance of {@link WarningsType_sql2016 }
     * 
     */
    public WarningsType_sql2016 createWarningsType_sql2016() {
        return new WarningsType_sql2016();
    }

    /**
     * Create an instance of {@link ParallelismType_sql2016 .Activation_sql2016 }
     * 
     */
    public ParallelismType_sql2016 .Activation_sql2016 createParallelismType_sql2016Activation_sql2016() {
        return new ParallelismType_sql2016 .Activation_sql2016();
    }

    /**
     * Create an instance of {@link ParallelismType_sql2016 .BrickRouting_sql2016 }
     * 
     */
    public ParallelismType_sql2016 .BrickRouting_sql2016 createParallelismType_sql2016BrickRouting_sql2016() {
        return new ParallelismType_sql2016 .BrickRouting_sql2016();
    }

    /**
     * Create an instance of {@link ConstantScanType_sql2016 .Values_sql2016 }
     * 
     */
    public ConstantScanType_sql2016 .Values_sql2016 createConstantScanType_sql2016Values_sql2016() {
        return new ConstantScanType_sql2016 .Values_sql2016();
    }

    /**
     * Create an instance of {@link CursorPlanType_sql2016 .Operation_sql2016 }
     * 
     */
    public CursorPlanType_sql2016 .Operation_sql2016 createCursorPlanType_sql2016Operation_sql2016() {
        return new CursorPlanType_sql2016 .Operation_sql2016();
    }

    /**
     * Create an instance of {@link RunTimeInformationType_sql2016 .RunTimeCountersPerThread_sql2016 }
     * 
     */
    public RunTimeInformationType_sql2016 .RunTimeCountersPerThread_sql2016 createRunTimeInformationType_sql2016RunTimeCountersPerThread_sql2016() {
        return new RunTimeInformationType_sql2016 .RunTimeCountersPerThread_sql2016();
    }

    /**
     * Create an instance of {@link ReceivePlanType_sql2016 .Operation_sql2016 }
     * 
     */
    public ReceivePlanType_sql2016 .Operation_sql2016 createReceivePlanType_sql2016Operation_sql2016() {
        return new ReceivePlanType_sql2016 .Operation_sql2016();
    }

    /**
     * Create an instance of {@link DefinedValuesListType_sql2016 .DefinedValue_sql2016 .ValueVector_sql2016 }
     * 
     */
    public DefinedValuesListType_sql2016 .DefinedValue_sql2016 .ValueVector_sql2016 createDefinedValuesListType_sql2016DefinedValue_sql2016ValueVector_sql2016() {
        return new DefinedValuesListType_sql2016 .DefinedValue_sql2016 .ValueVector_sql2016();
    }

    /**
     * Create an instance of {@link RunTimePartitionSummaryType_sql2016 .PartitionsAccessed_sql2016 .PartitionRange_sql2016 }
     * 
     */
    public RunTimePartitionSummaryType_sql2016 .PartitionsAccessed_sql2016 .PartitionRange_sql2016 createRunTimePartitionSummaryType_sql2016PartitionsAccessed_sql2016PartitionRange_sql2016() {
        return new RunTimePartitionSummaryType_sql2016 .PartitionsAccessed_sql2016 .PartitionRange_sql2016();
    }

    /**
     * Create an instance of {@link OrderByType_sql2016 .OrderByColumn_sql2016 }
     * 
     */
    public OrderByType_sql2016 .OrderByColumn_sql2016 createOrderByType_sql2016OrderByColumn_sql2016() {
        return new OrderByType_sql2016 .OrderByColumn_sql2016();
    }

    /**
     * Create an instance of {@link StmtCondType_sql2016 .Condition_sql2016 }
     * 
     */
    public StmtCondType_sql2016 .Condition_sql2016 createStmtCondType_sql2016Condition_sql2016() {
        return new StmtCondType_sql2016 .Condition_sql2016();
    }

    /**
     * Create an instance of {@link StmtCondType_sql2016 .Then_sql2016 }
     * 
     */
    public StmtCondType_sql2016 .Then_sql2016 createStmtCondType_sql2016Then_sql2016() {
        return new StmtCondType_sql2016 .Then_sql2016();
    }

    /**
     * Create an instance of {@link StmtCondType_sql2016 .Else_sql2016 }
     * 
     */
    public StmtCondType_sql2016 .Else_sql2016 createStmtCondType_sql2016Else_sql2016() {
        return new StmtCondType_sql2016 .Else_sql2016();
    }

    /**
     * Create an instance of {@link ShowPlanXML.BatchSequence_sql2016 .Batch_sql2016 }
     * 
     */
    public ShowPlanXML.BatchSequence_sql2016 .Batch_sql2016 createShowPlanXMLBatchSequence_sql2016Batch_sql2016() {
        return new ShowPlanXML.BatchSequence_sql2016 .Batch_sql2016();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ScalarType_sql2016 }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", name = "ScalarOperator", scope = AssignType_sql2016 .class)
    public JAXBElement<ScalarType_sql2016> createAssignType_sql2016ScalarOperator(ScalarType_sql2016 value) {
        return new JAXBElement<ScalarType_sql2016>(_AssignType_sql2016ScalarOperator_QNAME, ScalarType_sql2016 .class, AssignType_sql2016 .class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ColumnReferenceType_sql2016 }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", name = "ColumnReference", scope = AssignType_sql2016 .class)
    public JAXBElement<ColumnReferenceType_sql2016> createAssignType_sql2016ColumnReference(ColumnReferenceType_sql2016 value) {
        return new JAXBElement<ColumnReferenceType_sql2016>(_AssignType_sql2016ColumnReference_QNAME, ColumnReferenceType_sql2016 .class, AssignType_sql2016 .class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DefinedValuesListType_sql2016 .DefinedValue_sql2016 .ValueVector_sql2016 }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", name = "ValueVector", scope = DefinedValuesListType_sql2016 .DefinedValue_sql2016 .class)
    public JAXBElement<DefinedValuesListType_sql2016 .DefinedValue_sql2016 .ValueVector_sql2016> createDefinedValuesListType_sql2016DefinedValue_sql2016ValueVector(DefinedValuesListType_sql2016 .DefinedValue_sql2016 .ValueVector_sql2016 value) {
        return new JAXBElement<DefinedValuesListType_sql2016 .DefinedValue_sql2016 .ValueVector_sql2016>(_DefinedValuesListType_sql2016DefinedValue_sql2016ValueVector_QNAME, DefinedValuesListType_sql2016 .DefinedValue_sql2016 .ValueVector_sql2016 .class, DefinedValuesListType_sql2016 .DefinedValue_sql2016 .class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ScalarType_sql2016 }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", name = "ScalarOperator", scope = DefinedValuesListType_sql2016 .DefinedValue_sql2016 .class)
    public JAXBElement<ScalarType_sql2016> createDefinedValuesListType_sql2016DefinedValue_sql2016ScalarOperator(ScalarType_sql2016 value) {
        return new JAXBElement<ScalarType_sql2016>(_AssignType_sql2016ScalarOperator_QNAME, ScalarType_sql2016 .class, DefinedValuesListType_sql2016 .DefinedValue_sql2016 .class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ColumnReferenceType_sql2016 }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", name = "ColumnReference", scope = DefinedValuesListType_sql2016 .DefinedValue_sql2016 .class)
    public JAXBElement<ColumnReferenceType_sql2016> createDefinedValuesListType_sql2016DefinedValue_sql2016ColumnReference(ColumnReferenceType_sql2016 value) {
        return new JAXBElement<ColumnReferenceType_sql2016>(_AssignType_sql2016ColumnReference_QNAME, ColumnReferenceType_sql2016 .class, DefinedValuesListType_sql2016 .DefinedValue_sql2016 .class, value);
    }

}
