package com.ustudents.engine.core.ecs;

import com.ustudents.engine.Game;
import com.ustudents.engine.core.cli.print.Out;
import com.ustudents.engine.core.cli.print.style.Style;
import com.ustudents.engine.utility.TypeUtil;

import java.util.*;

/** Defines the main handler of every elements of the ECS. */
@SuppressWarnings({"unused", "unchecked"})
public class Registry {
    /** The total number of entities living. */
    private int totalNumberOfEntities;

    /** The last used entity number (ID - 1), used to keep track of entity ID to use. */
    private int lastEntityNumber;

    /** Map to keep track of systems per ID. */
    private final Map<Integer, System> systems;

    /** Map to keep track of entity per ID. */
    private final Map<Integer, Entity> entityPerIndex;

    /** Map to keep track of entity signature per ID. */
    private final Map<Integer, BitSet> signaturePerEntity;

    /** Map to keep track of entities per name. */
    private final Map<String, Set<Entity>> entitiesPerName;

    /** Map to keep track of name per entity. */
    private final Map<Integer, String> namePerEntity;

    /** Map to keep track of entities per tag. */
    private final Map<String, Set<Entity>> entitiesPerTag;

    /** Map to keep track of tags per entity. */
    private final Map<Integer, Set<String>> tagsPerEntity;

    /** Map to keep track parent per entity. */
    private final Map<Integer, Entity> parentPerEntity;

    /** Map to keep track of children per entity. */
    private final Map<Integer, List<Entity>> childrenPerEntity;

    /** Set to keep track of entities at root (with no parent). */
    private final List<Entity> entitiesAtRoot;

    /**
     * Set to keep track of entities that needs to be added to the systems (after being created),
     * to make sure it does not perturb any game loop.
     */
    private final Set<Entity> entitiesToBeAdded;

    /**
     * Set to keep track of entities that needs to be deleted from the systems and memory (after being killed),
     * to make sure it does not perturb any game loop.
     */
    private final Set<Entity> entitiesToBeDeleted;

    /** List to keep track of all component pools (list of components per type). */
    private final List<Pool> componentPools;

    /** Deque to keep track of IDs that can be reused (meaning they were recently freed from killed entities). */
    private final Deque<Integer> freeIds;

    /** The component type registry to keep track of component IDs. */
    private final ComponentTypeRegistry componentTypeRegistry;

    /** Value to use as a base capacity for component pools. */
    private final int baseComponentPoolCapacity;

    /** Class constructor. */
    public Registry() {
        systems = new HashMap<>();
        entityPerIndex = new HashMap<>();
        signaturePerEntity = new HashMap<>();
        entitiesPerName = new HashMap<>();
        namePerEntity = new HashMap<>();
        entitiesPerTag = new HashMap<>();
        tagsPerEntity = new HashMap<>();
        parentPerEntity = new HashMap<>();
        childrenPerEntity = new HashMap<>();
        entitiesAtRoot = new ArrayList<>();
        entitiesToBeAdded = new HashSet<>();
        entitiesToBeDeleted = new HashSet<>();
        componentPools = new ArrayList<>();
        freeIds = new ArrayDeque<>();
        componentTypeRegistry = new ComponentTypeRegistry();
        baseComponentPoolCapacity = 64;
    }

    /**
     * Adds a system of given type.
     *
     * @param classType The system type class.
     * @param args The system type constructor arguments.
     * @param <T> The system type.
     */
    public <T extends System> void addSystem(Class<T> classType, Object... args) {
        int systemId = classType.getName().hashCode();
        T system;

        if (args.length == 0) {
            system = TypeUtil.createInstance(classType, this);
        } else {
            system = TypeUtil.createInstance(classType, this, args);
        }

        assert system != null;

        systems.put(systemId, system);
    }

