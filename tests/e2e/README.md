
# Module for launch E2E tests related to Che 7

## Requirements

- node 8.x
- "Chrome" browser 69.x or later
- deployed Che 7 with accessible URL

## Before launch

**Perform commands:**

- ```export TS_SELENIUM_BASE_URL=<Che7 URL>```
- ```npm install```

## Default launch

- ```npm test```

## Custom launch

- Use environment variables which described in the **```'TestConstants.ts'```** file
- Use environment variables for setting timeouts if needed. You can see the list in **```'TimeoutConstants.ts'```**. You can see the list of those variables and their value if you set the ```'TS_SELENIUM_PRINT_TIMEOUT_VARIABLES = true'```

## Docker launch

- open terminal and go to the "e2e" directory
- export the ```"TS_SELENIUM_BASE_URL"``` variable with "Che" url
- run command ```"npm run test-docker"```

## Docker launch with changed tests

**For launching tests with local changes perform next steps:**

- open terminal and go to the "e2e" directory
- export the ```"TS_SELENIUM_BASE_URL"``` variable with "Che" url
- run command ```"npm run test-docker-mount-e2e"```

## Debug docker launch

The ```'eclipse/che-e2e'``` docker image has VNC server instaled inside. For connecting use ```'0.0.0.0:5920'``` adress.

## The "Happy Path" scenario launching

**The easiest way to do that is to perform steps which are described in the "Docker launch" paragraph.
For running tests without docker, please perform next steps:**

- Deploy Che on Kubernetes infrastructure by using 'Minikube' and 'Chectl' <https://github.com/eclipse/che/blob/master/deploy/kubernetes/README.md>
- Create workspace by using 'Chectl' and devfile
  - link to 'Chectl' manual <https://github.com/che-incubator/chectl#chectl-workspacestart>
  - link to devfile ( **```For successfull test passing, exactly provided devfile should be used```** )
    <https://gist.githubusercontent.com/Ohrimenko1988/93f5426f4ebc1705c55feb8ff0396a49/raw/cbea89ad145ba33ed34a151a12c50f045f9f3b78/yaml-ls-bug.yaml>
- Provide the **```'TS_SELENIUM_BASE_URL'```** environment variable as described above
- perform command **```'npm run test-happy-path'```**
