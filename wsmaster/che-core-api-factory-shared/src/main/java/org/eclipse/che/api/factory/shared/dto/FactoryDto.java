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
package org.eclipse.che.api.factory.shared.dto;

import static org.eclipse.che.api.core.factory.FactoryParameter.Obligation.MANDATORY;
import static org.eclipse.che.api.core.factory.FactoryParameter.Obligation.OPTIONAL;

import java.util.List;
import org.eclipse.che.api.core.factory.FactoryParameter;
import org.eclipse.che.api.core.model.factory.Factory;
import org.eclipse.che.api.core.rest.shared.dto.Hyperlinks;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.workspace.shared.dto.devfile.DevfileDto;
import org.eclipse.che.dto.shared.DTO;

/**
 * Factory of version 4.0
 *
 * @author Max Shaposhnik
 */
@DTO
public interface FactoryDto extends Factory, Hyperlinks {

  @Override
  @FactoryParameter(obligation = MANDATORY)
  String getV();

  void setV(String v);

  FactoryDto withV(String v);

  @FactoryParameter(obligation = MANDATORY)
  DevfileDto getDevfile();

  void setDevfile(DevfileDto workspace);

  FactoryDto withDevfile(DevfileDto devfileDto);

  @Override
  @FactoryParameter(obligation = OPTIONAL, trackedOnly = true)
  PoliciesDto getPolicies();

  void setPolicies(PoliciesDto policies);

  FactoryDto withPolicies(PoliciesDto policies);

  @Override
  @FactoryParameter(obligation = OPTIONAL)
  AuthorDto getCreator();

  void setCreator(AuthorDto creator);

  FactoryDto withCreator(AuthorDto creator);

  @Override
  @FactoryParameter(obligation = OPTIONAL)
  ButtonDto getButton();

  void setButton(ButtonDto button);

  FactoryDto withButton(ButtonDto button);

  @Override
  @FactoryParameter(obligation = OPTIONAL)
  IdeDto getIde();

  void setIde(IdeDto ide);

  FactoryDto withIde(IdeDto ide);

  @Override
  @FactoryParameter(obligation = OPTIONAL, setByServer = true)
  String getId();

  void setId(String id);

  FactoryDto withId(String id);

  /**
   * Indicates filename in repository from which the factory was created (for example, .devfile or
   * .factory.json) or just contains 'repo' value if factory was created from bare GitHub
   * repository. For custom raw URL's (pastebin, gist etc) value is {@code null}
   */
  @FactoryParameter(obligation = OPTIONAL, setByServer = true)
  String getSource();

  void setSource(String source);

  FactoryDto withSource(String source);

  @Override
  @FactoryParameter(obligation = OPTIONAL)
  String getName();

  void setName(String name);

  FactoryDto withName(String name);

  @Override
  FactoryDto withLinks(List<Link> links);
}
