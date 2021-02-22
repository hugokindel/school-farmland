package com.ustudents.engine.gui;

import com.ustudents.engine.core.Resources;
import com.ustudents.engine.core.Window;
import com.ustudents.engine.core.event.EventListener;
import com.ustudents.engine.ecs.Entity;
import com.ustudents.engine.ecs.Registry;
import com.ustudents.engine.ecs.component.core.TransformComponent;
import com.ustudents.engine.ecs.component.graphics.NineSlicedSpriteComponent;
import com.ustudents.engine.ecs.component.graphics.TextureComponent;
import com.ustudents.engine.ecs.component.graphics.UiRendererComponent;
import com.ustudents.engine.ecs.component.gui.ButtonComponent;
import com.ustudents.engine.ecs.component.gui.TextComponent;
import com.ustudents.engine.graphic.*;
import com.ustudents.engine.scene.Scene;
import com.ustudents.engine.scene.SceneManager;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.w3c.dom.Text;

public class GuiBuilder {
    public static class TextData {
        public String text;

        public String id;

        public Font font;

        public Vector2f position;

        public Vector2f scale;

        public Origin origin;

        public Anchor anchor;

        public Color color;

        public boolean applyGlobalScaling;

        public TextData(String text) {
            this.text = text;
            this.id = "button";
            this.position = new Vector2f();
            this.scale = new Vector2f(1.0f, 1.0f);
            this.font = Resources.loadFont("ui/default.ttf", 16);
            this.origin = new Origin(Origin.Vertical.Top, Origin.Horizontal.Left);
            this.anchor = new Anchor(Anchor.Vertical.Top, Anchor.Horizontal.Left);
            this.color = Color.WHITE;
            this.applyGlobalScaling = true;
        }
    }

    public static class ButtonData {
        public String text;

        public String id;

        public Font font;

        public Vector2f position;

        public Vector2f scale;

        public Origin origin;

        public Anchor anchor;

        public EventListener listener;

        public boolean applyGlobalScaling;

        public ButtonData(String text, EventListener listener) {
            this.text = text;
            this.id = "button";
            this.position = new Vector2f();
            this.listener = listener;
            this.scale = new Vector2f(1.0f, 1.0f);
            this.font = Resources.loadFont("ui/default.ttf", 16);
            this.origin = new Origin(Origin.Vertical.Top, Origin.Horizontal.Left);
            this.anchor = new Anchor(Anchor.Vertical.Top, Anchor.Horizontal.Left);
            this.applyGlobalScaling = true;
        }
    }

    public static class ImageData {
        public Texture texture;

        public String id;

        public Vector2f position;

        public Vector2f scale;

        public Origin origin;

        public Anchor anchor;

        public boolean applyGlobalScaling;

        public ImageData(Texture texture) {
            this.texture = texture;
            this.id = "image";
            this.scale = new Vector2f(1.0f, 1.0f);
            this.applyGlobalScaling = false;
            this.position = new Vector2f();
            this.origin = new Origin(Origin.Vertical.Top, Origin.Horizontal.Left);
            this.anchor = new Anchor(Anchor.Vertical.Top, Anchor.Horizontal.Left);
        }
    }

    public static class WindowData {
        public String id;

        public Vector2f position;

        public Vector2f scale;

        public Origin origin;

        public Anchor anchor;

        public WindowData() {
            this.id = "window";
            this.position = new Vector2f();
            this.scale = new Vector2f(1.0f, 1.0f);
            this.origin = new Origin(Origin.Vertical.Top, Origin.Horizontal.Left);
            this.anchor = new Anchor(Anchor.Vertical.Top, Anchor.Horizontal.Left);
        }

        public static WindowData copy(WindowData data) {
            WindowData windowData = new WindowData();
            windowData.id = data.id;
            windowData.position = data.position;
            windowData.scale = data.scale;
            windowData.origin = data.origin;
            windowData.anchor = data.anchor;
            return windowData;
        }
    }

