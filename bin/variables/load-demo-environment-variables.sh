#!/usr/bin/env bash

set -eu

echo 'export ENVIRONMENT=demo'

# urls
echo "export SERVICE_AUTH_PROVIDER_API_BASE_URL=http://rpe-service-auth-provider-demo.service.core-compute-demo.internal"
echo "export IDAM_API_BASE_URL=https://idam-api.demo.platform.hmcts.net"
echo "export CCD_IDAM_REDIRECT_URL=https://www-ccd.demo.platform.hmcts.net/oauth2redirect"
echo "export CCD_DEFINITION_STORE_API_BASE_URL=http://ccd-definition-store-api-demo.service.core-compute-demo.internal"
echo "export CAMUNDA_BASE_URL=https://camunda-bpm.demo.platform.hmcts.net"

# definition placeholders
echo "export CCD_DEF_CASE_SERVICE_BASE_URL=http://unspec-service-demo.service.core-compute-demo.internal"
echo "export https_proxy=proxyout.reform.hmcts.net:8080"
echo "export http_proxy=proxyout.reform.hmcts.net:8080"
