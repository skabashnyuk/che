package org.eclipse.che.workspace.infrastructure.kubernetes.provision;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.google.common.collect.ImmutableMap;

import java.util.Map.Entry;
import java.util.Optional;

import org.eclipse.che.api.core.model.workspace.config.MachineConfig;
import org.eclipse.che.api.core.model.workspace.config.ServerConfig;
import org.eclipse.che.api.core.model.workspace.runtime.RuntimeIdentity;
import org.eclipse.che.api.workspace.server.model.impl.ServerConfigImpl;
import org.eclipse.che.api.workspace.server.spi.InfrastructureException;
import org.eclipse.che.api.workspace.server.spi.environment.InternalMachineConfig;
import org.eclipse.che.multiuser.machine.authentication.server.signature.SignatureKeyManager;
import org.eclipse.che.workspace.infrastructure.kubernetes.KubernetesClientFactory;
import org.eclipse.che.workspace.infrastructure.kubernetes.Names;
import org.eclipse.che.workspace.infrastructure.kubernetes.environment.KubernetesEnvironment;
import org.eclipse.che.workspace.infrastructure.kubernetes.jwt.JwtProxyConfigBuilder;

import javax.inject.Inject;

import static java.util.Collections.emptyMap;

/**
 * @author Sergii Leshchenko
 */
public class JwtProxyProvisioner implements ConfigurationProvisioner<KubernetesEnvironment> {

    private static final int JWT_PROXY_MEMORY_LIMIT_BYTES = 128 * 1024 * 1024; //128mb

    private final KubernetesClientFactory kubernetesClientFactory;
    private final SignatureKeyManager     signatureKeyManager;

    @Inject
    public JwtProxyProvisioner(KubernetesClientFactory kubernetesClientFactory,
                               SignatureKeyManager signatureKeyManager) {
        this.kubernetesClientFactory = kubernetesClientFactory;
        this.signatureKeyManager = signatureKeyManager;
    }

    @Override
    public void provision(KubernetesEnvironment k8sEnv, RuntimeIdentity identity)
            throws InfrastructureException {
        KubernetesClient client = kubernetesClientFactory.create(identity.getWorkspaceId());
        byte[] encodedPublicKey = signatureKeyManager.getKeyPair().getPublic().getEncoded();
        client.secrets().createOrReplaceWithNew()
              .withNewMetadata()
              .withName("jwtproxy-config-"+identity.getWorkspaceId())
              .endMetadata()
              .withStringData(
                      ImmutableMap.of("mykey.pub", java.util.Base64.getEncoder().encodeToString(encodedPublicKey),
                                      "config.yaml", new JwtProxyConfigBuilder().setListenAddress(":4471")
                      .setProxyUpstream("http://localhost:4401/")
                      .setPublicKeyPath("/config/mykey.pub")
                                                                                .build()))
              .done();

        Optional<Entry<String, InternalMachineConfig>> wsagentMachineCfgEntryOpt =
                k8sEnv.getMachines().entrySet().stream()
                      .filter(m -> m.getValue().getServers().containsKey("wsagent/http"))
                      .findAny();

        if (wsagentMachineCfgEntryOpt.isPresent()) {
            Entry<String, InternalMachineConfig> wsagentMachineCfgEntry = wsagentMachineCfgEntryOpt.get();
            InternalMachineConfig machineCfg = wsagentMachineCfgEntry.getValue();
            String machineName = wsagentMachineCfgEntry.getKey();
            ServerConfig wsagentHttpServer = machineCfg.getServers().get("wsagent/http");
            if (wsagentHttpServer != null) {
                ServerConfigImpl securedWsagentHttp =
                        new ServerConfigImpl("4471", wsagentHttpServer.getProtocol(), wsagentHttpServer.getPath(),
                                             wsagentHttpServer.getAttributes());

                ServerConfig wsagentWsServer = machineCfg.getServers().get("wsagent/ws");
                ServerConfigImpl securedWsagentWs =
                        new ServerConfigImpl("4471", wsagentWsServer.getProtocol(), wsagentWsServer.getPath(),
                                             wsagentWsServer.getAttributes());

                machineCfg.getServers().put("wsagent/http", securedWsagentHttp);
                machineCfg.getServers().put("wsagent/ws", securedWsagentWs);

                Pod targetPod = null;

                for (Pod pod : k8sEnv.getPods().values()) {
                    for (Container container : pod.getSpec().getContainers()) {
                        if (machineName.equals(Names.machineName(pod, container))) {
                            targetPod = pod;
                        }
                    }
                }

                if (targetPod != null) {
                    addJwtProxyMachine(identity.getWorkspaceId(), k8sEnv, targetPod);
                } else {
                    throw new InfrastructureException("Container with wsagent is not found");
                }

            }
        }
    }

    private void addJwtProxyMachine(String workspaceId, KubernetesEnvironment k8sEnv, Pod pod) {
        InternalMachineConfig jwtProxyMachine = new InternalMachineConfig(null,
                                                                          ImmutableMap.of("secure-wsagent",
                                                                                          new ServerConfigImpl(
                                                                                                  "4471/tcp", "http",
                                                                                                  "/api",emptyMap())),
                                                                          emptyMap(),
                                                                          ImmutableMap
                                                                                  .of(MachineConfig.MEMORY_LIMIT_ATTRIBUTE,
                                                                                      Integer.toString(
                                                                                              JWT_PROXY_MEMORY_LIMIT_BYTES)),
                                                                          null);

        k8sEnv.getMachines().put(pod.getMetadata().getName() + "/" + "che-jwtproxy", jwtProxyMachine);

        PodSpec spec = pod.getSpec();
        spec.getVolumes().add(new VolumeBuilder().withName("jwtproxy-config-volume")
                                                 .withNewSecret()
                                                 .withSecretName("jwtproxy-config-"+workspaceId)
                                                 .endSecret()
                                                 .build());

        Container container = new ContainerBuilder().withName("che-jwtproxy")
                                                    .withImage("quay.io/coreos/jwtproxy")
                                                    .withPorts(new ContainerPort(4471, null, null, "wsagent", "TCP"))
                                                    .withVolumeMounts(
                                                            new VolumeMount("/config/", "jwtproxy-config-volume",
                                                                            false, null))
                                                    .build();

        spec.getContainers().add(container);
    }
}
