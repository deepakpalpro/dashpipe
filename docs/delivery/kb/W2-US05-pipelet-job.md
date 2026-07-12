# KB: Pipelet Job client (Wave 2)

| Field | Value |
|-------|--------|
| **Article / Story** | KB-W2-US05 / W2-US05 |
| **Audience** | Platform engineers |
| **Product area** | Orchestration / Kubernetes |

## Prerequisites

- W2-US04 async run
- Architecture §10.3 Job shape
- Optional: Rancher Desktop / Kind with working `kubectl`

## Feature overview

Each **pipeline step** is the config for one pipelet; at run time the orchestrator asks `PipeletJobClient` to spawn that step as a Job/Pod (architecture §10.3).

| Type | Class | Role |
|------|-------|------|
| Interface | `PipeletJobClient` | `create(PipeletJobRequest)` |
| Default | `StubPipeletJobClient` | Records creates in-memory (`pipeline.k8s.enabled=false`) |
| Cluster | `Fabric8PipeletJobClient` | Creates real Jobs via Fabric8 (`pipeline.k8s.enabled=true`) |
| Request | `PipeletJobRequest` | tenant/pipeline/execution/pipelet + queues + `ioMode` / `amqpUrl` |

Naming (architecture §10.3):

- Job name: `exec-{executionId}-stage-{n}` (DNS-1123 sanitized)
- Namespace: `tenant-{tenantId}` (**lowercase**, e.g. `tenant-t001`)

**Wiring:** orchestrator creates stage-1 Job from step 1; `StubStageWorker` creates Jobs for steps 2..N when `pipeline.orchestration.stub-stage-worker=true`.

## Enable Fabric8 (Rancher Desktop)

```bash
# Ensure cluster is up
kubectl config current-context   # rancher-desktop
kubectl get nodes

# Build pipelet images into the local store Rancher can pull (IfNotPresent)
for p in plet-s3-source plet-csv-source plet-rest-source plet-csv-to-json plet-python-filter plet-field-mapper plet-webhook-destination; do
  docker build -f pipelets/$p/Dockerfile -t "dashflow/${p}:local" pipelets
done

# API with local + k8s profiles
./mvnw -pl dashflow-api spring-boot:run -Dspring-boot.run.profiles=local,k8s
```

Profile `k8s` sets:

| Property | Value |
|----------|-------|
| `pipeline.k8s.enabled` | `true` |
| `pipeline.orchestration.stub-stage-worker` | `false` |
| `pipeline.orchestration.amqp-url` | `amqp://pipeline:pipeline@host.docker.internal:5672/` |

Then **Run** a pipeline from the UI. Verify:

```bash
kubectl get jobs -n tenant-t001
kubectl get pods -n tenant-t001
kubectl describe job -n tenant-t001 <job-name>
```

Image map defaults live under `pipeline.k8s.images` in `application.yml`.

## How to verify

```bash
docker compose up -d mysql rabbitmq
./mvnw -pl dashflow-api test -Dtest=PipeletJobClientTest,PipeletJobManifestFactoryTest,PipelineRunOrchestratorTest,Fabric8PipeletJobClientIT
```

`Fabric8PipeletJobClientIT` skips automatically if the cluster API is unreachable.

## Failure modes

| Symptom | Check | Mitigation |
|---------|-------|------------|
| Zero Job creates after run | `pipeline.k8s.enabled` still false | Use profile `local,k8s` |
| ImagePullBackOff | Image not in Rancher store | Rebuild with `docker build …:local`; policy `IfNotPresent` |
| Pod cannot reach RabbitMQ | AMQP host is `localhost` | Use `host.docker.internal` via `pipeline.orchestration.amqp-url` |
| Wrong namespace | Uppercase tenant id | Client lowercases to `tenant-t001` |
| Stub still advancing stages | `stub-stage-worker=true` | Set false under profile `k8s` |

## Related

- Developer TDD: [`../tdd/stories/w2/W2-US05-tdd.md`](../tdd/stories/w2/W2-US05-tdd.md)
- Next: connector Secrets into Job env; DLQ (W2-US06)
