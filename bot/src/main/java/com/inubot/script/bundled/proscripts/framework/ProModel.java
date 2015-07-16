/*
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the license, or (at your option) any later version.
 */
package com.inubot.script.bundled.proscripts.framework;

import java.util.*;

/**
 * @author Dogerina
 * @since 15-07-2015
 */
public abstract class ProModel {

    private final List<Listener> listeners;

    public ProModel() {
        this.listeners = new ArrayList<>();
    }

    public final void addLater(Listener listener) {
        synchronized (this) {
            listeners.add(listener);
        }
    }

    public final void fireLater(String property) {
        Event<ProModel> event = new Event<>(this, property);
        synchronized (this) {
            listeners.forEach(o -> o.encounter(event));
        }
    }

    public interface Listener {
        void encounter(Event<?> event);
    }

    public static final class Event<T> extends EventObject {

        private final String property;

        /**
         * Constructs a prototypical Event.
         *
         * @param source The object on which the Event initially occurred.
         * @throws IllegalArgumentException if source is null.
         */
        public Event(T source, String property) {
            super(source);
            this.property = property;
        }

        /**
         * @return The source event object
         */
        public final T getSource() {
            return (T) super.getSource();
        }

        /**
         * @return The changed property
         */
        public String getProperty() {
            return property;
        }
    }
}