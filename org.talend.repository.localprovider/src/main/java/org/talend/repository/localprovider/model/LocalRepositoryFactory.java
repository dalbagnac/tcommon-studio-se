// ============================================================================
//
// Copyright (C) 2006-2011 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.repository.localprovider.model;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.EMap;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature.Setting;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.jface.resource.ImageDescriptor;
import org.talend.commons.exception.BusinessException;
import org.talend.commons.exception.LoginException;
import org.talend.commons.exception.PersistenceException;
import org.talend.commons.exception.ResourceNotFoundException;
import org.talend.commons.ui.runtime.exception.ExceptionHandler;
import org.talend.commons.ui.runtime.image.ECoreImage;
import org.talend.commons.ui.runtime.image.ImageProvider;
import org.talend.commons.ui.runtime.image.ImageUtils;
import org.talend.commons.utils.VersionUtils;
import org.talend.commons.utils.data.container.Container;
import org.talend.commons.utils.data.container.RootContainer;
import org.talend.commons.utils.io.FilesUtils;
import org.talend.commons.utils.workbench.resources.ResourceUtils;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.language.ECodeLanguage;
import org.talend.core.model.general.Project;
import org.talend.core.model.general.TalendNature;
import org.talend.core.model.metadata.MetadataManager;
import org.talend.core.model.metadata.builder.connection.Connection;
import org.talend.core.model.metadata.builder.connection.ConnectionPackage;
import org.talend.core.model.metadata.builder.connection.MetadataTable;
import org.talend.core.model.metadata.builder.connection.QueriesConnection;
import org.talend.core.model.metadata.builder.connection.Query;
import org.talend.core.model.metadata.builder.connection.SAPConnection;
import org.talend.core.model.metadata.builder.connection.SAPFunctionUnit;
import org.talend.core.model.metadata.builder.connection.SAPIDocUnit;
import org.talend.core.model.migration.IMigrationToolService;
import org.talend.core.model.properties.BRMSConnectionItem;
import org.talend.core.model.properties.BusinessProcessItem;
import org.talend.core.model.properties.ByteArray;
import org.talend.core.model.properties.ConnectionItem;
import org.talend.core.model.properties.ContextItem;
import org.talend.core.model.properties.EDIFACTConnectionItem;
import org.talend.core.model.properties.EbcdicConnectionItem;
import org.talend.core.model.properties.FTPConnectionItem;
import org.talend.core.model.properties.FileItem;
import org.talend.core.model.properties.FolderItem;
import org.talend.core.model.properties.HL7ConnectionItem;
import org.talend.core.model.properties.HeaderFooterConnectionItem;
import org.talend.core.model.properties.Item;
import org.talend.core.model.properties.ItemState;
import org.talend.core.model.properties.JobDocumentationItem;
import org.talend.core.model.properties.JobletDocumentationItem;
import org.talend.core.model.properties.JobletProcessItem;
import org.talend.core.model.properties.LinkDocumentationItem;
import org.talend.core.model.properties.LinkRulesItem;
import org.talend.core.model.properties.ProcessItem;
import org.talend.core.model.properties.ProjectReference;
import org.talend.core.model.properties.PropertiesFactory;
import org.talend.core.model.properties.PropertiesPackage;
import org.talend.core.model.properties.Property;
import org.talend.core.model.properties.ReferenceFileItem;
import org.talend.core.model.properties.RoutineItem;
import org.talend.core.model.properties.RulesItem;
import org.talend.core.model.properties.SQLPatternItem;
import org.talend.core.model.properties.SVGBusinessProcessItem;
import org.talend.core.model.properties.SnippetItem;
import org.talend.core.model.properties.SpagoBiServer;
import org.talend.core.model.properties.Status;
import org.talend.core.model.properties.TDQItem;
import org.talend.core.model.properties.User;
import org.talend.core.model.properties.ValidationRulesConnectionItem;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.model.repository.Folder;
import org.talend.core.model.repository.IRepositoryContentHandler;
import org.talend.core.model.repository.IRepositoryViewObject;
import org.talend.core.model.repository.RepositoryContentManager;
import org.talend.core.model.repository.RepositoryObject;
import org.talend.core.model.repository.RepositoryViewObject;
import org.talend.core.repository.constants.FileConstants;
import org.talend.core.repository.model.AbstractEMFRepositoryFactory;
import org.talend.core.repository.model.FolderHelper;
import org.talend.core.repository.model.ILocalRepositoryFactory;
import org.talend.core.repository.model.ProxyRepositoryFactory;
import org.talend.core.repository.model.ResourceModelUtils;
import org.talend.core.repository.model.VersionList;
import org.talend.core.repository.utils.AbstractResourceChangesService;
import org.talend.core.repository.utils.RoutineUtils;
import org.talend.core.repository.utils.TDQServiceRegister;
import org.talend.core.repository.utils.URIHelper;
import org.talend.core.repository.utils.XmiResourceManager;
import org.talend.core.service.ICorePerlService;
import org.talend.cwm.helper.ConnectionHelper;
import org.talend.cwm.helper.SubItemHelper;
import org.talend.repository.ProjectManager;
import org.talend.repository.RepositoryWorkUnit;
import org.talend.repository.localprovider.exceptions.IncorrectFileException;
import org.talend.repository.localprovider.i18n.Messages;
import org.talend.repository.model.ERepositoryStatus;
import org.talend.repository.model.IProxyRepositoryFactory;
import org.talend.repository.model.RepositoryConstants;
import orgomg.cwm.foundation.businessinformation.BusinessinformationPackage;

/**
 * DOC smallet class global comment. Detailled comment <br/>
 * 
 * $Id$ $Id: RepositoryFactory.java,v 1.55 2006/08/23
 * 14:30:39 tguiu Exp $
 * 
 */
public class LocalRepositoryFactory extends AbstractEMFRepositoryFactory implements ILocalRepositoryFactory {

    private static final String BIN = "bin"; //$NON-NLS-1$

    private static Logger log = Logger.getLogger(LocalRepositoryFactory.class);

    private static LocalRepositoryFactory singleton = null;

    private boolean copyScreenshotFlag = false;

    public LocalRepositoryFactory() {
        super();
        singleton = this;
    }

    public static LocalRepositoryFactory getInstance() {
        if (singleton == null) {
            singleton = new LocalRepositoryFactory();
        }
        return singleton;
    }

    /**
     * DOC smallet Comment method "getObjectFromFolder".
     * 
     * @param type - the type of object to search
     * @param onlyLastVersion specify <i>true</i> if only the last version of an object must be returned, false for all
     * version
     * @param project - the project to searched in
     * 
     * @param <T> - DOC smallet
     * @return a structure of all object of type in the project
     * @throws PersistenceException if project, folder or resource cannot be found
     */
    protected <K, T> RootContainer<K, T> getObjectFromFolder(Project project, ERepositoryObjectType type,
            boolean onlyLastVersion, boolean... options) throws PersistenceException {
        long currentTime = System.currentTimeMillis();

        RootContainer<K, T> toReturn = new RootContainer<K, T>();

        IProject fsProject = ResourceModelUtils.getProject(project);
        // added for bug 18318
        if (fsProject == null) {
            return null;
        }
        IFolder objectFolder = null;
        try {
            objectFolder = ResourceUtils.getFolder(fsProject, ERepositoryObjectType.getFolderName(type), true);
        } catch (ResourceNotFoundException rex) {
            // log.info(rex.getMessage());
            return new RootContainer<K, T>(); // return empty
        }
        // projectModified = false;
        addFolderMembers(project, type, toReturn, objectFolder, onlyLastVersion, options);

        // if (projectModified) {
        // saveProject(project);
        // }

        String arg1 = toReturn.absoluteSize() + ""; //$NON-NLS-1$
        String arg2 = (System.currentTimeMillis() - currentTime) / 1000 + ""; //$NON-NLS-1$

        log.trace(Messages.getString("LocalRepositoryFactory.logRetrievingFiles", new String[] { arg1, arg2 })); //$NON-NLS-1$

        return toReturn;
    }

    protected <K, T> RootContainer<K, T> getObjectFromFolder(Project project, ERepositoryObjectType type, String folderName,
            boolean onlyLastVersion, boolean... options) throws PersistenceException {
        long currentTime = System.currentTimeMillis();
        // if (type.equals(ERepositoryObjectType.METADATA_CONNECTIONS)) {
        // System.out.println("dfdfdfdfdfdf");
        // }
        RootContainer<K, T> toReturn = new RootContainer<K, T>();

        IProject fsProject = ResourceModelUtils.getProject(project);
        IFolder objectFolder = null;
        try {
            String tempFolderName = ERepositoryObjectType.getFolderName(type);
            if (folderName != null && !"".equals(folderName)) {
                tempFolderName = folderName;
            }
            objectFolder = ResourceUtils.getFolder(fsProject, tempFolderName, true);
        } catch (ResourceNotFoundException rex) {
            // log.info(rex.getMessage());
            return new RootContainer<K, T>(); // return empty
        }
        // projectModified = false;
        addFolderMembers(project, type, toReturn, objectFolder, onlyLastVersion, options);

        // if (projectModified) {
        // saveProject(project);
        // }

        String arg1 = toReturn.absoluteSize() + ""; //$NON-NLS-1$
        String arg2 = (System.currentTimeMillis() - currentTime) / 1000 + ""; //$NON-NLS-1$

        log.trace(Messages.getString("LocalRepositoryFactory.logRetrievingFiles", new String[] { arg1, arg2 })); //$NON-NLS-1$

        return toReturn;
    }

    /**
     * 
     * DOC smallet Comment method "addFolderMembers".
     * 
     * @param <T> - DOC smallet
     * @param type - DOC smallet
     * @param toReturn - DOC smallet
     * @param objectFolder - DOC smallet
     * @param onlyLastVersion specify <i>true</i> if only the last version of an object must be returned, false for all
     * version
     * @throws PersistenceException - DOC smallet
     */
    protected <K, T> void addFolderMembers(Project project, ERepositoryObjectType type, Container<K, T> toReturn,
            Object objectFolder, boolean onlyLastVersion, boolean... options) throws PersistenceException {
        FolderHelper folderHelper = getFolderHelper(project.getEmfProject());
        FolderItem currentFolderItem = null;
        IFolder physicalFolder;
        if (objectFolder instanceof IFolder) {
            if (!((IFolder) objectFolder).getName().equals(BIN)) {
                currentFolderItem = folderHelper.getFolder(((IFolder) objectFolder).getProjectRelativePath());
                if (currentFolderItem == null) {
                    // create folder
                    currentFolderItem = folderHelper.createFolder(((IFolder) objectFolder).getProjectRelativePath()
                            .toPortableString());
                }
            }
            physicalFolder = (IFolder) objectFolder;
        } else {
            currentFolderItem = (FolderItem) objectFolder;
            physicalFolder = getPhysicalProject(project).getFolder(folderHelper.getFullFolderPath(currentFolderItem));
        }
        List<String> propertyFounds = new ArrayList<String>();
        List<String> folderNamesFounds = new ArrayList<String>();
        List<Item> invalidItems = new ArrayList<Item>();
        if (currentFolderItem != null) { // only for bin directory
            for (Item curItem : (List<Item>) currentFolderItem.getChildren()) {
                Property property = curItem.getProperty();
                if (property != null) {
                    if (curItem instanceof FolderItem) {
                        FolderItem subFolder = (FolderItem) curItem;
                        Container<K, T> cont = toReturn.addSubContainer(subFolder.getProperty().getLabel());
                        subFolder.setParent(currentFolderItem);

                        cont.setProperty(property);
                        cont.setId(property.getId());
                        addFolderMembers(project, type, cont, curItem, onlyLastVersion, options);
                        folderNamesFounds.add(curItem.getProperty().getLabel());
                    } else {
                        if (property.eResource() != null) {
                            property.getItem().setParent(currentFolderItem);
                            IRepositoryViewObject currentObject;
                            if (options.length > 0 && options[0] == true) {
                                // called from repository view
                                currentObject = new RepositoryViewObject(property);
                            } else {
                                currentObject = new RepositoryObject(property);
                            }
                            propertyFounds.add(property.eResource().getURI().lastSegment());
                            addItemToContainer(toReturn, currentObject, onlyLastVersion);

                            addToHistory(property.getId(), type, property.getItem().getState().getPath());
                        } else {
                            invalidItems.add(curItem);
                        }
                    }
                } else {
                    invalidItems.add(curItem);
                }
            }
            for (Item item : invalidItems) {
                item.setParent(null);
            }
            currentFolderItem.getChildren().removeAll(invalidItems);
        }

        // check the items from physical folder, in case any item has been added (or deleted) manually (or from copy to
        // branch)
        if (physicalFolder.exists()) {
            List<String> physicalPropertyFounds = new ArrayList<String>();
            List<String> physicalDirectoryFounds = new ArrayList<String>();
            for (IResource current : ResourceUtils.getMembers(physicalFolder)) {
                if (current instanceof IFile) {
                    try {
                        String fileName = ((IFile) current).getName();
                        IRepositoryViewObject currentObject = null;
                        physicalPropertyFounds.add(fileName);

                        if (xmiResourceManager.isPropertyFile((IFile) current) && !propertyFounds.contains(fileName)) {
                            Property property = null;
                            try {
                                property = xmiResourceManager.loadProperty(current);
                            } catch (RuntimeException e) {
                                // property will be null
                                ExceptionHandler.process(e);
                            }
                            if (property != null) {
                                if (currentFolderItem != null && !currentFolderItem.getChildren().contains(property.getItem())) {
                                    currentFolderItem.getChildren().add(property.getItem());
                                    property.getItem().setParent(currentFolderItem);
                                }
                                if (options.length > 0 && options[0] == true) {
                                    // called from repository view
                                    currentObject = new RepositoryViewObject(property);
                                } else {
                                    currentObject = new RepositoryObject(property);
                                }
                            } else {
                                log.error(Messages.getString("LocalRepositoryFactory.CannotLoadProperty") + current); //$NON-NLS-1$
                            }
                            addItemToContainer(toReturn, currentObject, onlyLastVersion);
                        }
                    } catch (IncorrectFileException e) {
                        ExceptionHandler.process(e);
                    } catch (PersistenceException e) {
                        ExceptionHandler.process(e);
                    }
                } else if (current instanceof IFolder) {
                    if (!((IFolder) current).getName().startsWith(".") && !FilesUtils.isSVNFolder(current)) {
                        physicalDirectoryFounds.add(((IFolder) current).getName());
                        if (!folderNamesFounds.contains(((IFolder) current).getName())) {
                            Container<K, T> cont = toReturn.addSubContainer(current.getName());
                            FolderItem folder = folderHelper.getFolder(current.getProjectRelativePath());

                            Property property = null;
                            if (folder == null) {
                                folder = folderHelper.createFolder(current.getProjectRelativePath().toString());
                            }
                            property = folder.getProperty();
                            folder.setParent(currentFolderItem);

                            cont.setProperty(property);
                            cont.setId(property.getId());
                            addFolderMembers(project, type, cont, (IFolder) current, onlyLastVersion, options);
                        }
                        if (current.getName().equals(BIN)) {
                            // if empty directory, just delete it
                            IResource[] binFolder = ResourceUtils.getMembers((IFolder) current);
                            if (binFolder.length == 0 || (binFolder.length == 1 && FilesUtils.isSVNFolder(binFolder[0]))) {
                                try {
                                    ((IFolder) current).delete(true, null);
                                } catch (CoreException e) {
                                    // not catched, not important if can delete or not
                                }
                            }

                        }
                    }
                }
            }

            // in case any property has been deleted manually, should delete from the emf model
            if (currentFolderItem != null) { // test if null only for bin directory
                List<Item> itemsDeleted = new ArrayList<Item>();
                for (Item curItem : (List<Item>) currentFolderItem.getChildren()) {
                    if (!(curItem instanceof FolderItem)) {
                        String name;
                        if (curItem.eResource() != null) {
                            name = curItem.eResource().getURI().lastSegment();
                        } else {
                            name = curItem.getProperty().getLabel() + "_" + curItem.getProperty().getVersion() + "."
                                    + FileConstants.PROPERTIES_EXTENSION;
                        }
                        if (!physicalPropertyFounds.contains(name)) {
                            itemsDeleted.add(curItem);
                        }
                    } else {
                        if (!physicalDirectoryFounds.contains(curItem.getProperty().getLabel())) {
                            itemsDeleted.add(curItem);
                        }
                    }
                }
                for (Item item : itemsDeleted) {
                    item.setParent(null);

                    // In case one item has been physically deleted, but is still in the emf model.
                    // Make sure this item is not returned.

                    Iterator<IRepositoryViewObject> it = (Iterator<IRepositoryViewObject>) toReturn.getMembers().iterator();
                    while (it.hasNext()) {
                        IRepositoryViewObject object = it.next();
                        if (object.getLabel().equals(item.getProperty().getLabel())
                                && object.getId().equals(item.getProperty().getId())
                                && object.getVersion().equals(item.getProperty().getVersion())) {
                            it.remove();
                        }
                    }

                }
                currentFolderItem.getChildren().removeAll(itemsDeleted);
            }
        }
    }

