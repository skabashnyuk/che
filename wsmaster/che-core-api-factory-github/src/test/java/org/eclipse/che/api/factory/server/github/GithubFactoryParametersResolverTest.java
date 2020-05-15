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
package org.eclipse.che.api.factory.server.github;

import static java.util.Collections.singletonMap;
import static org.eclipse.che.api.factory.shared.Constants.CURRENT_VERSION;
import static org.eclipse.che.api.factory.shared.Constants.URL_PARAMETER_NAME;
import static org.eclipse.che.api.workspace.server.devfile.Constants.CURRENT_API_VERSION;
import static org.eclipse.che.dto.server.DtoFactory.newDto;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Optional;
import org.eclipse.che.api.factory.server.urlfactory.RemoteFactoryUrl;
import org.eclipse.che.api.factory.server.urlfactory.URLFactoryBuilder;
import org.eclipse.che.api.factory.shared.dto.FactoryDto;
import org.eclipse.che.api.workspace.server.devfile.FileContentProvider;
import org.eclipse.che.api.workspace.shared.dto.devfile.DevfileDto;
import org.eclipse.che.api.workspace.shared.dto.devfile.MetadataDto;
import org.eclipse.che.api.workspace.shared.dto.devfile.ProjectDto;
import org.eclipse.che.api.workspace.shared.dto.devfile.SourceDto;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/**
 * Validate operations performed by the Github Factory service
 *
 * @author Florent Benoit
 */
@Listeners(MockitoTestNGListener.class)
public class GithubFactoryParametersResolverTest {

  /** Parser which will allow to check validity of URLs and create objects. */
  @Spy private GithubURLParser githubUrlParser = new GithubURLParser();

  /** Converter allowing to convert github URL to other objects. */
  @Spy
  private GithubSourceStorageBuilder githubSourceStorageBuilder = new GithubSourceStorageBuilder();

  /** Parser which will allow to check validity of URLs and create objects. */
  @Mock private URLFactoryBuilder urlFactoryBuilder;

  /**
   * Capturing the location parameter when calling {@link
   * URLFactoryBuilder#createFactoryFromJson(RemoteFactoryUrl)} or {@link
   * URLFactoryBuilder#createFactoryFromDevfile(RemoteFactoryUrl, FileContentProvider, Map)}
   */
  @Captor private ArgumentCaptor<RemoteFactoryUrl> factoryUrlArgumentCaptor;

  /** Instance of resolver that will be tested. */
  @InjectMocks private GithubFactoryParametersResolver githubFactoryParametersResolver;

  /** Check missing parameter name can't be accepted by this resolver */
  @Test
  public void checkMissingParameter() {
    Map<String, String> parameters = singletonMap("foo", "this is a foo bar");
    boolean accept = githubFactoryParametersResolver.accept(parameters);
    // shouldn't be accepted
    assertFalse(accept);
  }

  /** Check url which is not a github url can't be accepted by this resolver */
  @Test
  public void checkInvalidAcceptUrl() {
    Map<String, String> parameters = singletonMap(URL_PARAMETER_NAME, "http://www.eclipse.org/che");
    boolean accept = githubFactoryParametersResolver.accept(parameters);
    // shouldn't be accepted
    assertFalse(accept);
  }

  /** Check github url will be be accepted by this resolver */
  @Test
  public void checkValidAcceptUrl() {
    Map<String, String> parameters =
        singletonMap(URL_PARAMETER_NAME, "https://github.com/codenvy/codenvy.git");
    boolean accept = githubFactoryParametersResolver.accept(parameters);
    // shouldn't be accepted
    assertTrue(accept);
  }

  @Test
  public void shouldGenerateDevfileForFactoryWithNoDevfileOrJson() throws Exception {

    String githubUrl = "https://github.com/eclipse/che";

    FactoryDto computedFactory = generateDevfileFactory();

    when(urlFactoryBuilder.buildDefaultDevfile(any())).thenReturn(computedFactory.getDevfile());

    when(urlFactoryBuilder.createFactoryFromJson(any(RemoteFactoryUrl.class)))
        .thenReturn(Optional.empty());
    when(urlFactoryBuilder.createFactoryFromDevfile(any(RemoteFactoryUrl.class), any(), anyMap()))
        .thenReturn(Optional.empty());
    Map<String, String> params = ImmutableMap.of(URL_PARAMETER_NAME, githubUrl);
    // when
    FactoryDto factory = githubFactoryParametersResolver.createFactory(params);
    // then
    verify(urlFactoryBuilder).buildDefaultDevfile(eq("che"));
    assertEquals(factory, computedFactory);
    SourceDto source = factory.getDevfile().getProjects().get(0).getSource();
    assertEquals(source.getLocation(), githubUrl + ".git");
    assertEquals(source.getBranch(), "master");
  }

