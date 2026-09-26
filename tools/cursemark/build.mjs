import fs from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {createTransformationGeometry,boxFaces,standardSkinFaces} from './geometry.mjs';

export const workspace=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../..');
let id=700;
export async function mcp(name,args={}){
  if(!process.env.BLOCKBENCH_MCP_SECRET)throw new Error('Set BLOCKBENCH_MCP_SECRET for the local MCP connection.');
  const r=await fetch('http://127.0.0.1:39741/mcp',{method:'POST',headers:{
    Authorization:'Bearer '+process.env.BLOCKBENCH_MCP_SECRET,
    Accept:'application/json, text/event-stream','Content-Type':'application/json'},
    body:JSON.stringify({jsonrpc:'2.0',id:id++,method:'tools/call',params:{name,arguments:args}})});
  const data=await r.json();
  if(data.error||data.result?.isError)throw new Error(name+': '+JSON.stringify(data));
  const c=data.result.content;
  const p=JSON.parse(c.find(x=>x.type==='text').text);
  if(p.ok===false)throw new Error(name+': '+JSON.stringify(p));
  return {result:p.result,images:c.filter(x=>x.type==='image')};
}

export async function setup(){
  const geo=createTransformationGeometry();
  const existing=(await mcp('get_elements')).result;
  if(existing.cubes.some(x=>x.name==='wing_left_palm_main'))throw new Error('Transformation already built; use paint/export.');
  const batch={create_groups:geo.groups,create_cubes:geo.cubes.map(({material,pattern,...c})=>c),
    undo_label:'Sculpt Heaven hand-shaped wings'};
  await mcp('apply_geometry_batch',batch);
  console.log('Geometry created:',geo.cubes.length,'accessory cubes;',geo.groups.length,'groups');
  console.log('Geometry audit:',(await mcp('check_model')).result.summary);
  await mcp('set_project_meta',{texture_width:256,texture_height:256});
  await mcp('ensure_texture',{name:'curse_mark_heaven_atlas.png',width:256,height:256,fill:'#00000000'});
  const names=geo.cubes.map(c=>c.name);
  // Intentionally box-pack for the later 1.12 ModelRenderer attachment export.
  await mcp('pack_box_uv',{cubes:names,texture:'curse_mark_heaven_atlas.png',mode:'box',padding:1,auto_resize:false});
  // Reserve the upper-left 128 square for the standard player skin.
  // Pack entire box nets (never isolated faces), retaining one UV offset per cuboid.
  let x=1,y=130,row=0;
  const entries=[];
  for(const c of geo.cubes){
    const [w,h,d]=c.to.map((v,i)=>Math.round(v-c.from[i]));
    const rw=2*(w+d),rh=h+d;
    if(x+rw>255){x=1;y+=row+1;row=0;}
    if(y+rh>255)throw new Error('Atlas overflow');
    c.uv_offset=[x,y]; c.box_uv=true;
    const faces=boxFaces(x,y,w,h,d);
    for(const [face,uv]of Object.entries(faces))entries.push({cube:c.name,face,uv,rotation:0});
    x+=rw+1;row=Math.max(row,rh);
  }
  for(const [cube,faces]of Object.entries(standardSkinFaces())){
    for(const [face,uv]of Object.entries(faces))entries.push({cube,face,uv,rotation:0});
  }
  await mcp('set_face_uv',{entries});
  const all=(await mcp('get_elements')).result.cubes.map(c=>c.name);
  await mcp('assign_texture',{cubes:all,texture:'curse_mark_heaven_atlas.png'});
  const uv=(await mcp('get_uv_layout',{include_overlaps:false})).result;
  console.log('UV layout summary:',uv.summary);
  await fs.mkdir(path.join(workspace,'models/cursemark'),{recursive:true});
  await fs.writeFile(path.join(workspace,'models/cursemark/.geometry-data.json'),JSON.stringify(geo,null,2));
  console.log('Atlas nets end at row',y+row);
}

if(process.argv[1]===fileURLToPath(import.meta.url)&&process.argv[2]==='setup')await setup();
