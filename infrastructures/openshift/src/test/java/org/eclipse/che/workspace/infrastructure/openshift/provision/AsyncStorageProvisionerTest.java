/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.workspace.infrastructure.openshift.provision;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.UUID.randomUUID;
import static org.eclipse.che.api.workspace.shared.Constants.ASYNC_PERSIST_ATTRIBUTE;
import static org.eclipse.che.api.workspace.shared.Constants.PERSIST_VOLUMES_ATTRIBUTE;
import static org.eclipse.che.workspace.infrastructure.openshift.provision.AsyncStorageProvisioner.ASYNC_STORAGE;
import static org.eclipse.che.workspace.infrastructure.openshift.provision.AsyncStorageProvisioner.ASYNC_STORAGE_CONFIG;
import static org.eclipse.che.workspace.infrastructure.openshift.provision.AsyncStorageProvisioner.SSH_KEY_NAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.DoneableConfigMap;
import io.fabric8.kubernetes.api.model.DoneablePersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.DoneablePod;
import io.fabric8.kubernetes.api.model.DoneableService;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.ServiceResource;
import io.fabric8.openshift.client.OpenShiftClient;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.workspace.runtime.RuntimeIdentity;
import org.eclipse.che.api.ssh.server.SshManager;
import org.eclipse.che.api.ssh.server.model.impl.SshPairImpl;
import org.eclipse.che.api.workspace.server.spi.InfrastructureException;
import org.eclipse.che.workspace.infrastructure.openshift.OpenShiftClientFactory;
import org.eclipse.che.workspace.infrastructure.openshift.environment.OpenShiftEnvironment;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

@Listeners(MockitoTestNGListener.class)
public class AsyncStorageProvisionerTest {

  private static final String WORKSPACE_ID = UUID.randomUUID().toString();
  private static final String NAMESPACE = UUID.randomUUID().toString();
  private static final String CONFIGMAP_NAME = NAMESPACE + ASYNC_STORAGE_CONFIG;
  private static final String VPC_NAME = UUID.randomUUID().toString();
  private static final String USER = "user";

  @Mock private OpenShiftEnvironment openShiftEnvironment;
  @Mock private RuntimeIdentity identity;
  @Mock private OpenShiftClientFactory clientFactory;
  @Mock private OpenShiftClient osClient;
  @Mock private SshManager sshManager;
  @Mock private Resource<PersistentVolumeClaim, DoneablePersistentVolumeClaim> pvcResource;
  @Mock private Resource<ConfigMap, DoneableConfigMap> mapResource;
  @Mock private PodResource<Pod, DoneablePod> podResource;
  @Mock private ServiceResource<Service, DoneableService> serviceResource;
  @Mock private MixedOperation mixedOperationPvc;
  @Mock private MixedOperation mixedOperationConfigMap;
  @Mock private MixedOperation mixedOperationPod;
  @Mock private MixedOperation mixedOperationService;
  @Mock private NonNamespaceOperation namespacePvcOperation;
  @Mock private NonNamespaceOperation namespaceConfigMapOperation;
  @Mock private NonNamespaceOperation namespacePodOperation;
  @Mock private NonNamespaceOperation namespaceServiceOperation;

  private Map<String, String> attributes;
  private AsyncStorageProvisioner asyncStorageProvisioner;
  private SshPairImpl sshPair;

  @BeforeMethod
  public void setUp() {
    asyncStorageProvisioner =
        new AsyncStorageProvisioner(
            "Always",
            "10Gi",
            "org/image:tag",
            "ReadWriteOnce",
            "common",
            VPC_NAME,
            "storage",
            sshManager,
            clientFactory);
    attributes = new HashMap<>(2);
    attributes.put(ASYNC_PERSIST_ATTRIBUTE, "true");
    attributes.put(PERSIST_VOLUMES_ATTRIBUTE, "false");
    sshPair = new SshPairImpl(USER, "internal", SSH_KEY_NAME, "", "");
  }

  @Test(expectedExceptions = InfrastructureException.class)
  public void shouldThrowExceptionIfNotCommonStrategy() throws Exception {
    AsyncStorageProvisioner asyncStorageProvisioner =
        new AsyncStorageProvisioner(
            "Always",
            "10Gi",
            "org/image:tag",
            "ReadWriteOnce",
            randomUUID().toString(),
            VPC_NAME,
            "storageClass",
            sshManager,
            clientFactory);
    when(openShiftEnvironment.getAttributes()).thenReturn(attributes);
    asyncStorageProvisioner.provision(openShiftEnvironment, identity);
    verifyNoMoreInteractions(sshManager);
    verifyNoMoreInteractions(clientFactory);
    verifyNoMoreInteractions(identity);
  }

