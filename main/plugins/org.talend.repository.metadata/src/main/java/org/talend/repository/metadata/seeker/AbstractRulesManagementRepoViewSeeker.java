// ============================================================================
//
// Copyright (C) 2006-2019 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.repository.metadata.seeker;

import java.util.List;

import org.talend.core.model.repository.ERepositoryObjectType;

/**
 * DOC ggu class global comment. Detailled comment <br/>
 *
 * $Id: talend.epf 55206 2011-02-15 17:32:14Z mhirt $
 *
 */
public abstract class AbstractRulesManagementRepoViewSeeker extends AbstractMetadataRepoViewSeeker {

    /*
     * (non-Javadoc)
     *
     * @see org.talend.core.repository.seeker.AbstractRepoViewSeeker#getPreExpandTypes()
     */
    @Override
    protected List<ERepositoryObjectType> getPreExpandTypes() {
        List<ERepositoryObjectType> preExpandTypes = super.getPreExpandTypes();
        preExpandTypes.add(ERepositoryObjectType.METADATA_RULES_MANAGEMENT);
        return preExpandTypes;
    }
}
