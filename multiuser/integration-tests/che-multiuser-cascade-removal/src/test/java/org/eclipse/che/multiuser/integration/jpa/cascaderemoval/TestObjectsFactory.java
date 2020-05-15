/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.multiuser.integration.jpa.cascaderemoval;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

import com.google.common.collect.ImmutableMap;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.che.account.shared.model.Account;
import org.eclipse.che.account.spi.AccountImpl;
import org.eclipse.che.api.core.model.workspace.devfile.Metadata;
import org.eclipse.che.api.factory.server.model.impl.AuthorImpl;
import org.eclipse.che.api.factory.server.model.impl.FactoryImpl;
import org.eclipse.che.api.ssh.server.model.impl.SshPairImpl;
import org.eclipse.che.api.user.server.model.impl.ProfileImpl;
import org.eclipse.che.api.user.server.model.impl.UserImpl;
import org.eclipse.che.api.workspace.server.model.impl.WorkspaceConfigImpl;
import org.eclipse.che.api.workspace.server.model.impl.WorkspaceImpl;
import org.eclipse.che.api.workspace.server.model.impl.devfile.ActionImpl;
import org.eclipse.che.api.workspace.server.model.impl.devfile.ComponentImpl;
import org.eclipse.che.api.workspace.server.model.impl.devfile.DevfileImpl;
import org.eclipse.che.api.workspace.server.model.impl.devfile.EntrypointImpl;
import org.eclipse.che.api.workspace.server.model.impl.devfile.MetadataImpl;
import org.eclipse.che.api.workspace.server.model.impl.devfile.ProjectImpl;
import org.eclipse.che.api.workspace.server.model.impl.devfile.SourceImpl;
import org.eclipse.che.multiuser.machine.authentication.server.signature.model.impl.SignatureKeyPairImpl;
import org.eclipse.che.multiuser.permission.workspace.server.model.impl.WorkerImpl;
import org.eclipse.che.multiuser.resource.spi.impl.FreeResourcesLimitImpl;
import org.eclipse.che.multiuser.resource.spi.impl.ResourceImpl;

/**
 * Defines method for creating tests object instances.
 *
 * @author Yevhenii Voevodin
 */
public final class TestObjectsFactory {

  public static AccountImpl createAccount(String id) {
    return new AccountImpl(id, id + "_name", "test");
  }

  public static UserImpl createUser(String id) {
    return new UserImpl(
        id, id + "@eclipse.org", id + "_name", "password", asList(id + "_alias1", id + "_alias2"));
  }

  public static ProfileImpl createProfile(String userId) {
    return new ProfileImpl(
        userId,
        new HashMap<>(
            ImmutableMap.of(
                "attribute1", "value1",
                "attribute2", "value2",
                "attribute3", "value3")));
  }

  public static Map<String, String> createPreferences() {
    return new HashMap<>(
        ImmutableMap.of(
            "preference1", "value1",
            "preference2", "value2",
            "preference3", "value3"));
  }

  public static WorkspaceConfigImpl createWorkspaceConfig(String id) {
    return new WorkspaceConfigImpl(
        id + "_name", id + "description", "default-env", null, null, null, null);
  }

  public static WorkspaceImpl createWorkspace(String id, Account account) {
    return new WorkspaceImpl(id, account, createWorkspaceConfig(id));
  }

  public static SshPairImpl createSshPair(String owner, String service, String name) {
    return new SshPairImpl(owner, service, name, "public-key", "private-key");
  }

  public static FactoryImpl createFactory(String id, String creator) {
    return new FactoryImpl(
        id,
        id + "-name",
        "4.0",
        createDevfile(id),
        new AuthorImpl(creator, System.currentTimeMillis()),
        null,
        null,
        null);
  }

  public static DevfileImpl createDevfile(String id) {
    return new DevfileImpl(
        "0.0.1",
        asList(createDevfileProject(id + "-project1"), createDevfileProject(id + "-project2")),
        asList(
            createDevfileComponent(id + "-component1"), createDevfileComponent(id + "-component2")),
        asList(createDevfileCommand(id + "-command1"), createDevfileCommand(id + "-command2")),
        singletonMap("attribute1", "value1"),
        createMetadata(id + "name"));
  }

  private static ComponentImpl createDevfileComponent(String name) {
    return new ComponentImpl(
        "kubernetes",
        name,
        "eclipse/che-theia/0.0.1",
        ImmutableMap.of("java.home", "/home/user/jdk11"),
        "https://mysite.com/registry/somepath",
        "/dev.yaml",
        "refContent",
        ImmutableMap.of("app.kubernetes.io/component", "webapp"),
        singletonList(createEntrypoint()),
        "image",
        "256G",
        "128M",
        "200m",
        "100m",
        false,
        singletonList("command"),
        singletonList("arg"),
        null,
        null,
        null);
  }

  private static EntrypointImpl createEntrypoint() {
    return new EntrypointImpl(
        "parentName",
        singletonMap("parent", "selector"),
        "containerName",
        asList("command1", "command2"),
        asList("arg1", "arg2"));
  }

  private static org.eclipse.che.api.workspace.server.model.impl.devfile.CommandImpl
      createDevfileCommand(String name) {
    return new org.eclipse.che.api.workspace.server.model.impl.devfile.CommandImpl(
        name, singletonList(createAction()), singletonMap("attr1", "value1"), null);
  }

  private static ActionImpl createAction() {
    return new ActionImpl("exec", "component", "run.sh", "/home/user", null, null);
  }

  private static ProjectImpl createDevfileProject(String name) {
    return new ProjectImpl(name, createDevfileSource(), "path");
  }

  private static SourceImpl createDevfileSource() {
    return new SourceImpl(
        "type", "http://location", "branch1", "point1", "tag1", "commit1", "sparseCheckoutDir1");
  }

  public static Metadata createMetadata(String name) {
    return new MetadataImpl(name);
  }

  public static WorkerImpl createWorker(String userId, String workspaceId) {
    return new WorkerImpl(workspaceId, userId, Arrays.asList("read", "write", "run"));
  }

  public static FreeResourcesLimitImpl createFreeResourcesLimit(String accountId) {
    return new FreeResourcesLimitImpl(
        accountId,
        Arrays.asList(new ResourceImpl("test1", 123, "mb"), new ResourceImpl("test2", 234, "h")));
  }

  public static SignatureKeyPairImpl createSignatureKeyPair(String workspaceId)
      throws NoSuchAlgorithmException {
    final KeyPairGenerator kpg;
    kpg = KeyPairGenerator.getInstance("RSA");
    kpg.initialize(512);
    final KeyPair pair = kpg.generateKeyPair();
    return new SignatureKeyPairImpl(workspaceId, pair.getPublic(), pair.getPrivate());
  }

  private TestObjectsFactory() {}
}
