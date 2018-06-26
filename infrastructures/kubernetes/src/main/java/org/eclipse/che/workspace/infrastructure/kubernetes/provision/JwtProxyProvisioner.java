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

import static java.util.Collections.emptyMap;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import java.util.Map;
import org.eclipse.che.api.core.model.workspace.config.MachineConfig;
import org.eclipse.che.api.core.model.workspace.config.ServerConfig;
import org.eclipse.che.api.core.model.workspace.runtime.RuntimeIdentity;
import org.eclipse.che.api.workspace.server.model.impl.ServerConfigImpl;
import org.eclipse.che.api.workspace.server.spi.InfrastructureException;
import org.eclipse.che.api.workspace.server.spi.environment.InternalMachineConfig;
import org.eclipse.che.workspace.infrastructure.kubernetes.Names;
import org.eclipse.che.workspace.infrastructure.kubernetes.environment.KubernetesEnvironment;

/** @author Sergii Leshchenko */
public class JwtProxyProvisioner {

  private static final int JWT_PROXY_MEMORY_LIMIT_BYTES = 128 * 1024 * 1024; // 128mb

  public void provision(
      String machineName,
      InternalMachineConfig machineCfg,
      Map<String, ServerConfig> servers,
      String serverName,
      String securedPort,
      String configFilePath,
      KubernetesEnvironment k8sEnv,
      RuntimeIdentity identity)
      throws InfrastructureException {

    servers.forEach(
        (key, serverConfig) ->
            machineCfg
                .getServers()
                .put(
                    key,
                    new ServerConfigImpl(
                        securedPort,
                        serverConfig.getProtocol(),
                        serverConfig.getPath(),
                        serverConfig.getAttributes())));
    Pod targetPod = null;
    for (Pod pod : k8sEnv.getPods().values()) {
      for (Container container : pod.getSpec().getContainers()) {
        if (machineName.equals(Names.machineName(pod, container))) {
          targetPod = pod;
        }
      }
    }

    if (targetPod != null) {
      addJwtProxyMachine(identity.getWorkspaceId(), k8sEnv, targetPod, serverName, configFilePath);
    } else {
      throw new InfrastructureException(
          "The corresponding container for machine '" + machineName + "' is not found");
    }
  }

  private void addJwtProxyMachine(
      String workspaceId,
      KubernetesEnvironment k8sEnv,
      Pod pod,
      String serverName,
      String configFilePath) {
    InternalMachineConfig jwtProxyMachine =
        new InternalMachineConfig(
            null,
            emptyMap(),
            emptyMap(),
            ImmutableMap.of(
                MachineConfig.MEMORY_LIMIT_ATTRIBUTE,
                Integer.toString(JWT_PROXY_MEMORY_LIMIT_BYTES)),
            null);

    k8sEnv
        .getMachines()
        .put(pod.getMetadata().getName() + "/" + serverName + "-jwtproxy", jwtProxyMachine);

    PodSpec spec = pod.getSpec();
    if (spec.getVolumes().stream().noneMatch(v -> v.getName().equals("jwtproxy-config-volume"))) {
      spec.getVolumes()
          .add(
              new VolumeBuilder()
                  .withName("jwtproxy-config-volume")
                  .withNewSecret()
                  .withSecretName("jwtproxy-config-" + workspaceId)
                  .endSecret()
                  .build());
    }

    Container container =
        new ContainerBuilder()
            .withName(serverName + "-jwtproxy")
            .withImage("mshaposh/jwtproxy2")
            .withPorts(new ContainerPort(4471, null, null, "wsagent", "TCP"))
            .withVolumeMounts(new VolumeMount("/config/", "jwtproxy-config-volume", false, null))
            .withArgs("-config", configFilePath)
            .build();

    spec.getContainers().add(container);
  }
}
