package com.github.bitsky;

import java.util.function.Function;

public enum EInterpolationFunction {
    Linear(0, "Linear", (f) -> f);
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
