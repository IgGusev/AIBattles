// FYI this hasn't been updated since Tart's 2020 PPvE tournament

package data.missions.PPvE_underdog;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.AssignmentTargetAPI;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.BattleObjectiveAPI;
import com.fs.starfarer.api.combat.CombatAssignmentType;
import com.fs.starfarer.api.combat.CombatEngineAPI;
//import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI.AssignmentInfo;
import com.fs.starfarer.api.combat.DeployedFleetMemberAPI;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetGoal;
//import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;
import com.fs.starfarer.api.util.IntervalUtil;
import data.missions.scripts.AI_missionUtils;
import data.missions.scripts.AI_missionUtils.FleetMemberData;
import static data.missions.scripts.AI_missionUtils.createFleetReturnRefit;
import data.scripts.plugins.AI_freeCamPlugin;
import data.scripts.plugins.AI_relocatePlugin;
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
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector2f;
import static data.missions.scripts.AI_missionUtils.cutoff;
import static data.missions.scripts.AI_missionUtils.gracePeriodOff;
import static data.missions.scripts.AI_missionUtils.gracePeriodOn;
import data.scripts.util.MagicAnim;
import org.lwjgl.util.vector.Vector3f;

public class MissionDefinition implements MissionDefinitionPlugin {
        
    public static Logger log = Global.getLogger(MissionDefinition.class);
    
    public static Map<HullSize, Float> SPAM_THRESHOLD = new HashMap<>();
    private static final Map<Integer, Vector3f> MATCHES = new HashMap<>();
    private static final Map<Integer,String> TEAMS = new HashMap<>();
    
    private static int match, player, enemy, max_cr, clock=0;
    private static boolean first=true, deploy=false, isShown=false, delay=false, teamReady=false;
    private static final List<String> BLOCKED = new ArrayList<>();
    private static Map<Integer, FleetMemberData> PLAYER_A_FLEET = new HashMap<>();
    private static Map<Integer, FleetMemberData> PLAYER_B_FLEET = new HashMap<>();
    private static final List<ShipAPI> PLAYER_A_SHIPS = new ArrayList<>();
    private static final List<ShipAPI> PLAYER_B_SHIPS = new ArrayList<>();
    
    public static final String PATH = "tournament/";
    public static final String ROUND_DATA = PATH+"round_data.csv";
    public static final String ROUND_MATCHES = PATH+"PPvE_underdog_matches.csv";
    public static final String ROUND_BLACKLIST = PATH+"tournament_blacklist.csv";
    public static final String OVERBUDGET = " ! OVERBUDGET ! ";
    
    private static int ROUND, MAX_WAVE, BATTLESCAPE, SUPPLIES, WAVE=1, SPEAKER, HULL_BUDGET, EQUIPEMENT_BUDGET, SHARED_BUDGET;
    private static float HIKE,countdown=10;
    private static boolean REVERSE, MAINTENANCE, CLOCK, STATUS, VS, ANNOUNCER, NO_RETREAT, FLAGSHIP, BLACKLIST, ANTISPAM, HULLMOD, MAX_CR, sound1=false, sound2=false, end=false;
    private static float mapX, mapY, timer=0;
    private static String prefixPlayerA, prefixPlayerB;
    
    private static int teamA=100, teamB=100, countdownTrigger=9;
    private static final IntervalUtil STATUS_CHECK = new IntervalUtil(0.2f,0.3f);
    private static final IntervalUtil DELAY_TIMER = new IntervalUtil(5,5);
    private static float waveTimer=0;
//    private static CombatFleetManagerAPI.AssignmentInfo task;
    
    private static Map<Integer, Vector3f> progress = new HashMap<>();

