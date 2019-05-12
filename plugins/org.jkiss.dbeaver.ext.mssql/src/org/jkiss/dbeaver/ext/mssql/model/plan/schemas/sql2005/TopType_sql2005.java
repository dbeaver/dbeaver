
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2005;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for TopType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="TopType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RelOpBaseType">
 *       &lt;sequence>
 *         &lt;element name="TieColumns" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ColumnReferenceListType" minOccurs="0"/>
 *         &lt;element name="TopExpression" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ScalarExpressionType" minOccurs="0"/>
 *         &lt;element name="RelOp" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RelOpType"/>
 *       &lt;/sequence>
 *       &lt;attribute name="RowCount" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="Rows" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="IsPercent" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="WithTies" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TopType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "tieColumns",
    "topExpression",
    "relOp"
})
public class TopType_sql2005
    extends RelOpBaseType_sql2005
{

    @XmlElement(name = "TieColumns", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ColumnReferenceListType_sql2005 tieColumns;
    @XmlElement(name = "TopExpression", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ScalarExpressionType_sql2005 topExpression;
    @XmlElement(name = "RelOp", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
    protected RelOpType_sql2005 relOp;
    @XmlAttribute(name = "RowCount")
    protected Boolean rowCount;
    @XmlAttribute(name = "Rows")
    protected Integer rows;
    @XmlAttribute(name = "IsPercent")
    protected Boolean isPercent;
    @XmlAttribute(name = "WithTies")
    protected Boolean withTies;

    /**
     * Gets the value of the tieColumns property.
     * 
     * @return
     *     possible object is
     *     {@link ColumnReferenceListType_sql2005 }
     *     
     */
    public ColumnReferenceListType_sql2005 getTieColumns() {
        return tieColumns;
    }

    /**
     * Sets the value of the tieColumns property.
     * 
     * @param value
     *     allowed object is
     *     {@link ColumnReferenceListType_sql2005 }
     *     
     */
    public void setTieColumns(ColumnReferenceListType_sql2005 value) {
        this.tieColumns = value;
    }

    /**
     * Gets the value of the topExpression property.
     * 
     * @return
     *     possible object is
     *     {@link ScalarExpressionType_sql2005 }
     *     
     */
    public ScalarExpressionType_sql2005 getTopExpression() {
        return topExpression;
    }

    /**
     * Sets the value of the topExpression property.
     * 
     * @param value
     *     allowed object is
     *     {@link ScalarExpressionType_sql2005 }
     *     
     */
    public void setTopExpression(ScalarExpressionType_sql2005 value) {
        this.topExpression = value;
    }

    /**
     * Gets the value of the relOp property.
     * 
     * @return
     *     possible object is
     *     {@link RelOpType_sql2005 }
     *     
     */
    public RelOpType_sql2005 getRelOp() {
        return relOp;
    }

    /**
     * Sets the value of the relOp property.
     * 
     * @param value
     *     allowed object is
     *     {@link RelOpType_sql2005 }
     *     
     */
    public void setRelOp(RelOpType_sql2005 value) {
        this.relOp = value;
    }

    /**
     * Gets the value of the rowCount property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean getRowCount() {
        return rowCount;
    }

    /**
     * Sets the value of the rowCount property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setRowCount(Boolean value) {
        this.rowCount = value;
    }

    /**
     * Gets the value of the rows property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getRows() {
        return rows;
    }

    /**
     * Sets the value of the rows property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setRows(Integer value) {
        this.rows = value;
    }

    /**
     * Gets the value of the isPercent property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean getIsPercent() {
        return isPercent;
    }

    /**
     * Sets the value of the isPercent property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setIsPercent(Boolean value) {
        this.isPercent = value;
    }

    /**
     * Gets the value of the withTies property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean getWithTies() {
        return withTies;
    }

    /**
     * Sets the value of the withTies property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setWithTies(Boolean value) {
        this.withTies = value;
    }

}
