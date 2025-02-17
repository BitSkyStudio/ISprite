package com.github.bitsky;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AnimatedSprite {
    public final HashMap<UUID, AnimatedSpriteBone> bones;
    public AnimatedSpriteBone rootBone;
    public ArrayList<VertexedImage> images;
    public AnimatedSprite() {
        this.bones = new HashMap<>();
        this.rootBone = new AnimatedSpriteBone(this, null);
        this.rootBone.name = "root";
        this.bones.put(this.rootBone.id, this.rootBone);
        this.images = new ArrayList<>();
    }
    public JSONObject save(){
        JSONObject json = new JSONObject();
        JSONObject bones = new JSONObject();
        for(Map.Entry<UUID, AnimatedSpriteBone> entry : this.bones.entrySet()){
            bones.put(entry.getKey().toString(), entry.getValue().save());
        }
        json.put("bones", bones);
        json.put("root", rootBone.id.toString());
        JSONArray imagesJson = new JSONArray();
        for(VertexedImage image : images){
            imagesJson.put(image.save());
        }
        json.put("images", imagesJson);
        return json;
    }
    public void load(JSONObject json){
        this.bones.clear();
        JSONObject bones = json.getJSONObject("bones");
        for(String id : bones.keySet()){
            AnimatedSpriteBone bone = new AnimatedSpriteBone(this, null);
            bone.load(bones.getJSONObject(id));
            bone.id = UUID.fromString(id);
            this.bones.put(bone.id, bone);
        }
        this.rootBone = this.bones.get(UUID.fromString(json.getString("root")));
        this.images.clear();
        for(Object im : json.getJSONArray("images")){
            VertexedImage image = new VertexedImage(null, null);
            image.load((JSONObject) im);
            this.images.add(image);
        }
    }
    public AnimatedSpriteBone addChildNodeTo(AnimatedSpriteBone parent){
        AnimatedSpriteBone spriteBone = new AnimatedSpriteBone(this, parent.id);
        parent.children.add(spriteBone.id);
        this.bones.put(spriteBone.id, spriteBone);
        return spriteBone;
    }
    public void removeNode(AnimatedSpriteBone node){
        if(node == rootBone)
            return;
        bones.get(node.parent).children.remove(node.id);
        ArrayList<AnimatedSpriteBone> queue = new ArrayList<>();
        queue.add(node);
        while(!queue.isEmpty()){
            AnimatedSpriteBone bone = queue.remove(0);
            bones.remove(bone.id);
            for(UUID child : bone.children){
                queue.add(bones.get(child));
            }
        }
    }
}
