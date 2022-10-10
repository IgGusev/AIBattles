package data.missions.PvE;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatAssignmentType;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI.AssignmentInfo;
import com.fs.starfarer.api.combat.DeployedFleetMemberAPI;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;
import com.fs.starfarer.api.util.IntervalUtil;
import data.missions.scripts.AI_FleetMemberUtils;
import data.missions.scripts.AI_missionUtils;
import data.missions.scripts.AI_missionUtils.FleetMemberData;
import static data.missions.scripts.AI_missionUtils.createFleetReturnRefit;
import static data.missions.scripts.AI_missionUtils.gracePeriodOff;
import static data.missions.scripts.AI_missionUtils.gracePeriodOn;
import data.scripts.plugins.AI_freeCamPlugin;
import data.scripts.plugins.AI_relocatePlugin;
import data.scripts.util.MagicAnim;
import data.scripts.util.MagicRender;
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
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

public class MissionDefinition implements MissionDefinitionPlugin {

    public static Logger log = Global.getLogger(MissionDefinition.class);

    public static Map<HullSize, Float> SPAM_THRESHOLD = new HashMap<>();
    private static final Map<Integer, Vector2f> MATCHES = new HashMap<>();

    private static int match, player, enemy, clock = 0;
    private static boolean first = true, reveal = false, deploy = false, cr = false, isShown = false;
    private static final List<String> BLOCKED = new ArrayList<>();
    private static Map<Integer, AI_missionUtils.FleetMemberData> PLAYER_A_FLEET = new HashMap<>();
    private static Map<Integer, AI_missionUtils.FleetMemberData> PLAYER_B_FLEET = new HashMap<>();
    private static final List<ShipAPI> PLAYER_A_SHIPS = new ArrayList<>();
    private static final List<ShipAPI> PLAYER_B_SHIPS = new ArrayList<>();

    private static List<String> PlayerA = new ArrayList<>();
    private static List<String> PlayerB = new ArrayList<>();

    public static final String PATH = "tournament/";
    public static final String ROUND_DATA = PATH + "round_data.csv";
    public static final String ROUND_MATCHES = PATH + "PvE_matches.csv";
    public static final String ROUND_BLACKLIST = PATH + "tournament_blacklist.csv";
    public static final String OVERBUDGET = " ! OVERBUDGET ! ";

    private static int ROUND, MAX_WAVE, BATTLESCAPE, WINNER = -1, LOSER = -1, SUPPLIES;
    private static float HIKE;
    private static boolean MAINTENANCE, CLOCK, VS, NO_RETREAT, ADMIRAL_AI, FLAGSHIP, BLACKLIST, ANTISPAM, HULLMOD, MAX_CR, SOUND = false, END = false, SHAME = false;
    private static boolean STATUS, SHOW_DP, SHOW_COST;
    private static float mapX, mapY, timer = 0;
    private static String prefixPlayerA, prefixPlayerB;

    private static int teamA = 100, teamB = 100;
    private static final IntervalUtil status = new IntervalUtil(0.2f, 0.3f);

    //New in 11th
    private static int CR = 70, TIMEOUT;

    //PvE stuff
    private static boolean delay = false, teamReady = false;
    private static float countdown = 10;
    private static int max_cr, WAVE = 1, countdownTrigger = 9;
    private static final IntervalUtil STATUS_CHECK = new IntervalUtil(0.2f, 0.3f);
    private static final IntervalUtil DELAY_TIMER = new IntervalUtil(5, 5);
    private static float waveTimer = 0;
    private static Map<Integer, Vector3f> progress = new HashMap<>();

    @Override
    public void defineMission(MissionDefinitionAPI api) {

        //cleanup
        BLOCKED.clear();
        PLAYER_A_FLEET.clear();
        PLAYER_B_FLEET.clear();
        PLAYER_A_SHIPS.clear();
        PLAYER_B_SHIPS.clear();
        clock = 0;
        waveTimer = 0;
        reveal = false;//fixes timeout bug
        cr = false;//fixes CR only being applied once

        //read the matches data from CSVs
        createMatches();

        //cycle the matches while holding space
        if (first) {
            first = false;
            match = 0;
        } else if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) {
            match++;
            if (match >= MATCHES.size()) {
                match = 0;
            }
        } else if (Keyboard.isKeyDown(Keyboard.KEY_LEFT)) {
            match--;
            if (match < 0) {
                match = MATCHES.size() - 1;
            }
        }

        //PvE
        //player number
        player = Math.round(MATCHES.get(match).x);
        max_cr = (int) MATCHES.get(match).y;
        PlayerA = AI_missionUtils.createPlayerJSON(PATH, player);

