package com.github.bitsky;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpriteAnimation {
    public HashMap<UUID, AnimationTrack> boneTracks;
    public SpriteAnimation() {
        this.boneTracks = new HashMap<>();
    }
    public AnimatedSpritePose getPose(float time){
        HashMap<UUID,Transform> transforms = new HashMap<>();
        for(Map.Entry<UUID, AnimationTrack> entry : boneTracks.entrySet()){
            transforms.put(entry.getKey(), entry.getValue().getTransformAtTime(time));
        }
        return new AnimatedSpritePose(transforms);
    }
    public AnimationTrack getTrack(UUID id){
        if(!boneTracks.containsKey(id))
            boneTracks.put(id, new AnimationTrack());
        return boneTracks.get(id);
    }
    public JSONObject save(){
        JSONObject tracks = new JSONObject();
        for(Map.Entry<UUID, AnimationTrack> entry : boneTracks.entrySet()){
            tracks.put(entry.getKey().toString(), entry.getValue().save());
        }
        return tracks;
    }
    public void load(JSONObject tracks){
        this.boneTracks.clear();
        for(String entry : tracks.keySet()){
            JSONObject track = tracks.getJSONObject(entry);
            AnimationTrack animationTrack = new AnimationTrack();
            animationTrack.load(track);
            boneTracks.put(UUID.fromString(entry), animationTrack);
        }
    }
    public float getAnimationLength(){
        float maxLength = 0;
        for(AnimationTrack track : boneTracks.values()){
            for(float time : track.translations.track.keySet()){
                maxLength = Math.max(maxLength, time);
            }
            for(float time : track.rotations.track.keySet()){
                maxLength = Math.max(maxLength, time);
            }
            for(float time : track.scales.track.keySet()){
                maxLength = Math.max(maxLength, time);
            }
        }
        return maxLength;
    }
}
