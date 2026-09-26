'use strict';

// Atlas-safe derivative of the existing Sharingan eye assets. Run with NODE_PATH
// pointing at a node_modules directory that contains sharp. --check is read-only.
const fs = require('node:fs/promises');
const path = require('node:path');
const assert = require('node:assert/strict');
const sharp = require('sharp');
const root = path.resolve(__dirname, '../..');
const assets = path.join(root, 'src/main/resources/assets/narutomod');
const source = path.join(root, 'models/madara/eyes');
const templatePath = path.join(assets, 'textures/mangekyosharinganhelmet_sasuke.png');
const variants = [
  {name:'Madara MS', svg:'madara_ms.svg', suffix:'madara', item:'mangekyosharinganmadarahelmet'},
  {name:'Madara EMS', svg:'madara_ems.svg', suffix:'madara_eternal', item:'mangekyosharinganmadaraeternalhelmet'}
];
const irisRects = [{left:816, top:153, width:34, height:33}, {left:943, top:153, width:34, height:33}];
const highlight = {left:768, top:0, width:256, height:256};
let checks = 0;
function check(value, reason) { checks++; assert(value, reason); }
function worn(v) { return path.join(assets, 'textures', `mangekyosharinganhelmet_${v.suffix}.png`); }
function icon(v) { return path.join(assets, 'textures/blocks', `mangekyosharingan_${v.suffix}.png`); }
async function raw(file) { return sharp(file).ensureAlpha().raw().toBuffer({resolveWithObject:true}); }
function inHighlight(x,y) { return x>=highlight.left && x<highlight.left+highlight.width && y>=0 && y<256; }

async function build() {
  const template = await raw(templatePath);
  assert.equal(template.info.width, 2048);
  assert.equal(template.info.height, 512);
  const cleared = Buffer.from(template.data);
  for(let y=0;y<256;y++) cleared.fill(0, (y*2048+768)*4, (y*2048+1024)*4);
  for(const v of variants) {
    const svg = await fs.readFile(path.join(source,v.svg));
    const iris = await sharp(svg).resize(34,33,{fit:'fill'}).png().toBuffer();
    await sharp(cleared,{raw:template.info}).composite(irisRects.map(rect=>({input:iris,left:rect.left,top:rect.top}))).png().toFile(worn(v));
    await sharp(svg).resize(64,64).png().toFile(icon(v));
  }
  await preview();
}

function svgLabel(text,x,y,size=18,color='#d9e3ef') { return `<text x="${x}" y="${y}" fill="${color}" font-size="${size}" font-family="Arial,sans-serif">${text}</text>`; }
async function preview() {
  const width=1100,height=760, pieces=[];
  let labels=svgLabel('Madara Mangekyo Sharingan',32,42,28)+svgLabel('Actual 64px icons, exact legacy eye atlas, and an offline skin-fit mockup',32,72,16,'#92a3b7');
  for(let i=0;i<variants.length;i++) {
    const v=variants[i],x=36+i*540;
    labels+=svgLabel(v.name,x,118,22)+svgLabel('Inventory icon (4x nearest)',x,412,15)+svgLabel('Worn UV / player face preview',x,716,15);
    const large=await sharp(icon(v)).resize(256,256,{kernel:'nearest'}).png().toBuffer();
    pieces.push({input:large,left:x,top:136});
    const pattern=await sharp(path.join(source,v.svg)).resize(184,184).png().toBuffer();
    pieces.push({input:pattern,left:x+300,top:170});
    const texture=await fs.readFile(worn(v));
    const face=await sharp(texture).extract({left:256,top:256,width:256,height:256}).png().toBuffer();
    const iris=await sharp(texture).extract(highlight).png().toBuffer();
    const skin=Buffer.from(`<svg xmlns="http://www.w3.org/2000/svg" width="256" height="256"><rect width="256" height="256" fill="#b88b6c"/><path d="M0 0H256V64H224V32H64V64H0Z" fill="#49382e"/><rect x="0" y="192" width="32" height="64" fill="#b18267"/><rect x="224" y="160" width="32" height="96" fill="#aa7b60"/><rect x="96" y="192" width="64" height="32" fill="#a5745b"/><rect x="96" y="224" width="64" height="12" fill="#6e4d3c"/></svg>`);
    const mock=await sharp(skin).composite([{input:face},{input:iris}]).png().toBuffer();
    pieces.push({input:mock,left:x,top:440});
    const rawFace=await sharp(face).composite([{input:iris}]).png().toBuffer();
    pieces.push({input:rawFace,left:x+264,top:440});
  }
  labels+=svgLabel('Pattern: ShounenSuki / Ju gatsu mikka, CC BY-SA 3.0. Atlas fit: existing mod. Preview is not a game screenshot.',32,746,13,'#92a3b7');
  const layout=Buffer.from(`<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}"><rect width="100%" height="100%" fill="#111722"/><path d="M548 106V720" stroke="#2e3b4f"/>${labels}</svg>`);
  await sharp(layout).composite(pieces).png().toFile(path.join(source,'madara_eyes_preview.png'));
}

