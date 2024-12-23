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
            for(Map.Entry<Float, ValueInterpolationPair<Vector2>> entry : this.translations.track.entrySet()){
                JSONObject translation = new JSONObject();
                translation.put("x", entry.getValue().value.x);
                translation.put("y", entry.getValue().value.y);
                translation.put("interpolation", entry.getValue().interpolationFunction.id);
                translations.put(entry.getKey().toString(), translation);
            }
            tracks.put("translations", translations);
        }
        if(!rotations.track.isEmpty()){
            JSONObject rotations = new JSONObject();
            for(Map.Entry<Float, ValueInterpolationPair<Float>> entry : this.rotations.track.entrySet()){
                JSONObject rotation = new JSONObject();
                rotation.put("value", entry.getValue().value);
                rotation.put("interpolation", entry.getValue().interpolationFunction.id);
                rotations.put(entry.getKey().toString(), rotation);
            }
            tracks.put("rotations", rotations);
        }
        if(!scales.track.isEmpty()){
            JSONObject scales = new JSONObject();
            for(Map.Entry<Float, ValueInterpolationPair<Float>> entry : this.scales.track.entrySet()){
                JSONObject scale = new JSONObject();
                scale.put("value", entry.getValue().value);
                scale.put("interpolation", entry.getValue().interpolationFunction.id);
                scales.put(entry.getKey().toString(), scale);
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
                this.translations.addKeyframe(Float.parseFloat(time), new Vector2(translation.getFloat("x"), translation.getFloat("y")), EInterpolationFunction.byId((byte) translation.getInt("interpolation")));
            }
        }
        if(track.has("rotations")) {
            JSONObject rotations = track.getJSONObject("rotations");
            for(String time : rotations.keySet()){
                JSONObject rotation = rotations.getJSONObject(time);
                this.rotations.addKeyframe(Float.parseFloat(time), rotation.getFloat("value"), EInterpolationFunction.byId((byte) rotation.getInt("interpolation")));
            }
        }
        if(track.has("scales")) {
            JSONObject scales = track.getJSONObject("scales");
            for(String time : scales.keySet()){
                JSONObject scale = scales.getJSONObject(time);
                this.scales.addKeyframe(Float.parseFloat(time), scale.getFloat("value"), EInterpolationFunction.byId((byte) scale.getInt("interpolation")));
            }
        }
    }
    public static class ValueInterpolationPair<T>{
        public T value;
        public EInterpolationFunction interpolationFunction;
        public ValueInterpolationPair(T value, EInterpolationFunction interpolationFunction) {
            this.value = value;
            this.interpolationFunction = interpolationFunction;
        }
    }
    public static class PropertyTrack<T>{
        public final HashMap<Float, ValueInterpolationPair<T>> track;
        private EasingFunction<T> easingFunction;
        public PropertyTrack(EasingFunction<T> easingFunction) {
            this.track = new HashMap<>();
            this.easingFunction = easingFunction;
        }
        public void addKeyframe(float time, T value, EInterpolationFunction interpolationFunction){
            track.put(time, new ValueInterpolationPair<>(value, interpolationFunction));
        }
        public void modifyKeyframe(float time, float toTime){
            ValueInterpolationPair<T> v = this.track.get(time);
            track.remove(time);
            track.put(toTime, v);
        }

        public T getValueAtTime(float time){
            ArrayList<Map.Entry<Float, ValueInterpolationPair<T>>> list = new ArrayList<>(track.entrySet());
            list.sort(Map.Entry.comparingByKey());
            if(list.isEmpty())
                return null;
            if(time <= list.get(0).getKey())
                return list.get(0).getValue().value;
            if(time >= list.get(list.size()-1).getKey())
                return list.get(list.size()-1).getValue().value;

            float previousTime = list.get(0).getKey();
            T previousValue = list.get(0).getValue().value;
            for(int i = 1;i < list.size();i++){
                Map.Entry<Float, ValueInterpolationPair<T>> entry = list.get(i);
                if(entry.getKey() >= time){
                    float lerpValue = (time-previousTime)/(entry.getKey()-previousTime);
                    return easingFunction.getEased(previousValue, entry.getValue().value, entry.getValue().interpolationFunction.function.apply(lerpValue));
                }
                previousTime = entry.getKey();
                previousValue = entry.getValue().value;
            }
            throw new IllegalStateException("unreachable");
        }
        @FunctionalInterface
        public interface EasingFunction<T>{
            T getEased(T first, T second, float t);
        }
    }
}
