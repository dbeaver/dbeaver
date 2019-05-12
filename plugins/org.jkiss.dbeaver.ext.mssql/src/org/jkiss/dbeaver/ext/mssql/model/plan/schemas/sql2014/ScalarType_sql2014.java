
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2014;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * Scalar expression. If root of scalar tree contains semantically equivalent string representation of entire expression
 * 
 * <p>Java class for ScalarType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ScalarType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;choice>
 *           &lt;element name="Aggregate" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}AggregateType"/>
 *           &lt;element name="Arithmetic" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ArithmeticType"/>
 *           &lt;element name="Assign" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}AssignType"/>
 *           &lt;element name="Compare" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}CompareType"/>
 *           &lt;element name="Const" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ConstType"/>
 *           &lt;element name="Convert" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ConvertType"/>
 *           &lt;element name="Identifier" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}IdentType"/>
 *           &lt;element name="IF" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ConditionalType"/>
 *           &lt;element name="Intrinsic" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}IntrinsicType"/>
 *           &lt;element name="Logical" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}LogicalType"/>
 *           &lt;element name="MultipleAssign" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}MultAssignType"/>
 *           &lt;element name="ScalarExpressionList" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ScalarExpressionListType"/>
 *           &lt;element name="Sequence" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ScalarSequenceType"/>
 *           &lt;element name="Subquery" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}SubqueryType"/>
 *           &lt;element name="UDTMethod" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}UDTMethodType"/>
 *           &lt;element name="UserDefinedAggregate" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}UDAggregateType"/>
 *           &lt;element name="UserDefinedFunction" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}UDFType"/>
 *         &lt;/choice>
 *         &lt;element name="InternalInfo" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}InternalInfoType" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="ScalarString" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ScalarType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "aggregate",
    "arithmetic",
    "assign",
    "compare",
    "_const",
    "convert",
    "identifier",
    "_if",
    "intrinsic",
    "logical",
    "multipleAssign",
    "scalarExpressionList",
    "sequence",
    "subquery",
    "udtMethod",
    "userDefinedAggregate",
    "userDefinedFunction",
    "internalInfo"
})
public class ScalarType_sql2014 {

