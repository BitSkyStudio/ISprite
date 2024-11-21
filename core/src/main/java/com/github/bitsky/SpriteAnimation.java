package com.github.bitsky;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpriteAnimation {
    public HashMap<UUID, AnimationTrack> boneTracks;
    public SpriteAnimation(HashMap<UUID, AnimationTrack> boneTracks) {
        this.boneTracks = boneTracks;
    }
    public AnimatedSpritePose getPose(float time){
        HashMap<UUID,Transform> transforms = new HashMap<>();
        for(Map.Entry<UUID, AnimationTrack> entry : boneTracks.entrySet()){
            transforms.put(entry.getKey(), entry.getValue().getTransformAtTime(time));
        }
        return new AnimatedSpritePose(transforms);
    }
}