    @Override
    public void defineMission(MissionDefinitionAPI api) {
        
        //cleanup
        BLOCKED.clear();
        PLAYER_A_FLEET.clear();
        PLAYER_B_FLEET.clear();
        PLAYER_A_SHIPS.clear();
        PLAYER_B_SHIPS.clear();
        progress.clear();
        clock=0;
        waveTimer=0;
        SPEAKER = MathUtils.getRandomNumberInRange(1, 8);
        
        //read the matches data from CSVs
        createMatches();
        
        //cycle the matches while holding space        
        if(first){
            first = false;
            match=0;
        } else if(Keyboard.isKeyDown(Keyboard.KEY_RIGHT)){                
            match++;
            if(match>=MATCHES.size()){
                match=0;
            }
        } else if (Keyboard.isKeyDown(Keyboard.KEY_LEFT)){                     
            match--;
            if(match<0){
                match=MATCHES.size()-1;
            }
        }

        //P&P  
        //While both players are spawned together, they are displayed on the opposite side in the briefing
        max_cr=(int)MATCHES.get(match).z;
        
        player=Math.round(MATCHES.get(match).x);        
        List<String> PlayerA = AI_missionUtils.createPlayerJSON(PATH, player);
        
        api.initFleet(FleetSide.PLAYER, PlayerA.get(1), FleetGoal.ATTACK, false, 0,0);
        api.setFleetTagline(FleetSide.PLAYER, PlayerA.get(2));
        //for now, default reckless commander AI to avoid retreats
        api.getDefaultCommander(FleetSide.PLAYER).setPersonality("reckless");
        prefixPlayerA = PlayerA.get(1);
        
        PLAYER_A_FLEET = createFleetReturnRefit(api, FleetSide.PLAYER, ROUND, player, PATH, prefixPlayerA);
        
        
        enemy=Math.round(MATCHES.get(match).y);
        List<String> PlayerB = AI_missionUtils.createPlayerJSON(PATH, enemy);
        List<String> AIwaves = AI_missionUtils.createPlayerJSON(PATH, 0);
        
        api.initFleet(FleetSide.ENEMY, AIwaves.get(1), FleetGoal.ATTACK, true, 1,1);
        api.setFleetTagline(FleetSide.ENEMY, PlayerB.get(2)); 
        //for now, default reckless commander AI to avoid retreats
        api.getDefaultCommander(FleetSide.ENEMY).setPersonality("reckless");
        prefixPlayerB = PlayerB.get(1);
               
        PLAYER_B_FLEET = createFleetReturnRefit(api, FleetSide.ENEMY, ROUND, enemy, PATH, prefixPlayerB);
        
        
        Vector2f playerAcost,playerBcost;        
        playerAcost = AI_missionUtils.splitFleetCost(
                Global.getCombatEngine(),
                PlayerA.get(0),
                ROUND,
                PLAYER_A_FLEET,
                HULLMOD,
                ANTISPAM,
                HIKE,
                SPAM_THRESHOLD,
                BLACKLIST,
                BLOCKED,
                MAINTENANCE,
                SUPPLIES);  

        playerBcost = AI_missionUtils.splitFleetCost(
                Global.getCombatEngine(),
                PlayerB.get(0),
                ROUND,
                PLAYER_B_FLEET,
                HULLMOD,
                ANTISPAM,
                HIKE,
                SPAM_THRESHOLD,
                BLACKLIST,
                BLOCKED,
                MAINTENANCE,
                SUPPLIES);  
        
        //setup Briefing
        //GREEN PLAYER
        float hull_threshold=(float)HULL_BUDGET * (1+(float)SHARED_BUDGET/100) + 1;
        float equipement_threshold=(float)EQUIPEMENT_BUDGET * (1+(float)SHARED_BUDGET/100) + 1;
        if(playerAcost.x>0){
            if(playerAcost.x > hull_threshold){
                api.addBriefingItem("Green Player:  "+PlayerA.get(0)+"   "+(int)playerAcost.x+ OVERBUDGET +" / "+(int)playerAcost.y);
            } else if (playerAcost.y > equipement_threshold){
                api.addBriefingItem("Green Player:  "+PlayerA.get(0)+"   "+(int)playerAcost.x+" / "+(int)playerAcost.y+ OVERBUDGET);
            } else {
                api.addBriefingItem("Green Player:  "+PlayerA.get(0)+"   "+(int)playerAcost.x+" / "+(int)playerAcost.y);
            }
        } else if(playerAcost.x==0){
            api.addBriefingItem("Green Player:  "+PlayerA.get(0)+" : ERROR LOADING FLEET FILE!");
        } else {            
            api.addBriefingItem("Green Player:  "+PlayerA.get(0)+" : BLACKLISTED ELEMENT DETECTED!!!!");
        }
        //YELLOW PLAYER
        if(playerBcost.x>0){
            if(playerBcost.x > hull_threshold){
                api.addBriefingItem("Yellow Player:  "+PlayerB.get(0)+"   "+(int)playerBcost.x+ OVERBUDGET +" / "+(int)playerBcost.y);
            } else if (playerBcost.y > equipement_threshold){
                api.addBriefingItem("Yellow Player:  "+PlayerB.get(0)+"   "+(int)playerBcost.x+" / "+(int)playerBcost.y+ OVERBUDGET);
            } else {
                api.addBriefingItem("Yellow Player:  "+PlayerB.get(0)+"   "+(int)playerBcost.x+" / "+(int)playerBcost.y);
            }
        } else if(playerBcost.x==0){
            api.addBriefingItem("Yellow Player:  "+PlayerB.get(0)+" : ERROR LOADING FLEET FILE!");            
        } else {    
            api.addBriefingItem("Yellow Player: "+PlayerB.get(0)+" : BLACKLISTED ELEMENT DETECTED!!!!");
        }
        //TEAM
        if(playerAcost.x+playerBcost.x > 2*HULL_BUDGET){
            api.addBriefingItem("Match "+AI_missionUtils.matchLetter(Math.round(MATCHES.get(match).x), REVERSE)+"   "+TEAMS.get(match)+" combined cost: "+(int)(playerAcost.x+playerBcost.x)+ OVERBUDGET +" / "+(int)(playerAcost.y+playerBcost.y));
        } else if (playerAcost.y+playerBcost.y > 2*EQUIPEMENT_BUDGET){
            api.addBriefingItem("Match "+AI_missionUtils.matchLetter(Math.round(MATCHES.get(match).x), REVERSE)+"   "+TEAMS.get(match)+" combined cost: "+(int)(playerAcost.x+playerBcost.x)+" / "+(int)(playerAcost.y+playerBcost.y)+ OVERBUDGET );
        } else {        
            api.addBriefingItem("Match "+AI_missionUtils.matchLetter(Math.round(MATCHES.get(match).x), REVERSE)+"   "+TEAMS.get(match)+" combined cost: "+(int)(playerAcost.x+playerBcost.x)+" / "+(int)(playerAcost.y+playerBcost.y));
        }
        
        //set the terrain
        AI_missionUtils.setBattlescape(api, BATTLESCAPE);        
        
        //add the price check and anti-retreat plugin
        api.addPlugin(new Plugin());
        
//        //trick to have the briefieg side clearer:
//        for(int i=0; i<PLAYER_B_FLEET.size(); i++){
//            FleetMemberAPI m = PLAYER_B_FLEET.get(i).MEMBER;
//            m.setOwner(1);
//            m.updateStats();
//        }
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
            
            //removes deployment points to prevent the AI to deploy the "player ships" showed on its side in the briefing
            engine.setMaxFleetPoints(FleetSide.ENEMY, 0);
            engine.setMaxFleetPoints(FleetSide.PLAYER, 0);
            
            mapX=engine.getMapWidth();
            mapY=engine.getMapHeight();            
            
            deploy=false;
            sound1=false;
            sound2=false;
            isShown=false;
            end=false;
            
            WAVE=1;
            
            //tweak commander AI
            engine.getFleetManager(FleetSide.PLAYER).getFleetCommander().setPersonality("reckless");
            engine.getFleetManager(FleetSide.PLAYER).getTaskManager(false).setPreventFullRetreat(NO_RETREAT);
            engine.getFleetManager(FleetSide.PLAYER).getTaskManager(true).setPreventFullRetreat(NO_RETREAT);
            engine.getFleetManager(FleetSide.ENEMY).getFleetCommander().setPersonality("reckless");
            engine.getFleetManager(FleetSide.ENEMY).getTaskManager(false).setPreventFullRetreat(NO_RETREAT);
            engine.getFleetManager(FleetSide.ENEMY).getTaskManager(true).setPreventFullRetreat(NO_RETREAT);
            engine.getFleetManager(FleetSide.PLAYER).getTaskManager(true).getCommandPointsStat().modifyMult("tournament", 0);
            engine.getFleetManager(FleetSide.PLAYER).getTaskManager(false).getCommandPointsStat().modifyMult("tournament", 0);
//            engine.getFleetManager(FleetSide.ENEMY).getTaskManager(true).getCommandPointsStat().modifyMult("tournament", 0);
//            engine.getFleetManager(FleetSide.ENEMY).getTaskManager(false).getCommandPointsStat().modifyMult("tournament", 0);
            
                
                
//            //swap player B fleet to player's side
//            for(int i=0; i<PLAYER_B_FLEET.size(); i++){
//                FleetMemberAPI m = PLAYER_B_FLEET.get(i).MEMBER;
//                m.setAlly(true);
//                m.setOwner(0);
//                m.updateStats();
//                engine.getFleetManager(FleetSide.PLAYER).addToReserves(m);
//                engine.getFleetManager(FleetSide.ENEMY).removeFromReserves(m);
//            }
//            engine.getFleetManager(FleetSide.ENEMY).getReservesCopy().clear();
        }
        
//        private int bonus0 = 0;
//        private int bonus1 = 0;
        
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
            
