// FYI this hasn't been updated since I don't even know how long

package data.missions.PvP;

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
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;
import com.fs.starfarer.api.util.IntervalUtil;
import data.missions.scripts.AI_missionUtils;
import static data.missions.scripts.AI_missionUtils.createFleetReturnRefit;
import static data.missions.scripts.AI_missionUtils.cutoff;
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
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector2f;

public class MissionDefinition implements MissionDefinitionPlugin {
        
    public static Logger log = Global.getLogger(MissionDefinition.class);
    
    public static Map<HullSize, Float> SPAM_THRESHOLD = new HashMap<>();
    private static final Map<Integer, Vector2f> MATCHES = new HashMap<>();
    
    private static int match, player, enemy, clock=0;
    private static boolean first=true, reveal=false, deploy=false, cr=false, isShown=false;
    private static final List<String> BLOCKED = new ArrayList<>();
    private static Map<Integer, AI_missionUtils.FleetMemberData> PLAYER_A_FLEET = new HashMap<>();
    private static Map<Integer, AI_missionUtils.FleetMemberData> PLAYER_B_FLEET = new HashMap<>();
    private static final List<ShipAPI> PLAYER_A_SHIPS = new ArrayList<>();
    private static final List<ShipAPI> PLAYER_B_SHIPS = new ArrayList<>();    

    private static List<String> PlayerA = new ArrayList<>();
    private static List<String> PlayerB = new ArrayList<>();    
    
    public static final String PATH = "tournament/";
    public static final String ROUND_DATA = PATH+"round_data.csv";
    public static final String ROUND_MATCHES = PATH+"PvP_matches.csv";
    public static final String ROUND_BLACKLIST = PATH+"tournament_blacklist.csv";
    public static final String OVERBUDGET = " ! OVERBUDGET ! ";
    
    private static int ROUND, MAX_WAVE, BATTLESCAPE, WINNER=-1, LOSER=-1, SUPPLIES;
    private static float HIKE;
    private static boolean MAINTENANCE, CLOCK, VS, NO_RETREAT, FLAGSHIP, BLACKLIST, ANTISPAM, HULLMOD, MAX_CR, SOUND=false, END=false, SHAME=false;
    private static boolean STATUS, SHOW_DP, SHOW_COST;
    private static float mapX, mapY, timer=0;
    private static String prefixPlayerA, prefixPlayerB;
    
    private static int teamA=100, teamB=100;
    private static final IntervalUtil status = new IntervalUtil(0.2f,0.3f);

    //New in 11th
    private static int CR=70, TIMEOUT;
    
