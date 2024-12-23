package com.github.bitsky;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import org.json.JSONObject;

import java.util.*;

public class AnimationTrack {
    public PropertyTrack<Vector2> translations;
    public PropertyTrack<Float> rotations;
    public PropertyTrack<Float> scales;
    public AnimationTrack() {
        this.translations = new PropertyTrack<>((first, second, t) -> first.cpy().lerp(second, t));
        this.rotations = new PropertyTrack<>(MathUtils::lerp);
        this.scales = new PropertyTrack<>(MathUtils::lerp);
    }
    public JSONObject save(){
        JSONObject tracks = new JSONObject();
        if(!translations.track.isEmpty()){
            JSONObject translations = new JSONObject();
            for(Map.Entry<Float, Vector2> entry : this.translations.track.entrySet()){
                JSONObject translation = new JSONObject();
                translation.put("x", entry.getValue().x);
                translation.put("y", entry.getValue().y);
                translations.put(entry.getKey().toString(), translation);
            }
            tracks.put("translations", translations);
        }
        if(!rotations.track.isEmpty()){
            JSONObject rotations = new JSONObject();
            for(Map.Entry<Float, Float> entry : this.rotations.track.entrySet()){
                rotations.put(entry.getKey().toString(), entry.getValue());
            }
            tracks.put("rotations", rotations);
        }
        if(!scales.track.isEmpty()){
            JSONObject scales = new JSONObject();
            for(Map.Entry<Float, Float> entry : this.scales.track.entrySet()){
                scales.put(entry.getKey().toString(), entry.getValue());
            }
            tracks.put("scales", scales);
        }
        return tracks;
    }
    public Transform getTransformAtTime(float time){
        return new Transform(translations.getValueAtTime(time), rotations.getValueAtTime(time), scales.getValueAtTime(time));
    }
    public void load(JSONObject track) {
        translations.track.clear();
        rotations.track.clear();
        scales.track.clear();
        if(track.has("translations")) {
            JSONObject translations = track.getJSONObject("translations");
            for(String time : translations.keySet()){
                JSONObject translation = translations.getJSONObject(time);
                this.translations.addKeyframe(Float.parseFloat(time), new Vector2(translation.getFloat("x"), translation.getFloat("y")));
            }
        }
        if(track.has("rotations")) {
            JSONObject rotations = track.getJSONObject("rotations");
            for(String time : rotations.keySet()){
                this.rotations.addKeyframe(Float.parseFloat(time), rotations.getFloat(time));
            }
        }
        if(track.has("scales")) {
            JSONObject scales = track.getJSONObject("scales");
            for(String time : scales.keySet()){
                this.scales.addKeyframe(Float.parseFloat(time), scales.getFloat(time));
            }
        }
    }
    public static class PropertyTrack<T>{
        public final HashMap<Float, T> track;
        private EasingFunction<T> easingFunction;
        public PropertyTrack(EasingFunction<T> easingFunction) {
            this.track = new HashMap<>();
            this.easingFunction = easingFunction;
        }
        public void addKeyframe(float time, T value){
            track.put(time, value);
        }
        public void modifyKeyframe(float time, float toTime){
            T v = this.track.get(time);
            track.remove(time);
            track.put(toTime, v);
        }

        public T getValueAtTime(float time){
            ArrayList<Map.Entry<Float, T>> list = new ArrayList<>(track.entrySet());
            list.sort(Map.Entry.comparingByKey());
            if(list.isEmpty())
                return null;
            if(time <= list.get(0).getKey())
                return list.get(0).getValue();
            if(time >= list.get(list.size()-1).getKey())
                return list.get(list.size()-1).getValue();

            float previousTime = list.get(0).getKey();
            T previousValue = list.get(0).getValue();
            for(int i = 1;i < list.size();i++){
                Map.Entry<Float, T> entry = list.get(i);
                if(entry.getKey() >= time){
                    float lerpValue = (time-previousTime)/(entry.getKey()-previousTime);
                    return easingFunction.getEased(previousValue, entry.getValue(), lerpValue);
                }
                previousTime = entry.getKey();
                previousValue = entry.getValue();
            }
            throw new IllegalStateException("unreachable");
        }
        @FunctionalInterface
        public interface EasingFunction<T>{
            T getEased(T first, T second, float t);
        }
    }
}
