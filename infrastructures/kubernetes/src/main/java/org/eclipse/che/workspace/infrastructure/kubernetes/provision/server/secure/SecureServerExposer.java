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
package org.eclipse.che.workspace.infrastructure.kubernetes.provision.server.secure;

import io.fabric8.kubernetes.api.model.ServicePort;
import java.util.Map;
import org.eclipse.che.api.core.model.workspace.config.ServerConfig;
import org.eclipse.che.api.workspace.server.spi.InfrastructureException;
import org.eclipse.che.workspace.infrastructure.kubernetes.environment.KubernetesEnvironment;
import org.eclipse.che.workspace.infrastructure.kubernetes.server.ExternalServerExposerStrategy;

/** @author Sergii Leshchenko */
public interface SecureServerExposer<T extends KubernetesEnvironment> {

  void expose(
      String machineName,
      String serviceName,
      Map<String, ServicePort> portToServicePort,
      Map<String, ServerConfig> secureServers)
      throws InfrastructureException;

  class DefaultSecureServerExposer<X extends KubernetesEnvironment>
      implements SecureServerExposer<X> {
    private ExternalServerExposerStrategy<X> exposerStrategy;
    private X k8sEnv;

    public DefaultSecureServerExposer(X k8sEnv, ExternalServerExposerStrategy<X> exposerStrategy) {
      this.exposerStrategy = exposerStrategy;
      this.k8sEnv = k8sEnv;
    }

    @Override
    public void expose(
        String machineName,
        String serviceName,
        Map<String, ServicePort> portToServicePort,
        Map<String, ServerConfig> secureServers) {
      exposerStrategy.expose(k8sEnv, machineName, serviceName, portToServicePort, secureServers);
    }
  }
}
