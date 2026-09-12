"""Тесты постоянной PTY-сессии (только POSIX: тут живёт bash)."""

import os
import sys

import pytest

from termbridge.executor import ShellSession

pytestmark = pytest.mark.skipif(
    sys.platform.startswith("win"), reason="PTY-сессия тестируется на POSIX"
)


@pytest.fixture()
def shell(tmp_path):
    s = ShellSession.create("auto", cwd=str(tmp_path), timeout=30)
    s.start()
    yield s
    s.close()


def test_simple_output(shell):
    r = shell.run("echo hello-bridge")
    assert r.returncode == 0
    assert "hello-bridge" in r.output
    assert not r.timed_out


def test_stderr_captured_and_rc(shell):
    r = shell.run("echo out; echo err 1>&2; false")
    assert "out" in r.output
    assert "err" in r.output
    assert r.returncode == 1


def test_cwd_persists_between_commands(shell, tmp_path):
    r1 = shell.run("mkdir -p nested/dir && cd nested/dir")
    assert r1.returncode == 0
    r2 = shell.run("pwd")
    assert os.path.realpath(r2.output.splitlines()[-1].strip()) == os.path.realpath(
        os.path.join(str(tmp_path), "nested", "dir")
    )
    assert os.path.realpath(r2.cwd) == os.path.realpath(
        os.path.join(str(tmp_path), "nested", "dir")
    )


def test_export_persists(shell):
    shell.run("export BRIDGE_VAR=42")
    r = shell.run("echo value=$BRIDGE_VAR")
    assert "value=42" in r.output


def test_multiline_script(shell):
    code = 'if true; then\n  echo branch-ok\nfi'
    r = shell.run(code)
    assert r.returncode == 0
    assert "branch-ok" in r.output


def test_command_not_found_rc(shell):
    r = shell.run("command_that_definitely_does_not_exist_xyz")
    assert r.returncode != 0
    assert "not found" in r.output or "No such file" in r.output


def test_timeout_and_ctrl_c(shell):
    r = shell.run("sleep 30", timeout=1)
    assert r.timed_out is True
    # сессия жива после Ctrl+C и снова выполняет команды
    r2 = shell.run("echo after-timeout")
    assert "after-timeout" in r2.output
    assert r2.returncode == 0


def test_ansi_stripped(shell):
    r = shell.run("printf '\\033[31mred\\033[0m text\\n'")
    assert "\x1b[" not in r.output
    assert "red text" in r.output
