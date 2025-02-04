// ============================================================================
//
// Copyright (C) 2006-2021 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.designer.maven.aether;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.codehaus.plexus.PlexusContainerException;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.util.artifact.SubArtifact;
import org.eclipse.aether.util.listener.ChainedRepositoryListener;
import org.eclipse.aether.util.listener.ChainedTransferListener;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.talend.commons.CommonsPlugin;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.designer.maven.aether.util.MavenLibraryResolverProvider;
import org.talend.designer.maven.aether.util.TalendAetherProxySelector;

/**
 * created by wchen on Aug 10, 2017 Detailled comment
 *
 */
public class RepositorySystemFactory {

    private static Boolean ignoreArtifactDescriptorRepositories;

    private static Map<LocalRepository, DefaultRepositorySystemSession> sessions = new HashMap<LocalRepository, DefaultRepositorySystemSession>();

    private static DefaultRepositorySystemSession newRepositorySystemSession(String localRepositoryPath)
            throws PlexusContainerException {
        LocalRepository localRepo = new LocalRepository(localRepositoryPath);
        DefaultRepositorySystemSession repositorySystemSession = sessions.get(localRepo);
        if (repositorySystemSession == null) {
            repositorySystemSession = MavenRepositorySystemUtils.newSession();
            repositorySystemSession
                    .setLocalRepositoryManager(
                            MavenLibraryResolverProvider.newRepositorySystem().newLocalRepositoryManager(repositorySystemSession,
                                    localRepo));
            repositorySystemSession.setTransferListener(new ChainedTransferListener());
            repositorySystemSession.setRepositoryListener(new ChainedRepositoryListener());
            repositorySystemSession.setProxySelector(new TalendAetherProxySelector());
            repositorySystemSession.setIgnoreArtifactDescriptorRepositories(
                    RepositorySystemFactory.isIgnoreArtifactDescriptorRepositories());
            sessions.put(localRepo, repositorySystemSession);
        }

        return repositorySystemSession;
    }

    private static void doDeploy(File content, File pomFile, String localRepository, String repositoryId, String repositoryUrl,
            String userName, String password, String groupId, String artifactId, String classifier, String extension,
            String version) throws Exception {
        DefaultRepositorySystemSession session = null;
        RepositorySystem system = MavenLibraryResolverProvider.newRepositorySystem();
        session = newRepositorySystemSession(localRepository);

        DeployRequest deployRequest = new DeployRequest();
        Artifact jarArtifact = new DefaultArtifact(groupId, artifactId, classifier, extension, version);
        jarArtifact = jarArtifact.setFile(content);
        deployRequest.addArtifact(jarArtifact);

        // try to find the pom file
        if (pomFile == null) {
            String strClassifier = classifier == null ? "" : ("-" + classifier);
            String pomPath = localRepository + "/" + groupId.replaceAll("\\.", "/") + "/" + artifactId + "/" + version + "/"
                    + artifactId + "-" + version + strClassifier + "." + "pom";
            pomFile = new File(pomPath);
        }
        if (pomFile.exists()) {
            Artifact pomArtifact = new SubArtifact(jarArtifact, "", "pom");
            pomArtifact = pomArtifact.setFile(pomFile);
            deployRequest.addArtifact(pomArtifact);
        }

        Authentication auth = new AuthenticationBuilder().addUsername(userName).addPassword(password).build();
        RemoteRepository distRepo = new RemoteRepository.Builder(repositoryId, "default", repositoryUrl).setAuthentication(auth)
                .build();
        distRepo = new RemoteRepository.Builder(distRepo).setProxy(new TalendAetherProxySelector().getProxy(distRepo)).build();

        deployRequest.setRepository(distRepo);

        system.deploy(session, deployRequest);
    }

    public static File resolve(String localRepository, String repositoryId, String repositoryUrl, String userName,
            String password, String groupId, String artifactId, String classifier, String extension, String version)
            throws Exception {
        DefaultRepositorySystemSession session = null;
        RepositorySystem system = MavenLibraryResolverProvider.newRepositorySystem();
        session = newRepositorySystemSession(localRepository);

        ArtifactRequest resolveRequest = new ArtifactRequest();
        Artifact jarArtifact = new DefaultArtifact(groupId, artifactId, classifier, extension, version);
        resolveRequest.setArtifact(jarArtifact);

        Authentication auth = new AuthenticationBuilder().addUsername(userName).addPassword(password).build();
        RepositoryPolicy defaultPolicy = new RepositoryPolicy();
        RepositoryPolicy snapshotPolicy = new RepositoryPolicy(true, RepositoryPolicy.UPDATE_POLICY_ALWAYS,
                defaultPolicy.getChecksumPolicy());

        RemoteRepository distRepo = new RemoteRepository.Builder(repositoryId, "default", repositoryUrl).setAuthentication(auth)
                .setSnapshotPolicy(snapshotPolicy).build();
        distRepo = new RemoteRepository.Builder(distRepo).setProxy(new TalendAetherProxySelector().getProxy(distRepo)).build();

        resolveRequest.addRepository(distRepo);

        try {
            ArtifactResult result = system.resolveArtifact(session, resolveRequest);
            if (result.isResolved()) {
                return result.getArtifact().getFile();
            } else {
                return null;
            }
        } catch (ArtifactResolutionException ae) {
            if (ae.getResult().isMissing()) {
                if (CommonsPlugin.isDebugMode()) {
                    ExceptionHandler.process(ae);
                }
                throw new FileNotFoundException(ae.getMessage());
            } else {
                throw ae;
            }
        }
    }

    public static void deploy(File content, String localRepository, String repositoryId, String repositoryUrl, String userName,
            String password, String groupId, String artifactId, String classifier, String extension, String version)
            throws Exception {

        doDeploy(content, null, localRepository, repositoryId, repositoryUrl, userName, password, groupId, artifactId, classifier,
                extension, version);
    }

    public static void deployWithPOM(File content, File pomFile, String localRepository, String repositoryId,
            String repositoryUrl, String userName, String password, String groupId, String artifactId, String classifier,
            String extension, String version) throws Exception {
        doDeploy(content, pomFile, localRepository, repositoryId, repositoryUrl, userName, password, groupId, artifactId,
                classifier, extension, version);
    }

    public static boolean isIgnoreArtifactDescriptorRepositories() {
        if (ignoreArtifactDescriptorRepositories == null) {
            ignoreArtifactDescriptorRepositories = Boolean.valueOf(
                    System.getProperty("talend.studio.aether.ignoreArtifactDescriptorRepositories", Boolean.TRUE.toString()));
        }
        return ignoreArtifactDescriptorRepositories;
    }

}
