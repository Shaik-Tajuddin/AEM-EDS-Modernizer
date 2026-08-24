#!/usr/bin/env bash
# ==============================================================================
# AEM -> EDS Modernizer - End-to-End Test Suite
# Tests standalone server startup, Dry Run, Migration, and all Phase 1 & 2 APIs
# ==============================================================================

set -euo pipefail

PORT=${PORT:-8080}
BASE_URL="http://localhost:${PORT}/api"

echo "======================================================================"
echo " Starting AEM -> EDS Modernizer End-to-End Test Suite"
echo " Target API Base: ${BASE_URL}"
echo "======================================================================"

# 1. Health check
echo -n "1. Checking /health ... "
HEALTH=$(curl -s "${BASE_URL}/health")
echo "${HEALTH}" | grep -q "UP" && echo "PASS [OK]" || (echo "FAIL" && exit 1)

# 2. Create Project
echo -n "2. Creating project 'wknd-demo' ... "
PROJECT=$(curl -s -X POST "${BASE_URL}/projects" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "wknd-demo",
    "name": "WKND Experience Modernizer",
    "aemAuthorUrl": "https://mock-aem.local",
    "contentRoot": "/content/wknd",
    "edsGitRepoUrl": "https://github.com/company/wknd-eds"
  }')
echo "${PROJECT}" | grep -q "wknd-demo" && echo "PASS [OK]" || (echo "FAIL" && exit 1)

# 3. Trigger Dry Run
echo -n "3. Triggering Mandatory Dry Run ... "
DRYRUN=$(curl -s -X POST "${BASE_URL}/projects/wknd-demo/dryrun")
echo "${DRYRUN}" | grep -q "COMPLETED" && echo "PASS [OK]" || (echo "FAIL" && exit 1)

# 4. Verify Site Inventory
echo -n "4. Verifying Site Inventory ... "
INV=$(curl -s "${BASE_URL}/projects/wknd-demo/inventory")
echo "${INV}" | grep -q "pages" && echo "PASS [OK]" || (echo "FAIL" && exit 1)

# 5. Verify Migration Plan & Estimates
echo -n "5. Verifying Migration Plan & Estimates ... "
PLAN=$(curl -s "${BASE_URL}/projects/wknd-demo/plan")
echo "${PLAN}" | grep -q "derivationTrail" && echo "PASS [OK]" || (echo "FAIL" && exit 1)

# 6. Trigger Approved Migration
echo -n "6. Triggering Full Migration ... "
MIGRATE=$(curl -s -X POST "${BASE_URL}/projects/wknd-demo/migrate")
echo "${MIGRATE}" | grep -q "COMPLETED" && echo "PASS [OK]" || (echo "FAIL" && exit 1)

# 7. Verify Phase 2 URL Redirects
echo -n "7. Verifying Phase 2 URL Redirects ... "
REDIRECTS=$(curl -s "${BASE_URL}/projects/wknd-demo/redirects")
echo "${REDIRECTS}" | grep -q "sourceUrl" && echo "PASS [OK]" || (echo "FAIL" && exit 1)

# 8. Verify Phase 2 Dependencies
echo -n "8. Verifying Phase 2 Dependency Graph ... "
DEPS=$(curl -s "${BASE_URL}/projects/wknd-demo/dependencies")
echo "${DEPS}" | grep -q "edgeType" && echo "PASS [OK]" || (echo "FAIL" && exit 1)

# 9. Verify Phase 2 Rollout Stages
echo -n "9. Verifying Phase 2 Rollout Stages ... "
ROLLOUT=$(curl -s "${BASE_URL}/projects/wknd-demo/rollout-stages")
echo "${ROLLOUT}" | grep -q "stageName" && echo "PASS [OK]" || (echo "FAIL" && exit 1)

# 10. Verify Phase 2 Repairs
echo -n "10. Verifying Phase 2 Automated Repairs ... "
REPAIRS=$(curl -s "${BASE_URL}/projects/wknd-demo/repairs")
echo "${REPAIRS}" | grep -q "successful" && echo "PASS [OK]" || (echo "FAIL" && exit 1)

echo "======================================================================"
echo " ALL END-TO-END TESTS PASSED SUCCESSFULLY! "
echo "======================================================================"