            if(!deploy){
                
                deploy=true;                   
                
                int setCr=100;
                if(!MAX_CR){
                    setCr=max_cr;
                }
                AI_missionUtils.ForcedSideSpawnWithAlly(engine, FleetSide.PLAYER, mapX, mapY, true, PLAYER_A_FLEET, PLAYER_B_FLEET, setCr);                
                
                //setup the defend assignment
//                AssignmentTargetAPI point = engine.getFleetManager(FleetSide.PLAYER).createWaypoint(new Vector2f(), false);
//                task = engine.getFleetManager(FleetSide.PLAYER).getTaskManager(false).createAssignment(CombatAssignmentType.DEFEND, point, false);
                
                for(BattleObjectiveAPI o : engine.getObjectives()){
//                    AssignmentTargetAPI point = engine.getFleetManager(FleetSide.PLAYER).createWaypoint(o.getLocation(), false);
                    AssignmentTargetAPI point = (AssignmentTargetAPI)o;
                    engine.getFleetManager(FleetSide.PLAYER).getTaskManager(false).createAssignment(CombatAssignmentType.CONTROL, point, false);
                }

                //fleet status list
                for(int i=0; i<PLAYER_A_FLEET.size(); i++){
                    if(PLAYER_A_FLEET.containsKey(i)){
                        PLAYER_A_SHIPS.add(engine.getFleetManager(FleetSide.PLAYER).getShipFor(PLAYER_A_FLEET.get(i).MEMBER));
                    }
                }
                for(int i=0; i<PLAYER_B_FLEET.size(); i++){
                    if(PLAYER_B_FLEET.containsKey(i)){
                        PLAYER_B_SHIPS.add(engine.getFleetManager(FleetSide.PLAYER).getShipFor(PLAYER_B_FLEET.get(i).MEMBER));
                    }
                }
                
                //SPAWN the starting enemy ships
                AI_missionUtils.WaveSpawning(
                        engine,
                        WAVE,
                        ROUND,
                        mapY-1000,
                        mapX,
                        PATH
                );
                
                //removes deployment points to prevent the AI to deploy the "player ships" showed on its side in the briefing
                engine.setMaxFleetPoints(FleetSide.ENEMY, 0);
                engine.setMaxFleetPoints(FleetSide.PLAYER, 0);
                
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
            
            if(!engine.isPaused()){
                waveTimer+=amount;
                if(waveTimer>60){
                    if(!engine.getFleetManager(FleetSide.PLAYER).getTaskManager(false).isFullAssault()){
                        engine.getFleetManager(FleetSide.PLAYER).getTaskManager(false).setFullAssault(true);
                    }
                    
                    if(!engine.getFleetManager(FleetSide.PLAYER).getTaskManager(true).isFullAssault()){
                        engine.getFleetManager(FleetSide.PLAYER).getTaskManager(true).setFullAssault(true);
                    }
                    
//                    for(FleetMemberAPI m : engine.getFleetManager(FleetSide.ENEMY).getDeployedCopy()){
//                        m.getCaptain().setPersonality("reckless");
//                    }
                    
                    for(ShipAPI s : engine.getShips()){
                        if(s.getOriginalOwner()==1 && s.isAlive() && s.getCaptain()!=null){
                            s.getCaptain().setPersonality("reckless");
                        }
                    }
                    
                    if(!engine.getFleetManager(FleetSide.ENEMY).getTaskManager(true).isFullAssault()){
                        engine.getFleetManager(FleetSide.ENEMY).getTaskManager(true).setFullAssault(true);
                    }
                    
                    if(!engine.getFleetManager(FleetSide.ENEMY).getTaskManager(false).isFullAssault()){
                        engine.getFleetManager(FleetSide.ENEMY).getTaskManager(false).setFullAssault(true);
                    }
                } else {
                    if(engine.getFleetManager(FleetSide.PLAYER).getTaskManager(false).isFullAssault()){
                        engine.getFleetManager(FleetSide.PLAYER).getTaskManager(false).setFullAssault(false);
                    }
                    
                    if(engine.getFleetManager(FleetSide.PLAYER).getTaskManager(true).isFullAssault()){
                        engine.getFleetManager(FleetSide.PLAYER).getTaskManager(true).setFullAssault(false);
                    }
                }
            }
            
            ////////////////////////////////////
            //                                //
            //         VERSUS SCREEN          //
            //                                //
            ////////////////////////////////////
            
            if(VS && !isShown){
                if(engine.getTotalElapsedTime(false)>1f){
                    isShown=true;
                    MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_team"),
                            MagicRender.positioning.CENTER,
                            new Vector2f(),
                            new Vector2f(),
                            new Vector2f(512,256),
                            new Vector2f(10,5),
                            0,
                            0,
                            Color.WHITE,
                            false,
                            0f, 
                            3.2f, 
                            0.2f);
                    MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_teamF"),
                            MagicRender.positioning.CENTER,
                            new Vector2f(),
                            new Vector2f(),
                            new Vector2f(512,256),
                            new Vector2f(10,5),
                            0,
                            0,
                            Color.WHITE,
                            true,
                            0f, 
                            0f, 
                            0.5f);

                    MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_player"+player),
                            MagicRender.positioning.CENTER,
                            new Vector2f(128,-160),
                            new Vector2f(-10,0),
                            new Vector2f(512,256),
                            new Vector2f(),
                            0,
                            0,
                            Color.WHITE,
                            false,
                            0f, 
                            3f, 
                            0.2f);

