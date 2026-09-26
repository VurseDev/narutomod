import fs from 'node:fs/promises';
import path from 'node:path';
import crypto from 'node:crypto';
import assert from 'node:assert/strict';
import {root, decodePNG, encodePNG} from './png.mjs';

const assets=path.join(root,'src/main/resources/assets/narutomod');
const add=(a,b)=>a.map((v,i)=>v+b[i]);
const radians=r=>[-r[0]*180/Math.PI,r[1]*180/Math.PI,-r[2]*180/Math.PI];
const bounds=quads=>[0,1,2].map(axis=>{
  const values=quads.flatMap(q=>q.xyz.filter((_,i)=>i%3===axis));
  return [Math.min(...values),Math.max(...values)];
});
function area(q) {
  const a=q.xyz.slice(0,3),b=q.xyz.slice(3,6).map((v,i)=>v-a[i]),c=q.xyz.slice(6,9).map((v,i)=>v-a[i]);
  return Math.hypot(b[1]*c[2]-b[2]*c[1],b[2]*c[0]-b[0]*c[2],b[0]*c[1]-b[1]*c[0]);
}

// Slice a legacy cuboid without stretching its original outer UVs. New interior
// joint caps take the matching end-face UVs; the silhouette is unchanged at rest.
function slice(box,region) {
  const full=bounds(box.quads);
  return box.quads.map(q=>{
    const xyz=q.xyz.map((v,i)=>{const a=i%3;return Math.abs(v-full[a][0])<1e-5?region[a][0]:region[a][1];});
    const a=q.xyz.slice(0,3),u=q.xyz.slice(3,6).map((v,i)=>v-a[i]),v=q.xyz.slice(9,12).map((v,i)=>v-a[i]);
    const uu=u.reduce((s,x)=>s+x*x,0),vv=v.reduce((s,x)=>s+x*x,0);
    const uv=[];
    for(let i=0;i<4;i++){
      const p=xyz.slice(i*3,i*3+3).map((x,j)=>x-a[j]);
      const s=uu?p.reduce((n,x,j)=>n+x*u[j],0)/uu:0,t=vv?p.reduce((n,x,j)=>n+x*v[j],0)/vv:0;
      for(let j=0;j<2;j++)uv.push(q.uv[j]+s*(q.uv[2+j]-q.uv[j])+t*(q.uv[6+j]-q.uv[j]));
    }
    return {...q,xyz,uv};
  });
}

