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

import javax.inject.Inject;
import javax.inject.Named;
import org.eclipse.che.api.core.model.workspace.runtime.RuntimeIdentity;
import org.eclipse.che.workspace.infrastructure.kubernetes.environment.KubernetesEnvironment;
import org.eclipse.che.workspace.infrastructure.kubernetes.provision.server.secure.SecureServerExposer.DefaultSecureServerExposer;
import org.eclipse.che.workspace.infrastructure.kubernetes.server.ExternalServerExposerStrategy;
import org.eclipse.che.workspace.infrastructure.kubernetes.server.jwtproxy.JwtProxySecureServerExposerFactory;

/** @author Sergii Leshchenko */
public class SecureServerExposerFactory<T extends KubernetesEnvironment> {

  private final boolean agentsAuthEnabled;

  private final ExternalServerExposerStrategy<T> exposerStrategy;
  private final JwtProxySecureServerExposerFactory<T> jwtProxySecureServerExposerFactory;

  @Inject
  public SecureServerExposerFactory(
      @Named("che.agents.auth_enabled") boolean agentsAuthEnabled,
      ExternalServerExposerStrategy<T> exposerStrategy,
      JwtProxySecureServerExposerFactory<T> jwtProxySecureServerExposerFactory) {
    this.agentsAuthEnabled = agentsAuthEnabled;
    this.exposerStrategy = exposerStrategy;
    this.jwtProxySecureServerExposerFactory = jwtProxySecureServerExposerFactory;
  }

  public SecureServerExposer<T> create(T k8sEnv, RuntimeIdentity identity) {
    if (agentsAuthEnabled) {
      return jwtProxySecureServerExposerFactory.create(k8sEnv, identity);
    }
    return new DefaultSecureServerExposer<>(k8sEnv, exposerStrategy);
  }
}
