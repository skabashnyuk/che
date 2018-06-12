package org.eclipse.che.workspace.infrastructure.kubernetes.provision;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import java.util.List;
import java.util.Map.Entry;
import org.eclipse.che.api.core.model.workspace.runtime.RuntimeIdentity;
import org.eclipse.che.api.workspace.server.spi.InfrastructureException;
import org.eclipse.che.workspace.infrastructure.kubernetes.environment.KubernetesEnvironment;

/**
 * @author Sergii Leshchenko
 */
public class JwtProxyProvisioner implements ConfigurationProvisioner<KubernetesEnvironment> {

  @Override
  public void provision(KubernetesEnvironment k8sEnv, RuntimeIdentity identity)
      throws InfrastructureException {
    Entry<String, Pod> podEntry = k8sEnv.getPods().entrySet().iterator().next();

    Pod pod = podEntry.getValue();

    pod.getSpec().getVolumes().add(new VolumeBuilder().withName("secret-verifier-volume")
        .withNewSecret().withSecretName("secret-verifier-volume").endSecret().build());

    List<Container> containers = pod.getSpec().getContainers();

    Container container = new ContainerBuilder().withName("che-jwtproxy")
        .withImage("eclipse/che-jwtproxy")
        .withPorts(new ContainerPort(4471, null, null, "wsagent", "TCP"))
        .withVolumeMounts(new VolumeMount("/etc/jwtproxy/", "secret-verifier-volume", false, null))
        .build();

    containers.add(container);
  }
}
