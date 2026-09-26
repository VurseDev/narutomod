// Hand-authored Minecraft skin nets, 2 texels per model unit. No antialiasing.
import {boxFaces} from './geometry.mjs';
const C={skin:'#C7B29A',skinHi:'#D9C5AC',skinShade:'#AE957F',skinDark:'#8C7568',
  ash:'#928B80',ashHi:'#ACA394',ashShade:'#7A756F',ashDark:'#615C5B',
  hair:'#202832',hairHi:'#354450',hairShade:'#161D28',
  navy:'#252A39',navyHi:'#343A4C',navyShade:'#1A1E2C',
  wrap:'#A3ADAC',wrapHi:'#BCC4BD',wrapShade:'#7D8A8B',
  rope:'#6D557D',ropeHi:'#937CA2',ropeShade:'#493C60',ink:'#17171D',
  sclera:'#1C1B23',eye:'#C2A266',white:'#D6D0BB',red:'#9A4D54'};
const rgba=c=>c?c.slice(1).match(/../g).map(x=>parseInt(x,16)).concat(255):[0,0,0,0];
const pal=Object.fromEntries(Object.entries(C).map(([k,v])=>[k,rgba(v)]));
function canvas(w,h){return{w,h,data:new Uint8Array(w*h*4)};}
function put(a,x,y,c){x=Math.floor(x);y=Math.floor(y);if(x<0||y<0||x>=a.w||y>=a.h)return;
  a.data.set(Array.isArray(c)?c:pal[c]||rgba(c),(y*a.w+x)*4);}
function rect(a,x,y,w,h,c){for(let j=y;j<y+h;j++)for(let i=x;i<x+w;i++)put(a,i,j,c);}
function disk(a,x,y,r,c){for(let j=Math.floor(y-r);j<=y+r;j++)for(let i=Math.floor(x-r);i<=x+r;i++)
  if((i-x)**2+(j-y)**2<r*r+.4)put(a,i,j,c);}
function line(a,x,y,x2,y2,c,size=1){const n=Math.max(Math.abs(x2-x),Math.abs(y2-y))*2+1;
  for(let k=0;k<=n;k++)disk(a,x+(x2-x)*k/n,y+(y2-y)*k/n,size/2,c);}
function curve(a,p,c='ink',size=1.7){for(let n=0;n<=70;n++){
  const t=n/70,s=1-t,x=s*s*s*p[0]+3*s*s*t*p[2]+3*s*t*t*p[4]+t*t*t*p[6];
  const y=s*s*s*p[1]+3*s*s*t*p[3]+3*s*t*t*p[5]+t*t*t*p[7];
  disk(a,x,y,size/2,c);}}