    private static class WindowContainer {
        Entity entity;
        WindowData data;
        Entity content;
        TextData contentData;

        public static WindowContainer copy(WindowContainer data, TextData contentData) {
            WindowContainer windowContainer = new WindowContainer();
            windowContainer.entity = data.entity;
            windowContainer.data = WindowData.copy(data.data);
            windowContainer.contentData = contentData;
            windowContainer.content = data.content;
            return windowContainer;
        }
    }

    Scene scene = SceneManager.getScene();

    Registry registry = SceneManager.getScene().getRegistry();

    Entity canvas;

    Font font = Resources.loadFont("ui/default.ttf", 16);

    Vector2f globalScale;

    WindowContainer currentWindow;

    public GuiBuilder() {
        canvas = registry.createEntityWithName("canvas");
        globalScale = new Vector2f(3.0f, 3.0f);
    }

    public void addText(TextData data) {
        if (currentWindow == null) {
            Entity text = canvas.createChildWithName(data.id);
            TransformComponent transformComponent = createScaledComponent(data.scale, data.applyGlobalScaling);
            textPosition(data, transformComponent);
            Window.get().sizeChanged.add((dataType, windowData) -> textPosition(data, transformComponent));
            text.addComponent(transformComponent);
            text.addComponent(new UiRendererComponent());
            TextComponent textComponent = text.addComponent(new TextComponent(data.text, data.font));
            textComponent.color = data.color;
        } else {
            Entity text = currentWindow.entity.createChildWithName(data.id);
            TransformComponent transformComponent = createScaledComponent(data.scale, data.applyGlobalScaling);
            textPosition(data, transformComponent);
            text.addComponent(transformComponent);
            text.addComponent(new UiRendererComponent());
            TextComponent textComponent = text.addComponent(new TextComponent(data.text, data.font));
            textComponent.color = data.color;
            currentWindow.content = text;
            currentWindow.contentData = data;
        }
    }

    public void addButton(ButtonData data) {
        Entity button = canvas.createChildWithName(data.id);
        TransformComponent transformComponent = createScaledComponent(data.scale, data.applyGlobalScaling);
        buttonPosition(data, transformComponent);
        Window.get().sizeChanged.add((dataType, windowData) -> buttonPosition(data, transformComponent));
        button.addComponent(transformComponent);
        button.addComponent(new UiRendererComponent());
        button.addComponent(new ButtonComponent(data.text, data.listener));
    }

    public void addImage(ImageData data) {
        Entity image = canvas.createChildWithName(data.id);
        TransformComponent transformComponent = createScaledComponent(data.scale, data.applyGlobalScaling);
        imagePosition(data, transformComponent);
        Window.get().sizeChanged.add((dataType, windowData) -> imagePosition(data, transformComponent));
        image.addComponent(transformComponent);
        image.addComponent(new UiRendererComponent());
        image.addComponent(new TextureComponent(data.texture));
    }

    public void beginWindow(WindowData data) {
        Entity window = canvas.createChildWithName(data.id);
        currentWindow = new WindowContainer();
        currentWindow.entity = window;
        currentWindow.data = data;
    }

    public void endWindow() {
        NineSlicedSprite nineSlicedSprite = new NineSlicedSprite(Resources.loadSpritesheet("ui/window_default.json"));

        TransformComponent transformComponent = createScaledComponent(currentWindow.data.scale, true);
        WindowContainer copy = WindowContainer.copy(currentWindow, currentWindow.contentData);
        windowPosition(copy.content, copy.data, transformComponent);
        Window.get().sizeChanged.add((dataType, windowData) -> windowPosition(copy.content, copy.data, transformComponent));
        currentWindow.entity.addComponent(transformComponent);
        currentWindow.entity.addComponent(new UiRendererComponent());
        currentWindow.entity.addComponent(new NineSlicedSpriteComponent(nineSlicedSprite, currentWindow.content.getComponent(TextComponent.class).getSize().div(transformComponent.scale)));

        TransformComponent contentTransform = currentWindow.content.getComponent(TransformComponent.class);
        currentWindow.content.getComponent(UiRendererComponent.class).zIndex++;
        contentTransform.position = new Vector2f(transformComponent.position.x + 5 * transformComponent.scale.x, transformComponent.position.y + 5 * transformComponent.scale.y);
        Window.get().sizeChanged.add((dataType, windowData) -> contentTransform.position = new Vector2f(transformComponent.position.x + 5 * transformComponent.scale.x, transformComponent.position.y + 5 * transformComponent.scale.y));

        currentWindow = null;
    }