    @XmlElement(name = "Aggregate", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected AggregateType_sql2014 aggregate;
    @XmlElement(name = "Arithmetic", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ArithmeticType_sql2014 arithmetic;
    @XmlElement(name = "Assign", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected AssignType_sql2014 assign;
    @XmlElement(name = "Compare", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected CompareType_sql2014 compare;
    @XmlElement(name = "Const", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ConstType_sql2014 _const;
    @XmlElement(name = "Convert", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ConvertType_sql2014 convert;
    @XmlElement(name = "Identifier", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected IdentType_sql2014 identifier;
    @XmlElement(name = "IF", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ConditionalType_sql2014 _if;
    @XmlElement(name = "Intrinsic", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected IntrinsicType_sql2014 intrinsic;
    @XmlElement(name = "Logical", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected LogicalType_sql2014 logical;
    @XmlElement(name = "MultipleAssign", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected MultAssignType_sql2014 multipleAssign;
    @XmlElement(name = "ScalarExpressionList", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ScalarExpressionListType_sql2014 scalarExpressionList;
    @XmlElement(name = "Sequence", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ScalarSequenceType_sql2014 sequence;
    @XmlElement(name = "Subquery", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected SubqueryType_sql2014 subquery;
    @XmlElement(name = "UDTMethod", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected UDTMethodType_sql2014 udtMethod;
    @XmlElement(name = "UserDefinedAggregate", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected UDAggregateType_sql2014 userDefinedAggregate;
    @XmlElement(name = "UserDefinedFunction", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected UDFType_sql2014 userDefinedFunction;
    @XmlElement(name = "InternalInfo", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected InternalInfoType_sql2014 internalInfo;
    @XmlAttribute(name = "ScalarString")
    protected String scalarString;

    /**
     * Gets the value of the aggregate property.
     * 
     * @return
     *     possible object is
     *     {@link AggregateType_sql2014 }
     *     
     */
    public AggregateType_sql2014 getAggregate() {
        return aggregate;
    }

    /**
     * Sets the value of the aggregate property.
     * 
     * @param value
     *     allowed object is
     *     {@link AggregateType_sql2014 }
     *     
     */
    public void setAggregate(AggregateType_sql2014 value) {
        this.aggregate = value;
    }

    /**
     * Gets the value of the arithmetic property.
     * 
     * @return
     *     possible object is
     *     {@link ArithmeticType_sql2014 }
     *     
     */
    public ArithmeticType_sql2014 getArithmetic() {
        return arithmetic;
    }

    /**
     * Sets the value of the arithmetic property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArithmeticType_sql2014 }
     *     
     */
    public void setArithmetic(ArithmeticType_sql2014 value) {
        this.arithmetic = value;
    }

    /**
     * Gets the value of the assign property.
     * 
     * @return
     *     possible object is
     *     {@link AssignType_sql2014 }
     *     
     */
    public AssignType_sql2014 getAssign() {
        return assign;
    }

    /**
     * Sets the value of the assign property.
     * 
     * @param value
     *     allowed object is
     *     {@link AssignType_sql2014 }
     *     
     */
    public void setAssign(AssignType_sql2014 value) {
        this.assign = value;
    }

    /**
     * Gets the value of the compare property.
     * 
     * @return
     *     possible object is
     *     {@link CompareType_sql2014 }
     *     
     */
    public CompareType_sql2014 getCompare() {
        return compare;
    }

    /**
     * Sets the value of the compare property.
     * 
     * @param value
     *     allowed object is
     *     {@link CompareType_sql2014 }
     *     
     */
    public void setCompare(CompareType_sql2014 value) {
        this.compare = value;
    }

    /**
     * Gets the value of the const property.
     * 
     * @return
     *     possible object is
     *     {@link ConstType_sql2014 }
     *     
     */
    public ConstType_sql2014 getConst() {
        return _const;
    }

    /**
     * Sets the value of the const property.
     * 
     * @param value
     *     allowed object is
     *     {@link ConstType_sql2014 }
     *     
     */
    public void setConst(ConstType_sql2014 value) {
        this._const = value;
    }

    /**
     * Gets the value of the convert property.
     * 
     * @return
     *     possible object is
     *     {@link ConvertType_sql2014 }
     *     
     */
    public ConvertType_sql2014 getConvert() {
        return convert;
    }

    /**
     * Sets the value of the convert property.
     * 
     * @param value
     *     allowed object is
     *     {@link ConvertType_sql2014 }
     *     
     */
    public void setConvert(ConvertType_sql2014 value) {
        this.convert = value;
    }

    /**
     * Gets the value of the identifier property.
     * 
     * @return
     *     possible object is
     *     {@link IdentType_sql2014 }
     *     
     */
    public IdentType_sql2014 getIdentifier() {
        return identifier;
    }

    /**
     * Sets the value of the identifier property.
     * 
     * @param value
     *     allowed object is
     *     {@link IdentType_sql2014 }
     *     
     */
    public void setIdentifier(IdentType_sql2014 value) {
        this.identifier = value;
    }

    /**
     * Gets the value of the if property.
     * 
     * @return
     *     possible object is
     *     {@link ConditionalType_sql2014 }
     *     
     */
    public ConditionalType_sql2014 getIF() {
        return _if;
    }

    /**
     * Sets the value of the if property.
     * 
     * @param value
     *     allowed object is
     *     {@link ConditionalType_sql2014 }
     *     
     */
    public void setIF(ConditionalType_sql2014 value) {
        this._if = value;
    }

    /**
     * Gets the value of the intrinsic property.
     * 
     * @return
     *     possible object is
     *     {@link IntrinsicType_sql2014 }
     *     
     */
    public IntrinsicType_sql2014 getIntrinsic() {
        return intrinsic;
    }

    /**
     * Sets the value of the intrinsic property.
     * 
     * @param value
     *     allowed object is
     *     {@link IntrinsicType_sql2014 }
     *     
     */
    public void setIntrinsic(IntrinsicType_sql2014 value) {
        this.intrinsic = value;
    }

    /**
     * Gets the value of the logical property.
     * 
     * @return
     *     possible object is
     *     {@link LogicalType_sql2014 }
     *     
     */
    public LogicalType_sql2014 getLogical() {
        return logical;
    }

    /**
     * Sets the value of the logical property.
     * 
     * @param value
     *     allowed object is
     *     {@link LogicalType_sql2014 }
     *     
     */
    public void setLogical(LogicalType_sql2014 value) {
        this.logical = value;
    }

    /**
     * Gets the value of the multipleAssign property.
     * 
     * @return
     *     possible object is
     *     {@link MultAssignType_sql2014 }
     *     
     */
    public MultAssignType_sql2014 getMultipleAssign() {
        return multipleAssign;
    }

    /**
     * Sets the value of the multipleAssign property.
     * 
     * @param value
     *     allowed object is
     *     {@link MultAssignType_sql2014 }
     *     
     */
    public void setMultipleAssign(MultAssignType_sql2014 value) {
        this.multipleAssign = value;
    }

    /**
     * Gets the value of the scalarExpressionList property.
     * 
     * @return
     *     possible object is
     *     {@link ScalarExpressionListType_sql2014 }
     *     
     */
    public ScalarExpressionListType_sql2014 getScalarExpressionList() {
        return scalarExpressionList;
    }

    /**
     * Sets the value of the scalarExpressionList property.
     * 
     * @param value
     *     allowed object is
     *     {@link ScalarExpressionListType_sql2014 }
     *     
     */
    public void setScalarExpressionList(ScalarExpressionListType_sql2014 value) {
        this.scalarExpressionList = value;
    }

    /**
     * Gets the value of the sequence property.
     * 
     * @return
     *     possible object is
     *     {@link ScalarSequenceType_sql2014 }
     *     
     */
    public ScalarSequenceType_sql2014 getSequence() {
        return sequence;
    }

    /**
     * Sets the value of the sequence property.
     * 
     * @param value
     *     allowed object is
     *     {@link ScalarSequenceType_sql2014 }
     *     
     */
    public void setSequence(ScalarSequenceType_sql2014 value) {
        this.sequence = value;
    }

    /**
     * Gets the value of the subquery property.
     * 
     * @return
     *     possible object is
     *     {@link SubqueryType_sql2014 }
     *     
     */
    public SubqueryType_sql2014 getSubquery() {
        return subquery;
    }

    /**
     * Sets the value of the subquery property.
     * 
     * @param value
     *     allowed object is
     *     {@link SubqueryType_sql2014 }
     *     
     */
    public void setSubquery(SubqueryType_sql2014 value) {
        this.subquery = value;
    }

    /**
     * Gets the value of the udtMethod property.
     * 
     * @return
     *     possible object is
     *     {@link UDTMethodType_sql2014 }
     *     
     */
    public UDTMethodType_sql2014 getUDTMethod() {
        return udtMethod;
    }

    /**
     * Sets the value of the udtMethod property.
     * 
     * @param value
     *     allowed object is
     *     {@link UDTMethodType_sql2014 }
     *     
     */
    public void setUDTMethod(UDTMethodType_sql2014 value) {
        this.udtMethod = value;
    }

    /**
     * Gets the value of the userDefinedAggregate property.
     * 
     * @return
     *     possible object is
     *     {@link UDAggregateType_sql2014 }
     *     
     */
    public UDAggregateType_sql2014 getUserDefinedAggregate() {
        return userDefinedAggregate;
    }

    /**
     * Sets the value of the userDefinedAggregate property.
     * 
     * @param value
     *     allowed object is
     *     {@link UDAggregateType_sql2014 }
     *     
     */
    public void setUserDefinedAggregate(UDAggregateType_sql2014 value) {
        this.userDefinedAggregate = value;
    }

    /**
     * Gets the value of the userDefinedFunction property.
     * 
     * @return
     *     possible object is
     *     {@link UDFType_sql2014 }
     *     
     */
    public UDFType_sql2014 getUserDefinedFunction() {
        return userDefinedFunction;
    }

    /**
     * Sets the value of the userDefinedFunction property.
     * 
     * @param value
     *     allowed object is
     *     {@link UDFType_sql2014 }
     *     
     */
    public void setUserDefinedFunction(UDFType_sql2014 value) {
        this.userDefinedFunction = value;
    }

    /**
     * Gets the value of the internalInfo property.
     * 
     * @return
     *     possible object is
     *     {@link InternalInfoType_sql2014 }
     *     
     */
    public InternalInfoType_sql2014 getInternalInfo() {
        return internalInfo;
    }

    /**
     * Sets the value of the internalInfo property.
     * 
     * @param value
     *     allowed object is
     *     {@link InternalInfoType_sql2014 }
     *     
     */
    public void setInternalInfo(InternalInfoType_sql2014 value) {
        this.internalInfo = value;
    }

    /**
     * Gets the value of the scalarString property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getScalarString() {
        return scalarString;
    }

    /**
     * Sets the value of the scalarString property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setScalarString(String value) {
        this.scalarString = value;
    }

}
