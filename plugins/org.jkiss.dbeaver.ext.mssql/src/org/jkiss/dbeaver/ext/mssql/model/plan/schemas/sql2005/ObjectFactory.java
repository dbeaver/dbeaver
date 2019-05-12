
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2005;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2005 package. 
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

    private final static QName _DefinedValuesListType_sql2005DefinedValue_sql2005ValueVector_QNAME = new QName("http://schemas.microsoft.com/sqlserver/2004/07/showplan", "ValueVector");
    private final static QName _DefinedValuesListType_sql2005DefinedValue_sql2005ScalarOperator_QNAME = new QName("http://schemas.microsoft.com/sqlserver/2004/07/showplan", "ScalarOperator");
    private final static QName _DefinedValuesListType_sql2005DefinedValue_sql2005ColumnReference_QNAME = new QName("http://schemas.microsoft.com/sqlserver/2004/07/showplan", "ColumnReference");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2005
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
     * Create an instance of {@link DefinedValuesListType_sql2005 }
     * 
     */
    public DefinedValuesListType_sql2005 createDefinedValuesListType_sql2005() {
        return new DefinedValuesListType_sql2005();
    }

    /**
     * Create an instance of {@link DefinedValuesListType_sql2005 .DefinedValue_sql2005 }
     * 
     */
    public DefinedValuesListType_sql2005 .DefinedValue_sql2005 createDefinedValuesListType_sql2005DefinedValue_sql2005() {
        return new DefinedValuesListType_sql2005 .DefinedValue_sql2005();
    }

    /**
     * Create an instance of {@link ConstantScanType_sql2005 }
     * 
     */
    public ConstantScanType_sql2005 createConstantScanType_sql2005() {
        return new ConstantScanType_sql2005();
    }

    /**
     * Create an instance of {@link CursorPlanType_sql2005 }
     * 
     */
    public CursorPlanType_sql2005 createCursorPlanType_sql2005() {
        return new CursorPlanType_sql2005();
    }

    /**
     * Create an instance of {@link OrderByType_sql2005 }
     * 
     */
    public OrderByType_sql2005 createOrderByType_sql2005() {
        return new OrderByType_sql2005();
    }

    /**
     * Create an instance of {@link RunTimeInformationType_sql2005 }
     * 
     */
    public RunTimeInformationType_sql2005 createRunTimeInformationType_sql2005() {
        return new RunTimeInformationType_sql2005();
    }

    /**
     * Create an instance of {@link StmtCondType_sql2005 }
     * 
     */
    public StmtCondType_sql2005 createStmtCondType_sql2005() {
        return new StmtCondType_sql2005();
    }

    /**
     * Create an instance of {@link ReceivePlanType_sql2005 }
     * 
     */
    public ReceivePlanType_sql2005 createReceivePlanType_sql2005() {
        return new ReceivePlanType_sql2005();
    }

    /**
     * Create an instance of {@link ShowPlanXML.BatchSequence_sql2005 }
     * 
     */
    public ShowPlanXML.BatchSequence_sql2005 createShowPlanXMLBatchSequence_sql2005() {
        return new ShowPlanXML.BatchSequence_sql2005();
    }

    /**
     * Create an instance of {@link ColumnType_sql2005 }
     * 
     */
    public ColumnType_sql2005 createColumnType_sql2005() {
        return new ColumnType_sql2005();
    }

    /**
     * Create an instance of {@link ColumnGroupType_sql2005 }
     * 
     */
    public ColumnGroupType_sql2005 createColumnGroupType_sql2005() {
        return new ColumnGroupType_sql2005();
    }

    /**
     * Create an instance of {@link BaseStmtInfoType_sql2005 }
     * 
     */
    public BaseStmtInfoType_sql2005 createBaseStmtInfoType_sql2005() {
        return new BaseStmtInfoType_sql2005();
    }

    /**
     * Create an instance of {@link LogicalType_sql2005 }
     * 
     */
    public LogicalType_sql2005 createLogicalType_sql2005() {
        return new LogicalType_sql2005();
    }

    /**
     * Create an instance of {@link SubqueryType_sql2005 }
     * 
     */
    public SubqueryType_sql2005 createSubqueryType_sql2005() {
        return new SubqueryType_sql2005();
    }

    /**
     * Create an instance of {@link AggregateType_sql2005 }
     * 
     */
    public AggregateType_sql2005 createAggregateType_sql2005() {
        return new AggregateType_sql2005();
    }

    /**
     * Create an instance of {@link UDFType_sql2005 }
     * 
     */
    public UDFType_sql2005 createUDFType_sql2005() {
        return new UDFType_sql2005();
    }

    /**
     * Create an instance of {@link ScalarExpressionListType_sql2005 }
     * 
     */
    public ScalarExpressionListType_sql2005 createScalarExpressionListType_sql2005() {
        return new ScalarExpressionListType_sql2005();
    }

    /**
     * Create an instance of {@link RelOpType_sql2005 }
     * 
     */
    public RelOpType_sql2005 createRelOpType_sql2005() {
        return new RelOpType_sql2005();
    }

    /**
     * Create an instance of {@link SeekPredicatesType_sql2005 }
     * 
     */
    public SeekPredicatesType_sql2005 createSeekPredicatesType_sql2005() {
        return new SeekPredicatesType_sql2005();
    }

    /**
     * Create an instance of {@link AssignType_sql2005 }
     * 
     */
    public AssignType_sql2005 createAssignType_sql2005() {
        return new AssignType_sql2005();
    }

    /**
     * Create an instance of {@link InternalInfoType_sql2005 }
     * 
     */
    public InternalInfoType_sql2005 createInternalInfoType_sql2005() {
        return new InternalInfoType_sql2005();
    }

    /**
     * Create an instance of {@link StmtSimpleType_sql2005 }
     * 
     */
    public StmtSimpleType_sql2005 createStmtSimpleType_sql2005() {
        return new StmtSimpleType_sql2005();
    }

    /**
     * Create an instance of {@link MergeType_sql2005 }
     * 
     */
    public MergeType_sql2005 createMergeType_sql2005() {
        return new MergeType_sql2005();
    }

    /**
     * Create an instance of {@link MemoryFractionsType_sql2005 }
     * 
     */
    public MemoryFractionsType_sql2005 createMemoryFractionsType_sql2005() {
        return new MemoryFractionsType_sql2005();
    }

    /**
     * Create an instance of {@link TableValuedFunctionType_sql2005 }
     * 
     */
    public TableValuedFunctionType_sql2005 createTableValuedFunctionType_sql2005() {
        return new TableValuedFunctionType_sql2005();
    }

    /**
     * Create an instance of {@link ConvertType_sql2005 }
     * 
     */
    public ConvertType_sql2005 createConvertType_sql2005() {
        return new ConvertType_sql2005();
    }

    /**
     * Create an instance of {@link RemoteType_sql2005 }
     * 
     */
    public RemoteType_sql2005 createRemoteType_sql2005() {
        return new RemoteType_sql2005();
    }

    /**
     * Create an instance of {@link IdentType_sql2005 }
     * 
     */
    public IdentType_sql2005 createIdentType_sql2005() {
        return new IdentType_sql2005();
    }

    /**
     * Create an instance of {@link CollapseType_sql2005 }
     * 
     */
    public CollapseType_sql2005 createCollapseType_sql2005() {
        return new CollapseType_sql2005();
    }

    /**
     * Create an instance of {@link MultAssignType_sql2005 }
     * 
     */
    public MultAssignType_sql2005 createMultAssignType_sql2005() {
        return new MultAssignType_sql2005();
    }

    /**
     * Create an instance of {@link HashType_sql2005 }
     * 
     */
    public HashType_sql2005 createHashType_sql2005() {
        return new HashType_sql2005();
    }

    /**
     * Create an instance of {@link RemoteFetchType_sql2005 }
     * 
     */
    public RemoteFetchType_sql2005 createRemoteFetchType_sql2005() {
        return new RemoteFetchType_sql2005();
    }

    /**
     * Create an instance of {@link CLRFunctionType_sql2005 }
     * 
     */
    public CLRFunctionType_sql2005 createCLRFunctionType_sql2005() {
        return new CLRFunctionType_sql2005();
    }

    /**
     * Create an instance of {@link StmtCursorType_sql2005 }
     * 
     */
    public StmtCursorType_sql2005 createStmtCursorType_sql2005() {
        return new StmtCursorType_sql2005();
    }

    /**
     * Create an instance of {@link RemoteQueryType_sql2005 }
     * 
     */
    public RemoteQueryType_sql2005 createRemoteQueryType_sql2005() {
        return new RemoteQueryType_sql2005();
    }

    /**
     * Create an instance of {@link ColumnReferenceListType_sql2005 }
     * 
     */
    public ColumnReferenceListType_sql2005 createColumnReferenceListType_sql2005() {
        return new ColumnReferenceListType_sql2005();
    }

    /**
     * Create an instance of {@link ConcatType_sql2005 }
     * 
     */
    public ConcatType_sql2005 createConcatType_sql2005() {
        return new ConcatType_sql2005();
    }

    /**
     * Create an instance of {@link IntrinsicType_sql2005 }
     * 
     */
    public IntrinsicType_sql2005 createIntrinsicType_sql2005() {
        return new IntrinsicType_sql2005();
    }

    /**
     * Create an instance of {@link FilterType_sql2005 }
     * 
     */
    public FilterType_sql2005 createFilterType_sql2005() {
        return new FilterType_sql2005();
    }

    /**
     * Create an instance of {@link ArithmeticType_sql2005 }
     * 
     */
    public ArithmeticType_sql2005 createArithmeticType_sql2005() {
        return new ArithmeticType_sql2005();
    }

    /**
     * Create an instance of {@link MissingIndexGroupType_sql2005 }
     * 
     */
    public MissingIndexGroupType_sql2005 createMissingIndexGroupType_sql2005() {
        return new MissingIndexGroupType_sql2005();
    }

    /**
     * Create an instance of {@link UDTMethodType_sql2005 }
     * 
     */
    public UDTMethodType_sql2005 createUDTMethodType_sql2005() {
        return new UDTMethodType_sql2005();
    }

    /**
     * Create an instance of {@link TableScanType_sql2005 }
     * 
     */
    public TableScanType_sql2005 createTableScanType_sql2005() {
        return new TableScanType_sql2005();
    }

    /**
     * Create an instance of {@link ScalarInsertType_sql2005 }
     * 
     */
    public ScalarInsertType_sql2005 createScalarInsertType_sql2005() {
        return new ScalarInsertType_sql2005();
    }

    /**
     * Create an instance of {@link ColumnReferenceType_sql2005 }
     * 
     */
    public ColumnReferenceType_sql2005 createColumnReferenceType_sql2005() {
        return new ColumnReferenceType_sql2005();
    }

    /**
     * Create an instance of {@link SeekPredicateType_sql2005 }
     * 
     */
    public SeekPredicateType_sql2005 createSeekPredicateType_sql2005() {
        return new SeekPredicateType_sql2005();
    }

    /**
     * Create an instance of {@link StmtReceiveType_sql2005 }
     * 
     */
    public StmtReceiveType_sql2005 createStmtReceiveType_sql2005() {
        return new StmtReceiveType_sql2005();
    }

    /**
     * Create an instance of {@link BitmapType_sql2005 }
     * 
     */
    public BitmapType_sql2005 createBitmapType_sql2005() {
        return new BitmapType_sql2005();
    }

    /**
     * Create an instance of {@link SpoolType_sql2005 }
     * 
     */
    public SpoolType_sql2005 createSpoolType_sql2005() {
        return new SpoolType_sql2005();
    }

    /**
     * Create an instance of {@link TopType_sql2005 }
     * 
     */
    public TopType_sql2005 createTopType_sql2005() {
        return new TopType_sql2005();
    }

    /**
     * Create an instance of {@link ScalarType_sql2005 }
     * 
     */
    public ScalarType_sql2005 createScalarType_sql2005() {
        return new ScalarType_sql2005();
    }

    /**
     * Create an instance of {@link StreamAggregateType_sql2005 }
     * 
     */
    public StreamAggregateType_sql2005 createStreamAggregateType_sql2005() {
        return new StreamAggregateType_sql2005();
    }

    /**
     * Create an instance of {@link CompareType_sql2005 }
     * 
     */
    public CompareType_sql2005 createCompareType_sql2005() {
        return new CompareType_sql2005();
    }

    /**
     * Create an instance of {@link StmtBlockType_sql2005 }
     * 
     */
    public StmtBlockType_sql2005 createStmtBlockType_sql2005() {
        return new StmtBlockType_sql2005();
    }

    /**
     * Create an instance of {@link SequenceType_sql2005 }
     * 
     */
    public SequenceType_sql2005 createSequenceType_sql2005() {
        return new SequenceType_sql2005();
    }

    /**
     * Create an instance of {@link SimpleIteratorOneChildType_sql2005 }
     * 
     */
    public SimpleIteratorOneChildType_sql2005 createSimpleIteratorOneChildType_sql2005() {
        return new SimpleIteratorOneChildType_sql2005();
    }

    /**
     * Create an instance of {@link MissingIndexesType_sql2005 }
     * 
     */
    public MissingIndexesType_sql2005 createMissingIndexesType_sql2005() {
        return new MissingIndexesType_sql2005();
    }

    /**
     * Create an instance of {@link ComputeScalarType_sql2005 }
     * 
     */
    public ComputeScalarType_sql2005 createComputeScalarType_sql2005() {
        return new ComputeScalarType_sql2005();
    }

    /**
     * Create an instance of {@link NestedLoopsType_sql2005 }
     * 
     */
    public NestedLoopsType_sql2005 createNestedLoopsType_sql2005() {
        return new NestedLoopsType_sql2005();
    }

    /**
     * Create an instance of {@link ObjectType_sql2005 }
     * 
     */
    public ObjectType_sql2005 createObjectType_sql2005() {
        return new ObjectType_sql2005();
    }

    /**
     * Create an instance of {@link IndexedViewInfoType_sql2005 }
     * 
     */
    public IndexedViewInfoType_sql2005 createIndexedViewInfoType_sql2005() {
        return new IndexedViewInfoType_sql2005();
    }

    /**
     * Create an instance of {@link RemoteModifyType_sql2005 }
     * 
     */
    public RemoteModifyType_sql2005 createRemoteModifyType_sql2005() {
        return new RemoteModifyType_sql2005();
    }

    /**
     * Create an instance of {@link SplitType_sql2005 }
     * 
     */
    public SplitType_sql2005 createSplitType_sql2005() {
        return new SplitType_sql2005();
    }

    /**
     * Create an instance of {@link StmtUseDbType_sql2005 }
     * 
     */
    public StmtUseDbType_sql2005 createStmtUseDbType_sql2005() {
        return new StmtUseDbType_sql2005();
    }

    /**
     * Create an instance of {@link SingleColumnReferenceType_sql2005 }
     * 
     */
    public SingleColumnReferenceType_sql2005 createSingleColumnReferenceType_sql2005() {
        return new SingleColumnReferenceType_sql2005();
    }

    /**
     * Create an instance of {@link ParallelismType_sql2005 }
     * 
     */
    public ParallelismType_sql2005 createParallelismType_sql2005() {
        return new ParallelismType_sql2005();
    }

    /**
     * Create an instance of {@link MissingIndexType_sql2005 }
     * 
     */
    public MissingIndexType_sql2005 createMissingIndexType_sql2005() {
        return new MissingIndexType_sql2005();
    }

    /**
     * Create an instance of {@link ConstType_sql2005 }
     * 
     */
    public ConstType_sql2005 createConstType_sql2005() {
        return new ConstType_sql2005();
    }

    /**
     * Create an instance of {@link ConditionalType_sql2005 }
     * 
     */
    public ConditionalType_sql2005 createConditionalType_sql2005() {
        return new ConditionalType_sql2005();
    }

    /**
     * Create an instance of {@link GenericType_sql2005 }
     * 
     */
    public GenericType_sql2005 createGenericType_sql2005() {
        return new GenericType_sql2005();
    }

    /**
     * Create an instance of {@link ScalarSequenceType_sql2005 }
     * 
     */
    public ScalarSequenceType_sql2005 createScalarSequenceType_sql2005() {
        return new ScalarSequenceType_sql2005();
    }

    /**
     * Create an instance of {@link QueryPlanType_sql2005 }
     * 
     */
    public QueryPlanType_sql2005 createQueryPlanType_sql2005() {
        return new QueryPlanType_sql2005();
    }

    /**
     * Create an instance of {@link SetOptionsType_sql2005 }
     * 
     */
    public SetOptionsType_sql2005 createSetOptionsType_sql2005() {
        return new SetOptionsType_sql2005();
    }

    /**
     * Create an instance of {@link FunctionType_sql2005 }
     * 
     */
    public FunctionType_sql2005 createFunctionType_sql2005() {
        return new FunctionType_sql2005();
    }

    /**
     * Create an instance of {@link RelOpBaseType_sql2005 }
     * 
     */
    public RelOpBaseType_sql2005 createRelOpBaseType_sql2005() {
        return new RelOpBaseType_sql2005();
    }

    /**
     * Create an instance of {@link ScalarExpressionType_sql2005 }
     * 
     */
    public ScalarExpressionType_sql2005 createScalarExpressionType_sql2005() {
        return new ScalarExpressionType_sql2005();
    }

    /**
     * Create an instance of {@link RowsetType_sql2005 }
     * 
     */
    public RowsetType_sql2005 createRowsetType_sql2005() {
        return new RowsetType_sql2005();
    }

    /**
     * Create an instance of {@link UpdateType_sql2005 }
     * 
     */
    public UpdateType_sql2005 createUpdateType_sql2005() {
        return new UpdateType_sql2005();
    }

    /**
     * Create an instance of {@link CreateIndexType_sql2005 }
     * 
     */
    public CreateIndexType_sql2005 createCreateIndexType_sql2005() {
        return new CreateIndexType_sql2005();
    }

    /**
     * Create an instance of {@link SortType_sql2005 }
     * 
     */
    public SortType_sql2005 createSortType_sql2005() {
        return new SortType_sql2005();
    }

    /**
     * Create an instance of {@link SegmentType_sql2005 }
     * 
     */
    public SegmentType_sql2005 createSegmentType_sql2005() {
        return new SegmentType_sql2005();
    }

    /**
     * Create an instance of {@link ScanRangeType_sql2005 }
     * 
     */
    public ScanRangeType_sql2005 createScanRangeType_sql2005() {
        return new ScanRangeType_sql2005();
    }

    /**
     * Create an instance of {@link SimpleUpdateType_sql2005 }
     * 
     */
    public SimpleUpdateType_sql2005 createSimpleUpdateType_sql2005() {
        return new SimpleUpdateType_sql2005();
    }

    /**
     * Create an instance of {@link UDAggregateType_sql2005 }
     * 
     */
    public UDAggregateType_sql2005 createUDAggregateType_sql2005() {
        return new UDAggregateType_sql2005();
    }

    /**
     * Create an instance of {@link IndexScanType_sql2005 }
     * 
     */
    public IndexScanType_sql2005 createIndexScanType_sql2005() {
        return new IndexScanType_sql2005();
    }

    /**
     * Create an instance of {@link TopSortType_sql2005 }
     * 
     */
    public TopSortType_sql2005 createTopSortType_sql2005() {
        return new TopSortType_sql2005();
    }

    /**
     * Create an instance of {@link UDXType_sql2005 }
     * 
     */
    public UDXType_sql2005 createUDXType_sql2005() {
        return new UDXType_sql2005();
    }

    /**
     * Create an instance of {@link WarningsType_sql2005 }
     * 
     */
    public WarningsType_sql2005 createWarningsType_sql2005() {
        return new WarningsType_sql2005();
    }

    /**
     * Create an instance of {@link StarJoinInfoType_sql2005 }
     * 
     */
    public StarJoinInfoType_sql2005 createStarJoinInfoType_sql2005() {
        return new StarJoinInfoType_sql2005();
    }

    /**
     * Create an instance of {@link DefinedValuesListType_sql2005 .DefinedValue_sql2005 .ValueVector_sql2005 }
     * 
     */
    public DefinedValuesListType_sql2005 .DefinedValue_sql2005 .ValueVector_sql2005 createDefinedValuesListType_sql2005DefinedValue_sql2005ValueVector_sql2005() {
        return new DefinedValuesListType_sql2005 .DefinedValue_sql2005 .ValueVector_sql2005();
    }

    /**
     * Create an instance of {@link ConstantScanType_sql2005 .Values_sql2005 }
     * 
     */
    public ConstantScanType_sql2005 .Values_sql2005 createConstantScanType_sql2005Values_sql2005() {
        return new ConstantScanType_sql2005 .Values_sql2005();
    }

    /**
     * Create an instance of {@link CursorPlanType_sql2005 .Operation_sql2005 }
     * 
     */
    public CursorPlanType_sql2005 .Operation_sql2005 createCursorPlanType_sql2005Operation_sql2005() {
        return new CursorPlanType_sql2005 .Operation_sql2005();
    }

    /**
     * Create an instance of {@link OrderByType_sql2005 .OrderByColumn_sql2005 }
     * 
     */
    public OrderByType_sql2005 .OrderByColumn_sql2005 createOrderByType_sql2005OrderByColumn_sql2005() {
        return new OrderByType_sql2005 .OrderByColumn_sql2005();
    }

    /**
     * Create an instance of {@link RunTimeInformationType_sql2005 .RunTimeCountersPerThread_sql2005 }
     * 
     */
    public RunTimeInformationType_sql2005 .RunTimeCountersPerThread_sql2005 createRunTimeInformationType_sql2005RunTimeCountersPerThread_sql2005() {
        return new RunTimeInformationType_sql2005 .RunTimeCountersPerThread_sql2005();
    }

    /**
     * Create an instance of {@link StmtCondType_sql2005 .Condition_sql2005 }
     * 
     */
    public StmtCondType_sql2005 .Condition_sql2005 createStmtCondType_sql2005Condition_sql2005() {
        return new StmtCondType_sql2005 .Condition_sql2005();
    }

    /**
     * Create an instance of {@link StmtCondType_sql2005 .Then_sql2005 }
     * 
     */
    public StmtCondType_sql2005 .Then_sql2005 createStmtCondType_sql2005Then_sql2005() {
        return new StmtCondType_sql2005 .Then_sql2005();
    }

    /**
     * Create an instance of {@link StmtCondType_sql2005 .Else_sql2005 }
     * 
     */
    public StmtCondType_sql2005 .Else_sql2005 createStmtCondType_sql2005Else_sql2005() {
        return new StmtCondType_sql2005 .Else_sql2005();
    }

    /**
     * Create an instance of {@link ReceivePlanType_sql2005 .Operation_sql2005 }
     * 
     */
    public ReceivePlanType_sql2005 .Operation_sql2005 createReceivePlanType_sql2005Operation_sql2005() {
        return new ReceivePlanType_sql2005 .Operation_sql2005();
    }

    /**
     * Create an instance of {@link ShowPlanXML.BatchSequence_sql2005 .Batch_sql2005 }
     * 
     */
    public ShowPlanXML.BatchSequence_sql2005 .Batch_sql2005 createShowPlanXMLBatchSequence_sql2005Batch_sql2005() {
        return new ShowPlanXML.BatchSequence_sql2005 .Batch_sql2005();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DefinedValuesListType_sql2005 .DefinedValue_sql2005 .ValueVector_sql2005 }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", name = "ValueVector", scope = DefinedValuesListType_sql2005 .DefinedValue_sql2005 .class)
    public JAXBElement<DefinedValuesListType_sql2005 .DefinedValue_sql2005 .ValueVector_sql2005> createDefinedValuesListType_sql2005DefinedValue_sql2005ValueVector(DefinedValuesListType_sql2005 .DefinedValue_sql2005 .ValueVector_sql2005 value) {
        return new JAXBElement<DefinedValuesListType_sql2005 .DefinedValue_sql2005 .ValueVector_sql2005>(_DefinedValuesListType_sql2005DefinedValue_sql2005ValueVector_QNAME, DefinedValuesListType_sql2005 .DefinedValue_sql2005 .ValueVector_sql2005 .class, DefinedValuesListType_sql2005 .DefinedValue_sql2005 .class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ScalarType_sql2005 }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", name = "ScalarOperator", scope = DefinedValuesListType_sql2005 .DefinedValue_sql2005 .class)
    public JAXBElement<ScalarType_sql2005> createDefinedValuesListType_sql2005DefinedValue_sql2005ScalarOperator(ScalarType_sql2005 value) {
        return new JAXBElement<ScalarType_sql2005>(_DefinedValuesListType_sql2005DefinedValue_sql2005ScalarOperator_QNAME, ScalarType_sql2005 .class, DefinedValuesListType_sql2005 .DefinedValue_sql2005 .class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ColumnReferenceType_sql2005 }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", name = "ColumnReference", scope = DefinedValuesListType_sql2005 .DefinedValue_sql2005 .class)
    public JAXBElement<ColumnReferenceType_sql2005> createDefinedValuesListType_sql2005DefinedValue_sql2005ColumnReference(ColumnReferenceType_sql2005 value) {
        return new JAXBElement<ColumnReferenceType_sql2005>(_DefinedValuesListType_sql2005DefinedValue_sql2005ColumnReference_QNAME, ColumnReferenceType_sql2005 .class, DefinedValuesListType_sql2005 .DefinedValue_sql2005 .class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ScalarType_sql2005 }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", name = "ScalarOperator", scope = AssignType_sql2005 .class)
    public JAXBElement<ScalarType_sql2005> createAssignType_sql2005ScalarOperator(ScalarType_sql2005 value) {
        return new JAXBElement<ScalarType_sql2005>(_DefinedValuesListType_sql2005DefinedValue_sql2005ScalarOperator_QNAME, ScalarType_sql2005 .class, AssignType_sql2005 .class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ColumnReferenceType_sql2005 }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", name = "ColumnReference", scope = AssignType_sql2005 .class)
    public JAXBElement<ColumnReferenceType_sql2005> createAssignType_sql2005ColumnReference(ColumnReferenceType_sql2005 value) {
        return new JAXBElement<ColumnReferenceType_sql2005>(_DefinedValuesListType_sql2005DefinedValue_sql2005ColumnReference_QNAME, ColumnReferenceType_sql2005 .class, AssignType_sql2005 .class, value);
    }

}