    private <K, T> void addItemToContainer(Container<K, T> container, IRepositoryViewObject objectToAdd, boolean onlyLastVersion)
            throws PersistenceException {
        if (objectToAdd != null) {
            K key;
            String currentVersion = null;
            try {
                currentVersion = objectToAdd.getVersion();
            } catch (Exception e) {
                // e.printStackTrace();
                ExceptionHandler.process(e);
            }
            if (onlyLastVersion) {
                key = (K) objectToAdd.getId();
            } else {
                key = (K) new MultiKey(objectToAdd.getId(), currentVersion);
            }

            try {
                if (onlyLastVersion) {
                    // Version :
                    T old = container.getMember(key);

                    if (old == null) {
                        container.addMember(key, (T) objectToAdd);
                    } else {
                        String oldVersion = ((IRepositoryViewObject) old).getVersion();
                        if (VersionUtils.compareTo(oldVersion, currentVersion) < 0) {
                            container.addMember(key, (T) objectToAdd);
                        }
                    }
                } else {
                    container.addMember(key, (T) objectToAdd);
                }
            } catch (BusinessException e) {
                throw new PersistenceException(e);
            }
        }

    }

    public List<IRepositoryViewObject> getAll(Project project, ERepositoryObjectType type, boolean withDeleted,
            boolean allVersions) throws PersistenceException {
        IFolder folder = null;
        try {
            if (type.hasFolder()) {
                folder = LocalResourceModelUtils.getFolder(project, type);
            } else {
                return Collections.emptyList();
            }
        } catch (ResourceNotFoundException e) {
            return Collections.emptyList();
        }
        return convert(getSerializableFromFolder(project, folder, null, type, allVersions, true, withDeleted, false));
    }

    /**
     * 
     * Get all object in a folder recursively.
     * 
     * @param folder - the folder to search in
     * @param id - the id of the object searched. Specify <code>null</code> to get all objects.
     * @param type - the type searched
     * @param allVersion - <code>true</code> if all version of each object must be return or <code>false</code> if only
     * the most recent version
     * @return a list (may be empty) of objects found
     * @throws PersistenceException
     */
    protected List<IRepositoryViewObject> getSerializableFromFolder(Project project, Object folder, String id,
            ERepositoryObjectType type, boolean allVersion, boolean searchInChildren, boolean withDeleted,
            boolean avoidSaveProject, boolean... recursiveCall) throws PersistenceException {
        List<IRepositoryViewObject> toReturn = new VersionList(allVersion);
        FolderHelper folderHelper = getFolderHelper(project.getEmfProject());

        if (folder != null) {
            IFolder physicalFolder;
            FolderItem currentFolderItem = null;
            if (folder instanceof IFolder) {
                if (!((IFolder) folder).getName().equals(BIN)) {
                    currentFolderItem = folderHelper.getFolder(((IFolder) folder).getProjectRelativePath());
                    if (((IFolder) folder).getLocation().toPortableString().contains(BIN)) {
                        // don't do anything for bin directory
                    } else if (currentFolderItem == null) {
                        // create folder
                        currentFolderItem = folderHelper.createFolder(((IFolder) folder).getProjectRelativePath()
                                .toPortableString());
                    }
                }
                physicalFolder = (IFolder) folder;
            } else {
                currentFolderItem = (FolderItem) folder;
                physicalFolder = getPhysicalProject(project).getFolder(folderHelper.getFullFolderPath(currentFolderItem));
            }
            List<String> propertyFounds = new ArrayList<String>();
            List<String> folderNamesFounds = new ArrayList<String>();
            List<Item> toRemoveFromFolder = new ArrayList<Item>();
            if (currentFolderItem != null) {
                for (Item curItem : (List<Item>) currentFolderItem.getChildren()) {
                    Property property = curItem.getProperty();
                    if (property != null) {
                        if (curItem instanceof FolderItem && searchInChildren) {
                            folderNamesFounds.add(curItem.getProperty().getLabel());
                            toReturn.addAll(getSerializableFromFolder(project, curItem, id, type, allVersion, true, withDeleted,
                                    avoidSaveProject, true));
                        } else if (!(curItem instanceof FolderItem)) {
                            if (property.eResource() != null) {
                                if (id == null || property.getId().equals(id)) {
                                    if (withDeleted || !property.getItem().getState().isDeleted()) {
                                        toReturn.add(new RepositoryObject(property));
                                    }
                                }
                                propertyFounds.add(property.eResource().getURI().lastSegment());
                                property.getItem().setParent(currentFolderItem);
                                addToHistory(id, type, property.getItem().getState().getPath());
                            } else {
                                toRemoveFromFolder.add(curItem);
                            }
                        }
                    } else {
                        toRemoveFromFolder.add(curItem);
                    }
                }
                if (toRemoveFromFolder.size() != 0) {
                    currentFolderItem.getChildren().removeAll(toRemoveFromFolder);
                }
            }
            // check the items from physical folder, in case any item has been added (or deleted) manually (or from copy
            // to branch)
            if (physicalFolder.exists()) {
                List<String> physicalPropertyFounds = new ArrayList<String>();
                List<String> physicalDirectoryFounds = new ArrayList<String>();
                for (IResource current : ResourceUtils.getMembers(physicalFolder)) {
                    if (current instanceof IFile) {
                        if (xmiResourceManager.isPropertyFile((IFile) current)) {
                            String fileName = ((IFile) current).getName();
                            physicalPropertyFounds.add(fileName);
                            if (!propertyFounds.contains(fileName)) {
                                Property property = null;
                                try {
                                    property = xmiResourceManager.loadProperty(current);
                                } catch (RuntimeException e) {
                                    // property will be null
                                    ExceptionHandler.process(e);
                                }
                                if (property != null) {
                                    addToHistory(property.getId(), type, property.getItem().getState().getPath());
                                    if (id == null || property.getId().equals(id)) {
                                        if (withDeleted || !property.getItem().getState().isDeleted()) {
                                            toReturn.add(new RepositoryObject(property));
                                        }
                                    }
                                    if (currentFolderItem != null
                                            && !currentFolderItem.getChildren().contains(property.getItem())) {
                                        currentFolderItem.getChildren().add(property.getItem());
                                        property.getItem().setParent(currentFolderItem);
                                    }
                                } else {
                                    log.error(Messages.getString("LocalRepositoryFactory.CannotLoadProperty") + current); //$NON-NLS-1$
                                }
                            }
                        }
                    } else if (current instanceof IFolder) { // &&
                        if (!((IFolder) current).getName().startsWith(".") && !FilesUtils.isSVNFolder(current)
                                && searchInChildren) {
                            String fileName = ((IFolder) current).getName();
                            physicalDirectoryFounds.add(fileName);
                            if (!folderNamesFounds.contains(((IFolder) current).getName())) {
                                FolderItem parentFolder = folderHelper.getFolder(current.getProjectRelativePath());

                                if (parentFolder == null) {
                                    parentFolder = folderHelper.createFolder(current.getProjectRelativePath().toString());
                                }
                                parentFolder.setParent(currentFolderItem);
                                toReturn.addAll(getSerializableFromFolder(project, (IFolder) current, id, type, allVersion, true,
                                        withDeleted, avoidSaveProject, true));
                            }
                            if (((IFolder) current).getName().equals(BIN)) {

                                // if empty directory, just delete it
                                IResource[] binFolder = ResourceUtils.getMembers((IFolder) current);

                                if (binFolder.length == 0 || (binFolder.length == 1 && FilesUtils.isSVNFolder(binFolder[0]))) {
                                    try {
                                        ((IFolder) current).delete(true, null);
                                    } catch (CoreException e) {
                                        // not catched, not important if can delete or not
                                    }
                                }
                            }
                        }
                    }
                }

                if (currentFolderItem != null) { // for bin directory
                    // in case any property has been deleted manually, should delete from the emf model
                    List<Item> itemsDeleted = new ArrayList<Item>();
                    for (Item curItem : (List<Item>) currentFolderItem.getChildren()) {
                        if (!(curItem instanceof FolderItem)) {
                            String name;
                            if (curItem.eResource() != null) {
                                name = curItem.eResource().getURI().lastSegment();
                            } else {
                                name = curItem.getProperty().getLabel() + "_" + curItem.getProperty().getVersion() + "."
                                        + FileConstants.PROPERTIES_EXTENSION;
                            }
                            if (!physicalPropertyFounds.contains(name)) {
                                itemsDeleted.add(curItem);
                            }
                        } else if (searchInChildren && ProxyRepositoryFactory.getInstance().isFullLogonFinished()) {
                            if (!physicalDirectoryFounds.contains(curItem.getProperty().getLabel())) {
                                itemsDeleted.add(curItem);
                            }
                        }
                    }
                    for (Item item : itemsDeleted) {
                        item.setParent(null);

                        // In case one item has been physically deleted, but is still in the emf model.
                        // Make sure this item is not returned.

                        Iterator<IRepositoryViewObject> it = toReturn.iterator();
                        while (it.hasNext()) {
                            IRepositoryViewObject object = it.next();
                            if (object.getLabel().equals(item.getProperty().getLabel())
                                    && object.getId().equals(item.getProperty().getId())
                                    && object.getVersion().equals(item.getProperty().getVersion())) {
                                it.remove();
                            }
                        }
                    }
                    currentFolderItem.getChildren().removeAll(itemsDeleted);
                }
            }
        }
        return toReturn;
    }

    static {
        // /PTODO tgu quick fix for registering the emf package. needed to make
        // the extention point work
        ConnectionPackage.eINSTANCE.getClass();
    }

    public Project createProject(Project projectInfor) throws PersistenceException {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

        String technicalLabel = Project.createTechnicalName(projectInfor.getLabel());
        IProject prj = root.getProject(technicalLabel);

        final IWorkspace workspace = ResourcesPlugin.getWorkspace();

        try {
            IProjectDescription desc = null;
            if (prj.exists()) {
                desc = prj.getDescription();
            } else {
                desc = workspace.newProjectDescription(projectInfor.getLabel());
            }
            desc.setNatureIds(new String[] { TalendNature.ID });
            desc.setComment(projectInfor.getDescription());

            if (!prj.exists()) {
                prj.create(desc, null);
            }
            prj.open(IResource.BACKGROUND_REFRESH, null);
            prj.setDefaultCharset("UTF-8", null);
        } catch (CoreException e) {
            throw new PersistenceException(e);
        }

        Project project = new Project();
        // Fill project object
        project.setLabel(projectInfor.getLabel());
        project.setDescription(projectInfor.getDescription());
        project.setLanguage(projectInfor.getLanguage());
        project.setAuthor(projectInfor.getAuthor());
        project.setLocal(true);
        project.setTechnicalLabel(technicalLabel);

        saveProject(prj, project);

        return project;
    }

