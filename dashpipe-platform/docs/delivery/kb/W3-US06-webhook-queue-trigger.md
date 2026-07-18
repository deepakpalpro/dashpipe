# KB: Webhook queue on-demand trigger (Wave 3)

| Field | Value |
|-------|--------|
| **Article / Story** | KB-W3-US06 / W3-US06 |
| **Audience** | Platform engineers / support |
| **Product area** | Webhook Ingress / On-demand processing |

## Prerequisites

- W3-US01 accept + publish to `tenant.{T}.webhook.{C}.in`
- W2-US05 `StubPipeletJobClient` (or real Job client)
- MySQL + RabbitMQ up

## Feature overview

Ingress still returns **202 without starting a Job**. After publish, the queue is registered for watching. A separate **poller** (architecture §11.6 Pipeline Manager style) reads RabbitMQ depth and calls `PipeletJobClient` when depth &gt; 0.

| Piece | Role |
|-------|------|
| `WebhookQueueWatchRegistry` | Queues to watch (registered on successful publish) |
| `WebhookQueueDepthPoller` | Scheduled poll when enabled |
| `WebhookProcessorTrigger` | Coalesce: one Job create while depth stays &gt; 0 |
| `StubPipeletJobClient` | Records creates locally (no Kind required) |

Enable with:

```yaml
pipeline:
  webhook:
    queue-trigger:
      enabled: true
      poll-interval-ms: 500
```

Default is **disabled** so US01 ITs keep asserting no Job on accept.

Wave 3 binding is synthetic: pipeline `webhook-{connectorId}`, pipelet `webhook-processor`. Real `pipeline_steps` wiring is deferred.

## How to verify

```bash
docker compose up -d mysql rabbitmq
./mvnw -pl dashpipe-api test -Dtest=WebhookQueueTriggerIT,WebhookProcessorTriggerTest,WebhookControllerIT
```

`WebhookQueueTriggerIT` enables the poller via `@TestPropertySource`.

## Failure modes

| Symptom | Check | Mitigation |
|---------|-------|------------|
| No Job after publish | `queue-trigger.enabled` false | Enable for local/dev or IT |
| Job storm | Coalesce broken | One create per idle→busy edge |
| Job on accept | Bug in ingress | Accept must only register watch; never `create` |
| Depth always 0 | Queue name / declare | Confirm topology + message left on `.in` |

## Related

- Developer TDD: [`../tdd/stories/w3/W3-US06-tdd.md`](../tdd/stories/w3/W3-US06-tdd.md)
- Accept (no Job): [`W3-US01-webhook-ingress-accept.md`](W3-US01-webhook-ingress-accept.md)
- Architecture §11.6, §10.3
