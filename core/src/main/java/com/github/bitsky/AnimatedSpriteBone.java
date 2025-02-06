package com.github.bitsky;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

public class AnimatedSpriteBone {
    public AnimatedSprite sprite;
    public UUID parent;
    public UUID id;
    public String name;
    public ArrayList<UUID> children;
    public Transform baseTransform;
    public Color color;
    public AnimatedSpriteBone(AnimatedSprite sprite, UUID parent) {
        this.sprite = sprite;
        this.parent = parent;
        this.id = UUID.randomUUID();
        this.children = new ArrayList<>();
        this.baseTransform = new Transform(new Vector2(), 0f, 1f);
        this.name = "bone";
        Random random = new Random();
        this.color = new Color(random.nextFloat(), random.nextFloat(), random.nextFloat(), 1f);
    }
    public void childrenRecursive(ArrayList<UUID> children){
        for(UUID child : this.children){
            children.add(child);
            ISpriteMain.getInstance().sprite.bones.get(child).childrenRecursive(children);
        }
    }
    public JSONObject save(){
        JSONObject json = new JSONObject();
        if(parent != null)
            json.put("parent", parent.toString());
        json.put("name", name);
        JSONArray children = new JSONArray();
        for(UUID child : this.children){
            children.put(child.toString());
        }
        json.put("children", children);
        json.put("baseTransform", baseTransform.save());
        json.put("color", color.toString());
        return json;
    }
    public void load(JSONObject json){
        this.parent = json.has("parent")?UUID.fromString(json.getString("parent")):null;
        this.name = json.getString("name");
        this.children.clear();
        for(Object child : json.getJSONArray("children")){
            this.children.add(UUID.fromString(((String)child)));
        }
        this.baseTransform.load(json.getJSONObject("baseTransform"));
        this.color = Color.valueOf(json.getString("color"));
    }
}
