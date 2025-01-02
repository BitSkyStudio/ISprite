package com.github.bitsky;

import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class AnimationStateMachine {
    public final HashMap<UUID,State> states;
    public UUID startState;
    public AnimationStateMachine() {
        this.states = new HashMap<>();
        this.startState = addState(new Vector2()).id;
    }
    public State addState(Vector2 position){
        State state = new State("State", position);
        this.states.put(state.id, state);
        return state;
    }

    public void removeState(UUID state) {

        for (State checkState : states.values()) {
            ArrayList<StateTransition> removeTransitions = new ArrayList<>();
            for (StateTransition stateTransition : checkState.transitions) {
                if (stateTransition.target.equals(state)) {
                    removeTransitions.add(stateTransition);
                }
            }

            checkState.transitions.removeAll(removeTransitions);
        }

        this.states.remove(state);
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
        public final ArrayList<TransitionCondition> conditions;
        public boolean requireFinished;
        public StateTransition(UUID target) {
            this.target = target;
            this.blendTime = 0.2f;
            this.interpolationFunction = EInterpolationFunction.Linear;
            this.conditions = new ArrayList<>();
            this.requireFinished = false;
        }
    }
    public static class TransitionCondition{
        public final UUID propertyId;
        public final float value;
        public final EComparator comparator;
        public TransitionCondition(UUID propertyId, float value, EComparator comparator) {
            this.propertyId = propertyId;
            this.value = value;
            this.comparator = comparator;
        }
    }
    public enum EComparator{
        Equal("=="),
        NotEqual("!="),
        Less("<"),
        More(">"),
        LessEqual("<="),
        MoreEqual(">=");
        public final String display;
        EComparator(String display) {
            this.display = display;
        }
        public boolean passes(float a, float b){
            switch(this){
                case Equal:
                    return a == b;
                case NotEqual:
                    return a != b;
                case Less:
                    return a < b;
                case More:
                    return a > b;
                case LessEqual:
                    return a <= b;
                case MoreEqual:
                    return a >= b;
            }
            throw new IllegalStateException("unreachable");
        }
    }
}
