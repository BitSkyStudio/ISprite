package com.github.bitsky;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import java.util.ArrayList;
import java.util.UUID;

public class AnimatedSpriteBone {
    public final AnimatedSprite sprite;
    public final UUID id;
    public final ArrayList<UUID> children;
    public final Transform baseTransform;
    public AnimatedSpriteBone(AnimatedSprite sprite) {
        this.sprite = sprite;
        this.id = UUID.randomUUID();
        this.children = new ArrayList<>();
        this.baseTransform = new Transform();
    }
    public void drawDebugBones(Transform transform, ShapeRenderer shapeRenderer){
        Transform ownTransform = transform.transformChild(baseTransform);
        shapeRenderer.line(transform.translation, ownTransform.translation);
        for(UUID childId : children){
            AnimatedSpriteBone child = sprite.bones.get(childId);
            child.drawDebugBones(ownTransform, shapeRenderer);
        }
    }
}
