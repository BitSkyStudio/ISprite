package com.github.bitsky;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

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
    public Transform getTransformAtTime(float time){
        return new Transform(translations.getValueAtTime(time), rotations.getValueAtTime(time), scales.getValueAtTime(time));
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