    private void textPosition(TextData data, TransformComponent transformComponent) {
        transformComponent.position = new Vector2f(data.position.x, data.position.y);

        switch (data.origin.horizontal) {
            case Custom:
                transformComponent.position.x += data.origin.customHorizontal;
                break;
            case Left:
                break;
            case Center:
                transformComponent.position.x -= data.font.getScaledTextWidth(data.text, transformComponent.scale.x) / 2 * transformComponent.scale.x;
                break;
            case Right:
                transformComponent.position.x -= data.font.getScaledTextWidth(data.text, transformComponent.scale.x) * transformComponent.scale.x;
                break;
        }

        switch (data.origin.vertical) {
            case Custom:
                transformComponent.position.x += data.origin.customVertical;
                break;
            case Top:
                break;
            case Middle:
                transformComponent.position.y -= data.font.getScaledTextHeight(data.text, transformComponent.scale.y) / 2 * transformComponent.scale.y;
                break;
            case Bottom:
                transformComponent.position.y -= data.font.getScaledTextHeight(data.text, transformComponent.scale.y) * transformComponent.scale.x;
                break;
        }

        Vector2i windowSize = Window.get().getSize();

        switch (data.anchor.horizontal) {
            case Left:
                break;
            case Center:
                transformComponent.position.x += (float)windowSize.x / 2;
                break;
            case Right:
                transformComponent.position.x += (float)windowSize.x;
                break;
        }

        switch (data.anchor.vertical) {

            case Top:
                break;
            case Middle:
                transformComponent.position.y += (float)windowSize.y / 2;
                break;
            case Bottom:
                transformComponent.position.y += (float)windowSize.y;
                break;
        }
    }

    private void buttonPosition(ButtonData data, TransformComponent transformComponent) {
        transformComponent.position = new Vector2f(data.position.x, data.position.y);

        switch (data.origin.horizontal) {
            case Custom:
                transformComponent.position.x += data.origin.customHorizontal;
                break;
            case Left:
                break;
            case Center:
                transformComponent.position.x -= (5 + (data.font.getScaledTextWidth(data.text, transformComponent.scale.x) / 2)) * transformComponent.scale.x;
                break;
            case Right:
                transformComponent.position.x -= (10 + (data.font.getScaledTextWidth(data.text, transformComponent.scale.x))) * transformComponent.scale.x;
                break;
        }

        switch (data.origin.vertical) {
            case Custom:
                transformComponent.position.x += data.origin.customVertical;
                break;
            case Top:
                break;
            case Middle:
                transformComponent.position.y -= (5 + (data.font.getScaledTextHeight(data.text, transformComponent.scale.y) / 2)) * transformComponent.scale.y;
                break;
            case Bottom:
                transformComponent.position.y -= (10 + (data.font.getScaledTextHeight(data.text, transformComponent.scale.y))) * transformComponent.scale.x;
                break;
        }

        Vector2i windowSize = Window.get().getSize();

        switch (data.anchor.horizontal) {
            case Left:
                break;
            case Center:
                transformComponent.position.x += (float)windowSize.x / 2;
                break;
            case Right:
                transformComponent.position.x += (float)windowSize.x;
                break;
        }

        switch (data.anchor.vertical) {

            case Top:
                break;
            case Middle:
                transformComponent.position.y += (float)windowSize.y / 2;
                break;
            case Bottom:
                transformComponent.position.y += (float)windowSize.y;
                break;
        }
    }