export function generate(ref,kind) {
  const skeleton=kind==='skeletal',perfect=kind==='perfect',armored=kind==='armored';
  const scale=skeleton?.30:1,source=new Map(ref.bones.map(b=>[b.name,b]));
  const groups=[],cubes=[];
  const group=(name,origin,parent,rotation=[0,0,0])=>{groups.push({name,origin,rotation,...(parent?{parent}:{})});return name;};
  group('root',[0,0,0]);group('torso',[0,24,0],'root');
  const convert=(p,offset)=>add([p[0]*scale,24-p[1]*scale,p[2]*scale],offset);
  function copy(original,prefix,parent,offset=[0,0,0],skip=[],aliases={},armSide=null) {
    const positions=new Map(),names=new Map();
    function visit(n,p,parentName){
      if(skip.includes(n))return;
      const b=source.get(n);if(!b)return;
      const pos=add(p,b.pivot),origin=convert(pos,offset),name=aliases[n]||prefix+n;
      positions.set(n,pos);names.set(n,name);
      group(name,origin,parentName,radians(b.rotation));
      for(const box of b.cubes){
        if(perfect && armSide && n===original && box.index===0){
          const region=bounds(box.quads),cx=(region[0][0]+region[0][1])/2;
          const fore=group(prefix+'forearm',convert(add(pos,[cx,5,0]),offset),name);
          const hand=group(prefix+'hand',convert(add(pos,[cx,8.5,0]),offset),fore);
          emit(box,fore,n,origin,[[...region[0]],[5,8.5],[...region[2]]],'forearm');
          emit(box,hand,n,origin,[[...region[0]],[8.5,9],[...region[2]]],'palm');
          for(let f=0;f<4;f++){
            const low=region[0][0]+f*(region[0][1]-region[0][0])/4,high=region[0][0]+(f+1)*(region[0][1]-region[0][0])/4;
            const finger=group(prefix+'finger'+f,convert(add(pos,[(low+high)/2,9,0]),offset),hand);
            emit(box,finger,n,origin,[[low,high],[9,10],[...region[2]]],'finger'+f);
          }
        } else if(!skeleton && !perfect && armSide && (n==='cube_r18'||n==='cube_r19')) {
          const region=bounds(box.quads),pivot=add(pos,[(region[0][0]+region[0][1])/2,region[1][0],(region[2][0]+region[2][1])/2]);
          const finger=group(prefix+'finger'+box.index,convert(pivot,offset),name);
          emit(box,finger,n,origin);
        } else emit(box,name,n,origin);
      }
      for(const child of ref.bones)if(child.parent===n)visit(child.name,pos,name);
    }
    function emit(box,parentName,sourceBone,origin,region=null,suffix='') {
      const quads=(region?slice(box,region):box.quads).filter(q=>area(q)>1e-8).map(q=>({
        xyz:q.xyz.map((v,i)=>origin[i%3]+v*scale*(i%3===1?-1:1)),uv:q.uv.slice()
      }));
      if(!quads.length)return;
      cubes.push({name:prefix+sourceBone+'_box'+box.index+(suffix?'_'+suffix:''),parent:parentName,origin,
        quads,sourceBone,sourceBox:box.index,sourceOrigin:origin,sourceScale:scale,...(region?{sourceRegion:region}:{})});
    }
    visit(original,[0,0,0],parent);
    if(armSide && skeleton){
      const foreSource=armSide==='right'?'cube_r13':'cube_r16',handSource=armSide+'Hand';
      const elbow=positions.get(foreSource).slice();elbow[1]-=6.6;
      const fore=group(prefix+'forearm',convert(elbow,offset),aliases[original]);
      for(const n of [foreSource,handSource])groups.find(g=>g.name===names.get(n)).parent=fore;
    }
    if(armSide && !skeleton && !perfect){
      // The legacy hand bone's authored pivot is far outside its visible fingers.
      // It has zero bind rotation, so moving only this animation pivot preserves
      // every bind vertex while producing a wrist rotation at the actual hand.
      const hand=groups.find(g=>g.name===prefix+'hand');
      const p=positions.get(armSide==='right'?'cube_r18':'cube_r19');
      if(hand&&p)hand.origin=convert(add(p,[0,-1.5,0]),offset);
    }
    if(armSide && perfect){
      const weapon=groups.find(g=>g.name===prefix+'sword');
      if(weapon)weapon.parent=prefix+'hand';
    }
  }
  copy('bipedBody','legacy_','torso');
  if(!skeleton)for(const side of ['Right','Left'])copy('biped'+side+'Leg','legacy_','root',[0,0,0],[],{['biped'+side+'Leg']:'leg_'+side.toLowerCase()});
  for(const rear of [false,true]){
    const prefix=rear?'rear_':'front_',offset=[0,0,rear?1.6:-1.6];
    const facing=group(prefix+'facing',[0,24,offset[2]],'torso',[0,rear?180:0,0]);
    const head=group(prefix+'head',[0,skeleton?26.4:24,offset[2]],facing);
    // One shared crown keeps the exact original silhouette. Duplicating the
    // full hair/helmet subtree on the rear face intersected both faces.
    const skip=skeleton?(rear?['HornStyle1','HornStyle2']:['HornStyle1']):perfect?(rear?['hair','HeadDecor']:[]):armored&&!rear?[]:['Hat'];
    copy('bipedHead',prefix,head,offset,skip);
    copy('bipedHeadwear',prefix,head,offset);
    for(const side of ['right','left']){
      const original='biped'+(side==='right'?'Right':'Left')+'Arm',p=prefix+side+'_';
      const aliases={[original]:p+'upper'};
      if(skeleton){aliases[side+'Hand']=p+'hand';aliases[side+'Fingers']=p+'finger0';}
      else if(!perfect){aliases[side==='right'?'bone':'bone7']=p+'forearm';aliases[side==='right'?'bone2':'bone8']=p+'hand';}
      copy(original,p,facing,offset,(!skeleton&&!perfect&&!armored)?['spikes','spikes2']:[],aliases,side);
    }
  }
  // Retain required transform parents, prune unused legacy placeholders only.
  const used=new Set(cubes.map(c=>c.parent));
  for(let changed=true;changed;){changed=false;for(const g of groups)if(used.has(g.name)&&g.parent&&!used.has(g.parent)){used.add(g.parent);changed=true;}}
  const geo={kind,texture:'susanoo_madara_'+kind+'.png',source:'models/madara/reference/'+ref.kind.toLowerCase()+'.json',
    groups:groups.filter(g=>used.has(g.name)),cubes};
  geo.sealPoses=bakeSeals(geo);return geo;
}