  @Test(expectedExceptions = InfrastructureException.class)
  public void shouldThrowExceptionIfAsyncStorageForNotEphemeralWorkspace() throws Exception {
    Map attributes = new HashMap<>(2);
    attributes.put(ASYNC_PERSIST_ATTRIBUTE, "true");
    attributes.put(PERSIST_VOLUMES_ATTRIBUTE, "true");
    when(openShiftEnvironment.getAttributes()).thenReturn(attributes);
    asyncStorageProvisioner.provision(openShiftEnvironment, identity);
    verifyNoMoreInteractions(sshManager);
    verifyNoMoreInteractions(clientFactory);
    verifyNoMoreInteractions(identity);
  }

  @Test
  public void shouldDoNothingIfNotSetAttribute() throws InfrastructureException {
    when(openShiftEnvironment.getAttributes()).thenReturn(emptyMap());
    asyncStorageProvisioner.provision(openShiftEnvironment, identity);
    verifyNoMoreInteractions(sshManager);
    verifyNoMoreInteractions(clientFactory);
    verifyNoMoreInteractions(identity);
  }

  @Test
  public void shouldDoNothingIfAttributesAsyncPersistOnly() throws InfrastructureException {
    when(openShiftEnvironment.getAttributes())
        .thenReturn(singletonMap(PERSIST_VOLUMES_ATTRIBUTE, "false"));
    asyncStorageProvisioner.provision(openShiftEnvironment, identity);
    verifyNoMoreInteractions(sshManager);
    verifyNoMoreInteractions(clientFactory);
    verifyNoMoreInteractions(identity);
  }

  @Test
  public void shouldCreateAll() throws InfrastructureException, ServerException, ConflictException {
    when(openShiftEnvironment.getAttributes()).thenReturn(attributes);
    when(clientFactory.create(anyString())).thenReturn(osClient);
    when(identity.getWorkspaceId()).thenReturn(WORKSPACE_ID);
    when(identity.getInfrastructureNamespace()).thenReturn(NAMESPACE);
    when(identity.getOwnerId()).thenReturn(USER);
    when(sshManager.getPairs(USER, "internal")).thenReturn(singletonList(sshPair));

    when(osClient.persistentVolumeClaims()).thenReturn(mixedOperationPvc);
    when(mixedOperationPvc.inNamespace(NAMESPACE)).thenReturn(namespacePvcOperation);
    when(namespacePvcOperation.withName(VPC_NAME)).thenReturn(pvcResource);
    when(pvcResource.get()).thenReturn(null);

    when(osClient.configMaps()).thenReturn(mixedOperationConfigMap);
    when(mixedOperationConfigMap.inNamespace(NAMESPACE)).thenReturn(namespaceConfigMapOperation);
    when(namespaceConfigMapOperation.withName(anyString())).thenReturn(mapResource);
    when(mapResource.get()).thenReturn(null);

    when(osClient.pods()).thenReturn(mixedOperationPod);
    when(mixedOperationPod.inNamespace(NAMESPACE)).thenReturn(namespacePodOperation);
    when(namespacePodOperation.withName(ASYNC_STORAGE)).thenReturn(podResource);
    when(podResource.get()).thenReturn(null);

    when(osClient.services()).thenReturn(mixedOperationService);
    when(mixedOperationService.inNamespace(NAMESPACE)).thenReturn(namespaceServiceOperation);
    when(namespaceServiceOperation.withName(ASYNC_STORAGE)).thenReturn(serviceResource);
    when(serviceResource.get()).thenReturn(null);

    asyncStorageProvisioner.provision(openShiftEnvironment, identity);
    verify(identity, times(1)).getInfrastructureNamespace();
    verify(identity, times(1)).getOwnerId();
    verify(sshManager, times(1)).getPairs(USER, "internal");
    verify(sshManager, never()).generatePair(USER, "internal", SSH_KEY_NAME);
    verify(osClient.services().inNamespace(NAMESPACE), times(1)).create(any(Service.class));
    verify(osClient.configMaps().inNamespace(NAMESPACE), times(1)).create(any(ConfigMap.class));
    verify(osClient.pods().inNamespace(NAMESPACE), times(1)).create(any(Pod.class));
    verify(osClient.persistentVolumeClaims().inNamespace(NAMESPACE), times(1))
        .create(any(PersistentVolumeClaim.class));
  }