    private void imagePosition(ImageData data, TransformComponent transformComponent) {
        transformComponent.position = new Vector2f(data.position.x, data.position.y);

        switch (data.origin.horizontal) {
            case Custom:
                transformComponent.position.x += data.origin.customHorizontal;
                break;
            case Left:
                break;
            case Center:
                transformComponent.position.x -= data.texture.getWidth() * transformComponent.scale.x / 2;
                break;
            case Right:
                transformComponent.position.x -= data.texture.getWidth() * transformComponent.scale.x;
                break;
        }

        switch (data.origin.vertical) {
            case Custom:
                transformComponent.position.x += data.origin.customVertical;
                break;
            case Top:
                break;
            case Middle:
                transformComponent.position.x -= data.texture.getHeight() * transformComponent.scale.y / 2;
                break;
            case Bottom:
                transformComponent.position.x -= data.texture.getHeight() * transformComponent.scale.y;
                break;
        }

        Vector2i windowSize = Window.get().getSize();

        switch (data.anchor.horizontal) {
            case Left:
                break;
            case Center:
                transformComponent.position.x += (float)windowSize.x / 2;
                break;
            case Right:
                transformComponent.position.x += (float)windowSize.x;
                break;
        }

        switch (data.anchor.vertical) {

            case Top:
                break;
            case Middle:
                transformComponent.position.y += (float)windowSize.y / 2;
                break;
            case Bottom:
                transformComponent.position.y += (float)windowSize.y;
                break;
        }
    }

    private void windowPosition(Entity content, WindowData data, TransformComponent transformComponent) {
        if (content != null && content.getComponentSafe(TextComponent.class) != null) {
            transformComponent.position = new Vector2f(data.position.x, data.position.y);

            TextComponent textComponent = content.getComponent(TextComponent.class);

            switch (data.origin.horizontal) {
                case Custom:
                    transformComponent.position.x += data.origin.customHorizontal;
                    break;
                case Left:
                    break;
                case Center:
                    transformComponent.position.x -= (5 + (textComponent.font.getScaledTextWidth(textComponent.text, transformComponent.scale.x) / 2)) * transformComponent.scale.x;
                    break;
                case Right:
                    transformComponent.position.x -= (10 + (textComponent.font.getScaledTextWidth(textComponent.text, transformComponent.scale.x))) * transformComponent.scale.x;
                    break;
            }

            switch (data.origin.vertical) {
                case Custom:
                    transformComponent.position.x += data.origin.customVertical;
                    break;
                case Top:
                    break;
                case Middle:
                    transformComponent.position.y -= (5 + (textComponent.font.getScaledTextHeight(textComponent.text, transformComponent.scale.y) / 2)) * transformComponent.scale.y;
                    break;
                case Bottom:
                    transformComponent.position.y -= (10 + (textComponent.font.getScaledTextHeight(textComponent.text, transformComponent.scale.y))) * transformComponent.scale.x;
                    break;
            }

            Vector2i windowSize = Window.get().getSize();

            switch (data.anchor.horizontal) {
                case Left:
                    break;
                case Center:
                    transformComponent.position.x += (float)windowSize.x / 2;
                    break;
                case Right:
                    transformComponent.position.x += (float)windowSize.x;
                    break;
            }

            switch (data.anchor.vertical) {

                case Top:
                    break;
                case Middle:
                    transformComponent.position.y += (float)windowSize.y / 2;
                    break;
                case Bottom:
                    transformComponent.position.y += (float)windowSize.y;
                    break;
            }
        }
    }

    private TransformComponent createScaledComponent(Vector2f scale, boolean applyGlobalScaling) {
        TransformComponent transformComponent = new TransformComponent();

        if (applyGlobalScaling) {
            transformComponent.scale = new Vector2f(globalScale.x, globalScale.y);
        }

        transformComponent.scale.mul(scale);

        return transformComponent;
    }
}