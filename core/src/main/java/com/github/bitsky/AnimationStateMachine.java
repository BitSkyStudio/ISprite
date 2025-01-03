package com.github.bitsky;

import com.badlogic.gdx.math.Vector2;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AnimationStateMachine {
    public final HashMap<UUID,State> states;
    public UUID startState;
    public AnimationStateMachine() {
        this.states = new HashMap<>();
        this.startState = addState(new Vector2()).id;
    }
    public JSONObject save(){
        JSONObject json = new JSONObject();
        JSONObject statesJson = new JSONObject();
        for(Map.Entry<UUID, State> entry : states.entrySet()){
            statesJson.put(entry.getKey().toString(), entry.getValue().save());
        }
        json.put("states", statesJson);
        json.put("startState", startState.toString());
        return json;
    }
    public void load(JSONObject json){
        this.states.clear();
        JSONObject statesJson = json.getJSONObject("states");
        for(String id : statesJson.keySet()){
            State state = new State("", null);
            state.load(statesJson.getJSONObject(id));
            this.states.put(state.id, state);
        }
        this.startState = UUID.fromString(json.getString("startState"));
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
        public UUID id;
        public String name;
        public final ArrayList<StateTransition> transitions;
        public Vector2 position;
        public boolean endState;
        public State(String name, Vector2 position) {
            this.position = position;
            this.id = UUID.randomUUID();
            this.name = name;
            this.transitions = new ArrayList<>();
            this.endState = false;
        }
        public JSONObject save(){
            JSONObject json = new JSONObject();
            json.put("id", id.toString());
            json.put("name", name);
            JSONArray transitionsJson = new JSONArray();
            for(StateTransition stateTransition : transitions){
                transitionsJson.put(stateTransition.save());
            }
            json.put("transitions", transitionsJson);
            JSONObject positionJson = new JSONObject();
            positionJson.put("x", position.x);
            positionJson.put("y", position.y);
            json.put("position", positionJson);
            json.put("end", endState);
            return json;
        }
        public void load(JSONObject json){
            this.id = UUID.fromString(json.getString("id"));
            this.name = json.getString("name");
            this.transitions.clear();
            for(Object transitionJson : json.getJSONArray("transitions")){
                StateTransition stateTransition = new StateTransition(null);
                stateTransition.load((JSONObject) transitionJson);
                transitions.add(stateTransition);
            }
            JSONObject positionJson = json.getJSONObject("position");
            this.position = new Vector2(positionJson.getFloat("x"), positionJson.getFloat("y"));
            this.endState = json.getBoolean("end");
        }

        public void addTransition(State other) {
            this.transitions.add(new StateTransition(other.id));
        }
    }
    public class StateTransition{
        public UUID target;
        public float blendTime;
        public EInterpolationFunction interpolationFunction;
        public final ArrayList<TransitionCondition> conditions;
        public boolean requireFinished;
        public StateTransition(UUID target) {
            this.target = target;
            this.blendTime = 0.2f;
            this.interpolationFunction = EInterpolationFunction.Linear;
            this.conditions = new ArrayList<>();
            this.requireFinished = false;
        }
        public JSONObject save(){
            JSONObject json = new JSONObject();
            json.put("target", target.toString());
            json.put("blendTime", blendTime);
            json.put("interpolation", interpolationFunction.id);
            JSONArray conditionsJson = new JSONArray();
            for(TransitionCondition condition : conditions){
                JSONObject conditionJson = new JSONObject();
                conditionJson.put("property", condition.propertyId.toString());
                conditionJson.put("value", condition.value);
                conditionJson.put("cmp", condition.comparator.display);
                conditionsJson.put(conditionJson);
            }
            json.put("conditions", conditionsJson);
            json.put("requireFinished", requireFinished);
            return json;
        }
        public void load(JSONObject json){
            this.target = UUID.fromString(json.getString("target"));
            this.blendTime = json.getFloat("blendTime");
            this.interpolationFunction = EInterpolationFunction.byId((byte) json.getInt("interpolation"));
            this.conditions.clear();
            JSONArray conditionsJson = json.getJSONArray("conditions");
            for(Object obj : conditionsJson){
                JSONObject condition = (JSONObject) obj;
                this.conditions.add(new TransitionCondition(UUID.fromString(condition.getString("property")), condition.getFloat("value"), EComparator.byName(condition.getString("cmp"))));
            }
            this.requireFinished = json.getBoolean("requireFinished");
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
        public static EComparator byName(String name){
            for(EComparator cmp : values()){
                if(cmp.display.equals(name))
                    return cmp;
            }
            return null;
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