const mul=(a,b)=>{const r=Array(16).fill(0);for(let i=0;i<4;i++)for(let j=0;j<4;j++)for(let k=0;k<4;k++)r[i*4+j]+=a[i*4+k]*b[k*4+j];return r;};
const point=(m,p)=>[0,1,2].map(i=>m[i*4]*p[0]+m[i*4+1]*p[1]+m[i*4+2]*p[2]+m[i*4+3]);
function local(p,degrees){
  const [x,y,z]=degrees.map(v=>v*Math.PI/180),cx=Math.cos(x),sx=Math.sin(x),cy=Math.cos(y),sy=Math.sin(y),cz=Math.cos(z),sz=Math.sin(z);
  const m=[cz*cy,cz*sy*sx-sz*cx,cz*sy*cx+sz*sx,0,sz*cy,sz*sy*sx+cz*cx,sz*sy*cx-cz*sx,0,-sy,cy*sx,cy*cx,0,0,0,0,1];
  for(let i=0;i<3;i++)m[i*4+3]=p[i]-m[i*4]*p[0]-m[i*4+1]*p[1]-m[i*4+2]*p[2];return m;
}
function bakeSeals(geo){
  const named=new Map(geo.groups.map(g=>[g.name,g])),result={};
  const world=(n,pose,cache=new Map())=>{if(cache.has(n))return cache.get(n);const b=named.get(n),r=add(b.rotation,pose[n]||[0,0,0]);let m=local(b.origin,r);if(b.parent)m=mul(world(b.parent,pose,cache),m);cache.set(n,m);return m;};
  const descendant=(n,parent)=>{for(let b=named.get(n);b;b=named.get(b.parent))if(b.name===parent)return true;return false;};
  const audit=[];
  for(let seal=1;seal<=6;seal++){
    const pose=result[seal]={};
    for(const facing of ['front','rear'])for(const sideName of ['right','left']){
      const prefix=facing+'_'+sideName,side=sideName==='right'?-1:1,hand=prefix+'_hand';
      const verts=geo.cubes.filter(c=>descendant(c.parent,hand)&&!c.name.includes('sword')).flatMap(c=>{
        const m=world(c.parent,{});return c.quads.flatMap(q=>[0,1,2,3].map(i=>point(m,q.xyz.slice(i*3,i*3+3))));
      });
      const center=[0,1,2].map(i=>verts.reduce((sum,p)=>sum+p[i],0)/verts.length),hm=world(hand,{});
      const relative=center.map((v,i)=>v-hm[i*4+3]);
      const grip=[0,1,2].map(i=>hm[i]*relative[0]+hm[4+i]*relative[1]+hm[8+i]*relative[2]);
      const f=named.get(facing+'_facing'),spread=geo.kind==='skeletal'?1.9:1.05;
      const target=point(world(f.name,{}),[side*(seal===6?5.5:spread),f.origin[1]-(geo.kind==='skeletal'?.5:3.5),f.origin[2]-(seal===6?10:6.5)]);
      pose[prefix+'_upper']=[65,0,-side*25];pose[prefix+'_forearm']=[65,0,side*25];pose[hand]=[0,side*(seal===3?35:15),0];
      const distance=()=>{const p=point(world(hand,pose),grip);return p.reduce((s,v,i)=>s+(v-target[i])**2,0);};
      for(const step of [35,18,9,4,2,.7])for(let repeat=0;repeat<5;repeat++)for(const joint of [prefix+'_forearm',prefix+'_upper'])for(let axis=0;axis<3;axis++){
        const old=pose[joint][axis];let best=old,error=distance();
        for(const delta of [-step,step]){pose[joint][axis]=Math.max(-150,Math.min(150,old+delta));const next=distance();if(next<error){error=next;best=pose[joint][axis];}}
        pose[joint][axis]=best;
      }
      const error=Math.sqrt(distance());audit.push({seal,arm:prefix,error:Number(error.toFixed(3))});
    }
  }
  geo.sealReachAudit=audit;
  console.log(geo.kind+' baked hand-seal reach: max error '+Math.max(...audit.filter(a=>a.seal!==6).map(a=>a.error)).toFixed(3)+' model units');
  return result;
}