async function verify() {
  const base=await raw(templatePath), masks=[];
  for(const v of variants) {
    const image=await raw(worn(v)), item=await raw(icon(v));
    check(image.info.width===2048 && image.info.height===512,'legacy worn atlas dimensions preserved');
    check(item.info.width===64 && item.info.height===64,'inventory icon matches existing 64px assets');
    let changedOutside=0, opaqueOutsideIris=0, changedInside=0;
    for(let y=0;y<512;y++) for(let x=0;x<2048;x++) {
      const offset=(y*2048+x)*4;
      const changed=!base.data.subarray(offset,offset+4).equals(image.data.subarray(offset,offset+4));
      if(!inHighlight(x,y)) changedOutside+=changed?1:0;
      else {
        changedInside+=changed?1:0;
        if(!irisRects.some(r=>x>=r.left&&x<r.left+r.width&&y>=r.top&&y<r.top+r.height) && image.data[offset+3]) opaqueOutsideIris++;
      }
    }
    check(changedOutside===0,'eyelids, eye whites, and all non-iris atlas pixels are byte-for-byte preserved');
    check(changedInside>400,'both legacy Sasuke irises replaced');
    check(opaqueOutsideIris===0,'no glow or iris pixels leak outside the two existing bounds');
    const left=await sharp(worn(v)).extract(irisRects[0]).raw().toBuffer();
    const right=await sharp(worn(v)).extract(irisRects[1]).raw().toBuffer();
    check(left.equals(right),'left/right eyes use the same non-mirrored canonical pattern');
    const model=JSON.parse(await fs.readFile(path.join(assets, 'models/item',v.item+'.json'),'utf8'));
    check(model.parent==='narutomod:custom/mangekyo_amaterasu','existing item model/display transform reused');
    check(model.textures['1']===`narutomod:blocks/mangekyosharingan_${v.suffix}`,'model resolves the correct unique texture');
    masks.push(item.data);
  }
  check(!masks[0].equals(masks[1]),'MS and EMS remain visually distinct');
  const java=await fs.readFile(path.join(root,'src/main/java/net/narutomod/procedure/ProcedureSharinganHelmetTickEvent.java'),'utf8');
  check(java.includes('nextInt(3)') && java.includes('ItemMangekyoSharinganMadara.helmet'),'all three families reachable through the awakening roll');
  const sources=await Promise.all(['MadaraTemporalController.java','entity/EntitySusanooMadara.java','item/ItemMangekyoSharinganMadara.java'].map(f=>fs.readFile(path.join(root,'src/main/java/net/narutomod',f),'utf8')));
  const required=new Set(['item.mangekyosharinganmadarahelmet.name','item.mangekyosharinganmadaraeternalhelmet.name']);
  for(const text of sources) for(const m of text.matchAll(/"((?:tooltip\.madara\.|message\.narutomod\.madara_|entity\.susanoomadara\.)[^"\s]+)"/g)) required.add(m[1]);
  for(const m of sources[0].matchAll(/status\([^,\n]+,"([a-z_]+)"/g)) required.add('message.madara.temporal_'+m[1]);
  for(const lang of ['en_us','pt_br']) {
    const lines=(await fs.readFile(path.join(assets,'lang',lang+'.lang'),'utf8')).split(/\r?\n/);
    for(const key of required) check(lines.filter(l=>l.startsWith(key+'=')).length===1,`${lang}: exactly one translation for ${key}`);
  }
  console.log(`PASS: ${checks} Madara eye asset, atlas-fit, acquisition, and localization checks.`);
}

(async()=> { if(!process.argv.includes('--check')) await build(); await verify(); })().catch(error=>{console.error(error);process.exitCode=1;});