  @Test
  public void shouldNotCreateConfigMap()
      throws InfrastructureException, ServerException, ConflictException {
    when(openShiftEnvironment.getAttributes()).thenReturn(attributes);
    when(clientFactory.create(anyString())).thenReturn(osClient);
    when(identity.getWorkspaceId()).thenReturn(WORKSPACE_ID);
    when(identity.getInfrastructureNamespace()).thenReturn(NAMESPACE);
    when(identity.getOwnerId()).thenReturn(USER);

    when(osClient.persistentVolumeClaims()).thenReturn(mixedOperationPvc);
    when(mixedOperationPvc.inNamespace(NAMESPACE)).thenReturn(namespacePvcOperation);
    when(namespacePvcOperation.withName(VPC_NAME)).thenReturn(pvcResource);
    when(pvcResource.get()).thenReturn(null);

    when(osClient.configMaps()).thenReturn(mixedOperationConfigMap);
    when(mixedOperationConfigMap.inNamespace(NAMESPACE)).thenReturn(namespaceConfigMapOperation);
    when(namespaceConfigMapOperation.withName(CONFIGMAP_NAME)).thenReturn(mapResource);
    ObjectMeta meta = new ObjectMeta();
    meta.setName(CONFIGMAP_NAME);
    ConfigMap configMap = new ConfigMap();
    configMap.setMetadata(meta);
    when(mapResource.get()).thenReturn(configMap);

    when(osClient.pods()).thenReturn(mixedOperationPod);
    when(mixedOperationPod.inNamespace(NAMESPACE)).thenReturn(namespacePodOperation);
    when(namespacePodOperation.withName(ASYNC_STORAGE)).thenReturn(podResource);
    when(podResource.get()).thenReturn(null);

    when(osClient.services()).thenReturn(mixedOperationService);
    when(mixedOperationService.inNamespace(NAMESPACE)).thenReturn(namespaceServiceOperation);
    when(namespaceServiceOperation.withName(ASYNC_STORAGE)).thenReturn(serviceResource);
    when(serviceResource.get()).thenReturn(null);

    asyncStorageProvisioner.provision(openShiftEnvironment, identity);
    verify(identity, times(1)).getInfrastructureNamespace();
    verify(identity, times(1)).getOwnerId();
    verify(identity, times(1)).getWorkspaceId();
    verify(sshManager, never()).getPairs(USER, "internal");
    verify(sshManager, never()).generatePair(USER, "internal", SSH_KEY_NAME);
    verify(osClient.services().inNamespace(NAMESPACE), times(1)).create(any(Service.class));
    verify(osClient.configMaps().inNamespace(NAMESPACE), never()).create(any(ConfigMap.class));
    verify(osClient.pods().inNamespace(NAMESPACE), times(1)).create(any(Pod.class));
    verify(osClient.persistentVolumeClaims().inNamespace(NAMESPACE), times(1))
        .create(any(PersistentVolumeClaim.class));
  }

