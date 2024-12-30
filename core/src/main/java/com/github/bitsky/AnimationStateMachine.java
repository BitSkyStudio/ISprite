package com.github.bitsky;

import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class AnimationStateMachine {
    public final HashMap<UUID,State> states;
    public AnimationStateMachine() {
        this.states = new HashMap<>();
    }

    public void addState(Vector2 position){
        State state = new State("State", position);
        this.states.put(state.id, state);
    }
    public class State{
        public final UUID id;
        public String name;
        public final ArrayList<StateTransition> transitions;
        public final Vector2 position;
        public State(String name, Vector2 position) {
            this.position = position;
            this.id = UUID.randomUUID();
            this.name = name;
            this.transitions = new ArrayList<>();
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
