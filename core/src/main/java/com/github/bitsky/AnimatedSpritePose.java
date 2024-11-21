package com.github.bitsky;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
}
