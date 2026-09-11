"""Original vanilla-style engine/shaft art and resources. Requires Pillow."""
from pathlib import Path
import json
from PIL import Image, ImageDraw
ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources'
A=RES/'assets/elementalstaves'
def write(path,data):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(data,indent=2,ensure_ascii=False)+'\n')
steel=['#23272b','#40494e','#68777b','#9eada9','#d4d9c3']
copper=['#5d3929','#9d613a','#c99254','#e7bd7b']
for name in ['engine_side','engine_front','engine_back','engine_back_on','engine_top','shaft_bearing']:
    im=Image.new('RGBA',(16,16),steel[1]);d=ImageDraw.Draw(im)
    d.rectangle((0,0,15,15),outline=steel[0]);d.line((1,14,1,1,14,1),fill=steel[3]);d.line((2,14,14,14,14,2),fill=steel[0])
    for x,y in [(2,2),(12,2),(2,12),(12,12)]:d.rectangle((x,y,x+1,y+1),fill=steel[0]);d.point((x,y),fill=steel[4])
    if name=='engine_side':
        for y in [4,7,10]:d.rectangle((4,y,11,y+1),fill=steel[0]);d.line((4,y,11,y),fill=steel[2])
        d.line((2,6,2,10),fill=copper[2]);d.line((13,6,13,10),fill=copper[1])
    elif name=='engine_front':
        d.rectangle((3,3,12,12),fill=copper[0]);d.rectangle((4,4,11,11),outline=copper[2]);d.rectangle((6,6,9,9),fill=steel[0])
        d.point((4,4),fill=copper[3]);d.point((11,11),fill=copper[1])
        d.line((6,2,9,2),fill=copper[2])
    elif name.startswith('engine_back'):
        d.rectangle((3,4,12,12),fill=steel[0]);d.rectangle((4,5,11,11),fill='#ec8c36' if name.endswith('_on') else '#151b20')
        for x in [5,8,11]:d.line((x,5,x,11),fill=steel[1])
        d.line((4,13,11,13),fill=steel[2])
    elif name=='engine_top':
        d.rectangle((4,3,11,12),fill=steel[0]);d.rectangle((5,4,10,11),fill=steel[2])
        for y in [5,7,9]:d.line((5,y,10,y),fill=steel[0])
    else:
        d.rectangle((3,3,12,12),fill=copper[0]);d.rectangle((4,4,11,11),outline=copper[2]);d.rectangle((6,6,9,9),fill=steel[0])
    p=A/f'textures/block/{name}.png';p.parent.mkdir(parents=True,exist_ok=True);im.save(p,optimize=True)
im=Image.new('RGBA',(64,32),steel[2]);d=ImageDraw.Draw(im)
# Cuboid atlas regions: shaft sides (4x16), central coupling, flywheel and offset crank pin.
for x in [0,16,20,36]:d.line((x,0,x,19),fill=steel[0]);d.line((x+1,0,x+1,19),fill=steel[3])
for y in [4,19]:d.line((0,y,39,y),fill=steel[1])
d.rectangle((40,0,63,10),fill=copper[1]);d.line((40,1,63,1),fill=copper[3]);d.line((40,8,63,8),fill=copper[0])
d.rectangle((0,20,23,31),fill=steel[1]);d.rectangle((2,22,11,31),fill=copper[1]);d.rectangle((14,22,23,31),fill=copper[1])
for x in [2,14]:
    d.rectangle((x+1,23,x+8,30),outline=copper[3]);d.rectangle((x+3,25,x+6,28),fill=steel[3])
    d.line((x+1,26,x+8,26),fill=steel[0])
d.rectangle((40,12,63,21),fill=steel[3]);d.rectangle((48,24,55,27),fill=copper[3])
p=A/'textures/entity/mechanical_rotor.png';p.parent.mkdir(parents=True,exist_ok=True);im.save(p,optimize=True)

for lit in [False,True]:
    write(A/f'models/block/coal_engine{"_on" if lit else ""}.json',{
        'parent':'minecraft:block/cube','textures':{
            'particle':'elementalstaves:block/engine_side',
            'north':'elementalstaves:block/engine_front','south':'elementalstaves:block/engine_back'+('_on' if lit else ''),
            'east':'elementalstaves:block/engine_side','west':'elementalstaves:block/engine_side',
            'up':'elementalstaves:block/engine_top','down':'elementalstaves:block/engine_side'}})
variants={}
for facing,rot in [('north',{}),('south',{'y':180}),('east',{'y':90}),('west',{'y':270}),('up',{'x':270}),('down',{'x':90})]:
    for lit in [False,True]:variants[f'facing={facing},lit={str(lit).lower()}']={'model':'elementalstaves:block/coal_engine'+('_on' if lit else ''),**rot}
write(A/'blockstates/coal_engine.json',{'variants':variants})