                    MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_player"+enemy),
                            MagicRender.positioning.CENTER,
                            new Vector2f(-128,160),
                            new Vector2f(10,0),
                            new Vector2f(512,256),
                            new Vector2f(),
                            0,
                            0,
                            Color.WHITE,
                            false,
                            0f, 
                            3f, 
                            0.2f);
                    
                    if(ANNOUNCER && !sound2){
                        sound2=true;
                        Global.getSoundPlayer().playUISound(
                                "AIB_"+SPEAKER+"_start",
                                1,
                                1
                        );
                    }
                }else if(engine.getTotalElapsedTime(false)>0.5f){
                    float time = 1f - engine.getTotalElapsedTime(false);
                    
                    MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_player"+player),
                            MagicRender.positioning.CENTER,
                            new Vector2f(128+(time*3000),-160),
                            new Vector2f(-10,0),
                            new Vector2f(512,256),
                            new Vector2f(),
                            0,
                            0,
                            Color.WHITE,
                            false,
                            -1f, 
                            -1f, 
                            -1f);

                    MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_player"+enemy),
                            MagicRender.positioning.CENTER,
                            new Vector2f(-128-(time*3000),160),
                            new Vector2f(10,0),
                            new Vector2f(512,256),
                            new Vector2f(),
                            0,
                            0,
                            Color.WHITE,
                            false,
                            -1f, 
                            -1f, 
                            -1f);
                    if(!sound1){
                        sound1=true;
                        Global.getSoundPlayer().playUISound(
                                "AIB_versusS",
                                1,
                                1
                        );
                    }
                }
            }
            
            if(engine.isPaused()){return;}
            
            ////////////////////////////////////
            //                                //
            //          ANTI-RETREAT          //
            //                                //
            ////////////////////////////////////    
            
            if(NO_RETREAT){                
                
                engine.getFleetManager(FleetSide.PLAYER).getTaskManager(false).setPreventFullRetreat(true);
                engine.getFleetManager(FleetSide.PLAYER).getTaskManager(true).setPreventFullRetreat(true);
                engine.getFleetManager(FleetSide.ENEMY).getTaskManager(true).setPreventFullRetreat(true);                
                
                for (ShipAPI ship : engine.getShips()) {
                    if (!ship.isAlive()||ship.isFighter()) {
                        continue;
                    }

                    AssignmentInfo assignment = engine.getFleetManager(ship.getOriginalOwner()).getTaskManager(false).getAssignmentFor(ship);

                    if (assignment != null) {
                        if (assignment.getType() == CombatAssignmentType.RETREAT) {
                            DeployedFleetMemberAPI dmember = engine.getFleetManager(ship.getOriginalOwner()).getDeployedFleetMember(ship);

                            if (dmember != null) {
                                engine.getFleetManager(ship.getOriginalOwner()).getTaskManager(dmember.isAlly()).orderSearchAndDestroy(dmember, false);
//                                if (ship.getOriginalOwner() == 0) {
//                                    bonus0++;
//                                    engine.getFleetManager(0).getTaskManager(false).getCommandPointsStat().modifyFlat("flatcpbonus0", bonus0);
//                                } else {
//                                    bonus1++;
//                                    engine.getFleetManager(1).getTaskManager(false).getCommandPointsStat().modifyFlat("flatcpbonus1", bonus1);
//                                }
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
            
            if(CLOCK){
                if(!end){
                    clock=(int)engine.getTotalElapsedTime(false);
                    if(waveTimer>240){
                        engine.maintainStatusForPlayerShip(
                                "clock",
                                null,
                                "Timer",
                                clock+" seconds",
                                true
                        );
                    } else {
                        engine.maintainStatusForPlayerShip(
                                "clock",
                                null,
                                "Timer",
                                clock+" seconds",
                                false
                        );
                    }
                } else {
                    engine.maintainStatusForPlayerShip(
                            "clock",
                            null,
                            "Timer",
                            clock+" seconds",
                            false
                    );
                }
            }
            
            ////////////////////////////////////
            //                                //
            //          FLEET STATUS          //
            //                                //
            ////////////////////////////////////            
            
            if(STATUS){
                STATUS_CHECK.advance(amount);
                if(STATUS_CHECK.intervalElapsed()){
                    teamB = AI_missionUtils.fleetStatusUpdate(PLAYER_B_SHIPS,false);
                    teamA = AI_missionUtils.fleetStatusUpdate(PLAYER_A_SHIPS,false);
                }            
                engine.maintainStatusForPlayerShip(
                        "status2",
                        "graphics/AIB/icons/hullsys/AI_teamSupport.png",
                        "Yellow fleet status:",
                        teamB+"% remaining.",
                        teamB<25
                );
                engine.maintainStatusForPlayerShip(
                        "status1",
                        "graphics/AIB/icons/hullsys/AI_teamLead.png",
                        "Green fleet status:",
                        teamA+"% remaining.",
                        teamA<25
                );
            }
            
            ////////////////////////////////////
            //                                //
            //         VICTORY SCREEN         //
            //                                //
            ////////////////////////////////////
            
            timer+=engine.getElapsedInLastFrame();

            if(timer>1&& !end){
                timer=0;
                int playerAlive=0, enemyAlive=0;

                //check for members alive
                for(ShipAPI s : engine.getShips()){
                    if(!s.isAlive()||s.isHulk())continue;
                    if(s.isDrone()||s.isFighter())continue;
                    if(s.getParentStation()!=null)continue;
                    if(s.getOwner()>0){
                        enemyAlive++;
                    }else{
                        playerAlive++;
                    }
                }

                if(playerAlive==0){
                    
                    //display death wave
                    end=true;                    
                    engine.endCombat(3, FleetSide.ENEMY);
                    engine.getTimeMult().modifyMult("AI_mission_end", 0.33f);
                    
                    if(WAVE==MAX_WAVE){
                        MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_waveF"),
                                MagicRender.positioning.CENTER,
                                new Vector2f(0,0),
                                new Vector2f(-3,0),
                                new Vector2f(512,256),
                                new Vector2f(10,5),
                                0,
                                0,
                                Color.WHITE,
                                false,
                                0.25f, 
                                10f, 
                                1f);                        
                    } else {
                        MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_wave"+WAVE),
                                MagicRender.positioning.CENTER,
                                new Vector2f(0,0),
                                new Vector2f(-3,0),
                                new Vector2f(512,256),
                                new Vector2f(10,5),
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
                            new Vector2f(0,0),
                            new Vector2f(3,0),
                            new Vector2f(512,256),
                            new Vector2f(-10,-5),
                            0,
                            0,
                            Color.WHITE,
                            false,
                            1f, 
                            10f, 
                            1f);      
                    
                    if(ANNOUNCER){
                        if(WAVE==1){
                            Global.getSoundPlayer().playUISound(
                                    "AIB_"+SPEAKER+"_shame",
                                    1,
                                    1
                            );                          
                        } else {
                            Global.getSoundPlayer().playUISound(
                                    "AIB_"+SPEAKER+"_defeat",
                                    1,
                                    1
                            );  
                        }
                    }
                    
                    //battle report
                    progress.put(
                            WAVE+1,
                            new Vector3f(
                                    0,
                                    0,
                                    waveTimer
                            )
                    );
                    log.info("--- BATTLE REPORT ---");
                    log.info(" ");
                    for(int i=1; i<=progress.size(); i++){
                        log.info("WAVE "+(i)+","+progress.get(i).x+","+progress.get(i).y+","+progress.get(i).z);
                    }
                        
                } else if(enemyAlive<1 && !delay){
                    WAVE++;  
                    if(WAVE>MAX_WAVE){                    
                        end=true;
                        engine.endCombat(3, FleetSide.PLAYER);
                        engine.getTimeMult().modifyMult("AI_mission_end", 0.33f);
                        
                        //VICTORY (dude, you rock!)
                        MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_victory"),
                                MagicRender.positioning.CENTER,
                                new Vector2f(0,0),
                                new Vector2f(0,0),
                                new Vector2f(512,256),
                                new Vector2f(20,10),
                                0,
                                0,
                                Color.WHITE,
                                false,
                                0.25f, 
                                10f, 
                                1f);
                        
                        if(ANNOUNCER){
                            Global.getSoundPlayer().playUISound(
                                    "AIB_"+SPEAKER+"_victory",
                                    1,
                                    1
                            );       
                        }
                        
                        //battle report
                        progress.put(
                                WAVE,
                                new Vector3f(
                                        Math.round((AI_missionUtils.fleetStatusUpdate(PLAYER_A_SHIPS,true)+AI_missionUtils.fleetStatusUpdate(PLAYER_B_SHIPS,true))/2),
                                        AI_missionUtils.fleetStatusUpdate(PLAYER_A_SHIPS,true),
                                        waveTimer
                                )
                        );
                        log.info("--- BATTLE REPORT ---");
                        log.info(" ");
                        for(int i=1; i<=progress.size(); i++){
                            log.info("WAVE "+(i)+","+progress.get(i).x+","+progress.get(i).y+","+progress.get(i).z);
                        }
                        
                    } else {
                        
                        // NEW WAVE after a delay
                        delay=true;
                        DELAY_TIMER.setElapsed(0);
                        teamReady=false; 
                        countdown=10;
                        countdownTrigger=9;
                        gracePeriodOn(PLAYER_A_SHIPS);
                        gracePeriodOn(PLAYER_B_SHIPS);
                        
                        if(WAVE==7){
                            Global.getSoundPlayer().playUISound("AIB_alarm", 1, 1);
                            MagicRender.screenspace(
                                    Global.getSettings().getSprite("misc", "AI_danger"),
                                    MagicRender.positioning.CENTER,
                                    new Vector2f(0,320),
                                    new Vector2f(),
                                    new Vector2f(1920,128),
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
                                    new Vector2f(0,-320),
                                    new Vector2f(),
                                    new Vector2f(1920,128),
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
                                        Math.round((AI_missionUtils.fleetStatusUpdate(PLAYER_A_SHIPS,true)+AI_missionUtils.fleetStatusUpdate(PLAYER_B_SHIPS,true))/2),
                                        AI_missionUtils.fleetStatusUpdate(PLAYER_A_SHIPS,true),
                                        waveTimer
                                )
                        );
                        log.info("WAVE "+WAVE+","+progress.get(WAVE).x+","+progress.get(WAVE).y+","+progress.get(WAVE).z);
                    }
                    
                    waveTimer=0;
                } 
            }
            
            if(delay){
                
//                //move to the center
//                if(countdown==10){
//                    for (DeployedFleetMemberAPI dfm : engine.getFleetManager(FleetSide.PLAYER).getDeployedCopyDFM()){
//                        if(dfm.canBeGivenOrders()){
//                            engine.getFleetManager(FleetSide.PLAYER).getTaskManager(dfm.isAlly()).giveAssignment(dfm, task, false);
//                        }
//                    }
//                }
                
                countdown-=amount;
                
                if(countdown<=countdownTrigger && countdownTrigger>0){
                    
                    ////////////////////////////////////
                    //                                //
                    //        VISUAL COUNTDOWN        //
                    //                                //
                    ////////////////////////////////////  
                    
                    MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_counter"+countdownTrigger),
                                MagicRender.positioning.CENTER,
                                new Vector2f(0,300),
                                new Vector2f(0,20),
                                new Vector2f(76,76),
                                new Vector2f(12,12),
                                0,
                                0,
                                Color.WHITE,
                                false,
                                0.05f, 
                                0.2f, 
                                1);
                    countdownTrigger--;
                }

                if(countdown<5 && !teamReady){
                    teamReady=true;

                    ////////////////////////////////////
                    //                                //
                    //       READY PLAYER FLEET       //
                    //                                //
                    ////////////////////////////////////  

                    int setCr=100;
                    if(!MAX_CR){
                        setCr=max_cr;
                    }
                    AI_missionUtils.prepareForNewWave(engine, new Vector2f(), new Vector2f(0.75f,0.75f), setCr, 0.5f);

                } else if(countdown<=0){
                    delay=false;

                    ////////////////////////////////////
                    //                                //
                    //       NEW WAVE INCOMING        //
                    //                                //
                    ////////////////////////////////////  
                    
                    //display wave
                    if(WAVE>5){
                        
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

                        if(ANNOUNCER){                            
                            Global.getSoundPlayer().playUISound(
                                    "AIB_"+SPEAKER+"_last",
                                    1,
                                    1
                            );
                        }
                    } else {                            
                        MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_wave"+WAVE),
                                MagicRender.positioning.CENTER,
                                new Vector2f(0,0),
                                new Vector2f(0,0),
                                new Vector2f(256,128),
                                new Vector2f(0,0),
                                0,
                                0,
                                Color.WHITE,
                                false,
                                0.25f, 
                                3f, 
                                1f);
                    }

                    //turn off full assault
                    if(engine.getFleetManager(FleetSide.ENEMY).getTaskManager(true).isFullAssault()){
                        engine.getFleetManager(FleetSide.ENEMY).getTaskManager(true).setFullAssault(false);
                    }
                    if(engine.getFleetManager(FleetSide.ENEMY).getTaskManager(false).isFullAssault()){
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
                    gracePeriodOff(PLAYER_B_SHIPS);
                    //canced defend order
//                    engine.getFleetManager(FleetSide.PLAYER).getTaskManager(false).orderSearchAndDestroy();
//                    engine.getFleetManager(FleetSide.PLAYER).getTaskManager(true).orderSearchAndDestroy();
                    
                    if(WAVE>5){
                        AI_freeCamPlugin.cameraForceDrag();
                    }
                    
                } else if(countdown<6 && WAVE>5){
                    
                    ////////////////////////////////////
                    //                                //
                    //       BOSS FORESHADOWING       //
                    //                                //
                    ////////////////////////////////////  
                    
                    //camera override
                    AI_freeCamPlugin.cameraOverride(new Vector2f(0, mapY/2 - 500));
                    
                    //visual effect
                    if(countdown>1){
                        float intensity = 2*MagicAnim.SO(6-countdown, 0, 12);
                        AI_missionUtils.warpZone(engine, intensity, new Vector2f(0, mapY/2 - 500));
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
    
    private void createMatches(){
        //read round_data.csv
        try {
            JSONArray round = Global.getSettings().loadCSV(ROUND_DATA);
            
            JSONObject row = round.getJSONObject(0);
            ROUND=row.getInt("round");
            REVERSE=row.getBoolean("reverse");
            BATTLESCAPE=row.getInt("battlescape");
            VS=row.getBoolean("vsScreen");
            ANNOUNCER=row.getBoolean("announcer");
            MAX_WAVE=row.getInt("waves");
            NO_RETREAT=row.getBoolean("noRetreat");     
            FLAGSHIP=row.getBoolean("flagshipMode");
            HULLMOD=row.getBoolean("buyInHullmods");
            BLACKLIST=row.getBoolean("blacklist");
            ANTISPAM=row.getBoolean("antiSpam");
            CLOCK=row.getBoolean("clock");
            STATUS=row.getBoolean("status");
            MAX_CR=row.getBoolean("maxCR");
            MAINTENANCE=row.getBoolean("maintenance");
            SUPPLIES=row.getInt("supplies");
            HULL_BUDGET=row.getInt("hullBudget");
            EQUIPEMENT_BUDGET=row.getInt("equipementBudget");
            SHARED_BUDGET=row.getInt("sharedBudgetPercent");  
            
            if(ANTISPAM){
                HIKE=((float)row.getDouble("hike"))/100;
                SPAM_THRESHOLD.put(ShipAPI.HullSize.FIGHTER, (float)row.getInt("fighterT")+(ROUND-1)*(float)row.getInt("fighterR"));
                SPAM_THRESHOLD.put(ShipAPI.HullSize.FRIGATE, (float)row.getInt("frigateT")+(ROUND-1)*(float)row.getInt("frigateR"));
                SPAM_THRESHOLD.put(ShipAPI.HullSize.DESTROYER, (float)row.getInt("destroyerT")+(ROUND-1)*(float)row.getInt("destroyerR"));
                SPAM_THRESHOLD.put(ShipAPI.HullSize.CRUISER, (float)row.getInt("cruiserT")+(ROUND-1)*(float)row.getInt("cruiserR"));
                SPAM_THRESHOLD.put(ShipAPI.HullSize.CAPITAL_SHIP, (float)row.getInt("capitalT")+(ROUND-1)*(float)row.getInt("capitalR"));                
            }

        } catch (IOException | JSONException ex) {
            log.error("unable to read round_data.csv");
        }  
        
        //read round_matches.csv
        MATCHES.clear();
        try {
            JSONArray matches = Global.getSettings().loadCSV(ROUND_MATCHES);
            for(int i = 0; i < matches.length(); i++) {
            
                JSONObject row = matches.getJSONObject(i);
                player = row.getInt("playerA");
                enemy = row.getInt("playerB");
                max_cr = row.getInt("max_cr");
                String team = cutoff(row.getString("teamName"),42);
                
                if(REVERSE){
                    MATCHES.put(2*i, new Vector3f(player,enemy,max_cr)); 
                    MATCHES.put(2*i+1, new Vector3f(enemy,player,max_cr)); 
                    TEAMS.put(2*i, team);
                    TEAMS.put(2*i+1, team);
                } else {
                    MATCHES.put(i, new Vector3f(player,enemy,max_cr));  
                    TEAMS.put(i, team);
                }
            }
        } catch (IOException | JSONException ex) {
            log.error("unable to read round_matches.csv");
        }
        
        //create the blacklist if needed
        if(BLACKLIST){
            try {
                JSONArray blacklist = Global.getSettings().getMergedSpreadsheetDataForMod("id", ROUND_BLACKLIST, "aibattles");
                for(int i = 0; i < blacklist.length(); i++) {
                    JSONObject row = blacklist.getJSONObject(i);
                    BLOCKED.add(row.getString("id"));
                }
            } catch (IOException | JSONException ex) {
                log.error("unable to read round_blacklist.csv");                
            }
        }
    }
}