package com.github.bitsky;

import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class AnimationStateMachine {
    public final HashMap<UUID,State> states;
    public UUID startState;
    public final HashMap<UUID,InputProperty> properties;
    public AnimationStateMachine() {
        this.states = new HashMap<>();
        this.startState = addState(new Vector2()).id;
        this.properties = new HashMap<>();
    }

    public State addState(Vector2 position){
        State state = new State("State", position);
        this.states.put(state.id, state);
        return state;
    }
    public void addProperty(){
        InputProperty property = new InputProperty();
        this.properties.put(property.id, property);
    }
    public class InputProperty{
        public UUID id;
        public String name;
        public float value;
        public Float resetValue;
        public InputProperty() {
            this.id = UUID.randomUUID();
            this.name = "property";
            this.value = 0;
            this.resetValue = null;
        }
    }
    public class State{
        public final UUID id;
        public String name;
        public final ArrayList<StateTransition> transitions;
        public final Vector2 position;
        public boolean endState;
        public State(String name, Vector2 position) {
            this.position = position;
            this.id = UUID.randomUUID();
            this.name = name;
            this.transitions = new ArrayList<>();
            this.endState = false;
        }

        public void addTransition(State other) {
            this.transitions.add(new StateTransition(other.id));
        }
    }
    public class StateTransition{
        public final UUID target;
        public final float blendTime;
        public final EInterpolationFunction interpolationFunction;
        public StateTransition(UUID target) {
            this.target = target;
            this.blendTime = 0.2f;
            this.interpolationFunction = EInterpolationFunction.Linear;
        }
    }
}
