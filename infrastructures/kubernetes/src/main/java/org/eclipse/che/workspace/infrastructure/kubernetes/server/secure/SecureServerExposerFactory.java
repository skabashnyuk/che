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
package org.eclipse.che.workspace.infrastructure.kubernetes.server.secure;

import javax.inject.Inject;
import javax.inject.Named;
import org.eclipse.che.api.core.model.workspace.runtime.RuntimeIdentity;
import org.eclipse.che.workspace.infrastructure.kubernetes.environment.KubernetesEnvironment;
import org.eclipse.che.workspace.infrastructure.kubernetes.server.external.ExternalServerExposerStrategy;
import org.eclipse.che.workspace.infrastructure.kubernetes.server.secure.SecureServerExposer.DefaultSecureServerExposer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author Sergii Leshchenko */
public class SecureServerExposerFactory<T extends KubernetesEnvironment> {
  private static final Logger LOG = LoggerFactory.getLogger(SecureServerExposerFactory.class);

  private final boolean agentsAuthEnabled;
  private final String serverExposer;

  private final ExternalServerExposerStrategy<T> exposerStrategy;

  @Inject
  public SecureServerExposerFactory(
      @Named("che.agents.auth_enabled") boolean agentsAuthEnabled,
      @Named("che.agents.auth.secure_server_exposer") String serverExposer,
      ExternalServerExposerStrategy<T> exposerStrategy) {
    this.agentsAuthEnabled = agentsAuthEnabled;
    this.serverExposer = serverExposer;
    this.exposerStrategy = exposerStrategy;
  }

  /**
   * Creates instance of {@link SecureServerExposer} that will expose secure servers for runtime
   * with the specified runtime identity.
   */
  public SecureServerExposer<T> create(RuntimeIdentity identity) {
    if (!agentsAuthEnabled) {
      // return default secure server exposer because no need to protect servers with authentication
      return new DefaultSecureServerExposer<>(exposerStrategy);
    }

    switch (serverExposer) {
      case "default":
        return new DefaultSecureServerExposer<>(exposerStrategy);
      default:
        LOG.warn(
            "Unknown secure servers exposer is configured '"
                + serverExposer
                + "'. "
                + "Default one will be created.");
        return new DefaultSecureServerExposer<>(exposerStrategy);
    }
  }
}
