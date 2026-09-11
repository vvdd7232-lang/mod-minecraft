"""Rebuild the original 16px art. Requires Pillow (pip install -r tools/requirements.txt).
Run from any directory; deterministic output, no external textures used.
"""
from pathlib import Path
import random
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
TEX = ROOT / 'src/main/resources/assets/elementalstaves/textures'
STEEL = ['#182b39', '#294452', '#426775', '#62929b', '#91bec1', '#d2e6da']
WOOD = ['#30251e', '#543a28', '#875d38', '#c08d51']
ELEMENTS = {
    'fire': ['#68272b', '#b6402d', '#ed7536', '#ffc968', '#fff0ba'],
    'aqua': ['#183d66', '#216ba0', '#31a6c0', '#7cdee1', '#d4fff0'],
    'air': ['#45576e', '#738aa0', '#aabec6', '#d6e4df', '#fff8df'],
    'earth': ['#393f24', '#637239', '#92a24b', '#bfcc78', '#e9e5ad'],
    'life': ['#234c37', '#368451', '#64b76b', '#a8df87', '#e6f3b3'],
    'lightning': ['#60442b', '#a47b35', '#dec44e', '#f4e896', '#fffae0'],
}

def save(im, folder, name):
    im.save(TEX / folder / (name + '.png'), optimize=True)

def sprite(rows, colors):
    im = Image.new('RGBA', (16,16))
    assert len(rows) <= 16
    for y,row in enumerate(rows):
        assert len(row) <= 16, row
        for x,c in enumerate(row):
            if c != '.': im.putpixel((x,y), tuple(bytes.fromhex(colors[c].lstrip('#'))) + (255,))
    return im

def outline(im, color=STEEL[0]):
    # A single pixel silhouette, no blur or antialiasing.
    out = Image.new('RGBA', im.size)
    d=ImageDraw.Draw(out)
    for y in range(im.height):
        for x in range(im.width):
            if im.getpixel((x,y))[3]:
                for dx,dy in [(0,-1),(0,1),(-1,0),(1,0)]:
                    if 0<=x+dx<im.width and 0<=y+dy<im.height: d.point((x+dx,y+dy),fill=color)
    out.alpha_composite(im)
    return out

def tile(seed, palette):
    rng=random.Random(seed)
    im=Image.new('RGBA',(16,16),palette[1]);d=ImageDraw.Draw(im)
    # Low contrast, short mineral facets rather than high-frequency confetti.
    for _ in range(38):
        x,y=rng.randrange(16),rng.randrange(16)
        d.line((x,y,min(15,x+rng.randrange(1,4)),y),fill=rng.choice(palette))
    return im

# Blocks: riveted metal, cut stone, crystalline storage and inset lighting.
im=tile(9,STEEL[2:4]);d=ImageDraw.Draw(im)
d.rectangle((0,0,15,15),outline=STEEL[0]);d.line((1,14,1,1,14,1),fill=STEEL[4]);d.line((2,14,14,14,14,2),fill=STEEL[1])
for x,y in [(2,2),(12,2),(2,12),(12,12)]:
    d.rectangle((x,y,x+1,y+1),fill=STEEL[1]);d.point((x,y),fill=STEEL[5])
d.line((4,4,11,4),fill=STEEL[3]);d.line((4,11,11,11),fill=STEEL[2])
save(im,'block','elemental_block')

for name,pal in [('elemental_ore',['#747775','#858883','#959891']),('deepslate_elemental_ore',['#373b3e','#464c4d','#565c5b'])]:
    im=tile(31,pal);d=ImageDraw.Draw(im)
    if name.startswith('deepslate'):
        for y in [3,7,11,15]:d.line((0,y,15,y),fill=pal[0])
    for x,y in [(2,2),(10,1),(6,6),(1,11),(11,10)]:
        d.polygon([(x,y+1),(x+1,y),(x+3,y),(x+3,y+2),(x+1,y+3)],fill=STEEL[0])
        d.line((x+1,y+1,x+2,y+1),fill='#b0e8d6');d.point((x+1,y+2),fill='#66b3b5');d.point((x+2,y+2),fill=STEEL[2])
    save(im,'block',name)

im=tile(18,STEEL[0:3]);d=ImageDraw.Draw(im)
for x,y in [(-1,0),(6,-1),(12,1),(2,6),(9,6),(-2,12),(5,12),(13,12)]:
    d.polygon([(x,y+1),(x+2,y),(x+4,y+2),(x+3,y+5),(x,y+4)],fill=STEEL[3])
    d.line((x,y+1,x+2,y,x+3,y+1),fill=STEEL[4]);d.line((x+3,y+3,x+2,y+4),fill=STEEL[1])
    d.point((x+1,y+2),fill=STEEL[5])
save(im,'block','raw_elemental_block')

im=tile(3,STEEL[2:4]);d=ImageDraw.Draw(im)
for x in [1,6,11]:
    d.line((x,0,x,15),fill=STEEL[1]);d.line((x+1,0,x+1,15),fill=STEEL[4]);d.line((x+3,0,x+3,15),fill=STEEL[2])
