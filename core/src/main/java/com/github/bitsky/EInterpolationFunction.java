package com.github.bitsky;

import com.badlogic.gdx.math.Interpolation;

import java.util.function.Function;

public enum EInterpolationFunction {
    Linear(0, "Linear", (f) -> f),
    CircleIn(1, "CircleIn", Interpolation.circleIn::apply),
    CircleOut(2, "CircleOut", Interpolation.circleOut::apply),
    Circle(3, "Circle", Interpolation.circle::apply),
    Bounce(4, "Bounce", Interpolation.bounce::apply),
    BounceIn(5, "Bounce", Interpolation.bounceIn::apply),
    BounceOut(6, "Bounce", Interpolation.bounceOut::apply),
    Elastic(7, "Elastic", Interpolation.elastic::apply),
    ElasticIn(8, "ElasticIn", Interpolation.elasticIn::apply),
    ElasticOut(9, "ElasticOut", Interpolation.elasticOut::apply),
    Fade(10, "Fade", Interpolation.fade::apply),
    SwingIn(11, "SwingIn", Interpolation.swingIn::apply),
    SwingOut(12, "SwingOut", Interpolation.swingOut::apply),
    Swing(13, "Swing", Interpolation.swing::apply);

    public byte id;
    public String name;
    public Function<Float,Float> function;

    EInterpolationFunction(int id, String name, Function<Float, Float> function) {
        this.id = (byte) id;
        this.name = name;
        this.function = function;
    }

    public static EInterpolationFunction byId(byte id){
        for(EInterpolationFunction interpolationFunction : values()){
            if(interpolationFunction.id == id)
                return interpolationFunction;
        }
        return null;
    }
}