        api.initFleet(FleetSide.PLAYER, PlayerA.get(1), FleetGoal.ATTACK, ADMIRAL_AI, 1);
        api.setFleetTagline(FleetSide.PLAYER, PlayerA.get(2));

        prefixPlayerA = PlayerA.get(1);

        PLAYER_A_FLEET = createFleetReturnRefit(api, FleetSide.PLAYER, ROUND, player, PATH, prefixPlayerA);
        for (FleetMemberData data : PLAYER_A_FLEET.values()) {
            if (data.MEMBER.isFlagship()) {
                AI_FleetMemberUtils.moveFleetCommanderSkillsToFlagship(api, FleetSide.PLAYER, data.MEMBER, true);
            }
            AI_FleetMemberUtils.addOfficerDetailsHullmod(data.MEMBER);
        }

        log.info("________________________________");
        log.info("Player A Name: " + PlayerA.get(0));
        log.info("Player A Prefix: " + PlayerA.get(1));
        log.info("Player A Tag: " + PlayerA.get(2));
        log.info("________________________________");

        enemy = Math.round(MATCHES.get(match).y);
        List<String> AIwaves = AI_missionUtils.createPlayerJSON(PATH, 0);
        api.initFleet(FleetSide.ENEMY, AIwaves.get(1), FleetGoal.ATTACK, ADMIRAL_AI, 1, 1);
        api.setFleetTagline(FleetSide.ENEMY, "A Few Angry Ships");

        int playerDPCost = AI_missionUtils.DPFleetCostWithMemberData(Global.getCombatEngine(),
                PlayerA.get(0),
                PLAYER_A_FLEET,
                BLACKLIST,
                BLOCKED
        );

        //setup Briefing
        log.info("Adding briefing");
        if (playerDPCost > 0) {//negative values indicate error, probably blacklist
            String costOuput = "";
            if (SHOW_DP) {
                costOuput = costOuput + PlayerA.get(0) + " DP : " + playerDPCost;
            }
            /*
             if(SHOW_COST){
             costOuput = costOuput +"    Credits : "+playerCost;
             }
             */
            api.addBriefingItem(costOuput);
            log.info("Cost briefing: " + costOuput);
        } else {
            String errorMessage = "";
            switch (playerDPCost) {
                case -1:
                    errorMessage = "BLACKLISTED HULL DETECTED!!!";
                    break;
                case -2:
                    errorMessage = "BLACKLISTED WEAPON DETECTED!!!";
                    break;
                case -3:
                    errorMessage = "BLACKLISTED WING DETECTED!!!";
                    break;
                case -4:
                    errorMessage = "BLACKLISTED HULLMOD DETECTED!!!";
                    break;
                case -5:
                    errorMessage = "SHIP USING EXTRA OP";
                    break;
                case -6:
                    errorMessage = "SHIP USING EXTRA CAPS";
                    break;
                case -7:
                    errorMessage = "SHIP USING EXTRA VENTS";
                    break;
                default:
                    errorMessage = "NO SHIPS DETECTED!!!";
            }
            api.addBriefingItem(PlayerA.get(0) + ": " + errorMessage);
            log.info("Error briefing: " + PlayerA.get(0) + ": " + errorMessage);
        }

        //set the terrain
        AI_missionUtils.setBattlescape(api, BATTLESCAPE);
        log.info("Using battlescape " + BATTLESCAPE + ".");

