/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.workspace.infrastructure.kubernetes.provision;

import com.google.common.collect.ImmutableSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.eclipse.che.api.core.model.workspace.config.ServerConfig;
import org.eclipse.che.api.core.model.workspace.runtime.RuntimeIdentity;
import org.eclipse.che.api.workspace.server.spi.InfrastructureException;
import org.eclipse.che.api.workspace.server.spi.environment.InternalMachineConfig;
import org.eclipse.che.multiuser.machine.authentication.server.signature.SignatureKeyManager;
import org.eclipse.che.workspace.infrastructure.kubernetes.KubernetesClientFactory;
import org.eclipse.che.workspace.infrastructure.kubernetes.environment.KubernetesEnvironment;
import org.eclipse.che.workspace.infrastructure.kubernetes.jwt.JwtProxyConfigBuilder;

/** @author Sergii Leshchenko */
public class ServersAuthenticationProvisioner
    implements ConfigurationProvisioner<KubernetesEnvironment> {

  private static final String PUBLIC_KEY_HEADER = "-----BEGIN PUBLIC KEY-----\n";
  private static final String PUBLIC_KEY_FOOTER = "\n-----END PUBLIC KEY-----";

  private final KubernetesClientFactory kubernetesClientFactory;
  private final SignatureKeyManager signatureKeyManager;
  private final JwtProxyProvisioner jwtProxyProvisioner;

  @Inject
  public ServersAuthenticationProvisioner(
      KubernetesClientFactory kubernetesClientFactory,
      SignatureKeyManager signatureKeyManager,
      JwtProxyProvisioner jwtProxyProvisioner) {
    this.kubernetesClientFactory = kubernetesClientFactory;
    this.signatureKeyManager = signatureKeyManager;
    this.jwtProxyProvisioner = jwtProxyProvisioner;
  }

  @Override
  public void provision(KubernetesEnvironment k8sEnv, RuntimeIdentity identity)
      throws InfrastructureException {
    Map<String, String> configs = new HashMap<>();

    secureServer("ws-agent", ImmutableSet.of("wsagent/http", "wsagent/ws"), 4471, 4401, k8sEnv, identity, configs);

    secureServer("exec-agent", ImmutableSet.of("exec-agent/http", "exec-agent/ws"), 4472, 4412, k8sEnv, identity, configs);

    secureServer("terminal", ImmutableSet.of("terminal"), 4473, 4411, k8sEnv, identity, configs);

    byte[] encodedPublicKey = signatureKeyManager.getKeyPair().getPublic().getEncoded();
    configs.put("mykey.pub",
        PUBLIC_KEY_HEADER
            + java.util.Base64.getEncoder().encodeToString(encodedPublicKey)
            + PUBLIC_KEY_FOOTER);

    KubernetesClient client = kubernetesClientFactory.create(identity.getWorkspaceId());
    client
        .secrets()
        .createOrReplaceWithNew()
        .withNewMetadata()
        .withName("jwtproxy-config-" + identity.getWorkspaceId())
        .endMetadata()
        .withStringData(configs)
        .done();
  }

  private void secureServer(String name, Set<String> servers, int newPort, int targetPort,
      KubernetesEnvironment k8sEnv, RuntimeIdentity identity, Map<String, String> configs)
      throws InfrastructureException {
    List<Entry<String, InternalMachineConfig>> machinesToSecure =
        k8sEnv
            .getMachines()
            .entrySet()
            .stream()
            .filter(m -> m.getValue().getServers().keySet().containsAll(servers))
            .collect(Collectors.toList());

    for (Entry<String, InternalMachineConfig> machineToSecureEntry : machinesToSecure) {
      String machineName = machineToSecureEntry.getKey();
      InternalMachineConfig machine = machineToSecureEntry.getValue();

      Map<String, ServerConfig> toSecure = new HashMap<>();
      servers.forEach(s -> toSecure.put(s, machine.getServers().get(s)));

      jwtProxyProvisioner.provision(
          machineName,
          machine,
          toSecure,
          name,
          newPort + "/tcp",
          "/config/" + name +"-config.yaml",
          k8sEnv,
          identity);

      configs.put(
          name + "-config.yaml",
          new JwtProxyConfigBuilder()
              .setListenAddress(":" + newPort)
              .setProxyUpstream("http://localhost:" +targetPort+"/")
              .setPublicKeyPath("/config/mykey.pub")
              .build());
    }
  }
}
