
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2008;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ParallelismType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ParallelismType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RelOpBaseType">
 *       &lt;sequence>
 *         &lt;element name="PartitionColumns" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ColumnReferenceListType" minOccurs="0"/>
 *         &lt;element name="OrderBy" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}OrderByType" minOccurs="0"/>
 *         &lt;element name="HashKeys" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ColumnReferenceListType" minOccurs="0"/>
 *         &lt;element name="ProbeColumn" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}SingleColumnReferenceType" minOccurs="0"/>
 *         &lt;element name="Predicate" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ScalarExpressionType" minOccurs="0"/>
 *         &lt;element name="RelOp" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RelOpType"/>
 *       &lt;/sequence>
 *       &lt;attribute name="PartitioningType" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}PartitionType" />
 *       &lt;attribute name="InRow" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ParallelismType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "partitionColumns",
    "orderBy",
    "hashKeys",
    "probeColumn",
    "predicate",
    "relOp"
})
public class ParallelismType_sql2008
    extends RelOpBaseType_sql2008
{

    @XmlElement(name = "PartitionColumns", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ColumnReferenceListType_sql2008 partitionColumns;
    @XmlElement(name = "OrderBy", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected OrderByType_sql2008 orderBy;
    @XmlElement(name = "HashKeys", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ColumnReferenceListType_sql2008 hashKeys;
    @XmlElement(name = "ProbeColumn", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected SingleColumnReferenceType_sql2008 probeColumn;
    @XmlElement(name = "Predicate", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ScalarExpressionType_sql2008 predicate;
    @XmlElement(name = "RelOp", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
    protected RelOpType_sql2008 relOp;
    @XmlAttribute(name = "PartitioningType")
    protected PartitionType_sql2008 partitioningType;
    @XmlAttribute(name = "InRow")
    protected Boolean inRow;

    /**
     * Gets the value of the partitionColumns property.
     * 
     * @return
     *     possible object is
     *     {@link ColumnReferenceListType_sql2008 }
     *     
     */
    public ColumnReferenceListType_sql2008 getPartitionColumns() {
        return partitionColumns;
    }

    /**
     * Sets the value of the partitionColumns property.
     * 
     * @param value
     *     allowed object is
     *     {@link ColumnReferenceListType_sql2008 }
     *     
     */
    public void setPartitionColumns(ColumnReferenceListType_sql2008 value) {
        this.partitionColumns = value;
    }

    /**
     * Gets the value of the orderBy property.
     * 
     * @return
     *     possible object is
     *     {@link OrderByType_sql2008 }
     *     
     */
    public OrderByType_sql2008 getOrderBy() {
        return orderBy;
    }

    /**
     * Sets the value of the orderBy property.
     * 
     * @param value
     *     allowed object is
     *     {@link OrderByType_sql2008 }
     *     
     */
    public void setOrderBy(OrderByType_sql2008 value) {
        this.orderBy = value;
    }

    /**
     * Gets the value of the hashKeys property.
     * 
     * @return
     *     possible object is
     *     {@link ColumnReferenceListType_sql2008 }
     *     
     */
    public ColumnReferenceListType_sql2008 getHashKeys() {
        return hashKeys;
    }

    /**
     * Sets the value of the hashKeys property.
     * 
     * @param value
     *     allowed object is
     *     {@link ColumnReferenceListType_sql2008 }
     *     
     */
    public void setHashKeys(ColumnReferenceListType_sql2008 value) {
        this.hashKeys = value;
    }

    /**
     * Gets the value of the probeColumn property.
     * 
     * @return
     *     possible object is
     *     {@link SingleColumnReferenceType_sql2008 }
     *     
     */
    public SingleColumnReferenceType_sql2008 getProbeColumn() {
        return probeColumn;
    }

    /**
     * Sets the value of the probeColumn property.
     * 
     * @param value
     *     allowed object is
     *     {@link SingleColumnReferenceType_sql2008 }
     *     
     */
    public void setProbeColumn(SingleColumnReferenceType_sql2008 value) {
        this.probeColumn = value;
    }

    /**
     * Gets the value of the predicate property.
     * 
     * @return
     *     possible object is
     *     {@link ScalarExpressionType_sql2008 }
     *     
     */
    public ScalarExpressionType_sql2008 getPredicate() {
        return predicate;
    }

    /**
     * Sets the value of the predicate property.
     * 
     * @param value
     *     allowed object is
     *     {@link ScalarExpressionType_sql2008 }
     *     
     */
    public void setPredicate(ScalarExpressionType_sql2008 value) {
        this.predicate = value;
    }

    /**
     * Gets the value of the relOp property.
     * 
     * @return
     *     possible object is
     *     {@link RelOpType_sql2008 }
     *     
     */
    public RelOpType_sql2008 getRelOp() {
        return relOp;
    }

    /**
     * Sets the value of the relOp property.
     * 
     * @param value
     *     allowed object is
     *     {@link RelOpType_sql2008 }
     *     
     */
    public void setRelOp(RelOpType_sql2008 value) {
        this.relOp = value;
    }

    /**
     * Gets the value of the partitioningType property.
     * 
     * @return
     *     possible object is
     *     {@link PartitionType_sql2008 }
     *     
     */
    public PartitionType_sql2008 getPartitioningType() {
        return partitioningType;
    }

    /**
     * Sets the value of the partitioningType property.
     * 
     * @param value
     *     allowed object is
     *     {@link PartitionType_sql2008 }
     *     
     */
    public void setPartitioningType(PartitionType_sql2008 value) {
        this.partitioningType = value;
    }

    /**
     * Gets the value of the inRow property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean getInRow() {
        return inRow;
    }

    /**
     * Sets the value of the inRow property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setInRow(Boolean value) {
        this.inRow = value;
    }

}