    /**
     * Removes a system.
     *
     * @param classType The system type class.
     * @param <T> The system type.
     */
    public <T extends System> void removeSystem(Class<T> classType) {
        int systemId = classType.getName().hashCode();

        systems.remove(systemId);
    }

    /**
     * Check whether is possess a specific system.
     *
     * @param classType The system type class.
     * @param <T> The system type.
     *
     * @return if it has the system.
     */
    public <T extends System> boolean hasSystem(Class<T> classType) {
        int systemId = classType.getName().hashCode();

        return systems.containsKey(systemId);
    }

    /**
     * Gets the system.
     *
     * @param classType The system type class.
     * @param <T> The system type.
     *
     * @return the system.
     */
    public <T extends System> T getSystem(Class<T> classType) {
        int systemId = classType.getName().hashCode();

        return (T)systems.get(systemId);
    }

    /**
     * Creates an entity of a specific subtype.
     *
     * @param classType The entity type class.
     * @param args The entity type constructor arguments.
     * @param <T> The entity type.
     *
     * @return the entity.
     */
    public <T extends Entity> T createEntity(Class<T> classType, Object... args) {
        int entityId = freeIds.isEmpty() ? lastEntityNumber++ : freeIds.pop();
        T entity;

        if (args.length == 0) {
            entity = TypeUtil.createInstance(classType, entityId, this);
        } else {
            entity = TypeUtil.createInstance(classType, entityId, this, args);
        }

        entityPerIndex.put(entityId, entity);
        entitiesAtRoot.add(entity);
        signaturePerEntity.put(entityId, new BitSet());
        entitiesToBeAdded.add(entity);
        totalNumberOfEntities++;

        if (Game.get().isDebugging()) {
            Out.printlnDebug("entity " + Style.Bold + entityId + Style.Reset + ": created");
        }

        return entity;
    }

    /**
     * Creates an entity.
     *
     * @return the entity.
     */
    public Entity createEntity() {
        return createEntity(Entity.class);
    }

    /**
     * Kills an entity (it will be deleted at the next registry update).
     *
     * @param entity The entity.
     */
    public void killEntity(Entity entity) {
        entitiesToBeDeleted.add(entity);
        totalNumberOfEntities--;

        if (Game.get().isDebugging()) {
            Out.printlnDebug("entity " + Style.Bold + entity.getId() + Style.Reset + ": killed");
        }
    }

    /**
     * Returns an entity by ID.
     *
     * @param index The ID.
     *
     * @return the entity.
     */
    public Entity getEntityById(int index) {
        return entityPerIndex.get(index);
    }

    /**
     * Gets all entity by tag.
     *
     * @param tag The tag.
     *
     * @return the entities.
     */
    public Set<Entity> getEntitiesByTag(String tag) {
        return entitiesPerTag.get(tag);
    }

    /**
     * Gets all entity by name.
     *
     * @param name The name.
     *
     * @return the entities.
     */
    public Set<Entity> getEntitiesByName(String name) {
        return entitiesPerName.get(name);
    }

    /**
     * Gets all entity at root (with no parents).
     *
     * @return a set of entity.
     */
    public List<Entity> getEntitiesAtRoot() {
        return entitiesAtRoot;
    }

    /**
     * Sets the name of an entity.
     *
     * @param entity The entity.
     * @param name The name.
     */
    public void setNameOfEntity(Entity entity, String name) {
        if (!entitiesPerName.containsKey(name)) {
            entitiesPerName.put(name, new HashSet<>());
        }

        entitiesPerName.get(name).add(entity);
        namePerEntity.put(entity.getId(), name);
    }

    /**
     * Gets the name of an entity.
     *
     * @param entity The entity.
     *
     * @return the name.
     */
    public String getNameOfEntity(Entity entity) {
        int entityId = entity.getId();

        if (!namePerEntity.containsKey(entityId)) {
            return null;
        }

        return namePerEntity.get(entityId);
    }

