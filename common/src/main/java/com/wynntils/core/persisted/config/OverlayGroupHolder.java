/*
 * Copyright © Wynntils 2023.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.persisted.config;

import com.google.gson.reflect.TypeToken;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.consumers.overlays.Overlay;
import com.wynntils.core.consumers.overlays.RenderState;
import com.wynntils.mc.event.RenderEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.reflect.FieldUtils;

public class OverlayGroupHolder {
    private final Field field;
    private final Feature parent;
    private final int defaultCount;
    private final RenderEvent.ElementType elementType;
    private final RenderState renderState;

    private final Class<?> overlayClass;

    public OverlayGroupHolder(
            Field field,
            Feature parent,
            RenderEvent.ElementType elementType,
            RenderState renderState,
            int defaultCount) {
        this.field = field;
        this.parent = parent;
        this.elementType = elementType;
        this.renderState = renderState;
        this.defaultCount = defaultCount;

        Type genericType = this.field.getGenericType();

        if (genericType instanceof ParameterizedType parameterizedType) {
            this.overlayClass =
                    TypeToken.get(parameterizedType.getActualTypeArguments()[0]).getRawType();
        } else {
            throw new IllegalArgumentException("Field " + field.getName() + " is not a list.");
        }
    }

    public String getConfigKey() {
        return parent.getJsonName() + ".groupedOverlay." + field.getName() + ".ids";
    }

    public int getOverlayCount() {
        try {
            return ((List<?>) FieldUtils.readField(field, parent, true)).size();
        } catch (IllegalAccessException e) {
            return defaultCount;
        }
    }

    public List<? extends Overlay> getOverlays() {
        try {
            return ((List<? extends Overlay>) FieldUtils.readField(field, parent, true));
        } catch (IllegalAccessException e) {
            return List.of();
        }
    }

    public RenderEvent.ElementType getElementType() {
        return elementType;
    }

    public RenderState getRenderState() {
        return renderState;
    }

    public int getDefaultCount() {
        return defaultCount;
    }

    public Feature getParent() {
        return parent;
    }

    public String getFieldName() {
        return field.getName();
    }

    public Class<?> getOverlayClass() {
        return overlayClass;
    }

    // Do not call this. Use OverlayManager instead.
    public void initGroup(List<Integer> ids) {
        try {
            // Validate that the class is actually an Overlay subclass
            if (!Overlay.class.isAssignableFrom(overlayClass)) {
                throw new IllegalArgumentException("Class " + overlayClass.getName() + " is not a subclass of Overlay");
            }
            
            List<Overlay> overlays = new ArrayList<>();

            for (Integer id : ids) {
                // Validate input
                if (id == null || id < 0) {
                    WynntilsMod.warn("Skipping invalid overlay id: " + id);
                    continue;
                }
                
                // Use specific constructor with validation
                Constructor<? extends Overlay> constructor = overlayClass.asSubclass(Overlay.class).getConstructor(int.class);
                constructor.setAccessible(true);
                Overlay overlay = constructor.newInstance(id);
                overlays.add(overlay);
            }

            FieldUtils.writeField(field, parent, overlays, true);
        } catch (Exception e) {
            WynntilsMod.error("Failed to initialize grouped overlay: " + field.getName(), e);
        }
    }
}
