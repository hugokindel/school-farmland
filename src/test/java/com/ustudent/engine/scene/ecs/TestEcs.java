package com.ustudent.engine.scene.ecs;

import com.ustudents.engine.core.ecs.Entity;
import com.ustudents.engine.core.ecs.Registry;
import com.ustudents.farmland.component.BoxColliderComponent;
import com.ustudents.farmland.component.CameraFollowComponent;
import com.ustudents.farmland.component.SpriteComponent;
import com.ustudents.farmland.component.TransformComponent;
import org.joml.Vector2f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TestEcs {
    private Registry registry;

    @BeforeEach
    public void init() {
        registry = new Registry();
    }

    @Test
    public void testEntityCreation() {
        registry.createEntity();

        registry.updateEntities();

        assertEquals(registry.getTotalNumberOfEntities(), 1);
        assertEquals(registry.getLastEntityNumber(), 1);
        assertNotNull(registry.getEntityById(0));
    }

    @Test
    public void testMultipleEntityCreation() {
        registry.createEntity();
        registry.createEntity();
        registry.createEntity();

        registry.updateEntities();

        assertEquals(registry.getTotalNumberOfEntities(), 3);
        assertEquals(registry.getLastEntityNumber(), 3);
        assertNotNull(registry.getEntityById(0));
        assertNotNull(registry.getEntityById(1));
        assertNotNull(registry.getEntityById(2));
    }

    @Test
    public void testEntityDestruction() {
        registry.createEntity();

        registry.updateEntities();

        registry.killEntity(registry.getEntityById(0));

        registry.updateEntities();

        assertEquals(registry.getTotalNumberOfEntities(), 0);
        assertEquals(registry.getLastEntityNumber(), 1);
        assertNull(registry.getEntityById(0));
    }

    @Test
    public void testComponentCreation() {
        Entity entity = registry.createEntity();
        entity.addComponent(TransformComponent.class);
        entity.addComponent(CameraFollowComponent.class);

        registry.updateEntities();

        assertEquals(registry.getTotalNumberOfEntities(), 1);
        assertEquals(registry.getLastEntityNumber(), 1);
        assertEquals(entity.getNumberOfComponents(), 2);
        assertNotNull(registry.getEntityById(0));
    }

    @Test
    public void testComponentDestruction() {
        Entity entity = registry.createEntity();
        entity.addComponent(TransformComponent.class);
        entity.addComponent(CameraFollowComponent.class);

        registry.updateEntities();

        entity.removeComponent(CameraFollowComponent.class);

        assertEquals(registry.getTotalNumberOfEntities(), 1);
        assertEquals(registry.getLastEntityNumber(), 1);
        assertEquals(entity.getNumberOfComponents(), 1);
        assertNotNull(registry.getEntityById(0));
    }

    @Test
    public void testGeneral1() {
        Entity playerContainer = registry.createEntity();
        playerContainer.setName("player-container");

        Entity player1 = registry.createEntity();
        player1.addComponent(TransformComponent.class, new Vector2f(57, 52), new Vector2f(1, 1));
        player1.addComponent(SpriteComponent.class, "player1.png");
        player1.addComponent(CameraFollowComponent.class);
        player1.addComponent(BoxColliderComponent.class, 50, 50);
        player1.addTag("players");
        player1.setName("player1");
        player1.setParent(playerContainer);

        Entity player2 = registry.createEntity();
        player2.addComponent(TransformComponent.class, new Vector2f(12, 67), new Vector2f(1, 1), 21.0);
        player2.addComponent(SpriteComponent.class, "player2.png");
        player2.addComponent(BoxColliderComponent.class, 50, 50);
        player2.addTag("players");
        player2.setName("player2");
        player2.setParent(playerContainer);

        Entity player3 = registry.createEntity();
        player3.addComponent(TransformComponent.class, new Vector2f(876, 20), new Vector2f(1, 1), 57.0);
        player3.addComponent(SpriteComponent.class, "player3.png");
        player3.addComponent(BoxColliderComponent.class, 75, 75);
        player3.addTag("players");
        player3.setName("player3");
        player3.setParent(playerContainer);

        Entity player4 = registry.createEntity();
        player4.addComponent(TransformComponent.class, new Vector2f(78, 1), new Vector2f(1, 1));
        player4.addComponent(SpriteComponent.class, "player4.png");
        player4.addComponent(BoxColliderComponent.class, 80, 80);
        player4.addTag("players");
        player4.setName("player4");
        player4.setParent(playerContainer);
        player1.getComponents();

        registry.updateEntities();

        assertEquals(registry.getTotalNumberOfEntities(), 5);
        assertEquals(playerContainer.getName(), "player-container");
        assertEquals(player1.getName(), "player1");
        assertEquals(player2.getName(), "player2");
        assertEquals(player3.getName(), "player3");
        assertEquals(player4.getName(), "player4");
        assertEquals(playerContainer.getId(), 0);
        assertEquals(player1.getId(), 1);
        assertEquals(player2.getId(), 2);
        assertEquals(player3.getId(), 3);
        assertEquals(player4.getId(), 4);
        assertEquals(playerContainer.getChildren().size(), 4);



        assertNotNull(registry.getEntityById(0));
    }
}