    /**
     * Checks if the entity has a name.
     *
     * @param entity The entity.
     *
     * @return if it has a name.
     */
    public boolean entityHasName(Entity entity) {
        return namePerEntity.containsKey(entity.getId());
    }

    /**
     * Removes the name from the entity.
     *
     * @param entity The entity.
     */
    public void removeNameFromEntity(Entity entity) {
        int entityId = entity.getId();

        if (namePerEntity.containsKey(entityId)) {
            String name = namePerEntity.get(entityId);
            entitiesPerName.get(name).remove(entity);

            if (entitiesPerName.get(name).isEmpty()) {
                entitiesPerName.remove(name);
            }

            namePerEntity.remove(entityId);
        }
    }

    /**
     * Adds a tag to the entity.
     *
     * @param entity The entity.
     * @param tag The tag.
     */
    public void addEntityToTag(Entity entity, String tag) {
        int entityId = entity.getId();

        if (!entitiesPerTag.containsKey(tag)) {
            entitiesPerTag.put(tag, new HashSet<>());
        }

        entitiesPerTag.get(tag).add(entity);

        if (!tagsPerEntity.containsKey(entityId)) {
            tagsPerEntity.put(entityId, new HashSet<>());
        }

        tagsPerEntity.get(entityId).add(tag);
    }


    /**
     * Gets all the tags of the entity.
     *
     * @param entity The entity.
     *
     * @return the tags.
     */
    public Set<String> getTagsOfEntity(Entity entity) {
        int entityId = entity.getId();

        if (!tagsPerEntity.containsKey(entityId)) {
            return new HashSet<>();
        }

        return tagsPerEntity.get(entityId);
    }

    /**
     * Checks if the entity has a specific tag.
     *
     * @param entity The entity.
     * @param tag The tag.
     *
     * @return if it has the tag.
     */
    public boolean entityHasTag(Entity entity, String tag) {
        return entitiesPerTag.containsKey(tag) && entitiesPerTag.get(tag).contains(entity);
    }

    /**
     * Removes the tag from the entity (if it has it).
     *
     * @param entity The entity.
     * @param tag The tag.
     */
    public void removeTagFromEntity(Entity entity, String tag) {
        int entityId = entity.getId();

        if (tagsPerEntity.containsKey(entityId)) {
            tagsPerEntity.get(entityId).remove(tag);
        }

        if (entitiesPerTag.containsKey(tag)) {
            entitiesPerTag.get(tag).remove(entity);
        }
    }

    /**
     * Removes all tags from the entity (if it has any).
     *
     * @param entity The entity.
     */
    public void removeAllTagsFromEntity(Entity entity) {
        int entityId = entity.getId();

        if (tagsPerEntity.containsKey(entityId)) {
            Set<String> tags = tagsPerEntity.get(entityId);

            for (String tag : tags) {
                if (entitiesPerTag.containsKey(tag)) {
                    entitiesPerTag.get(tag).remove(entity);

                    if (entitiesPerTag.get(tag).isEmpty()) {
                        entitiesPerTag.remove(tag);
                    }
                }

                tagsPerEntity.get(entityId).remove(tag);
            }

            if (tagsPerEntity.get(entityId).isEmpty()) {
                tagsPerEntity.remove(entityId);
            }
        }
    }

    /**
     * Sets the parent of the entity.
     *
     * @param entity The entity.
     * @param parentEntity The parent entity.
     */
    public void setParentOfEntity(Entity entity, Entity parentEntity) {
        int entityId = entity.getId();
        int parentId = parentEntity.getId();

        if (parentPerEntity.containsKey(entityId)) {
            int originalParentId = parentPerEntity.get(entityId).getId();

            childrenPerEntity.get(originalParentId).remove(entity);

            if (childrenPerEntity.get(originalParentId).isEmpty()) {
                childrenPerEntity.remove(originalParentId);
            }
        } else {
            entitiesAtRoot.remove(entity);
        }

        parentPerEntity.put(entityId, parentEntity);

        if (!childrenPerEntity.containsKey(parentId)) {
            childrenPerEntity.put(parentId, new ArrayList<>());
        }

        childrenPerEntity.get(parentId).add(entity);
    }