for y in [0,15]:d.line((0,y,15,y),fill=STEEL[1])
for y in [1,14]:d.line((0,y,15,y),fill=STEEL[4])
save(im,'block','elemental_pillar_side')
im=Image.new('RGBA',(16,16),STEEL[2]);d=ImageDraw.Draw(im)
for inset,col in [(0,STEEL[1]),(1,STEEL[4]),(3,STEEL[1]),(4,STEEL[3]),(6,STEEL[1]),(7,STEEL[4])]:d.rectangle((inset,inset,15-inset,15-inset),outline=col)
save(im,'block','elemental_pillar_end')

for name,lit in [('chiseled_elemental_block',False),('charged_elemental_block',True)]:
    im=tile(14,STEEL[1:3]);d=ImageDraw.Draw(im)
    d.rectangle((0,0,15,15),outline=STEEL[0]);d.rectangle((1,1,14,14),outline=STEEL[3]);d.line((2,2,13,2),fill=STEEL[4])
    d.polygon([(7,3),(12,7),(8,12),(3,8)],fill=STEEL[0])
    d.line((7,4,11,7,8,11,4,8,7,4),fill=STEEL[4] if lit else STEEL[2])
    d.rectangle((7,7,8,8),fill='#e2faac' if lit else STEEL[3])
    if lit:
        for coords in [(7,0,7,3),(12,7,15,7),(8,12,8,15),(0,8,3,8)]:d.line(coords,fill='#96cec1')
    save(im,'block',name)
im=Image.new('RGBA',(16,16),STEEL[0]);d=ImageDraw.Draw(im)
d.rectangle((1,1,14,14),fill=STEEL[3]);d.rectangle((2,2,13,13),fill='#7ab8ad');d.rectangle((3,3,12,12),fill='#c8e3ba');d.rectangle((5,4,10,11),fill='#edf1cd')
for x,y in [(1,1),(12,1),(1,12),(12,12)]:d.rectangle((x,y,x+2,y+2),fill=STEEL[1]);d.point((x,y),fill=STEEL[4])
d.line((4,2,11,2),fill='#f6f5d6');d.line((3,13,12,13),fill='#538e8c')
save(im,'block','elemental_lamp')

# Hand-authored item masks. Light comes from the upper-left.
colors={str(i):v for i,v in enumerate(STEEL)}|{'w':WOOD[1],'W':WOOD[2],'h':WOOD[3]}
items={
'elemental_sword':[
'.............54.','............543.','...........543..','..........543...','.........543....','........543.....','...4...543......','...34.543.......','....3453........','.....43.........','....W.34........','...hW..3........','..hW............','.hW.............','..W.............'],
'elemental_pickaxe':[
'................','....455554......','...43333344.....','........W344....','.......hW.344...','......hW...34...','.....hW.....3...','....hW..........','...hW...........','..hW............','.hW.............','..W.............'],
'elemental_axe':[
'................','........444.....','.......45554....','......455334....','......453334....','......43334.....','......hW34......','.....hW.........','....hW..........','...hW...........','..hW............','.hW.............','..W.............'],
'elemental_shovel':[
'................','..........44....','.........4554...','........45533...','........45333...','........4333....','.......hW23.....','......hW........','.....hW.........','....hW..........','...hW...........','..hW............','...W............'],
'elemental_hoe':[
'................','......455554....','......3333W34...','.........hW.3...','........hW......','.......hW.......','......hW........','.....hW.........','....hW..........','...hW...........','..hW............','...W............'],
'elemental_ingot':[
'................','................','................','................','....45555554....','...4554444434...','..455444443334..','..444444433332..','..333333322222..','...2222222222...','................'],
'raw_elemental':[
'................','................','........54......','.......5443.....','...54.54433.....','..544354432.....','..54333432......','...433323.43....','....3222.4432...','..543...43332...','..4332...322....','...32...........'],
'elemental_helmet':[
'................','.....45554......','...455444334....','..45443333332...','..54433333332...','..44333333332...','..43322222232...','..43........2...','..43........2...','...3........2...'],
'elemental_chestplate':[
'................','..44........44..','.4554......4332.','.54434....43332.','.44334444433332.','.43334444333322.','..333344333332..','...3335433332...','...3334333332...','...4333333332...','...4333333332...','...3333333322...','...4222222222...'],
'elemental_leggings':[
'................','...4555554432...','...4333433332...','...4333333332...','...4333333332...','...4332.43332...','...4332.43332...','...4332.43332...','...4332.43332...','...4332.43332...','...4322.43222...','...3332.33322...'],
'elemental_boots':[
'................','................','...4432.4432....','...4332.4332....','...4332.4332....','...4332.4332....','...4322.4322....','..44332.44332...','..54332.43332...','..33322.33322...'],
}
for name,rows in items.items():save(outline(sprite(rows,colors)),'item',name)

