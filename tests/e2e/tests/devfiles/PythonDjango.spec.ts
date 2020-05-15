/*********************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
import { WorkspaceNameHandler } from '../../utils/WorkspaceNameHandler';
import 'reflect-metadata';
import * as codeExecutionHelper from '../../testsLibrary/CodeExecutionTests';
import * as workspaceHandler from '../../testsLibrary/WorksapceHandlingTests';
import * as projectManager from '../../testsLibrary/ProjectAndFileTests';

const workspaceStack: string = 'Python Django';
const workspaceSampleName: string = 'django-realworld-example-app';
const workspaceRootFolderName: string = 'conduit';

const taskInstallDependencies: string = 'install dependencies';
const taskMigrate: string = 'migrate';
const taskRunServer: string = 'run server';
const taskExpectedDialogText: string = 'A process is now listening on port 7000';
const taskCustomUrlSubpath: string = '/api/';

suite(`${workspaceStack} test`, async () => {

    suite(`Create ${workspaceStack} workspace`, async () => {
        workspaceHandler.createAndOpenWorkspace(workspaceStack);
        projectManager.waitWorkspaceReadiness(workspaceSampleName, workspaceRootFolderName);
    });

    suite('Install dependencies', async () => {
        codeExecutionHelper.runTask(taskInstallDependencies, 60_000);
        codeExecutionHelper.closeTerminal(taskInstallDependencies);
    });

    suite('Migrate Django application project', async () => {
        codeExecutionHelper.runTask(taskMigrate, 30_000);
        codeExecutionHelper.closeTerminal(taskMigrate);
    });

    suite('Run django server', async () => {
        codeExecutionHelper.runTaskWithDialogShellDjangoWorkaround(taskRunServer, taskExpectedDialogText, taskCustomUrlSubpath, 30_000);
    });

    suite ('Stopping and deleting the workspace', async () => {
        let workspaceName = 'not defined';
        suiteSetup( async () => {
            workspaceName = await WorkspaceNameHandler.getNameFromUrl();
        });
        test (`Stop worksapce`, async () => {
            await workspaceHandler.stopWorkspace(workspaceName);
        });
        test (`Remove workspace`, async () => {
            await workspaceHandler.removeWorkspace(workspaceName);
        });
    });
});
