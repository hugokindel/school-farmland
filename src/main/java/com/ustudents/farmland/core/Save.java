package com.ustudents.farmland.core;

import com.ustudents.engine.Game;
import com.ustudents.engine.core.Resources;
import com.ustudents.engine.core.cli.print.Out;
import com.ustudents.engine.core.event.EventDispatcher;
import com.ustudents.engine.core.json.annotation.JsonSerializable;
import com.ustudents.engine.core.json.annotation.JsonSerializableConstructor;
import com.ustudents.engine.graphic.Color;
import com.ustudents.engine.graphic.Sprite;
import com.ustudents.engine.graphic.Texture;
import com.ustudents.engine.network.NetMode;
import com.ustudents.engine.utility.SeedRandom;
import com.ustudents.farmland.Farmland;
import com.ustudents.farmland.core.grid.Cell;
import com.ustudents.farmland.core.item.Item;
import com.ustudents.farmland.core.player.Player;
import com.ustudents.farmland.network.actions.EndTurnMessage;
import com.ustudents.farmland.network.general.LoadSaveResponse;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector4f;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@JsonSerializable
@SuppressWarnings("unchecked")
public class Save {
    public static final int timePerTurn = 90;

    @JsonSerializable
    public String name;

    @JsonSerializable
    public Integer turn;

    @JsonSerializable
    public Integer turnTimePassed;

    @JsonSerializable
    public Integer mapWidth;

    @JsonSerializable
    public Integer mapHeight;

    @JsonSerializable
    public Long seed;

    @JsonSerializable
    public Integer currentPlayerId;

    @JsonSerializable
    public List<Player> players;

    @JsonSerializable
    public List<Item> itemsTurn;

    @JsonSerializable
    public List<List<Cell>> cells;

    @JsonSerializable
    public Integer capacity;

    public Integer localPlayerId;

    public Boolean startWithBots;

    @JsonSerializable
    public List<Integer> deadPlayers;

    public String path;

    public EventDispatcher turnEnded = new EventDispatcher();

    public EventDispatcher itemUsed = new EventDispatcher();

    public SeedRandom random;

    public Save() {
        this.itemsTurn = new ArrayList<>();
    }

    public Save(String name, Vector2i mapSize, Long seed, int numberOfBots) {
        mapWidth = mapSize.x;
        mapHeight = mapSize.y;
        this.seed = seed;

        if (this.seed == null) {
            this.seed = System.currentTimeMillis();
        }
        SeedRandom random = new SeedRandom(this.seed);

        this.deadPlayers = new LinkedList<>();
        this.startWithBots = numberOfBots > 0;
        this.capacity = 1;
        this.turn = 0;
        this.turnTimePassed = 0;
        this.currentPlayerId = 0;
        this.name = name;
        this.players = new ArrayList<>();
        this.itemsTurn = new ArrayList<>();
        this.random = new SeedRandom(seed);

        this.cells = new ArrayList<>();

        Texture cellBackground = Resources.loadTexture("terrain/grass.png");

        for (int x = 0; x < mapSize.x; x++) {
            this.cells.add(new ArrayList<>());

            for (int y = 0; y < mapSize.y; y++) {
                Vector2f spriteRegion = new Vector2f(
                        24 * random.generateInRange(1, 120 / 24),
                        24 * random.generateInRange(1, 120 / 24));
                Sprite sprite = new Sprite(cellBackground,
                        new Vector4f(spriteRegion.x, spriteRegion.y, 24, 24));
                Vector4f viewRectangle = new Vector4f(
                        5 + x * 24,
                        5 + y * 24,
                        5 + x * 24 + 24,
                        5 + y * 24 + 24);

                cells.get(x).add(new Cell(sprite, viewRectangle));
            }
        }

        List<Color> usedColors = new ArrayList<>();
        List<Vector2i> usedLocations = new ArrayList<>();
        usedLocations.add(new Vector2i(mapSize.x / 2 - 1, mapSize.y / 2 - 1));
        for (int i = 0; i < numberOfBots; i++) {
            this.players.add(new Player("Robot " + (i + 1), "Village de Robot " + (i + 1), generateColor(random, usedColors), Player.Type.Robot));
            Vector2i villagePosition = generateMapLocation(random, usedLocations);
            this.players.get(i).village.position = new Vector2f(5 + villagePosition.x * 24, 5 + villagePosition.y * 24);
            this.cells.get(villagePosition.x).get(villagePosition.y).setOwned(true, i);
            this.cells.get(villagePosition.x + 1).get(villagePosition.y).setOwned(true, i);
            this.cells.get(villagePosition.x).get(villagePosition.y + 1).setOwned(true, i);
            this.cells.get(villagePosition.x + 1).get(villagePosition.y + 1).setOwned(true, i);
        }

        File f = new File(Resources.getSavesDirectoryName());
        this.path = "save-" + (getMaxSavedGamesId() + 1) + ".json";

        if (Game.isDebugging()) {
            Out.printlnDebug("Savegame created.");
        }
    }

    public Save(String name, String playerName, String playerVillageName, Color playerColor, Vector2i mapSize, Long seed, int numberOfBots) {
        this(name, mapSize, seed, numberOfBots);
        addPlayer(playerName, playerVillageName, playerColor, Player.Type.Human);
    }

    @JsonSerializableConstructor
    public void deserialize() {
        random = new SeedRandom(this.seed);
        deadPlayers = new ArrayList<>();
    }

