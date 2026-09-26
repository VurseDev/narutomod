// Blockbench coordinates: Y up, front -Z, feet Y=0, neck Y=24.
// Whole integer cuboid sizes preserve the Minecraft 1.12 ModelBox export.
export function createTransformationGeometry() {
  const groups = [], cubes = [];
  const group = (name, origin, parent, rotation=[0,0,0]) => {
    groups.push({name,origin,parent,rotation}); return name;
  };
  const cube = (name, from, size, parent, material, pattern, origin, rotation=[0,0,0], inflate=0) => {
    cubes.push({name,from,to:from.map((v,i)=>v+size[i]),parent,origin:origin||from,
      rotation,inflate,material,pattern});
  };
  group('heaven_wings',[0,24,3],'body_body');
  // Mirrored anatomy: root tendon -> wrist -> broad palm -> four fingers + thumb.
  for (const side of [-1,1]) {
    const s=side===1?'left':'right';
    const point=(x,y,z)=>[side*x,y,z];
    const g=group('wing_'+s,point(3,23,3),'heaven_wings');
    const mirrorCube=(n, f, size, parent, material='wing', pattern='skin', origin, rotation=[0,0,0])=>{
      const from=point(...f); if(side===-1)from[0]-=size[0];
      cube('wing_'+s+'_'+n,from,size,parent,material,pattern,origin?point(...origin):undefined,
        [rotation[0],side*rotation[1],side*rotation[2]]);
    };
    mirrorCube('scapula',[2,20,2],[5,6,4],g,'wing','root');
    mirrorCube('wrist',[5,20,4],[8,5,4],g,'wing','tendon',[5,22,5],[0,0,14]);
    mirrorCube('wrist_ridge',[6,22,3],[7,2,3],g,'wing','tendon',[5,22,5],[0,0,14]);
    const palm=group('wing_'+s+'_palm',point(12,23,6),g);
    mirrorCube('palm_heel',[10,18,4.1],[8,9,4],palm,'wing','palm');
    mirrorCube('palm_main',[12,22,3.85],[13,9,4],palm,'wing','palm');
    mirrorCube('palm_crown',[13,30,4.7],[11,3,3],palm,'wing','palm');
    mirrorCube('palm_outer',[22,24,4.6],[5,5,3],palm,'wing','palm');
    mirrorCube('heel_lower',[12,16,4.5],[5,3,3],palm,'wing','tendon');
    mirrorCube('heel_tip',[13,14,5],[3,3,2],palm,'wing','tendon');
    // Raised tendons converge at the wrist. Fine lines remain painted into the atlas.
    for(let j=0;j<4;j++) mirrorCube('tendon_'+j,[13+j*2.3,24,3.6],[1,7,1],palm,
      'ridge','tendon',[13+j*2.3,24,5],[0,0,-j*7]);
    // Bases spread across the crown; bending phalanges taper into blunt claw tips.
    const fingers=[
      {n:'little',x:12.3,y:29.5,a:12,l:[5,4,2],w:3},
      {n:'ring',x:16.1,y:31,a:-8,l:[7,5,2],w:4},
      {n:'middle',x:20.1,y:31,a:-27,l:[8,5,2],w:4},
      {n:'index',x:23.5,y:29,a:-51,l:[7,5,2],w:4},
      {n:'thumb',x:23.5,y:24,a:-115,l:[6,4,2],w:4}
    ];
    for(const f of fingers){
      const fg=group('wing_'+s+'_'+f.n,point(f.x,f.y,6),palm,[0,0,side*f.a]);
      // The absolute coordinates are in the unrotated parent frame.
      mirrorCube(f.n+'_proximal',[f.x-f.w/2,f.y,4.5],[f.w,f.l[0],3],fg,'wing','finger');
      const knY=f.y+f.l[0]-1;
      mirrorCube(f.n+'_knuckle',[f.x-f.w/2-.5,knY,4],[f.w+1,2,4],fg,'ridge','knuckle');
      const tipg=group('wing_'+s+'_'+f.n+'_curl',point(f.x,f.y+f.l[0],6),fg,[-8,0,side*13]);
      mirrorCube(f.n+'_middle',[f.x-1.5,f.y+f.l[0],4.5],[3,f.l[1],3],tipg,'wing','finger');
      mirrorCube(f.n+'_tip',[f.x-1,f.y+f.l[0]+f.l[1]-.25,5],[2,f.l[2],2],tipg,'wing','tip');
      mirrorCube(f.n+'_nail',[f.x-.5,f.y+f.l[0]+f.l[1]+.1,4.85],[1,1,1],tipg,'nail','nail');
    }
    // Thick skin webs fill only the bases. Negative gaps expose the five digits.
    mirrorCube('web_ring',[13.5,31,5],[3,5,2],palm,'web','web',[15,31,6],[0,0,6]);
    mirrorCube('web_middle',[17.5,32,5],[4,5,2],palm,'web','web',[19,32,6],[0,0,-18]);
    mirrorCube('web_index',[22,30,5],[4,5,2],palm,'web','web',[23,31,6],[0,0,-39]);
    mirrorCube('web_thumb',[25,25,5],[4,4,2],palm,'web','web',[25,27,6],[0,0,-55]);
  }
  return {groups,cubes};
}

export function boxFaces(u,v,w,h,d,s=1) {
  const r=a=>a.map(n=>n*s);
  return {
    east:r([u,v+d,u+d,v+d+h]), north:r([u+d,v+d,u+d+w,v+d+h]),
    west:r([u+d+w,v+d,u+2*d+w,v+d+h]), south:r([u+2*d+w,v+d,u+2*d+2*w,v+d+h]),
    up:r([u+d+w,v+d,u+d,v]), down:r([u+d+2*w,v,u+d+w,v+d])
  };
}

export function standardSkinFaces(slim=false) {
  const a=slim?3:4;
  return {
    body_head_cube:boxFaces(0,0,8,8,8,2),
    body_body_cube:boxFaces(16,16,8,12,4,2),
    body_arm_right_cube:boxFaces(40,16,a,12,4,2),
    body_arm_left_cube:boxFaces(32,48,a,12,4,2),
    body_leg_right_cube:boxFaces(0,16,4,12,4,2),
    body_leg_left_cube:boxFaces(16,48,4,12,4,2)
  };
}