        //add the price check and anti-retreat plugin
        api.addPlugin(new Plugin());
    }

    public final static class Plugin extends BaseEveryFrameCombatPlugin {

        ////////////////////////////////////
        //                                //
        //      BATTLE INITIALISATION     //
        //                                //
        ////////////////////////////////////
        @Override
        public void init(CombatEngineAPI engine) {

            ////////////////////////////////////
            //                                //
            //    CAMERA AND TIME CONTROLS    //
            //                                //
            ////////////////////////////////////
            EveryFrameCombatPlugin free_cam = new AI_freeCamPlugin();
            engine.removePlugin(free_cam);
            engine.addPlugin(free_cam);

            EveryFrameCombatPlugin relocate = new AI_relocatePlugin();
            engine.removePlugin(relocate);
            engine.addPlugin(relocate);

            //add loads of deployment points to preven the AI from holding ships
            Global.getCombatEngine().setMaxFleetPoints(FleetSide.ENEMY, 9999);
            Global.getCombatEngine().setMaxFleetPoints(FleetSide.PLAYER, 9999);

            mapX = engine.getMapWidth();
            mapY = engine.getMapHeight();

            deploy = false;
            SOUND = false;

            isShown = false;
            END = false;
            SHAME = false;

            WAVE = 1;
        }

        private int bonus0 = 0;
        private int bonus1 = 0;

        ////////////////////////////////////
        //                                //
        //         ADVANCE PLUGIN         //
        //                                //
        ////////////////////////////////////
        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            CombatEngineAPI engine = Global.getCombatEngine();

            ////////////////////////////////////
            //                                //
            //          FORCED SPAWN          //
            //                                //
            ////////////////////////////////////
            if (!deploy) {

                deploy = true;

                log.info("Map size: " + (int) mapX + "x" + (int) mapY);

                int setCr = 100;
                if (!MAX_CR) {
                    setCr = max_cr;
                }
                //public static void ForcedSpawn(CombatEngineAPI engine, FleetSide side, float mapX, float mapY, boolean suppressMessage, int setCR)
                AI_missionUtils.ForcedSpawn(engine, FleetSide.PLAYER, mapX, mapY, true, setCr);
                AI_missionUtils.ForcedSpawn(engine, FleetSide.ENEMY, mapX, mapY, true, setCr);

                //fleet status list
                for (int i = 0; i < PLAYER_A_FLEET.size(); i++) {
                    if (PLAYER_A_FLEET.containsKey(i)) {
                        PLAYER_A_SHIPS.add(engine.getFleetManager(FleetSide.PLAYER).getShipFor(PLAYER_A_FLEET.get(i).MEMBER));
                    }
                }
                for (int i = 0; i < PLAYER_B_FLEET.size(); i++) {
                    if (PLAYER_B_FLEET.containsKey(i)) {
                        PLAYER_B_SHIPS.add(engine.getFleetManager(FleetSide.ENEMY).getShipFor(PLAYER_B_FLEET.get(i).MEMBER));
                    }
                }

                //SPAWN the starting enemy ships
                AI_missionUtils.WaveSpawning(
                        engine,
                        WAVE,
                        ROUND,
                        mapY - 1000,
                        mapX,
                        PATH
                );

                //removes deployment points to prevent the AI to deploy the "player ships" showed on its side in the briefing
                engine.setMaxFleetPoints(FleetSide.ENEMY, 0);
                engine.setMaxFleetPoints(FleetSide.PLAYER, 0);

                //map reveal to all
                engine.getFogOfWar(1).revealAroundPoint(engine, 0, 0, 90000);
                engine.getFogOfWar(0).revealAroundPoint(engine, 0, 0, 90000);

                engine.setDoNotEndCombat(true);

                progress.put(
                        WAVE,
                        new Vector3f(
                                100,
                                100,
                                0
                        )
                );

                return;
            }

            ////////////////////////////////////
            //                                //
            //         ANTI STALLING          //
            //                                //
            ////////////////////////////////////
            if (!engine.isPaused()) {
                waveTimer += amount;
                if (waveTimer > 60) {
                    if (!engine.getFleetManager(FleetSide.PLAYER).getTaskManager(false).isFullAssault()) {
                        engine.getFleetManager(FleetSide.PLAYER).getTaskManager(false).setFullAssault(true);
                    }

                    if (!engine.getFleetManager(FleetSide.PLAYER).getTaskManager(true).isFullAssault()) {
                        engine.getFleetManager(FleetSide.PLAYER).getTaskManager(true).setFullAssault(true);
                    }

//                    for(FleetMemberAPI m : engine.getFleetManager(FleetSide.ENEMY).getDeployedCopy()){
//                        m.getCaptain().setPersonality("reckless");
//                    }
                    for (ShipAPI s : engine.getShips()) {
                        if (s.getOriginalOwner() == 1 && s.isAlive() && s.getCaptain() != null) {
                            s.getCaptain().setPersonality("reckless");
                        }
                    }

                    if (!engine.getFleetManager(FleetSide.ENEMY).getTaskManager(true).isFullAssault()) {
                        engine.getFleetManager(FleetSide.ENEMY).getTaskManager(true).setFullAssault(true);
                    }

                    if (!engine.getFleetManager(FleetSide.ENEMY).getTaskManager(false).isFullAssault()) {
                        engine.getFleetManager(FleetSide.ENEMY).getTaskManager(false).setFullAssault(true);
                    }

                    //map reveal to all
                    engine.getFogOfWar(1).revealAroundPoint(engine, 0, 0, 90000);
                    engine.getFogOfWar(0).revealAroundPoint(engine, 0, 0, 90000);
                } else {
                    if (engine.getFleetManager(FleetSide.PLAYER).getTaskManager(false).isFullAssault()) {
                        engine.getFleetManager(FleetSide.PLAYER).getTaskManager(false).setFullAssault(false);
                    }

                    if (engine.getFleetManager(FleetSide.PLAYER).getTaskManager(true).isFullAssault()) {
                        engine.getFleetManager(FleetSide.PLAYER).getTaskManager(true).setFullAssault(false);
                    }
                }
            }

            ////////////////////////////////////
            //                                //
            //         VERSUS SCREEN          //
            //                                //
            ////////////////////////////////////
            if (VS && !isShown) {
                if (engine.getTotalElapsedTime(false) > 1f) {
                    isShown = true;
                    MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_versus"),
                            MagicRender.positioning.CENTER,
                            new Vector2f(),
                            new Vector2f(),
                            new Vector2f(512, 256),
                            new Vector2f(10, 5),
                            0,
                            0,
                            Color.WHITE,
                            false,
                            0f,
                            3.2f,
                            0.2f);
                    MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_versusF"),
                            MagicRender.positioning.CENTER,
                            new Vector2f(),
                            new Vector2f(),
                            new Vector2f(512, 256),
                            new Vector2f(10, 5),
                            0,
                            0,
                            Color.WHITE,
                            true,
                            0f,
                            0f,
                            0.5f);

                    MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_player" + player),
                            MagicRender.positioning.CENTER,
                            new Vector2f(128, -160),
                            new Vector2f(-10, 0),
                            new Vector2f(512, 256),
                            new Vector2f(),
                            0,
                            0,
                            Color.WHITE,
                            false,
                            0f,
                            3f,
                            0.2f);

                    MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_player" + enemy),
                            MagicRender.positioning.CENTER,
                            new Vector2f(-128, 160),
                            new Vector2f(10, 0),
                            new Vector2f(512, 256),
                            new Vector2f(),
                            0,
                            0,
                            Color.WHITE,
                            false,
                            0f,
                            3f,
                            0.2f);
                } else if (engine.getTotalElapsedTime(false) > 0.5f) {
                    float time = 1f - engine.getTotalElapsedTime(false);

                    MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_player" + player),
                            MagicRender.positioning.CENTER,
                            new Vector2f(128 + (time * 3000), -160),
                            new Vector2f(-10, 0),
                            new Vector2f(512, 256),
                            new Vector2f(),
                            0,
                            0,
                            Color.WHITE,
                            false,
                            -1f,
                            -1f,
                            -1f);

                    MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_player" + enemy),
                            MagicRender.positioning.CENTER,
                            new Vector2f(-128 - (time * 3000), 160),
                            new Vector2f(10, 0),
                            new Vector2f(512, 256),
                            new Vector2f(),
                            0,
                            0,
                            Color.WHITE,
                            false,
                            -1f,
                            -1f,
                            -1f);
                    if (!SOUND) {
                        SOUND = true;
                        Global.getSoundPlayer().playUISound(
                                "AIB_versusS",
                                1,
                                1
                        );
                    }
                }
            }

            if (engine.isPaused()) {
                return;
            }

            /* 8th tournament code. Remember to remove later.
             ////////////////////////////////////
             //                                //
             //         CR ADJUSTMENTS         //
             //                                //
             ////////////////////////////////////

             if(!cr){
             cr=true;
             if(MAX_CR){
             for(ShipAPI s : engine.getShips()){
             s.setCurrentCR(1);
             log.info(s.getId()+" CR set to 100 percent");
             }
             } else {
             for(ShipAPI s : engine.getShips()){
             float CR = Math.max(0, Math.min(1,0.7f*s.getMutableStats().getMaxCombatReadiness().computeMultMod()));
             s.setCurrentCR(CR);
             for(String m : s.getMutableStats().getMaxCombatReadiness().getMultMods().keySet()){
             log.info(s.getHullSpec().getHullName()+" "+s.getVariant().getDisplayName()+" CR modified by "+m);
             }
             log.info(s.getHullSpec().getHullName()+" "+s.getVariant().getDisplayName()+" CR set to "+CR*100+" percent");
             }
             }
             }
             */
            ////////////////////////////////////
            //                                //
            //         ANTI-CR BATTLE         //
            //                                //
            ////////////////////////////////////
            /*
             if (engine.getTotalElapsedTime(false)>TIMEOUT){
             if(!reveal){
             reveal=true;

             //TIMEOUT screen
             MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_timeout"),
             MagicRender.positioning.CENTER,
             new Vector2f(),
             new Vector2f(),
             new Vector2f(512,256),
             new Vector2f(10,5),
             0,
             0,
             Color.WHITE,
             false,
             0.25f,
             3f,
             1f);

             //all ships reckless
             for(ShipAPI s : engine.getShips()){
             if(s.isAlive() && s.getCaptain()!=null){
             s.getCaptain().setPersonality("reckless");
             //all ships scream
             engine.addFloatingText(s.getLocation(), "WAAAAAAGH", 15, Color.RED, s, 3, 2);
             }
             }
             //both sides full assault
             //do it twice 'cuz it's maybe bugged
             if(!engine.getFleetManager(FleetSide.PLAYER).getTaskManager(false).isFullAssault()){
             engine.getFleetManager(FleetSide.PLAYER).getTaskManager(false).setFullAssault(true);
             }

             if(!engine.getFleetManager(FleetSide.PLAYER).getTaskManager(true).isFullAssault()){
             engine.getFleetManager(FleetSide.PLAYER).getTaskManager(true).setFullAssault(true);
             }

             if(!engine.getFleetManager(FleetSide.ENEMY).getTaskManager(true).isFullAssault()){
             engine.getFleetManager(FleetSide.ENEMY).getTaskManager(true).setFullAssault(true);
             }

             if(!engine.getFleetManager(FleetSide.ENEMY).getTaskManager(false).isFullAssault()){
             engine.getFleetManager(FleetSide.ENEMY).getTaskManager(false).setFullAssault(true);
             }

             }
             //map reveal to all
             engine.getFogOfWar(1).revealAroundPoint(engine, 0, 0, 90000);
             engine.getFogOfWar(0).revealAroundPoint(engine, 0, 0, 90000);
             }
             */
            ////////////////////////////////////
            //                                //
            //          ANTI-RETREAT          //
            //                                //
            ////////////////////////////////////
            if (NO_RETREAT) {

                engine.getFleetManager(FleetSide.PLAYER).getTaskManager(false).setPreventFullRetreat(true);
                engine.getFleetManager(FleetSide.PLAYER).getTaskManager(true).setPreventFullRetreat(true);
                engine.getFleetManager(FleetSide.ENEMY).getTaskManager(true).setPreventFullRetreat(true);

                for (ShipAPI ship : engine.getShips()) {
                    if (!ship.isAlive() || ship.isFighter()) {
                        continue;
                    }

                    AssignmentInfo assignment = engine.getFleetManager(ship.getOriginalOwner()).getTaskManager(false).getAssignmentFor(ship);

                    if (assignment != null) {
                        if (assignment.getType() == CombatAssignmentType.RETREAT) {
                            DeployedFleetMemberAPI dmember = engine.getFleetManager(ship.getOriginalOwner()).getDeployedFleetMember(ship);

                            if (dmember != null) {
                                engine.getFleetManager(ship.getOriginalOwner()).getTaskManager(false).orderSearchAndDestroy(dmember, false);
                                if (ship.getOriginalOwner() == 0) {
                                    bonus0++;
                                    engine.getFleetManager(0).getTaskManager(false).getCommandPointsStat().modifyFlat("flatcpbonus0", bonus0);
                                } else {
                                    bonus1++;
                                    engine.getFleetManager(1).getTaskManager(false).getCommandPointsStat().modifyFlat("flatcpbonus1", bonus1);
                                }
                            }
                        }
                    }
                }
            }

            ////////////////////////////////////
            //                                //
            //         TICKING CLOCK          //
            //                                //
            ////////////////////////////////////
            if (CLOCK) {
                if (!END) {
                    clock = (int) engine.getTotalElapsedTime(false);
                    engine.maintainStatusForPlayerShip(
                            "clock",
                            null,
                            "Timer",
                            clock + " seconds",
                            true
                    );
                } else {
                    engine.maintainStatusForPlayerShip(
                            "clock",
                            null,
                            "Timer",
                            clock + " seconds",
                            false
                    );
                }
            }

            ////////////////////////////////////
            //                                //
            //          FLEET STATUS          //
            //                                //
            ////////////////////////////////////
            if (STATUS) {
                status.advance(amount);
                if (status.intervalElapsed()) {
                    teamA = AI_missionUtils.fleetStatusUpdateDP(PLAYER_A_SHIPS);
                    //teamB = AI_missionUtils.fleetStatusUpdateDP(PLAYER_B_SHIPS);
                }
                /*
                 engine.maintainStatusForPlayerShip("status2",
                 "graphics/AIB/icons/hullsys/AI_teamSupport.png",
                 PlayerB.get(0)+" fleet status:",
                 teamB+"% DP remaining.",
                 teamB<25
                 );
                 */
                engine.maintainStatusForPlayerShip("status1",
                        "graphics/AIB/icons/hullsys/AI_teamLead.png",
                        PlayerA.get(0) + " fleet status:",
                        teamA + "% DP remaining.",
                        teamA < 25
                );

            }

            ////////////////////////////////////
            //                                //
            //         VICTORY SCREEN         //
            //                                //
            ////////////////////////////////////
            timer += engine.getElapsedInLastFrame();

            if (timer > 1 && !END) {
                timer = 0;
                int playerAlive = 0, enemyAlive = 0;

                //check for members alive
                for (ShipAPI s : engine.getShips()) {
                    if (!s.isAlive() || s.isHulk()) {
                        continue;
                    }
                    if (s.isDrone() || s.isFighter()) {
                        continue;
                    }
                    if (s.getParentStation() != null) {
                        continue;
                    }
                    if (s.getOwner() > 0) {
                        enemyAlive++;
                    } else {
                        playerAlive++;
                    }
                }

                if (playerAlive == 0) {

                    //display death wave
                    END = true;
                    engine.endCombat(3, FleetSide.ENEMY);
                    engine.getTimeMult().modifyMult("AI_mission_end", 0.33f);

                    if (WAVE == MAX_WAVE) {
                        MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_waveF"),
                                MagicRender.positioning.CENTER,
                                new Vector2f(0, 0),
                                new Vector2f(-3, 0),
                                new Vector2f(512, 256),
                                new Vector2f(10, 5),
                                0,
                                0,
                                Color.WHITE,
                                false,
                                0.25f,
                                10f,
                                1f);
                    } else {
                        MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_wave" + WAVE),
                                MagicRender.positioning.CENTER,
                                new Vector2f(0, 0),
                                new Vector2f(-3, 0),
                                new Vector2f(512, 256),
                                new Vector2f(10, 5),
                                0,
                                0,
                                Color.WHITE,
                                false,
                                0.25f,
                                10f,
                                1f);
                    }

                    MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_vanquished"),
                            MagicRender.positioning.CENTER,
                            new Vector2f(0, 0),
                            new Vector2f(3, 0),
                            new Vector2f(512, 256),
                            new Vector2f(-10, -5),
                            0,
                            0,
                            Color.WHITE,
                            false,
                            1f,
                            10f,
                            1f);

                    //battle report
                    progress.put(
                            WAVE + 1,
                            new Vector3f(
                                    0,
                                    0,
                                    waveTimer
                            )
                    );
                    log.info("--- BATTLE REPORT ---");
                    log.info(" ");
                    for (int i = 1; i <= progress.size(); i++) {
                        log.info("WAVE " + (i) + "," + progress.get(i).x + "," + progress.get(i).y + "," + progress.get(i).z);
                    }

                } else if (enemyAlive < 1 && !delay) {
                    WAVE++;
                    if (WAVE > MAX_WAVE) {
                        END = true;
                        engine.endCombat(3, FleetSide.PLAYER);
                        engine.getTimeMult().modifyMult("AI_mission_end", 0.33f);

                        //VICTORY (dude, you rock!)
                        MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_victory"),
                                MagicRender.positioning.CENTER,
                                new Vector2f(0, 0),
                                new Vector2f(0, 0),
                                new Vector2f(512, 256),
                                new Vector2f(20, 10),
                                0,
                                0,
                                Color.WHITE,
                                false,
                                0.25f,
                                10f,
                                1f);

                        //battle report
                        progress.put(
                                WAVE,
                                new Vector3f(
                                        Math.round((AI_missionUtils.fleetStatusUpdate(PLAYER_A_SHIPS, true) + AI_missionUtils.fleetStatusUpdate(PLAYER_B_SHIPS, true)) / 2),
                                        AI_missionUtils.fleetStatusUpdate(PLAYER_A_SHIPS, true),
                                        waveTimer
                                )
                        );
                        log.info("--- BATTLE REPORT ---");
                        log.info(" ");
                        for (int i = 1; i <= progress.size(); i++) {
                            log.info("WAVE " + (i) + "," + progress.get(i).x + "," + progress.get(i).y + "," + progress.get(i).z);
                        }

                    } else {

                        // NEW WAVE after a delay
                        delay = true;
                        DELAY_TIMER.setElapsed(0);
                        teamReady = false;
                        countdown = 10;
                        countdownTrigger = 9;
                        gracePeriodOn(PLAYER_A_SHIPS);
                        //gracePeriodOn(PLAYER_B_SHIPS);

                        if (WAVE == 7) {
                            Global.getSoundPlayer().playUISound("AIB_alarm", 1, 1);

                            MagicRender.screenspace(
                                    Global.getSettings().getSprite("misc", "AI_danger"),
                                    MagicRender.positioning.CENTER,
                                    new Vector2f(0, 320),
                                    new Vector2f(),
                                    new Vector2f(1920, 128),
                                    new Vector2f(),
                                    0,
                                    0,
                                    Color.white,
                                    false,
                                    0.1f,
                                    4,
                                    0.5f);
                            MagicRender.screenspace(
                                    Global.getSettings().getSprite("misc", "AI_danger"),
                                    MagicRender.positioning.CENTER,
                                    new Vector2f(0, -320),
                                    new Vector2f(),
                                    new Vector2f(1920, 128),
                                    new Vector2f(),
                                    0,
                                    0,
                                    Color.white,
                                    false,
                                    0.1f,
                                    4,
                                    0.5f);
                        }

                        //add entry to battleReport
                        progress.put(
                                WAVE,
                                new Vector3f(
                                        //Math.round((AI_missionUtils.fleetStatusUpdate(PLAYER_A_SHIPS,true)+AI_missionUtils.fleetStatusUpdate(PLAYER_B_SHIPS,true))/2),
                                        Math.round((AI_missionUtils.fleetStatusUpdate(PLAYER_A_SHIPS, true))),
                                        AI_missionUtils.fleetStatusUpdate(PLAYER_A_SHIPS, true),
                                        waveTimer
                                )
                        );
                        log.info("WAVE " + WAVE + "," + progress.get(WAVE).x + "," + progress.get(WAVE).y + "," + progress.get(WAVE).z);
                    }

                    waveTimer = 0;
                }
            }

            if (delay) {

//                //move to the center
//                if(countdown==10){
//                    for (DeployedFleetMemberAPI dfm : engine.getFleetManager(FleetSide.PLAYER).getDeployedCopyDFM()){
//                        if(dfm.canBeGivenOrders()){
//                            engine.getFleetManager(FleetSide.PLAYER).getTaskManager(dfm.isAlly()).giveAssignment(dfm, task, false);
//                        }
//                    }
//                }
                countdown -= amount;

                if (countdown <= countdownTrigger && countdownTrigger > 0) {

                    ////////////////////////////////////
                    //                                //
                    //        VISUAL COUNTDOWN        //
                    //                                //
                    ////////////////////////////////////
                    MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_counter" + countdownTrigger),
                            MagicRender.positioning.CENTER,
                            new Vector2f(0, 300),
                            new Vector2f(0, 20),
                            new Vector2f(76, 76),
                            new Vector2f(12, 12),
                            0,
                            0,
                            Color.WHITE,
                            false,
                            0.05f,
                            0.2f,
                            1);
                    countdownTrigger--;
                }

                if (countdown < 5 && !teamReady) {
                    teamReady = true;

                    ////////////////////////////////////
                    //                                //
                    //       READY PLAYER FLEET       //
                    //                                //
                    ////////////////////////////////////
                    int setCr = 100;
                    if (!MAX_CR) {
                        setCr = max_cr;
                    }
                    AI_missionUtils.prepareForNewWave(engine, new Vector2f(), new Vector2f(0.75f, 0.75f), setCr, 0.5f);

                } else if (countdown <= 0) {
                    delay = false;

                    ////////////////////////////////////
                    //                                //
                    //       NEW WAVE INCOMING        //
                    //                                //
                    ////////////////////////////////////
                    //display wave
                    if (WAVE > 5) {

//                        MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_waveF"),
//                                MagicRender.positioning.CENTER,
//                                new Vector2f(0,0),
//                                new Vector2f(0,0),
//                                new Vector2f(256,128),
//                                new Vector2f(0,0),
//                                0,
//                                0,
//                                Color.WHITE,
//                                false,
//                                0.25f,
//                                3f,
//                                1f);
                    } else {
                        MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_wave" + WAVE),
                                MagicRender.positioning.CENTER,
                                new Vector2f(0, 0),
                                new Vector2f(0, 0),
                                new Vector2f(256, 128),
                                new Vector2f(0, 0),
                                0,
                                0,
                                Color.WHITE,
                                false,
                                0.25f,
                                3f,
                                1f);
                    }

                    //turn off full assault
                    if (engine.getFleetManager(FleetSide.ENEMY).getTaskManager(true).isFullAssault()) {
                        engine.getFleetManager(FleetSide.ENEMY).getTaskManager(true).setFullAssault(false);
                    }
                    if (engine.getFleetManager(FleetSide.ENEMY).getTaskManager(false).isFullAssault()) {
                        engine.getFleetManager(FleetSide.ENEMY).getTaskManager(false).setFullAssault(false);
                    }

                    //SPAWN the new enemy ships
                    AI_missionUtils.WaveSpawning(
                            engine,
                            WAVE,
                            ROUND,
                            mapY,
                            mapX,
                            PATH
                    );

                    //remove invincibility
                    gracePeriodOff(PLAYER_A_SHIPS);
                    //gracePeriodOff(PLAYER_B_SHIPS);

                    //map reveal to all
                    engine.getFogOfWar(1).revealAroundPoint(engine, 0, 0, 90000);
                    engine.getFogOfWar(0).revealAroundPoint(engine, 0, 0, 90000);

                    //canced defend order
