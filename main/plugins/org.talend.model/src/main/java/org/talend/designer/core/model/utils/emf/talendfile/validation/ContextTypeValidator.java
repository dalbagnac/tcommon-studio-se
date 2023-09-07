/**
 *
 * $Id$
 */
package org.talend.designer.core.model.utils.emf.talendfile.validation;

import org.eclipse.emf.common.util.EList;

/**
 * A sample validator interface for {@link org.talend.designer.core.model.utils.emf.talendfile.ContextType}.
 * This doesn't really do anything, and it's not a real EMF artifact.
 * It was generated by the org.eclipse.emf.examples.generator.validator plug-in to illustrate how EMF's code generator can be extended.
 * This can be disabled with -vmargs -Dorg.eclipse.emf.examples.generator.validator=false.
 */
public interface ContextTypeValidator {
    boolean validate();

    boolean validateContextParameter(EList value);
    boolean validateConfirmationNeeded(boolean value);
    boolean validateHide(boolean value);

    boolean validateName(String value);
}