    /**
     * Gets the parent of the entity.
     *
     * @param entity The entity.
     *
     * @return the parent.
     */
    public Entity getParentOfEntity(Entity entity) {
        int entityId = entity.getId();

        return parentPerEntity.get(entityId);
    }

    /**
     * Checks if the entity has a parent.
     *
     * @param entity The entity.
     *
     * @return if it has a parent.
     */
    public boolean entityHasParent(Entity entity) {
        int entityId = entity.getId();

        return parentPerEntity.containsKey(entityId);
    }

    /**
     * Removes the parent from the entity (if it has one).
     * 
     * @param entity The entity.
     */
    public void removeParentFromEntity(Entity entity) {
        int entityId = entity.getId();

        if (parentPerEntity.containsKey(entityId)) {
            int originalParentId = parentPerEntity.get(entityId).getId();

            childrenPerEntity.get(originalParentId).remove(entity);

            if (childrenPerEntity.get(originalParentId).isEmpty()) {
                childrenPerEntity.remove(originalParentId);
            }
        }

        parentPerEntity.remove(entityId);
        entitiesAtRoot.add(entity);
    }

    /**
     * Gets the children from the entity.
     * 
     * @param entity The entity.
     * @return a set of children.
     */
    public List<Entity> getChildrenOfEntity(Entity entity) {
        int entityId = entity.getId();

        if (!childrenPerEntity.containsKey(entityId)) {
            return new ArrayList<>();
        }

        return childrenPerEntity.get(entityId);
    }

    /**
     * Add a component of a given type to the entity.
     * 
     * @param entity The entity.
     * @param classType The component type class.
     * @param args The component type constructor arguments.
     * @param <T> The component type.
     */
    public <T extends Component> void addComponentToEntity(Entity entity, Class<T> classType, Object... args) {
        int entityId = entity.getId();
        int componentId = componentTypeRegistry.getIdForType(classType);

        if (componentPools.size() <= componentId) {
            componentPools.add(new ComponentPool<T>(baseComponentPoolCapacity));
        }

        T component = TypeUtil.createInstance(classType, args);

        assert component != null;

        component.setId(componentTypeRegistry.getIdForType(classType));
        ((ComponentPool<T>)componentPools.get(componentId)).set(entityId, component);
        signaturePerEntity.get(entityId).set(componentId);

        if (Game.get().isDebugging()) {
            Out.printlnDebug("entity " + Style.Bold + entity.getId() + Style.Reset + ": component " + Style.Bold + component.getId() + Style.Reset + ": added");
        }
    }

    /**
     * Gets the component of a given type of the entity.
     * 
     * @param entity The entity.
     * @param classType The component type class.
     * @param <T> The component type.
     *           
     * @return the component.
     */
    public <T extends Component> T getComponentOfEntity(Entity entity, Class<T> classType) {
        int entityId = entity.getId();
        int componentId = componentTypeRegistry.getIdForType(classType);

        return ((ComponentPool<T>)componentPools.get(componentId)).getFromEntity(entityId);
    }

    /**
     * Gets all components of the entity.
     * 
     * @param entity The entity.
     *               
     * @return the components.
     */
    public Set<Component> getComponentsOfEntity(Entity entity) {
        int entityId = entity.getId();
        Set<Component> set = new HashSet<>();

        for (Pool pool : componentPools) {
            ComponentPool<Component> componentPool = ((ComponentPool<Component>)pool);

            if (componentPool.containsEntity(entityId)) {
                set.add(componentPool.getFromEntity(entityId));
            }
        }

        return set;
    }

