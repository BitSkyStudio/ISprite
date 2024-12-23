package com.github.bitsky;

import com.badlogic.gdx.math.Vector2;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.UUID;

public class AnimatedSpriteBone {
    public AnimatedSprite sprite;
    public UUID parent;
    public UUID id;
    public String name;
    public ArrayList<UUID> children;
    public Transform baseTransform;
    public AnimatedSpriteBone(AnimatedSprite sprite, UUID parent) {
        this.sprite = sprite;
        this.parent = parent;
        this.id = UUID.randomUUID();
        this.children = new ArrayList<>();
        this.baseTransform = new Transform(new Vector2(), 0f, 1f);
        this.name = "bone";
    }
    public JSONObject save(){
        JSONObject json = new JSONObject();
        json.put("parent", parent.toString());
        json.put("name", name);
        JSONArray children = new JSONArray();
        for(UUID child : this.children){
            children.put(child.toString());
        }
        json.put("children", children);
        json.put("baseTransform", baseTransform.save());
        return json;
    }
    public void load(JSONObject json){
        this.parent = UUID.fromString(json.getString("parent"));
        this.name = json.getString("name");
        this.children.clear();
        for(Object child : json.getJSONArray("children")){
            this.children.add(UUID.fromString(((String)child)));
        }
        this.baseTransform.load(json.getJSONObject("baseTransform"));
    }
}
