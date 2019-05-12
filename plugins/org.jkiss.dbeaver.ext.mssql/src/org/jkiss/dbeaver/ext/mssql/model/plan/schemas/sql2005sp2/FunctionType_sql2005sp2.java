
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2005sp2;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * Shows the plan for the UDF or stored procedure
 * 	
 * 
 * <p>Java class for FunctionType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="FunctionType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Statements" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}StmtBlockType"/>
 *       &lt;/sequence>
 *       &lt;attribute name="ProcName" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "FunctionType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "statements"
})
public class FunctionType_sql2005sp2 {

    @XmlElement(name = "Statements", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
    protected StmtBlockType_sql2005sp2 statements;
    @XmlAttribute(name = "ProcName")
    protected String procName;

    /**
     * Gets the value of the statements property.
     * 
     * @return
     *     possible object is
     *     {@link StmtBlockType_sql2005sp2 }
     *     
     */
    public StmtBlockType_sql2005sp2 getStatements() {
        return statements;
    }

    /**
     * Sets the value of the statements property.
     * 
     * @param value
     *     allowed object is
     *     {@link StmtBlockType_sql2005sp2 }
     *     
     */
    public void setStatements(StmtBlockType_sql2005sp2 value) {
        this.statements = value;
    }

    /**
     * Gets the value of the procName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getProcName() {
        return procName;
    }

    /**
     * Sets the value of the procName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setProcName(String value) {
        this.procName = value;
    }

}