//                    engine.getFleetManager(FleetSide.PLAYER).getTaskManager(false).orderSearchAndDestroy();
//                    engine.getFleetManager(FleetSide.PLAYER).getTaskManager(true).orderSearchAndDestroy();
                } else if (countdown < 6 && WAVE > 5) {

                    ////////////////////////////////////
                    //                                //
                    //       BOSS FORESHADOWING       //
                    //                                //
                    ////////////////////////////////////
                    //camera override
                    AI_freeCamPlugin.cameraOverride(new Vector2f(0, mapY / 2 - 500));

                    //visual effect
                    if (countdown > 1) {
                        float intensity = 2 * MagicAnim.SO(6 - countdown, 0, 12);
                        AI_missionUtils.warpZone(engine, intensity, new Vector2f(0, mapY / 2 - 500));
                    }
                }
            }
        }
    }

    ////////////////////////////////////
    //                                //
    //           CSV READING          //
    //                                //
    ////////////////////////////////////
    private void createMatches() {
        //read round_data.csv
        log.info("Reading CSV");
        try {
            log.info("Importing data:");
            JSONArray round = Global.getSettings().getMergedSpreadsheetDataForMod("round", ROUND_DATA, "aibattles");
            //log.info("Parsing row");
            JSONObject row = round.getJSONObject(0);
            //log.info("Reading round");
            ROUND = row.getInt("round");

            //log.info("Reading battlescape ");
            BATTLESCAPE = row.getInt("battlescape");

            //log.info("Reading vsScreen");
            VS = row.getBoolean("vsScreen");

            //log.info("Reading waves");
            MAX_WAVE = row.getInt("waves");

            //log.info("Reading noRetreat");
            NO_RETREAT = row.getBoolean("noRetreat");

            ADMIRAL_AI = row.getBoolean("admiralAI");

            //log.info("Reading flagshipMode");
            FLAGSHIP = row.getBoolean("flagshipMode");

            //log.info("Reading buyInHullmods");
            HULLMOD = row.getBoolean("buyInHullmods");

            //log.info("Reading blacklist");
            BLACKLIST = row.getBoolean("blacklist");

            //log.info("Reading antiSpam");
            ANTISPAM = row.getBoolean("antiSpam");

            //log.info("Reading clock");
            CLOCK = row.getBoolean("clock");

            //log.info("Reading status");
            STATUS = row.getBoolean("status");

            //log.info("Reading maxCR");
            MAX_CR = row.getBoolean("maxCR");

            //log.info("Reading maintenance");
            MAINTENANCE = row.getBoolean("maintenance");

            //log.info("Reading supplies");
            SUPPLIES = row.getInt("supplies");

            //log.info("Reading showDP");
            SHOW_DP = row.getBoolean("showDP");

            //log.info("Reading showCost");
            SHOW_COST = row.getBoolean("showCost");

            //log.info("Reading timeout");
            TIMEOUT = row.getInt("timeout");

            if (ANTISPAM) {
                HIKE = ((float) row.getDouble("hike")) / 100;
                SPAM_THRESHOLD.put(ShipAPI.HullSize.FIGHTER, (float) row.getInt("fighterT") + (ROUND - 1) * (float) row.getInt("fighterR"));
                SPAM_THRESHOLD.put(ShipAPI.HullSize.FRIGATE, (float) row.getInt("frigateT") + (ROUND - 1) * (float) row.getInt("frigateR"));
                SPAM_THRESHOLD.put(ShipAPI.HullSize.DESTROYER, (float) row.getInt("destroyerT") + (ROUND - 1) * (float) row.getInt("destroyerR"));
                SPAM_THRESHOLD.put(ShipAPI.HullSize.CRUISER, (float) row.getInt("cruiserT") + (ROUND - 1) * (float) row.getInt("cruiserR"));
                SPAM_THRESHOLD.put(ShipAPI.HullSize.CAPITAL_SHIP, (float) row.getInt("capitalT") + (ROUND - 1) * (float) row.getInt("capitalR"));
            }

        } catch (IOException | JSONException ex) {
            log.error("unable to read " + ROUND_DATA);
        }

        //read round_matches.csv
        MATCHES.clear();
        try {
            JSONArray matches = Global.getSettings().loadCSV(ROUND_MATCHES);
            for (int i = 0; i < matches.length(); i++) {

                JSONObject row = matches.getJSONObject(i);
                player = row.getInt("playerA");
                //enemy = row.getInt("playerB");
                max_cr = row.getInt("max_cr");

                MATCHES.put(i, new Vector2f(player, max_cr));
            }
        } catch (IOException | JSONException ex) {
            log.error("unable to read " + ROUND_MATCHES);
        }

        //create the blacklist if needed
        if (BLACKLIST) {
            try {
                JSONArray blacklist = Global.getSettings().getMergedSpreadsheetDataForMod("id", ROUND_BLACKLIST, "aibattles");
                for (int i = 0; i < blacklist.length(); i++) {
                    JSONObject row = blacklist.getJSONObject(i);
                    BLOCKED.add(row.getString("id"));
                }
            } catch (IOException | JSONException ex) {
                log.error("unable to read " + ROUND_BLACKLIST);
            }
        }
    }
}
