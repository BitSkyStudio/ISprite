package com.github.bitsky;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import org.json.JSONObject;

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
        Float lerpedRotation = (this.rotation!=null&&other.rotation!=null)?Float.valueOf(MathUtils.lerpAngle(this.rotation, other.rotation, v)):(this.rotation!=null?this.rotation:other.rotation);
        Float lerpedScale = (this.scale!=null&&other.scale!=null)?Float.valueOf(MathUtils.lerp(this.scale, other.scale, v)):(this.scale!=null?this.scale:other.scale);
        return new Transform(lerpedTranslation, lerpedRotation, lerpedScale);
    }
    public Transform multiply(float s) {
        return new Transform(translation==null?null:translation.cpy().scl(s), rotation==null?null:rotation*s, scale==null?null:scale*s);
    }
    public Transform patch(Transform other){
        return new Transform(this.translation==null?other.translation:this.translation, this.rotation==null?other.rotation:this.rotation, this.scale==null?other.scale:this.scale);
    }

    public Transform add(Transform other){
        Vector2 addedTranslation = (this.translation!=null&&other.translation!=null)?this.translation.cpy().add(other.translation):(this.translation!=null?this.translation:other.translation);
        Float addedRotation = (this.rotation!=null&&other.rotation!=null)?Float.valueOf(this.rotation+other.rotation):(this.rotation!=null?this.rotation:other.rotation);
        Float addedScale = (this.scale!=null&&other.scale!=null)?Float.valueOf(this.scale+other.scale):(this.scale!=null?this.scale:other.scale);
        return new Transform(addedTranslation, addedRotation, addedScale);
    }
    public JSONObject save(){
        JSONObject json = new JSONObject();
        if(translation != null){
            JSONObject translation = new JSONObject();
            translation.put("x", this.translation.x);
            translation.put("y", this.translation.y);
            json.put("translation", translation);
        }
        if(rotation != null)
            json.put("rotation", rotation);
        if(scale != null)
            json.put("scale", scale);
        return json;
    }

    public void load(JSONObject json){
        this.translation = null;
        this.rotation = null;
        this.scale = null;
        if(json.has("translation")){
            JSONObject translation = json.getJSONObject("translation");
            this.translation = new Vector2(translation.getFloat("x"), translation.getFloat("y"));
        }
        if(json.has("rotation"))
            this.rotation = json.getFloat("rotation");
        if(json.has("scale"))
            this.scale = json.getFloat("scale");
    }
}
