/*
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the license, or (at your option) any later version.
 */
package org.runedream.api.oldschool.collection;

import org.runedream.api.methods.Game;
import org.runedream.api.util.filter.Filter;
import org.runedream.api.oldschool.Character;

public class CharacterPool<T extends Character, K extends CharacterPool<T, K>> extends IdentifiableEntityPool<T, K> {

    public CharacterPool(final Iterable<T>... elements) {
        super(elements);
    }

    public CharacterPool(final T... elements) {
        super(elements);
    }

    public K targeting(final Character<?>... targets) {
        return include(new Filter<T>() {
            @Override
            public boolean accept(T character) {
                for (final Character c : targets) {
                    if (c.equals(character.getTarget())) {
                        return true;
                    }
                }
                return false;
            }
        });
    }

    public K targeting(final boolean targeting) {
        return include(new Filter<T>() {
            @Override
            public boolean accept(T t) {
                return targeting == (t.getTargetIndex() != -1);
            }
        });
    }

    public K animating(final boolean animating) {
        return include(new Filter<T>() {
            @Override
            public boolean accept(T t) {
                return animating == (t.getAnimation() != -1);
            }
        });
    }

    public K animation(final int... animations) {
        return include(t -> {
            for (final int animation : animations) {
                if (t.getAnimation() == animation) {
                    return true;
                }
            }
            return false;
        });
    }

    public K targeting(final int... indexes) {
        return include(t -> {
            for (final int index : indexes) {
                if (t.getTargetIndex() == index) {
                    return true;
                }
            }
            return false;
        });
    }

    public K healthBarVisible(final boolean visible) {
        return include(new Filter<T>() {
            @Override
            public boolean accept(T t) {
                return (t.getHealthBarCycle() + 20 > Game.getEngineCycle()) == visible;
            }
        });
    }

    public K health(Filter<Integer> hpFilter) {
        return include(new Filter<T>() {
            @Override
            public boolean accept(T t) {
                return hpFilter.accept(t.getHealth());
            }
        });
    }
}
