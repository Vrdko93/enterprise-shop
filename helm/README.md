# Enterprise Shop — Helm Deployment Guide

## Overview

This Helm chart deploys the Enterprise Shop microservices application to Kubernetes.

It includes:

- 6 Java services: User, Product, Order, Payment, Notification, and API Gateway
- Frontend
- PostgreSQL
- Redis
- Kafka

The chart supports separate development and production configurations, configurable image tags, replica counts, resource limits, and PostgreSQL persistent storage.

## Prerequisites

- Kubernetes cluster
- Helm 3 or 4
- kubectl configured for the target cluster
- Container images accessible to the cluster
- Kubernetes Secrets containing database credentials and the JWT signing key
- A suitable Kubernetes StorageClass for dynamically provisioned PostgreSQL storage

Required Secrets:

| Secret | Required keys |
|---|---|
| `postgres-secret` | `POSTGRES_USER`, `POSTGRES_PASSWORD` |
| `app-secret` | `JWT_SECRET` |

Secret names can be changed in `values.yaml`.

Create these Secrets securely in the target namespace before installing the chart. Do not commit credentials to Git.

## Development deployment

For a new installation that creates its own PostgreSQL PVC and initialization ConfigMap:

```bash
helm upgrade --install enterprise-shop ./helm/enterprise-shop \
  --namespace enterprise-shop-dev \
  --create-namespace \
  -f ./helm/enterprise-shop/values-dev.yaml \
  --set postgres.persistence.existingClaim="" \
  --set postgres.existingInitConfigMap=""
```

For an existing installation, the default configuration reuses `postgres-pvc` and `postgres-init`.

**Important:** Existing Kubernetes resources created outside Helm may conflict with Helm ownership. Do not install the chart over an existing deployment without planning the migration.

## Production deployment

```bash
helm upgrade --install enterprise-shop ./helm/enterprise-shop \
  --namespace enterprise-shop-prod \
  --create-namespace \
  -f ./helm/enterprise-shop/values-prod.yaml \
  --set postgres.persistence.existingClaim="" \
  --set postgres.existingInitConfigMap=""
```

Production uses image tag `1.0.1` and increased replica counts for selected services.

Ensure that all required application images have been published to an accessible container registry before deployment.

## Upgrade

After modifying the chart or values:

```bash
helm upgrade enterprise-shop ./helm/enterprise-shop \
  --namespace enterprise-shop-prod \
  -f ./helm/enterprise-shop/values-prod.yaml \
  --set postgres.persistence.existingClaim="" \
  --set postgres.existingInitConfigMap=""
```

## Rollback

List release revisions:

```bash
helm history enterprise-shop -n enterprise-shop-prod
```

Roll back to a previous revision:

```bash
helm rollback enterprise-shop 1 -n enterprise-shop-prod
```

Replace `1` with the desired revision.

## Uninstall

```bash
helm uninstall enterprise-shop -n enterprise-shop-prod
```

The Helm-created PostgreSQL PVC has a `helm.sh/resource-policy: keep` annotation to help prevent accidental data deletion during uninstall.

Always verify the PVC and underlying storage retention settings before uninstalling a production release.

## Validation

Lint development configuration:

```bash
helm lint ./helm/enterprise-shop \
  -f ./helm/enterprise-shop/values-dev.yaml
```

Lint production configuration:

```bash
helm lint ./helm/enterprise-shop \
  -f ./helm/enterprise-shop/values-prod.yaml
```

Render production manifests:

```bash
helm template enterprise-shop ./helm/enterprise-shop \
  -f ./helm/enterprise-shop/values-prod.yaml \
  --set postgres.persistence.existingClaim="" \
  --set postgres.existingInitConfigMap=""
```

Package the chart:

```bash
helm package ./helm/enterprise-shop --destination ./helm/packages
```

## AWS deployment considerations

For deployment to Amazon EKS:

1. Push application container images to Amazon ECR or another accessible registry and update the Helm image references.
2. Configure PostgreSQL storage using an EBS-backed StorageClass and the Amazon EBS CSI driver.
3. Provision required Kubernetes Secrets securely, potentially using AWS Secrets Manager with an appropriate integration.
4. Configure ingress, external access, DNS, and TLS as required.
5. Review CPU, memory, scaling, and high-availability settings for production.

The current chart deploys PostgreSQL, Kafka, and Redis as single-replica workloads. Additional architecture changes are recommended before using them for production-critical workloads.

## Deployment status

The chart has passed Helm lint and template rendering for both development and production configurations.

A live Kubernetes installation, upgrade, rollback, and application health verification have not yet been performed.

## Observability

Enterprise Shop includes centralized logging and distributed tracing, configured through Helm.

### Architecture

**Distributed tracing**

Java microservices → OpenTelemetry Collector → Grafana Tempo → Grafana

All six Java services use Micrometer Tracing with OpenTelemetry and export traces over OTLP HTTP to `otel-collector:4318`.

The OpenTelemetry Collector forwards traces to Tempo over OTLP gRPC on port `4317`.

**Centralized logging**

Kubernetes container logs → Fluent Bit → Grafana Loki → Grafana

Fluent Bit runs as a DaemonSet, reads Kubernetes container logs, enriches them with Kubernetes metadata, and forwards them to Loki.

### Observability components

| Component | Image version | Purpose |
|---|---|---|
| OpenTelemetry Collector | 0.115.1 | Receive and forward traces |
| Grafana Tempo | 2.6.1 | Distributed trace storage |
| Grafana Loki | 3.3.2 | Centralized log storage |
| Fluent Bit | 3.2.2 | Kubernetes log collection |
| Grafana | 11.4.0 | Log and trace visualization |

### Grafana access

Grafana is exposed internally through the Kubernetes Service `grafana` on port `3000`.

For local access after deployment:

```bash
kubectl -n enterprise-shop port-forward svc/grafana 3000:3000
```

Open `http://localhost:3000` in a browser.

Grafana's administrator username is `admin`. Its password is read from the existing Kubernetes Secret `grafana-secret`, key `admin-password`.

The Secret must be created securely before deployment. Do not commit administrator passwords to Git.

### Grafana data sources

The Helm chart automatically provisions:

- **Loki:** `http://loki:3100`
- **Tempo:** `http://tempo:3200`

Grafana is configured to search Loki for log messages containing the trace ID selected in Tempo.

### Storage limitations

The current Grafana, Loki, and Tempo Deployments use `emptyDir` volumes. Their locally stored data can be lost when Pods are replaced or rescheduled.

For AWS production, use durable storage such as Amazon S3 for Loki and Tempo, and persistent storage or an external database for Grafana.

### Security and production considerations

- Keep Grafana administrator credentials in Kubernetes Secrets or AWS Secrets Manager.
- Restrict access to Grafana and observability endpoints.
- Configure resource requests, limits, and retention policies for production workloads.
- Review Fluent Bit permissions and Loki label cardinality.
- Replace temporary observability storage before production deployment.

### Validation

Validate the development chart:

```bash
helm lint helm/enterprise-shop -f helm/enterprise-shop/values-dev.yaml
```

Validate the production chart:

```bash
helm lint helm/enterprise-shop -f helm/enterprise-shop/values-prod.yaml
```

Render the manifests without deploying:

```bash
helm template enterprise-shop helm/enterprise-shop \
  -f helm/enterprise-shop/values-dev.yaml
```

These commands validate Helm templates but do not confirm runtime functionality. After deploying to Kubernetes, verify Collector, Tempo, Loki, Fluent Bit, and Grafana Pod health, trace ingestion, log delivery, and trace-to-log navigation.