    @Override
    public void defineMission(MissionDefinitionAPI api) {
        
        //cleanup
        BLOCKED.clear();
        PLAYER_A_FLEET.clear();
        PLAYER_B_FLEET.clear();
        PLAYER_A_SHIPS.clear();
        PLAYER_B_SHIPS.clear();
        clock=0;
        reveal=false;//fixes timeout bug
        cr=false;//fixes CR only being applied once
        
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
        
        
        //PvP    
        
        //player number
        player=Math.round(MATCHES.get(match).x);        
        PlayerA = AI_missionUtils.createPlayerJSON(PATH, player);
        
        api.initFleet(FleetSide.PLAYER, PlayerA.get(1), FleetGoal.ATTACK, true, 1);
        api.setFleetTagline(FleetSide.PLAYER, PlayerA.get(2));

        prefixPlayerA = PlayerA.get(1);
        
        PLAYER_A_FLEET = createFleetReturnRefit(api, FleetSide.PLAYER, ROUND, player, PATH, prefixPlayerA);
        
        log.info("________________________________");
        log.info("Player A Name: "+PlayerA.get(0));
        log.info("Player A Prefix: "+PlayerA.get(1));
        log.info("Player A Tag: "+PlayerA.get(2));
        log.info("________________________________");
        
        enemy=Math.round(MATCHES.get(match).y);
        PlayerB = AI_missionUtils.createPlayerJSON(PATH, enemy);
        
        api.initFleet(FleetSide.ENEMY, PlayerB.get(1), FleetGoal.ATTACK, true, 1);
        api.setFleetTagline(FleetSide.ENEMY, PlayerB.get(2)); 

        prefixPlayerB = PlayerB.get(1);
               
        PLAYER_B_FLEET = createFleetReturnRefit(api, FleetSide.ENEMY, ROUND, enemy, PATH, prefixPlayerB);

        log.info("________________________________");
        log.info("Player B Name: "+PlayerB.get(0));
        log.info("Player B Prefix: "+PlayerB.get(1));
        log.info("Player B Tag: "+PlayerB.get(2));   
        log.info("________________________________");
        
        int playerDPCost = AI_missionUtils.DPFleetCostWithMemberData(Global.getCombatEngine(),
                PlayerA.get(0),
                PLAYER_A_FLEET,
                BLACKLIST,
                BLOCKED
        );
        
        int enemyDPCost = AI_missionUtils.DPFleetCostWithMemberData(Global.getCombatEngine(),
                PlayerB.get(0),
                PLAYER_B_FLEET,
                BLACKLIST,
                BLOCKED
        );
        
        //setup Briefing
        if(playerDPCost>0){//negative values indicate error, probably blacklist
            String costOuput = "";
            if(SHOW_DP){
                costOuput = costOuput + PlayerA.get(0)+" DP : "+playerDPCost;
            }
            /*
            if(SHOW_COST){
                costOuput = costOuput +"    Credits : "+playerCost;
            }
            */
            api.addBriefingItem(costOuput);
        } else {
            String errorMessage;
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
                default:
                    errorMessage = "NO SHIPS DETECTED!!!";
            }
            api.addBriefingItem(PlayerA.get(0)+" : "+errorMessage);
        }
        api.addBriefingItem("  VERSUS");
        if(enemyDPCost>0){
            String costOuput = "";
            if(SHOW_DP){
                costOuput = costOuput + PlayerB.get(0)+" DP : "+enemyDPCost;
            }
            /*
            if(SHOW_COST){
                costOuput = costOuput +"    Credits : "+enemyCost;
            }
            */
            api.addBriefingItem(costOuput);
        } else {
            String errorMessage;
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
                default:
                    errorMessage = "NO SHIPS DETECTED!!!";
            }
            api.addBriefingItem(PlayerA.get(0)+" : "+errorMessage);
        }
        
        //set the terrain
        AI_missionUtils.setBattlescape(api, BATTLESCAPE);        
        
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
            
            mapX=engine.getMapWidth();
            mapY=engine.getMapHeight();            
            
            deploy=false;
            SOUND=false;
            
            isShown=false;
            END=false;
            SHAME=false;
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
            
            if(!deploy){
                
                deploy=true;                
                    
                log.info("Map size: "+(int)mapX+"x"+(int)mapY);  
                
                //public static void ForcedSpawn(CombatEngineAPI engine, FleetSide side, float mapX, float mapY, boolean suppressMessage, int setCR)
                AI_missionUtils.ForcedSpawn(engine, FleetSide.PLAYER, mapX, mapY, true, CR);                
                AI_missionUtils.ForcedSpawn(engine, FleetSide.ENEMY, mapX, mapY, true, CR);
                
                //fleet status list
                for(int i=0; i<PLAYER_A_FLEET.size(); i++){
                    if(PLAYER_A_FLEET.containsKey(i)){
                        PLAYER_A_SHIPS.add(engine.getFleetManager(FleetSide.PLAYER).getShipFor(PLAYER_A_FLEET.get(i).MEMBER));
                    }
                }
                for(int i=0; i<PLAYER_B_FLEET.size(); i++){
                    if(PLAYER_B_FLEET.containsKey(i)){
                        PLAYER_B_SHIPS.add(engine.getFleetManager(FleetSide.ENEMY).getShipFor(PLAYER_B_FLEET.get(i).MEMBER));
                    }
                }
    
                //removes deployment points to prevent the AI to deploy the "player ships" showed on its side in the briefing
                engine.setMaxFleetPoints(FleetSide.ENEMY, 0);
                engine.setMaxFleetPoints(FleetSide.PLAYER, 0);
                
                return;
            }                
            
            ////////////////////////////////////
            //                                //
            //         VERSUS SCREEN          //
            //                                //
            ////////////////////////////////////
            
            if(VS && !isShown){
                if(engine.getTotalElapsedTime(false)>1f){
                    isShown=true;
                    MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_versus"),
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
                    MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_versusF"),
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
                    if(!SOUND){
                        SOUND=true;
                        Global.getSoundPlayer().playUISound(
                                "AIB_versusS",
                                1,
                                1
                        );
                    }
                }
            }
            
            if(engine.isPaused()){return;}
            
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
            
            
            if(CLOCK){
                if(!END){
                    clock=(int)engine.getTotalElapsedTime(false);
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
            }
            
            ////////////////////////////////////
            //                                //
            //          FLEET STATUS          //
            //                                //
            ////////////////////////////////////            
            
            if(STATUS){
                status.advance(amount);
                if(status.intervalElapsed()){
                    teamA = AI_missionUtils.fleetStatusUpdateDP(PLAYER_A_SHIPS);
                    teamB = AI_missionUtils.fleetStatusUpdateDP(PLAYER_B_SHIPS);
                }            
                engine.maintainStatusForPlayerShip("status2",
                        "graphics/AIB/icons/hullsys/AI_teamSupport.png",
                        PlayerB.get(0)+" fleet status:",
                        teamB+"% DP remaining.",
                        teamB<25
                );
                
                engine.maintainStatusForPlayerShip("status1",
                        "graphics/AIB/icons/hullsys/AI_teamLead.png",
                        PlayerA.get(0)+" fleet status:",
                        teamA+"% DP remaining.",
                        teamA<25
                );
                
            }
            
            ////////////////////////////////////
            //                                //
            //         VICTORY SCREEN         //
            //                                //
            ////////////////////////////////////

            //getting log results
            timer+=engine.getElapsedInLastFrame();

            int playerAlive=0, enemyAlive=0, flawless=0;
            if(timer>1&& !END){
                timer=0;
                playerAlive = 0; enemyAlive = 0; flawless = 0;

                //check for members alive
                for(FleetMemberAPI m : engine.getFleetManager(FleetSide.PLAYER).getDeployedCopy()){
                    if(!m.isFighterWing()){
                        playerAlive++;
                    }
                }

                for(FleetMemberAPI m : engine.getFleetManager(FleetSide.ENEMY).getDeployedCopy()){
                    if(!m.isFighterWing()){
                        enemyAlive++;
                    }
                }

                if(playerAlive==0){
                    //player dead
                    END=true;
                    WINNER=enemy;
                    LOSER=player;
                    //check for enemy losses
                    for(FleetMemberAPI m : engine.getFleetManager(FleetSide.ENEMY).getDisabledCopy()){
                        if(!m.isFighterWing()){
                            flawless++;
                        }
                    }                
                    for(FleetMemberAPI m : engine.getFleetManager(FleetSide.ENEMY).getDestroyedCopy()){
                        if(!m.isFighterWing()){
                            flawless++;
                        }
                    }
                    //no loss? Shaming defeat
                    if (flawless<1){
                        SHAME=true;
                    }                    
                } else if(enemyAlive==0){
                    END=true;
                    WINNER=player;
                    LOSER=enemy;
                    for(FleetMemberAPI m : engine.getFleetManager(FleetSide.PLAYER).getDisabledCopy()){
                        if(!m.isFighterWing()){
                            flawless++;
                        }
                    }                
                    for(FleetMemberAPI m : engine.getFleetManager(FleetSide.PLAYER).getDestroyedCopy()){
                        if(!m.isFighterWing()){
                            flawless++;
                        }
                    }
                    if (flawless<1){
                        SHAME=true;
                    } 
                } 
                if (END) {
                    float DPremaining = 0;
                    log.info("__________________________");
                    log.info("__________________________");
                    log.info("COMBAT ENDED");
                    log.info("TIME ELAPSED: "+clock);
                    if (enemyAlive == 0) {
                        log.info("WINNER: "+PlayerA.get(0));
                        log.info("TAGLINE: "+PlayerA.get(2));
                        log.info("_________");
                        log.info("Remaining ships:");
                        for (FleetMemberAPI m : engine.getFleetManager(FleetSide.PLAYER).getDeployedCopy()) {
                            if (m.isFighterWing()) { continue; }
                            log.info(m.getShipName()+" - "+m.getVariant().getFullDesignationWithHullName()+", "+m.getDeploymentPointsCost()+" DP");
                            DPremaining += m.getDeploymentPointsCost();
                        }
                        log.info("_________");
                        teamA = AI_missionUtils.fleetStatusUpdateDP(PLAYER_A_SHIPS);
                        log.info("Remaining DP%: "+teamA);
                    } else {
                        log.info("WINNER: "+PlayerB.get(0));
                        log.info("TAGLINE: "+PlayerB.get(2));
                        log.info("_________");

                        log.info("Remaining ships:");
                        for (FleetMemberAPI m : engine.getFleetManager(FleetSide.ENEMY).getDeployedCopy()) {
                            if (m.isFighterWing()) { continue; }
                            log.info(m.getShipName()+" - "+m.getVariant().getFullDesignationWithHullName()+", "+m.getDeploymentPointsCost()+" DP");
                            DPremaining += m.getDeploymentPointsCost();
                        }
                        teamB = AI_missionUtils.fleetStatusUpdateDP(PLAYER_B_SHIPS);
                        log.info("Remaining DP%: "+teamB);
                    }
                    log.info("Remaining DP: "+DPremaining);
                    log.info("__________________________");
                    log.info("__________________________");
                }
            }            
            
            if(VS){
                if(END){
                    if(WINNER>-1){
                        //display winner                    
                        MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_player"+WINNER),
                                MagicRender.positioning.CENTER,
                                new Vector2f(64,128),
                                new Vector2f(-10,0),
                                new Vector2f(512,256),
                                new Vector2f(10,5),
                                0,
                                0,
                                Color.WHITE,
                                false,
                                0.25f, 
                                10f, 
                                1f);
                        MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_player"+WINNER),
                                MagicRender.positioning.CENTER,
                                new Vector2f(64,128),
                                new Vector2f(-10,0),
                                new Vector2f(512,256),
                                new Vector2f(10,5),
                                0,
                                0,
                                Color.WHITE,
                                true,
                                0f, 
                                0f, 
                                1f);

                        //play sound                    
                        if(SHAME){                     
                            Global.getSoundPlayer().playUISound(
                                    "AIB_shameS",
                                    1,
                                    1
                            );                        
                        } else {                        
                            Global.getSoundPlayer().playUISound(
                                    "AIB_victoryS",
                                    1,
                                    1
                            );
                        }

                        //reset winner                    
                        WINNER=-2;
                    }

                    if(WINNER<-1 && timer>0.5f){
                        WINNER=-1;
                        if(SHAME){
                            //diplay "SHAMED"                   
                            MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_shame"),
                                    MagicRender.positioning.CENTER,
                                    new Vector2f(0,0),
                                    new Vector2f(0,0),
                                    new Vector2f(512,256),
                                    new Vector2f(10,5),
                                    0,
                                    0,
                                    Color.WHITE,
                                    false,
                                    0f, 
                                    10f, 
                                    1f);
                            MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_shameF"),
                                    MagicRender.positioning.CENTER,
                                    new Vector2f(0,0),
                                    new Vector2f(0,0),
                                    new Vector2f(512,256),
                                    new Vector2f(10,5),
                                    0,
                                    0,
                                    Color.WHITE,
                                    true,
                                    0f, 
                                    0f, 
                                    0.5f);                    
                        } else {
                            //diplay "WINS"                        
                            MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_win"),
                                    MagicRender.positioning.CENTER,
                                    new Vector2f(-64,0),
                                    new Vector2f(10,0),
                                    new Vector2f(512,256),
                                    new Vector2f(10,5),
                                    0,
                                    0,
                                    Color.WHITE,
                                    false,
                                    0f, 
                                    10f, 
                                    1f);
                            MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_winF"),
                                    MagicRender.positioning.CENTER,
                                    new Vector2f(-128,0),
                                    new Vector2f(10,0),
                                    new Vector2f(512,256),
                                    new Vector2f(10,5),
                                    0,
                                    0,
                                    Color.WHITE,
                                    true,
                                    0f, 
                                    0f, 
                                    0.5f);
                        }   
                    }

                    if(SHAME && LOSER>-1 && timer>1f){
                        //display loser            
                        MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_player"+LOSER),
                                MagicRender.positioning.CENTER,
                                new Vector2f(-64,-128),
                                new Vector2f(10,0),
                                new Vector2f(512,256),
                                new Vector2f(-10,-5),
                                0,
                                0,
                                Color.WHITE,
                                false,
                                0.25f, 
                                10f, 
                                1f);
                        MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_player"+LOSER),
                                MagicRender.positioning.CENTER,
                                new Vector2f(-64,-128),
                                new Vector2f(10,0),
                                new Vector2f(512,256),
                                new Vector2f(-10,-5),
                                0,
                                0,
                                Color.WHITE,
                                true,
                                0f, 
                                0f, 
                                1f);

                        //reset loser
                        LOSER=-1;
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
        log.info("Reading CSV");
        try {
            log.info("Importing data:");
            JSONArray round = Global.getSettings().getMergedSpreadsheetDataForMod("round", ROUND_DATA, "aibattles");
            //log.info("Parsing row");
            JSONObject row = round.getJSONObject(0);
            //log.info("Reading round");
            ROUND=row.getInt("round");
            
            //log.info("Reading battlescape ");
            BATTLESCAPE=row.getInt("battlescape");
            
            //log.info("Reading vsScreen");
            VS=row.getBoolean("vsScreen");
            
            //log.info("Reading waves");
            MAX_WAVE=row.getInt("waves");
            
            //log.info("Reading noRetreat");
            NO_RETREAT=row.getBoolean("noRetreat");
            
            //log.info("Reading flagshipMode");
            FLAGSHIP=row.getBoolean("flagshipMode");
            
            //log.info("Reading buyInHullmods");
            HULLMOD=row.getBoolean("buyInHullmods");
            
            //log.info("Reading blacklist");
            BLACKLIST=row.getBoolean("blacklist");
            
            //log.info("Reading antiSpam");
            ANTISPAM=row.getBoolean("antiSpam");
            
            //log.info("Reading clock");
            CLOCK=row.getBoolean("clock");
            
            //log.info("Reading status");
            STATUS=row.getBoolean("status");
            
            //log.info("Reading maxCR");
            MAX_CR=row.getBoolean("maxCR");
            
            //log.info("Reading maintenance");
            MAINTENANCE=row.getBoolean("maintenance");
            
            //log.info("Reading supplies");
            SUPPLIES=row.getInt("supplies");
            
            //log.info("Reading showDP");
            SHOW_DP=row.getBoolean("showDP");
            
            //log.info("Reading showCost");
            SHOW_COST=row.getBoolean("showCost");
            
            //log.info("Reading timeout");
            TIMEOUT=row.getInt("timeout");
            
            if(ANTISPAM){
                HIKE=((float)row.getDouble("hike"))/100;
                SPAM_THRESHOLD.put(ShipAPI.HullSize.FIGHTER, (float)row.getInt("fighterT")+(ROUND-1)*(float)row.getInt("fighterR"));
                SPAM_THRESHOLD.put(ShipAPI.HullSize.FRIGATE, (float)row.getInt("frigateT")+(ROUND-1)*(float)row.getInt("frigateR"));
                SPAM_THRESHOLD.put(ShipAPI.HullSize.DESTROYER, (float)row.getInt("destroyerT")+(ROUND-1)*(float)row.getInt("destroyerR"));
                SPAM_THRESHOLD.put(ShipAPI.HullSize.CRUISER, (float)row.getInt("cruiserT")+(ROUND-1)*(float)row.getInt("cruiserR"));
                SPAM_THRESHOLD.put(ShipAPI.HullSize.CAPITAL_SHIP, (float)row.getInt("capitalT")+(ROUND-1)*(float)row.getInt("capitalR"));                
            }

        } catch (IOException | JSONException ex) {
            log.error("unable to read "+ROUND_DATA);
        }  
        
        //read round_matches.csv
        MATCHES.clear();
        try {
            JSONArray matches = Global.getSettings().loadCSV(ROUND_MATCHES);
            for(int i = 0; i < matches.length(); i++) {
            
                JSONObject row = matches.getJSONObject(i);
                player = row.getInt("playerA");
                enemy = row.getInt("playerB");

                MATCHES.put(2*i, new Vector2f(player,enemy));  
                MATCHES.put(2*i+1, new Vector2f(enemy,player));  
            }
        } catch (IOException | JSONException ex) {
            log.error("unable to read "+ROUND_MATCHES);
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
                log.error("unable to read "+ROUND_BLACKLIST);                
            }
        }
    }
}