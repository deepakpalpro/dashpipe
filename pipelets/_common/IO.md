# Shared I/O transport

See [`io_transport.py`](io_transport.py) and [`../REGISTRY.md`](../REGISTRY.md).

| Env | Purpose |
|-----|---------|
| `IO_MODE` | `stdio` \| `queue` (default `queue`) |
| `INPUT_QUEUE` | Stage input (queue mode) |
| `OUTPUT_QUEUE` | Next stage (empty on last) |
| `AMQP_URL` | Broker URL |
| `SOURCE_TRIGGER` | `once` skips kickoff consume for sources |
