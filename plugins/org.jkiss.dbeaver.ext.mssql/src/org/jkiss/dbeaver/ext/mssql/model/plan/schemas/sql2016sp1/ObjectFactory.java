
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2016sp1;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2016sp1 package. 
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

    private final static QName _DefinedValuesListType_sql2016sp1DefinedValue_sql2016sp1ValueVector_QNAME = new QName("http://schemas.microsoft.com/sqlserver/2004/07/showplan", "ValueVector");
    private final static QName _DefinedValuesListType_sql2016sp1DefinedValue_sql2016sp1ScalarOperator_QNAME = new QName("http://schemas.microsoft.com/sqlserver/2004/07/showplan", "ScalarOperator");
    private final static QName _DefinedValuesListType_sql2016sp1DefinedValue_sql2016sp1ColumnReference_QNAME = new QName("http://schemas.microsoft.com/sqlserver/2004/07/showplan", "ColumnReference");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2016sp1
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
     * Create an instance of {@link ParallelismType_sql2016sp1 }
     * 
     */
    public ParallelismType_sql2016sp1 createParallelismType_sql2016sp1() {
        return new ParallelismType_sql2016sp1();
    }

    /**
     * Create an instance of {@link ConstantScanType_sql2016sp1 }
     * 
     */
    public ConstantScanType_sql2016sp1 createConstantScanType_sql2016sp1() {
        return new ConstantScanType_sql2016sp1();
    }

    /**
     * Create an instance of {@link CursorPlanType_sql2016sp1 }
     * 
     */
    public CursorPlanType_sql2016sp1 createCursorPlanType_sql2016sp1() {
        return new CursorPlanType_sql2016sp1();
    }

    /**
     * Create an instance of {@link RunTimeInformationType_sql2016sp1 }
     * 
     */
    public RunTimeInformationType_sql2016sp1 createRunTimeInformationType_sql2016sp1() {
        return new RunTimeInformationType_sql2016sp1();
    }

    /**
     * Create an instance of {@link ReceivePlanType_sql2016sp1 }
     * 
     */
    public ReceivePlanType_sql2016sp1 createReceivePlanType_sql2016sp1() {
        return new ReceivePlanType_sql2016sp1();
    }

    /**
     * Create an instance of {@link DefinedValuesListType_sql2016sp1 }
     * 
     */
    public DefinedValuesListType_sql2016sp1 createDefinedValuesListType_sql2016sp1() {
        return new DefinedValuesListType_sql2016sp1();
    }

    /**
     * Create an instance of {@link DefinedValuesListType_sql2016sp1 .DefinedValue_sql2016sp1 }
     * 
     */
    public DefinedValuesListType_sql2016sp1 .DefinedValue_sql2016sp1 createDefinedValuesListType_sql2016sp1DefinedValue_sql2016sp1() {
        return new DefinedValuesListType_sql2016sp1 .DefinedValue_sql2016sp1();
    }

    /**
     * Create an instance of {@link RunTimePartitionSummaryType_sql2016sp1 }
     * 
     */
    public RunTimePartitionSummaryType_sql2016sp1 createRunTimePartitionSummaryType_sql2016sp1() {
        return new RunTimePartitionSummaryType_sql2016sp1();
    }

    /**
     * Create an instance of {@link RunTimePartitionSummaryType_sql2016sp1 .PartitionsAccessed_sql2016sp1 }
     * 
     */
    public RunTimePartitionSummaryType_sql2016sp1 .PartitionsAccessed_sql2016sp1 createRunTimePartitionSummaryType_sql2016sp1PartitionsAccessed_sql2016sp1() {
        return new RunTimePartitionSummaryType_sql2016sp1 .PartitionsAccessed_sql2016sp1();
    }

    /**
     * Create an instance of {@link OrderByType_sql2016sp1 }
     * 
     */
    public OrderByType_sql2016sp1 createOrderByType_sql2016sp1() {
        return new OrderByType_sql2016sp1();
    }

    /**
     * Create an instance of {@link StmtCondType_sql2016sp1 }
     * 
     */
    public StmtCondType_sql2016sp1 createStmtCondType_sql2016sp1() {
        return new StmtCondType_sql2016sp1();
    }

    /**
     * Create an instance of {@link ShowPlanXML.BatchSequence_sql2016sp1 }
     * 
     */
    public ShowPlanXML.BatchSequence_sql2016sp1 createShowPlanXMLBatchSequence_sql2016sp1() {
        return new ShowPlanXML.BatchSequence_sql2016sp1();
    }

    /**
     * Create an instance of {@link SeekPredicatePartType_sql2016sp1 }
     * 
     */
    public SeekPredicatePartType_sql2016sp1 createSeekPredicatePartType_sql2016sp1() {
        return new SeekPredicatePartType_sql2016sp1();
    }

    /**
     * Create an instance of {@link UDFType_sql2016sp1 }
     * 
     */
    public UDFType_sql2016sp1 createUDFType_sql2016sp1() {
        return new UDFType_sql2016sp1();
    }

    /**
     * Create an instance of {@link ScalarExpressionListType_sql2016sp1 }
     * 
     */
    public ScalarExpressionListType_sql2016sp1 createScalarExpressionListType_sql2016sp1() {
        return new ScalarExpressionListType_sql2016sp1();
    }

    /**
     * Create an instance of {@link SeekPredicatesType_sql2016sp1 }
     * 
     */
    public SeekPredicatesType_sql2016sp1 createSeekPredicatesType_sql2016sp1() {
        return new SeekPredicatesType_sql2016sp1();
    }

    /**
     * Create an instance of {@link QueryExecTimeType_sql2016sp1 }
     * 
     */
    public QueryExecTimeType_sql2016sp1 createQueryExecTimeType_sql2016sp1() {
        return new QueryExecTimeType_sql2016sp1();
    }

    /**
     * Create an instance of {@link AssignType_sql2016sp1 }
     * 
     */
    public AssignType_sql2016sp1 createAssignType_sql2016sp1() {
        return new AssignType_sql2016sp1();
    }

    /**
     * Create an instance of {@link StmtSimpleType_sql2016sp1 }
     * 
     */
    public StmtSimpleType_sql2016sp1 createStmtSimpleType_sql2016sp1() {
        return new StmtSimpleType_sql2016sp1();
    }

    /**
     * Create an instance of {@link RollupInfoType_sql2016sp1 }
     * 
     */
    public RollupInfoType_sql2016sp1 createRollupInfoType_sql2016sp1() {
        return new RollupInfoType_sql2016sp1();
    }

    /**
     * Create an instance of {@link MergeType_sql2016sp1 }
     * 
     */
    public MergeType_sql2016sp1 createMergeType_sql2016sp1() {
        return new MergeType_sql2016sp1();
    }

    /**
     * Create an instance of {@link TableValuedFunctionType_sql2016sp1 }
     * 
     */
    public TableValuedFunctionType_sql2016sp1 createTableValuedFunctionType_sql2016sp1() {
        return new TableValuedFunctionType_sql2016sp1();
    }

    /**
     * Create an instance of {@link RemoteType_sql2016sp1 }
     * 
     */
    public RemoteType_sql2016sp1 createRemoteType_sql2016sp1() {
        return new RemoteType_sql2016sp1();
    }

    /**
     * Create an instance of {@link CollapseType_sql2016sp1 }
     * 
     */
    public CollapseType_sql2016sp1 createCollapseType_sql2016sp1() {
        return new CollapseType_sql2016sp1();
    }

    /**
     * Create an instance of {@link MultAssignType_sql2016sp1 }
     * 
     */
    public MultAssignType_sql2016sp1 createMultAssignType_sql2016sp1() {
        return new MultAssignType_sql2016sp1();
    }

    /**
     * Create an instance of {@link HashType_sql2016sp1 }
     * 
     */
    public HashType_sql2016sp1 createHashType_sql2016sp1() {
        return new HashType_sql2016sp1();
    }

    /**
     * Create an instance of {@link StmtCursorType_sql2016sp1 }
     * 
     */
    public StmtCursorType_sql2016sp1 createStmtCursorType_sql2016sp1() {
        return new StmtCursorType_sql2016sp1();
    }

    /**
     * Create an instance of {@link ColumnReferenceListType_sql2016sp1 }
     * 
     */
    public ColumnReferenceListType_sql2016sp1 createColumnReferenceListType_sql2016sp1() {
        return new ColumnReferenceListType_sql2016sp1();
    }

    /**
     * Create an instance of {@link ConcatType_sql2016sp1 }
     * 
     */
    public ConcatType_sql2016sp1 createConcatType_sql2016sp1() {
        return new ConcatType_sql2016sp1();
    }

    /**
     * Create an instance of {@link ArithmeticType_sql2016sp1 }
     * 
     */
    public ArithmeticType_sql2016sp1 createArithmeticType_sql2016sp1() {
        return new ArithmeticType_sql2016sp1();
    }

    /**
     * Create an instance of {@link MissingIndexGroupType_sql2016sp1 }
     * 
     */
    public MissingIndexGroupType_sql2016sp1 createMissingIndexGroupType_sql2016sp1() {
        return new MissingIndexGroupType_sql2016sp1();
    }

    /**
     * Create an instance of {@link UDTMethodType_sql2016sp1 }
     * 
     */
    public UDTMethodType_sql2016sp1 createUDTMethodType_sql2016sp1() {
        return new UDTMethodType_sql2016sp1();
    }

    /**
     * Create an instance of {@link SeekPredicateType_sql2016sp1 }
     * 
     */
    public SeekPredicateType_sql2016sp1 createSeekPredicateType_sql2016sp1() {
        return new SeekPredicateType_sql2016sp1();
    }

    /**
     * Create an instance of {@link TraceFlagType_sql2016sp1 }
     * 
     */
    public TraceFlagType_sql2016sp1 createTraceFlagType_sql2016sp1() {
        return new TraceFlagType_sql2016sp1();
    }

    /**
     * Create an instance of {@link StmtReceiveType_sql2016sp1 }
     * 
     */
    public StmtReceiveType_sql2016sp1 createStmtReceiveType_sql2016sp1() {
        return new StmtReceiveType_sql2016sp1();
    }

    /**
     * Create an instance of {@link HashSpillDetailsType_sql2016sp1 }
     * 
     */
    public HashSpillDetailsType_sql2016sp1 createHashSpillDetailsType_sql2016sp1() {
        return new HashSpillDetailsType_sql2016sp1();
    }

    /**
     * Create an instance of {@link TopType_sql2016sp1 }
     * 
     */
    public TopType_sql2016sp1 createTopType_sql2016sp1() {
        return new TopType_sql2016sp1();
    }

    /**
     * Create an instance of {@link OptimizerHardwareDependentPropertiesType_sql2016sp1 }
     * 
     */
    public OptimizerHardwareDependentPropertiesType_sql2016sp1 createOptimizerHardwareDependentPropertiesType_sql2016sp1() {
        return new OptimizerHardwareDependentPropertiesType_sql2016sp1();
    }

    /**
     * Create an instance of {@link CompareType_sql2016sp1 }
     * 
     */
    public CompareType_sql2016sp1 createCompareType_sql2016sp1() {
        return new CompareType_sql2016sp1();
    }

    /**
     * Create an instance of {@link StmtBlockType_sql2016sp1 }
     * 
     */
    public StmtBlockType_sql2016sp1 createStmtBlockType_sql2016sp1() {
        return new StmtBlockType_sql2016sp1();
    }

    /**
     * Create an instance of {@link SequenceType_sql2016sp1 }
     * 
     */
    public SequenceType_sql2016sp1 createSequenceType_sql2016sp1() {
        return new SequenceType_sql2016sp1();
    }

    /**
     * Create an instance of {@link MissingIndexesType_sql2016sp1 }
     * 
     */
    public MissingIndexesType_sql2016sp1 createMissingIndexesType_sql2016sp1() {
        return new MissingIndexesType_sql2016sp1();
    }

    /**
     * Create an instance of {@link ObjectType_sql2016sp1 }
     * 
     */
    public ObjectType_sql2016sp1 createObjectType_sql2016sp1() {
        return new ObjectType_sql2016sp1();
    }

    /**
     * Create an instance of {@link IndexedViewInfoType_sql2016sp1 }
     * 
     */
    public IndexedViewInfoType_sql2016sp1 createIndexedViewInfoType_sql2016sp1() {
        return new IndexedViewInfoType_sql2016sp1();
    }

    /**
     * Create an instance of {@link RemoteModifyType_sql2016sp1 }
     * 
     */
    public RemoteModifyType_sql2016sp1 createRemoteModifyType_sql2016sp1() {
        return new RemoteModifyType_sql2016sp1();
    }

    /**
     * Create an instance of {@link SingleColumnReferenceType_sql2016sp1 }
     * 
     */
    public SingleColumnReferenceType_sql2016sp1 createSingleColumnReferenceType_sql2016sp1() {
        return new SingleColumnReferenceType_sql2016sp1();
    }

    /**
     * Create an instance of {@link ConstType_sql2016sp1 }
     * 
     */
    public ConstType_sql2016sp1 createConstType_sql2016sp1() {
        return new ConstType_sql2016sp1();
    }

    /**
     * Create an instance of {@link ConditionalType_sql2016sp1 }
     * 
     */
    public ConditionalType_sql2016sp1 createConditionalType_sql2016sp1() {
        return new ConditionalType_sql2016sp1();
    }

    /**
     * Create an instance of {@link GenericType_sql2016sp1 }
     * 
     */
    public GenericType_sql2016sp1 createGenericType_sql2016sp1() {
        return new GenericType_sql2016sp1();
    }

    /**
     * Create an instance of {@link SetOptionsType_sql2016sp1 }
     * 
     */
    public SetOptionsType_sql2016sp1 createSetOptionsType_sql2016sp1() {
        return new SetOptionsType_sql2016sp1();
    }

    /**
     * Create an instance of {@link FunctionType_sql2016sp1 }
     * 
     */
    public FunctionType_sql2016sp1 createFunctionType_sql2016sp1() {
        return new FunctionType_sql2016sp1();
    }

    /**
     * Create an instance of {@link RelOpBaseType_sql2016sp1 }
     * 
     */
    public RelOpBaseType_sql2016sp1 createRelOpBaseType_sql2016sp1() {
        return new RelOpBaseType_sql2016sp1();
    }

    /**
     * Create an instance of {@link CreateIndexType_sql2016sp1 }
     * 
     */
    public CreateIndexType_sql2016sp1 createCreateIndexType_sql2016sp1() {
        return new CreateIndexType_sql2016sp1();
    }

    /**
     * Create an instance of {@link SegmentType_sql2016sp1 }
     * 
     */
    public SegmentType_sql2016sp1 createSegmentType_sql2016sp1() {
        return new SegmentType_sql2016sp1();
    }

    /**
     * Create an instance of {@link BatchHashTableBuildType_sql2016sp1 }
     * 
     */
    public BatchHashTableBuildType_sql2016sp1 createBatchHashTableBuildType_sql2016sp1() {
        return new BatchHashTableBuildType_sql2016sp1();
    }

    /**
     * Create an instance of {@link SimpleUpdateType_sql2016sp1 }
     * 
     */
    public SimpleUpdateType_sql2016sp1 createSimpleUpdateType_sql2016sp1() {
        return new SimpleUpdateType_sql2016sp1();
    }

    /**
     * Create an instance of {@link StarJoinInfoType_sql2016sp1 }
     * 
     */
    public StarJoinInfoType_sql2016sp1 createStarJoinInfoType_sql2016sp1() {
        return new StarJoinInfoType_sql2016sp1();
    }

    /**
     * Create an instance of {@link ColumnType_sql2016sp1 }
     * 
     */
    public ColumnType_sql2016sp1 createColumnType_sql2016sp1() {
        return new ColumnType_sql2016sp1();
    }

    /**
     * Create an instance of {@link ColumnGroupType_sql2016sp1 }
     * 
     */
    public ColumnGroupType_sql2016sp1 createColumnGroupType_sql2016sp1() {
        return new ColumnGroupType_sql2016sp1();
    }

    /**
     * Create an instance of {@link RemoteRangeType_sql2016sp1 }
     * 
     */
    public RemoteRangeType_sql2016sp1 createRemoteRangeType_sql2016sp1() {
        return new RemoteRangeType_sql2016sp1();
    }

    /**
     * Create an instance of {@link BaseStmtInfoType_sql2016sp1 }
     * 
     */
    public BaseStmtInfoType_sql2016sp1 createBaseStmtInfoType_sql2016sp1() {
        return new BaseStmtInfoType_sql2016sp1();
    }

    /**
     * Create an instance of {@link LogicalType_sql2016sp1 }
     * 
     */
    public LogicalType_sql2016sp1 createLogicalType_sql2016sp1() {
        return new LogicalType_sql2016sp1();
    }

    /**
     * Create an instance of {@link WaitStatListType_sql2016sp1 }
     * 
     */
    public WaitStatListType_sql2016sp1 createWaitStatListType_sql2016sp1() {
        return new WaitStatListType_sql2016sp1();
    }

    /**
     * Create an instance of {@link SubqueryType_sql2016sp1 }
     * 
     */
    public SubqueryType_sql2016sp1 createSubqueryType_sql2016sp1() {
        return new SubqueryType_sql2016sp1();
    }

    /**
     * Create an instance of {@link AggregateType_sql2016sp1 }
     * 
     */
    public AggregateType_sql2016sp1 createAggregateType_sql2016sp1() {
        return new AggregateType_sql2016sp1();
    }

    /**
     * Create an instance of {@link TraceFlagListType_sql2016sp1 }
     * 
     */
    public TraceFlagListType_sql2016sp1 createTraceFlagListType_sql2016sp1() {
        return new TraceFlagListType_sql2016sp1();
    }

    /**
     * Create an instance of {@link WindowAggregateType_sql2016sp1 }
     * 
     */
    public WindowAggregateType_sql2016sp1 createWindowAggregateType_sql2016sp1() {
        return new WindowAggregateType_sql2016sp1();
    }

    /**
     * Create an instance of {@link RelOpType_sql2016sp1 }
     * 
     */
    public RelOpType_sql2016sp1 createRelOpType_sql2016sp1() {
        return new RelOpType_sql2016sp1();
    }

    /**
     * Create an instance of {@link SeekPredicateNewType_sql2016sp1 }
     * 
     */
    public SeekPredicateNewType_sql2016sp1 createSeekPredicateNewType_sql2016sp1() {
        return new SeekPredicateNewType_sql2016sp1();
    }

    /**
     * Create an instance of {@link InternalInfoType_sql2016sp1 }
     * 
     */
    public InternalInfoType_sql2016sp1 createInternalInfoType_sql2016sp1() {
        return new InternalInfoType_sql2016sp1();
    }

    /**
     * Create an instance of {@link MemoryFractionsType_sql2016sp1 }
     * 
     */
    public MemoryFractionsType_sql2016sp1 createMemoryFractionsType_sql2016sp1() {
        return new MemoryFractionsType_sql2016sp1();
    }

    /**
     * Create an instance of {@link ConvertType_sql2016sp1 }
     * 
     */
    public ConvertType_sql2016sp1 createConvertType_sql2016sp1() {
        return new ConvertType_sql2016sp1();
    }

    /**
     * Create an instance of {@link SpillToTempDbType_sql2016sp1 }
     * 
     */
    public SpillToTempDbType_sql2016sp1 createSpillToTempDbType_sql2016sp1() {
        return new SpillToTempDbType_sql2016sp1();
    }

    /**
     * Create an instance of {@link IdentType_sql2016sp1 }
     * 
     */
    public IdentType_sql2016sp1 createIdentType_sql2016sp1() {
        return new IdentType_sql2016sp1();
    }

    /**
     * Create an instance of {@link UnmatchedIndexesType_sql2016sp1 }
     * 
     */
    public UnmatchedIndexesType_sql2016sp1 createUnmatchedIndexesType_sql2016sp1() {
        return new UnmatchedIndexesType_sql2016sp1();
    }

    /**
     * Create an instance of {@link RemoteFetchType_sql2016sp1 }
     * 
     */
    public RemoteFetchType_sql2016sp1 createRemoteFetchType_sql2016sp1() {
        return new RemoteFetchType_sql2016sp1();
    }

    /**
     * Create an instance of {@link CLRFunctionType_sql2016sp1 }
     * 
     */
    public CLRFunctionType_sql2016sp1 createCLRFunctionType_sql2016sp1() {
        return new CLRFunctionType_sql2016sp1();
    }

    /**
     * Create an instance of {@link SortSpillDetailsType_sql2016sp1 }
     * 
     */
    public SortSpillDetailsType_sql2016sp1 createSortSpillDetailsType_sql2016sp1() {
        return new SortSpillDetailsType_sql2016sp1();
    }

    /**
     * Create an instance of {@link RemoteQueryType_sql2016sp1 }
     * 
     */
    public RemoteQueryType_sql2016sp1 createRemoteQueryType_sql2016sp1() {
        return new RemoteQueryType_sql2016sp1();
    }

    /**
     * Create an instance of {@link SwitchType_sql2016sp1 }
     * 
     */
    public SwitchType_sql2016sp1 createSwitchType_sql2016sp1() {
        return new SwitchType_sql2016sp1();
    }

    /**
     * Create an instance of {@link IntrinsicType_sql2016sp1 }
     * 
     */
    public IntrinsicType_sql2016sp1 createIntrinsicType_sql2016sp1() {
        return new IntrinsicType_sql2016sp1();
    }

    /**
     * Create an instance of {@link FilterType_sql2016sp1 }
     * 
     */
    public FilterType_sql2016sp1 createFilterType_sql2016sp1() {
        return new FilterType_sql2016sp1();
    }

    /**
     * Create an instance of {@link ThreadStatType_sql2016sp1 }
     * 
     */
    public ThreadStatType_sql2016sp1 createThreadStatType_sql2016sp1() {
        return new ThreadStatType_sql2016sp1();
    }

    /**
     * Create an instance of {@link WaitWarningType_sql2016sp1 }
     * 
     */
    public WaitWarningType_sql2016sp1 createWaitWarningType_sql2016sp1() {
        return new WaitWarningType_sql2016sp1();
    }

    /**
     * Create an instance of {@link TableScanType_sql2016sp1 }
     * 
     */
    public TableScanType_sql2016sp1 createTableScanType_sql2016sp1() {
        return new TableScanType_sql2016sp1();
    }

    /**
     * Create an instance of {@link ThreadReservationType_sql2016sp1 }
     * 
     */
    public ThreadReservationType_sql2016sp1 createThreadReservationType_sql2016sp1() {
        return new ThreadReservationType_sql2016sp1();
    }

    /**
     * Create an instance of {@link ForeignKeyReferencesCheckType_sql2016sp1 }
     * 
     */
    public ForeignKeyReferencesCheckType_sql2016sp1 createForeignKeyReferencesCheckType_sql2016sp1() {
        return new ForeignKeyReferencesCheckType_sql2016sp1();
    }

    /**
     * Create an instance of {@link ScalarInsertType_sql2016sp1 }
     * 
     */
    public ScalarInsertType_sql2016sp1 createScalarInsertType_sql2016sp1() {
        return new ScalarInsertType_sql2016sp1();
    }

    /**
     * Create an instance of {@link MemoryGrantWarningInfo_sql2016sp1 }
     * 
     */
    public MemoryGrantWarningInfo_sql2016sp1 createMemoryGrantWarningInfo_sql2016sp1() {
        return new MemoryGrantWarningInfo_sql2016sp1();
    }

    /**
     * Create an instance of {@link WindowType_sql2016sp1 }
     * 
     */
    public WindowType_sql2016sp1 createWindowType_sql2016sp1() {
        return new WindowType_sql2016sp1();
    }

    /**
     * Create an instance of {@link ColumnReferenceType_sql2016sp1 }
     * 
     */
    public ColumnReferenceType_sql2016sp1 createColumnReferenceType_sql2016sp1() {
        return new ColumnReferenceType_sql2016sp1();
    }

    /**
     * Create an instance of {@link BitmapType_sql2016sp1 }
     * 
     */
    public BitmapType_sql2016sp1 createBitmapType_sql2016sp1() {
        return new BitmapType_sql2016sp1();
    }

    /**
     * Create an instance of {@link SetPredicateElementType_sql2016sp1 }
     * 
     */
    public SetPredicateElementType_sql2016sp1 createSetPredicateElementType_sql2016sp1() {
        return new SetPredicateElementType_sql2016sp1();
    }

    /**
     * Create an instance of {@link SpoolType_sql2016sp1 }
     * 
     */
    public SpoolType_sql2016sp1 createSpoolType_sql2016sp1() {
        return new SpoolType_sql2016sp1();
    }

    /**
     * Create an instance of {@link ScalarType_sql2016sp1 }
     * 
     */
    public ScalarType_sql2016sp1 createScalarType_sql2016sp1() {
        return new ScalarType_sql2016sp1();
    }

    /**
     * Create an instance of {@link StreamAggregateType_sql2016sp1 }
     * 
     */
    public StreamAggregateType_sql2016sp1 createStreamAggregateType_sql2016sp1() {
        return new StreamAggregateType_sql2016sp1();
    }

    /**
     * Create an instance of {@link RollupLevelType_sql2016sp1 }
     * 
     */
    public RollupLevelType_sql2016sp1 createRollupLevelType_sql2016sp1() {
        return new RollupLevelType_sql2016sp1();
    }

    /**
     * Create an instance of {@link MemoryGrantType_sql2016sp1 }
     * 
     */
    public MemoryGrantType_sql2016sp1 createMemoryGrantType_sql2016sp1() {
        return new MemoryGrantType_sql2016sp1();
    }

    /**
     * Create an instance of {@link SimpleIteratorOneChildType_sql2016sp1 }
     * 
     */
    public SimpleIteratorOneChildType_sql2016sp1 createSimpleIteratorOneChildType_sql2016sp1() {
        return new SimpleIteratorOneChildType_sql2016sp1();
    }

    /**
     * Create an instance of {@link ComputeScalarType_sql2016sp1 }
     * 
     */
    public ComputeScalarType_sql2016sp1 createComputeScalarType_sql2016sp1() {
        return new ComputeScalarType_sql2016sp1();
    }

    /**
     * Create an instance of {@link NestedLoopsType_sql2016sp1 }
     * 
     */
    public NestedLoopsType_sql2016sp1 createNestedLoopsType_sql2016sp1() {
        return new NestedLoopsType_sql2016sp1();
    }

    /**
     * Create an instance of {@link ParameterizationType_sql2016sp1 }
     * 
     */
    public ParameterizationType_sql2016sp1 createParameterizationType_sql2016sp1() {
        return new ParameterizationType_sql2016sp1();
    }

    /**
     * Create an instance of {@link SplitType_sql2016sp1 }
     * 
     */
    public SplitType_sql2016sp1 createSplitType_sql2016sp1() {
        return new SplitType_sql2016sp1();
    }

    /**
     * Create an instance of {@link StmtUseDbType_sql2016sp1 }
     * 
     */
    public StmtUseDbType_sql2016sp1 createStmtUseDbType_sql2016sp1() {
        return new StmtUseDbType_sql2016sp1();
    }

    /**
     * Create an instance of {@link MissingIndexType_sql2016sp1 }
     * 
     */
    public MissingIndexType_sql2016sp1 createMissingIndexType_sql2016sp1() {
        return new MissingIndexType_sql2016sp1();
    }

    /**
     * Create an instance of {@link ScalarSequenceType_sql2016sp1 }
     * 
     */
    public ScalarSequenceType_sql2016sp1 createScalarSequenceType_sql2016sp1() {
        return new ScalarSequenceType_sql2016sp1();
    }

    /**
     * Create an instance of {@link QueryPlanType_sql2016sp1 }
     * 
     */
    public QueryPlanType_sql2016sp1 createQueryPlanType_sql2016sp1() {
        return new QueryPlanType_sql2016sp1();
    }

    /**
     * Create an instance of {@link ScalarExpressionType_sql2016sp1 }
     * 
     */
    public ScalarExpressionType_sql2016sp1 createScalarExpressionType_sql2016sp1() {
        return new ScalarExpressionType_sql2016sp1();
    }

    /**
     * Create an instance of {@link RowsetType_sql2016sp1 }
     * 
     */
    public RowsetType_sql2016sp1 createRowsetType_sql2016sp1() {
        return new RowsetType_sql2016sp1();
    }

    /**
     * Create an instance of {@link PutType_sql2016sp1 }
     * 
     */
    public PutType_sql2016sp1 createPutType_sql2016sp1() {
        return new PutType_sql2016sp1();
    }

    /**
     * Create an instance of {@link UpdateType_sql2016sp1 }
     * 
     */
    public UpdateType_sql2016sp1 createUpdateType_sql2016sp1() {
        return new UpdateType_sql2016sp1();
    }

    /**
     * Create an instance of {@link WaitStatType_sql2016sp1 }
     * 
     */
    public WaitStatType_sql2016sp1 createWaitStatType_sql2016sp1() {
        return new WaitStatType_sql2016sp1();
    }

    /**
     * Create an instance of {@link AffectingConvertWarningType_sql2016sp1 }
     * 
     */
    public AffectingConvertWarningType_sql2016sp1 createAffectingConvertWarningType_sql2016sp1() {
        return new AffectingConvertWarningType_sql2016sp1();
    }

    /**
     * Create an instance of {@link SortType_sql2016sp1 }
     * 
     */
    public SortType_sql2016sp1 createSortType_sql2016sp1() {
        return new SortType_sql2016sp1();
    }

    /**
     * Create an instance of {@link ScanRangeType_sql2016sp1 }
     * 
     */
    public ScanRangeType_sql2016sp1 createScanRangeType_sql2016sp1() {
        return new ScanRangeType_sql2016sp1();
    }

    /**
     * Create an instance of {@link UDAggregateType_sql2016sp1 }
     * 
     */
    public UDAggregateType_sql2016sp1 createUDAggregateType_sql2016sp1() {
        return new UDAggregateType_sql2016sp1();
    }

    /**
     * Create an instance of {@link IndexScanType_sql2016sp1 }
     * 
     */
    public IndexScanType_sql2016sp1 createIndexScanType_sql2016sp1() {
        return new IndexScanType_sql2016sp1();
    }

    /**
     * Create an instance of {@link TopSortType_sql2016sp1 }
     * 
     */
    public TopSortType_sql2016sp1 createTopSortType_sql2016sp1() {
        return new TopSortType_sql2016sp1();
    }

    /**
     * Create an instance of {@link GuessedSelectivityType_sql2016sp1 }
     * 
     */
    public GuessedSelectivityType_sql2016sp1 createGuessedSelectivityType_sql2016sp1() {
        return new GuessedSelectivityType_sql2016sp1();
    }

    /**
     * Create an instance of {@link UDXType_sql2016sp1 }
     * 
     */
    public UDXType_sql2016sp1 createUDXType_sql2016sp1() {
        return new UDXType_sql2016sp1();
    }

    /**
     * Create an instance of {@link ForeignKeyReferenceCheckType_sql2016sp1 }
     * 
     */
    public ForeignKeyReferenceCheckType_sql2016sp1 createForeignKeyReferenceCheckType_sql2016sp1() {
        return new ForeignKeyReferenceCheckType_sql2016sp1();
    }

    /**
     * Create an instance of {@link WarningsType_sql2016sp1 }
     * 
     */
    public WarningsType_sql2016sp1 createWarningsType_sql2016sp1() {
        return new WarningsType_sql2016sp1();
    }

    /**
     * Create an instance of {@link ParallelismType_sql2016sp1 .Activation_sql2016sp1 }
     * 
     */
    public ParallelismType_sql2016sp1 .Activation_sql2016sp1 createParallelismType_sql2016sp1Activation_sql2016sp1() {
        return new ParallelismType_sql2016sp1 .Activation_sql2016sp1();
    }

    /**
     * Create an instance of {@link ParallelismType_sql2016sp1 .BrickRouting_sql2016sp1 }
     * 
     */
    public ParallelismType_sql2016sp1 .BrickRouting_sql2016sp1 createParallelismType_sql2016sp1BrickRouting_sql2016sp1() {
        return new ParallelismType_sql2016sp1 .BrickRouting_sql2016sp1();
    }

    /**
     * Create an instance of {@link ConstantScanType_sql2016sp1 .Values_sql2016sp1 }
     * 
     */
    public ConstantScanType_sql2016sp1 .Values_sql2016sp1 createConstantScanType_sql2016sp1Values_sql2016sp1() {
        return new ConstantScanType_sql2016sp1 .Values_sql2016sp1();
    }

    /**
     * Create an instance of {@link CursorPlanType_sql2016sp1 .Operation_sql2016sp1 }
     * 
     */
    public CursorPlanType_sql2016sp1 .Operation_sql2016sp1 createCursorPlanType_sql2016sp1Operation_sql2016sp1() {
        return new CursorPlanType_sql2016sp1 .Operation_sql2016sp1();
    }

    /**
     * Create an instance of {@link RunTimeInformationType_sql2016sp1 .RunTimeCountersPerThread_sql2016sp1 }
     * 
     */
    public RunTimeInformationType_sql2016sp1 .RunTimeCountersPerThread_sql2016sp1 createRunTimeInformationType_sql2016sp1RunTimeCountersPerThread_sql2016sp1() {
        return new RunTimeInformationType_sql2016sp1 .RunTimeCountersPerThread_sql2016sp1();
    }

    /**
     * Create an instance of {@link ReceivePlanType_sql2016sp1 .Operation_sql2016sp1 }
     * 
     */
    public ReceivePlanType_sql2016sp1 .Operation_sql2016sp1 createReceivePlanType_sql2016sp1Operation_sql2016sp1() {
        return new ReceivePlanType_sql2016sp1 .Operation_sql2016sp1();
    }

    /**
     * Create an instance of {@link DefinedValuesListType_sql2016sp1 .DefinedValue_sql2016sp1 .ValueVector_sql2016sp1 }
     * 
     */
    public DefinedValuesListType_sql2016sp1 .DefinedValue_sql2016sp1 .ValueVector_sql2016sp1 createDefinedValuesListType_sql2016sp1DefinedValue_sql2016sp1ValueVector_sql2016sp1() {
        return new DefinedValuesListType_sql2016sp1 .DefinedValue_sql2016sp1 .ValueVector_sql2016sp1();
    }

    /**
     * Create an instance of {@link RunTimePartitionSummaryType_sql2016sp1 .PartitionsAccessed_sql2016sp1 .PartitionRange_sql2016sp1 }
     * 
     */
    public RunTimePartitionSummaryType_sql2016sp1 .PartitionsAccessed_sql2016sp1 .PartitionRange_sql2016sp1 createRunTimePartitionSummaryType_sql2016sp1PartitionsAccessed_sql2016sp1PartitionRange_sql2016sp1() {
        return new RunTimePartitionSummaryType_sql2016sp1 .PartitionsAccessed_sql2016sp1 .PartitionRange_sql2016sp1();
    }

    /**
     * Create an instance of {@link OrderByType_sql2016sp1 .OrderByColumn_sql2016sp1 }
     * 
     */
    public OrderByType_sql2016sp1 .OrderByColumn_sql2016sp1 createOrderByType_sql2016sp1OrderByColumn_sql2016sp1() {
        return new OrderByType_sql2016sp1 .OrderByColumn_sql2016sp1();
    }

    /**
     * Create an instance of {@link StmtCondType_sql2016sp1 .Condition_sql2016sp1 }
     * 
     */
    public StmtCondType_sql2016sp1 .Condition_sql2016sp1 createStmtCondType_sql2016sp1Condition_sql2016sp1() {
        return new StmtCondType_sql2016sp1 .Condition_sql2016sp1();
    }

    /**
     * Create an instance of {@link StmtCondType_sql2016sp1 .Then_sql2016sp1 }
     * 
     */
    public StmtCondType_sql2016sp1 .Then_sql2016sp1 createStmtCondType_sql2016sp1Then_sql2016sp1() {
        return new StmtCondType_sql2016sp1 .Then_sql2016sp1();
    }

    /**
     * Create an instance of {@link StmtCondType_sql2016sp1 .Else_sql2016sp1 }
     * 
     */
    public StmtCondType_sql2016sp1 .Else_sql2016sp1 createStmtCondType_sql2016sp1Else_sql2016sp1() {
        return new StmtCondType_sql2016sp1 .Else_sql2016sp1();
    }

    /**
     * Create an instance of {@link ShowPlanXML.BatchSequence_sql2016sp1 .Batch_sql2016sp1 }
     * 
     */
    public ShowPlanXML.BatchSequence_sql2016sp1 .Batch_sql2016sp1 createShowPlanXMLBatchSequence_sql2016sp1Batch_sql2016sp1() {
        return new ShowPlanXML.BatchSequence_sql2016sp1 .Batch_sql2016sp1();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DefinedValuesListType_sql2016sp1 .DefinedValue_sql2016sp1 .ValueVector_sql2016sp1 }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", name = "ValueVector", scope = DefinedValuesListType_sql2016sp1 .DefinedValue_sql2016sp1 .class)
    public JAXBElement<DefinedValuesListType_sql2016sp1 .DefinedValue_sql2016sp1 .ValueVector_sql2016sp1> createDefinedValuesListType_sql2016sp1DefinedValue_sql2016sp1ValueVector(DefinedValuesListType_sql2016sp1 .DefinedValue_sql2016sp1 .ValueVector_sql2016sp1 value) {
        return new JAXBElement<DefinedValuesListType_sql2016sp1 .DefinedValue_sql2016sp1 .ValueVector_sql2016sp1>(_DefinedValuesListType_sql2016sp1DefinedValue_sql2016sp1ValueVector_QNAME, DefinedValuesListType_sql2016sp1 .DefinedValue_sql2016sp1 .ValueVector_sql2016sp1 .class, DefinedValuesListType_sql2016sp1 .DefinedValue_sql2016sp1 .class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ScalarType_sql2016sp1 }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", name = "ScalarOperator", scope = DefinedValuesListType_sql2016sp1 .DefinedValue_sql2016sp1 .class)
    public JAXBElement<ScalarType_sql2016sp1> createDefinedValuesListType_sql2016sp1DefinedValue_sql2016sp1ScalarOperator(ScalarType_sql2016sp1 value) {
        return new JAXBElement<ScalarType_sql2016sp1>(_DefinedValuesListType_sql2016sp1DefinedValue_sql2016sp1ScalarOperator_QNAME, ScalarType_sql2016sp1 .class, DefinedValuesListType_sql2016sp1 .DefinedValue_sql2016sp1 .class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ColumnReferenceType_sql2016sp1 }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", name = "ColumnReference", scope = DefinedValuesListType_sql2016sp1 .DefinedValue_sql2016sp1 .class)
    public JAXBElement<ColumnReferenceType_sql2016sp1> createDefinedValuesListType_sql2016sp1DefinedValue_sql2016sp1ColumnReference(ColumnReferenceType_sql2016sp1 value) {
        return new JAXBElement<ColumnReferenceType_sql2016sp1>(_DefinedValuesListType_sql2016sp1DefinedValue_sql2016sp1ColumnReference_QNAME, ColumnReferenceType_sql2016sp1 .class, DefinedValuesListType_sql2016sp1 .DefinedValue_sql2016sp1 .class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ScalarType_sql2016sp1 }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", name = "ScalarOperator", scope = AssignType_sql2016sp1 .class)
    public JAXBElement<ScalarType_sql2016sp1> createAssignType_sql2016sp1ScalarOperator(ScalarType_sql2016sp1 value) {
        return new JAXBElement<ScalarType_sql2016sp1>(_DefinedValuesListType_sql2016sp1DefinedValue_sql2016sp1ScalarOperator_QNAME, ScalarType_sql2016sp1 .class, AssignType_sql2016sp1 .class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ColumnReferenceType_sql2016sp1 }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", name = "ColumnReference", scope = AssignType_sql2016sp1 .class)
    public JAXBElement<ColumnReferenceType_sql2016sp1> createAssignType_sql2016sp1ColumnReference(ColumnReferenceType_sql2016sp1 value) {
        return new JAXBElement<ColumnReferenceType_sql2016sp1>(_DefinedValuesListType_sql2016sp1DefinedValue_sql2016sp1ColumnReference_QNAME, ColumnReferenceType_sql2016sp1 .class, AssignType_sql2016sp1 .class, value);
    }

}
