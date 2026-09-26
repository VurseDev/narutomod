import fs from 'node:fs/promises';
import path from 'node:path';
import {root} from './png.mjs';
export async function call(name,args={}){
  const response=await fetch('http://127.0.0.1:39741/mcp',{method:'POST',headers:{Authorization:'Bearer '+process.env.BLOCKBENCH_MCP_TOKEN,Accept:'application/json, text/event-stream','Content-Type':'application/json'},body:JSON.stringify({jsonrpc:'2.0',id:Date.now(),method:'tools/call',params:{name,arguments:args}})});
  const result=await response.json();if(result.error||result.result?.isError)throw Error(JSON.stringify(result));
  return result.result;
}
const name=process.argv[2]||'get_project_summary';
if(name==='capture'){
  const checked=await call('check_model');
  console.log(checked.content.filter(c=>c.type==='text'));
  const findings=JSON.parse(checked.content.find(c=>c.type==='text').text).result.findings;
  // MCP 0.6.1 only counts Cube.all; a native mesh project legitimately has no
  // cubes. Hierarchy errors still block review; source/mesh checks run locally.
  if(findings.some(f=>f.severity==='error'&&f.code!=='NO_CUBES'))throw Error('Fix hierarchy errors before capture');
  const result=await call('capture_views',{views:['north','south','iso'],max_edge:512,format:'png'});
  const dir=path.join(root,'build/reports/madara/blockbench');await fs.mkdir(dir,{recursive:true});
  let i=0;for(const c of result.content||[]){if(c.type==='image'){const file=path.join(dir,'view-'+i+++'.png');await fs.writeFile(file,Buffer.from(c.data,'base64'));console.log(file);}else if(c.type==='text')console.log(c.text.slice(0,1500));}
} else {
  const result=await call(name,process.argv[3]?JSON.parse(process.argv[3]):{});
  for(const c of result.content||[])if(c.type==='text')console.log(c.text);
}
