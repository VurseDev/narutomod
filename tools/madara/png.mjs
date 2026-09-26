import path from 'node:path';
import zlib from 'node:zlib';
import {fileURLToPath} from 'node:url';
export const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../..');
export function decodePNG(bytes) {
  let pos=8,width,height,type,depth,palette,alpha,parts=[];
  while(pos<bytes.length) {
    const n=bytes.readUInt32BE(pos),tag=bytes.toString('ascii',pos+4,pos+8),data=bytes.subarray(pos+8,pos+8+n);pos+=n+12;
    if(tag==='IHDR'){width=data.readUInt32BE(0);height=data.readUInt32BE(4);depth=data[8];type=data[9];if(data[12])throw new Error('Interlaced PNG not supported');}
    if(tag==='PLTE')palette=data;if(tag==='tRNS')alpha=data;if(tag==='IDAT')parts.push(data);
  }
  const channels=({0:1,2:3,3:1,4:2,6:4})[type];
  if(depth!==8||!channels)throw new Error('Expected 8-bit PNG, got '+depth+'/'+type);
  const raw=zlib.inflateSync(Buffer.concat(parts)),stride=width*channels,scan=Buffer.alloc(stride*height),rgba=Buffer.alloc(width*height*4);
  const paeth=(a,b,c)=>{const p=a+b-c,aa=Math.abs(p-a),bb=Math.abs(p-b),cc=Math.abs(p-c);return aa<=bb&&aa<=cc?a:bb<=cc?b:c;};
  for(let y=0;y<height;y++){const filter=raw[y*(stride+1)];for(let x=0;x<stride;x++) {
    const i=y*stride+x,a=x>=channels?scan[i-channels]:0,b=y?scan[i-stride]:0,c=y&&x>=channels?scan[i-stride-channels]:0;
    scan[i]=(raw[y*(stride+1)+1+x]+[0,a,b,Math.floor((a+b)/2),paeth(a,b,c)][filter])&255;
  }}
  for(let p=0;p<width*height;p++){const i=p*channels,o=p*4;
    if(type===6||type===2){rgba[o]=scan[i];rgba[o+1]=scan[i+1];rgba[o+2]=scan[i+2];rgba[o+3]=type===6?scan[i+3]:255;}
    else if(type===3){rgba[o]=palette[scan[i]*3];rgba[o+1]=palette[scan[i]*3+1];rgba[o+2]=palette[scan[i]*3+2];rgba[o+3]=alpha?.[scan[i]]??255;}
    else {rgba[o]=rgba[o+1]=rgba[o+2]=scan[i];rgba[o+3]=type===4?scan[i+1]:255;}
  }return {width,height,rgba};
}
export function encodePNG(width,height,rgba) {
  function crc(b){let c=-1;for(const x of b){c^=x;for(let i=0;i<8;i++)c=(c>>>1)^((c&1)?0xedb88320:0);}return(c^-1)>>>0;}
  function chunk(tag,data){const head=Buffer.alloc(8),tail=Buffer.alloc(4);head.writeUInt32BE(data.length);head.write(tag,4);tail.writeUInt32BE(crc(Buffer.concat([Buffer.from(tag),data])));return Buffer.concat([head,data,tail]);}
  const ihdr=Buffer.alloc(13);ihdr.writeUInt32BE(width);ihdr.writeUInt32BE(height,4);ihdr[8]=8;ihdr[9]=6;
  const raw=Buffer.alloc((width*4+1)*height);for(let y=0;y<height;y++)rgba.copy(raw,y*(width*4+1)+1,y*width*4,(y+1)*width*4);
  return Buffer.concat([Buffer.from([137,80,78,71,13,10,26,10]),chunk('IHDR',ihdr),chunk('IDAT',zlib.deflateSync(raw)),chunk('IEND',Buffer.alloc(0))]);
}