# Different crown silhouettes, but the same wood, bindings and metal socket.
heads={
'fire':['....3...','...34...','..234.2.','..23432.','.123432.','.123432.','..1222..','...11...'],
'aqua':['....3...','...343..','...342..','..34422.','.344222.','.342222.','..2222..','...11...'],
'air':['..3333..','.34...3.','.3....3.','.3..433.','.3...3..','..333...','...32...','...11...'],
'earth':['..2222..','.233332.','.234322.','.233222.','.232222.','..2222..','...11...'],
'life':['.....3..','....342.','.32.342.','.34232..','..322...','...23...','...21...','...11...'],
'lightning':['....33..','...343..','..343...','.34433..','..233...','...32...','..32....','..2.....'],
}
for name,rows in heads.items():
    im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
    d.line((2,13,9,6),fill=WOOD[0],width=3);d.line((2,13,9,6),fill=WOOD[2]);d.point((3,12),fill=WOOD[3]);d.point((5,10),fill=WOOD[3])
    d.line((7,6,9,8),fill=STEEL[3]);d.line((7,5,10,8),fill=STEEL[4])
    head=Image.new('RGBA',(16,16)); hd=ImageDraw.Draw(head)
    for y,row in enumerate(rows):
        for x,c in enumerate(row):
            if c!='.':hd.point((x+7,y+1),fill=ELEMENTS[name][int(c)])
    im.alpha_composite(outline(head,ELEMENTS[name][0]));save(im,'item',name+'_staff')

# Standard 64x32 humanoid UV layout, not a resized item sprite.
# Each box: origin, width, height, depth; all six faces are filled separately.
def armor_box(im,u,v,w,h,depth,part):
    d=ImageDraw.Draw(im)
    faces=[(u+depth,v,w,depth,'top'),(u+depth+w,v,w,depth,'bottom'),
           (u,v+depth,depth,h,'right'),(u+depth,v+depth,w,h,'front'),
           (u+depth+w,v+depth,depth,h,'left'),(u+2*depth+w,v+depth,w,h,'back')]
    for x,y,fw,fh,face in faces:
        for j in range(fh):
            for i in range(fw):
                if part=='boots' and face not in ('top','bottom') and j<6:continue
                if part=='boots' and face=='top':continue
                if part=='helmet' and face=='front' and j>=4 and 1<=i<fw-1:continue
                if part=='helmet' and face in ('left','right','back') and j>=6:continue
                base=3 if face in ('front','top') else 2
                c=STEEL[base]
                if i==0:c=STEEL[4]
                elif i==fw-1:c=STEEL[1]
                if j==fh-1:c=STEEL[1]
                if part in ('body','legs') and face not in ('top','bottom') and j==1:c=STEEL[4]
                if part=='boots' and j==7:c=STEEL[4]
                if part=='body' and face=='front' and i in (3,4) and j in (3,4):c=STEEL[5]
                if part=='arm' and j in (2,7):c=STEEL[1]
                d.point((x+i,y+j),fill=c)

im=Image.new('RGBA',(64,32))
armor_box(im,0,0,8,8,8,'helmet');armor_box(im,16,16,8,12,4,'body');armor_box(im,40,16,4,12,4,'arm');armor_box(im,0,16,4,12,4,'boots')
save(im,'models/armor','elemental_layer_1')
im=Image.new('RGBA',(64,32))
armor_box(im,0,16,4,12,4,'legs');armor_box(im,16,16,8,12,4,'body')
save(im,'models/armor','elemental_layer_2')

# Human-readable contact sheet; nearest-neighbour only, preserving actual pixels.
font_path='/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
font=ImageFont.truetype(font_path,13) if Path(font_path).exists() else ImageFont.load_default()
titlefont=ImageFont.truetype(font_path,26) if Path(font_path).exists() else font
sheet=Image.new('RGB',(1000,1060),'#202a2e');d=ImageDraw.Draw(sheet)
d.text((28,20),'ELEMENTAL STAVES / VANILLA TEXTURE UPDATE',font=titlefont,fill='#d2e6da')
d.text((28,58),'16 x 16 original pixel art  /  steel, crystal & six elements  /  armor UV: 64 x 32',font=font,fill='#91bec1')
paths=sorted(p for p in (TEX/'block').glob('*.png') if not p.stem.startswith('engine_') and p.stem != 'shaft_bearing')+sorted((TEX/'item').glob('*.png'))
for idx,p in enumerate(paths):
    x=24+(idx%7)*138;y=100+(idx//7)*184
    d.rectangle((x,y,x+119,y+119),fill='#303e43')
    im=Image.open(p).resize((112,112),Image.Resampling.NEAREST);sheet.paste(im,(x+4,y+4),im)
    words=p.stem.replace('elemental_','').replace('_',' ')
    if len(words)>18: words=words.replace(' ','\n',1)
    d.text((x,y+128),words,font=font,fill='#d2e6da')
for idx in (1,2):
    im=Image.open(TEX/f'models/armor/elemental_layer_{idx}.png').resize((384,192),Image.Resampling.NEAREST)
    x=28+(idx-1)*490;y=850
    sheet.paste(im,(x,y),im);d.text((x,y-24),f'ARMOR / LAYER {idx}',font=font,fill='#91bec1')
sheet.save(ROOT/'docs/texture-preview.png',optimize=True)
print('Updated 28 textures and docs/texture-preview.png')