    public void saveProject(Project project) throws PersistenceException {
        for (EReference reference : project.getEmfProject().eClass().getEAllReferences()) {
            if (reference.getName().equals("folders")) {
                if (!reference.isTransient()) {
                    reference.setTransient(true);
                }
            }
        }
        Resource projectResource = project.getEmfProject().eResource();
        Collection<FolderItem> folders = EcoreUtil.getObjectsByType(projectResource.getContents(),
                PropertiesPackage.eINSTANCE.getFolderItem());
        if (!folders.isEmpty()) {
            projectResource.getContents().removeAll(folders);
        }
        Collection<ItemState> itemStates = EcoreUtil.getObjectsByType(projectResource.getContents(),
                PropertiesPackage.eINSTANCE.getItemState());
        if (!itemStates.isEmpty()) {
            projectResource.getContents().removeAll(itemStates);
        }
        Collection<Item> items = EcoreUtil.getObjectsByType(projectResource.getContents(), PropertiesPackage.eINSTANCE.getItem());
        if (!items.isEmpty()) {
            projectResource.getContents().removeAll(items);
        }
        Collection<Property> properties = EcoreUtil.getObjectsByType(projectResource.getContents(),
                PropertiesPackage.eINSTANCE.getProperty());
        if (!properties.isEmpty()) {
            projectResource.getContents().removeAll(properties);
        }

        if (projectResource == null) {
            if (project.getEmfProject() != null && project.getEmfProject().eIsProxy()) {
                ResourceSet resourceSet = xmiResourceManager.resourceSet;
                project.setEmfProject((org.talend.core.model.properties.Project) EcoreUtil.resolve(project.getEmfProject(),
                        resourceSet));
                projectResource = project.getEmfProject().eResource();
            }
        }
        xmiResourceManager.saveResource(projectResource);
    }

    /**
     * DOC qwei Comment method "saveProjectf".
     * 
     * @param author
     * @param prj
     * @param project
     * @throws PersistenceException
     */
    private void saveProject(IProject prj, Project project) throws PersistenceException {
        Resource projectResource = xmiResourceManager.createProjectResource(prj);
        projectResource.getContents().add(project.getEmfProject());
        projectResource.getContents().add(project.getAuthor());
        projectResource.getContents().add(project.getExchangeUser());
        xmiResourceManager.saveResource(projectResource);
    }

    /**
     * DOC smallet Comment method "synchronizeRoutines".
     * 
     * @throws PersistenceException
     */
    public void synchronizeRoutines(IProject prj) throws PersistenceException {
        if (prj == null) {
            Project project = getRepositoryContext().getProject();
            prj = ResourceModelUtils.getProject(project);
        }

        // Purge old routines :
        // 1. old built-in:
        // IFolder f1 = ResourceUtils.getFolder(prj, ERepositoryObjectType.getFolderName(ERepositoryObjectType.ROUTINES)
        // + IPath.SEPARATOR + RepositoryConstants.SYSTEM_DIRECTORY, false);
        // ResourceUtils.deleteResource(f1);

        // MOD mzhao 15422: Unable to open the MDM workspace
        try {
            createSystemRoutines();
        } catch (Exception e) {
            log.error(e, e);
        }
    }

    public void synchronizeSqlpatterns(IProject prj) throws PersistenceException {
        if (prj == null) {
            Project project = getRepositoryContext().getProject();
            prj = ResourceModelUtils.getProject(project);
        }

        // Purge old sqlpatterns :
        // 1. old built-in:
        // IFolder sqlpatternRoot = ResourceUtils.getFolder(prj, ERepositoryObjectType
        // .getFolderName(ERepositoryObjectType.SQLPATTERNS), false);
        // clearSystemSqlPatterns(sqlpatternRoot);
        createSystemSQLPatterns();
    }

    /**
     * bqian Comment method "clearSystemSqlPatterns".
     * 
     * @param f1
     */
    private void clearSystemSqlPatterns(IFolder sqlPatternFolder) {
        try {
            for (IResource resource : sqlPatternFolder.members()) {
                if (resource.getType() == IResource.FOLDER) {
                    IFolder category = (IFolder) resource;
                    IFolder sysFolder = (IFolder) category.getFolder(RepositoryConstants.SYSTEM_DIRECTORY);
                    if (sysFolder != null && sysFolder.exists()) {
                        ResourceUtils.emptyFolder(sysFolder);
                    }
                }
            }
        } catch (Exception e) {
            ExceptionHandler.process(e);
        }
    }

    /**
     * DOC smallet Comment method "createFolders".
     * 
     * @param prj
     * @param project
     * @throws PersistenceException
     */
    private void createFolders(IProject prj, org.talend.core.model.properties.Project emfProject) throws PersistenceException {
        FolderHelper folderHelper = getFolderHelper(emfProject);

        // Folder creation :
        for (ERepositoryObjectType type : (ERepositoryObjectType[]) ERepositoryObjectType.values()) {
            if (type.isDQItemType() && !type.isSharedType()) {
                continue;
            }
            try {
                if (type.hasFolder()) {
                    String folderName = ERepositoryObjectType.getFolderName(type);
                    createFolder(prj, folderHelper, folderName);
                }
            } catch (IllegalArgumentException iae) {
                // Some repository object type doesn't need a folder
            }
        }

        // Special folders creation :
        // 1. Temp folder :
        createFolder(prj, folderHelper, RepositoryConstants.TEMP_DIRECTORY);

        // 2. Img folder :
        createFolder(prj, folderHelper, RepositoryConstants.IMG_DIRECTORY);
        createFolder(prj, folderHelper, RepositoryConstants.IMG_DIRECTORY_OF_JOB_OUTLINE);
        createFolder(prj, folderHelper, RepositoryConstants.IMG_DIRECTORY_OF_JOBLET_OUTLINE);

        // 4. Code/routines/Snippets :
        createFolder(prj, folderHelper, "code/routines/system"); //$NON-NLS-1$  
        // 5. Job Disigns/System
        // createFolder(prj, folderHelper, "process/system"); //$NON-NLS-1$

    }

    protected FolderHelper getFolderHelper(org.talend.core.model.properties.Project emfProject) {
        // add this code to select the user directly from the project instead of get the one from
        // getRepositoryContext().getUser().
        // this avoids problems in case of commit with projects with references.
        // (Reference projects will take the user from main project, so create wrong link..)

        // note: same code added for svn repository

        Collection<User> users = EcoreUtil.getObjectsByType(emfProject.eResource().getContents(),
                PropertiesPackage.eINSTANCE.getUser());
        User user = null;
        for (User projectUser : users) {
            user = projectUser;
            if (projectUser.getLogin().equals(getRepositoryContext().getUser().getLogin())) {
                user = projectUser;
                break;
            }
        }

        return LocalFolderHelper.createInstance(emfProject, user);
    }

    /**
     * DOC smallet Comment method "createFolder".
     * 
     * @param prj
     * @param folderHelper
     * @throws PersistenceException
     */
    private void createFolder(IProject prj, FolderHelper folderHelper, String path) throws PersistenceException {
        IFolder folderTemp = ResourceUtils.getFolder(prj, path, false);
        if (!folderTemp.exists()) {
            ResourceUtils.createFolder(folderTemp);
        }
        if (folderHelper.getFolder(new Path(path)) == null) {
            folderHelper.createSystemFolder(new Path(path));
        }
    }

    public Project[] readProjects(boolean local) throws PersistenceException {
        // TODO SML Delete this method when remote is implemented

        IProject[] prjs = ResourceUtils.getProjetWithNature(TalendNature.ID);

        List<Project> toReturn = new ArrayList<Project>();

        Exception ex = null;
        int readSuccess = 0;
        for (int i = 0; i < prjs.length; i++) {
            IProject p = prjs[i];
            try {
                readProject(local, toReturn, p, false);
                readProject(local, toReturn, p, true);
                readSuccess++;
            } catch (Exception e) {
                ExceptionHandler.process(e);
            }

        }
        if (readSuccess == 0 && prjs.length > 0) {
            throw new PersistenceException("Read projects error.", ex); //$NON-NLS-1$
        } else {
            return toReturn.toArray(new Project[toReturn.size()]);
        }
    }

    private void readProject(boolean local, List<Project> toReturn, IProject p, boolean useOldProjectFile) {
        xmiResourceManager.setUseOldProjectFile(useOldProjectFile);
        try {
            if (xmiResourceManager.hasTalendProjectFile(p)) {
                org.talend.core.model.properties.Project emfProject = xmiResourceManager.loadProject(p);
                // if (emfProject.isLocal() == local) {
                Project project = new Project(emfProject);
                if (GlobalServiceRegister.getDefault().isServiceRegistered(ICorePerlService.class)) {
                    toReturn.add(project);
                } else {
                    if (project.getLanguage().equals(ECodeLanguage.JAVA))
                        toReturn.add(project);
                }
                // }
            }
        } catch (PersistenceException e) {
            ExceptionHandler.process(e);
        }

        xmiResourceManager.setUseOldProjectFile(false);
    }

    /**
     * @see org.talend.core.model.repository.factories.IRepositoryFactory#readProject(java.lang.String,
     * java.lang.String, java.lang.String)
     */
    public Project[] readProject() throws PersistenceException {
        return readProjects(true);
    }

    @Override
    public Project[] readProject(boolean unloadResource) throws PersistenceException, BusinessException {
        xmiResourceManager.setAvoidUnloadResource(unloadResource);
        return readProjects(true);
    }

    private void synchronizeFolders(final IProject project, final org.talend.core.model.properties.Project emfProject)
            throws PersistenceException {
        final FolderHelper helper = getFolderHelper(emfProject);
        final Set<IPath> listFolders = helper.listFolders();
        try {
            project.accept(new IResourceVisitor() {

                public boolean visit(IResource resource) throws CoreException {
                    if (resource.getType() == IResource.FOLDER) {
                        IPath path = resource.getProjectRelativePath();

                        // ignore folders with . (e.g. : .settings) see bug 364
                        if (path.lastSegment().equals(".settings")) { //$NON-NLS-1$
                            return false;
                        }
                        if (path.lastSegment().equals("bin")) { //$NON-NLS-1$
                            return false;
                        }
                        if (path.toPortableString().equals("lib")) { //$NON-NLS-1$
                            return false;
                        }
                        if (path.lastSegment().equals(".svnlog")) { //$NON-NLS-1$
                            return false;
                        }

                        if (!listFolders.remove(path)) {
                            // create emf folder
                            helper.createFolder(path);
                        } else {
                            // add state to existing emf folder
                            FolderItem folder = helper.getFolder(path);
                            if (folder != null && folder.getState() == null) {
                                helper.createItemState(folder);
                            }
                        }
                    }
                    return true;
                }

            });
        } catch (CoreException e) {
            // e.printStackTrace();
            ExceptionHandler.process(e);
        }
        // delete remaining folders
        for (IPath path : listFolders) {
            helper.deleteFolder(path);
        }
        // xmiResourceManager.saveResource(emfProject.eResource());
    }

