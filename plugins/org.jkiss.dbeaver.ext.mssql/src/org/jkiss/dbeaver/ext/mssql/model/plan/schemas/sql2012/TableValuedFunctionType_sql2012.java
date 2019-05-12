
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2012;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for TableValuedFunctionType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="TableValuedFunctionType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RelOpBaseType">
 *       &lt;sequence>
 *         &lt;element name="Object" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ObjectType" minOccurs="0"/>
 *         &lt;element name="Predicate" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ScalarExpressionType" minOccurs="0"/>
 *         &lt;element name="ParameterList" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ScalarExpressionListType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TableValuedFunctionType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "object",
    "predicate",
    "parameterList"
})
public class TableValuedFunctionType_sql2012
    extends RelOpBaseType_sql2012
{

    @XmlElement(name = "Object", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ObjectType_sql2012 object;
    @XmlElement(name = "Predicate", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ScalarExpressionType_sql2012 predicate;
    @XmlElement(name = "ParameterList", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ScalarExpressionListType_sql2012 parameterList;

    /**
     * Gets the value of the object property.
     * 
     * @return
     *     possible object is
     *     {@link ObjectType_sql2012 }
     *     
     */
    public ObjectType_sql2012 getObject() {
        return object;
    }

    /**
     * Sets the value of the object property.
     * 
     * @param value
     *     allowed object is
     *     {@link ObjectType_sql2012 }
     *     
     */
    public void setObject(ObjectType_sql2012 value) {
        this.object = value;
    }

    /**
     * Gets the value of the predicate property.
     * 
     * @return
     *     possible object is
     *     {@link ScalarExpressionType_sql2012 }
     *     
     */
    public ScalarExpressionType_sql2012 getPredicate() {
        return predicate;
    }

    /**
     * Sets the value of the predicate property.
     * 
     * @param value
     *     allowed object is
     *     {@link ScalarExpressionType_sql2012 }
     *     
     */
    public void setPredicate(ScalarExpressionType_sql2012 value) {
        this.predicate = value;
    }

    /**
     * Gets the value of the parameterList property.
     * 
     * @return
     *     possible object is
     *     {@link ScalarExpressionListType_sql2012 }
     *     
     */
    public ScalarExpressionListType_sql2012 getParameterList() {
        return parameterList;
    }

    /**
     * Sets the value of the parameterList property.
     * 
     * @param value
     *     allowed object is
     *     {@link ScalarExpressionListType_sql2012 }
     *     
     */
    public void setParameterList(ScalarExpressionListType_sql2012 value) {
        this.parameterList = value;
    }

}
