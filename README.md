# This repo is now deprecated the following ones are used instead
- [civil-damages-service](https://github.com/hmcts/civil-damages-service)
- [civil-damages-ccd-definition](https://github.com/hmcts/civil-damages-ccd-definition)
- [civil-damages-camunda-bpmn-definition](https://github.com/hmcts/civil-damages-camunda-bpmn-definition)

# civil-damages-claims

[![Build Status](https://travis-ci.org/hmcts/unspec-service.svg?branch=master)](https://travis-ci.org/hmcts/unspec-service)

Civil Damages Claims's CCD Callback Service.

### Contents:
- [Prerequisites](#prerequisites)
- [Testing](#testing)
- [Building and deploying application](#building-and-deploying-the-application)
- [Camunda](#camunda)

## Prerequisites:
- [Docker](https://www.docker.com)
- [realpath-osx](https://github.com/harto/realpath-osx) (Mac OS only)
- [jq](https://stedolan.github.io/jq/)

Run command:
```
git submodule init
git submodule update
```

Add services, roles and users (in this order) from civil-unspecified-docker repository using scripts located in bin directory.

Load CCD definition:

CCD definition is stored in JSON format. To load it into CCD instance run:

```bash
$ ./bin/import-ccd-definition.sh
```

Note: Above script will export JSON content into XLSX file and upload it into instance of CCD definition store.

Additional note:

You can skip some of the files by using -e option on the import-ccd-definitions, i.e.

```bash
$ ./bin/import-ccd-definition.sh -e UserProfile.json,*-nonprod.json
```

The command above will skip UserProfile.json and all files with -nonprod suffix (from the folders).

## Testing
The repo uses codeceptjs framework for e2e tests.

To install dependencies enter `yarn install`.

To run e2e tests enter `yarn test` in the command line.

### Optional configuration

To run tests with browser window open set `SHOW_BROWSER_WINDOW=true`. By default, the browser window is hidden.

### Smoke test

To run smoke tests enter `yarn test:smoke`.

### API test

To run API tests enter `yarn test:api`.

### Contract testing


#### Generate contracts

You can generate contracts as follows:

```
./gradlew contract
```

#### Publish contracts

If you want to publish the contracts to hmcts pact broker, please set this env variable accordingly before running the publish command.
```
export PACT_BROKER_FULL_URL=http://pact-broker.platform.hmcts.net/
```
and if you want to publish the RPA contract to the PactFlow pact broker, please set this env variable accordingly before running the publish command.
By setting your env variable to this, the IDAM contract will be ignored and only the RPA contract will be published to PactFlow.
```
export PACT_BROKER_FULL_URL=https://civil-damages-claims.pactflow.io/
```
Before running, you should set the API token to connect to the pactflow portal as follows:

```bash
export PACT_BROKER_TOKEN=<api token here>
```
The API Token can be obtained on [Confluence](https://tools.hmcts.net/confluence/display/CU/Pactflow).

To publish your contracts:
```
./gradlew pactPublish
```

* If connecting to Pactflow, please disable the HMCTS VPN.

## Building and deploying the application

### Building the application

The project uses [Gradle](https://gradle.org) as a build tool. It already contains
`./gradlew` wrapper script, so there's no need to install gradle.

To build the project execute the following command:

```bash
  ./gradlew build
```

### Running the application

Create the image of the application by executing the following command:

```bash
  ./gradlew assemble
```

Create docker image:

```bash
  docker-compose build
```

Run the distribution (created in `build/install/unspec-service` directory)
by executing the following command:

```bash
  docker-compose up
```

This will start the API container exposing the application's port
(set to `4000` in this template app).

In order to test if the application is up, you can call its health endpoint:

```bash
  curl http://localhost:4000/health
```

You should get a response similar to this:

```
  {"status":"UP","diskSpace":{"status":"UP","total":249644974080,"free":137188298752,"threshold":10485760}}
```

### Alternative script to run application

To skip all the setting up and building, just execute the following command:

```bash
./bin/run-in-docker.sh
```

For more information:

```bash
./bin/run-in-docker.sh -h
```

Script includes bare minimum environment variables necessary to start api instance. Whenever any variable is changed or any other script regarding docker image/container build, the suggested way to ensure all is cleaned up properly is by this command:

```bash
docker-compose rm
```

It clears stopped containers correctly. Might consider removing clutter of images too, especially the ones fiddled with:

```bash
docker images

docker image rm <image-id>
```

There is no need to remove postgres and java or similar core images.

## Camunda

Camunda UI runs on `http:localhost:9404`. You can login with:
```$xslt
username: demo
password: demo
```

The REST API is available at `http:localhost:9404/engine-rest/`. The REST API documentation is available [here](https://docs.camunda.org/manual/latest/reference/rest/).

To upload all bpmn diagrams via the REST API there is a script located in `./bin directory`.
Run `./bin/import-bpmn-diagram.sh .` to upload it to Camunda. The diagram must exist within
`src/main/resources/camunda`. By setting `CAMUNDA_BASE_URL` env variable you can also use this script to upload diagrams to
Camunda in other environments.

## License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details

