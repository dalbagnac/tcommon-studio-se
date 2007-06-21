/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package org.talend.core.model.properties;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>License</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link org.talend.core.model.properties.License#getLicense <em>License</em>}</li>
 * </ul>
 * </p>
 *
 * @see org.talend.core.model.properties.PropertiesPackage#getLicense()
 * @model
 * @generated
 */
public interface License extends EObject {
    /**
     * Returns the value of the '<em><b>License</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of the '<em>License</em>' attribute isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     * @return the value of the '<em>License</em>' attribute.
     * @see #setLicense(byte[])
     * @see org.talend.core.model.properties.PropertiesPackage#getLicense_License()
     * @model
     * @generated
     */
    byte[] getLicense();

    /**
     * Sets the value of the '{@link org.talend.core.model.properties.License#getLicense <em>License</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @param value the new value of the '<em>License</em>' attribute.
     * @see #getLicense()
     * @generated
     */
    void setLicense(byte[] value);

} // License