  @Test
  public void shouldReturnFactoryFromRepositoryWithDevfile() throws Exception {

    String githubUrl = "https://github.com/eclipse/che";

    FactoryDto computedFactory = generateDevfileFactory();

    when(urlFactoryBuilder.createFactoryFromDevfile(any(RemoteFactoryUrl.class), any(), anyMap()))
        .thenReturn(Optional.of(computedFactory));

    Map<String, String> params = ImmutableMap.of(URL_PARAMETER_NAME, githubUrl);
    // when
    FactoryDto factory = githubFactoryParametersResolver.createFactory(params);
    // then
    assertNotNull(factory.getDevfile());

    // check we called the builder with the following devfile file
    verify(urlFactoryBuilder)
        .createFactoryFromDevfile(factoryUrlArgumentCaptor.capture(), any(), anyMap());
    verify(urlFactoryBuilder, never()).buildDefaultDevfile(eq("che"));
    assertEquals(
        factoryUrlArgumentCaptor.getValue().devfileFileLocation(),
        "https://raw.githubusercontent.com/eclipse/che/master/devfile.yaml");

    assertEquals(factoryUrlArgumentCaptor.getValue().getDevfileFilename(), "devfile.yaml");
  }

  @Test
  public void shouldSetDefaultProjectIntoDevfileIfNotSpecified() throws Exception {

    String githubUrl = "https://github.com/eclipse/che/tree/foobar";

    FactoryDto computedFactory = generateDevfileFactory();

    when(urlFactoryBuilder.createFactoryFromDevfile(any(RemoteFactoryUrl.class), any(), anyMap()))
        .thenReturn(Optional.of(computedFactory));

    Map<String, String> params = ImmutableMap.of(URL_PARAMETER_NAME, githubUrl);
    // when
    FactoryDto factory = githubFactoryParametersResolver.createFactory(params);
    // then
    assertNotNull(factory.getDevfile());
    SourceDto source = factory.getDevfile().getProjects().get(0).getSource();
    assertEquals(source.getLocation(), "https://github.com/eclipse/che.git");
    assertEquals(source.getBranch(), "foobar");
  }

  @Test
  public void shouldSetBranchIntoDevfileIfNotMatchesCurrent() throws Exception {

    String githubUrl = "https://github.com/eclipse/che/tree/foobranch";

    FactoryDto computedFactory = generateDevfileFactory();
    computedFactory
        .getDevfile()
        .getProjects()
        .add(
            newDto(ProjectDto.class)
                .withSource(
                    newDto(SourceDto.class).withLocation("https://github.com/eclipse/che.git")));

    when(urlFactoryBuilder.createFactoryFromDevfile(any(RemoteFactoryUrl.class), any(), anyMap()))
        .thenReturn(Optional.of(computedFactory));

    Map<String, String> params = ImmutableMap.of(URL_PARAMETER_NAME, githubUrl);
    // when
    FactoryDto factory = githubFactoryParametersResolver.createFactory(params);
    // then
    assertNotNull(factory.getDevfile());
    SourceDto source = factory.getDevfile().getProjects().get(0).getSource();
    assertEquals(source.getBranch(), "foobranch");
  }

  @Test
  public void shouldReturnFactoryFromRepositoryWithFactoryJson() throws Exception {

    String githubUrl = "https://github.com/eclipse/che";

    FactoryDto computedFactory = generateDevfileFactory();

    when(urlFactoryBuilder.createFactoryFromJson(any(RemoteFactoryUrl.class)))
        .thenReturn(Optional.of(computedFactory));

    Map<String, String> params = ImmutableMap.of(URL_PARAMETER_NAME, githubUrl);
    // when
    githubFactoryParametersResolver.createFactory(params);
    // then
    // check we called the builder with the following factory json file
    verify(urlFactoryBuilder).createFactoryFromJson(factoryUrlArgumentCaptor.capture());
    verify(urlFactoryBuilder, never()).buildDefaultDevfile(eq("che"));
    assertEquals(
        factoryUrlArgumentCaptor.getValue().factoryFileLocation(),
        "https://raw.githubusercontent.com/eclipse/che/master/.factory.json");

    assertEquals(factoryUrlArgumentCaptor.getValue().getFactoryFilename(), ".factory.json");
  }

  private FactoryDto generateDevfileFactory() {
    return newDto(FactoryDto.class)
        .withV(CURRENT_VERSION)
        .withSource("repo")
        .withDevfile(
            newDto(DevfileDto.class)
                .withApiVersion(CURRENT_API_VERSION)
                .withMetadata(newDto(MetadataDto.class).withName("che")));
  }
}
