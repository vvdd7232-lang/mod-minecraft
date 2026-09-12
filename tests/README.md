# Тесты

```bash
python -m pytest tests/ -q
```

## Тестовый Chromium

Для `test_e2e.py` нужен headless-Chromium с поддержкой CDP. Поиск бинаря:

1. переменная окружения `TERMBRIDGE_TEST_CHROME=/путь/к/chromium`;
2. `tools/sparticuz/package/bin/chromium` (относительно корня репозитория).

Если в системе обычный Chromium/Chrome — проще всего:

```bash
export TERMBRIDGE_TEST_CHROME=$(which chromium || which google-chrome)
python -m pytest tests/ -q
```

### Получение бинаря в офлайн-окружении (без доступа к CDN Playwright)

Рабочий приём — npm-пакет `@sparticuz/chromium` (тарболл отдаёт сам
registry.npmjs.org, браузер в нём лежит в brotli-архиве):

```bash
mkdir -p tools && cd tools
curl -L -o chromium.tgz \
  https://registry.npmjs.org/@sparticuz/chromium/-/chromium-<ВЕРСИЯ>.tgz
mkdir -p sparticuz && tar xzf chromium.tgz -C sparticuz

pip install brotli
cd sparticuz/package/bin
python -c "import brotli, pathlib
for n in ('chromium.br','al2023.tar.br','swiftshader.tar.br','fonts.tar.br'):
    p = pathlib.Path(n); p.with_suffix('').write_bytes(brotli.decompress(p.read_bytes()))"
mkdir -p al2023 swiftshader fonts
tar xf al2023.tar -C al2023
tar xf swiftshader.tar -C swiftshader
tar xf fonts.tar -C fonts
chmod +x chromium
ln -sf swiftshader/libEGL.so libEGL.so
ln -sf swiftshader/libGLESv2.so libGLESv2.so
ln -sf swiftshader/libvk_swiftshader.so libvk_swiftshader.so
ln -sf swiftshader/libvulkan.so.1 libvulkan.so.1
ln -sf swiftshader/vk_swiftshader_icd.json vk_swiftshader_icd.json
```

Запуск (переменные окружения для библиотек подставляет сам тест, вручную — так):

```bash
LD_LIBRARY_PATH="$PWD/al2023/lib:$PWD/swiftshader" \
VK_ICD_FILENAMES="$PWD/swiftshader/vk_swiftshader_icd.json" \
./chromium --headless=new --no-sandbox --disable-dev-shm-usage \
  --disable-gpu --no-zygote --single-process --enable-unsafe-swiftshader \
  --remote-debugging-port=9222 about:blank
```

Каталог `tools/` в git не коммитится (см. `.gitignore`).
