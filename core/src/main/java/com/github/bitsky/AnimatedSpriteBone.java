package com.github.bitsky;

import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.UUID;

public class AnimatedSpriteBone {
    public final AnimatedSprite sprite;
    public final UUID parent;
    public final UUID id;
    public final ArrayList<UUID> children;
    public final Transform baseTransform;
    public AnimatedSpriteBone(AnimatedSprite sprite, UUID parent) {
        this.sprite = sprite;
        this.parent = parent;
        this.id = UUID.randomUUID();
        this.children = new ArrayList<>();
        this.baseTransform = new Transform(new Vector2(), 0f, 1f);
    }
}
