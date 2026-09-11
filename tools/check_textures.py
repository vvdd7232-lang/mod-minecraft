"""Validate size, transparency, and local model texture references."""
from pathlib import Path
import json
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/elementalstaves'
paths = list((ASSETS / 'textures').rglob('*.png'))
assert len(paths) == 28, f'Unexpected texture count: {len(paths)}'
for path in paths:
    with Image.open(path) as im:
        armor = 'armor' in path.parts
        assert im.size == ((64, 32) if armor else (16, 16)), path
        assert im.mode == 'RGBA', path
        alpha = set(im.getchannel('A').tobytes())
        assert alpha <= {0, 255}, f'Soft/blurred alpha: {path}'
        assert 255 in alpha, f'Empty texture: {path}'
        if 'block' in path.parts:
            assert alpha == {255}, f'Holes in block texture: {path}'
        else:
            assert 0 in alpha, f'Missing transparent background: {path}'
        if armor:
            assert im.crop((32, 0, 64, 16)).getbbox() is None, f'Pixels outside armor UV: {path}'
for path in (ASSETS / 'models').rglob('*.json'):
    for ref in json.loads(path.read_text()).get('textures', {}).values():
        if ref.startswith('elementalstaves:'):
            target = ASSETS / 'textures' / (ref.split(':', 1)[1] + '.png')
            assert target.is_file(), f'{path}: missing {ref}'
print(f'PASS: {len(paths)} PNGs; dimensions, hard alpha, armor UV bounds and model references')
