/*
 * By Tartiflette
 * Enable to move ships around in case of AI derping
 */
package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import java.util.List;
import org.apache.log4j.Logger;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector2f;

public class AI_relocatePlugin extends BaseEveryFrameCombatPlugin {
    
    private Logger log = Global.getLogger(AI_relocatePlugin.class);    
    private CombatEngineAPI engine;
    private boolean reset=true;
    private float relocate=0;
    
    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        
        if(engine==null){        
            engine = Global.getCombatEngine();
        }
        
        if(!engine.isPaused()){
            return;
        }
        
        ////////////////////////////////////
        //                                //
        //        RELOCATION TOOL         //
        //                                //
        ////////////////////////////////////              

        if (Keyboard.isKeyDown(Keyboard.KEY_Z)){
            if(relocate<0.25f){
                relocate+=amount;
            } else {
                reset=true;
                //relocate ship
                if(engine.getPlayerShip().getShipTarget()!=null){
                    Vector2f point=engine.getPlayerShip().getShipTarget().getLocation();
                    point.set(engine.getPlayerShip().getMouseTarget());
//                    log.info("Moving "+engine.getPlayerShip().getShipTarget().getName());
                }
            }
        } else if(Keyboard.isKeyDown(Keyboard.KEY_X)){
            if(relocate<0.25f){
                relocate+=amount;
            } else {
                reset=true;
                //rotate ship                
                if(engine.getPlayerShip().getShipTarget()!=null){
                    Vector2f point=engine.getPlayerShip().getMouseTarget();
                    float aim=VectorUtils.getAngle(engine.getPlayerShip().getShipTarget().getLocation(), point);
                    engine.getPlayerShip().getShipTarget().setFacing(aim);     
//                    log.info("Orienting "+engine.getPlayerShip().getShipTarget().getName());               
                }
            }
            
        } else if(reset){
            reset=false;
            relocate=0;
        }
    }
}