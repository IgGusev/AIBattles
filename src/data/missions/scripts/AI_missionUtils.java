package data.missions.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.FullName.Gender;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI.SkillLevelAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.FighterLaunchBayAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.plugins.OfficerLevelupPlugin;
import data.scripts.util.MagicLensFlare;
import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class AI_missionUtils {

    private static final Logger log = Global.getLogger(AI_missionUtils.class);
    private static final Map<Integer, String> MATCH_LETTER = new HashMap<>();

    static {
        MATCH_LETTER.put(0, "A");
        MATCH_LETTER.put(1, "B");
        MATCH_LETTER.put(2, "C");
        MATCH_LETTER.put(3, "D");
        MATCH_LETTER.put(4, "E");
        MATCH_LETTER.put(5, "F");
        MATCH_LETTER.put(6, "G");
        MATCH_LETTER.put(7, "H");
        MATCH_LETTER.put(8, "I");
        MATCH_LETTER.put(9, "J");
        MATCH_LETTER.put(10, "K");
        MATCH_LETTER.put(11, "L");
        MATCH_LETTER.put(12, "M");
        MATCH_LETTER.put(13, "N");
        MATCH_LETTER.put(14, "O");
        MATCH_LETTER.put(15, "P");
        MATCH_LETTER.put(16, "Q");
        MATCH_LETTER.put(17, "R");
        MATCH_LETTER.put(18, "S");
        MATCH_LETTER.put(19, "T");
        MATCH_LETTER.put(20, "U");
        MATCH_LETTER.put(21, "V");
        MATCH_LETTER.put(22, "W");
        MATCH_LETTER.put(23, "X");
        MATCH_LETTER.put(24, "Y");
        MATCH_LETTER.put(25, "Z");
    }

    private static HashMap<HullSize, Integer> mag = new HashMap();

    static {
        mag.put(HullSize.FRIGATE, 10);
        mag.put(HullSize.DESTROYER, 20);
        mag.put(HullSize.CRUISER, 30);
        mag.put(HullSize.CAPITAL_SHIP, 50);
    }         
    
    ////////////////////////////////////
    //                                //
    //         PLAYER CREATION        //
    //                                //
    ////////////////////////////////////
    public static List<String> createPlayerCSV(String PATH, Integer playerNumber) {

        String name = "error";
        String tag = "error";
        String prefix = "error";
        //determine the files to read
        String theData = PATH + "player" + playerNumber + "_data.csv";

        //read playerX_data.csv
        try {
            JSONArray playerData = Global.getSettings().getMergedSpreadsheetDataForMod("name", theData, "aibattles");

            JSONObject row = playerData.getJSONObject(0);
            name = row.getString("name");
            tag = row.getString("tag");
            prefix = row.getString("prefix");

        } catch (IOException | JSONException ex) {
            log.error("unable to read player" + playerNumber + "_data.csv");
        }
        List<String> player = new ArrayList<>();
        player.add(0, name);
        player.add(1, prefix);
        player.add(2, tag);
        return player;
    }

    ////
    // PPVE 2020 player json creation
    ////
    public static List<String> createPlayerJSON(String PATH, Integer playerNumber) {

        String name = "error";
        String tag = "error";
        String prefix = "error";
        //determine the files to read
        String fullPath = PATH + "player" + playerNumber + "_data.json";

        //read playerX_data.json
        try {
            JSONObject playerData = Global.getSettings().loadJSON(fullPath);

            JSONObject row = playerData.getJSONObject("player");
            name = cutoff(row.getString("name"), 24);
            tag = cutoff(row.getString("tag"), 42);
            prefix = cutoff(row.getString("prefix"), 24);

        } catch (IOException | JSONException ex) {
            log.error("unable to read player" + playerNumber + "_data.json");
        }
        List<String> player = new ArrayList<>();
        player.add(0, name);
        player.add(1, prefix);
        player.add(2, tag);
        return player;
    }

    public static Map<String, PersonAPI> createOfficers(String PATH, Integer playerNumber) {

        Map<String, PersonAPI> officers = new HashMap<>();
        final OfficerLevelupPlugin plugin = (OfficerLevelupPlugin) Global.getSettings().getPlugin("officerLevelUp");

        //determine the files to read
        String fullPath = PATH + "player" + playerNumber + "_data.json";

        //read playerX_data.json
        try {
            JSONObject playerData = Global.getSettings().loadJSON(fullPath);

            if (playerData.has("officers")) {

                try {
                    JSONArray seedsJson = playerData.getJSONArray("officers");
                    for (int i = 0; i < seedsJson.length(); i++) {
                        JSONObject officerJson = seedsJson.getJSONObject(i);

                        PersonAPI officer = OfficerManagerEvent.createOfficer(Global.getSector().getFaction(Factions.PLAYER), 1, false);

//                        Random random = new Random(1);
//                        PersonAPI officer = Global.getSector().getFaction(Factions.INDEPENDENT).createRandomPerson(Gender.MALE, random);
                        //optional cusomization
                        if (officerJson.has("portrait") && officerJson.getString("portrait") != null) {
                            officer.setPortraitSprite(officerJson.getString("portrait"));
                        } else {
                            officer.setPortraitSprite("graphics/portraits/portrait_generic.png");
                        }
                        FullName name = officer.getName();
                        if (officerJson.has("firstName") && officerJson.getString("firstName") != null) {
                            name.setFirst(cutoff(officerJson.getString("firstName"), 24));
                        } else {
                            name.setFirst(officerJson.getString("John"));
                        }
                        if (officerJson.has("lastName") && officerJson.getString("lastName") != null) {
                            name.setLast(cutoff(officerJson.getString("lastName"), 24));
                        } else {
                            name.setLast(officerJson.getString("Doe"));
                        }
                        if (officerJson.has("isFemale") && officerJson.getString("isFemale") != null) {
                            if (officerJson.getBoolean("isFemale")) {
                                name.setGender(Gender.FEMALE);
                            } else {
                                name.setGender(Gender.MALE);
                            }
                        } else {
                            name.setGender(Gender.MALE);
                        }
                        officer.setName(name);

                        log.info("Creating officer " + officer.getName().getFirst() + " " + officer.getName().getLast() + " with the following skills:");

                        //actually usefull stuff
                        officer.getStats().setLevel(officerJson.getJSONArray("skills").length());
                        officer.getStats().setPoints(officerJson.getJSONArray("skills").length() * 1);

                        //reset skills
                        for (SkillLevelAPI s : officer.getStats().getSkillsCopy()) {
                            officer.getStats().setSkillLevel(s.getSkill().getId(), 0);
                        }
                        //assign proper ones
                        for (int j = 0; j < officerJson.getJSONArray("skills").length(); j++) {
                            officer.getStats().setSkillLevel(officerJson.getJSONArray("skills").getString(j), 2);
                            log.info(" - " + officerJson.getJSONArray("skills").getString(j) + ": " + officer.getStats().getSkillLevel(officerJson.getJSONArray("skills").getString(j)));
                            //log.info(" - " + officer.getStats().getSkillLevel(officerJson.getJSONArray("skills").getString(j)));
                        }
                        //officer.setPersonality(officerJson.getString("personality"));
                        officer.getStats().refreshCharacterStatsEffects();

                        officers.put(officerJson.getString("ship"), officer);
                    }
                } catch (JSONException ex) {
                    log.error("unable get officers from player" + playerNumber + "_data.json");
                }
            }

        } catch (IOException | JSONException ex) {
            log.error("unable to read player" + playerNumber + "_data.csv");
        }
        return officers;
    }

    ////////////////////////////////////
    //                                //
    //         FLEET CREATION         //
    //                                //
    ////////////////////////////////////
    public static Map<FleetMemberAPI, Boolean> addRoundReturnFlagship(MissionDefinitionAPI api, FleetSide side, Integer ROUND, Integer playerNumber, String PATH, String prefix) {

        String theFleet = PATH + "player" + playerNumber + "_fleet.csv";

        Map<String, PersonAPI> officers = createOfficers(PATH, playerNumber);
        Map<String, Boolean> officerUsed = new HashMap<>();
        for (String officer : officers.keySet()) {
            officerUsed.put(officer, Boolean.FALSE);
        }

        Map<FleetMemberAPI, Boolean> fleet = new HashMap<>();

        //read playerX_fleet.csv
        try {
            JSONArray playerFleet = Global.getSettings().getMergedSpreadsheetDataForMod("rowNumber", theFleet, "aibattles");
            for (int i = 0; i < playerFleet.length(); i++) {
                JSONObject row = playerFleet.getJSONObject(i);
                //only add the ships for the relevant round
                if (row.getInt("round") <= ROUND) {

                    String variant = row.getString("variant");
                    FleetMemberAPI member = api.addToFleet(side, variant, FleetMemberType.SHIP, false);
                    if (!row.getString("shipName").isEmpty()) {
                        String shipName = prefix + " " + row.getString("shipName");
                        member.setShipName(shipName);
                    }
                    member.updateStats();

                    //custom officer
                    if (officers.containsKey(row.getString("shipName"))) {
                        if (!(officerUsed.get(row.getString("shipName")))) {
                            member.setCaptain(officers.get(row.getString("shipName")));
                            member.updateStats();
                            log.info("Captain " + member.getCaptain().getNameString() + " assigned to " + member.getShipName());
                            officerUsed.put(row.getString("shipName"), Boolean.TRUE);
                        } else {
                            log.info("Officer " + member.getCaptain().getNameString() + " is already assigned.");
                        }
                    } else {
                        String personality = row.getString("personality");
                        member.getCaptain().setPersonality(personality);
                    }

                    if (row.getBoolean("isFlagship")) {
                        fleet.put(member, true);
                    } else {
                        fleet.put(member, false);
                    }
                }
            }
        } catch (IOException | JSONException ex) {
            log.error("unable to read player" + playerNumber + "_fleet.csv");
        }

        return fleet;
    }

    ////
    // PPVE 2020 FLEET LOADING
    ////
    public static Map<Integer, FleetMemberData> createFleetReturnRefit(
            MissionDefinitionAPI api,
            FleetSide side,
            Integer ROUND,
            Integer playerNumber,
            String PATH,
            String prefix
    ) {

        String theFleet = PATH + "player" + playerNumber + "_fleet.csv";
        boolean first = Boolean.FALSE; //no flagships this time

        Map<String, PersonAPI> officers = createOfficers(PATH, playerNumber);
        Map<String, Boolean> officerUsed = new HashMap<>();
        for (String officer : officers.keySet()) {
            officerUsed.put(officer, Boolean.FALSE);
        }

        Map<Integer, FleetMemberData> fleet = new HashMap<>();
        int j = 0;
        //read playerX_fleet.csv
        try {
            JSONArray playerFleet = Global.getSettings().loadCSV(theFleet);
            for (int i = 0; i < playerFleet.length(); i++) {
                JSONObject row = playerFleet.getJSONObject(i);
                //only add the ships for the relevant round
                if (row.getInt("aquiredRound") <= ROUND) {

                    String variant = row.getString("variantID");
                    String personality = row.getString("defaultPersonality");

                    //add ship
                    FleetMemberAPI member = api.addToFleet(side, variant, FleetMemberType.SHIP, first);
                    if (first) {
                        first = Boolean.FALSE;
                    }

                    if (!row.getString("shipName").isEmpty()) {
                        String shipName = prefix + " " + cutoff(row.getString("shipName"), 24);
                        member.setShipName(shipName);
                    }
                    member.getVariant().setVariantDisplayName(cutoff(member.getVariant().getDisplayName(), 24));

                    //custom officer
                    if (officers.containsKey(row.getString("shipName"))) {
                        if (!(officerUsed.get(row.getString("shipName")))) {
                            PersonAPI officer = officers.get(row.getString("shipName"));
                            if (personality.matches("timid|cautious|steady|aggressive|reckless")) {
                                officer.setPersonality(personality);
                            } else {
                                log.info("Personality \"" + personality + "\" for " + member.getShipName() + " (" + member.getHullId() + ") not found");
                                officer.setPersonality(Personalities.STEADY);
                            }
                            member.setCaptain(officer);
                            log.info(" ");
                            log.info("Captain " + member.getCaptain().getNameString() + " assigned to " + member.getShipName());
                            log.info("Personality: " + member.getCaptain().getPersonalityAPI().getDisplayName());
                            for (SkillLevelAPI s : member.getCaptain().getStats().getSkillsCopy()) {
                                //                            log.info("skill: "+s.getSkill().getId()+" level: "+s.getLevel());
                            }
                            officerUsed.put(row.getString("shipName"), Boolean.TRUE);
                        } else {
                            log.info("Officer " + officers.get(row.getString("shipName")) + " is already assigned.");
                        }
                    } else {
                        member.getCaptain().setPersonality(personality);
                    }

                    int round = row.getInt("aquiredRound");
                    int refit = row.getInt("refittedRound");

                    member.updateStats();

                    fleet.put(j, new FleetMemberData(member, round, refit));
                    j++;
                }
            }
        } catch (IOException | JSONException ex) {
            log.error("unable to read player" + playerNumber + "_fleet.csv");
        }

        return fleet;
    }

    ////////////////////////////////////
    //                                //
    //     BATTLESPACE CREATION       //
    //                                //
    ////////////////////////////////////
    /**
     * @param api Mission api
     *
     * @param map Map index
     */
    public static void setBattlescape(MissionDefinitionAPI api, Integer map) {

        float WIDTH, HEIGHT;
        //select the relevant battlescape
        switch (map) {
            case 1:
                WIDTH = 8000;
                HEIGHT = 8000;
                api.initMap(-WIDTH / 2, WIDTH / 2, -HEIGHT / 2, HEIGHT / 2);
                //add objectives
                //public void addObjective(float x, float y, String type)
                //"sensor_array", "nav_buoy", "comm_relay"
                api.addObjective(0, 0, "comm_relay");
                //add nebulaes
                //public void addNebula(float x, float y, float radius)
                api.addNebula(0.25f * WIDTH, 0.25f * HEIGHT, 500);
                api.addNebula(-0.25f * WIDTH, 0.25f * HEIGHT, 500);
                api.addNebula(0.25f * WIDTH, -0.25f * HEIGHT, 500);
                api.addNebula(-0.25f * WIDTH, -0.25f * HEIGHT, 250);
                //add asteroids
                //public void addAsteroidField(float x, float y, float angle, float width, float minSpeed, float maxSpeed, int quantity)
                api.addAsteroidField(0, 0, 0, 500, 10, 15, 15);
                api.setBackgroundSpriteName("graphics/AIB/backgrounds/AIB_bg1.jpg");
                break;
            case 2:
                WIDTH = 12000;
                HEIGHT = 8000;
                api.initMap(-WIDTH / 2, WIDTH / 2, -HEIGHT / 2, HEIGHT / 2);
                //add objectives
                //public void addObjective(float x, float y, String type)
                //"sensor_array", "nav_buoy", "comm_relay"
                api.addObjective(-0.25f * WIDTH, 0.05f * HEIGHT, "sensor_array");
                api.addObjective(0.25f * WIDTH, -0.05f * HEIGHT, "nav_buoy");
                api.addObjective(0.25f * WIDTH, 0.05f * HEIGHT, "sensor_array");
                api.addObjective(-0.25f * WIDTH, -0.05f * HEIGHT, "nav_buoy");
                api.addObjective(-0.25f * WIDTH, 0, "comm_relay");
                api.addObjective(0.25f * WIDTH, 0, "comm_relay");
                //add asteroids
                //public void addAsteroidField(float x, float y, float angle, float width, float minSpeed, float maxSpeed, int quantity)
                api.addAsteroidField(-0.4f * WIDTH, 0, 90, 1000, 20, 25, 40);
                api.addAsteroidField(0.4f * WIDTH, 0, -90, 1000, 20, 25, 40);
                api.setBackgroundSpriteName("graphics/AIB/backgrounds/AIB_bg2.jpg");
                break;
            case 3:
                WIDTH = 12000;
                HEIGHT = 9000;
                api.initMap(-WIDTH / 2, WIDTH / 2, -HEIGHT / 2, HEIGHT / 2);
                //add objectives
                //public void addObjective(float x, float y, String type)
                //"sensor_array", "nav_buoy", "comm_relay"
                api.addObjective(-0.25f * WIDTH, -0f * HEIGHT, "comm_relay");
                api.addObjective(0.25f * WIDTH, 0f * HEIGHT, "comm_relay");
                api.addObjective(-0.25f * WIDTH, 0.05f * HEIGHT, "nav_buoy");
                api.addObjective(-0.25f * WIDTH, -0.05f * HEIGHT, "nav_buoy");
                api.addObjective(-0.3f * WIDTH, 0f * HEIGHT, "nav_buoy");
                api.addObjective(0.25f * WIDTH, 0.05f * HEIGHT, "sensor_array");
                api.addObjective(0.25f * WIDTH, -0.05f * HEIGHT, "sensor_array");
                api.addObjective(0.3f * WIDTH, 0f * HEIGHT, "sensor_array");
                //add nebulaes
                api.addNebula(0.3f * WIDTH, 0.4f * HEIGHT, 600);
                api.addNebula(-0.3f * WIDTH, 0.4f * HEIGHT, 600);
                api.addNebula(0.3f * WIDTH, -0.4f * HEIGHT, 600);
                api.addNebula(-0.3f * WIDTH, -0.4f * HEIGHT, 600);

                api.addNebula(0.3f * WIDTH, 0.2f * HEIGHT, 1000);
                api.addNebula(-0.3f * WIDTH, 0.2f * HEIGHT, 1000);
                api.addNebula(0.3f * WIDTH, -0.2f * HEIGHT, 1000);
                api.addNebula(-0.3f * WIDTH, -0.2f * HEIGHT, 1000);

                api.addNebula(0.3f * WIDTH, 0, 1500);
                api.addNebula(-0.3f * WIDTH, 0, 1500);
                api.setBackgroundSpriteName("graphics/AIB/backgrounds/AIB_bg3.jpg");
                break;
            case 4:
                WIDTH = 12000;
                HEIGHT = 12000;
                api.initMap(-WIDTH / 2, WIDTH / 2, -HEIGHT / 2, HEIGHT / 2);
                //add objectives
                //public void addObjective(float x, float y, String type)
                //"sensor_array", "nav_buoy", "comm_relay"
//                api.addObjective(-0.05f * WIDTH, 0.05f * HEIGHT, "sensor_array");
//                api.addObjective(0.05f * WIDTH, 0.05f * HEIGHT, "nav_buoy");
//                api.addObjective(0.05f * WIDTH, -0.05f * HEIGHT, "sensor_array");
//                api.addObjective(-0.05f * WIDTH, -0.05f * HEIGHT, "nav_buoy");
                api.addObjective(0, -0.05f * HEIGHT, "comm_relay");
                api.addObjective(0, 0.05f * HEIGHT, "comm_relay");
                //add nebulaes
                //public void addNebula(float x, float y, float radius)
                for (int x = 0; x < 10; x++) {
                    Vector2f pos = MathUtils.getRandomPointInCircle(new Vector2f(), 3000);
                    float rad = ((float) Math.random() + 0.5f) * 400;
                    api.addNebula(pos.x, pos.y, rad);
                }
                api.setBackgroundSpriteName("graphics/AIB/backgrounds/AIB_bg4.jpg");
                break;
            case 5:
                WIDTH = 10000;
                HEIGHT = 12000;
                api.initMap(-WIDTH / 2, WIDTH / 2, -HEIGHT / 2, HEIGHT / 2);
                //add objectives
                //public void addObjective(float x, float y, String type)
                //"sensor_array", "nav_buoy", "comm_relay"
                api.addObjective(0, -0.25f * HEIGHT, "comm_relay");
                api.addObjective(0, 0.25f * HEIGHT, "comm_relay");
                api.addObjective(-0.05f * WIDTH, -0.25f * HEIGHT, "nav_buoy");
                api.addObjective(0.05f * WIDTH, 0.25f * HEIGHT, "nav_buoy");
                api.addObjective(-0.05f * WIDTH, 0.25f * HEIGHT, "sensor_array");
                api.addObjective(0.05f * WIDTH, -0.25f * HEIGHT, "sensor_array");
                //add nebulaes
                //public void addNebula(float x, float y, float radius)
                api.addNebula(0.05f * WIDTH, 0.3f * HEIGHT, 500);
                api.addNebula(-0.05f * WIDTH, 0.3f * HEIGHT, 500);
                api.addNebula(0.05f * WIDTH, -0.3f * HEIGHT, 500);
                api.addNebula(-0.05f * WIDTH, -0.3f * HEIGHT, 500);
                api.setBackgroundSpriteName("graphics/AIB/backgrounds/AIB_bg5.jpg");
                break;

            //side spawning maps
            case 11:
                WIDTH = 10000;
                HEIGHT = 10000;
                api.initMap(-WIDTH / 2, WIDTH / 2, -HEIGHT / 2, HEIGHT / 2);
                //add objectives
                //public void addObjective(float x, float y, String type)
                //"sensor_array", "nav_buoy", "comm_relay"
                api.addObjective(0, 0, "comm_relay");
                api.addObjective(-0.25f * WIDTH, -0.25f * HEIGHT, "nav_buoy");
                api.addObjective(0.25f * WIDTH, 0.25f * HEIGHT, "nav_buoy");
                api.addObjective(-0.25f * WIDTH, 0.25f * HEIGHT, "sensor_array");
                api.addObjective(0.25f * WIDTH, -0.25f * HEIGHT, "sensor_array");
                //add nebulaes
                //public void addNebula(float x, float y, float radius)
                api.addNebula(-0.25f * WIDTH, -0.25f * HEIGHT, 500);
                api.addNebula(0.25f * WIDTH, 0.25f * HEIGHT, 500);
                api.addNebula(-0.25f * WIDTH, 0.25f * HEIGHT, 500);
                api.addNebula(0.25f * WIDTH, -0.25f * HEIGHT, 500);
                api.setBackgroundSpriteName("graphics/AIB/backgrounds/AIB_bg6.jpg");
                break;

            case 12:
                WIDTH = 12000;
                HEIGHT = 12000;
                api.initMap(-WIDTH / 2, WIDTH / 2, -HEIGHT / 2, HEIGHT / 2);
                //add objectives
                //public void addObjective(float x, float y, String type)
                //"sensor_array", "nav_buoy", "comm_relay"
                api.addObjective(-0.15f * WIDTH, 0.1f * HEIGHT, "nav_buoy");
                api.addObjective(0.15f * WIDTH, -0.1f * HEIGHT, "nav_buoy");
                api.addObjective(-0.15f * WIDTH, -0.1f * HEIGHT, "sensor_array");
                api.addObjective(0.15f * WIDTH, 0.1f * HEIGHT, "sensor_array");
                api.addObjective(0, 0.15f * HEIGHT, "comm_relay");
                api.addObjective(0, -0.15f * HEIGHT, "comm_relay");

                for (int x = 0; x < 5; x++) {
                    Vector2f pos = MathUtils.getRandomPointInCircle(new Vector2f(-0.5f * WIDTH, -0.5f * HEIGHT), 4000);
                    float rad = ((float) Math.random() + 0.5f) * 400;
                    api.addNebula(pos.x, pos.y, rad);
                }
                for (int x = 0; x < 5; x++) {
                    Vector2f pos = MathUtils.getRandomPointInCircle(new Vector2f(0.5f * WIDTH, -0.5f * HEIGHT), 4000);
                    float rad = ((float) Math.random() + 0.5f) * 400;
                    api.addNebula(pos.x, pos.y, rad);
                }
                for (int x = 0; x < 5; x++) {
                    Vector2f pos = MathUtils.getRandomPointInCircle(new Vector2f(0.5f * WIDTH, 0.5f * HEIGHT), 4000);
                    float rad = ((float) Math.random() + 0.5f) * 400;
                    api.addNebula(pos.x, pos.y, rad);
                }
                for (int x = 0; x < 5; x++) {
                    Vector2f pos = MathUtils.getRandomPointInCircle(new Vector2f(-0.5f * WIDTH, 0.5f * HEIGHT), 4000);
                    float rad = ((float) Math.random() + 0.5f) * 400;
                    api.addNebula(pos.x, pos.y, rad);
                }
                //add nebulaes
                //public void addNebula(float x, float y, float radius)
//                api.addNebula(-0.25f*WIDTH, -0.25f*HEIGHT, 500);
//                api.addNebula(0.25f*WIDTH, 0.25f*HEIGHT, 500);
//                api.addNebula(-0.25f*WIDTH, 0.25f*HEIGHT, 500);
//                api.addNebula(0.25f*WIDTH, -0.25f*HEIGHT, 500);
//                api.setBackgroundSpriteName("graphics/AIB/backgrounds/AIB_bg7.jpg");
                break;

            case 13:
                WIDTH = 12000;
                HEIGHT = 12000;
                api.initMap(-WIDTH / 2, WIDTH / 2, -HEIGHT / 2, HEIGHT / 2);
                //add objectives
                //public void addObjective(float x, float y, String type)
                //"sensor_array", "nav_buoy", "comm_relay"
                //api.addObjective(0, 0, "comm_relay");
                api.addObjective(-0.1f * WIDTH, 0.1f * HEIGHT, "nav_buoy");
                api.addObjective(0.1f * WIDTH, -0.1f * HEIGHT, "nav_buoy");
                api.addObjective(-0.1f * WIDTH, -0.1f * HEIGHT, "sensor_array");
                api.addObjective(0.1f * WIDTH, 0.1f * HEIGHT, "sensor_array");

                //add nebulaes
                //public void addNebula(float x, float y, float radius)
                for (int x = 0; x < 5; x++) {
                    Vector2f pos = MathUtils.getRandomPointInCircle(new Vector2f(-0.5f * WIDTH, -0.5f * HEIGHT), 4000);
                    float rad = ((float) Math.random() + 0.5f) * 400;
                    api.addNebula(pos.x, pos.y, rad);
                }
                for (int x = 0; x < 5; x++) {
                    Vector2f pos = MathUtils.getRandomPointInCircle(new Vector2f(0.5f * WIDTH, -0.5f * HEIGHT), 4000);
                    float rad = ((float) Math.random() + 0.5f) * 400;
                    api.addNebula(pos.x, pos.y, rad);
                }
                for (int x = 0; x < 5; x++) {
                    Vector2f pos = MathUtils.getRandomPointInCircle(new Vector2f(0.5f * WIDTH, 0.5f * HEIGHT), 4000);
                    float rad = ((float) Math.random() + 0.5f) * 400;
                    api.addNebula(pos.x, pos.y, rad);
                }
                for (int x = 0; x < 5; x++) {
                    Vector2f pos = MathUtils.getRandomPointInCircle(new Vector2f(-0.5f * WIDTH, 0.5f * HEIGHT), 4000);
                    float rad = ((float) Math.random() + 0.5f) * 400;
                    api.addNebula(pos.x, pos.y, rad);
                }
                api.addNebula(-0.5f * WIDTH, -0.5f * HEIGHT, 1000);
                api.addNebula(0.5f * WIDTH, 0.5f * HEIGHT, 1000);
                api.addNebula(-0.5f * WIDTH, 0.5f * HEIGHT, 1000);
                api.addNebula(0.5f * WIDTH, -0.5f * HEIGHT, 1000);

//                api.addAsteroidField(-0.4f * WIDTH, 0.4f, 90, 1000, 20, 25, 40);
//                api.addAsteroidField(0.4f * WIDTH, 0.4f, -90, 1000, 20, 25, 40);
//                api.addAsteroidField(-0.4f * WIDTH, -0.4f, 90, 1000, 20, 25, 40);
//                api.addAsteroidField(0.4f * WIDTH, -0.4f, -90, 1000, 20, 25, 40);
                api.setBackgroundSpriteName("graphics/AIB/backgrounds/AIB_bg8.jpg");
                break;

            case 14:
                WIDTH = 16000;
                HEIGHT = 13000;
                api.initMap(-WIDTH / 2, WIDTH / 2, -HEIGHT / 2, HEIGHT / 2);
                //add objectives
                //public void addObjective(float x, float y, String type)
                //"sensor_array", "nav_buoy", "comm_relay"
                //api.addObjective(0, 0, "comm_relay");
                api.addObjective(-0.05f * WIDTH, 0.15f * HEIGHT, "nav_buoy");
                api.addObjective(0.05f * WIDTH, 0.15f * HEIGHT, "sensor_array");
                api.addObjective(-0.05f * WIDTH, -0.15f * HEIGHT, "sensor_array");
                api.addObjective(0.05f * WIDTH, -0.15f * HEIGHT, "nav_buoy");
                //add nebulaes
                //public void addNebula(float x, float y, float radius)
//                api.addNebula(-0.1f*WIDTH, -0.1f*HEIGHT, 500);
//                api.addNebula(0.1f*WIDTH, 0.1f*HEIGHT, 500);
//                api.addNebula(-0.1f*WIDTH, 0.1f*HEIGHT, 500);
//                api.addNebula(0.1f*WIDTH, -0.1f*HEIGHT, 500);
                api.setBackgroundSpriteName("graphics/AIB/backgrounds/AIB_bg9.jpg");
                break;

            case 15:
                WIDTH = 12000;
                HEIGHT = 12000;
                api.initMap(-WIDTH / 2, WIDTH / 2, -HEIGHT / 2, HEIGHT / 2);
                //add objectives
                //public void addObjective(float x, float y, String type)
                //"sensor_array", "nav_buoy", "comm_relay"
                //api.addObjective(0, 0, "comm_relay");
                api.addObjective(-0.15f * WIDTH, 0.0f * HEIGHT, "nav_buoy");
                api.addObjective(-0.05f * WIDTH, 0.0f * HEIGHT, "sensor_array");
                api.addObjective(0.05f * WIDTH, 0.0f * HEIGHT, "sensor_array");
                api.addObjective(0.15f * WIDTH, 0.0f * HEIGHT, "nav_buoy");
                //add nebulaes
                //public void addNebula(float x, float y, float radius)
//                api.addNebula(-0.1f*WIDTH, -0.1f*HEIGHT, 500);
//                api.addNebula(0.1f*WIDTH, 0.1f*HEIGHT, 500);
//                api.addNebula(-0.1f*WIDTH, 0.1f*HEIGHT, 500);
//                api.addNebula(0.1f*WIDTH, -0.1f*HEIGHT, 500);
                break;

            // ADDED BY RUBIN
            //ships crash into each other with only 1 objective per "row"
            case 20:
                WIDTH = 10000;
                HEIGHT = 8000;
                api.initMap(-WIDTH / 2, WIDTH / 2, -HEIGHT / 2, HEIGHT / 2);
                //add objectives
                //public void addObjective(float x, float y, String type)
                //"sensor_array", "nav_buoy", "comm_relay"
                api.addObjective(0, 0, "comm_relay");
                //api.addObjective(-0.1f*WIDTH, -0.3f*HEIGHT, "sensor_array");
                //api.addObjective(0.1f*WIDTH, -0.3f*HEIGHT, "sensor_array");
                //api.addObjective(-0.1f*WIDTH, 0.3f*HEIGHT, "sensor_array");
                //api.addObjective(0.1f*WIDTH, 0.3f*HEIGHT, "sensor_array");
                api.addObjective(0, -0.3f * HEIGHT, "sensor_array");
                api.addObjective(0, 0.3f * HEIGHT, "sensor_array");
                //api.addObjective(-0.1f*WIDTH, -0.15f*HEIGHT, "nav_buoy");
                //api.addObjective(0.1f*WIDTH, -0.15f*HEIGHT, "nav_buoy");
                //api.addObjective(-0.1f*WIDTH, 0.15f*HEIGHT, "nav_buoy");
                //api.addObjective(0.1f*WIDTH, 0.15f*HEIGHT, "nav_buoy");
                api.addObjective(0, -0.15f * HEIGHT, "nav_buoy");
                api.addObjective(0, 0.15f * HEIGHT, "nav_buoy");
                break;

            //too many objectives, too small a map
            case 21:
                WIDTH = 10000;
                HEIGHT = 8000;
                api.initMap(-WIDTH / 2, WIDTH / 2, -HEIGHT / 2, HEIGHT / 2);
                //add objectives
                //public void addObjective(float x, float y, String type)
                //"sensor_array", "nav_buoy", "comm_relay"
                api.addObjective(0, 0, "comm_relay");
                api.addObjective(-0.05f * WIDTH, -0.25f * HEIGHT, "sensor_array");
                api.addObjective(0.05f * WIDTH, -0.25f * HEIGHT, "sensor_array");
                api.addObjective(-0.05f * WIDTH, 0.25f * HEIGHT, "sensor_array");
                api.addObjective(0.05f * WIDTH, 0.25f * HEIGHT, "sensor_array");
                api.addObjective(-0.05f * WIDTH, -0.1f * HEIGHT, "nav_buoy");
                api.addObjective(0.05f * WIDTH, -0.1f * HEIGHT, "nav_buoy");
                api.addObjective(-0.05f * WIDTH, 0.1f * HEIGHT, "nav_buoy");
                api.addObjective(0.05f * WIDTH, 0.1f * HEIGHT, "nav_buoy");
                break;

            //too many objectives
            case 22:
                WIDTH = 10000;
                HEIGHT = 12000;
                api.initMap(-WIDTH / 2, WIDTH / 2, -HEIGHT / 2, HEIGHT / 2);
                //add objectives
                //public void addObjective(float x, float y, String type)
                //"sensor_array", "nav_buoy", "comm_relay"
                api.addObjective(0, 0, "comm_relay");
                api.addObjective(-0.05f * WIDTH, -0.25f * HEIGHT, "sensor_array");
                api.addObjective(0.05f * WIDTH, -0.25f * HEIGHT, "sensor_array");
                api.addObjective(-0.05f * WIDTH, 0.25f * HEIGHT, "sensor_array");
                api.addObjective(0.05f * WIDTH, 0.25f * HEIGHT, "sensor_array");
                api.addObjective(-0.05f * WIDTH, -0.1f * HEIGHT, "nav_buoy");
                api.addObjective(0.05f * WIDTH, -0.1f * HEIGHT, "nav_buoy");
                api.addObjective(-0.05f * WIDTH, 0.1f * HEIGHT, "nav_buoy");
                api.addObjective(0.05f * WIDTH, 0.1f * HEIGHT, "nav_buoy");
                break;

            //too many sensor arrays, too large?
            case 23:
                WIDTH = 12000;
                HEIGHT = 12000;
                api.initMap(-WIDTH / 2, WIDTH / 2, -HEIGHT / 2, HEIGHT / 2);
                //add objectives
                //public void addObjective(float x, float y, String type)
                //"sensor_array", "nav_buoy", "comm_relay"
                //api.addObjective(0, 0, "comm_relay");
                api.addObjective(-0.2f * WIDTH, 0.0f * HEIGHT, "nav_buoy");
                api.addObjective(0.2f * WIDTH, 0.0f * HEIGHT, "nav_buoy");
                api.addObjective(0.0f * WIDTH, 0.0f * HEIGHT, "sensor_array");
                api.addObjective(-0.1f * WIDTH, (float) (-Math.sqrt(3) / 10 * HEIGHT), "sensor_array");
                api.addObjective(0.1f * WIDTH, (float) (-Math.sqrt(3) / 10 * HEIGHT), "sensor_array");
                api.addObjective(-0.1f * WIDTH, (float) (Math.sqrt(3) / 10 * HEIGHT), "sensor_array");
                api.addObjective(0.1f * WIDTH, (float) (Math.sqrt(3) / 10 * HEIGHT), "sensor_array");
                break;

            case 24:
                WIDTH = 10000;
                HEIGHT = 10000;
                api.initMap(-WIDTH / 2, WIDTH / 2, -HEIGHT / 2, HEIGHT / 2);
                //add objectives
                //public void addObjective(float x, float y, String type)
                //"sensor_array", "nav_buoy", "comm_relay"
                //api.addObjective(0, 0, "comm_relay");
                api.addObjective(-0.2f * WIDTH, 0.0f * HEIGHT, "nav_buoy");
                api.addObjective(0.2f * WIDTH, 0.0f * HEIGHT, "nav_buoy");
                api.addObjective(0.0f * WIDTH, 0.0f * HEIGHT, "sensor_array");
                api.addObjective(-0.0f * WIDTH, -0.2f * HEIGHT, "sensor_array");
                api.addObjective(0.0f * WIDTH, 0.2f * HEIGHT, "sensor_array");
                break;

            //THE AI IS NOTICEABLY WORSE WHEN EITHER A) TOO MANY OBJECTIVES, OR B) OBJECTIVES IN TOO MANY "ROW"S
            case 25:
                WIDTH = 12000;
                HEIGHT = 12000;
                api.initMap(-WIDTH / 2, WIDTH / 2, -HEIGHT / 2, HEIGHT / 2);
                //add objectives
                //public void addObjective(float x, float y, String type)
                //"sensor_array", "nav_buoy", "comm_relay"
                //api.addObjective(0, 0, "comm_relay");
                api.addObjective(-0.15f * WIDTH, 0.0f * HEIGHT, "nav_buoy");
                api.addObjective(0.15f * WIDTH, 0.0f * HEIGHT, "nav_buoy");
                api.addObjective(0.0f * WIDTH, 0.0f * HEIGHT, "sensor_array");
                api.addObjective(-0.0f * WIDTH, -0.15f * HEIGHT, "sensor_array");
                api.addObjective(0.0f * WIDTH, 0.15f * HEIGHT, "sensor_array");
                api.addObjective(-0.1f * WIDTH, -0.15f * HEIGHT, "comm_relay");
                api.addObjective(0.1f * WIDTH, -0.15f * HEIGHT, "comm_relay");
                api.addObjective(-0.1f * WIDTH, 0.15f * HEIGHT, "comm_relay");
                api.addObjective(0.1f * WIDTH, 0.15f * HEIGHT, "comm_relay");
                break;

            case 26:
                WIDTH = 12000;
                HEIGHT = 10000;
                api.initMap(-WIDTH / 2, WIDTH / 2, -HEIGHT / 2, HEIGHT / 2);
                //add objectives
                //public void addObjective(float x, float y, String type)
                //"sensor_array", "nav_buoy", "comm_relay"
                //api.addObjective(0, 0, "comm_relay");
                api.addObjective(-0.15f * WIDTH, -0.1f * HEIGHT, "nav_buoy");
                api.addObjective(0.15f * WIDTH, -0.1f * HEIGHT, "nav_buoy");
                api.addObjective(0.0f * WIDTH, -0.1f * HEIGHT, "sensor_array");
                api.addObjective(-0.15f * WIDTH, 0.1f * HEIGHT, "nav_buoy");
                api.addObjective(0.15f * WIDTH, 0.1f * HEIGHT, "nav_buoy");
                api.addObjective(0.0f * WIDTH, 0.1f * HEIGHT, "sensor_array");
                break;

            case 27:
                WIDTH = 12000;
                HEIGHT = 10000;
                api.initMap(-WIDTH / 2, WIDTH / 2, -HEIGHT / 2, HEIGHT / 2);
                //add objectives
                //public void addObjective(float x, float y, String type)
                //"sensor_array", "nav_buoy", "comm_relay"
                //api.addObjective(0, 0, "comm_relay");
                api.addObjective(-0.125f * WIDTH, 0.0f * HEIGHT, "nav_buoy");
                api.addObjective(0.125f * WIDTH, 0.0f * HEIGHT, "nav_buoy");
                api.addObjective(0.0f * WIDTH, 0.0f * HEIGHT, "sensor_array");
                break;

            case 28:
                WIDTH = 12000;
                HEIGHT = 12000;
                api.initMap(-WIDTH / 2, WIDTH / 2, -HEIGHT / 2, HEIGHT / 2);
                //add objectives
                //public void addObjective(float x, float y, String type)
                //"sensor_array", "nav_buoy", "comm_relay"
                //api.addObjective(0, 0, "comm_relay");
                api.addObjective(-0.125f * WIDTH, 0.0f * HEIGHT, "nav_buoy");
                api.addObjective(0.125f * WIDTH, 0.0f * HEIGHT, "nav_buoy");
                api.addObjective(0.0f * WIDTH, 0.0f * HEIGHT, "sensor_array");
                break;

            case 29:
                WIDTH = 8000;
                HEIGHT = 8000;
                api.initMap(-WIDTH / 2, WIDTH / 2, -HEIGHT / 2, HEIGHT / 2);
                //add objectives
                //public void addObjective(float x, float y, String type)
                //"sensor_array", "nav_buoy", "comm_relay"
                api.addObjective(0.0f * WIDTH, 0.0f * HEIGHT, "comm_relay");
                break;
        }
    }

    ////////////////////////////////////
    //                                //
    //  2020 PvP DP COST CALCULATION  //
    //                                //
    ////////////////////////////////////
    //Now uses FleetMemberData from the 2020 PPvE Tournament
    public static int DPFleetCostWithMemberData(CombatEngineAPI engine,
            String name,
            Map<Integer, FleetMemberData> FLEET,
            boolean BLACKLIST,
            List<String> BLOCKED) {   
        
        log.info(" ");
        log.info(" ");
        log.info("____________________________");
        log.info("Computing DP Cost of Fleet");
        log.info("____________________________");
        log.info(" ");
        log.info(" ");

        float totalDP = 0;

        for (Map.Entry e : FLEET.entrySet()) {

            FleetMemberAPI s = ((FleetMemberData) e.getValue()).MEMBER;

            //Check blacklist items
            if (BLACKLIST) {
                //hull
                if (BLOCKED.contains(s.getHullSpec().getBaseHullId())) {
                    log.error("BLACKLISTED HULL DETECTED:" + s.getHullSpec().getHullName());
                    //return negative value for error
                    return -1;
                }
                //weapons
                for (String slot : s.getVariant().getNonBuiltInWeaponSlots()) {
                    if (BLOCKED.contains(s.getVariant().getWeaponId(slot))) {
                        log.error("BLACKLISTED WEAPON DETECTED:" + s.getVariant().getWeaponId(slot) + " on " + s.getShipName());
                        //return negative value for error
                        return -2;
                    }
                }
                //wings
                for (String wing : s.getVariant().getNonBuiltInWings()) {
                    if (BLOCKED.contains(wing)) {
                        log.error("BLACKLISTED WING DETECTED:" + wing + " on " + s.getShipName());
                        //return negative value for error
                        return -3;
                    }
                }
                //hullmods
                for (String mod : s.getVariant().getNonBuiltInHullmods()) {
                    if (BLOCKED.contains(mod)) {
                        log.error("BLACKLISTED HULLMOD DETECTED:" + mod + " on " + s.getShipName());
                        //return negative value for error
                        return -4;
                    }
                }
            }//End Blacklist Check
            
            //Check for invalid variants
            if (s.getVariant().getUnusedOP(null) < 0) {
                log.error("OVER OP LIMIT: " + s.getVariant().getUnusedOP(null)*-1 + " on " + s.getShipName());
                return -5;
            }
            if (s.getVariant().getNumFluxCapacitors() - mag.get(s.getHullSpec().getHullSize()) > 0) {
                log.warn("OVER DEFAULT CAPS LIMIT: " + (s.getVariant().getNumFluxCapacitors() - mag.get(s.getHullSpec().getHullSize())) + " on " + s.getShipName());
                return -6;
            }
            if (s.getVariant().getNumFluxVents() - mag.get(s.getHullSpec().getHullSize()) > 0) {
                log.warn("OVER DEFAULT VENTS LIMIT: " + (s.getVariant().getNumFluxVents() - mag.get(s.getHullSpec().getHullSize())) + " on " + s.getShipName());
                return -7;
            }
            
            //DP cost
            float hull_dp = s.getDeploymentPointsCost();
            log.info("      " + s.getVariant().getHullVariantId() + "'s base DP value: " + (int) hull_dp);
            log.info("____________");

            totalDP += hull_dp;
        }
        log.info(name + "'s Total DP Cost: " + (int) totalDP);
        return (int) totalDP;
    }

    ;
    
    
    ////////////////////////////////////
    //                                //
    //   2020 PPVE COST CALCULATION   //
    //                                //
    ////////////////////////////////////
    //Similar rules as regular fleet cost except both maintenance for regular ships and the Flagships pay half price for their weapons and wings.
    /**
     *
     * @param engine Combat Engine
     *
     * @param PLAYER Player name
     * 
     * @param ROUND current round
     *
     * @param FLEET List of FleetMember with their bought/refit round vector2f
     *
     * @param HULLMOD IF Hullmods have to be bought (once per fleet)
     *
     * @param ANTISPAM If ANTI-SPAM rules apply
     *
     * @param HIKE Price increase per exceeding hull
     *
     * @param SPAM_THRESHOLD Map of the price hike thresholds, set to high
     * values to ignore specific sizes
     *
     * @param BLACKLIST If BLACKLIST is applied
     *
     * @param BLOCKED List of all blocked IDs
     *
     * @param MAINTENANCE If MAINTENANCE rules apply
     *
     * @param SUPPLIES Supplies cost
     *
     * @return
     */
    public static Vector2f splitFleetCost(
            CombatEngineAPI engine,
            String PLAYER,
            Integer ROUND,
            Map<Integer, FleetMemberData> FLEET,
            boolean HULLMOD,
            boolean ANTISPAM, float HIKE, Map<HullSize, Float> SPAM_THRESHOLD,
            boolean BLACKLIST, List<String> BLOCKED,
            boolean MAINTENANCE, Integer SUPPLIES) {

        log.info(" ");
        log.info(" ");
        log.info("____________________________");
        log.info("____________________________");
        log.info(" ");
        log.info(" ");

        log.info("Antispam rule set to: " + ANTISPAM);
        if (ANTISPAM) {
            log.info("Price Hike: " + HIKE * 100 + "% per exceeding hull.");

            log.info("Thresholds: " + Math.round(SPAM_THRESHOLD.get(HullSize.FIGHTER))
                    + "/" + Math.round(SPAM_THRESHOLD.get(HullSize.FRIGATE))
                    + "/" + Math.round(SPAM_THRESHOLD.get(HullSize.DESTROYER))
                    + "/" + Math.round(SPAM_THRESHOLD.get(HullSize.CRUISER))
                    + "/" + Math.round(SPAM_THRESHOLD.get(HullSize.CAPITAL_SHIP)));
        }

        log.info("Maintenance rule set to: " + MAINTENANCE);
        if (MAINTENANCE) {
            log.info("Supplies price: " + SUPPLIES + " credits");
        }

        log.info("Hullmod rule set to: " + HULLMOD);

        log.info(" ");
        log.info(" ");
        log.info("____________________________");
        log.info(" ");
        log.info(PLAYER + "'s budget breakdown");
        log.info("____________________________");
        log.info(" ");

        float totalShipValue = 0;
        float totalEquipmentValue = 0;
        float maintenanceNext = 0;

        /*
         anti-spam map:
         KEY: HullSpec
         VALUE: Vector2f
         X=total number of hulls of the KEY type
         Y=number of hulls bought this round that can get the price hike
         */
        Map<ShipHullSpecAPI, Vector2f> HULL_LIST = new HashMap<>();
        Map<FighterWingSpecAPI, Vector2f> WING_LIST = new HashMap<>();
        List<String> HULLMOD_LIST = new ArrayList<>();

        log.info(" ");
        log.info("SHIPS:");
        log.info(" ");

        for (Map.Entry e : FLEET.entrySet()) {

            FleetMemberAPI s = ((FleetMemberData) e.getValue()).MEMBER;
            Vector2f r = new Vector2f(((FleetMemberData) e.getValue()).BOUGHT, ((FleetMemberData) e.getValue()).REFIT);

            //skipping variants for future rounds
            if (r.x > ROUND || r.y > ROUND) {
                log.error(s.getHullSpec().getHullName() + " " + s.getVariant().getDisplayName() + " is designed for a future round, skipping.");
            }

            log.info(" ");
            log.info(" Checking " + s.getVariant().getHullVariantId());
            log.info(s.getShipName() + "");
            log.info(s.getHullSpec().getHullName() + " " + s.getVariant().getDisplayName());

            //check for blocked content
            if (BLACKLIST) {
                //hull
                if (BLOCKED.contains(s.getHullSpec().getBaseHullId())) {
                    log.error("BLACKLISTED HULL DETECTED:" + s.getHullSpec().getHullName());
                    log.error("BLACKLISTED HULL DETECTED:" + s.getHullSpec().getHullName());
                    log.error("BLACKLISTED HULL DETECTED:" + s.getHullSpec().getHullName());
                    //return negative value for error
                    return new Vector2f(-1, -1);
                }
                //weapons
                for (String slot : s.getVariant().getNonBuiltInWeaponSlots()) {
                    if (BLOCKED.contains(s.getVariant().getWeaponId(slot))) {
                        log.error("BLACKLISTED WEAPON DETECTED:" + s.getVariant().getWeaponId(slot));
                        log.error("BLACKLISTED WEAPON DETECTED:" + s.getVariant().getWeaponId(slot));
                        log.error("BLACKLISTED WEAPON DETECTED:" + s.getVariant().getWeaponId(slot));
                        //return negative value for error
                        return new Vector2f(-1, -1);
                    }
                }
                //hullmods
                for (String mod : s.getVariant().getNonBuiltInHullmods()) {
                    if (BLOCKED.contains(mod)) {
                        log.error("BLACKLISTED HULLMOD DETECTED:" + mod);
                        log.error("BLACKLISTED HULLMOD DETECTED:" + mod);
                        log.error("BLACKLISTED HULLMOD DETECTED:" + mod);
                        //return negative value for error
                        return new Vector2f(-1, -1);
                    }
                }
                //fighters
                for (String wing : s.getVariant().getNonBuiltInWings()) {
                    if (BLOCKED.contains(wing)) {
                        log.error("BLACKLISTED WING DETECTED:" + wing);
                        log.error("BLACKLISTED WING DETECTED:" + wing);
                        log.error("BLACKLISTED WING DETECTED:" + wing);
                        //return negative value for error
                        return new Vector2f(-1, -1);
                    }
                }
            }

            //skip fighters
            if (s.isFighterWing()) {
                continue;
            }

            //check for cost profile
            boolean buyHull = true, buyEquipment = true;

            if (MAINTENANCE) {
                if (r.x == ROUND) {
                    log.info(" ");
                    log.info("      " + s.getVariant().getHullVariantId() + " is a new ship, full cost applied.");
                    buyHull = true;
                    buyEquipment = true;
                } else if (r.y == ROUND) {
                    log.info(" ");
                    log.info("      " + s.getVariant().getHullVariantId() + " is from a previous round with a new loadout, maintenance and loadout costs applied.");
                    buyHull = false;
                    buyEquipment = true;
                } else {
                    //Maintenance rule: no buying cost, only maintenance
                    log.info(" ");
                    log.info("      " + s.getVariant().getHullVariantId() + " is a ship from a previous round, maintenance cost applied.");
                    buyHull = false;
                    buyEquipment = false;
                }
            }

            //antispam collecting hull numbers
            if (ANTISPAM) {
                ShipHullSpecAPI shipID = s.getHullSpec();
                if (HULL_LIST.containsKey(shipID)) {
                    //add to existing entry
                    Vector2f data = HULL_LIST.get(shipID);
                    if (MAINTENANCE && !buyHull) {
                        //With Maintenance, only new hulls pay the hike
                        data = new Vector2f(data.x + 1, data.y);
                    } else {
                        data = new Vector2f(data.x + 1, data.y + 1);
                    }
                    HULL_LIST.put(shipID, data);
                } else {
                    float theThreshold = SPAM_THRESHOLD.get(shipID.getHullSize());
                    //new entry, starts at minus threshold to easily check when it is breached as extra hulls values are positive
                    Vector2f data = new Vector2f(1 - theThreshold, 1);
                    if (MAINTENANCE && !buyHull) {
                        //With Maintenance, only new hulls pay the hike
                        data = new Vector2f(1 - theThreshold, 0);
                    }
                    HULL_LIST.put(shipID, data);
                }
                //Fighter anti-spam
                if (!s.getVariant().getNonBuiltInWings().isEmpty()) {
                    for (String w : s.getVariant().getNonBuiltInWings()) {
                        FighterWingSpecAPI wingID = Global.getSettings().getFighterWingSpec(w);
                        //antispam collecting wing numbers
                        if (ANTISPAM) {
                            if (WING_LIST.containsKey(wingID)) {
                                //add to existing entry
                                Vector2f data = WING_LIST.get(wingID);
                                if (MAINTENANCE && !buyHull) {
                                    //With Maintenance, only new loadouts pay the hike
                                    data = new Vector2f(data.x + 1, data.y);
                                } else {
                                    data = new Vector2f(data.x + 1, data.y + 1);
                                }
                                WING_LIST.put(wingID, data);
                            } else {
                                float theThreshold = SPAM_THRESHOLD.get(HullSize.FIGHTER);
                                //new entry, starts at minus threshold to easily check when it is breached as extra hulls values are positive
                                Vector2f data = new Vector2f(1 - theThreshold, 1);
                                if (MAINTENANCE && !buyHull) {
                                    //With Maintenance, only new hulls pay the hike
                                    data = new Vector2f(1 - theThreshold, 0);
                                }
                                WING_LIST.put(wingID, data);
                            }
                        }
                    }
                }
            }

            float maintenance = 0;
            float hull = 0;
            float weapons = 0;
            float wings = 0;

            //MAINTENANCE
            //MAINTENANCE
            //MAINTENANCE
            if (MAINTENANCE) {
                float deploy = s.getStats().getSuppliesToRecover().getModifiedValue();
                float maintain = s.getStats().getSuppliesPerMonth().getModifiedValue();
                maintenance = (deploy + maintain) * SUPPLIES;
//                log.info(" ");
//                if (!buyHull){
//                    log.info("      " + s.getVariant().getHullVariantId() + "'s maintenance:");
//                } else {
//                    log.info("      " + s.getVariant().getHullVariantId() + "'s next round maintenance (ignored this round):");
//                }
//                log.info("      " + "      " + "Deploy supplies cost: " + deploy * SUPPLIES);
//                log.info("      " + "      " + "Monthly supplies cost: " + maintain * SUPPLIES);
//                log.info("      " + "      " + "Hull maintenance cost: " + (int) maintenance * SUPPLIES);
            }

            //HULL BUYING
            //HULL BUYING
            //HULL BUYING
            if (buyHull) {
                hull = s.getHullSpec().getBaseValue();
//                log.info("      " + s.getVariant().getHullVariantId() + "'s base hull price: " + (int) hull);
            }

            //EQUIPMENT BUYING
            //EQUIPMENT BUYING
            //EQUIPMENT BUYING
            if (buyEquipment) {
                //WEAPONS (except built-in ones)
                if (!s.getVariant().getNonBuiltInWeaponSlots().isEmpty()) {
                    for (String slot : s.getVariant().getNonBuiltInWeaponSlots()) {
                        weapons += s.getVariant().getWeaponSpec(slot).getBaseValue();
                    }
                }
//                log.info("      " + s.getVariant().getHullVariantId() + "'s weapons price: " + (int) weapons);

                //WINGS (except built-in ones)  
//                List<String> NBIW = s.getVariant().getNonBuiltInWings(); //DOESN'T WORK!!!!!!!!
                List<String> BIW = new ArrayList<>(s.getHullSpec().getBuiltInWings());
                List<String> FW = s.getVariant().getFittedWings();
//                if(!NBIW.isEmpty()){
//                    for(String w : s.getVariant().getNonBuiltInWings()){
//                        FighterWingSpecAPI ID = Global.getSettings().getFighterWingSpec(w);
//                        wings += ID.getBaseValue();
//                    }
//                }
                if (!FW.isEmpty()) {
                    for (String w : FW) {
                        if (BIW.contains(w)) {
                            //only remove one entry if there are several identical ones
                            for (int i = 0; i < BIW.size(); i++) {
                                if (BIW.get(i).equals(w)) {
                                    BIW.remove(i);
                                    break;
                                }
                            }
//                            BIW.remove(w);
                        } else {
                            FighterWingSpecAPI ID = Global.getSettings().getFighterWingSpec(w);
                            wings += ID.getBaseValue();
                        }
                    }
                }
//                log.info("      " + s.getVariant().getHullVariantId() + "'s wings price: " + (int) wings);

                //HULLMODS BUYING
                //HULLMODS BUYING
                //HULLMODS BUYING
                if (HULLMOD) {
//                    log.info(" ");
                    for (String h : s.getVariant().getNonBuiltInHullmods()) {
                        if (!HULLMOD_LIST.contains(h)) {
//                                log.info("      " + Global.getSettings().getHullModSpec(h).getDisplayName() + " added to buying list.");
                            //add the hullmod
                            HULLMOD_LIST.add(h);
                        }
                    }
                }
            }

            //COMPUTE SHIP COST
            float shipValue = 0;

            if (MAINTENANCE) {

                if (!buyHull) {
                    if (!buyEquipment) {
                        //Unchanged ship from a previous round
                        log.info(" ");
                        log.info("      " + s.getVariant().getHullVariantId() + " (unchanged) total cost: ");
                        log.info("      " + " maintenance: " + maintenance);
                        log.info(" ");

                        totalShipValue += maintenance;
                        maintenanceNext += maintenance;
                    } else {
                        //refitted ship from a previous round                    
                        log.info(" ");
                        log.info("      " + s.getVariant().getHullVariantId() + " (refit) total cost: ");
                        log.info(" ");
                        log.info("      " + " maintenance: " + maintenance);
                        log.info(" ");
                        log.info("      " + " weapons: " + weapons);
                        log.info("      " + " + wings: " + wings);
                        shipValue += weapons;
                        shipValue += wings;
                        log.info("      " + " = " + shipValue);
                        log.info(" ");

                        totalShipValue += maintenance;
                        totalEquipmentValue += shipValue;
                        maintenanceNext += maintenance;
                    }
                } else {
                    //new ship
                    log.info(" ");
                    log.info("      " + s.getVariant().getHullVariantId() + " (new) total cost: ");
                    log.info(" ");
                    log.info("      " + " hull price: " + hull);
                    log.info(" ");
                    log.info("      " + "weapons: " + weapons);
                    log.info("      " + " + wings: " + wings);
                    shipValue += weapons;
                    shipValue += wings;
                    log.info("      " + " = " + shipValue);
                    log.info(" ");
                    log.info("      " + " Next round's maintenance: " + maintenance);
                    log.info(" ");

                    totalShipValue += hull;
                    totalEquipmentValue += shipValue;
                    maintenanceNext += maintenance;
                }
            } else {
                //new ship
                log.info(" ");
                log.info("      " + s.getVariant().getHullVariantId() + " (new) total cost: ");
                log.info(" ");
                log.info("      " + " hull price: " + hull);
                log.info(" ");
                log.info("      " + "weapons: " + weapons);
                log.info("      " + " + wings: " + wings);
                shipValue += weapons;
                shipValue += wings;
                log.info("      " + " = " + shipValue);
                log.info(" ");

                totalShipValue += hull;
                totalEquipmentValue += shipValue;
            }
            log.info("      " + "______________");
        }

        log.info(" ");
        log.info("TOTAL SHIPS VALUE: " + (int) totalShipValue);
        log.info(" ");

        if (MAINTENANCE) {
            log.info("Ships maintenance cost for next round: " + (int) maintenanceNext);
            log.info(" ");
        }

        log.info("____________________________");

        //COMPUTE HULLMODS
        if (HULLMOD) {
            log.info(" ");
            log.info("HULLMODS:");
            log.info(" ");

            float hullmodsValue = 0;
            for (String h : HULLMOD_LIST) {
                float price = Global.getSettings().getHullModSpec(h).getBaseValue();
                log.info("      " + Global.getSettings().getHullModSpec(h).getDisplayName() + " hullmod bought this round for " + (int) price);
                hullmodsValue += price;
            }
            totalEquipmentValue += hullmodsValue;

            log.info(" ");
            log.info("TOTAL HULLMODS EXPENSES: " + (int) hullmodsValue);
            log.info(" ");
            log.info("____________________________");
        }

        //COMPUTE ANTI-SPAM
        float shipHike = 0, equipmentHike = 0;
        if (ANTISPAM) {

            log.info(" ");
            log.info("ANTI-SPAM:");
            log.info(" ");

            if (!HULL_LIST.isEmpty()) {
                for (ShipHullSpecAPI s : HULL_LIST.keySet()) {
                    Vector2f data = HULL_LIST.get(s);
                    if (data.x > 0 && data.y > 0) {
                        log.info("      " + s.getHullId() + " : " + (int) data.x + " hulls above threshold.");
                        log.info("      " + "Applying price hike to " + Math.min((int) data.y, (int) data.x) + " new ships.");
                        float extraCost = 0;
                        int j = (int) data.x;
                        for (int i = (int) data.y; i > 0; i--) {
                            //price hike only apply to the exceeding hulls, and only compound with the next hull
                            extraCost += s.getBaseValue() * HIKE * j;
                            j--;
                            if (j < 1) {
                                break;
                            }
                        }
                        log.info("      " + "Cost penalty: " + (int) extraCost);
                        shipHike += extraCost;
                    }
                }
            }

            if (!WING_LIST.isEmpty()) {
                for (FighterWingSpecAPI w : WING_LIST.keySet()) {
                    Vector2f data = WING_LIST.get(w);
                    if (data.x > 0 && data.y > 0) {
                        log.info("      " + w.getId() + " : " + (int) data.x + " wings above threshold.");
                        log.info("      " + "Applying price hike to " + Math.min((int) data.y, (int) data.x) + " new wings.");
                        float extraCost = 0;
                        int j = (int) data.x;
                        for (int i = (int) data.y; i > 0; i--) {
                            //price hike only apply to the exceeding wings, and only compound with the next hull
                            extraCost += w.getBaseValue() * HIKE * j;
                            j--;
                            if (j < 1) {
                                break;
                            }
                        }
                        log.info("      " + "Cost penalty: " + (int) extraCost);
                        equipmentHike += extraCost;
                    }
                }
            }
            if (shipHike == 0 && equipmentHike == 0) {
                log.info("      " + "No penalty recieved.");
            } else {
                log.info("TOTAL SHIP PENALTIES: " + (int) shipHike);
                log.info("TOTAL EQUIPMENT PENALTIES: " + (int) equipmentHike);
                totalShipValue += shipHike;
                totalEquipmentValue += equipmentHike;
            }
        }

        log.info(" ");
        log.info("____________________________");
        log.info(" ");
        log.info("TOTAL FLEET VALUE: " + (int) totalShipValue);
        log.info("TOTAL EQUIPMENT VALUE: " + (int) totalEquipmentValue);
        log.info("____________________________");
        return new Vector2f((int) totalShipValue, (int) totalEquipmentValue);
    }

    ////////////////////////////////////
    //                                //
    //     2019 COST CALCULATION      //
    //                                //
    ////////////////////////////////////
    //Similar rules as regular fleet cost except both maintenance for regular ships and the Flagships pay half price for their weapons and wings.
    /**
     *
     * @param engine Combat Engine
     *
     * @param name Player name
     *
     * @param FLEET List of FleetMember
     *
     * @param HULLMOD IF Hullmods have to be bought (once per fleet)
     *
     * @param IGNORED List of hullmods allied-bought (in tag-team tournament)
     * skipped if null
     *
     * @param FLAGSHIP If FLAGSHIP rules apply
     *
     * @param flagships The Flagship, unused if FLAGSHIP is set to false
     *
     * @param ANTISPAM If ANTI-SPAM rules apply
     *
     * @param HIKE Price increase per exceeding hull, compounded for each
     * additional hull past their threshold
     *
     * @param SPAM_THRESHOLD Map of the price hike thresholds, set to high
     * values to ignore specific sizes
     *
     * @param BLACKLIST If BLACKLIST is applied
     *
     * @param BLOCKED List of all blocked IDs
     *
     * @param MAINTENANCE If MAINTENANCE rules apply
     *
     * @param PREVIOUS_FLEET List of ships that only pay for maintenance rather
     * than full hull+weapons
     *
     * @param SUPPLIES Supplies cost
     *
     * @return
     */
    public static int CombinedFleetCost(
            CombatEngineAPI engine,
            String name,
            List<FleetMemberAPI> FLEET,
            boolean HULLMOD, List<String> IGNORED,
            boolean FLAGSHIP, List<FleetMemberAPI> flagships,
            boolean ANTISPAM, float HIKE, Map<HullSize, Float> SPAM_THRESHOLD,
            boolean BLACKLIST, List<String> BLOCKED,
            boolean MAINTENANCE, List<FleetMemberAPI> PREVIOUS_FLEET, Integer SUPPLIES) {

        log.info(" ");
        log.info(" ");
        log.info("____________________________");
        log.info("____________________________");
        log.info(" ");
        log.info(" ");

        log.info("Flagship rule set to: " + FLAGSHIP);
        if (FLAGSHIP) {
            log.info("Flagships are:");
            for (int i = 0; i < flagships.size(); i++) {
                log.info("   " + flagships.get(i).getHullId());
            }
        }

        log.info("Antispam rule set to: " + ANTISPAM);
        if (ANTISPAM) {
            log.info("Price Hike: " + HIKE * 100 + "% per exceeding hull.");

            log.info("Thresholds: " + Math.round(SPAM_THRESHOLD.get(HullSize.FIGHTER))
                    + "/" + Math.round(SPAM_THRESHOLD.get(HullSize.FRIGATE))
                    + "/" + Math.round(SPAM_THRESHOLD.get(HullSize.DESTROYER))
                    + "/" + Math.round(SPAM_THRESHOLD.get(HullSize.CRUISER))
                    + "/" + Math.round(SPAM_THRESHOLD.get(HullSize.CAPITAL_SHIP)));
        }

        log.info("Maintenance rule set to: " + MAINTENANCE);
        if (MAINTENANCE) {
            log.info("Supplies price: " + SUPPLIES + " credits");
        }

        log.info("Hullmod rule set to: " + HULLMOD);
        /*
         hullmod collection
         KEY: hullmod name
         VALUE: value multiplier, 0 if bought in a previous round with some versions of the MAINTENANCE rule or by the Ally in tag-team tournaments
         */
        Map<String, Integer> HULLMOD_LIST = new HashMap<>();
        if (HULLMOD) {
            if (IGNORED != null) {
                for (String s : IGNORED) {
                    HULLMOD_LIST.put(s, 0);
                    log.info(Global.getSettings().getHullModSpec(s).getDisplayName() + " already bought by the allied fleet");
                }
            }
        }

        log.info(" ");
        log.info(" ");
        log.info("____________________________");
        log.info(" ");
        log.info(name + "'s budget breakdown");
        log.info("____________________________");
        log.info(" ");

        float totalValue = 0;
        float maintenanceNext = 0;

        /*
         anti-spam map:
         KEY: HullSpec
         VALUE: Vector2f
         X=total number of hulls of the KEY type
         Y=number of hulls bought this round that can get the price hike
         */
        Map<ShipHullSpecAPI, Vector2f> HULL_LIST = new HashMap<>();
        Map<FighterWingSpecAPI, Vector2f> WING_LIST = new HashMap<>();

        log.info(" ");
        log.info("SHIPS:");
        log.info(" ");

        for (FleetMemberAPI s : FLEET) {

            //skip fighters
            if (s.isFighterWing()) {
                continue;
            }

            //check for blocked content
            if (BLACKLIST) {
                //hull
                if (BLOCKED.contains(s.getHullSpec().getBaseHullId())) {
                    log.error("BLACKLISTED HULL DETECTED:" + s.getHullSpec().getHullName());
                    //return negative value for error
                    return -1;
                }
                //weapons
                for (String slot : s.getVariant().getNonBuiltInWeaponSlots()) {
                    if (BLOCKED.contains(s.getVariant().getWeaponId(slot))) {
                        log.error("BLACKLISTED WEAPON DETECTED:" + s.getVariant().getWeaponId(slot));
                        //return negative value for error
                        return -1;
                    }
                }
                //hullmods
                for (String mod : s.getVariant().getNonBuiltInHullmods()) {
                    if (BLOCKED.contains(mod)) {
                        log.error("BLACKLISTED HULLMOD DETECTED:" + mod);
                        //return negative value for error
                        return -1;
                    }
                }
            }

            //antispam collecting hull numbers
            if (ANTISPAM) {
                ShipHullSpecAPI ID = s.getHullSpec();
                if (HULL_LIST.containsKey(ID)) {
                    //add to existing entry
                    Vector2f data = HULL_LIST.get(ID);
                    data = new Vector2f(data.x + 1, data.y);
                    //With Maintenance, only new hulls pay the hike
                    if (MAINTENANCE && !PREVIOUS_FLEET.contains(s) && !flagships.contains(s)) {
                        data = new Vector2f(data.x, data.y + 1);
                    }
                    HULL_LIST.put(ID, data);

                } else {
                    float theThreshold = SPAM_THRESHOLD.get(ID.getHullSize());
                    //new entry, starts at minus threshold to easily check when it is breached as extra hulls values are positive
                    Vector2f data = new Vector2f(1 - theThreshold, 0);
                    if (MAINTENANCE && !PREVIOUS_FLEET.contains(s) && !flagships.contains(s)) {
                        //With Maintenance, only new hulls pay the hike
                        data = new Vector2f(data.x, data.y + 1);
                    }
                    HULL_LIST.put(ID, data);
                }
            }

            log.info(" ");
            log.info(s.getVariant().getHullVariantId() + "'s cost:");

            //check for flagship
            if (FLAGSHIP && flagships.contains(s)) {
                //Maintenance rule: no buying cost, only maintenance
                log.info(" ");
                log.info("      " + s.getVariant().getHullVariantId() + " is the Flagship.");
            } else if (flagships.contains(s) && PREVIOUS_FLEET.isEmpty()) {
                //No free Round one Flagship: regular cost
                log.info(" ");
                log.info("      " + s.getVariant().getHullVariantId() + " is the Flagship.");
            } else if (MAINTENANCE && flagships.contains(s) && !PREVIOUS_FLEET.contains(s) && !PREVIOUS_FLEET.isEmpty()) {
                //Maintenance rule but no free Flagship: refit cost for the weapons/wings + maintenance
                log.info(" ");
                log.info("      " + s.getVariant().getHullVariantId() + " is the Flagship, with a new loadout.");
            } else if (MAINTENANCE && flagships.contains(s) && !PREVIOUS_FLEET.contains(s) && !PREVIOUS_FLEET.isEmpty()) {
                //Maintenance rule but no free Flagship: only maintenance if no refit
                log.info(" ");
                log.info("      " + s.getVariant().getHullVariantId() + " is the Flagship, unchanged from the previous round.");
            } else if (MAINTENANCE && PREVIOUS_FLEET.contains(s)) {
                //Maintenance rule: no buying cost, only maintenance
                log.info(" ");
                log.info("      " + s.getVariant().getHullVariantId() + " is a ship from a previous round.");
            }

            //maintenance cost
            float maintenance = 0;
            if (MAINTENANCE) {
                float deploy = s.getStats().getSuppliesToRecover().getModifiedValue();
                float maintain = s.getStats().getSuppliesPerMonth().getModifiedValue();
                maintenance = (deploy + maintain) * SUPPLIES;
                log.info(" ");
                if (PREVIOUS_FLEET.contains(s)
                        || (FLAGSHIP && flagships.contains(s))
                        || (!FLAGSHIP && flagships.contains(s) && !PREVIOUS_FLEET.isEmpty() && !PREVIOUS_FLEET.contains(s)) //FLAGSHIP REFFIT WITHOUT FREE FIRST ROUND RULE
                        ) {
                    log.info("      " + s.getVariant().getHullVariantId() + "'s maintenance:");
                } else {
                    log.info("      " + s.getVariant().getHullVariantId() + "'s next round maintenance (ignored at this time):");
                }
                log.info("      " + "      " + "Deploy supplies cost: " + deploy * SUPPLIES);
                log.info("      " + "      " + "Monthly supplies cost: " + maintain * SUPPLIES);
                log.info("      " + "      " + "Hull maintenance cost: " + (int) (deploy + maintain) * SUPPLIES);
            }

            //regular hull cost
            float hull = 0;
            float weapons = 0;
            float wings = 0;

            //HULL price
            hull = s.getHullSpec().getBaseValue();
            log.info("      " + s.getVariant().getHullVariantId() + "'s base hull price: " + (int) hull);

            //WEAPONS prices (except built-in ones)
            if (!s.getVariant().getNonBuiltInWeaponSlots().isEmpty()) {
                for (String slot : s.getVariant().getNonBuiltInWeaponSlots()) {
                    weapons += s.getVariant().getWeaponSpec(slot).getBaseValue();
                }
            }
            log.info("      " + s.getVariant().getHullVariantId() + "'s weapons price: " + (int) weapons);

            //WINGS prices (except built-in ones)
            List<String> BIwings = s.getHullSpec().getBuiltInWings();
            if (s.getStats().getNumFighterBays().getModifiedValue() > 0) {
                for (int i = 0; i < s.getStats().getNumFighterBays().getModifiedValue(); i++) {
                    //skip empty slots
                    if (s.getVariant().getWing(i) != null) {
                        //skip built-in
                        if (!BIwings.isEmpty() && BIwings.contains(s.getVariant().getWingId(i))) {
                            log.info("      " + "Skipping built-in" + s.getVariant().getWing(i).getId());
                        } else {

                            if (BLACKLIST && BLOCKED.contains(s.getVariant().getWing(i).getId())) {
                                log.error("BLACKLISTED WING DETECTED:" + s.getHullSpec().getHullName());
                                //return negative value for error
                                return -1;
                            }

                            if (ANTISPAM) {
                                FighterWingSpecAPI ID = s.getVariant().getWing(i);
                                if (WING_LIST.containsKey(ID)) {
                                    //add to existing entry
                                    Vector2f data = WING_LIST.get(ID);
                                    data = new Vector2f(data.x + 1, data.y);
                                    WING_LIST.put(ID, data);
                                } else {
                                    //new entry, only count toward the total amount of hulls, starts at minus threshold to easily check when it is breached
                                    WING_LIST.put(ID, new Vector2f(1 - SPAM_THRESHOLD.get(s.getHullSpec().getHullSize()), -SPAM_THRESHOLD.get(s.getHullSpec().getHullSize())));
                                }
                            }
                            //add the cost
                            wings += s.getVariant().getWing(i).getBaseValue();
                        }
                    }
                }
                log.info("      " + s.getVariant().getHullVariantId() + "'s wings price: " + (int) wings);
            }

            //HULLMODS
            if (HULLMOD) {
                log.info(" ");
                if (!MAINTENANCE || !PREVIOUS_FLEET.contains(s) || (FLAGSHIP && flagships.contains(s))) {
                    for (String h : s.getVariant().getNonBuiltInHullmods()) {
                        if (!HULLMOD_LIST.containsKey(h)) {
                            log.info("      " + Global.getSettings().getHullModSpec(h).getDisplayName() + " added to buying list.");
                            //add the hullmod with a multiplier
                            HULLMOD_LIST.put(h, 1);
                        } else {
                            log.info("      " + Global.getSettings().getHullModSpec(h).getDisplayName() + " already bought.");
                        }
                    }
                } else {
                    log.info("      " + "Ship from a previous round: hullmods omitted.");
                }

            }

            //COMPUTE SHIP COST
            float shipValue = 0;

            if (MAINTENANCE) {

                //FLAGSHIP
                if (FLAGSHIP && flagships.contains(s)) {
                    log.info(" ");
                    log.info("      " + s.getVariant().getHullVariantId() + " (flagship) total cost: ");
                    log.info("      " + " maintenance: " + maintenance);
                    log.info("      " + " + weapons half price: " + weapons / 2);
                    log.info("      " + " + wings half price: " + wings / 2);
                    shipValue += maintenance;
                    shipValue += weapons / 2;
                    shipValue += wings / 2;
                    log.info("      " + " = " + shipValue);
                    log.info(" ");

                    totalValue += shipValue;
                    maintenanceNext += shipValue;
                } else //REGULAR SHIP FROM PREVIOUS ROUND
                if (PREVIOUS_FLEET.contains(s)) {
                    log.info(" ");
                    log.info("      " + s.getVariant().getHullVariantId() + " (retained) total cost: ");
                    log.info("      " + " maintenance: " + maintenance);
                    log.info("      " + " + weapons half price: " + weapons / 2);
                    log.info("      " + " + wings half price: " + wings / 2);
                    shipValue += maintenance;
                    shipValue += weapons / 2;
                    shipValue += wings / 2;
                    log.info("      " + " = " + shipValue);
                    log.info(" ");

                    totalValue += shipValue;
                    maintenanceNext += shipValue;
                } else //FLAGSHIP FROM PREVIOUS ROUND WITH NEW LOADOUT BUT NO FREE FIRST ROUND
                if (!FLAGSHIP && flagships.contains(s) && !PREVIOUS_FLEET.isEmpty() && !PREVIOUS_FLEET.contains(s)) {
                    log.info(" ");
                    log.info("      " + s.getVariant().getHullVariantId() + " (Flagship refit) total cost: ");
                    log.info("      " + " maintenance: " + maintenance);
                    log.info("      " + " + weapons full price: " + weapons);
                    log.info("      " + " + wings full price: " + wings);
                    shipValue += maintenance;
                    shipValue += weapons;
                    shipValue += wings;
                    log.info("      " + " = " + shipValue);
                    log.info(" ");

                    totalValue += shipValue;
                    maintenanceNext += shipValue;
                } else //NEW REGULAR SHIP
                {
                    log.info(" ");
                    log.info("      " + s.getVariant().getHullVariantId() + " (new) total cost: ");
                    log.info("      " + " hull price: " + hull);
                    log.info("      " + " + weapons: " + weapons);
                    log.info("      " + " + wings: " + wings);
                    shipValue += hull;
                    shipValue += weapons;
                    shipValue += wings;
                    log.info("      " + " = " + shipValue);
                    log.info(" ");
                    float next = maintenance + wings / 2 + weapons / 2;
                    log.info("      " + " Next round's maintenance: " + next);
                    log.info(" ");

                    totalValue += shipValue;
                    maintenanceNext += next;
                }
            } else {

                //FLAGSHIP
                if (FLAGSHIP && flagships.contains(s)) {
                    log.info(" ");
                    log.info("      " + s.getVariant().getHullVariantId() + " (flagship) total cost: ");
                    log.info("      " + " hull half price: " + hull / 2);
                    log.info("      " + " + weapons half price: " + weapons / 2);
                    log.info("      " + " + wings half price: " + wings / 2);
                    shipValue += hull / 2;
                    shipValue += weapons / 2;
                    shipValue += wings / 2;
                    log.info("      " + " = " + shipValue);
                    log.info(" ");

                    totalValue += shipValue;
                } else //NEW REGULAR SHIP
                {
                    log.info(" ");
                    log.info("      " + s.getVariant().getHullVariantId() + " total cost: ");
                    log.info("      " + " hull price: " + hull);
                    log.info("      " + " + weapons: " + weapons);
                    log.info("      " + " + wings: " + wings);
                    shipValue += hull;
                    shipValue += weapons;
                    shipValue += wings;
                    log.info("      " + " = " + shipValue);
                    log.info(" ");

                    totalValue += shipValue;
                }
            }
            log.info("      " + "______________");
        }

        log.info(" ");
        log.info("TOTAL SHIPS VALUE: " + (int) totalValue);
        log.info(" ");

        if (MAINTENANCE) {
            log.info("Ships maintenance cost for next round: " + (int) maintenanceNext);
            log.info(" ");
        }

        log.info("____________________________");

        //COMPUTE HULLMODS
        if (HULLMOD) {
            log.info(" ");
            log.info("HULLMODS:");
            log.info(" ");

            float hullmodsValue = 0;
            for (String h : HULLMOD_LIST.keySet()) {
                if (HULLMOD_LIST.get(h) > 0) {
                    float price = Global.getSettings().getHullModSpec(h).getBaseValue();
                    log.info("      " + Global.getSettings().getHullModSpec(h).getDisplayName() + " hullmod bought this round for " + (int) price);
                    hullmodsValue += price;
                }
            }
            totalValue += hullmodsValue;

            log.info(" ");
            log.info("TOTAL HULLMODS EXPENSES: " + (int) hullmodsValue);
            log.info(" ");
            log.info("____________________________");
        }

        //COMPUTE ANTI-SPAM
        float hike = 0;
        if (ANTISPAM) {

            log.info(" ");
            log.info("ANTI-SPAM:");
            log.info(" ");

            if (!HULL_LIST.isEmpty()) {
                for (ShipHullSpecAPI s : HULL_LIST.keySet()) {
                    Vector2f data = HULL_LIST.get(s);
                    if (data.x > 0 && data.y > 0) {
                        log.info("      " + s.getHullId() + " : " + (int) data.x + " hulls above threshold.");
                        log.info("      " + "Applying price hike to " + (int) (Math.min(data.x, data.y)) + " new ships.");
                        float extraCost = 0;
                        float j = 0;
                        for (int i = (int) data.y; i > 0; i--) {
                            //price hike only apply to the exceeding hulls, and only compound with the next hull
                            extraCost += s.getBaseValue() * HIKE * Math.max(0, data.x - j);
                            j++;
                        }
                        log.info("      " + "Cost penalty: " + (int) extraCost);
                        hike += extraCost;
                    }
                }
            }

            if (!WING_LIST.isEmpty()) {
                for (FighterWingSpecAPI w : WING_LIST.keySet()) {
                    Vector2f data = WING_LIST.get(w);
                    if (data.x > 0 && data.y > 0) {
                        log.info("      " + w.getId() + " : " + (int) data.x + " wings above threshold.");
                        log.info("      " + "Applying price hike to " + (int) (Math.min(data.x, data.y)) + " new wings.");
                        float extraCost = 0;
                        float j = 0;
                        for (int i = (int) data.y; i > 0; i--) {
                            //price hike only apply to the exceeding hulls, and only compound with the next hull
                            extraCost += w.getBaseValue() * HIKE * Math.max(0, data.x - j);
                            j++;
                        }
                        log.info("      " + "Cost penalty: " + (int) extraCost);
                        hike += extraCost;
                    }
                }
            }
            if (hike == 0) {
                log.info("      " + "No penalty recieved.");
            } else {
                log.info("TOTAL PENALTIES: " + (int) hike);
                totalValue += hike;
            }
        }

        log.info(" ");
        log.info("____________________________");
        log.info(" ");
        log.info("TOTAL FLEET VALUE: " + (int) totalValue);
        log.info("____________________________");
        return (int) totalValue;
    }

    public static int DPFleetCost(CombatEngineAPI engine,
            String name,
            List<FleetMemberAPI> FLEET,
            boolean BLACKLIST,
            List<String> BLOCKED) {

        log.info(" ");
        log.info(" ");
        log.info("____________________________");
        log.info("Computing DP Cost of Fleet");
        log.info("____________________________");
        log.info(" ");
        log.info(" ");

        float totalDP = 0;

        for (FleetMemberAPI s : FLEET) {
            //Check blacklist items
            if (BLACKLIST) {
                //hull
                if (BLOCKED.contains(s.getHullSpec().getBaseHullId())) {
                    log.error("BLACKLISTED HULL DETECTED:" + s.getHullSpec().getHullName());
                    //return negative value for error
                    return -1;
                }
                //weapons
                for (String slot : s.getVariant().getNonBuiltInWeaponSlots()) {
                    if (BLOCKED.contains(s.getVariant().getWeaponId(slot))) {
                        log.error("BLACKLISTED WEAPON DETECTED:" + s.getVariant().getWeaponId(slot));
                        //return negative value for error
                        return -1;
                    }
                }
                //hullmods
                for (String mod : s.getVariant().getNonBuiltInHullmods()) {
                    if (BLOCKED.contains(mod)) {
                        log.error("BLACKLISTED HULLMOD DETECTED:" + mod);
                        //return negative value for error
                        return -1;
                    }
                }
            }//End Blacklist Check

            //DP cost
            float hull_dp = s.getDeploymentPointsCost();
            log.info("      " + s.getVariant().getHullVariantId() + "'s base DP value: " + (int) hull_dp);
            log.info("____________");

            totalDP += hull_dp;
        }
        log.info("Total DP Cost: " + (int) totalDP);
        return (int) totalDP;
    }

    ;
    
    ////////////////////////////////////
    //                                //
    //     ALLIED BOUGHT HULLMODS     //
    //                                //
    ////////////////////////////////////
    public static List<String> GetAlliedBoughtHullmods(
            List<FleetMemberAPI> FLEET,
            List<FleetMemberAPI> PREVIOUS_FLEET
    ) {
        List<String> HULLMODS = new ArrayList<>();

        for (FleetMemberAPI m : FLEET) {
            if (PREVIOUS_FLEET.contains(m)) {
                continue;
            }
            for (String h : m.getVariant().getNonBuiltInHullmods()) {
                if (!HULLMODS.contains(h)) {
                    HULLMODS.add(h);
                }
            }
        }
        return HULLMODS;
    }

    ////////////////////////////////////
    //                                //
    //     FORCED FLEET SPAWNING      //
    //                                //
    ////////////////////////////////////
    /**
     * @param engine Combat Engine.
     *
     * @param side FleetSide to deploy.
     *
     * @param mapX Map width.
     *
     * @param mapY Map height.
     *
     * @param suppressMessage Suppress UI spawning message on the side.
     *
     * @param setCr Set CR for fleet.
     */
    public static void ForcedSpawn(CombatEngineAPI engine, FleetSide side, float mapX, float mapY, boolean suppressMessage, int setCr) {

        if (suppressMessage) {
            engine.getFleetManager(side).setSuppressDeploymentMessages(true);
        }

        if (engine.getFleetManager(side).getReservesCopy().size() > 0) {

            //start by the middle
            float angle = -90, spawnX = 0, spawnY = mapY / 2;

            //reverse for player side
            if (side == FleetSide.PLAYER) {
                spawnY *= -1;
                angle *= -1;
            }

            for (FleetMemberAPI member : engine.getFleetManager(side).getReservesCopy()) {
                //ignore fighter wings
                if (member.isFighterWing()) {
                    continue;
                }

                //spawn location
                Vector2f loc = new Vector2f(spawnX, spawnY);

                //add ship
                engine.getFleetManager(side).spawnFleetMember(member, loc, angle, 3);
                log.info("Spawning " + side.name() + "'s " + member.getHullId() + " at " + (int) spawnX + "x" + (int) spawnY);

                float newCr = setCr;
                if (engine.getFleetManager(side).getShipFor(member).getCaptain().getStats().getSkillLevel(Skills.RELIABILITY_ENGINEERING) > 0) {
                    newCr += 15;
                }
                //I don't want to calculate based on the DP of the fleet so just +15. Whatever.
                if (engine.getFleetManager(side).getFleetCommander().getStats().getSkillLevel(Skills.CREW_TRAINING) > 0) {
                    newCr += 15;
                }

                /* 11th tournament SO change
                 if(engine.getFleetManager(side).getShipFor(member).getVariant().hasHullMod("safetyoverrides")){
                 newCr-=20;
                 }
                 */
                newCr = Math.min(1, Math.max(0, (newCr / 100)));
                engine.getFleetManager(side).getShipFor(member).setCurrentCR(newCr);
                log.info(engine.getFleetManager(side).getShipFor(member).getId() + " CR set to " + setCr + " percent");

                //set new location
                if (spawnX > 0) {
                    //switch to the left
                    spawnX *= -1;
                } else {
                    //switch back to the right
                    spawnX *= -1;
                    //add offset
                    spawnX += 500;
                }
                if (spawnX >= mapX / 4) {
                    //if the line of ships is too wide, get back to the center and a row behind
                    spawnX = 0;

                    //reverse for player side
                    if (side == FleetSide.PLAYER) {
                        spawnY -= 600;
                    } else {
                        spawnY += 600;
                    }
                }
            }
        }

        if (suppressMessage) {
            engine.getFleetManager(side).setSuppressDeploymentMessages(false);
        }
    }

    ////////////////////////////////////
    //                                //
    //   FORCED SIDE FLEET SPAWNING   //
    //                                //
    ////////////////////////////////////
    /**
     * @param engine Combat Engine.
     *
     * @param side FleetSide to deploy.
     *
     * @param mapSide 0 or 1 for left/right
     *
     * @param turncoat change the fleet player/enemy side upon deployment
     *
     * @param mapX Map width.
     *
     * @param mapY Map height.
     *
     * @param suppressMessage Suppress UI spawning message on the side.
     */
    public static void ForcedSideSpawn(CombatEngineAPI engine, FleetSide side, Integer mapSide, boolean turncoat, float mapX, float mapY, boolean suppressMessage) {

        if (suppressMessage) {
            engine.getFleetManager(FleetSide.ENEMY).setSuppressDeploymentMessages(true);
            engine.getFleetManager(FleetSide.PLAYER).setSuppressDeploymentMessages(true);
        }

        if (engine.getFleetManager(side).getReservesCopy().size() > 0) {

            //start by the middle
            float angle = 0 + 180 * mapSide, spawnX = -(mapX / 2) + mapX * mapSide, spawnY = 0;

            for (FleetMemberAPI member : engine.getFleetManager(side).getReservesCopy()) {
                //ignore fighter wings
                if (member.isFighterWing()) {
                    continue;
                }

                //spawn location
                Vector2f loc = new Vector2f(spawnX, spawnY);

                //check side changes
                int newSide = member.getOwner();
                if (turncoat) {
                    if (newSide > 0) {
                        newSide = 0;
                    } else {
                        newSide = 1;
                    }
                    member.setOwner(newSide);
                    member.setAlly(turncoat);
                }

                //add ship
                engine.getFleetManager(newSide).spawnFleetMember(member, loc, angle, 3);
                log.info("Spawning " + side.name() + "'s " + member.getHullId() + " at " + (int) spawnX + "x" + (int) spawnY);

                //set new location
                if (spawnY > 0) {
                    //switch to the left
                    spawnY *= -1;
                } else {
                    //switch back to the right
                    spawnY *= -1;
                    //add offset
                    spawnY += 500;
                }
                if (spawnY >= mapY / 4) {
                    //if the line of ships is too wide, get back to the center and a row behind
                    spawnY = 0;

                    //reverse for player side
                    if (side == FleetSide.PLAYER) {
                        spawnX -= 600;
                    } else {
                        spawnX += 600;
                    }
                }
            }
        }

        if (suppressMessage) {
            engine.getFleetManager(FleetSide.ENEMY).setSuppressDeploymentMessages(false);
            engine.getFleetManager(FleetSide.PLAYER).setSuppressDeploymentMessages(false);
        }
    }

    public static void ForcedSideSpawnWithAlly(
            CombatEngineAPI engine,
            FleetSide side,
            float mapX,
            float mapY,
            boolean suppressMessage,
            Map<Integer, FleetMemberData> orderLeft,
            Map<Integer, FleetMemberData> orderRight,
            float setCr) {

        if (suppressMessage) {
            engine.getFleetManager(FleetSide.PLAYER).setSuppressDeploymentMessages(true);
        }

        //ORDERED SPAWN
        if (orderLeft != null && orderRight != null) {
            //start by the middle
            Vector2f leftLoc = new Vector2f(-mapX / 2, 0), rightLoc = new Vector2f(mapX / 2, 0);
            float leftAngle = 0, rightAngle = 180;

            //LEFT SIDE
            for (int i = 0; i < orderLeft.size(); i++) {
                if (!orderLeft.containsKey(i)) {
                    continue;
                }
                FleetMemberAPI member = orderLeft.get(i).MEMBER;
                //ignore fighter wings
                if (member.isFighterWing()) {
                    continue;
                }
                //green on the left
                engine.getFleetManager(FleetSide.PLAYER).spawnFleetMember(member, leftLoc, leftAngle, 3);

                //Set CR to the allowed max
                float newCr = setCr;
                if (engine.getFleetManager(side).getShipFor(member).getCaptain().getStats().getSkillLevel(Skills.RELIABILITY_ENGINEERING) > 0) {
                    newCr += 15;
                }
                //I don't want to calculate based on the DP of the fleet so just +15. Whatever.
                if (engine.getFleetManager(side).getFleetCommander().getStats().getSkillLevel(Skills.CREW_TRAINING) > 0) {
                    newCr += 15;
                }
                newCr = Math.min(1, Math.max(0, (newCr / 100)));
                engine.getFleetManager(FleetSide.PLAYER).getShipFor(member).setCurrentCR(newCr);

                //calculate next spawn point
                float X = leftLoc.x, Y = leftLoc.y;

                if (Y < 0) {
                    //alternate side
                    Y *= -1;
                } else {
                    //alternate side
                    Y *= -1;
                    //offset one ship further, starting toward the bottom
                    Y -= 500;
                }
                //end of the row
                if (Y < -2600) {
                    //return to the center
                    Y = 0;
                    //offet one row behind
                    X -= 700;
                }
                leftLoc = new Vector2f(X, Y);
            }
            //RIGHT SIDE
            for (int i = 0; i < orderRight.size(); i++) {
                if (!orderRight.containsKey(i)) {
                    continue;
                }
                FleetMemberAPI member = orderRight.get(i).MEMBER;
                //ignore fighter wings
                if (member.isFighterWing()) {
                    continue;
                }
                //yellow to the right

                member.setOwner(0);
//                member.setAlly(true);
                engine.getFleetManager(FleetSide.PLAYER).addToReserves(member);
                engine.getFleetManager(FleetSide.PLAYER).spawnFleetMember(member, rightLoc, rightAngle, 3);
                engine.getFleetManager(FleetSide.PLAYER).getShipFor(member).setAlly(true);
//                engine.getFleetManager(FleetSide.ENEMY).spawnFleetMember(member, rightLoc, rightAngle, 3);                
//                engine.getFleetManager(FleetSide.ENEMY).getShipFor(member).setOwner(0);
//                engine.getFleetManager(FleetSide.ENEMY).getShipFor(member).setAlly(true);

                //Set CR to the allowed max
                float newCr = setCr;
                if (engine.getFleetManager(side).getShipFor(member).getCaptain().getStats().getSkillLevel(Skills.RELIABILITY_ENGINEERING) > 0) {
                    newCr += 15;
                }
                //I don't want to calculate based on the DP of the fleet so just +15. Whatever.
                if (engine.getFleetManager(side).getFleetCommander().getStats().getSkillLevel(Skills.CREW_TRAINING) > 0) {
                    newCr += 15;
                }
                newCr = Math.min(1, Math.max(0, (newCr / 100)));
                engine.getFleetManager(FleetSide.PLAYER).getShipFor(member).setCurrentCR(newCr);

                //calculate next spawnpoint
                float X = rightLoc.x, Y = rightLoc.y;

                if (Y < 0) {
                    //alternate side
                    Y *= -1;
                } else {
                    //alternate side
                    Y *= -1;
                    //offset one ship further, starting toward the bottom
                    Y -= 500;
                }
                //end of the row
                if (Y < -2600) {
                    //return to the center
                    Y = 0;
                    //offet one row behind
                    X += 700;
                }
                rightLoc = new Vector2f(X, Y);
            }
        }
//        else {
//            //NON ORDERED SPAWN
//            if (engine.getFleetManager(side).getReservesCopy().size() > 0) {
//
//                //start by the middle
//                Vector2f leftLoc= new Vector2f(-mapX/2,0), rightLoc=new Vector2f(mapX/2,0);
//                float leftAngle = 0, rightAngle=180;
//
//                for (FleetMemberAPI member : engine.getFleetManager(side).getReservesCopy()) {
//                    //ignore fighter wings
//                    if (member.isFighterWing()) {
//                        continue;
//                    }
//
//                    if(!member.isAlly()){
//                        //green on the left
//                        engine.getFleetManager(FleetSide.PLAYER).spawnFleetMember(member, leftLoc, leftAngle, 3);
//
//                        float X = leftLoc.x, Y=leftLoc.y;
//
//                        if(Y<0){
//                            //alternate side
//                            Y*=-1;
//                        } else {
//                            //alternate side
//                            Y*=-1;
//                            //offset one ship further, starting toward the bottom
//                            Y-=500;
//                        }
//                        //end of the row
//                        if(Y<-2600){
//                            //return to the center
//                            Y=0;
//                            //offet one row behind
//                            X-=700;
//                        }                    
//                        leftLoc=new Vector2f(X,Y);
//
//                    } else {
//                        //yellow to the right
//                        engine.getFleetManager(FleetSide.PLAYER).spawnFleetMember(member, rightLoc, rightAngle, 3);
//
//                        float X = rightLoc.x, Y=rightLoc.y;
//
//                        if(Y<0){
//                            //alternate side
//                            Y*=-1;
//                        } else {
//                            //alternate side
//                            Y*=-1;
//                            //offset one ship further, starting toward the bottom
//                            Y-=500;
//                        }
//                        //end of the row
//                        if(Y<-2600){
//                            //return to the center
//                            Y=0;
//                            //offet one row behind
//                            X+=700;
//                        }                    
//                        rightLoc=new Vector2f(X,Y);
//                    }
//                }
//            }
//        }

        if (suppressMessage) {
            engine.getFleetManager(FleetSide.PLAYER).setSuppressDeploymentMessages(false);
        }
    }

    ////////////////////////////////////
    //                                //
    //   PERCENTAGE FLEET REMAINING   //
    //                                //
    ////////////////////////////////////
    public static Integer fleetStatusUpdate(List<ShipAPI> fleet, boolean hullMult) {
        float totalFP = 0, aliveFP = 0;
        for (ShipAPI ship : fleet) {
            totalFP += ship.getHullSpec().getFleetPoints();
            if (ship.isAlive()) {
                if (hullMult) {
                    aliveFP += (ship.getHullLevel() * ship.getHullSpec().getFleetPoints());
                } else {
                    aliveFP += ship.getHullSpec().getFleetPoints();
                }
            }
        }
        float percent = 100 * (aliveFP / totalFP);
        return Math.round(percent);
    }

    //in Deployment Points
    public static Integer fleetStatusUpdateDP(List<ShipAPI> fleet) {
        float totalDP = 0, aliveDP = 0;
        for (ShipAPI ship : fleet) {
            if (ship.isFighter() || ship.isDrone()) {
                continue;
            }
            totalDP += ship.getFleetMember().getDeploymentPointsCost();
            if (ship.isAlive()) {
                aliveDP += ship.getFleetMember().getDeploymentPointsCost();
            }
        }
        float percent = 100 * (aliveDP / totalDP);
        return Math.round(percent);
    }

    ////////////////////////////////////
    //                                //
    //        READY EVERYTHING        //
    //                                //
    ////////////////////////////////////
    public static void prepareForNewWave(CombatEngineAPI engine, Vector2f center, Vector2f amount, float max_cr, float ammoAmount) {

//        AssignmentTargetAPI rally = engine.getFleetManager(FleetSide.PLAYER).createWaypoint(new Vector2f(), false);        
        for (ShipAPI s : engine.getShips()) {

            //remove debris
            if (!s.isAlive() || s.getOriginalOwner() != 0) {
                engine.removeEntity(s);
            } else {

                //skip modules
                if (s.getParentStation() != null) {
                    continue;
                }

                Vector2f L = s.getLocation();

                //scale around the relative center
                Vector2f.sub(L, center, L);
                L.set(L.x * amount.x, L.y * amount.y);
                Vector2f.add(L, center, L);

                if (max_cr > 0) {
                    //restore CR  
                    float newCr = max_cr;
                    if (s.getCaptain() != null && s.getCaptain().getStats().getSkillLevel(Skills.RELIABILITY_ENGINEERING) > 0) {
                        newCr += 15;
                    }
                    //I don't want to calculate based on the DP of the fleet so just +15. Whatever.
                    if (engine.getFleetManager(FleetSide.PLAYER).getFleetCommander().getStats().getSkillLevel(Skills.CREW_TRAINING) > 0) {
                        newCr += 15;
                    }

                    newCr = Math.min(1, Math.max(0, (newCr / 100)));
                    s.setCurrentCR(newCr);

                    //rebuild fighters
                    if (!s.getLaunchBaysCopy().isEmpty()) {
                        for (FighterLaunchBayAPI bay : s.getLaunchBaysCopy()) {
                            if (bay.getWing() == null) {
                                continue;
                            }

                            bay.makeCurrentIntervalFast();

                            int rebuild = bay.getWing().getSpec().getNumFighters() - bay.getWing().getWingMembers().size();
                            if (rebuild > 0) {
                                bay.setFastReplacements(rebuild);
                            }
                        }
                    }
                }

                if (ammoAmount > 0) {
                    //restore weapons
                    for (WeaponAPI w : s.getAllWeapons()) {
                        if (w.usesAmmo()) {
                            int ammo = w.getAmmo();
                            ammo = ammo + Math.round((w.getMaxAmmo() - ammo) * ammoAmount);
                            w.setAmmo(ammo);
                        }
                    }
                    //restore systems
                    if (s.getSystem() != null && s.getSystem().getMaxAmmo() > 0) {
                        if (s.getSystem().getAmmo() < s.getSystem().getMaxAmmo()) {
                            int ammo = s.getSystem().getAmmo();
                            ammo = ammo + Math.round((s.getSystem().getMaxAmmo() - ammo) * ammoAmount);
                            s.getSystem().setAmmo(ammo);
                        }
                    }
                }
                //defend the center
//                engine.getFleetManager(FleetSide.PLAYER).getTaskManager(s.isAlly()).giveAssignment(
//                        engine.getFleetManager(FleetSide.PLAYER).getDeployedFleetMember(s),
//                        engine.getFleetManager(FleetSide.PLAYER).getTaskManager(true).createAssignment(CombatAssignmentType.DEFEND, rally, false),
//                        false
//                );
            }
        }
        for (MissileAPI s : engine.getMissiles()) {
            engine.applyDamage(s, s.getLocation(), s.getHitpoints() * 10, DamageType.ENERGY, 0, true, true, s);
        }
        for (DamagingProjectileAPI s : engine.getProjectiles()) {
            engine.removeEntity(s);
        }
    }

    public static void gracePeriodOn(List<ShipAPI> fleet) {
        for (ShipAPI s : fleet) {
            s.getMutableStats().getHullDamageTakenMult().modifyMult("AI_battle", 0);
            s.getMutableStats().getArmorDamageTakenMult().modifyMult("AI_battle", 0);
        }
    }

    public static void gracePeriodOff(List<ShipAPI> fleet) {

        for (ShipAPI s : fleet) {
            s.getMutableStats().getHullDamageTakenMult().unmodify("AI_battle");
            s.getMutableStats().getArmorDamageTakenMult().unmodify("AI_battle");
        }
    }

    ////////////////////////////////////
    //                                //
    //           MATCH TAG            //
    //                                //
    ////////////////////////////////////
    public static String matchLetter(int matchNumber, boolean reverse) {

        if (!reverse) {
            return MATCH_LETTER.get(matchNumber);
        } else {
            return MATCH_LETTER.get((int) (matchNumber / 2));
        }
    }

    ////////////////////////////////////
    //                                //
    //         STRINGS CUTOFF         //
    //                                //
    ////////////////////////////////////
    public static String cutoff(String toCut, Integer threshold) {
        if (toCut.length() <= threshold) {
            return toCut;
        } else {
            return toCut.substring(0, threshold + 1);
        }
    }

    ////////////////////////////////////
    //                                //
    //         WAVES SPAWNING         //
    //                                //
    ////////////////////////////////////
    public static void WaveSpawning(CombatEngineAPI engine, Integer wave, Integer round, float mapY, float mapX, String PATH) {

        engine.getFleetManager(FleetSide.ENEMY).setSuppressDeploymentMessages(true);

        //initial positions
        float topX = 0, topY = mapY / 2 + 1000;
        float leftX = -1000 - (mapX / 2), leftY = 0;
        float rightX = 1000 + (mapX / 2), rightY = 0;
        float bottomX = 0, bottomY = -1000 - (mapY / 2);
        //special boss warp location
        float bossX = 0, bossY = mapY / 2 - 500;

        //read waves
        try {
            JSONArray enemyFleet = Global.getSettings().loadCSV(PATH + "player0_fleet.csv");
            ShipAPI spawn = null;

            Map<String, PersonAPI> officers = createOfficers(PATH, 0);
            Map<String, Boolean> officerUsed = new HashMap<>();
            for (String officer : officers.keySet()) {
                officerUsed.put(officer, Boolean.FALSE);
            }

            for (int i = 0; i < enemyFleet.length(); i++) {
                JSONObject row = enemyFleet.getJSONObject(i);

                //only add the ships for the relevant round
                if (row.getInt("wave") == wave) {

                    String variant = row.getString("variantID");
                    String personality = row.getString("defaultPersonality");
                    String side = row.getString("side");

                    float setCR = 0.7f;

                    switch (side) {

                        case "BOSS":
                            Vector2f boss = new Vector2f(bossX, bossY);
                            spawn = engine.getFleetManager(FleetSide.ENEMY).spawnShipOrWing(variant, boss, -90, 0.5f);
                            //levelup(spawn.getCaptain());
                            //spawn.getCaptain().getStats().refreshCharacterStatsEffects();

                            /*
                             if(wave==7){
                             Global.getSoundPlayer().playUISound("AIB_bossArrival_"+round, 1, 1);
                             } else {
                             Global.getSoundPlayer().playUISound("AIB_bossArrival_"+(round-1), 1, 1);
                             }
                             */
                            spawn.setCurrentCR(1);
                            log.info("Spawning " + spawn.getHullSpec().getHullName());
                            break;

                        case "TOP":
                            //spawn ship
                            Vector2f top = new Vector2f(topX, topY);
                            spawn = engine.getFleetManager(FleetSide.ENEMY).spawnShipOrWing(variant, top, -90, 3);
                            spawn.getCaptain().setPersonality(personality);

                            /*
                             if(topSpawn.isCapital()){
                             levelup(topSpawn.getCaptain());
                             topSpawn.getCaptain().getStats().refreshCharacterStatsEffects();
                             }
                             */
                            spawn.setCurrentCR(setCR);
                            log.info("Spawning " + spawn.getHullSpec().getHullName());

                            //calculate next spawn point
                            if (topX < 0) {
                                //alternate side
                                topX *= -1;
                            } else {
                                //alternate side
                                topX *= -1;
                                //offset one ship further, starting toward the bottom
                                topX -= 500;
                            }
                            //end of the row
                            if (topX < -2600) {
                                //return to the center
                                topX = 0;
                                //offet one row behind
                                topY += 900;
                            }
                            break;
                        case "LEFT":
                            //spawn ship
                            Vector2f left = new Vector2f(leftX, leftY);
                            spawn = engine.getFleetManager(FleetSide.ENEMY).spawnShipOrWing(variant, left, 0, 3);
                            spawn.getCaptain().setPersonality(personality);

                            /*
                             if(leftSpawn.isCapital()){
                             levelup(leftSpawn.getCaptain());
                             leftSpawn.getCaptain().getStats().refreshCharacterStatsEffects();
                             }
                             */
                            spawn.setCurrentCR(setCR);
                            log.info("Spawning " + spawn.getHullSpec().getHullName());

                            //calculate next spawn point
                            if (leftY < 0) {
                                //alternate side
                                leftY *= -1;
                            } else {
                                //alternate side
                                leftY *= -1;
                                //offset one ship further, starting toward the bottom
                                leftY -= 500;
                            }
                            //end of the row
                            if (leftY < -2600) {
                                //return to the center
                                leftY = 0;
                                //offet one row behind
                                leftX -= 900;
                            }
                            break;

                        case "RIGHT":
                            //spawn ship
                            Vector2f right = new Vector2f(rightX, rightY);
                            spawn = engine.getFleetManager(FleetSide.ENEMY).spawnShipOrWing(variant, right, 180, 3);
                            spawn.getCaptain().setPersonality(personality);

                            /*
                             if(rightSpawn.isCapital()){
                             levelup(rightSpawn.getCaptain());
                             rightSpawn.getCaptain().getStats().refreshCharacterStatsEffects();
                             }
                             */
                            spawn.setCurrentCR(setCR);
                            log.info("Spawning " + spawn.getHullSpec().getHullName());

                            //calculate next spawn point
                            if (rightY < 0) {
                                //alternate side
                                rightY *= -1;
                            } else {
                                //alternate side
                                rightY *= -1;
                                //offset one ship further, starting toward the bottom
                                rightY -= 500;
                            }
                            //end of the row
                            if (rightY < -2600) {
                                //return to the center
                                rightY = 0;
                                //offet one row behind
                                rightX += 900;
                            }
                            break;

                        case "BOTTOM":
                            //spawn ship
                            Vector2f bottom = new Vector2f(bottomX, bottomY);
                            spawn = engine.getFleetManager(FleetSide.ENEMY).spawnShipOrWing(variant, bottom, 90, 3);
                            /*
                             if(bottomSpawn.isCapital()){
                             levelup(bottomSpawn.getCaptain());
                             bottomSpawn.getCaptain().getStats().refreshCharacterStatsEffects();
                             }
                             */
                            spawn.setCurrentCR(setCR);
                            log.info("Spawning " + spawn.getHullSpec().getHullName());

                            //calculate next spawn point
                            if (bottomX < 0) {
                                //alternate side
                                bottomX *= -1;
                            } else {
                                //alternate side
                                bottomX *= -1;
                                //offset one ship further, starting toward the bottom
                                bottomX -= 500;
                            }
                            //end of the row
                            if (bottomX < -2600) {
                                //return to the center
                                bottomX = 0;
                                //offet one row behind
                                bottomY -= 900;
                            }
                            break;
                    }
                    if (spawn != null) {
                        spawn.getCaptain().setPersonality(personality);

                        if (!row.getString("shipName").isEmpty()) {
                            String shipName = "AI " + cutoff(row.getString("shipName"), 24);
                            spawn.setName(shipName);
                            spawn.getFleetMember().setShipName(shipName);
                        }
                        //custom officer
                        if (officers.containsKey(row.getString("shipName"))) {
                            if (!(officerUsed.get(row.getString("shipName")))) {
                                spawn.setCaptain(officers.get(row.getString("shipName")));
                                log.info(" ");
                                log.info("Captain " + spawn.getCaptain().getNameString() + " assigned to " + spawn.getName());
                                log.info("Personality: " + spawn.getCaptain().getPersonalityAPI().getDisplayName());
                                for (SkillLevelAPI s : spawn.getCaptain().getStats().getSkillsCopy()) {
                                    //                            log.info("skill: "+s.getSkill().getId()+" level: "+s.getLevel());
                                }
                                officerUsed.put(row.getString("shipName"), Boolean.TRUE);
                            } else {
                                log.info("Officer " + officers.get(row.getString("shipName")) + " is already assigned.");
                            }
                        }
                        spawn.getCaptain().getStats().refreshCharacterStatsEffects();
                    }
                }
            }
        } catch (IOException | JSONException ex) {
            log.error("unable to read Wave fleet.csv");
        }
    }

    private static void levelup(PersonAPI captain) {

        final OfficerLevelupPlugin plugin = (OfficerLevelupPlugin) Global.getSettings().getPlugin("officerLevelUp");

        captain.getStats().addXP(plugin.getXPForLevel(33));
        captain.getStats().setPoints(33);
        captain.setPortraitSprite("graphics/portraits/portrait_ai1b.png");
        captain.getStats().setSkillLevel("combat_endurance", 3);
        //captain.getStats().setSkillLevel("missile_specialization", 3);
        captain.getStats().setSkillLevel("ordnance_expert", 3);
        captain.getStats().setSkillLevel("target_analysis", 3);
        captain.getStats().setSkillLevel("damage_control", 3);
        captain.getStats().setSkillLevel("impact_mitigation", 3);
        captain.getStats().setSkillLevel("defensive_systems", 3);
        captain.getStats().setSkillLevel("advanced_countermeasures", 3);
        captain.getStats().setSkillLevel("evasive_action", 3);
        captain.getStats().setSkillLevel("helmsmanship", 3);
        //captain.getStats().setSkillLevel("carrier_command", 3);
        //captain.getStats().setSkillLevel("wing_commander", 3);
        //captain.getStats().setSkillLevel("strike_commander", 3);
        captain.getStats().setSkillLevel("gunnery_implants", 3);
        captain.getStats().setSkillLevel("flux_modulation", 3);
    }

    public static void warpZone(CombatEngineAPI engine, float intensity, Vector2f location) {
        if (Math.random() < 0.25f + (intensity * 0.5f)) {
            Vector2f offset = MathUtils.getRandomPointInCircle(new Vector2f(), 50 + 150 * intensity);
            engine.addHitParticle(
                    Vector2f.sub(location, offset, new Vector2f()),
                    offset,
                    MathUtils.getRandomNumberInRange(3, 5 + 5 * intensity),
                    0.25f + 0.5f * intensity,
                    MathUtils.getRandomNumberInRange(0.5f, 1 + intensity),
                    Color.cyan
            );
        }
        if (Math.random() < 0.1f + (intensity * 0.15f)) {
            MagicLensFlare.createSharpFlare(
                    engine,
                    engine.getPlayerShip(),
                    MathUtils.getRandomPointInCircle(location, 150 - 150 * intensity),
                    MathUtils.getRandomNumberInRange(3, 4 + 4 * intensity),
                    MathUtils.getRandomNumberInRange(30, 50 + 50 * intensity),
                    0,
                    Color.PINK,
                    Color.BLUE
            );

            /*
             if(Math.random()<0.1f){
             Global.getSoundPlayer().playSound("AIB_bossRipple", 0.8f+0.4f*intensity, 0.5f+0.5f*intensity, location, new Vector2f());
             }
             */
        }
        //Global.getSoundPlayer().playLoop("AIB_bossShadow", engine.getPlayerShip(), 1, 1, location, new Vector2f());
    }

    public static class FleetMemberData {

        public final FleetMemberAPI MEMBER;
        public final Integer BOUGHT;
        public final Integer REFIT;

        public FleetMemberData(FleetMemberAPI member, int bought, int refit) {
            this.MEMBER = member;
            this.BOUGHT = bought;
            this.REFIT = refit;
        }
    }
}
