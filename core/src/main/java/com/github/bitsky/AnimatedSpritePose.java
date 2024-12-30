package com.github.bitsky;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public class AnimatedSpritePose {
    public final HashMap<UUID,Transform> boneTransforms;
    public AnimatedSpritePose(HashMap<UUID, Transform> boneTransforms) {
        this.boneTransforms = boneTransforms;
    }
    public AnimatedSpritePose add(AnimatedSpritePose other){
        HashMap<UUID, Transform> newTransforms = new HashMap<>();
        for(Map.Entry<UUID, Transform> entry : boneTransforms.entrySet()){
            newTransforms.put(entry.getKey(), entry.getValue().add(other.boneTransforms.get(entry.getKey())));
        }
        return new AnimatedSpritePose(newTransforms);
    }
    public AnimatedSpritePose lerp(AnimatedSpritePose other, float v){
        HashMap<UUID, Transform> newTransforms = new HashMap<>();
        for(Map.Entry<UUID, Transform> entry : boneTransforms.entrySet()){
            newTransforms.put(entry.getKey(), entry.getValue().lerp(other.boneTransforms.get(entry.getKey()), v));
        }
        return new AnimatedSpritePose(newTransforms);
    }
    public AnimatedSpritePose multiply(float s){
        HashMap<UUID, Transform> newTransforms = new HashMap<>();
        for(Map.Entry<UUID, Transform> entry : boneTransforms.entrySet()){
            newTransforms.put(entry.getKey(), entry.getValue().multiply(s));
        }
        return new AnimatedSpritePose(newTransforms);
    }
    public void drawDebugBones(AnimatedSprite sprite, ShapeRenderer shapeRenderer, Function<UUID,Color> highlighter){
        drawDebugBone(sprite, sprite.rootBone, new Transform(new Vector2(), 0f, 1f), shapeRenderer, highlighter);
    }
    private void drawDebugBone(AnimatedSprite sprite, AnimatedSpriteBone bone, Transform transform, ShapeRenderer shapeRenderer, Function<UUID,Color> highlighter){
        Transform animTransform = this.boneTransforms.get(bone.id);
        Transform ownTransform = transform.transformChild(animTransform==null?bone.baseTransform:animTransform.patch(bone.baseTransform));
        //if(bone.id.equals(highlight))
        //    shapeRenderer.setColor(Color.GREEN);
        shapeRenderer.setColor(Color.YELLOW);
        shapeRenderer.line(transform.translation, ownTransform.translation);
        shapeRenderer.setColor(highlighter.apply(bone.id));
        drawArrow(shapeRenderer, ownTransform.translation, ownTransform.translation.cpy().add(new Vector2(0, ownTransform.scale*100).rotateRad(ownTransform.rotation)), ownTransform.scale*100/3);
        //shapeRenderer.setColor(Color.WHITE);
        for(UUID childId : bone.children){
            AnimatedSpriteBone child = sprite.bones.get(childId);
            drawDebugBone(sprite, child, ownTransform, shapeRenderer, highlighter);
        }
    }
    public static void drawArrow(ShapeRenderer shapeRenderer, Vector2 from, Vector2 to, float size){
        shapeRenderer.line(from, to);
        Vector2 diff = to.cpy().sub(from);
        float angle = diff.angleRad();
        shapeRenderer.line(to, to.cpy().add(new Vector2(size, 0).rotateRad((float) (angle+Math.PI+Math.PI/6))));
        shapeRenderer.line(to, to.cpy().add(new Vector2(size, 0).rotateRad((float) (angle+Math.PI-Math.PI/6))));
    }
    public HashMap<UUID,Transform> getBoneTransforms(AnimatedSprite sprite, Transform transform){
        HashMap<UUID,Transform> transforms = new HashMap<>();
        getBonePositionsRecurse(sprite, transforms, sprite.rootBone, transform);
        return transforms;
    }
    private void getBonePositionsRecurse(AnimatedSprite sprite, HashMap<UUID,Transform> transforms, AnimatedSpriteBone bone, Transform transform){
        Transform animTransform = this.boneTransforms.get(bone.id);
        Transform ownTransform = transform.transformChild(animTransform==null?bone.baseTransform:animTransform.patch(bone.baseTransform));
        transforms.put(bone.id, ownTransform);
        for(UUID childId : bone.children){
            AnimatedSpriteBone child = sprite.bones.get(childId);
            getBonePositionsRecurse(sprite, transforms, child, ownTransform);
        }
    }
}