    public Folder createFolder(Project project, ERepositoryObjectType type, IPath path, String label) throws PersistenceException {
        return createFolder(project, type, path, label, false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.core.extension.IRepositoryFactory#createFolder(org .talend.core.model.temp.Project,
     * org.talend.core.model.repository.EObjectType, org.eclipse.core.runtime.IPath, java.lang.String)
     */
    public Folder createFolder(Project project, ERepositoryObjectType type, IPath path, String label, boolean isImportItem)
            throws PersistenceException {
        if (type == null) {
            throw new IllegalArgumentException(Messages.getString("LocalRepositoryFactory.illegalArgumentException01")); //$NON-NLS-1$
        }
        if (path == null) {
            throw new IllegalArgumentException(Messages.getString("LocalRepositoryFactory.illegalArgumentException02")); //$NON-NLS-1$
        }
        if (label == null || label.length() == 0) {
            throw new IllegalArgumentException(Messages.getString("LocalRepositoryFactory.illegalArgumentException03")); //$NON-NLS-1$
        }

        IProject fsProject = ResourceModelUtils.getProject(project);

        String parentPath = ERepositoryObjectType.getFolderName(type);
        if (!path.isEmpty()) {
            parentPath += IPath.SEPARATOR + path.toString();
        }

        String completePath = parentPath + IPath.SEPARATOR + label;
        // MOD qiongli 2011-1-19.initialize system folder for top/tdq.
        // MOD qiongli 2011-3-28,bug 17588,revert the change.because bug 18883 and its r55695 fix the initialize for
        // top/tdq.
        FolderItem folderItem = getFolderHelper(project.getEmfProject()).createFolder(completePath);
        // Getting the folder :
        IFolder folder = ResourceUtils.getFolder(fsProject, completePath, false);
        if (!folder.exists()) {
            ResourceUtils.createFolder(folder);
        }

        return new Folder(folderItem.getProperty(), type);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IRepositoryFactory#isValid(org.talend.core .model.general.Project,
     * org.talend.core.model.repository.ERepositoryObjectType, org.eclipse.core.runtime.IPath, java.lang.String)
     */

    public boolean isPathValid(Project project, ERepositoryObjectType type, IPath path, String label) throws PersistenceException {
        if (path == null) {
            return false;
        }

        if (label.equalsIgnoreCase("bin")) {
            return false;
        } else if (RepositoryConstants.isSystemFolder(label) || RepositoryConstants.isGeneratedFolder(label)
                || RepositoryConstants.isJobsFolder(label) || RepositoryConstants.isJobletsFolder(label)) {
            // can't create the "system" ,"Generated", "Jobs" folder in the
            // root.
            return false;
        } else {
            // TODO SML Delete this ?
            IProject fsProject = ResourceModelUtils.getProject(project);
            String completePath = null;
            if (type.equals(ERepositoryObjectType.TDQ_PATTERN_REGEX)) {
                completePath = ERepositoryObjectType.getFolderName(type) + IPath.SEPARATOR + label.toString();
            } else {
                completePath = ERepositoryObjectType.getFolderName(type) + IPath.SEPARATOR + path.toString();
                completePath = completePath + IPath.SEPARATOR + label;
            }
            // if label is "", don't need to added label into completePath (used
            // for job's documentation and for folder
            // only)
            // if (!label.equals("")) {

            // }

            // Getting the folder :
            IFolder existingFolder = ResourceUtils.getFolder(fsProject, completePath, false);
            return !existingFolder.exists();
        }
    }

    public void deleteFolder(Project project, ERepositoryObjectType type, IPath path) throws PersistenceException {
        deleteFolder(project, type, path, false);
    }

    public void deleteFolder(Project project, ERepositoryObjectType type, IPath path, boolean fromEmptyRecycleBin)
            throws PersistenceException {
        // If the "System", "Generated", "Jobs" folder is created in the root,
        // it can't be deleted .
        if (RepositoryConstants.isSystemFolder(path.toString()) || RepositoryConstants.isGeneratedFolder(path.toString())
                || RepositoryConstants.isJobsFolder(path.toString()) || RepositoryConstants.isJobletsFolder(path.toString())) {
            return;
        }
        IProject fsProject = ResourceModelUtils.getProject(project);

        String completePath = new Path(ERepositoryObjectType.getFolderName(type)).append(path).toString();

        // Getting the folder :
        IFolder folder = ResourceUtils.getFolder(fsProject, completePath, true);
        getFolderHelper(project.getEmfProject()).deleteFolder(completePath);
        if (!fromEmptyRecycleBin) {
            saveProject(project);
        }
        ResourceUtils.deleteResource(folder);
    }

    public void moveFolder(ERepositoryObjectType type, IPath sourcePath, IPath targetPath) throws PersistenceException {
        if (RepositoryConstants.isSystemFolder(sourcePath.toString())
                || RepositoryConstants.isSystemFolder(targetPath.toString())) {
            // The "system" folder wasn't allowed to move
            return;
        }
        Project project = getRepositoryContext().getProject();
        IProject fsProject = ResourceModelUtils.getProject(project);

        String completeOldPath = ERepositoryObjectType.getFolderName(type) + IPath.SEPARATOR + sourcePath.toString();
        String completeNewPath;
        if (targetPath.equals("")) {
            completeNewPath = ERepositoryObjectType.getFolderName(type) + IPath.SEPARATOR + sourcePath.lastSegment();
        } else {
            completeNewPath = ERepositoryObjectType.getFolderName(type) + IPath.SEPARATOR + targetPath.toString()
                    + IPath.SEPARATOR + sourcePath.lastSegment();
        }
        if (completeNewPath.equals(completeOldPath)) {
            // nothing to do
            return;
        }
        // Getting the folder :
        IFolder folder = ResourceUtils.getFolder(fsProject, completeOldPath, false);

        FolderHelper folderHelper = getFolderHelper(getRepositoryContext().getProject().getEmfProject());
        FolderItem emfFolder = folderHelper.getFolder(completeOldPath);
        if (emfFolder == null && (type == ERepositoryObjectType.JOB_DOC || type == ERepositoryObjectType.JOBLET_DOC)) {
            IPath path = new Path(sourcePath.toString());
            ProxyRepositoryFactory.getInstance().createParentFoldersRecursively(project, type, path);
            emfFolder = folderHelper.getFolder(completeOldPath);
        }

        createFolder(getRepositoryContext().getProject(), type, targetPath, emfFolder.getProperty().getLabel());
        FolderItem newFolder = folderHelper.getFolder(completeNewPath);

        Item[] childrens = (Item[]) emfFolder.getChildren().toArray();
        for (int i = 0; i < childrens.length; i++) {
            if (childrens[i] instanceof FolderItem) {
                FolderItem children = (FolderItem) childrens[i];
                moveFolder(type, sourcePath.append(children.getProperty().getLabel()),
                        targetPath.append(emfFolder.getProperty().getLabel()));
            } else {
                emfFolder.getChildren().remove(childrens[i]);
                newFolder.getChildren().add(childrens[i]);

                AbstractResourceChangesService resChangeService = TDQServiceRegister.getInstance().getResourceChangeService(
                        AbstractResourceChangesService.class);
                if (resChangeService != null) {
                    resChangeService.postMove(childrens[i], targetPath.toString() + IPath.SEPARATOR + sourcePath.lastSegment());
                }

                childrens[i].setParent(newFolder);

                // MDO gdbu 2011-9-29 TDQ-3546
                List<Resource> affectedResources = xmiResourceManager.getAffectedResources(childrens[i].getProperty());
                for (Resource resource : affectedResources) {
                    IPath path = getPhysicalProject(project).getFullPath().append(completeNewPath)
                            .append(resource.getURI().lastSegment());
                    // Find cross reference and save them.
                    Map<EObject, Collection<Setting>> find = EcoreUtil.ExternalCrossReferencer.find(resource);
                    List<Resource> needSaves = new ArrayList<Resource>();
                    for (EObject object : find.keySet()) {
                        Resource re = object.eResource();
                        if (re == null) {
                            continue;
                        }
                        EcoreUtil.resolveAll(re);
                        needSaves.add(re);
                    }

                    xmiResourceManager.moveResource(resource, path);

                    if (resChangeService != null) {
                        for (Resource toSave : needSaves) {
                            resChangeService.saveResourceByEMFShared(toSave);
                        }
                    }
                }

                affectedResources = xmiResourceManager.getAffectedResources(childrens[i].getProperty());
                for (Resource resource : affectedResources) {
                    xmiResourceManager.saveResource(resource);
                }
                // ~TDQ-3546
            }
        }

        List<IRepositoryViewObject> serializableFromFolder = getSerializableFromFolder(project, folder, null, type, true, true,
                true, false);
        for (IRepositoryViewObject object : serializableFromFolder) {
            List<Resource> affectedResources = xmiResourceManager.getAffectedResources(object.getProperty());
            for (Resource resource : affectedResources) {
                IPath path = getPhysicalProject(project).getFullPath().append(completeNewPath)
                        .append(resource.getURI().lastSegment());
                xmiResourceManager.moveResource(resource, path);
            }
        }

        deleteFolder(getRepositoryContext().getProject(), type, sourcePath);

        xmiResourceManager.saveResource(getRepositoryContext().getProject().getEmfProject().eResource());
    }

    public void renameFolder(ERepositoryObjectType type, IPath path, String label) throws PersistenceException {
        IProject fsProject = ResourceModelUtils.getProject(getRepositoryContext().getProject());

        String completePath = ERepositoryObjectType.getFolderName(type) + IPath.SEPARATOR + path.toString();
        // Getting the folder :
        IFolder folder = ResourceUtils.getFolder(fsProject, completePath, false);

        // IPath targetPath = new
        // Path(SystemFolderNameFactory.getFolderName(type)).append(path).
        // removeLastSegments(1).append(label);
        IPath targetPath = new Path(label);
        getFolderHelper(getRepositoryContext().getProject().getEmfProject()).renameFolder(completePath, label);
        xmiResourceManager.saveResource(getRepositoryContext().getProject().getEmfProject().eResource());

        if (folder.exists()) {
            ResourceUtils.moveResource(folder, targetPath);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IRepositoryFactory#getProcess2()
     */
    public List<IRepositoryViewObject> getProcess2() throws PersistenceException {
        List<IRepositoryViewObject> toReturn = new VersionList(false);

        IFolder folder = LocalResourceModelUtils.getFolder(getRepositoryContext().getProject(), ERepositoryObjectType.PROCESS);

        for (IResource current : ResourceUtils.getMembers(folder)) {
            if (current instanceof IFile) {
                if (xmiResourceManager.isPropertyFile((IFile) current)) {
                    Property property = xmiResourceManager.loadProperty(current);
                    toReturn.add(new RepositoryObject(property));
                }
            }
        }
        return toReturn;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IRepositoryFactory#deleteObject(org.talend .core.model.general.Project,
     * org.talend.core.model.repository.IRepositoryViewObject)
     */

    public void deleteObjectLogical(Project project, IRepositoryViewObject objToDelete) throws PersistenceException {

        // can only delete in the main project
        // IProject fsProject = ResourceModelUtils.getProject(project);

        // IFolder bin = ResourceUtils.getFolder(fsProject, ERepositoryObjectType.getFolderName(objToDelete.getType())
        // + IPath.SEPARATOR + BIN, true);

        List<IRepositoryViewObject> allVersionToDelete = getAllVersion(project, objToDelete.getId(), false);
        for (IRepositoryViewObject currentVersion : allVersionToDelete) {
            ItemState state = currentVersion.getProperty().getItem().getState();
            state.setDeleted(true);
            xmiResourceManager.saveResource(state.eResource());
        }
    }

    public void deleteObjectPhysical(Project project, IRepositoryViewObject objToDelete) throws PersistenceException {
        deleteObjectPhysical(project, objToDelete, null);
    }

    public void deleteObjectPhysical(Project project, IRepositoryViewObject objToDelete, String version)
            throws PersistenceException {
        deleteObjectPhysical(project, objToDelete, version, false);
    }

    public void deleteObjectPhysical(Project project, IRepositoryViewObject objToDelete, String version,
            boolean fromEmptyRecycleBin) throws PersistenceException {
        if ("".equals(version)) { //$NON-NLS-1$
            version = null; // for all version
        }
        // can only delete in the main project
        List<IRepositoryViewObject> allVersionToDelete = getAllVersion(project, objToDelete.getId(), false);
        for (IRepositoryViewObject currentVersion : allVersionToDelete) {
            if (version == null || currentVersion.getVersion().equals(version)) {
                Item currentItem = currentVersion.getProperty().getItem();
                if (currentItem.getParent() instanceof FolderItem) {
                    ((FolderItem) currentItem.getParent()).getChildren().remove(currentItem);
                }
                currentItem.setParent(null);
                List<Resource> affectedResources = xmiResourceManager.getAffectedResources(currentVersion.getProperty());
                for (Resource resource : affectedResources) {
                    xmiResourceManager.deleteResource(resource);
                }
            }
        }
        if (!fromEmptyRecycleBin) {
            saveProject(project);
        }
    }

    public void restoreObject(IRepositoryViewObject objToRestore, IPath path) throws PersistenceException {
        // IProject fsProject = ResourceModelUtils.getProject(getRepositoryContext().getProject());
        // IFolder typeRootFolder = ResourceUtils.getFolder(fsProject,
        // ERepositoryObjectType.getFolderName(objToRestore.getType()),
        // true);
        // IPath thePath = (path == null ? typeRootFolder.getFullPath() :
        // typeRootFolder.getFullPath().append(path));
        // org.talend.core.model.properties.Project project = xmiResourceManager.loadProject(getProject());

        List<IRepositoryViewObject> allVersionToDelete = getAllVersion(getRepositoryContext().getProject(), objToRestore.getId(),
                false);
        for (IRepositoryViewObject currentVersion : allVersionToDelete) {
            ItemState itemState = currentVersion.getProperty().getItem().getState();
            itemState.setDeleted(false);
            xmiResourceManager.saveResource(itemState.eResource());
        }

        // xmiResourceManager.saveResource(project.eResource());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IRepositoryFactory#moveObject(org.talend. core.model.general.Project,
     * org.talend.core.model.repository.IRepositoryViewObject, org.eclipse.core.runtime.IPath)
     */
    public void moveObject(IRepositoryViewObject objToMove, IPath newPath) throws PersistenceException {
        Project project = getRepositoryContext().getProject();
        IProject fsProject = ResourceModelUtils.getProject(project);
        String folderName = ERepositoryObjectType.getFolderName(objToMove.getRepositoryObjectType()) + IPath.SEPARATOR + newPath;
        IFolder folder = ResourceUtils.getFolder(fsProject, folderName, true);

        ERepositoryObjectType itemType = ERepositoryObjectType.getItemType(objToMove.getProperty().getItem());
        FolderItem folderItem = getFolderItem(project, itemType, newPath);

        // get all objects from the current folder.
        List<IRepositoryViewObject> objects = getSerializableFromFolder(project, folderItem, null, itemType, false, false, true,
                true, false);

        for (IRepositoryViewObject oject : objects) {
            if (oject.getLabel().equalsIgnoreCase(objToMove.getLabel())) {
                throw new PersistenceException("Item already existing with the same name in this folder: name:"
                        + oject.getLabel() + " / folder:" + newPath.toPortableString());
            }
        }

        List<IRepositoryViewObject> allVersionToMove = getAllVersion(getRepositoryContext().getProject(), objToMove.getId(),
                false);
        for (IRepositoryViewObject obj : allVersionToMove) {
            Item currentItem = obj.getProperty().getItem();
            // Added yyin 20120614 TDQ-5468, add ByteArray into item from file.
            if (currentItem instanceof TDQItem) {
                this.saveTDQItem((TDQItem) currentItem);
            }
            // ~
            if (currentItem.getParent() instanceof FolderItem) {
                ((FolderItem) currentItem.getParent()).getChildren().remove(currentItem);
            }
            FolderItem newFolderItem = getFolderItem(project, objToMove.getRepositoryObjectType(), newPath);
            newFolderItem.getChildren().add(currentItem);
            currentItem.setParent(newFolderItem);

            ItemState state = obj.getProperty().getItem().getState();
            state.setPath(newPath.toString());
            xmiResourceManager.saveResource(state.eResource());

            // all resources attached must be saved before move the resources
            List<Resource> affectedResources = xmiResourceManager.getAffectedResources(obj.getProperty());
            for (Resource resource : affectedResources) {
                    xmiResourceManager.saveResource(resource);
                }
            
            for (Resource resource : affectedResources) {
                IPath path = folder.getFullPath().append(resource.getURI().lastSegment());
                // Find cross reference and save them.
                Map<EObject, Collection<Setting>> find = EcoreUtil.ExternalCrossReferencer.find(resource);
                List<Resource> needSaves = new ArrayList<Resource>();
                for (EObject object : find.keySet()) {
                    Resource re = object.eResource();
                    if (re == null) {
                        continue;
                    }
                    EcoreUtil.resolveAll(re);
                    needSaves.add(re);
                }
                xmiResourceManager.moveResource(resource, path);

                AbstractResourceChangesService resChangeService = TDQServiceRegister.getInstance().getResourceChangeService(
                        AbstractResourceChangesService.class);
                if (resChangeService != null) {
                    for (Resource toSave : needSaves) {
                        resChangeService.saveResourceByEMFShared(toSave);
                    }
                }
            }
            // all resources attached must be saved again after move the resources, or author will link to wrong path
            // for project file
            affectedResources = xmiResourceManager.getAffectedResources(obj.getProperty());
            for (Resource resource : affectedResources) {
                xmiResourceManager.saveResource(resource);
            }
        }
        saveProject(project);
    }

    // Added yyin 20120614 TDQ-5468
    private boolean saveTDQItem(TDQItem item) {
        AbstractResourceChangesService resChangeService = TDQServiceRegister.getInstance().getResourceChangeService(
                AbstractResourceChangesService.class);
        if (resChangeService != null) {
        return resChangeService.saveSourceFile(item);
        }
        return false;
    }

    public void lock(Item item) throws PersistenceException {
        if (getStatus(item) == ERepositoryStatus.DEFAULT) {
            // lockedObject.put(item.getProperty().getId(), new LockedObject(new
            // Date(), repositoryContext.getUser()));
            item.getState().setLockDate(new Date());
            item.getState().setLocker(getRepositoryContext().getUser());
            item.getState().setLocked(true);
            xmiResourceManager.saveResource(item.getProperty().eResource());
        }
    }

    public void unlock(Item item) throws PersistenceException {
        if (getStatus(item) == ERepositoryStatus.LOCK_BY_USER || item instanceof JobletDocumentationItem
                || item instanceof JobDocumentationItem) {
            // lockedObject.remove(obj.getProperty().getId());
            item.getState().setLocker(null);
            item.getState().setLockDate(null);
            item.getState().setLocked(false);
            xmiResourceManager.saveResource(item.getProperty().eResource());
        }
    }

    public List<Status> getDocumentationStatus() throws PersistenceException {
        // reloadProject(ProjectManager.getInstance().getCurrentProject());
        return copyList(ProjectManager.getInstance().getCurrentProject().getEmfProject().getDocumentationStatus());
    }

    public List<Status> getTechnicalStatus() throws PersistenceException {
        // reloadProject(ProjectManager.getInstance().getCurrentProject());
        Project currentProject = ProjectManager.getInstance().getCurrentProject();
        if (currentProject == null) {
            return null;
        }
        org.talend.core.model.properties.Project emfProject = currentProject.getEmfProject();
        if (emfProject == null) {
            return null;
        }
        return copyList(emfProject.getTechnicalStatus());
    }

    public List<SpagoBiServer> getSpagoBiServer() throws PersistenceException {
        // reloadProject(ProjectManager.getInstance().getCurrentProject());
        return copyListOfSpagoBiServer(ProjectManager.getInstance().getCurrentProject().getEmfProject().getSpagoBiServer());
    }

    private List<Status> copyList(List listOfStatus) {
        List<Status> result = new ArrayList<Status>();
        for (Object obj : listOfStatus) {
            result.add((Status) obj);
        }
        return result;
    }

    private List<SpagoBiServer> copyListOfSpagoBiServer(List listOfSpagoBiServer) {
        List<SpagoBiServer> result = new ArrayList<SpagoBiServer>();
        for (Object obj : listOfSpagoBiServer) {
            result.add((SpagoBiServer) obj);
        }
        return result;
    }

    public void setDocumentationStatus(List<Status> list) throws PersistenceException {
        Project curProject = ProjectManager.getInstance().getCurrentProject();
        // reloadProject(curProject);
        curProject.getEmfProject().getDocumentationStatus().clear();
        curProject.getEmfProject().getDocumentationStatus().addAll(list);
        saveProject(curProject);
    }

    public void setTechnicalStatus(List<Status> list) throws PersistenceException {
        Project curProject = ProjectManager.getInstance().getCurrentProject();
        // reloadProject(curProject);
        curProject.getEmfProject().getTechnicalStatus().clear();
        curProject.getEmfProject().getTechnicalStatus().addAll(list);
        saveProject(curProject);
    }

    public void setSpagoBiServer(List<SpagoBiServer> list) throws PersistenceException {
        Project curProject = ProjectManager.getInstance().getCurrentProject();
        // reloadProject(curProject);
        curProject.getEmfProject().getSpagoBiServer().clear();
        curProject.getEmfProject().getSpagoBiServer().addAll(list);
        saveProject(curProject);
    }

    public void setMigrationTasksDone(Project project, List<String> list) throws PersistenceException {
        // reloadProject(project);
        // IProject iproject = ResourceModelUtils.getProject(project);
        // org.talend.core.model.properties.Project loadProject = xmiResourceManager.loadProject(iproject);
        project.getEmfProject().getMigrationTasks().clear();
        project.getEmfProject().getMigrationTasks().addAll(list);
        saveProject(project);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IRepositoryFactory#isServerValid(java.lang .String, java.lang.String, int)
     */
    public String isServerValid() {
        return ""; //$NON-NLS-1$
    }

    private Resource create(IProject project, BusinessProcessItem item, IPath path) throws PersistenceException {
        Resource itemResource = xmiResourceManager.createItemResource(project, item, path,
                ERepositoryObjectType.BUSINESS_PROCESS, false);
        // notation depends on semantic ...
        // in case of new(=empty) diagram, we don't care about order
        // in other cases, the ordered addition references between notaion and
        // semantic will be updated
        itemResource.getContents().add(item.getSemantic());
        itemResource.getContents().add(item.getNotationHolder());
        item.computeNotationHolder();

        return itemResource;
    }

    private Resource create(IProject project, ConnectionItem item, ERepositoryObjectType type, IPath path)
            throws PersistenceException {
        Resource itemResource = xmiResourceManager.createItemResource(project, item, path, type, false);

        // itemResource.getContents().add(item.getConnection());
        MetadataManager.addContents(item, itemResource); // hywang 13221

        return itemResource;
    }

    private Resource create(IProject project, JobDocumentationItem item, IPath path, ERepositoryObjectType type)
            throws PersistenceException {
        Resource itemResource = xmiResourceManager.createItemResource(project, item, path, type, true);
        itemResource.getContents().clear();
        itemResource.getContents().add(item.getContent());

        return itemResource;
    }

    private Resource save(BusinessProcessItem item) {
        Resource itemResource = xmiResourceManager.getItemResource(item);
        itemResource.getContents().clear();
        // itemResource.getContents().add(item.getNotation());
        itemResource.getContents().add(item.getSemantic());
        itemResource.getContents().add(item.getNotationHolder());
        item.computeNotationHolder();

        return itemResource;
    }

    private Resource save(ConnectionItem item) {
        Resource itemResource = xmiResourceManager.getItemResource(item);
        itemResource.getContents().clear();
        MetadataManager.addContents(item, itemResource); // 13221

        // add to the current resource all Document and Description instances because they are not reference in
        // containment references.
        Map<EObject, Collection<Setting>> externalCrossref = EcoreUtil.ExternalCrossReferencer.find(item.getConnection());
        Collection<Object> documents = EcoreUtil.getObjectsByType(externalCrossref.keySet(),
                BusinessinformationPackage.Literals.DOCUMENT);
        for (Object doc : documents) {
            itemResource.getContents().add((EObject) doc);
        }
        Collection<Object> descriptions = EcoreUtil.getObjectsByType(externalCrossref.keySet(),
                BusinessinformationPackage.Literals.DESCRIPTION);
        for (Object doc : descriptions) {
            itemResource.getContents().add((EObject) doc);
        }
        // itemResource.getContents().add(item.getConnection());

        return itemResource;
    }

    private Resource save(TDQItem item) {
        Resource itemResource = xmiResourceManager.getItemResource(item);
        return itemResource;
    }

    private Resource create(IProject project, FileItem item, IPath path, ERepositoryObjectType type) throws PersistenceException {
        Resource itemResource = xmiResourceManager.createItemResource(project, item, path, type, true);

        itemResource.getContents().add(item.getContent());

        return itemResource;
    }

    private Resource save(FileItem item) {
        Resource itemResource = xmiResourceManager.getItemResource(item);

        ByteArray content = item.getContent();
        itemResource.getContents().clear();
        itemResource.getContents().add(content);

        return itemResource;
    }

    private Resource create(IProject project, ProcessItem item, IPath path, ERepositoryObjectType type)
            throws PersistenceException {
        Resource itemResource = xmiResourceManager.createItemResource(project, item, path, type, false);
        itemResource.getContents().add(item.getProcess());

        return itemResource;
    }

    private Resource createScreenshotResource(IProject project, Item item, IPath path, ERepositoryObjectType type)
            throws PersistenceException {
        Resource itemResource = xmiResourceManager.createScreenshotResource(project, item, path, type, false);
        if (item instanceof ProcessItem) {
            itemResource.getContents().addAll(((ProcessItem) item).getProcess().getScreenshots());
        } else if (item instanceof JobletProcessItem) {
            itemResource.getContents().addAll(((JobletProcessItem) item).getJobletProcess().getScreenshots());
        }

        return itemResource;
    }

    private Resource create(IProject project, JobletProcessItem item, IPath path, ERepositoryObjectType type)
            throws PersistenceException {
        Resource itemResource = xmiResourceManager.createItemResource(project, item, path, type, false);
        itemResource.getContents().add(item.getJobletProcess());
        ByteArray icon = item.getIcon();
        if (icon != null) {
            itemResource.getContents().add(icon);
        }
        return itemResource;
    }

    private Resource create(IProject project, ContextItem item, IPath path) throws PersistenceException {
        Resource itemResource = xmiResourceManager.createItemResource(project, item, path, ERepositoryObjectType.CONTEXT, false);
        itemResource.getContents().addAll(item.getContext());

        return itemResource;
    }

    private Resource save(ContextItem item) {
        Resource itemResource = xmiResourceManager.getItemResource(item);

        itemResource.getContents().clear();
        itemResource.getContents().addAll(item.getContext());

        return itemResource;
    }

    private Resource create(IProject project, SnippetItem item, IPath path) throws PersistenceException {
        Resource itemResource = xmiResourceManager.createItemResource(project, item, path, ERepositoryObjectType.SNIPPETS, false);
        itemResource.getContents().addAll(item.getVariables());

        return itemResource;
    }

    private Resource save(SnippetItem item) {
        Resource itemResource = xmiResourceManager.getItemResource(item);

        itemResource.getContents().clear();
        itemResource.getContents().addAll(item.getVariables());

        return itemResource;
    }

    private Resource saveScreenshots(Item item) {
        Resource itemResource = xmiResourceManager.getScreenshotResource(item);
        itemResource.getContents().clear();
        EMap screenshots = null;
        if (item instanceof ProcessItem) {
            screenshots = ((ProcessItem) item).getProcess().getScreenshots();
            itemResource.getContents().addAll(EcoreUtil.copyAll(screenshots));
        } else if (item instanceof JobletProcessItem) {
            screenshots = ((JobletProcessItem) item).getJobletProcess().getScreenshots();
            itemResource.getContents().addAll(screenshots);
        }
        return itemResource;
    }

    private Resource save(ProcessItem item) {
        item.getProcess().setScreenshot(null);
        Resource itemResource = xmiResourceManager.getItemResource(item);
        itemResource.getContents().clear();
        itemResource.getContents().add(item.getProcess());
        return itemResource;
    }

    private Resource save(JobletProcessItem item) {
        item.getJobletProcess().setScreenshot(null);
        Resource itemResource = xmiResourceManager.getItemResource(item);

        itemResource.getContents().clear();
        itemResource.getContents().add(item.getJobletProcess());
        if (item.getIcon() == null) {
            item.setIcon(PropertiesFactory.eINSTANCE.createByteArray());
            ImageDescriptor imageDesc = ImageProvider.getImageDesc(ECoreImage.JOBLET_COMPONENT_ICON);
            item.getIcon().setInnerContent(ImageUtils.saveImageToData(imageDesc));
        }
        if (item.getIcon() != null) {
            itemResource.getContents().add(item.getIcon());
        }
        return itemResource;
    }

    private Resource create(IProject project, LinkDocumentationItem item, IPath path) throws PersistenceException {
        Resource itemResource = xmiResourceManager.createItemResource(project, item, path, ERepositoryObjectType.DOCUMENTATION,
                false);
        itemResource.getContents().add(item.getLink());

        return itemResource;
    }

    private Resource create(IProject project, LinkRulesItem item, IPath path) throws PersistenceException { // hywang
        // 6484
        Resource itemResource = xmiResourceManager.createItemResource(project, item, path,
                ERepositoryObjectType.METADATA_FILE_LINKRULES, false);
        itemResource.getContents().add(item.getLink());

        return itemResource;
    }

    private Resource save(LinkDocumentationItem item) {
        Resource itemResource = xmiResourceManager.getItemResource(item);

        itemResource.getContents().clear();
        itemResource.getContents().add(item.getLink());

        return itemResource;
    }

    public void save(Project project, Item item) throws PersistenceException {
        xmiResourceManager.getAffectedResources(item.getProperty()); // only call this will force to load all sub items
                                                                     // in case some are not loaded

        computePropertyMaxInformationLevel(item.getProperty());
        item.getProperty().setModificationDate(new Date());
        Resource itemResource = null;
        Resource screenshotResource = null;
        EClass eClass = item.eClass();
        boolean screenshotFlag = false;
        if (eClass.eContainer() == PropertiesPackage.eINSTANCE) {
            switch (eClass.getClassifierID()) {

            case PropertiesPackage.HEADER_FOOTER_CONNECTION_ITEM:
                itemResource = save((HeaderFooterConnectionItem) item);
                break;
            case PropertiesPackage.BUSINESS_PROCESS_ITEM:
                itemResource = save((BusinessProcessItem) item);
                break;
            case PropertiesPackage.SVG_BUSINESS_PROCESS_ITEM:
                itemResource = save((SVGBusinessProcessItem) item);
                break;
            case PropertiesPackage.POSITIONAL_FILE_CONNECTION_ITEM:
            case PropertiesPackage.DELIMITED_FILE_CONNECTION_ITEM:
            case PropertiesPackage.DATABASE_CONNECTION_ITEM:
            case PropertiesPackage.REG_EX_FILE_CONNECTION_ITEM:
            case PropertiesPackage.XML_FILE_CONNECTION_ITEM:
            case PropertiesPackage.EXCEL_FILE_CONNECTION_ITEM:
            case PropertiesPackage.LDAP_SCHEMA_CONNECTION_ITEM:
            case PropertiesPackage.SALESFORCE_SCHEMA_CONNECTION_ITEM:
            case PropertiesPackage.WSDL_SCHEMA_CONNECTION_ITEM:
            case PropertiesPackage.SAP_CONNECTION_ITEM:
            case PropertiesPackage.MDM_CONNECTION_ITEM:
            case PropertiesPackage.HL7_CONNECTION_ITEM:
            case PropertiesPackage.FTP_CONNECTION_ITEM:
            case PropertiesPackage.BRMS_CONNECTION_ITEM:
            case PropertiesPackage.EBCDIC_CONNECTION_ITEM:
                // not really usefull for ConnectionItem : it's not copied to
                // another resource for edition
                itemResource = save((ConnectionItem) item);
                break;
            case PropertiesPackage.LDIF_FILE_CONNECTION_ITEM:
                // not really usefull for ConnectionItem : it's not copied to
                // another resource for edition
                itemResource = save((ConnectionItem) item);
                break;
            case PropertiesPackage.GENERIC_SCHEMA_CONNECTION_ITEM:
                // not really usefull for ConnectionItem : it's not copied to
                // another resource for edition
                itemResource = save((ConnectionItem) item);
                break;
            case PropertiesPackage.DOCUMENTATION_ITEM:
            case PropertiesPackage.ROUTINE_ITEM:
            case PropertiesPackage.JOB_SCRIPT_ITEM:
            case PropertiesPackage.SQL_PATTERN_ITEM:
                itemResource = save((FileItem) item);
                break;
            case PropertiesPackage.PROCESS_ITEM:
                screenshotResource = saveScreenshots(item);
                itemResource = save((ProcessItem) item);
                screenshotFlag = true;
                break;
            case PropertiesPackage.JOBLET_PROCESS_ITEM:
                screenshotResource = saveScreenshots(item);
                itemResource = save((JobletProcessItem) item);
                screenshotFlag = true;
                break;
            case PropertiesPackage.CONTEXT_ITEM:
                itemResource = save((ContextItem) item);
                break;
            case PropertiesPackage.SNIPPET_ITEM:
                itemResource = save((SnippetItem) item);
                break;
            case PropertiesPackage.JOB_DOCUMENTATION_ITEM:
                itemResource = save((JobDocumentationItem) item);
                break;
            case PropertiesPackage.JOBLET_DOCUMENTATION_ITEM:
                itemResource = save((JobletDocumentationItem) item);
                break;
            case PropertiesPackage.LINK_DOCUMENTATION_ITEM:
                itemResource = save((LinkDocumentationItem) item);
                break;
            case PropertiesPackage.RULES_ITEM:// feature 6484 added
                itemResource = save((RulesItem) item);
                break;
            case PropertiesPackage.VALIDATION_RULES_CONNECTION_ITEM:
                itemResource = save((ValidationRulesConnectionItem) item);
                break;
            case PropertiesPackage.EDIFACT_CONNECTION_ITEM:
                itemResource = save((EDIFACTConnectionItem) item);
                break;
            default:
                throw new UnsupportedOperationException();
            }
        } else {
            // MOD xqliu 2010-12-16 15750
            if (item instanceof TDQItem) {
                // MOD qiongli 2011-9-22 bug TDQ-3523,if the eResource is null,get the lateset version and reload.
                if (item.eResource() == null || item.getProperty().eResource() == null) {
                    IRepositoryViewObject repositoryViewObject = getLastVersion(new Project(ProjectManager.getInstance()
                            .getProject(item.getProperty().getItem())), item.getProperty().getId());
                    Property property = repositoryViewObject.getProperty();
                    item = property.getItem();
                }
                // Added yyin 20120614 TDQ-5468, add ByteArray into item from file.
                this.saveTDQItem((TDQItem) item);
                // ~
                itemResource = save((TDQItem) item);
            } else {
                for (IRepositoryContentHandler handler : RepositoryContentManager.getHandlers()) {
                    itemResource = handler.save(item);
                    if (itemResource != null) {
                        break;
                    }
                }
                if (itemResource == null) {
                    throw new UnsupportedOperationException();
                }
            }
            // ~ 15750
        }

        propagateFileName(project, item.getProperty());
        if (item.eResource() != null && itemResource != null) {
            List<Resource> referenceFileReources = getReferenceFilesResources(item, item.eResource());
            for (Resource referenceFileResource : referenceFileReources) {
                xmiResourceManager.saveResource(referenceFileResource);
            }
            xmiResourceManager.saveResource(item.eResource());
            xmiResourceManager.saveResource(itemResource);
            /* should release the refereneces of resources */
            referenceFileReources = null;
            if (screenshotFlag && !copyScreenshotFlag) {
                xmiResourceManager.saveResource(screenshotResource);
            }
            this.copyScreenshotFlag = false;
        }
    }

    public void save(Project project, Property property) throws PersistenceException {
        xmiResourceManager.getAffectedResources(property); // only call this will force to load all sub items in case
                                                           // some are not loaded

        computePropertyMaxInformationLevel(property);
        propagateFileName(project, property);
        // update the property of the node repository object
        Resource propertyResource = property.eResource();
        if (propertyResource != null) {
            xmiResourceManager.saveResource(propertyResource);
        }
    }

    public Item copy(Item originalItem, IPath path) throws PersistenceException, BusinessException {
        return copy(originalItem, path, true);
    }

    public Item copy(Item originalItem, IPath path, boolean changeLabelWithCopyPrefix) throws PersistenceException,
            BusinessException {
        Resource resource;
        ProjectManager projectManage = ProjectManager.getInstance();
        if (!projectManage.getProject(originalItem).equals(projectManage.getCurrentProject().getEmfProject())) {
            originalItem.getProperty().eResource().getContents().add(originalItem);
        }
        resource = originalItem.getProperty().eResource();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            resource.save(out, null);
            Resource createResource = new ResourceSetImpl().createResource(resource.getURI());
            ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
            createResource.load(in, null);
            Item newItem = copyFromResource(createResource, changeLabelWithCopyPrefix);
            // *need to create all referenece files when copy the item*//
            copyReferenceFiles(originalItem, newItem);
            create(getRepositoryContext().getProject(), newItem, path);
            copyScreenshotFile(originalItem, newItem);

            if (newItem instanceof ConnectionItem) {
                ConnectionItem connectionItem = (ConnectionItem) newItem;
                Resource itemResource = xmiResourceManager.getItemResource(connectionItem);
                if (itemResource != null && itemResource instanceof XMLResource) {
                    XMLResource xmlResource = (XMLResource) itemResource;
                    xmlResource.setID(connectionItem.getConnection(), EcoreUtil.generateUUID());
                }
            }
            return newItem;
        } catch (IOException e) {
            // e.printStackTrace();
            ExceptionHandler.process(e);
        }

        return null;
    }

    // add for bug TDI-20844,when copy or duplicate a job,joblet.just copy .screenshot file will be ok.
    private void copyScreenshotFile(Item originalItem, Item newItem) throws IOException {
        int id = originalItem.eClass().getClassifierID();
        if (id != PropertiesPackage.PROCESS_ITEM && id != PropertiesPackage.JOBLET_PROCESS_ITEM) {
            return;
        }
        this.copyScreenshotFlag = true;
        OutputStream os = null;
        InputStream is = null;
        try {
            URI orgPropertyResourceURI = EcoreUtil.getURI(originalItem.getProperty());
            URI orgRelativePlateformDestUri = orgPropertyResourceURI.trimFileExtension().appendFileExtension(
                    FileConstants.SCREENSHOT_EXTENSION);
            URL orgFileURL = FileLocator.toFileURL(new java.net.URL(
                    "platform:/resource" + orgRelativePlateformDestUri.toPlatformString(true))); //$NON-NLS-1$

            URI newPropertyResourceURI = EcoreUtil.getURI(newItem.getProperty());
            URI newRelativePlateformDestUri = newPropertyResourceURI.trimFileExtension().appendFileExtension(
                    FileConstants.SCREENSHOT_EXTENSION);
            URL newFileURL = FileLocator.toFileURL(new java.net.URL(
                    "platform:/resource" + newRelativePlateformDestUri.toPlatformString(true))); //$NON-NLS-1$

            os = new FileOutputStream(newFileURL.getFile());
            is = new BufferedInputStream(new FileInputStream(orgFileURL.getPath()));
            byte[] bytearray = new byte[512];
            int len = 0;
            while ((len = is.read(bytearray)) != -1) {
                os.write(bytearray, 0, len);
            }
        } finally {
            if (os != null) {
                os.close();
            }
            if (is != null) {
                is.close();
            }
        }

    }

    private void copyReferenceFiles(Item originalItem, Item newItem) throws PersistenceException {
        List<ReferenceFileItem> originalReferItems = originalItem.getReferenceResources();
        newItem.getReferenceResources().clear();
        for (ReferenceFileItem ri : originalReferItems) {
            ReferenceFileItem newRefItem = PropertiesFactory.eINSTANCE.createReferenceFileItem();
            newRefItem.setExtension(ri.getExtension());
            ByteArray byarray = PropertiesFactory.eINSTANCE.createByteArray();
            byarray.setInnerContent(ri.getContent().getInnerContent());

            newItem.getReferenceResources().add(newRefItem);
            newRefItem.setContent(byarray);
        }
    }

    private void propagateFileName(Project project, Property property) throws PersistenceException {
        List<IRepositoryViewObject> allVersionToMove = getAllVersion(project, property.getId(), false);
        for (IRepositoryViewObject object : allVersionToMove) {
            xmiResourceManager.propagateFileName(property, object.getProperty());
        }
    }

    public void create(Project project, Item item, IPath path, boolean... isImportItem) throws PersistenceException {
        computePropertyMaxInformationLevel(item.getProperty());

        if (item.getProperty().getVersion() == null) {
            item.getProperty().setVersion(VersionUtils.DEFAULT_VERSION);
        }
        if (item.getProperty().getAuthor() == null) {
            item.getProperty().setAuthor(getRepositoryContext().getUser());
        }

        if (item.getProperty().getCreationDate() == null) {
            item.getProperty().setCreationDate(new Date());
        }

        if (item.getProperty().getModificationDate() == null) {
            item.getProperty().setModificationDate(item.getProperty().getCreationDate());
        }

        ItemState itemState = PropertiesFactory.eINSTANCE.createItemState();
        if (item.getState() != null) {
            itemState.setDeleted(item.getState().isDeleted());
        } else {
            itemState.setDeleted(false);
        }
        itemState.setPath(path.toString());

        item.setState(itemState);
        IProject project2 = ResourceModelUtils.getProject(project);
        Resource itemResource = null;
        Resource screenshotsResource = null;
        EClass eClass = item.eClass();
        if (eClass.eContainer() == PropertiesPackage.eINSTANCE) {
            switch (eClass.getClassifierID()) {

            case PropertiesPackage.BUSINESS_PROCESS_ITEM:
                itemResource = create(project2, (BusinessProcessItem) item, path);
                break;
            case PropertiesPackage.SVG_BUSINESS_PROCESS_ITEM:
                itemResource = create(project2, (FileItem) item, path, ERepositoryObjectType.SVG_BUSINESS_PROCESS);
                break;
            case PropertiesPackage.DATABASE_CONNECTION_ITEM:
                itemResource = create(project2, (ConnectionItem) item, ERepositoryObjectType.METADATA_CONNECTIONS, path);
                break;
            case PropertiesPackage.SAP_CONNECTION_ITEM:
                itemResource = create(project2, (ConnectionItem) item, ERepositoryObjectType.METADATA_SAPCONNECTIONS, path);
                break;
            case PropertiesPackage.MDM_CONNECTION_ITEM:
                itemResource = create(project2, (ConnectionItem) item, ERepositoryObjectType.METADATA_MDMCONNECTION, path);
                break;
            case PropertiesPackage.EBCDIC_CONNECTION_ITEM:
                itemResource = create(project2, (EbcdicConnectionItem) item, ERepositoryObjectType.METADATA_FILE_EBCDIC, path);
                break;
            case PropertiesPackage.HL7_CONNECTION_ITEM:
                itemResource = create(project2, (HL7ConnectionItem) item, ERepositoryObjectType.METADATA_FILE_HL7, path);
                break;
            case PropertiesPackage.FTP_CONNECTION_ITEM:
                itemResource = create(project2, (FTPConnectionItem) item, ERepositoryObjectType.METADATA_FILE_FTP, path);
                break;
            case PropertiesPackage.BRMS_CONNECTION_ITEM:
                itemResource = create(project2, (BRMSConnectionItem) item, ERepositoryObjectType.METADATA_FILE_BRMS, path);
                break;
            case PropertiesPackage.DELIMITED_FILE_CONNECTION_ITEM:
                itemResource = create(project2, (ConnectionItem) item, ERepositoryObjectType.METADATA_FILE_DELIMITED, path);
                break;
            case PropertiesPackage.POSITIONAL_FILE_CONNECTION_ITEM:
                itemResource = create(project2, (ConnectionItem) item, ERepositoryObjectType.METADATA_FILE_POSITIONAL, path);
                break;
            case PropertiesPackage.REG_EX_FILE_CONNECTION_ITEM:
                itemResource = create(project2, (ConnectionItem) item, ERepositoryObjectType.METADATA_FILE_REGEXP, path);
                break;
            case PropertiesPackage.XML_FILE_CONNECTION_ITEM:
                itemResource = create(project2, (ConnectionItem) item, ERepositoryObjectType.METADATA_FILE_XML, path);
                break;
            case PropertiesPackage.EXCEL_FILE_CONNECTION_ITEM:
                itemResource = create(project2, (ConnectionItem) item, ERepositoryObjectType.METADATA_FILE_EXCEL, path);
                break;
            case PropertiesPackage.LDIF_FILE_CONNECTION_ITEM:
                itemResource = create(project2, (ConnectionItem) item, ERepositoryObjectType.METADATA_FILE_LDIF, path);
                break;
            case PropertiesPackage.VALIDATION_RULES_CONNECTION_ITEM:
                itemResource = create(project2, (ConnectionItem) item, ERepositoryObjectType.METADATA_VALIDATION_RULES, path);
                break;
            case PropertiesPackage.GENERIC_SCHEMA_CONNECTION_ITEM:
                itemResource = create(project2, (ConnectionItem) item, ERepositoryObjectType.METADATA_GENERIC_SCHEMA, path);
                break;
            case PropertiesPackage.LDAP_SCHEMA_CONNECTION_ITEM:
                itemResource = create(project2, (ConnectionItem) item, ERepositoryObjectType.METADATA_LDAP_SCHEMA, path);
                break;
            case PropertiesPackage.WSDL_SCHEMA_CONNECTION_ITEM:
                itemResource = create(project2, (ConnectionItem) item, ERepositoryObjectType.METADATA_WSDL_SCHEMA, path);
                break;
            case PropertiesPackage.SALESFORCE_SCHEMA_CONNECTION_ITEM:
                itemResource = create(project2, (ConnectionItem) item, ERepositoryObjectType.METADATA_SALESFORCE_SCHEMA, path);
                break;
            case PropertiesPackage.HEADER_FOOTER_CONNECTION_ITEM:
                itemResource = create(project2, (HeaderFooterConnectionItem) item, ERepositoryObjectType.METADATA_HEADER_FOOTER,
                        path);
                break;
            case PropertiesPackage.DOCUMENTATION_ITEM:
                itemResource = create(project2, (FileItem) item, path, ERepositoryObjectType.DOCUMENTATION);
                break;
            case PropertiesPackage.JOB_DOCUMENTATION_ITEM:
                itemResource = create(project2, (JobDocumentationItem) item, path, ERepositoryObjectType.JOB_DOC);
                break;
            case PropertiesPackage.JOBLET_DOCUMENTATION_ITEM:
                itemResource = create(project2, (JobletDocumentationItem) item, path, ERepositoryObjectType.JOBLET_DOC);
                break;
            case PropertiesPackage.ROUTINE_ITEM:
                itemResource = create(project2, (FileItem) item, path, ERepositoryObjectType.ROUTINES);
                break;
            case PropertiesPackage.JOB_SCRIPT_ITEM:
                itemResource = create(project2, (FileItem) item, path, ERepositoryObjectType.JOB_SCRIPT);
                break;
            case PropertiesPackage.SQL_PATTERN_ITEM:
                itemResource = create(project2, (FileItem) item, path, ERepositoryObjectType.SQLPATTERNS);
                break;
            case PropertiesPackage.PROCESS_ITEM:
                itemResource = create(project2, (ProcessItem) item, path, ERepositoryObjectType.PROCESS);
                screenshotsResource = createScreenshotResource(project2, (ProcessItem) item, path, ERepositoryObjectType.PROCESS);
                break;
            case PropertiesPackage.JOBLET_PROCESS_ITEM:
                itemResource = create(project2, (JobletProcessItem) item, path, ERepositoryObjectType.JOBLET);
                screenshotsResource = createScreenshotResource(project2, (JobletProcessItem) item, path,
                        ERepositoryObjectType.JOBLET);
                break;
            case PropertiesPackage.CONTEXT_ITEM:
                itemResource = create(project2, (ContextItem) item, path);
                break;
            case PropertiesPackage.SNIPPET_ITEM:
                itemResource = create(project2, (SnippetItem) item, path);
                break;
            case PropertiesPackage.LINK_DOCUMENTATION_ITEM:
                itemResource = create(project2, (LinkDocumentationItem) item, path);
                break;
            case PropertiesPackage.RULES_ITEM: // hywang add for 6484
                itemResource = create(project2, (FileItem) item, path, ERepositoryObjectType.METADATA_FILE_RULES);
                break;
            case PropertiesPackage.LINK_RULES_ITEM: // hywang add for 6484
                itemResource = create(project2, (LinkRulesItem) item, path);
                break;
            case PropertiesPackage.EDIFACT_CONNECTION_ITEM:// gldu add for 19384
                itemResource = create(project2, (EDIFACTConnectionItem) item, ERepositoryObjectType.METADATA_EDIFACT, path);
                break;
            default:

                throw new UnsupportedOperationException();
            }

        } else {
            if (item instanceof TDQItem) {
                // MOD mzhao resource change listener so that TOP can react the changes.
                AbstractResourceChangesService resChangeService = TDQServiceRegister.getInstance().getResourceChangeService(
                        AbstractResourceChangesService.class);
                if (resChangeService.isAnalysisOrReportItem(item)) {
                    path = Path.EMPTY;
                }
                itemResource = resChangeService.create(project2, item, eClass.getClassifierID(), path);
                if (itemResource == null) {
                    throw new UnsupportedOperationException();
                }
            } else {
                for (IRepositoryContentHandler handler : RepositoryContentManager.getHandlers()) {
                    itemResource = handler.create(project2, item, eClass.getClassifierID(), path);
                    if (itemResource != null) {
                        break;
                    }
                }
            }
        }
        Resource propertyResource = xmiResourceManager.createPropertyResource(itemResource);
        propertyResource.getContents().add(item.getProperty());
        propertyResource.getContents().add(item.getState());
        propertyResource.getContents().add(item);

        String parentPath = getParentPath(project, item, path);
        FolderHelper folderHelper = getFolderHelper(project.getEmfProject());
        FolderItem parentFolderItem = folderHelper.getFolder(parentPath);
        if (parentFolderItem == null) {
            parentFolderItem = folderHelper.createFolder(parentPath);
        }
        boolean add = parentFolderItem.getChildren().add(item);

        if (add) {
            item.setParent(parentFolderItem);
        }

        List referenceResources = item.getReferenceResources();
        for (ReferenceFileItem refFile : (List<ReferenceFileItem>) referenceResources) {
            Resource referenceFileResource = xmiResourceManager.getReferenceFileResource(propertyResource,
                    refFile.getExtension(), false);
            referenceFileResource.getContents().add(refFile.getContent());
            xmiResourceManager.saveResource(referenceFileResource);
            propertyResource.getContents().add(refFile);
        }
        xmiResourceManager.saveResource(screenshotsResource);
        xmiResourceManager.saveResource(itemResource);
        xmiResourceManager.saveResource(propertyResource);

        if (isImportItem.length == 0 || !isImportItem[0]) {
            saveProject(project);
        }
    }

    private List<Resource> getReferenceFilesResources(Item item, Resource propertyResource) {
        List referenceResources = item.getReferenceResources();
        List<Resource> referenceFileReources = new ArrayList<Resource>();
        for (ReferenceFileItem refFile : (List<ReferenceFileItem>) referenceResources) {
            Resource referenceFileReource = xmiResourceManager.getReferenceFileResource(propertyResource, refFile.getExtension(),
                    true);
            if (referenceFileReource.getContents() != null) {
                refFile.setContent((ByteArray) referenceFileReource.getContents().get(0));
            }
            referenceFileReources.add(referenceFileReource);
        }
        propertyResource.getContents().addAll(referenceResources);
        return referenceFileReources;
    }

    /**
     * DOC xqliu Comment method "getParentPath".
     * 
     * @param project
     * @param item
     * @param path
     * @return
     */
    private String getParentPath(Project project, Item item, IPath path) {
        return ERepositoryObjectType.getFolderName(ERepositoryObjectType.getItemType(item)) + IPath.SEPARATOR + path.toString();
    }

    private IProject getPhysicalProject(Project project) throws PersistenceException {
        return ResourceModelUtils.getProject(project);
    }

    public Property reload(Property property) {
        IFile file = URIHelper.getFile(property.eResource().getURI());
        List<Resource> affectedResources = xmiResourceManager.getAffectedResources(property);
        for (Resource resource : affectedResources) {
            resource.unload();
        }

        return xmiResourceManager.loadProperty(file);
    }

    public Property reload(Property property, IFile file) {
        // IFile file = URIHelper.getFile(property.eResource().getURI());
        List<Resource> affectedResources = xmiResourceManager.getAffectedResources(property);
        for (Resource resource : affectedResources) {
            resource.unload();
        }

        return xmiResourceManager.loadProperty(file);
    }

    public boolean doesLoggedUserExist() throws PersistenceException {
        boolean exist = false;
        org.talend.core.model.properties.Project emfProject = getRepositoryContext().getProject().getEmfProject();
        // IProject iProject = ResourceModelUtils.getProject(getRepositoryContext().getProject());
        // org.talend.core.model.properties.Project emfProject = xmiResourceManager.loadProject(iProject);
        Resource projectResource = emfProject.eResource();

        Collection users = EcoreUtil.getObjectsByType(projectResource.getContents(), PropertiesPackage.eINSTANCE.getUser());
        for (Iterator iter = users.iterator(); iter.hasNext();) {
            User emfUser = (User) iter.next();

            if (emfUser.getLogin().equals(getRepositoryContext().getUser().getLogin())) {
                getRepositoryContext().setUser(emfUser);
                exist = true;
            }
        }

        return exist;
    }

    public void createUser(Project project) throws PersistenceException {
        Resource projectResource = project.getEmfProject().eResource();
        projectResource.getContents().add(getRepositoryContext().getUser());
        xmiResourceManager.saveResource(projectResource);
    }

    public void initialize() throws PersistenceException {
        unloadUnlockedResources();
    }

    public void unloadUnlockedResources() {
        if (!ProxyRepositoryFactory.getInstance().isFullLogonFinished()) {
            // don't unload anything while logoon
            return;
        }
        List<Resource> resourceToUnload = new ArrayList<Resource>();
        List<URI> possibleItemsURItoUnload = new ArrayList<URI>();
        EList<Resource> kaka = xmiResourceManager.resourceSet.getResources();
        for (int i = 0; i < kaka.size(); i++) {
            Resource resource = xmiResourceManager.resourceSet.getResources().get(i);
            if (resource == null) {
                // only in case of bug from some items in the repository, to keep the repository stable even if a
                // problem happens
                continue;
            }
            final EList<EObject> contents = resource.getContents();
            for (int j = 0; j < contents.size(); j++) {
                EObject object = contents.get(j);
                if (object instanceof Property) {
                    if (((Property) object).getItem() instanceof FolderItem) {
                        continue;
                    }
                    if (((Property) object).getItem() instanceof RoutineItem) {
                        RoutineItem item = (RoutineItem) ((Property) object).getItem();
                        if (item.isBuiltIn()) {
                            continue;
                        }
                    }
                    if (((Property) object).getItem() instanceof SQLPatternItem) {
                        SQLPatternItem item = (SQLPatternItem) ((Property) object).getItem();
                        if (item.isSystem()) {
                            continue;
                        }
                    }

                    ERepositoryStatus status = getStatus(((Property) object).getItem());
                    if ((status == ERepositoryStatus.LOCK_BY_USER) || (status == ERepositoryStatus.NOT_UP_TO_DATE)) {
                        // System.out.println("locked (don't unload):" + ((Property) object).getLabel());
                        continue;
                    }
                    resourceToUnload.add(resource);
                    if (((Property) object).getItem() != null && ((Property) object).getItem().getParent() != null
                            && (((Property) object).getItem().getParent()) instanceof FolderItem) {

                        // bug 17768 :
                        boolean toKeepInMemorySinceDeleted = false;

                        if (((Property) object).getItem().getState().isDeleted()) {
                            toKeepInMemorySinceDeleted = true;
                        } else if (((Property) object).getItem() instanceof ConnectionItem) {
                            // should check if item is ConnectionItem, then check if any of the table is deleted.
                            // if yes, then don't unload all.
                            Connection connection = ((ConnectionItem) ((Property) object).getItem()).getConnection();

                            boolean haveTableDeleted = false;

                            for (MetadataTable table : ConnectionHelper.getTables(connection)) {
                                if (SubItemHelper.isDeleted(table)) {
                                    haveTableDeleted = true;
                                    break;
                                }
                            }
                            if (!haveTableDeleted && connection != null) {
                                QueriesConnection queriesConnection = connection.getQueries();
                                if (queriesConnection != null) {
                                    for (Query query : queriesConnection.getQuery()) {
                                        if (SubItemHelper.isDeleted(query)) {
                                            haveTableDeleted = true;
                                            break;
                                        }
                                    }
                                }

                                if (connection instanceof SAPConnection) {
                                    SAPConnection sapConn = (SAPConnection) connection;
                                    if (!haveTableDeleted) {
                                        EList<SAPFunctionUnit> funtions = sapConn.getFuntions();
                                        for (SAPFunctionUnit unit : funtions) {
                                            if (SubItemHelper.isDeleted(unit)) {
                                                haveTableDeleted = true;
                                                break;
                                            }
                                        }
                                    }
                                    if (!haveTableDeleted) {
                                        EList<SAPIDocUnit> iDocs = sapConn.getIDocs();
                                        for (SAPIDocUnit iDoc : iDocs) {
                                            if (SubItemHelper.isDeleted(iDoc)) {
                                                haveTableDeleted = true;
                                                break;
                                            }
                                        }
                                    }

                                }
                            }

                            if (haveTableDeleted) {
                                toKeepInMemorySinceDeleted = true;
                            }
                        }

                        // to free memory or parent will still hold the item
                        if (!toKeepInMemorySinceDeleted) {
                            ((FolderItem) ((Property) object).getItem().getParent()).getChildren().remove(
                                    ((Property) object).getItem());
                            ((Property) object).getItem().setParent(null);
                        }
                    }
                    possibleItemsURItoUnload.add(xmiResourceManager.getItemResourceURI(resource.getURI()));
                }
            }
        }
        kaka = xmiResourceManager.resourceSet.getResources();
        for (int i = 0; i < kaka.size(); i++) {
            Resource resource = kaka.get(i);
            final EList<EObject> contents = resource.getContents();
            for (int j = 0; j < contents.size(); j++) {
                EObject object = contents.get(j);
                if (!(object instanceof Property)) {
                    if (possibleItemsURItoUnload.contains(resource.getURI()) && !resourceToUnload.contains(resource)) {
                        resourceToUnload.add(resource);
                    }
                }
            }
        }

        // MOD mzhao resource change listener so that TOP can react the changes.
        AbstractResourceChangesService resChangeService = TDQServiceRegister.getInstance().getResourceChangeService(
                AbstractResourceChangesService.class);
        for (int i = 0; i < resourceToUnload.size(); i++) {
            Resource resource = resourceToUnload.get(i);
            if (resource.isLoaded()) {
                if (resChangeService != null) {
                    resChangeService.handleUnload(resource);
                }
                resource.unload();
            }
            // xmiResourceManager.resourceSet.getResources().remove(resource);
        }
    }

    public void logOnProject(Project project) throws PersistenceException, LoginException {
        if (getRepositoryContext().getUser().getLogin() == null) {
            throw new LoginException(Messages.getString("LocalRepositoryFactory.UserLoginCannotBeNull")); //$NON-NLS-1$
        }

        super.logOnProject(project);

        if (!doesLoggedUserExist()) {
            createUser(project);
        }

        IProject project2 = ResourceModelUtils.getProject(project);
        createFolders(project2, project.getEmfProject());
        synchronizeRoutines(project2);
        synchronizeSqlpatterns(project2);
        synchronizeFolders(project2, project.getEmfProject());
        changeRoutinesPackage(project);
        saveProject(project);

    }

    private void changeRoutinesPackage(Project project) {
        if (project == null) {
            return;
        }
        try {
            List<IRepositoryViewObject> allRoutines = getAll(project, ERepositoryObjectType.ROUTINES, true, true);
            for (IRepositoryViewObject object : allRoutines) {
                Item item = object.getProperty().getItem();
                RoutineUtils.changeRoutinesPackage(item);
            }
        } catch (PersistenceException e) {
            ExceptionHandler.process(e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IRepositoryFactory#getStatus(org.talend.core .model.properties.Item)
     */
    public ERepositoryStatus getStatus(Item item) {
        if (item != null && item.getState() != null) {
            if (item.getState().isDeleted()) {
                return ERepositoryStatus.DELETED;
            }

            // FIXME SML [Synchronizer] Move
            if (item.getState().isLocked()) {
                // User locker = item.getState().getLocker();
                // User connected = getRepositoryContext().getUser();
                // if (connected.equals(locker)) {
                return ERepositoryStatus.LOCK_BY_USER;
                // } else {
                // return ERepositoryStatus.LOCK_BY_OTHER;
                // }
            }

            if (item instanceof RoutineItem) {
                if (((RoutineItem) item).isBuiltIn()) {
                    return ERepositoryStatus.READ_ONLY;
                }
            }
            if (item instanceof SQLPatternItem) {
                if (((SQLPatternItem) item).isSystem()) {
                    return ERepositoryStatus.READ_ONLY;
                }
            }
        }
        return ERepositoryStatus.DEFAULT;
    }

    @Override
    protected Object getFolder(Project project, ERepositoryObjectType repositoryObjectType) throws PersistenceException {
        IProject fsProject = ResourceModelUtils.getProject(project);
        try {
            if (repositoryObjectType.hasFolder()) {
                String folderName = ERepositoryObjectType.getFolderName(repositoryObjectType);
                return ResourceUtils.getFolder(fsProject, folderName, true);
            }
        } catch (ResourceNotFoundException rex) {
            //
        }
        return null;
    }

    public List<org.talend.core.model.properties.Project> getReferencedProjects(Project project) {
        String parentBranch = getRepositoryContext().getFields().get(
                IProxyRepositoryFactory.BRANCH_SELECTION + "_" + project.getTechnicalLabel());

        List<org.talend.core.model.properties.Project> refProjectList = new ArrayList<org.talend.core.model.properties.Project>();
        for (ProjectReference refProject : (List<ProjectReference>) getRepositoryContext().getProject().getEmfProject()
                .getReferencedProjects()) {
            if (refProject.getBranch() == null || parentBranch.equals(refProject.getBranch())) {
                refProjectList.add(refProject.getReferencedProject());
            }
        }
        return refProjectList;
    }

    public Boolean hasChildren(Object parent) {
        return null;
    }

    public void updateItemsPath(ERepositoryObjectType type, IPath targetPath) throws PersistenceException {
        Project baseProject = getRepositoryContext().getProject();
        IProject project = ResourceModelUtils.getProject(baseProject);
        String folderPathString = ERepositoryObjectType.getFolderName(type) + IPath.SEPARATOR + targetPath.toString();
        IFolder folder = ResourceUtils.getFolder(project, folderPathString, false);
        unloadUnlockedResources();
        List<IRepositoryViewObject> serializableFromFolder = getSerializableFromFolder(baseProject, folder, null, type, true,
                false, false, false);
        for (IRepositoryViewObject repositoryObject : serializableFromFolder) {
            ItemState state = repositoryObject.getProperty().getItem().getState();
            state.setPath(getItemStatePath(type, targetPath).toString());
            xmiResourceManager.saveResource(state.eResource());
        }

        if (((IFolder) folder).exists()) {
            for (IResource current : ResourceUtils.getMembers((IFolder) folder)) {
                if (current instanceof IFolder) {
                    updateItemsPath(type, targetPath.append(current.getName()));
                }
            }
        }
    }

    /**
     * get the correct ItemStatePath for TDQ Elements(Patterns, Indicators, Rules, etc.).
     * 
     * @param type ERepositoryObjectType of the TDQ Element
     * @param targetPath target path of the TDQ Element
     * @return
     */
    private IPath getItemStatePath(ERepositoryObjectType type, IPath targetPath) {
        IPath itemStatePath = targetPath;
        if (ERepositoryObjectType.TDQ_PATTERN_REGEX.equals(type) || ERepositoryObjectType.TDQ_PATTERN_SQL.equals(type)
                || ERepositoryObjectType.TDQ_USERDEFINE_INDICATORS.equals(type)) {
            itemStatePath = Path.fromOSString(type.getFolder()).removeFirstSegments(2).append(targetPath);
        }
        return itemStatePath;
    }

    public boolean setAuthorByLogin(Item item, String login) throws PersistenceException {
        // IProject iProject = ResourceModelUtils.getProject(getRepositoryContext().getProject());
        // org.talend.core.model.properties.Project emfProject = xmiResourceManager.loadProject(iProject);
        Resource projectResource = getRepositoryContext().getProject().getEmfProject().eResource();

        Collection users = EcoreUtil.getObjectsByType(projectResource.getContents(), PropertiesPackage.eINSTANCE.getUser());
        for (Iterator iter = users.iterator(); iter.hasNext();) {
            User emfUser = (User) iter.next();
            if (emfUser.getLogin().equals(login)) {
                item.getProperty().setAuthor(emfUser);
                return true;
            }
        }
        return false;
    }

    // protected void saveProject() throws PersistenceException {
    // org.talend.core.model.properties.Project loadProject = xmiResourceManager.loadProject(getProject());
    // xmiResourceManager.saveResource(loadProject.eResource());
    // }

    public void unloadResources(Property property) {
        xmiResourceManager.unloadResources(property);
    }

    /**
     * MOD mzhao feature 9207
     */
    public void unloadResources(String uriString) {
        xmiResourceManager.unloadResource(uriString);
    }

    public void unloadResources() {
        xmiResourceManager.unloadResources();
    }

    @Override
    public Property getUptodateProperty(Project project, Property property) throws PersistenceException {
        Property uptodateProperty = super.getUptodateProperty(project, property);

        // FolderItem folderItem = null;
        // if (property.getItem().getParent() instanceof FolderItem) {
        // folderItem = (FolderItem) property.getItem().getParent();
        // folderItem.getChildren().remove(property.getItem());
        // }
        //
        // Property afterForceReload = xmiResourceManager.forceReloadProperty(uptodateProperty);
        // if (folderItem != null) {
        // folderItem.getChildren().add(afterForceReload.getItem());
        // afterForceReload.getItem().setParent(folderItem);
        // saveProject(project);
        // }

        return uptodateProperty;
    }

    public void reloadProject(Project project) throws PersistenceException {
        IProject pproject = getPhysicalProject(project);
        project.setEmfProject(xmiResourceManager.loadProject(pproject));
    }

    @Override
    public void executeRepositoryWorkUnit(RepositoryWorkUnit workUnit) {
        Project currentProject = ProjectManager.getInstance().getCurrentProject();
        if (currentProject != null && currentProject.isLocal()) {
            if (!workUnit.isAvoidUnloadResources()) { // 14969
                unloadUnlockedResources();
            }
        }
        super.executeRepositoryWorkUnit(workUnit);
        if (currentProject != null && currentProject.isLocal() && workUnit.isUnloadResourcesAfterRun()) { // 14969 avoid
            unloadUnlockedResources();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IRepositoryFactory#enableSandboxProject()
     */
    public boolean enableSandboxProject() {
        return false; // don't support in local model
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IRepositoryFactory#isLocalConnectionProvider()
     */
    public boolean isLocalConnectionProvider() throws PersistenceException {
        return true; // must be true
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.talend.repository.model.IRepositoryFactory#getMetadataByFolder(org.talend.core.model.repository.
     * ERepositoryObjectType, org.eclipse.core.runtime.IPath)
     */
    public <K, T> List<T> getMetadatasByFolder(Project project, ERepositoryObjectType itemType, IPath path) {
        FolderItem currentFolderItem = this.getFolderItem(project, itemType, path);
        if (currentFolderItem == null) {
            return new ArrayList<T>();
        }
        List<Item> invalidItems = new ArrayList<Item>();
        RootContainer<K, T> toReturn = new RootContainer<K, T>();
        for (Item curItem : (List<Item>) currentFolderItem.getChildren()) {
            if (curItem instanceof FolderItem) {
                //
            } else {
                Property property = curItem.getProperty();
                if (property != null) {
                    property.getItem().setParent(currentFolderItem);
                    IRepositoryViewObject currentObject;
                    currentObject = new RepositoryViewObject(property);
                    try {
                        addItemToContainer(toReturn, currentObject, true);
                    } catch (PersistenceException e) {
                        log.error(e, e);
                    }
                    addToHistory(property.getId(), itemType, property.getItem().getState().getPath());
                } else {
                    invalidItems.add(curItem);
                }
            }
        }
        for (Item item : invalidItems) {
            item.setParent(null);
        }
        currentFolderItem.getChildren().removeAll(invalidItems);
        return toReturn.getMembers();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IRepositoryFactory#getResourceManager()
     */
    public XmiResourceManager getResourceManager() {
        return this.xmiResourceManager;
    }

    public void executeMigrations(Project mainProject, boolean beforeLogon, SubMonitor monitorWrap) {
        IMigrationToolService service = (IMigrationToolService) GlobalServiceRegister.getDefault().getService(
                IMigrationToolService.class);
        service.executeProjectTasks(mainProject, beforeLogon, monitorWrap);
    }

}
