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
package org.eclipse.che.workspace.infrastructure.kubernetes.server.jwtproxy;

import static java.lang.Integer.parseInt;
import static java.util.stream.Collectors.toMap;

import com.google.inject.assistedinject.Assisted;
import io.fabric8.kubernetes.api.model.ServicePort;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.inject.Inject;
import org.eclipse.che.api.core.model.workspace.config.ServerConfig;
import org.eclipse.che.api.core.model.workspace.runtime.RuntimeIdentity;
import org.eclipse.che.api.workspace.server.model.impl.ServerConfigImpl;
import org.eclipse.che.api.workspace.server.spi.InfrastructureException;
import org.eclipse.che.multiuser.machine.authentication.server.signature.SignatureKeyManager;
import org.eclipse.che.workspace.infrastructure.kubernetes.environment.KubernetesEnvironment;
import org.eclipse.che.workspace.infrastructure.kubernetes.provision.server.secure.SecureServerExposer;
import org.eclipse.che.workspace.infrastructure.kubernetes.server.ExternalServerExposerStrategy;

/** @author Sergii Leshchenko */
public class JwtProxySecureServerExposer<T extends KubernetesEnvironment>
    implements SecureServerExposer<T> {

  private T k8sEnv;

  private ExternalServerExposerStrategy<T> exposerStrategy;

  private final JwtProxyProvisioner proxyProvisioner;

  @Inject
  public JwtProxySecureServerExposer(
      @Assisted T k8sEnv,
      @Assisted RuntimeIdentity identity,
      SignatureKeyManager signatureKeyManager,
      ExternalServerExposerStrategy<T> exposerStrategy) {
    this.k8sEnv = k8sEnv;
    this.exposerStrategy = exposerStrategy;

    proxyProvisioner = new JwtProxyProvisioner(k8sEnv, identity, signatureKeyManager);
  }

  // TODO Describe in PR description architecture and why it is done in this way
  // Describe extension point, how it is works for OpenShift

  // TODO Describe why it is needed to intoduce secure servers

  @Override
  public void expose(
      String machineName,
      String serviceName,
      Map<String, ServicePort> portToServicePort,
      Map<String, ServerConfig> secureServers)
      throws InfrastructureException {
    for (ServicePort servicePort : portToServicePort.values()) {
      int port = servicePort.getTargetPort().getIntVal();
      Map<String, ServerConfig> portServers =
          secureServers
              .entrySet()
              .stream()
              .filter(e -> parseInt(e.getValue().getPort().split("/")[0]) == port)
              .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

      if (portServers.isEmpty()) {
        // no servers found
        continue;
      }

      doCover(machineName, serviceName, servicePort, portServers);
    }
  }

  private void doCover(
      String machineName,
      String serviceName,
      ServicePort servicePort,
      Map<String, ServerConfig> ingressServers)
      throws InfrastructureException {
    ServicePort exposedServicePort =
        proxyProvisioner.expose(
            serviceName, servicePort.getTargetPort().getIntVal(), servicePort.getProtocol());

    Map<String, ServerConfig> securedServers = new HashMap<>();
    for (Entry<String, ServerConfig> serverConfigEntry : ingressServers.entrySet()) {
      ServerConfig server = serverConfigEntry.getValue();
      String serverName = serverConfigEntry.getKey();

      securedServers.put(
          serverName,
          new ServerConfigImpl(
              exposedServicePort.getPort() + "/" + exposedServicePort.getProtocol().toLowerCase(),
              server.getProtocol(),
              server.getPath(),
              server.getAttributes()));
    }

    exposerStrategy.expose(
        k8sEnv, machineName, proxyProvisioner.getServiceName(), exposedServicePort, securedServers);
  }
}
