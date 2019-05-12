
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2008;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for MergeType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="MergeType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RelOpBaseType">
 *       &lt;sequence>
 *         &lt;element name="InnerSideJoinColumns" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ColumnReferenceListType" minOccurs="0"/>
 *         &lt;element name="OuterSideJoinColumns" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ColumnReferenceListType" minOccurs="0"/>
 *         &lt;element name="Residual" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ScalarExpressionType" minOccurs="0"/>
 *         &lt;element name="PassThru" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ScalarExpressionType" minOccurs="0"/>
 *         &lt;element name="StarJoinInfo" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}StarJoinInfoType" minOccurs="0"/>
 *         &lt;element name="RelOp" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RelOpType" maxOccurs="2" minOccurs="2"/>
 *       &lt;/sequence>
 *       &lt;attribute name="ManyToMany" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MergeType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "innerSideJoinColumns",
    "outerSideJoinColumns",
    "residual",
    "passThru",
    "starJoinInfo",
    "relOp"
})
public class MergeType_sql2008
    extends RelOpBaseType_sql2008
{

    @XmlElement(name = "InnerSideJoinColumns", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ColumnReferenceListType_sql2008 innerSideJoinColumns;
    @XmlElement(name = "OuterSideJoinColumns", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ColumnReferenceListType_sql2008 outerSideJoinColumns;
    @XmlElement(name = "Residual", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ScalarExpressionType_sql2008 residual;
    @XmlElement(name = "PassThru", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ScalarExpressionType_sql2008 passThru;
    @XmlElement(name = "StarJoinInfo", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected StarJoinInfoType_sql2008 starJoinInfo;
    @XmlElement(name = "RelOp", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
    protected List<RelOpType_sql2008> relOp;
    @XmlAttribute(name = "ManyToMany")
    protected Boolean manyToMany;

    /**
     * Gets the value of the innerSideJoinColumns property.
     * 
     * @return
     *     possible object is
     *     {@link ColumnReferenceListType_sql2008 }
     *     
     */
    public ColumnReferenceListType_sql2008 getInnerSideJoinColumns() {
        return innerSideJoinColumns;
    }

    /**
     * Sets the value of the innerSideJoinColumns property.
     * 
     * @param value
     *     allowed object is
     *     {@link ColumnReferenceListType_sql2008 }
     *     
     */
    public void setInnerSideJoinColumns(ColumnReferenceListType_sql2008 value) {
        this.innerSideJoinColumns = value;
    }

    /**
     * Gets the value of the outerSideJoinColumns property.
     * 
     * @return
     *     possible object is
     *     {@link ColumnReferenceListType_sql2008 }
     *     
     */
    public ColumnReferenceListType_sql2008 getOuterSideJoinColumns() {
        return outerSideJoinColumns;
    }

    /**
     * Sets the value of the outerSideJoinColumns property.
     * 
     * @param value
     *     allowed object is
     *     {@link ColumnReferenceListType_sql2008 }
     *     
     */
    public void setOuterSideJoinColumns(ColumnReferenceListType_sql2008 value) {
        this.outerSideJoinColumns = value;
    }

    /**
     * Gets the value of the residual property.
     * 
     * @return
     *     possible object is
     *     {@link ScalarExpressionType_sql2008 }
     *     
     */
    public ScalarExpressionType_sql2008 getResidual() {
        return residual;
    }

    /**
     * Sets the value of the residual property.
     * 
     * @param value
     *     allowed object is
     *     {@link ScalarExpressionType_sql2008 }
     *     
     */
    public void setResidual(ScalarExpressionType_sql2008 value) {
        this.residual = value;
    }

    /**
     * Gets the value of the passThru property.
     * 
     * @return
     *     possible object is
     *     {@link ScalarExpressionType_sql2008 }
     *     
     */
    public ScalarExpressionType_sql2008 getPassThru() {
        return passThru;
    }

    /**
     * Sets the value of the passThru property.
     * 
     * @param value
     *     allowed object is
     *     {@link ScalarExpressionType_sql2008 }
     *     
     */
    public void setPassThru(ScalarExpressionType_sql2008 value) {
        this.passThru = value;
    }

    /**
     * Gets the value of the starJoinInfo property.
     * 
     * @return
     *     possible object is
     *     {@link StarJoinInfoType_sql2008 }
     *     
     */
    public StarJoinInfoType_sql2008 getStarJoinInfo() {
        return starJoinInfo;
    }

    /**
     * Sets the value of the starJoinInfo property.
     * 
     * @param value
     *     allowed object is
     *     {@link StarJoinInfoType_sql2008 }
     *     
     */
    public void setStarJoinInfo(StarJoinInfoType_sql2008 value) {
        this.starJoinInfo = value;
    }

    /**
     * Gets the value of the relOp property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the relOp property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getRelOp().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link RelOpType_sql2008 }
     * 
     * 
     */
    public List<RelOpType_sql2008> getRelOp() {
        if (relOp == null) {
            relOp = new ArrayList<RelOpType_sql2008>();
        }
        return this.relOp;
    }

    /**
     * Gets the value of the manyToMany property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean getManyToMany() {
        return manyToMany;
    }

    /**
     * Sets the value of the manyToMany property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setManyToMany(Boolean value) {
        this.manyToMany = value;
    }

}
