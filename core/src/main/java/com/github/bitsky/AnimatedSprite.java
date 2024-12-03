package com.github.bitsky;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class AnimatedSprite {
    public final HashMap<UUID, AnimatedSpriteBone> bones;
    public final AnimatedSpriteBone rootBone;
    public AnimatedSprite() {
        this.bones = new HashMap<>();
        this.rootBone = new AnimatedSpriteBone(this, null);
        this.rootBone.name = "root";
        this.bones.put(this.rootBone.id, this.rootBone);
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