    /**
     * Gets the number of components of the entity.
     * 
     * @param entity The entity.
     *               
     * @return the number of components.
     */
    public int getNumberOfComponentsOfEntity(Entity entity) {
        int entityId = entity.getId();

        return signaturePerEntity.get(entityId).cardinality();
    }

    /**
     * Checks if the entity has the component.
     * 
     * @param entity The entity.
     * @param classType The component type class.
     * @param <T> The component type.
     *           
     * @return if it has the component.
     */
    public <T extends Component> boolean entityHasComponent(Entity entity, Class<T> classType) {
        int componentId = componentTypeRegistry.getIdForType(classType);
        int entityId = entity.getId();

        return signaturePerEntity.get(entityId).get(componentId);
    }

    /**
     * Removes the component from the entity.
     *
     * @param entity The entity.
     * @param classType The component type class.
     * @param <T> The component type.
     */
    public <T extends Component> void removeComponentFromEntity(Entity entity, Class<T> classType) {
        int componentId = componentTypeRegistry.getIdForType(classType);
        int entityId = entity.getId();

        ((ComponentPool<T>)componentPools.get(componentId)).remove(entityId);
        signaturePerEntity.get(entityId).clear(componentId);

        if (Game.get().isDebugging()) {
            Out.printlnDebug("entity " + Style.Bold + entity.getId() + Style.Reset + ": component " + Style.Bold + componentId + Style.Reset + ": removed");
        }
    }

    /**
     * Adds the entity to all systems.
     *
     * @param entity The entity.
     */
    public void addEntityToSystems(Entity entity) {
        int entityId = entity.getId();
        BitSet entitySignature = (BitSet) signaturePerEntity.get(entityId).clone();

        for (Map.Entry<Integer, System> system : systems.entrySet()) {
            BitSet systemSignature = system.getValue().signature;
            entitySignature.and(systemSignature);

            if (entitySignature.equals(systemSignature)) {
                system.getValue().addEntity(entity);
            }
        }
    }

    /**
     * Removes the entity from all systems.
     *
     * @param entity The entity.
     */
    public void removeEntityFromSystems(Entity entity) {
        for (Map.Entry<Integer, System> system : systems.entrySet()) {
            system.getValue().removeEntity(entity);
        }
    }

    /** Updates the registry (takes care of all recently created entities and all recently killed entities). */
    public void updateEntities() {
        addCreatedEntities();
        removeKilledEntities();
    }

    /** @return the total number of entities currently living. */
    public int getTotalNumberOfEntities() {
        return totalNumberOfEntities;
    }

    /** @return the last entity number used. */
    public int getLastEntityNumber() {
        return lastEntityNumber;
    }

    /** @return the component type registry. */
    public ComponentTypeRegistry getComponentTypeRegistry() {
        return componentTypeRegistry;
    }

    public void update(double dt) {
        for (Map.Entry<Integer, System> system : systems.entrySet()) {
            system.getValue().update(dt);
        }
    }

    public void render() {
        for (Map.Entry<Integer, System> system : systems.entrySet()) {
            system.getValue().render();
        }
    }

    /** Add recently created entities to all systems. */
    private void addCreatedEntities() {
        for (Entity entity : entitiesToBeAdded) {
            addEntityToSystems(entity);
        }

        entitiesToBeAdded.clear();
    }

    /** Removes recently killed entities from all systems. */
    private void removeKilledEntities() {
        for (Entity entity : entitiesToBeDeleted) {
            removeEntityFromSystems(entity);

            int entityId = entity.getId();

            signaturePerEntity.remove(entityId);
            entityPerIndex.remove(entityId);

            for (Pool pool : componentPools) {
                if (pool != null) {
                    pool.removeEntityFromPool(entityId);
                }
            }

            freeIds.add(entityId);
            removeNameFromEntity(entity);
            removeAllTagsFromEntity(entity);
        }

        entitiesToBeDeleted.clear();
    }
}