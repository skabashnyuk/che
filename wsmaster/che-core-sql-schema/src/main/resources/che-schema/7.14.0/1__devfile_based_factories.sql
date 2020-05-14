--
-- Copyright (c) 2012-2020 Red Hat, Inc.
-- This program and the accompanying materials are made
-- available under the terms of the Eclipse Public License 2.0
-- which is available at https://www.eclipse.org/legal/epl-2.0/
--
-- SPDX-License-Identifier: EPL-2.0
--
-- Contributors:
--   Red Hat, Inc. - initial API and implementation
--

-- cleanup existed che6-based factories
ALTER TABLE che_factory DROP CONSTRAINT fk_che_f_workspace_id;
DELETE FROM che_factory_image;
DELETE FROM che_factory;
DELETE FROM che_factory_ide;
DELETE FROM che_factory_on_app_loaded_action_value;
DELETE FROM che_factory_on_projects_loaded_action_value;
DELETE FROM che_factory_on_app_closed_action_value;
DELETE FROM che_factory_on_projects_loaded_action;
DELETE FROM che_factory_on_app_closed_action;
DELETE FROM che_factory_action_properties;
DELETE FROM che_factory_action;
DELETE FROM che_factory_button;
ALTER TABLE che_factory ADD COLUMN devfile_id BIGINT;
-- constraints & indexes
ALTER TABLE che_factory ADD CONSTRAINT fk_che_factory_devfile_id FOREIGN KEY (devfile_id) REFERENCES devfile (id);
CREATE UNIQUE INDEX index_che_factory_devfile_id ON che_factory (devfile_id);