    public void addPlayer(String playerName, String playerVillageName, Color playerColor, Player.Type type) {
        int playerId = getAvailableHumanId();
        this.players.add(playerId, new Player(playerName, playerVillageName, playerColor, type));
        Vector2i villagePosition = generateMapLocation(random, getUsedLocations());
        this.players.get(playerId).village.position = new Vector2f(5 + villagePosition.x * 24, 5 + villagePosition.y * 24);
        this.cells.get(villagePosition.x).get(villagePosition.y).setOwned(true, playerId);
        this.cells.get(villagePosition.x + 1).get(villagePosition.y).setOwned(true, playerId);
        this.cells.get(villagePosition.x).get(villagePosition.y + 1).setOwned(true, playerId);
        this.cells.get(villagePosition.x + 1).get(villagePosition.y + 1).setOwned(true, playerId);
    }

    private int getMaxSavedGamesId() {
        File savedDir = new File(Resources.getSavesDirectoryName());
        File[] list = savedDir.listFiles();
        int max = -1;
        assert list != null;
        for (File file : list) {
            String tmp = file.getName().substring(5, 6);

            if (Character.isDigit(tmp.charAt(0))) {
                int fileId = Integer.parseInt(tmp);
                if (fileId > max) {
                    max = fileId;
                }
            }
        }
        return max;
    }

    public void endTurn() {
        if (Game.get().hasAuthority()) {
            Out.println("Turn end");

            if (Game.isDebugging()) {
                Out.printlnDebug("Turn end");
            }

            if (currentPlayerId == players.size() - 1) {
                turn++;
                currentPlayerId = 0;
            } else {
                currentPlayerId++;
            }

            turnEnded.dispatch();

            if (Game.get().getNetMode() == NetMode.DedicatedServer) {
                Farmland.get().getServer().broadcast(new LoadSaveResponse(Farmland.get().getLoadedSave()));
            }
        } else {
            Game.get().getClient().send(new EndTurnMessage());
        }
    }

    public Player getCurrentPlayer() {
        return players.get(currentPlayerId);
    }

    private Color generateColor(SeedRandom random, List<Color> usedColors) {
        while (true) {
            boolean unique = true;

            Color color = new Color();
            color.r = random.generateInRange(0, 255) / 255.0f;
            color.g = random.generateInRange(0, 255) / 255.0f;
            color.b = random.generateInRange(0, 255) / 255.0f;
            color.a = random.generateInRange(0, 255) / 255.0f;

            for (Color usedColor : usedColors) {
                if (color.equals(usedColor)) {
                    unique = false;
                    break;
                }
            }

            if (unique) {
                usedColors.add(color);
                return color;
            }
        }
    }

    private Vector2i generateMapLocation(SeedRandom random, List<Vector2i> usedLocations) {
        while (true) {
            boolean unique = true;

            Vector2i position = new Vector2i();
            position.x = random.generateInRange(0, cells.size() - 2);
            position.y = random.generateInRange(0, cells.get(0).size() - 2);

            for (Vector2i usedPosition : usedLocations) {
                if (position.x < usedPosition.x + 2 &&
                        position.x + 2 > usedPosition.x &&
                        position.y < usedPosition.y + 2 &&
                        position.y + 2 > usedPosition.y) {
                    unique = false;
                    break;
                }
            }

            if (unique) {
                usedLocations.add(position);
                return position;
            }
        }
    }

    public boolean PlayerMeetCondition() {
        for(Player player : players) {
            if(player.type == Player.Type.Human) {
                return player.money <= 0 || player.money >= 1000;
            }
        }

        return true;
    }

    public boolean BotMeetCondition() {
        for(Player player : players) {
            if(player.type == Player.Type.Robot && !Farmland.get().getLoadedSave().deadPlayers.contains(player.getId())) {
                if (player.money <= 0 || player.money >= 1000){
                    return true;
                }
            }
        }

        return false;
    }

    public Player getLocalPlayer() {
        return players.get(localPlayerId);
    }

    public boolean isLocalPlayerTurn() {
        return getCurrentPlayer().getId().equals(getLocalPlayer().getId());
    }

    public List<Color> getUsedColors() {
        List<Color> colors = new ArrayList<>();

        for (Player player : players) {
            if (!colors.contains(player.color)) {
                colors.add(player.color);
            }
        }

        return colors;
    }

    public List<Vector2i> getUsedLocations() {
        List<Vector2i> locations = new ArrayList<>();

        for (Player player : players) {
            if (player.village.position != null) {
                Vector2i villagePosition = new Vector2i(((int)player.village.position.x - 5) / 24, ((int)player.village.position.y - 5) / 24);

                if (!locations.contains(villagePosition)) {
                    locations.add(villagePosition);
                }
            }
        }

        return locations;
    }

    public Cell getCell(int x, int y) {
        return cells.get(x).get(y);
    }

    public Cell getCell(Vector2i position) {
        return cells.get(position.x).get(position.y);
    }

    private int getAvailableHumanId() {
        int i = 0;

        for (Player player : players) {
            if (player.type == Player.Type.Human) {
                i++;
            }
        }

        for (int x = 0; x < cells.size(); x++) {
            for (int y = 0; y < cells.get(x).size(); y++) {
                Cell cell = cells.get(x).get(y);

                if (cell.isOwnedByBot(this)) {
                    cell.ownerId++;
                }
            }
        }

        return i;
    }
}