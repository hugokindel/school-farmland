package com.ustudents.farmland.component;

import com.ustudents.engine.core.ecs.Component;
import com.ustudents.engine.graphic.imgui.annotation.Editable;

public class BoxColliderComponent extends Component {
    @Editable
    public Integer width;

    @Editable
    public Integer height;

    public BoxColliderComponent(Integer width, Integer height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public String toString() {
        return "BoxColliderComponent{" +
                "width=" + width +
                ", height=" + height +
                '}';
    }
}