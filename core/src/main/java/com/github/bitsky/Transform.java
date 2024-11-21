package com.github.bitsky;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

public class Transform {
    public Vector2 translation;
    public Float rotation;
    public Float scale;
    public Transform(Vector2 translation, Float rotation, Float scale) {
        this.translation = translation;
        this.rotation = rotation;
        this.scale = scale;
    }
    public Transform(){
        this(null, null, null);
    }
    public Transform lock(){
        return new Transform(this.translation!=null?this.translation:new Vector2(), this.rotation!=null?this.rotation:0, this.scale!=null?this.scale:1);
    }
    public Transform transformChild(Transform child){
        return new Transform(this.translation.cpy().add(child.translation.cpy().rotateRad(this.rotation).scl(this.scale)), this.rotation + child.rotation, this.scale * child.scale);
    }
    public Transform lerp(Transform other, float v){
        Vector2 lerpedTranslation = (this.translation!=null&&other.translation!=null)?this.translation.cpy().lerp(other.translation, v):(this.translation!=null?this.translation:other.translation);
        float lerpedRotation = (this.rotation!=null&&other.rotation!=null)?MathUtils.lerpAngle(this.rotation, other.rotation, v):(this.rotation!=null?this.rotation:other.rotation);
        float lerpedScale = (this.scale!=null&&other.scale!=null)?MathUtils.lerp(this.scale, other.scale, v):(this.scale!=null?this.scale:other.scale);
        return new Transform(lerpedTranslation, lerpedRotation, lerpedScale);
    }
    public Transform add(Transform other){
        Vector2 addedTranslation = (this.translation!=null&&other.translation!=null)?this.translation.cpy().add(other.translation):(this.translation!=null?this.translation:other.translation);
        float addedRotation = (this.rotation!=null&&other.rotation!=null)?this.rotation+other.rotation:(this.rotation!=null?this.rotation:other.rotation);
        float addedScale = (this.scale!=null&&other.scale!=null)?this.scale+other.scale:(this.scale!=null?this.scale:other.scale);
        return new Transform(addedTranslation, addedRotation, addedScale);
    }
}