function clips(geo,ids) {
  const animators={};
  for(const g of geo.groups){
    if(!/_(upper|forearm|hand|finger[0-3])$/.test(g.name))continue;
    const side=g.name.includes('_left')?1:-1;
    const rear=g.name.startsWith('rear_');
    const target=geo.sealPoses[rear?2:1][g.name]||(g.name.includes('finger')?[(g.name.endsWith('0')||!rear&&g.name.endsWith('1'))?0:75,0,0]:[0,0,0]);
    animators[ids.get(g.name)]={name:g.name,type:'bone',keyframes:[0,.25,.6,.9,1.25].map((t,i)=>({uuid:crypto.randomUUID(),channel:'rotation',time:t,color:-1,interpolation:'catmullrom',data_points:[Object.fromEntries(['x','y','z'].map((axis,j)=>[axis,String(i===0||i===4?0:target[j]*(i===3?.35:1))]))]}))};
  }
  return [{uuid:crypto.randomUUID(),name:'madara.handseal_release',loop:'once',override:false,length:1.25,animators},
    {uuid:crypto.randomUUID(),name:'madara.chakra_idle',loop:'loop',override:false,length:3,animators:{[ids.get('torso')]:{name:'torso',type:'bone',keyframes:[0,1.5,3].map((t,i)=>({uuid:crypto.randomUUID(),channel:'position',time:t,color:-1,interpolation:'catmullrom',data_points:[{x:'0',y:i===1?'.16':'0',z:'0'}]}))}}}];
}

