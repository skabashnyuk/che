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

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.eclipse.che.commons.lang.NameGenerator.generate;
import static org.eclipse.che.workspace.infrastructure.kubernetes.Constants.CHE_ORIGINAL_NAME_LABEL;
import static org.eclipse.che.workspace.infrastructure.kubernetes.server.KubernetesServerExposer.SERVER_PREFIX;
import static org.eclipse.che.workspace.infrastructure.kubernetes.server.KubernetesServerExposer.SERVER_UNIQUE_PART_SIZE;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.che.api.core.model.workspace.config.MachineConfig;
import org.eclipse.che.api.core.model.workspace.runtime.RuntimeIdentity;
import org.eclipse.che.api.workspace.server.spi.InfrastructureException;
import org.eclipse.che.api.workspace.server.spi.InternalInfrastructureException;
import org.eclipse.che.api.workspace.server.spi.environment.InternalMachineConfig;
import org.eclipse.che.multiuser.machine.authentication.server.signature.SignatureKeyManager;
import org.eclipse.che.workspace.infrastructure.kubernetes.environment.KubernetesEnvironment;
import org.eclipse.che.workspace.infrastructure.kubernetes.server.KubernetesServerExposer;

/** @author Sergii Leshchenko */
public class JwtProxyProvisioner {

  private static final int FIRST_AVAILABLE_PORT = 4400;

  private static final int JWT_PROXY_MEMORY_LIMIT_BYTES = 128 * 1024 * 1024; // 128mb

  private static final String PUBLIC_KEY_HEADER = "-----BEGIN PUBLIC KEY-----\n";
  private static final String PUBLIC_KEY_FOOTER = "\n-----END PUBLIC KEY-----";

  public static final String JWT_PROXY_CONFIG_FOLDER = "/config";
  public static final String JWT_PROXY_CONFIG_FILE = "config.yaml";
  public static final String JWT_PROXY_PUBLIC_KEY_FILE = "mykey.pub";
  public static final String JWT_PROXY_MACHINE_NAME = "jwtproxy";

  private final SignatureKeyManager signatureKeyManager;

  private final RuntimeIdentity identity;
  private final KubernetesEnvironment k8sEnv;

  private final JwtProxyConfigBuilder proxyConfigBuilder;

  private final String serviceName;
  private int availablePort;

  public JwtProxyProvisioner(
      KubernetesEnvironment k8sEnv,
      RuntimeIdentity identity,
      SignatureKeyManager signatureKeyManager) {
    this.signatureKeyManager = signatureKeyManager;

    this.k8sEnv = k8sEnv;
    this.identity = identity;

    this.proxyConfigBuilder = new JwtProxyConfigBuilder();

    this.serviceName = generate(SERVER_PREFIX, SERVER_UNIQUE_PART_SIZE) + "-jwtproxy";
    this.availablePort = FIRST_AVAILABLE_PORT;
  }

  public ServicePort expose(String backendServiceName, int backendServicePort, String protocol)
      throws InfrastructureException {
    ensureJwtProxyInjected();

    int listenPort = availablePort++;

    proxyConfigBuilder.addVerifierProxy(
        listenPort, "http://" + backendServiceName + ":" + backendServicePort);
    k8sEnv
        .getSecrets()
        .get(getSecretName())
        .getStringData()
        .put(JWT_PROXY_CONFIG_FILE, proxyConfigBuilder.build());

    ServicePort exposedPort =
        new ServicePortBuilder()
            .withName(backendServiceName + "-" + listenPort)
            .withPort(listenPort)
            .withProtocol(protocol)
            .withNewTargetPort(listenPort)
            .build();

    k8sEnv.getServices().get(getServiceName()).getSpec().getPorts().add(exposedPort);

    return exposedPort;
  }

  public String getServiceName() {
    return serviceName;
  }

  public String getSecretName() {
    return "jwtproxy-config-" + identity.getWorkspaceId();
  }

  private void ensureJwtProxyInjected() throws InfrastructureException {
    if (!k8sEnv.getMachines().containsKey(JWT_PROXY_MACHINE_NAME)) {
      k8sEnv.getMachines().put(JWT_PROXY_MACHINE_NAME, createJwtProxyMachine());
      k8sEnv.getPods().put("jwtproxy", createJwtProxyPod(identity));

      KeyPair keyPair = signatureKeyManager.getKeyPair();
      if (keyPair == null) {
        throw new InternalInfrastructureException(
            "Key pair for machine authentication does not exist");
      }
      Map<String, String> initSecretData = new HashMap<>();
      initSecretData.put(
          JWT_PROXY_PUBLIC_KEY_FILE,
          PUBLIC_KEY_HEADER
              + java.util.Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded())
              + PUBLIC_KEY_FOOTER);

      initSecretData.put(JWT_PROXY_CONFIG_FILE, proxyConfigBuilder.build());

      Secret jwtProxySecret =
          new SecretBuilder()
              .withNewMetadata()
              .withName(getSecretName())
              .endMetadata()
              .withStringData(initSecretData)
              .build();
      k8sEnv.getSecrets().put(jwtProxySecret.getMetadata().getName(), jwtProxySecret);

      Service jwtProxyService =
          new KubernetesServerExposer.ServiceBuilder()
              .withName(serviceName)
              .withSelectorEntry(CHE_ORIGINAL_NAME_LABEL, JWT_PROXY_MACHINE_NAME)
              .withMachineName(JWT_PROXY_MACHINE_NAME)
              .withPorts(emptyList())
              .build();
      k8sEnv.getServices().put(jwtProxyService.getMetadata().getName(), jwtProxyService);
    }
  }

  private InternalMachineConfig createJwtProxyMachine() {
    return new InternalMachineConfig(
        null,
        emptyMap(),
        emptyMap(),
        ImmutableMap.of(
            MachineConfig.MEMORY_LIMIT_ATTRIBUTE, Integer.toString(JWT_PROXY_MEMORY_LIMIT_BYTES)),
        null);
  }

  private Pod createJwtProxyPod(RuntimeIdentity identity) {
    return new PodBuilder()
        .withNewMetadata()
        .withName("jwtproxy")
        .withAnnotations(
            ImmutableMap.of(
                "org.eclipse.che.container.verifier.machine_name", JWT_PROXY_MACHINE_NAME))
        .endMetadata()
        .withNewSpec()
        .withContainers(
            new ContainerBuilder()
                .withName("verifier")
                .withImage("mshaposh/jwtproxy")
                .withVolumeMounts(
                    new VolumeMount(
                        JWT_PROXY_CONFIG_FOLDER + "/", "jwtproxy-config-volume", false, null))
                .withArgs("-config", JWT_PROXY_CONFIG_FOLDER + "/" + JWT_PROXY_CONFIG_FILE)
                .build())
        .withVolumes(
            new VolumeBuilder()
                .withName("jwtproxy-config-volume")
                .withNewSecret()
                .withSecretName("jwtproxy-config-" + identity.getWorkspaceId())
                .endSecret()
                .build())
        .endSpec()
        .build();
  }
}
