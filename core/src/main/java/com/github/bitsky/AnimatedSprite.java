package com.github.bitsky;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import java.util.HashMap;
import java.util.UUID;

public class AnimatedSprite {
    public final HashMap<UUID, AnimatedSpriteBone> bones;
    public final AnimatedSpriteBone rootBone;
    public AnimatedSprite() {
        this.bones = new HashMap<>();
        this.rootBone = new AnimatedSpriteBone(this);
        this.bones.put(this.rootBone.id, this.rootBone);
    }
    public AnimatedSpriteBone addChildNodeTo(AnimatedSpriteBone parent){
        AnimatedSpriteBone spriteBone = new AnimatedSpriteBone(this);
        parent.children.add(spriteBone.id);
        this.bones.put(spriteBone.id, spriteBone);
        return spriteBone;
    }
    public void drawDebugBones(ShapeRenderer shapeRenderer){
        rootBone.drawDebugBones(new Transform(), shapeRenderer);
    }
}