def box(start,end,texture):return {'from':start,'to':end,'faces':{f:{'texture':'#'+texture} for f in ['north','south','up','down','east','west']}}
shaft={'textures':{'particle':'elementalstaves:block/shaft_bearing','bearing':'elementalstaves:block/shaft_bearing'},
       'elements':[box([5,5,0],[11,11,2],'bearing'),box([5,5,14],[11,11,16],'bearing')]}
write(A/'models/block/drive_shaft.json',shaft)
write(A/'blockstates/drive_shaft.json',{'variants':{f'axis={axis}':{'model':'elementalstaves:block/drive_shaft',**rot} for axis,rot in [('z',{}),('x',{'y':90}),('y',{'x':90})]}})
shaft_item={**shaft,'parent':'minecraft:block/block','elements':shaft['elements']+[box([6,6,0],[10,10,16],'bearing'),box([5,5,6],[11,11,10],'bearing')]}
write(A/'models/item/drive_shaft.json',shaft_item)
write(A/'models/item/coal_engine.json',{'parent':'elementalstaves:block/coal_engine'})
for name in ['coal_engine','drive_shaft']:
    write(RES/f'data/elementalstaves/loot_table/blocks/{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':f'elementalstaves:{name}'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
    write(RES/f'data/elementalstaves/advancement/recipes/redstone/{name}.json',{'parent':'minecraft:recipes/root','criteria':{
        'has_iron':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':['minecraft:iron_ingot']}]}},
        'has_recipe':{'trigger':'minecraft:recipe_unlocked','conditions':{'recipe':f'elementalstaves:{name}'}}},
        'requirements':[['has_iron','has_recipe']],'rewards':{'recipes':[f'elementalstaves:{name}']}})
write(RES/'data/elementalstaves/recipe/drive_shaft.json',{'type':'minecraft:crafting_shaped','category':'redstone','pattern':['I','I','I'],'key':{'I':{'item':'minecraft:iron_ingot'}},'result':{'id':'elementalstaves:drive_shaft','count':4}})
write(RES/'data/elementalstaves/recipe/coal_engine.json',{'type':'minecraft:crafting_shaped','category':'redstone','pattern':['III','CFC','ISI'],'key':{'I':{'item':'minecraft:iron_ingot'},'C':{'item':'minecraft:copper_ingot'},'F':{'item':'minecraft:furnace'},'S':{'item':'elementalstaves:drive_shaft'}},'result':{'id':'elementalstaves:coal_engine'}})
for path in ['mineable/pickaxe','needs_stone_tool']:
    p=RES/f'data/minecraft/tags/block/{path}.json';data=json.loads(p.read_text())
    for name in ['coal_engine','drive_shaft']:
        ref='elementalstaves:'+name
        if ref not in data['values']:data['values'].append(ref)
    write(p,data)
for locale,values in {
'ru_ru':{'tooltip.elementalstaves.coal_engine.1':'ПКМ углём: +1; Shift + ПКМ: загрузить стопку. 40 об/мин.',
 'tooltip.elementalstaves.coal_engine.2':'Редстоун: пауза. Shift + ПКМ пустой рукой: забрать запас.',
 'tooltip.elementalstaves.drive_shaft.1':'Соедините по прямой с маховиком двигателя. До 32 валов.',
 'tooltip.elementalstaves.drive_shaft.2':'Ось задаёт грань установки. Поворотов и станков пока нет.',
 'block.elementalstaves.coal_engine':'Угольный двигатель','block.elementalstaves.drive_shaft':'Приводной вал',
'message.elementalstaves.engine.full':'Топливный слот заполнен или занят другим видом угля.',
'message.elementalstaves.engine.status':'Топливо: %s | Осталось горения: %s с | %s',
'message.elementalstaves.engine.paused':'Пауза: сигнал редстоуна','message.elementalstaves.engine.running':'Работает: 40 об/мин','message.elementalstaves.engine.idle':'Ожидание топлива'},
'en_us':{'tooltip.elementalstaves.coal_engine.1':'Use coal: +1; sneak-use: insert stack. 40 RPM.',
 'tooltip.elementalstaves.coal_engine.2':'Redstone pauses. Sneak-use empty hand: retrieve queued fuel.',
 'tooltip.elementalstaves.drive_shaft.1':'Connect straight to the engine flywheel. Up to 32 shafts.',
 'tooltip.elementalstaves.drive_shaft.2':'Axis follows placement face. No turns or machines yet.',
 'block.elementalstaves.coal_engine':'Coal Engine','block.elementalstaves.drive_shaft':'Drive Shaft',
'message.elementalstaves.engine.full':'Fuel slot is full or contains a different coal type.',
'message.elementalstaves.engine.status':'Fuel: %s | Burn remaining: %s s | %s',
'message.elementalstaves.engine.paused':'Paused by redstone','message.elementalstaves.engine.running':'Running: 40 RPM','message.elementalstaves.engine.idle':'Waiting for fuel'}
}.items():
    p=A/f'lang/{locale}.json';data=json.loads(p.read_text());data.update(values);write(p,data)
print('Mechanical textures, models, states, recipes, loot, tags and translations written')
