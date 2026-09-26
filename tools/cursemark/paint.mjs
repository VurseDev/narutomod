import fs from 'node:fs/promises';
import path from 'node:path';
import {mcp,workspace} from './build.mjs';

const palette={
  '.':'#98897C',h:'#B6A694',l:'#A99885',s:'#7C6E65',d:'#5A5150',k:'#403A40',
  b:'#303C49',c:'#40515F',e:'#586B77',f:'#25303C',g:'#1B2430',n:'#544E50',t:'#746B66'
};
function line(grid,x1,y1,x2,y2,c){
  const steps=Math.max(Math.abs(x2-x1),Math.abs(y2-y1),1);
  for(let i=0;i<=steps;i++){
    const x=Math.round(x1+(x2-x1)*i/steps),y=Math.round(y1+(y2-y1)*i/steps);
    if(grid[y]?.[x]!==undefined)grid[y][x]=c;
  }
}
function detail(c,face,w,h){
  let base=c.material==='hair'?'b':c.material==='nail'?'n':c.material==='web'?'s':'.';
  const a=Array.from({length:h},()=>Array(w).fill(base));
  const front=face==='north'||face==='south';
  if(c.material==='hair'){
    for(let y=0;y<h;y++)for(let x=0;x<w;x++){
      a[y][x]=(x===w-1||y===h-1)?'g':(x===0?'f':'b');
      if(w>2&&x===Math.floor(w*.45)&&y<h-2)a[y][x]='c';
      if(w>3&&x===Math.floor(w*.45)&&y>1&&y<h*.5)a[y][x]='e';
    }
    if(face==='up')a[0]=a[0].map((_,i)=>i%3===1?'e':'c');
  }else if(c.material==='nail'){
    a[0]=a[0].map(()=>face==='up'?'t':'n');
  }else{
    for(let y=0;y<h;y++)for(let x=0;x<w;x++){
      if(y===h-1||x===w-1)a[y][x]='s';
      else if(y===0||x===0)a[y][x]='l';
      if(face==='down')a[y][x]='s';
      if(face==='up'&&y>0&&x<w-1)a[y][x]='l';
    }
    if(front&&c.pattern==='palm'&&w>=7&&h>=6){
      // Fan-shaped metacarpal tendons radiate from the wrist; restrained sharp creases.
      for(let j=1;j<=3;j++){
        const x=Math.round(w*j/4);
        line(a,x,1,Math.round(w*.28+(x-w*.28)*.45),h-2,'s');
        line(a,x+1,1,Math.round(w*.28+(x-w*.28)*.45)+1,h-3,'l');
      }
      line(a,1,Math.round(h*.62),Math.round(w*.35),Math.round(h*.75),'d');
      line(a,Math.round(w*.35),Math.round(h*.75),Math.round(w*.7),Math.round(h*.58),'s');
      if(h>7)line(a,w-3,2,w-2,4,'d');
    }
    if(front&&['finger','tendon','root'].includes(c.pattern)){
      if(w>=3)line(a,Math.floor(w/2),1,Math.floor(w/2),h-2,'l');
      if(h>4){line(a,0,h-2,Math.max(0,w-2),h-2,'d');if(w>3)line(a,1,Math.floor(h/2),2,Math.floor(h/2)+1,'s');}
    }
    if(front&&c.pattern==='knuckle'){
      line(a,1,0,w-2,0,'h');line(a,1,h-1,w-2,h-1,'d');
      if(w>=4)a[0][Math.floor(w/2)]='l';
    }
    if(c.pattern==='web'&&front){
      line(a,0,h-1,w-1,0,'d');
      line(a,1,h-1,w-1,1,'l');
    }
  }
  // Mirror anatomical crease flow, not generic noise, on the opposite hand.
  if(c.name.startsWith('wing_right_')&&front)a.forEach(row=>row.reverse());
  return a.map(row=>row.join(''));
}

export async function paintAccessories(){
  const geo=JSON.parse(await fs.readFile(path.join(workspace,'models/cursemark/.geometry-data.json'),'utf8'));
  await mcp('shade_model_base',{texture:'curse_mark_heaven_atlas.png',cubes:geo.cubes.map(c=>c.name),
    base:'#98897C',top_light:.08,bottom_dark:.12,edge_darken:.08,noise:0,blur:0,crisp:true,seed:771,
    regions:[{match:'^hair_',color:'#303C49'},{match:'_web_',color:'#7C6E65'},{match:'_nail$',color:'#544E50'}]});
  const state=(await mcp('get_elements')).result;
  const faces=[];
  for(const c of geo.cubes){
    const live=state.cubes.find(x=>x.name===c.name);
    for(const [face,f]of Object.entries(live.faces)){
      const w=Math.round(Math.abs(f.uv[2]-f.uv[0])),h=Math.round(Math.abs(f.uv[3]-f.uv[1]));
      faces.push({cube:c.name,face,rows:detail(c,face,w,h)});
    }
  }
  for(let i=0;i<faces.length;i+=100)await mcp('paint_face_grid',{
    texture:'curse_mark_heaven_atlas.png',palette,faces:faces.slice(i,i+100)});
  console.log('Painted',faces.length,'accessory faces using',Object.keys(palette).length,'intentional colors');
  console.log((await mcp('check_model')).result.summary);
  const shot=await mcp('capture_views',{views:['iso'],max_edge:768,format:'png'});
  await fs.mkdir(path.join(workspace,'docs/cursemark'),{recursive:true});
  await fs.writeFile(path.join(workspace,'docs/cursemark/work_in_progress.png'),Buffer.from(shot.images[0].data,'base64'));
}

if(process.argv[2]==='accessories')await paintAccessories();