export function blockbench(geo,png){
  const ids=new Map(geo.groups.map(g=>[g.name,crypto.randomUUID()]));
  const elements=geo.cubes.map(c=>{
    const vertices={},faces={};
    for(let f=0;f<c.quads.length;f++){
      const q=c.quads[f],keys=[];const uv={};
      for(let i=0;i<4;i++){const key='v'+f+'_'+i;vertices[key]=q.xyz.slice(i*3,i*3+3).map((v,j)=>v-c.origin[j]);keys.push(key);uv[key]=q.uv.slice(i*2,i*2+2).map(v=>v*512);}
      // Mesh vertices are local to their origin; the Java model uses absolute
      // bind coordinates. Reflecting Java's downward Y also reverses winding.
      faces['f'+f]={vertices:keys.reverse(),uv,texture:0};
    }
    return {name:c.name,uuid:crypto.randomUUID(),type:'mesh',origin:c.origin,rotation:[0,0,0],vertices,faces,export:true,parent:c.parent};
  });
  function node(g){return {name:g.name,uuid:ids.get(g.name),origin:g.origin,rotation:g.rotation,export:true,isOpen:false,children:[...elements.filter(e=>e.parent===g.name).map(e=>e.uuid),...geo.groups.filter(c=>c.parent===g.name).map(node)]};}
  return {meta:{format_version:'4.10',model_format:'free',box_uv:false},name:'Madara Susanoo / '+geo.kind,resolution:{width:512,height:512},elements:elements.map(({parent,...e})=>e),outliner:geo.groups.filter(g=>!g.parent).map(node),textures:[{name:geo.texture,id:'0',uuid:crypto.randomUUID(),width:512,height:512,uv_width:512,uv_height:512,mode:'bitmap',source:'data:image/png;base64,'+png.toString('base64'),internal:true}],animations:clips(geo,ids)};
}

function verify(geo,ref){
  const named=new Map(geo.groups.map(g=>[g.name,g]));assert.equal(named.size,geo.groups.length);
  for(const g of geo.groups){const seen=new Set();for(let n=g;n;n=named.get(n.parent)){assert(!seen.has(n.name),'bone cycle '+n.name);seen.add(n.name);if(n.parent)assert(named.has(n.parent));}}
  for(const c of geo.cubes){
    assert(named.has(c.parent));const original=ref.bones.find(b=>b.name===c.sourceBone).cubes[c.sourceBox];
    const expected=(c.sourceRegion?slice(original,c.sourceRegion):original.quads).filter(q=>area(q)>1e-8);
    assert.equal(c.quads.length,expected.length);
    c.quads.forEach((q,i)=>{assert.deepEqual(q.uv,expected[i].uv,'UVs must match source');q.xyz.forEach((v,j)=>assert(Math.abs(v-(c.sourceOrigin[j%3]+expected[i].xyz[j]*c.sourceScale*(j%3===1?-1:1)))<1e-6));assert(q.uv.every(v=>v>=-1e-6&&v<=1.000001));});
  }
  for(const g of geo.groups)assert(geo.cubes.some(c=>c.parent===g.name)||geo.groups.some(child=>child.parent===g.name),'empty group '+g.name);
  assert(geo.cubes.every(c=>!/[Ww]ing|[Ff]lap/.test(c.sourceBone)),'no Sasuke wing geometry');
  console.log('PASS '+geo.kind+': '+geo.cubes.length+' source-derived parts, exact vertex/UV checks, valid hierarchy');
}

export async function build(){
  await fs.mkdir(path.join(assets,'models/custom/madara'),{recursive:true});
  for(const [source,kind] of [['skeleton','skeletal'],['clothed','humanoid'],['clothed','armored'],['winged','perfect']]){
    const ref=JSON.parse(await fs.readFile(path.join(root,'models/madara/reference/'+source+'.json'),'utf8'));
    const geo=generate(ref,kind);verify(geo,ref);
    const tex=decodePNG(await fs.readFile(path.join(assets,'textures',ref.texture)));
    // Preserve every atlas alpha/detail pixel. A single blue tint is applied
    // equally to original and duplicated parts, with no replacement color tiles.
    for(let i=0;i<tex.rgba.length;i+=4){tex.rgba[i]=Math.round(tex.rgba[i]*.27);tex.rgba[i+1]=Math.round(tex.rgba[i+1]*.62);tex.rgba[i+2]=Math.round(tex.rgba[i+2]*.97);}
    const png=encodePNG(tex.width,tex.height,tex.rgba);
    await fs.writeFile(path.join(assets,'textures',geo.texture),png);
    await fs.writeFile(path.join(assets,'models/custom/madara',kind+'.json'),JSON.stringify(geo));
    await fs.writeFile(path.join(root,'models/madara',kind+'.bbmodel'),JSON.stringify(blockbench(geo,png)));
  }
}
