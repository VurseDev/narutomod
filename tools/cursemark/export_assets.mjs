import fs from 'node:fs/promises';
import path from 'node:path';
import crypto from 'node:crypto';
import {fileURLToPath} from 'node:url';
import {mcp,workspace} from './build.mjs';
import {createSkinArt} from './skin_art.mjs';
const runtime=path.join(workspace,'src/main/resources/assets/narutomod/textures/cursemark');
const source=path.join(workspace,'textures/other/cursemark');
const modeldir=path.join(workspace,'models/cursemark');
const textureName='curse_mark_heaven_atlas.png';
export async function paintPixels(name,data,width,height,clear=false){
  await mcp('ensure_texture',{name,width,height,fill:'#00000000'});
  const pixels=[];
  for(let y=0;y<height;y++)for(let x=0;x<width;x++){
    const i=(y*width+x)*4;
    if(!data[i+3]&&!clear)continue;
    const color=data[i+3]?'#'+Array.from(data.subarray(i,i+4)).map(c=>c.toString(16).padStart(2,'0')).join(''):null;
    pixels.push({x,y,color});
  }
  for(let i=0;i<pixels.length;i+=3000)await mcp('edit_texture_pixels',{texture:name,pixels:pixels.slice(i,i+3000)});
  const result=await mcp('get_texture',{texture:name,max_edge:Math.max(width,height)});
  if(!result.images[0])throw new Error('Texture export returned no PNG: '+name);
  return Buffer.from(result.images[0].data,'base64');
}
export async function paintSkins(){
  await fs.mkdir(runtime,{recursive:true});await fs.mkdir(source,{recursive:true});
  for(const slim of [false,true]){
    const art=createSkinArt({slim});const suffix=slim?'_slim':'';
    for(const [key,stem] of Object.entries({stage1:'heaven_stage1',ignition:'heaven_ignition',stage2:'heaven_stage2'})){
      const name=stem+suffix+'.png';
      // Clear the named texture first. Blockbench keeps existing pixels when
      // ensure_texture is called on an already-created texture; without this,
      // a previous full-body preview would remain behind the transparent mark.
      const png=await paintPixels(name,art[key],128,128,true);
      await fs.writeFile(path.join(source,name),png);
      await fs.writeFile(path.join(runtime,name),png);
      console.log('Painted and exported',name);
    }
    if(!slim){
      // Paint the 128px skin into the reserved corner of the 256px assembly atlas.
      const pixels=[];for(let y=0;y<128;y++)for(let x=0;x<128;x++){
        const i=(y*128+x)*4;
        pixels.push({x,y,color:art.stage2[i+3]?'#'+Array.from(art.stage2.subarray(i,i+4)).map(v=>v.toString(16).padStart(2,'0')).join(''):null});
      }
      for(let i=0;i<pixels.length;i+=3000)await mcp('edit_texture_pixels',{texture:textureName,pixels:pixels.slice(i,i+3000)});
    }
  }
}
function texture(source64,name,w,h,uuid=crypto.randomUUID()){
  return{name,uuid,id:'0',width:w,height:h,uv_width:w,uv_height:h,mode:'bitmap',internal:true,
    source:'data:image/png;base64,'+source64,saved:true,render_mode:'default',visible:true};
}
export function serializeModel(state,tex,{name,format='free',cubes=state.cubes,groups=state.groups,geo,animations=[]}={}){
  const ids=new Set([...cubes,...groups].map(x=>x.uuid));
  const groupMap=new Map(groups.map(g=>[g.uuid,g]));
  function childNode(id){
    const g=groupMap.get(id);if(!g)return id;
    return{uuid:id,isOpen:true,children:g.children.filter(x=>ids.has(x)).map(childNode)};
  }
  const outliner=groups.filter(g=>!ids.has(g.parent)).map(g=>childNode(g.uuid));
  const elements=cubes.map(c=>{
    const native=geo?.cubes.find(x=>x.name===c.name);
    const {parent,...other}=c;
    const isAccessory=c.name.startsWith('wing_');
    return {...other,type:'cube',autouv:0,shade:true,export:true,color:0,
      box_uv:!!native||isAccessory,uv_offset:native?.uv_offset||c.uv_offset||[0,0],
      faces:Object.fromEntries(Object.entries(c.faces).map(([f,v])=>[f,{...v,texture:0}]))};
  });
  return{meta:{format_version:'5.0',model_format:format,box_uv:format==='modded_entity'},name,
    model_identifier:name.replace(/[^A-Za-z0-9_]/g,''),modded_entity_version:'1.12',modded_entity_flip_y:true,
    resolution:{width:tex.uv_width,height:tex.uv_height},elements,
    groups:groups.map(({parent,children,...g})=>({...g,export:true,isOpen:true})),outliner,textures:[tex],animations};
}
function subset(state,root){
  const r=state.groups.find(g=>g.name===root);if(!r)throw new Error('Missing group '+root);
  const ids=new Set([r.uuid]);
  for(let again=true;again;){again=false;for(const g of state.groups)if(ids.has(g.parent)&&!ids.has(g.uuid)){ids.add(g.uuid);again=true;}}
  return{groups:state.groups.filter(g=>ids.has(g.uuid)),cubes:state.cubes.filter(c=>ids.has(c.parent))};
}
export async function exportModels(){
  const state=(await mcp('get_elements')).result;
  let geo={cubes:[]};
  try{geo=JSON.parse(await fs.readFile(path.join(modeldir,'.geometry-data.json'),'utf8'));}
  catch{console.log('No cached geometry metadata; exporting from the live Blockbench scene.');}
  const atlas=(await mcp('get_texture',{texture:textureName,max_edge:256})).images[0].data;
  const tex=texture(atlas,textureName,256,256);
  // The player supplies the body, skin, and hair. Export only the Stage 2
  // hand-wing accessory as a standalone legacy rig.
  for(const [root,stem]of [['heaven_wings','wings']]){
    const sub=subset(state,root);
    const m=serializeModel(state,tex,{name:'CurseMarkHeaven'+stem,format:'modded_entity',...sub,geo});
    await fs.writeFile(path.join(modeldir,'curse_mark_heaven_'+stem+'.bbmodel'),JSON.stringify(m,null,2));
  }
  // Keep a named runtime/source atlas for a future Java ModelRenderer port.
  // The model subset only references the lower accessory region; the upper
  // skin-net square is transparent overlay art and is never a body model.
  const wingPng=Buffer.from(atlas,'base64');
  await fs.writeFile(path.join(source,'heaven_wings.png'),wingPng);
  await fs.writeFile(path.join(runtime,'heaven_wings.png'),wingPng);
  console.log('Exported transparent player overlays and the standalone legacy hand-wing rig.');
}
if(process.argv[1]===fileURLToPath(import.meta.url)){
  if(process.argv[2]==='skins')await paintSkins();
  if(process.argv[2]==='models')await exportModels();
}
