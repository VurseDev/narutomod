package net.narutomod.client;

import com.google.gson.*;
import java.io.Reader;
import java.util.*;
import net.narutomod.SusanooCastProfile;

/** Blockbench's exact hierarchical pivots and face UVs. Pure Java so the actual
 * animated mesh can be regression-tested and previewed without a GL context. */
public final class MadaraModel {
    public static final class Bone {
        public final String name, parent;
        public final double[] pivot, rotation;
        Bone(JsonObject json) {name=json.get("name").getAsString();parent=json.has("parent")?json.get("parent").getAsString():null;pivot=vec(json.getAsJsonArray("origin"));rotation=vec(json.getAsJsonArray("rotation"));}
    }
    public static final class Face {
        public final String bone, cube;
        public final double[] xyz, uv;
        public final float light;
        Face(String owner,String name,double[] points,double[] texture,float shade) {bone=owner;cube=name;xyz=points;uv=texture;light=shade;}
    }
    public static final class Pose {
        public int stage, phase, profile, armMask=3;
        public float age, castAge, walk, stride, headYaw, headPitch, attack;
        public boolean swords, bind;
        public int recoveryPhase=SusanooCastProfile.HOLD;
        public float recoveryAge;
    }
    public final List<Bone> bones=new ArrayList<>();
    public final List<Face> faces=new ArrayList<>();
    public final String texture;
    private final Map<String,Bone> named=new HashMap<>();
    private final Map<Integer,Map<String,double[]>> sealPoses=new HashMap<>();
    public MadaraModel(Reader reader) {
        JsonObject json=new JsonParser().parse(reader).getAsJsonObject();texture=json.get("texture").getAsString();
        if(json.has("sealPoses"))for(Map.Entry<String,JsonElement> entry:json.getAsJsonObject("sealPoses").entrySet()){
            Map<String,double[]> pose=new HashMap<>();
            for(Map.Entry<String,JsonElement> b:entry.getValue().getAsJsonObject().entrySet())pose.put(b.getKey(),vec(b.getValue().getAsJsonArray()));
            sealPoses.put(Integer.parseInt(entry.getKey()),pose);
        }
        for(JsonElement element:json.getAsJsonArray("groups")){Bone bone=new Bone(element.getAsJsonObject());if(named.put(bone.name,bone)!=null)throw new IllegalArgumentException("Duplicate bone "+bone.name);bones.add(bone);}
        for(Bone bone:bones){
            Set<String> ancestry=new HashSet<>();
            for(Bone at=bone;at!=null;at=named.get(at.parent)){
                if(!ancestry.add(at.name))throw new IllegalArgumentException("Bone cycle: "+at.name);
                if(at.parent!=null&&!named.containsKey(at.parent))throw new IllegalArgumentException("Missing parent "+at.parent);
            }
        }
        for(JsonElement element:json.getAsJsonArray("cubes")) {
            JsonObject box=element.getAsJsonObject();String owner=box.get("parent").getAsString(),name=box.get("name").getAsString();
            if(!named.containsKey(owner))throw new IllegalArgumentException("Missing cube parent "+owner);
            if(box.has("quads")){
                // Actual ModelBox vertex order and normalized UVs, including
                // mirror flags and legacy planes. No reconstructed UV net.
                for(JsonElement q:box.getAsJsonArray("quads")){
                    JsonObject quad=q.getAsJsonObject();double[] xyz=vec(quad.getAsJsonArray("xyz")),uv=vec(quad.getAsJsonArray("uv"));
                    if(xyz.length!=12||uv.length!=8)throw new IllegalArgumentException("Invalid quad "+name);
                    for(double v:xyz)if(!Double.isFinite(v))throw new IllegalArgumentException("Invalid vertex "+name);
                    for(double v:uv)if(!Double.isFinite(v)||v<-.000001||v>1.000001)throw new IllegalArgumentException("Invalid UV "+name);
                    faces.add(new Face(owner,name,xyz,uv,1));
                }
                continue;
            }
            double[] a=vec(box.getAsJsonArray("from")),b=vec(box.getAsJsonArray("to"));
            double[] origin=vec(box.getAsJsonArray("origin")),rotation=vec(box.getAsJsonArray("rotation"));
            double[] local=around(origin,rotation,0);
            for(Map.Entry<String,JsonElement> entry:box.getAsJsonObject("faces").entrySet()) {
                double[] points;float light;
                double x=a[0],y=a[1],z=a[2],xx=b[0],yy=b[1],zz=b[2];
                switch(entry.getKey()) {
                    case "north":points=new double[]{xx,yy,z,xx,y,z,x,y,z,x,yy,z};light=.94f;break;
                    case "south":points=new double[]{x,yy,zz,x,y,zz,xx,y,zz,xx,yy,zz};light=.86f;break;
                    case "east":points=new double[]{xx,yy,zz,xx,y,zz,xx,y,z,xx,yy,z};light=.77f;break;
                    case "west":points=new double[]{x,yy,z,x,y,z,x,y,zz,x,yy,zz};light=.82f;break;
                    case "up":points=new double[]{x,yy,z,x,yy,zz,xx,yy,zz,xx,yy,z};light=1;break;
                    default:points=new double[]{x,y,zz,x,y,z,xx,y,z,xx,y,zz};light=.69f;
                }
                double[] uv=vec(entry.getValue().getAsJsonObject().getAsJsonArray("uv"));
                double[] expanded={uv[0]/512,uv[1]/512,uv[0]/512,uv[3]/512,uv[2]/512,uv[3]/512,uv[2]/512,uv[1]/512};
                transform(points,local);faces.add(new Face(owner,name,points,expanded,light));
            }
        }
    }
    public boolean hasBone(String name){return named.containsKey(name);}
    public Map<String,double[]> matrices(Pose pose) {
        Map<String,double[]> result=new HashMap<>();
        for(Bone bone:bones)matrix(bone,pose,result,new HashSet<>());
        return result;
    }
    private double[] matrix(Bone bone,Pose pose,Map<String,double[]> output,Set<String> active) {
        if(output.containsKey(bone.name))return output.get(bone.name);
        if(!active.add(bone.name))throw new IllegalArgumentException("Bone cycle: "+bone.name);
        double[] r=bone.rotation.clone();if(!pose.bind){
            applyPose(bone.name,r,pose);
            applyBakedSeal(bone,r,pose);
            applyRecovery(bone,r,pose);
        }
        double dy=!pose.bind&&bone.name.equals("torso")?Math.sin(pose.age*.07)*.085:0;
        double[] m=around(bone.pivot,r,dy);
        if(bone.parent!=null)m=multiply(matrix(named.get(bone.parent),pose,output,active),m);
        active.remove(bone.name);output.put(bone.name,m);return m;
    }
    private void applyRecovery(Bone bone,double[] rotation,Pose pose) {
        if(pose.phase!=SusanooCastProfile.CANCEL&&pose.phase!=SusanooCastProfile.RELEASE)return;
        if(!(bone.name.startsWith("front_")||bone.name.startsWith("rear_")))return;
        Pose previous=new Pose();previous.stage=pose.stage;previous.profile=pose.profile;previous.armMask=pose.armMask;
        previous.age=pose.age;previous.walk=pose.walk;previous.stride=pose.stride;previous.attack=pose.attack;
        previous.headYaw=pose.headYaw;previous.headPitch=pose.headPitch;
        previous.phase=pose.recoveryPhase;previous.castAge=pose.recoveryAge;
        double[] from=bone.rotation.clone();applyPose(bone.name,from,previous);applyBakedSeal(bone,from,previous);
        float mix=SusanooCastProfile.smooth(SusanooCastProfile.clamp01(pose.castAge/
            (pose.phase==SusanooCastProfile.CANCEL?SusanooCastProfile.CANCEL_TICKS:4f)));
        if(pose.phase==SusanooCastProfile.CANCEL){
            previous.phase=SusanooCastProfile.IDLE;
            System.arraycopy(bone.rotation,0,rotation,0,3);applyPose(bone.name,rotation,previous);
        }
        for(int i=0;i<3;i++)rotation[i]=lerp(from[i],rotation[i],mix);
    }
    public static void applyPose(String name,double[] rotation,Pose pose) {
        if(name.equals("front_head")||name.equals("rear_head")){rotation[1]+=pose.headYaw*.42;rotation[0]+=pose.headPitch*.35;return;}
        if(name.startsWith("leg_")){rotation[0]+=Math.cos(pose.walk*.6+(name.endsWith("left")?Math.PI:0))*pose.stride*22;return;}
        boolean rear=name.startsWith("rear_");
        if(!(rear||name.startsWith("front_")))return;
        int side=name.contains("_left")?1:-1;
        boolean enabled=(pose.armMask&(rear?2:1))!=0;
        float weight=enabled?SusanooCastProfile.blendWeight(pose.phase,pose.castAge):0;
        int seal=rear?SusanooCastProfile.secondarySeal(pose.profile,pose.phase,pose.castAge):SusanooCastProfile.primarySeal(pose.profile,pose.phase,pose.castAge);
        int heldSeal=rear?SusanooCastProfile.secondarySeal(pose.profile,SusanooCastProfile.HOLD,0):SusanooCastProfile.primarySeal(pose.profile,SusanooCastProfile.HOLD,0);
        if(pose.phase==SusanooCastProfile.CANCEL)seal=heldSeal;
        // Each six-tick seal change is eased across two ticks, retaining the previous
        // pose at the boundary instead of snapping fingers between unrelated signs.
        int oldSeal=pose.phase==SusanooCastProfile.PREPARE?(rear?SusanooCastProfile.secondarySeal(pose.profile,pose.phase,Math.max(0,pose.castAge-6)):SusanooCastProfile.primarySeal(pose.profile,pose.phase,Math.max(0,pose.castAge-6))):seal;
        float sealMix=pose.phase==SusanooCastProfile.PREPARE?SusanooCastProfile.smooth(SusanooCastProfile.clamp01((pose.castAge%6)/2.5f)):1;
        if(pose.phase==SusanooCastProfile.RELEASE){oldSeal=heldSeal;sealMix=SusanooCastProfile.smooth(SusanooCastProfile.clamp01(pose.castAge/4));}
        // The rear pair already sits under a 180-degree facing root. Its local
        // rotations must match the front pair to bring both pairs to their chest.
        double direction=1;
        if(name.endsWith("_upper")){
            rotation[0]+=direction*(Math.sin(pose.age*.055+(rear?1:0))*2+weight*(seal==6?90:70));
            rotation[2]+=side*weight*(seal==6?10:-28);
            if(weight<.1)rotation[0]+=Math.cos(pose.walk*.6+(side>0?0:Math.PI))*pose.stride*12;
            rotation[0]+=direction*Math.sin(Math.sqrt(pose.attack)*Math.PI)*-65*(1-weight);
        } else if(name.endsWith("_forearm")){
            rotation[0]+=direction*weight*(seal==6?12:65);
            rotation[2]+=side*weight*(seal==6?0:30);
        } else if(name.endsWith("_hand")){
            rotation[0]+=direction*weight*(seal==6?-18:15);
            rotation[1]+=side*weight*(seal==SusanooCastProfile.SNAKE?35:20);
            rotation[2]+=side*weight*(seal==SusanooCastProfile.RAM?18:0);
        } else if(name.matches(".*_finger[0-3](_tip)?")) {
            int index=name.charAt(name.indexOf("_finger")+7)-'0';
            double curl=lerp(fingerCurl(oldSeal,index),fingerCurl(seal,index),sealMix);
            rotation[0]+=direction*weight*curl*(name.endsWith("_tip")?.9:1);
            if(seal==SusanooCastProfile.BIRD)rotation[2]+=weight*(index-1.5)*10;
        } else if(name.endsWith("_thumb"))rotation[2]+=side*weight*48;
        else if(name.contains("_hair_"))rotation[0]+=Math.sin(pose.age*.055)*4;
        else if(name.endsWith("_weapon"))rotation[0]+=weight*80;
    }
    private void applyBakedSeal(Bone bone,double[] rotation,Pose p){
        String name=bone.name;boolean rear=name.startsWith("rear_");
        if(!(name.endsWith("_upper")||name.endsWith("_forearm")||name.endsWith("_hand"))||sealPoses.isEmpty())return;
        if((p.armMask&(rear?2:1))==0)return;
        float weight=SusanooCastProfile.blendWeight(p.phase,p.castAge);if(weight<=0)return;
        int seal=rear?SusanooCastProfile.secondarySeal(p.profile,p.phase,p.castAge):SusanooCastProfile.primarySeal(p.profile,p.phase,p.castAge);
        int held=rear?SusanooCastProfile.secondarySeal(p.profile,SusanooCastProfile.HOLD,0):SusanooCastProfile.primarySeal(p.profile,SusanooCastProfile.HOLD,0);
        if(p.phase==SusanooCastProfile.CANCEL)seal=held;
        if(seal==0)seal=1;
        int old=p.phase==SusanooCastProfile.PREPARE?(rear?SusanooCastProfile.secondarySeal(p.profile,p.phase,Math.max(0,p.castAge-6)):SusanooCastProfile.primarySeal(p.profile,p.phase,Math.max(0,p.castAge-6))):seal;
        float mix=p.phase==SusanooCastProfile.PREPARE?SusanooCastProfile.smooth(SusanooCastProfile.clamp01((p.castAge%6)/2.5f)):1;
        if(p.phase==SusanooCastProfile.RELEASE){old=held;mix=SusanooCastProfile.smooth(SusanooCastProfile.clamp01(p.castAge/4));}
        double[] target=sealPoses.get(seal).get(name),previous=sealPoses.get(Math.max(1,old)).get(name);
        if(target==null)return;
        double[] idle=bone.rotation.clone();Pose neutral=new Pose();neutral.age=p.age;neutral.walk=p.walk;neutral.stride=p.stride;neutral.attack=p.attack;applyPose(name,idle,neutral);
        for(int i=0;i<3;i++)rotation[i]=lerp(idle[i],bone.rotation[i]+lerp(previous[i],target[i],mix),weight);
    }
    private static double fingerCurl(int seal,int index) {
        switch(seal){case SusanooCastProfile.TIGER:return index<2?0:88;case SusanooCastProfile.RAM:return index==0?8:73;case SusanooCastProfile.SNAKE:return 68;case SusanooCastProfile.BIRD:return index==0||index==3?7:81;case SusanooCastProfile.CLASP:return 84;default:return 6;}
    }
    public boolean visible(Face face,Pose p) {
        if(p.stage==0)return face.cube.startsWith("legacy_bipedBody");
        if(p.stage<3 && (face.bone.startsWith("leg_")||ancestor(face.bone,"leg_")))return false;
        boolean weapon=face.cube.contains("_sword_")||face.cube.contains("_blade_");
        if(weapon && (!p.swords||SusanooCastProfile.blendWeight(p.phase,p.castAge)>.08||p.stage==1||p.stage==5&&face.cube.startsWith("rear_")))return false;
        return true;
    }
    /** Rest bounds use the visible stage, excluding temporary weapon geometry. */
    public double[] bounds(int stage){
        Pose p=new Pose();p.stage=stage;p.bind=true;
        Map<String,double[]> m=matrices(p);double[] result={Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY};
        for(Face face:faces)if(visible(face,p)){
            double[] xyz=face.xyz.clone();transform(xyz,m.get(face.bone));
            for(int i=0;i<xyz.length;i++){int axis=i%3;result[axis]=Math.min(result[axis],xyz[i]);result[axis+3]=Math.max(result[axis+3],xyz[i]);}
        }
        return result;
    }
    private boolean ancestor(String name,String prefix) {Bone b=named.get(name);while(b!=null){if(b.name.startsWith(prefix))return true;b=named.get(b.parent);}return false;}
    public static double[] point(double[] m,double x,double y,double z){return new double[]{m[0]*x+m[1]*y+m[2]*z+m[3],m[4]*x+m[5]*y+m[6]*z+m[7],m[8]*x+m[9]*y+m[10]*z+m[11]};}
    public static void transform(double[] xyz,double[] m){for(int i=0;i<xyz.length;i+=3){double[] p=point(m,xyz[i],xyz[i+1],xyz[i+2]);System.arraycopy(p,0,xyz,i,3);}}
    private static double lerp(double a,double b,double f){return a+(b-a)*f;}
    private static double[] vec(JsonArray a){double[] r=new double[a.size()];for(int i=0;i<r.length;i++)r[i]=a.get(i).getAsDouble();return r;}
    private static double[] around(double[] p,double[] degrees,double dy){
        double x=Math.toRadians(degrees[0]),y=Math.toRadians(degrees[1]),z=Math.toRadians(degrees[2]);
        double cx=Math.cos(x),sx=Math.sin(x),cy=Math.cos(y),sy=Math.sin(y),cz=Math.cos(z),sz=Math.sin(z);
        double[] m={cz*cy,cz*sy*sx-sz*cx,cz*sy*cx+sz*sx,0,sz*cy,sz*sy*sx+cz*cx,sz*sy*cx-cz*sx,0,-sy,cy*sx,cy*cx,0,0,0,0,1};
        m[3]=p[0]-(m[0]*p[0]+m[1]*p[1]+m[2]*p[2]);m[7]=p[1]+dy-(m[4]*p[0]+m[5]*p[1]+m[6]*p[2]);m[11]=p[2]-(m[8]*p[0]+m[9]*p[1]+m[10]*p[2]);return m;
    }
    private static double[] multiply(double[] a,double[] b){double[] r=new double[16];for(int i=0;i<4;i++)for(int j=0;j<4;j++)for(int k=0;k<4;k++)r[i*4+j]+=a[i*4+k]*b[k*4+j];return r;}
}