  @Test
  public void shouldNotCreatePod()
      throws InfrastructureException, ServerException, ConflictException {
    when(openShiftEnvironment.getAttributes()).thenReturn(attributes);
    when(clientFactory.create(anyString())).thenReturn(osClient);
    when(identity.getWorkspaceId()).thenReturn(WORKSPACE_ID);
    when(identity.getInfrastructureNamespace()).thenReturn(NAMESPACE);
    when(identity.getOwnerId()).thenReturn(USER);
    when(sshManager.getPairs(USER, "internal")).thenReturn(singletonList(sshPair));

    when(osClient.persistentVolumeClaims()).thenReturn(mixedOperationPvc);
    when(mixedOperationPvc.inNamespace(NAMESPACE)).thenReturn(namespacePvcOperation);
    when(namespacePvcOperation.withName(VPC_NAME)).thenReturn(pvcResource);
    when(pvcResource.get()).thenReturn(null);

    when(osClient.configMaps()).thenReturn(mixedOperationConfigMap);
    when(mixedOperationConfigMap.inNamespace(NAMESPACE)).thenReturn(namespaceConfigMapOperation);
    when(namespaceConfigMapOperation.withName(CONFIGMAP_NAME)).thenReturn(mapResource);
    when(mapResource.get()).thenReturn(null);

    when(osClient.pods()).thenReturn(mixedOperationPod);
    when(mixedOperationPod.inNamespace(NAMESPACE)).thenReturn(namespacePodOperation);
    when(namespacePodOperation.withName(ASYNC_STORAGE)).thenReturn(podResource);
    ObjectMeta meta = new ObjectMeta();
    meta.setName(ASYNC_STORAGE);
    Pod pod = new Pod();
    pod.setMetadata(meta);
    when(podResource.get()).thenReturn(pod);

    when(osClient.services()).thenReturn(mixedOperationService);
    when(mixedOperationService.inNamespace(NAMESPACE)).thenReturn(namespaceServiceOperation);
    when(namespaceServiceOperation.withName(ASYNC_STORAGE)).thenReturn(serviceResource);
    when(serviceResource.get()).thenReturn(null);

    asyncStorageProvisioner.provision(openShiftEnvironment, identity);
    verify(identity, times(1)).getInfrastructureNamespace();
    verify(identity, times(1)).getOwnerId();
    verify(sshManager, times(1)).getPairs(USER, "internal");
    verify(sshManager, never()).generatePair(USER, "internal", SSH_KEY_NAME);
    verify(osClient.services().inNamespace(NAMESPACE), times(1)).create(any(Service.class));
    verify(osClient.configMaps().inNamespace(NAMESPACE), times(1)).create(any(ConfigMap.class));
    verify(osClient.pods().inNamespace(NAMESPACE), never()).create(any(Pod.class));
    verify(osClient.persistentVolumeClaims().inNamespace(NAMESPACE), times(1))
        .create(any(PersistentVolumeClaim.class));
  }

  @Test
  public void shouldNotCreateService()
      throws InfrastructureException, ServerException, ConflictException {
    when(openShiftEnvironment.getAttributes()).thenReturn(attributes);
    when(clientFactory.create(anyString())).thenReturn(osClient);
    when(identity.getWorkspaceId()).thenReturn(WORKSPACE_ID);
    when(identity.getInfrastructureNamespace()).thenReturn(NAMESPACE);
    when(identity.getOwnerId()).thenReturn(USER);
    when(sshManager.getPairs(USER, "internal")).thenReturn(singletonList(sshPair));

    when(osClient.persistentVolumeClaims()).thenReturn(mixedOperationPvc);
    when(mixedOperationPvc.inNamespace(NAMESPACE)).thenReturn(namespacePvcOperation);
    when(namespacePvcOperation.withName(VPC_NAME)).thenReturn(pvcResource);
    when(pvcResource.get()).thenReturn(null);

    when(osClient.configMaps()).thenReturn(mixedOperationConfigMap);
    when(mixedOperationConfigMap.inNamespace(NAMESPACE)).thenReturn(namespaceConfigMapOperation);
    when(namespaceConfigMapOperation.withName(CONFIGMAP_NAME)).thenReturn(mapResource);
    when(mapResource.get()).thenReturn(null);

    when(osClient.pods()).thenReturn(mixedOperationPod);
    when(mixedOperationPod.inNamespace(NAMESPACE)).thenReturn(namespacePodOperation);
    when(namespacePodOperation.withName(ASYNC_STORAGE)).thenReturn(podResource);
    when(podResource.get()).thenReturn(null);

    when(osClient.services()).thenReturn(mixedOperationService);
    when(mixedOperationService.inNamespace(NAMESPACE)).thenReturn(namespaceServiceOperation);
    when(namespaceServiceOperation.withName(ASYNC_STORAGE)).thenReturn(serviceResource);
    ObjectMeta meta = new ObjectMeta();
    meta.setName(ASYNC_STORAGE);
    Service service = new Service();
    service.setMetadata(meta);
    when(serviceResource.get()).thenReturn(service);

    asyncStorageProvisioner.provision(openShiftEnvironment, identity);
    verify(identity, times(1)).getInfrastructureNamespace();
    verify(identity, times(1)).getOwnerId();
    verify(sshManager, times(1)).getPairs(USER, "internal");
    verify(sshManager, never()).generatePair(USER, "internal", SSH_KEY_NAME);
    verify(osClient.services().inNamespace(NAMESPACE), never()).create(any(Service.class));
    verify(osClient.configMaps().inNamespace(NAMESPACE), times(1)).create(any(ConfigMap.class));
    verify(osClient.pods().inNamespace(NAMESPACE), times(1)).create(any(Pod.class));
    verify(osClient.persistentVolumeClaims().inNamespace(NAMESPACE), times(1))
        .create(any(PersistentVolumeClaim.class));
  }
}