function flame(a,p,width=2){curve(a,p,'ink',width);const [x,y]=p.slice(-2);disk(a,x,y,.6,'ink');}
function mapFace(atlas,face,uv){const [u1,v1,u2,v2]=uv;
  for(let y=0;y<face.h;y++)for(let x=0;x<face.w;x++){
    const ax=u1<u2?u1+x:u1-1-x,ay=v1<v2?v1+y:v1-1-y;
    atlas.data.set(face.data.subarray((y*face.w+x)*4,(y*face.w+x+1)*4),(ay*atlas.w+ax)*4);
  }
}
function facesFor(part,slim){const w=part.includes('arm')?(slim?3:4):part==='head'||part==='body'?8:4;
  const h=part==='head'?8:12,d=part==='head'?8:4;
  const offsets={head:[0,0],body:[16,16],arm_right:[40,16],arm_left:[32,48],leg_right:[0,16],leg_left:[16,48]};
  return boxFaces(...offsets[part],w,h,d,2);
}
function skinBase(part,face,w,h,stage2){
  const a=canvas(w,h),skin=stage2?'ash':'skin',shade=stage2?'ashShade':'skinShade',hi=stage2?'ashHi':'skinHi',dark=stage2?'ashDark':'skinDark';
  rect(a,0,0,w,h,skin);
  rect(a,0,0,1,h,shade);rect(a,w-1,0,1,h,shade);rect(a,1,0,w-2,1,hi);
  if(face==='down')rect(a,0,0,w,h,shade);
  if(part==='head'){
    if(face==='up'){
      rect(a,0,0,w,h,'hair');for(let x=2;x<w-1;x+=4)line(a,x,1,x+1,h-3,'hairHi');
    }else if(face==='north'){
      rect(a,0,0,w,4,'hair');rect(a,0,3,3,5,'hair');rect(a,w-3,2,3,7,'hair');
      rect(a,6,3,3,2,'hair');put(a,7,5,'hair');
      // Angular brows, thin expressive eyes and one-pixel irises.
      line(a,2,7,5,8,'hair');line(a,10,8,13,7,'hair');
      rect(a,2,9,4,2,stage2?'sclera':'white');rect(a,10,9,4,2,stage2?'sclera':'white');
      put(a,4,9,stage2?'eye':'red');put(a,11,9,stage2?'eye':'red');
      put(a,5,10,dark);put(a,10,10,dark);
      line(a,7,11,7,12,shade);put(a,8,12,hi);
      line(a,6,14,9,14,dark);line(a,6,15,9,15,shade);
      if(stage2){
        // Recognizable four-point cross/star over the bridge of the nose.
        line(a,7.5,8,7.5,13.5,'ink',1.5);
        line(a,5,11,10,11,'ink',1.5);rect(a,7,10,2,3,'ink');
      }
    }else if(face==='south'){
      rect(a,0,0,w,h-3,'hair');for(let x=1;x<w-1;x+=3){rect(a,x,2,1,h-4,'hairHi');rect(a,x,h-4,2,2,'hair');}
    }else if(face==='east'||face==='west'){
      rect(a,0,0,w,5,'hair');rect(a,face==='east'?0:w-6,4,6,h-7,'hair');
      rect(a,face==='east'?w-6:3,8,3,4,shade);put(a,face==='east'?w-5:4,9,hi);
    }
  }else if(part==='body'){
    if(stage2){
      if(face==='north'){
        line(a,2,3,6,4,shade);line(a,9,4,13,3,shade);
        line(a,7,3,7,10,shade);line(a,3,8,6,9,shade);line(a,9,9,12,8,shade);
        line(a,7,12,7,17,shade);line(a,5,12,6,12,hi);line(a,9,12,11,12,hi);
        line(a,3,16,5,18,shade);line(a,10,18,12,16,shade);put(a,8,17,dark);
      }else if(face==='south'){
        line(a,7,2,7,17,shade);line(a,2,4,5,7,shade);line(a,12,4,9,7,shade);
        line(a,2,14,5,17,shade);line(a,12,14,9,17,shade);
      }
    }else{
      rect(a,0,0,w,18,'navy');rect(a,0,0,2,18,'navyShade');rect(a,w-2,0,2,18,'navyShade');
      if(face==='north'){
        for(let y=0;y<9;y++)rect(a,5+Math.floor(y/3),y,6-2*Math.floor(y/3),1,skin);
        line(a,4,1,7,9,'navyHi');line(a,11,1,8,9,'navyHi');
        line(a,2,11,3,16,'navyHi');line(a,12,10,12,16,'navyHi');
      }
    }
    rect(a,0,h-6,w,6,'wrap');line(a,0,h-5,w-1,h-3,'wrapHi');
    rect(a,0,h-3,w,3,'ropeShade');
    for(let x=0;x<w;x++){put(a,x,h-3+(x%4<2?0:1),'ropeHi');put(a,x,h-2+(x%4<2?0:1),'rope');}
  }else if(part.includes('arm')){
    if(!stage2){rect(a,0,0,w,7,'navy');rect(a,0,5,w,2,'navyHi');}
    else{
      line(a,2,4,2,12,hi);line(a,w-2,7,w-2,15,shade);line(a,1,12,2,13,shade);
    }
    line(a,1,h-7,w-2,h-7,shade);
    if(face==='north'||face==='south'){
      for(let x=1;x<w-1;x+=2){line(a,x,h-3,x,h-1,shade);put(a,x,h-4,hi);}
    }
  }else{
    rect(a,0,0,w,h,'navy');rect(a,0,0,1,h,'navyShade');rect(a,w-1,0,1,h,'navyShade');
    line(a,2,3,2,h-8,'navyHi');line(a,w-2,7,w-2,h-7,'navyShade');
    // Tied waist cloth, dark cropped trousers, shins and open-toed sandals.
    for(let y=0;y<7;y++){const end=Math.max(1,w-Math.floor(y*.75));rect(a,0,y,end,1,y%3===0?'wrapHi':'wrap');if(end<w)put(a,end,y,'wrapShade');}
    rect(a,0,h-7,w,5,skin);rect(a,0,h-7,w,1,shade);
    rect(a,0,h-5,w,2,'navy');rect(a,0,h-1,w,1,'navyShade');
    if(face==='north'){for(let x=1;x<w-1;x+=2)put(a,x,h-2,shade);}
  }
  return a;
}
function curseFace(part,face,w,h){
  const a=canvas(w,h);
  if(face==='up'||face==='down'){
    if(part==='head'&&face==='up')return a;
    flame(a,[0,h*.6,w*.4,h*.2,w*.5,h*.8,w,h*.35],1.7);return a;
  }
  if(part==='head'){
    if(face==='north'){
      flame(a,[14,-1,10,1,15,3,11,5],2);flame(a,[11,5,8,6,12,8,14,7],1.8);
      flame(a,[16,8,12,8,16,12,12,12],2);flame(a,[12,12,8,10,12,15,8,16],2);
      flame(a,[12,12,10,13,9,12,8,12],1.4);flame(a,[15,3,13,3,14,5,16,5],1.4);
    }else if(face==='south'){
      // Three curled tomoe at the left rear neck; distinct and compact.
      const cx=4,cy=12;
      for(let k=0;k<3;k++){
        const ang=k*2*Math.PI/3;const x=cx+2*Math.cos(ang),y=cy+2*Math.sin(ang);
        disk(a,x,y,1.1,'ink');curve(a,[x,y,x+2*Math.cos(ang+.8),y+2*Math.sin(ang+.8),cx+3*Math.cos(ang+1.5),cy+3*Math.sin(ang+1.5),cx+2*Math.cos(ang+1.7),cy+2*Math.sin(ang+1.7)],'ink',1.1);
      }
    }else if(face==='west'){
      flame(a,[0,14,3,16,3,10,7,12],2);flame(a,[7,12,12,14,8,7,13,8],2);
      flame(a,[13,8,16,9,13,4,16,3],1.7);
    }
  }else if(part==='body'){
    const left=face==='north'?w-2:2;
    flame(a,[left,-1,left-4,4,left+1,6,left-3,9],2.3);
    flame(a,[left-3,9,left-6,11,left+1,14,left-2,17],2);
    flame(a,[left-2,17,left-6,20,left-2,22,left-6,25],1.8);
    flame(a,[left-3,9,left-7,7,left-7,12,left-10,11],1.8);
    flame(a,[left-2,17,left+2,18,left+1,13,left+4,14],1.5);
    if(face==='south')flame(a,[7,1,12,4,7,7,12,9],1.6);
  }else if(part.includes('arm')){
    const dense=part==='arm_left';
    if(!dense&&face==='east')return a;
    const x=dense?w*.5:w*.7;
    flame(a,[x,-1,x-4,3,x+4,4,x,8],dense?2.1:1.4);
    flame(a,[x,8,x-5,11,x+3,12,x-1,16],dense?2:1.3);
    flame(a,[x-1,16,x-4,19,x+4,20,x,24],dense?1.8:1.2);
    if(dense){
      flame(a,[x,8,x+4,7,x+1,3,w+1,4],1.8);
      flame(a,[x-1,16,x+3,17,x+2,13,w+1,13],1.5);
      flame(a,[x,21,x-3,23,2,18,-1,19],1.5);
    }
  }else{
    if(part==='leg_right'&&(face==='west'||face==='south'))return a;
    const x=w*.6;
    flame(a,[x,-1,x-3,2,x+2,5,x-1,7],1.5);
    if(part==='leg_left')flame(a,[x-1,7,x-4,10,x+3,11,x-1,15],1.2);
  }
  return a;
}
export function createSkinArt({slim=false}={}){
  const stage1=canvas(128,128),stage2=canvas(128,128),preview=canvas(128,128),ignition=canvas(128,128);
  for(const part of ['head','body','arm_right','arm_left','leg_right','leg_left']){
    for(const [face,uv]of Object.entries(facesFor(part,slim))){
      const w=Math.abs(uv[2]-uv[0]),h=Math.abs(uv[3]-uv[1]);
      const mask=curseFace(part,face,w,h),normal=skinBase(part,face,w,h,false);
      // Stage 2 is an overlay too: retain the black flame network and add
      // the compact star/cross over the bridge of the nose. The player's
      // own skin, clothes, and hair remain visible underneath it.
      const second=curseFace(part,face,w,h);
      if(part==='head'&&face==='north'){
        line(second,w*.5,Math.max(0,h*.28),w*.5,Math.min(h-1,h*.78),'ink',1.5);
        line(second,w*.32,h*.54,w*.68,h*.54,'ink',1.5);
        rect(second,Math.max(0,Math.floor(w*.44)),Math.max(0,Math.floor(h*.46)),Math.max(1,Math.floor(w*.12)),Math.max(1,Math.floor(h*.18)),'ink');
      }
      for(let i=0;i<normal.data.length;i+=4)if(mask.data[i+3])normal.data.set(mask.data.subarray(i,i+4),i);
      mapFace(stage1,mask,uv);mapFace(preview,normal,uv);mapFace(stage2,second,uv);
    }
  }
  // A matched ignition pass: the same pattern, two warm orange-red ink values.
  for(let y=0;y<128;y++)for(let x=0;x<128;x++){
    const i=(y*128+x)*4;if(stage1.data[i+3]){
      const inner=x>0&&y>0&&x<127&&y<127&&stage1.data[i-4+3]&&stage1.data[i+4+3]&&stage1.data[i-512+3]&&stage1.data[i+512+3];
      put(ignition,x,y,inner?'#FFD085':'#D85D35');
    }
  }
  return{width:128,height:128,stage1:stage1.data,ignition:ignition.data,stage2:stage2.data,previewStage1:preview.data};
}
