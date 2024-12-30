package com.github.bitsky;

import com.badlogic.gdx.math.Interpolation;

import java.util.function.Function;

public enum EInterpolationFunction {
    Linear(0, "Linear", (f) -> f),
    CircleIn(1, "CircleIn", Interpolation.circleIn::apply),
    CircleOut(2, "CircleOut", Interpolation.circleOut::apply),
    Circle(3, "Circle", Interpolation.circle::apply);
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
