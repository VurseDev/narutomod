package net.narutomod;

import com.google.gson.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.model.*;
import net.narutomod.entity.*;

/** Exports actual ModelBox vertices and UVs before any Madara edits. No GL calls.
 * This is the authoritative reference for one-to-one geometry validation. */
public final class LegacySusanooExporter {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    public static void main(String[] args) throws Exception {
        export("Skeleton",new EntitySusanooSkeleton.Renderer().new ModelSusanooSkeleton(),"susanooskeleton.png");
        export("Clothed",new EntitySusanooClothed.Renderer().new ModelSusanooClothed(),"susanoo_clothed.png");
        export("Winged",new EntitySusanooWinged.Renderer().new ModelSusanooWinged(),"susanoo_winged.png");
    }
    private static void export(String kind,ModelBase model,String texture) throws Exception {
        IdentityHashMap<ModelRenderer,String> names=new IdentityHashMap<>();
        for(Class<?> type=model.getClass();type!=null;type=type.getSuperclass())for(Field f:type.getDeclaredFields()) {
            if(f.getType()!=ModelRenderer.class)continue;
            f.setAccessible(true);ModelRenderer bone=(ModelRenderer)f.get(model);
            if(bone!=null)names.put(bone,f.getName());
        }
        IdentityHashMap<ModelRenderer,ModelRenderer> parents=new IdentityHashMap<>();
        for(ModelRenderer bone:names.keySet())if(bone.childModels!=null)for(ModelRenderer child:bone.childModels)parents.put(child,bone);
        Field quads=ModelBox.class.getDeclaredField("quadList");quads.setAccessible(true);
        List<Map<String,Object>> bones=new ArrayList<>();
        List<ModelRenderer> ordered=new ArrayList<>(names.keySet());ordered.sort(Comparator.comparing(names::get));
        for(ModelRenderer bone:ordered){
            Map<String,Object> row=new LinkedHashMap<>();row.put("name",names.get(bone));row.put("parent",names.get(parents.get(bone)));
            row.put("pivot",new float[]{bone.rotationPointX,bone.rotationPointY,bone.rotationPointZ});
            row.put("rotation",new float[]{bone.rotateAngleX,bone.rotateAngleY,bone.rotateAngleZ});
            List<Map<String,Object>> boxes=new ArrayList<>();int index=0;
            for(ModelBox box:bone.cubeList){
                Map<String,Object> cube=new LinkedHashMap<>();cube.put("index",index++);
                List<Map<String,Object>> faces=new ArrayList<>();int faceIndex=0;
                for(TexturedQuad quad:(TexturedQuad[])quads.get(box)){
                    Map<String,Object> face=new LinkedHashMap<>();float[] xyz=new float[12],uv=new float[8];
                    for(int i=0;i<4;i++){PositionTextureVertex v=quad.vertexPositions[i];xyz[i*3]=(float)v.vector3D.x;xyz[i*3+1]=(float)v.vector3D.y;xyz[i*3+2]=(float)v.vector3D.z;uv[i*2]=v.texturePositionX;uv[i*2+1]=v.texturePositionY;}
                    face.put("index",faceIndex++);face.put("xyz",xyz);face.put("uv",uv);faces.add(face);
                }cube.put("quads",faces);boxes.add(cube);
            }row.put("cubes",boxes);bones.add(row);
        }
        Map<String,Object> result=new LinkedHashMap<>();result.put("kind",kind);result.put("texture",texture);result.put("textureWidth",model.textureWidth);result.put("textureHeight",model.textureHeight);result.put("bones",bones);
        Path target=Paths.get("models/madara/reference/"+kind.toLowerCase(Locale.ROOT)+".json");Files.createDirectories(target.getParent());Files.write(target,JSON.toJson(result).getBytes(StandardCharsets.UTF_8));
        System.out.println("Exported actual "+kind+" ModelBox geometry: "+bones.size()+" bones -> "+target);
    }
}
