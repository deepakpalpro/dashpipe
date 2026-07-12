#!/usr/bin/env python3
"""Unit tests for pipelets/_common/io_transport.py (stdio path)."""

from __future__ import annotations

import io
import json
import os
import sys
import unittest
from contextlib import redirect_stdout
from pathlib import Path
from unittest import mock

ROOT = Path(__file__).resolve().parent
sys.path.insert(0, str(ROOT / "_common"))

import io_transport  # noqa: E402


class IoTransportStdioTest(unittest.TestCase):
    def setUp(self) -> None:
        self._env = os.environ.copy()
        os.environ["IO_MODE"] = "stdio"

    def tearDown(self) -> None:
        os.environ.clear()
        os.environ.update(self._env)

    def test_resolve_defaults_to_queue(self) -> None:
        os.environ.pop("IO_MODE", None)
        self.assertEqual(io_transport.resolve_io_mode(), "queue")

    def test_stdio_round_trip(self) -> None:
        payload = {"records": [{"a": 1}], "recordCount": 1}
        with mock.patch("sys.stdin", io.StringIO(json.dumps({"csv": "a\n1"}))):
            message = io_transport.read_message()
        self.assertEqual(message["csv"], "a\n1")

        buf = io.StringIO()
        with redirect_stdout(buf):
            io_transport.write_message(payload)
        self.assertEqual(json.loads(buf.getvalue()), payload)

    def test_source_stdio_skips_stdin(self) -> None:
        with mock.patch("sys.stdin", io.StringIO('{"should":"ignore"}')):
            message = io_transport.read_message(source=True)
        self.assertEqual(message, {})


class IoTransportQueueGuardsTest(unittest.TestCase):
    def setUp(self) -> None:
        self._env = os.environ.copy()
        os.environ["IO_MODE"] = "queue"

    def tearDown(self) -> None:
        os.environ.clear()
        os.environ.update(self._env)

    def test_queue_requires_amqp_url(self) -> None:
        os.environ.pop("AMQP_URL", None)
        os.environ["INPUT_QUEUE"] = "q.in"
        with self.assertRaises(SystemExit):
            io_transport.read_message()

    def test_source_once_skips_consume(self) -> None:
        os.environ["SOURCE_TRIGGER"] = "once"
        os.environ.pop("AMQP_URL", None)
        message = io_transport.read_message(source=True)
        self.assertEqual(message, {})


if __name__ == "__main__":
    unittest.main()
