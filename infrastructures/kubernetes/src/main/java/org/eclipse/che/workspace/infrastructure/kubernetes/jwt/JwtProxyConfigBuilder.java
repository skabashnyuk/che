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
package org.eclipse.che.workspace.infrastructure.kubernetes.jwt;

/** @author Sergii Leshchenko */
public class JwtProxyConfigBuilder {
  private String listenAddress;
  private String proxyUpstream;
  private String publicKeyPath;

  public JwtProxyConfigBuilder setListenAddress(String listenAddress) {
    this.listenAddress = listenAddress;
    return this;
  }

  public JwtProxyConfigBuilder setProxyUpstream(String proxyUpstream) {
    this.proxyUpstream = proxyUpstream;
    return this;
  }

  public JwtProxyConfigBuilder setPublicKeyPath(String publicKeyPath) {
    this.publicKeyPath = publicKeyPath;
    return this;
  }

  public String build() {
    return String.format(
        "jwtproxy:\n"
            + "  verifier_proxies:\n"
            + "  - listen_addr: %s #:4471\n"
            + "    verifier:\n"
            + "      upstream: %s # http://localhost:8080/\n"
            + "      audience: nil\n"
            + "      max_skew: 1m\n"
            + "      max_ttl: 5m\n"
            + "      key_server:\n"
            + "        type: preshared\n"
            + "        options:\n"
            + "          issuer: jwtproxy\n"
            + "          key_id: mykey\n"
            + "          public_key_path: %s\n"
            + "      claims_verifiers:\n"
            + "      - type: static\n"
            + "        options:\n"
            + "          iss: jwtproxy\n"
            + "\n"
            + "    # Key pair used to terminate TLS.\n"
            + "    #key_file: localhost.key\n"
            + "    #crt_file: localhost.crt\n"
            + "\n"
            + "  signer_proxy:\n"
            + "    enabled: false\n",
        listenAddress, proxyUpstream, publicKeyPath);
  }
}
