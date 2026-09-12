from termbridge.parser import extract_commands


def test_canonical_bash_block():
    msg = '''Сейчас инициализирую проект.

```bash
# [EXECUTE]

cd my_project && npm init -y
```
'''
    cmds = extract_commands(msg)
    assert len(cmds) == 1
    assert cmds[0].code == "cd my_project && npm init -y"
    assert cmds[0].marker_in_block is True


def test_marker_without_comment():
    msg = '```\n[EXECUTE]\ndocker compose up -d --build\n```'
    cmds = extract_commands(msg)
    assert len(cmds) == 1
    assert cmds[0].code == "docker compose up -d --build"


def test_marker_outside_block_single_fence():
    # ИИ написал метку перед блоком, один блок — берём его.
    msg = '[EXECUTE]\n```python\nprint(1)\n```'
    cmds = extract_commands(msg)
    assert len(cmds) == 1
    assert "print(1)" in cmds[0].code
    assert cmds[0].marker_in_block is False


def test_marker_outside_multiple_fences_ambiguous():
    msg = '[EXECUTE]\n```\necho one\n```\n```\necho two\n```'
    assert extract_commands(msg) == []


def test_no_marker_no_commands():
    msg = 'Просто текст с блоком:\n```bash\necho hi\n```'
    assert extract_commands(msg) == []


def test_multiple_execute_blocks():
    msg = (
        '```bash\n# [EXECUTE]\necho first\n```\n'
        '```bash\n# [EXECUTE]\necho second\n```'
    )
    cmds = extract_commands(msg)
    assert [c.code for c in cmds] == ["echo first", "echo second"]


def test_multiline_command_and_quotes():
    msg = '''```sh
# [EXECUTE]
mkdir -p a/b/c && \\
  cd a/b/c && \\
  echo "done"
```'''
    cmds = extract_commands(msg)
    assert len(cmds) == 1
    assert 'mkdir -p a/b/c' in cmds[0].code
    assert 'echo "done"' in cmds[0].code


def test_case_insensitive_and_slashes():
    msg = '```js\n// [execute]\nwinget install --silent Git.Git\n```'
    cmds = extract_commands(msg)
    assert len(cmds) == 1
    assert cmds[0].code == "winget install --silent Git.Git"


def test_empty():
    assert extract_commands("") == []
    assert extract_commands(None) == []
