
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2008;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for SortType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="SortType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RelOpBaseType">
 *       &lt;sequence>
 *         &lt;element name="OrderBy" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}OrderByType"/>
 *         &lt;element name="PartitionId" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}SingleColumnReferenceType" minOccurs="0"/>
 *         &lt;element name="RelOp" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RelOpType"/>
 *       &lt;/sequence>
 *       &lt;attribute name="Distinct" use="required" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SortType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "orderBy",
    "partitionId",
    "relOp"
})
@XmlSeeAlso({
    TopSortType_sql2008 .class
})
public class SortType_sql2008
    extends RelOpBaseType_sql2008
{

    @XmlElement(name = "OrderBy", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
    protected OrderByType_sql2008 orderBy;
    @XmlElement(name = "PartitionId", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected SingleColumnReferenceType_sql2008 partitionId;
    @XmlElement(name = "RelOp", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
    protected RelOpType_sql2008 relOp;
    @XmlAttribute(name = "Distinct", required = true)
    protected boolean distinct;

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
     * Gets the value of the partitionId property.
     * 
     * @return
     *     possible object is
     *     {@link SingleColumnReferenceType_sql2008 }
     *     
     */
    public SingleColumnReferenceType_sql2008 getPartitionId() {
        return partitionId;
    }

    /**
     * Sets the value of the partitionId property.
     * 
     * @param value
     *     allowed object is
     *     {@link SingleColumnReferenceType_sql2008 }
     *     
     */
    public void setPartitionId(SingleColumnReferenceType_sql2008 value) {
        this.partitionId = value;
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
     * Gets the value of the distinct property.
     * 
     */
    public boolean isDistinct() {
        return distinct;
    }

    /**
     * Sets the value of the distinct property.
     * 
     */
    public void setDistinct(boolean value) {
        this.distinct = value;
    }

}
