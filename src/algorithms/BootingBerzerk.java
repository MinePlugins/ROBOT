/* ******************************************************
 * Simovies - Eurobot 2015 Robomovies Simulator.
 * Copyright (C) 2014 <Binh-Minh.Bui-Xuan@ens-lyon.org>.
 * GPL version>=3 <http://www.gnu.org/licenses/>.
 * $Id: algorithms/BootingBerzerk.java 2014-11-03 buixuan.
 * ******************************************************/
package algorithms;

import java.util.ArrayList;

import robotsimulator.Brain;
import characteristics.Parameters;
import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;

public class BootingBerzerk extends Brain {
  //---PARAMETERS---//
  private static final double ANGLEPRECISION = 0.01;
  private static final double FIREANGLEPRECISION = Math.PI/(double)6;
  private static final int ALPHA = 0x1EADDA;
  private static final int BETA = 0x5EC0;
  private static final int GAMMA = 0x333;
  private static final int TEAM = 0xBADDAD;
   private static final int FIRE = 0xB52;
  private static final int UNDEFINED = 0xBADC0DE0;
  private static final double HEADINGPRECISION = 0.001;

  //---VARIABLES---//
  private boolean turnTask,turnRight,moveTask,berzerk,back;
  private double endTaskDirection,lastSeenDirection;
  private int endTaskCounter,berzerkInerty;
  private boolean firstMove,berzerkTurning;
  private double myX,myY;
  private int whoAmI;
  private boolean fireOrder;
  private double targetX,targetY;
  private int countDown;
  private boolean isMoving;
   

  //---CONSTRUCTORS---//
  public BootingBerzerk() { super(); }

  //---ABSTRACT-METHODS-IMPLEMENTATION---//
  public void activate() {
    isMoving=true;
    turnTask=true;
    moveTask=false;
    firstMove=true;
    berzerk=false;
    berzerkInerty=0;
    berzerkTurning=false;
    back=false;
    endTaskDirection=(Math.random()-0.5)*0.5*Math.PI;
    turnRight=(endTaskDirection>0);
    endTaskDirection+=getHeading();
    lastSeenDirection=Math.random()*Math.PI*2;
    if (turnRight) stepTurn(Parameters.Direction.RIGHT);
    else stepTurn(Parameters.Direction.LEFT);
    sendLogMessage("Turning point. Waza!");
    whoAmI = GAMMA;
    for (IRadarResult o: detectRadar())
      if (isSameDirection(o.getObjectDirection(),Parameters.NORTH)) whoAmI=ALPHA;
    for (IRadarResult o: detectRadar())
      if (isSameDirection(o.getObjectDirection(),Parameters.SOUTH) && whoAmI!=GAMMA) whoAmI=BETA;
    if (whoAmI == GAMMA){
      myX=Parameters.teamBMainBot1InitX;
      myY=Parameters.teamBMainBot1InitY;
    } else {
      myX=Parameters.teamBMainBot2InitX;
      myY=Parameters.teamBMainBot2InitY;
    }
    if (whoAmI == ALPHA){
      myX=Parameters.teamBMainBot3InitX;
      myY=Parameters.teamBMainBot3InitY;
    }
  }
  public void step() {
    /*if (Math.random()<0.01 && !berzerk) {
      fire(Math.random()*Math.PI*2);
      return;
    }*/
    if (isMoving){
      myX+=Parameters.teamAMainBotSpeed*Math.cos(myGetHeading());
      myY+=Parameters.teamAMainBotSpeed*Math.sin(myGetHeading());
      isMoving=true;
    }
    ArrayList<String> messages=fetchAllMessages();
    for (String m: messages) 
      if (Integer.parseInt(m.split(":")[1])==whoAmI || Integer.parseInt(m.split(":")[1])==TEAM){
        process(m);
        sendLogMessage(m);
      } 
      if (fireOrder /*&& friendlyFire*/) {
        // ne prend que 1 des 2 ordres
        //fireRythm*=1000;
        firePosition(targetX,targetY);
        //fire(0);
        sendLogMessage("Turning point. Waza!");
        return;
      }
    
    ArrayList<IRadarResult> radarResults = detectRadar();
    if (berzerk) {
      if (berzerkTurning) {
        endTaskCounter--;
        if (isHeading(endTaskDirection)) {
          berzerkTurning=false;
          move();
          sendLogMessage("Moving a head. Waza!");
        } else {
          if (turnRight) stepTurn(Parameters.Direction.RIGHT);
          else stepTurn(Parameters.Direction.LEFT);
        }
        return;
      }

      if (endTaskCounter<0) {
        turnTask=true;
        moveTask=false;
        berzerk=false;
        endTaskDirection=(Math.random()-0.5)*2*Math.PI;
        turnRight=(endTaskDirection>0);
        endTaskDirection+=getHeading();
        if (turnRight) stepTurn(Parameters.Direction.RIGHT);
        else stepTurn(Parameters.Direction.LEFT);
        sendLogMessage("Turning point. Waza!");
      } else {
        endTaskCounter--;
        if (Math.random()<0.1) {
          for (IRadarResult r : radarResults) {
            if (r.getObjectType()==IRadarResult.Types.OpponentMainBot) {
              fire(r.getObjectDirection());
              lastSeenDirection=r.getObjectDirection();
              return;
            }
          }
          fire(lastSeenDirection);
          return;
        } else {
          if (back) moveBack(); else move();
        }
      }
      if (berzerkInerty>50) {
        turnTask=true;
        moveTask=false;
        berzerk=false;
        endTaskDirection=(Math.random()-0.5)*2*Math.PI;
        turnRight=(endTaskDirection>0);
        endTaskDirection+=getHeading();
        if (turnRight) stepTurn(Parameters.Direction.RIGHT);
        else stepTurn(Parameters.Direction.LEFT);
        sendLogMessage("Turning point. Waza!");
        return;
      }
      if (endTaskCounter<0) {
        for (IRadarResult r : radarResults) {
          if (r.getObjectType()==IRadarResult.Types.OpponentMainBot) {
            fire(r.getObjectDirection());
            lastSeenDirection=r.getObjectDirection();
            berzerkInerty=0;
            return;
          }
        }
        fire(lastSeenDirection);
        berzerkInerty++;
        endTaskCounter=21;
        return;
      } else {
        endTaskCounter--;
        for (IRadarResult r : radarResults) {
          if (r.getObjectType()==IRadarResult.Types.OpponentMainBot) {
            lastSeenDirection=r.getObjectDirection();
            berzerkInerty=0;
            move();
            return;
          }
        }
        berzerkInerty++;
        move();
      }
      return;
    }
    if (radarResults.size()!=0){
      for (IRadarResult r : radarResults) {
        if (r.getObjectType()==IRadarResult.Types.OpponentMainBot) {
          berzerk=true;
          back=(Math.cos(getHeading()-r.getObjectDirection())>0);
          endTaskCounter=21;
          fire(r.getObjectDirection());
          lastSeenDirection=r.getObjectDirection();
          berzerkTurning=true;
          endTaskDirection=lastSeenDirection;
          double ref=endTaskDirection-getHeading();
          if (ref<0) ref+=Math.PI*2;
          turnRight=(ref>0 && ref<Math.PI);
          return;
        }
      }
      for (IRadarResult r : radarResults) {
        if (r.getObjectType()==IRadarResult.Types.OpponentSecondaryBot) {
          fire(r.getObjectDirection());
          return;
        }
      }
    }
    if (turnTask) {
      if (isHeading(endTaskDirection)) {
        if (firstMove) {
          firstMove=false;
  	  turnTask=false;
          moveTask=true;
          endTaskCounter=400;
	  move();
          sendLogMessage("Moving a head. Waza!");
          return;
        }
	turnTask=false;
        moveTask=true;
        endTaskCounter=100;
	move();
        sendLogMessage("Moving a head. Waza!");
      } else {
        if (turnRight) stepTurn(Parameters.Direction.RIGHT);
        else stepTurn(Parameters.Direction.LEFT);
      }
      return;
    }
    if (moveTask) {
      /*if (detectFront().getObjectType()!=NOTHING) {
        turnTask=true;
        moveTask=false;
        endTaskDirection=(Math.random()-0.5)*Math.PI;
        turnRight=(endTaskDirection>0);
        endTaskDirection+=getHeading();
        if (turnRight) stepTurn(Parameters.Direction.RIGHT);
        else stepTurn(Parameters.Direction.LEFT);
        sendLogMessage("Turning point. Waza!");
      }*/
      if (endTaskCounter<0) {
        turnTask=true;
        moveTask=false;
        endTaskDirection=(Math.random()-0.5)*2*Math.PI;
        turnRight=(endTaskDirection>0);
        endTaskDirection+=getHeading();
        if (turnRight) stepTurn(Parameters.Direction.RIGHT);
        else stepTurn(Parameters.Direction.LEFT);
        sendLogMessage("Turning point. Waza!");
      } else {
        endTaskCounter--;
        move();
      }
      return;
    }
    return;
  }
  private boolean isHeading(double dir){
    return Math.abs(Math.sin(getHeading()-dir))<Parameters.teamAMainBotStepTurnAngle;
  }
    private void process(String message){
    targetX=Double.parseDouble(message.split(":")[3]);
    targetY=Double.parseDouble(message.split(":")[4]);
    if (Integer.parseInt(message.split(":")[2])==FIRE && Math.abs(myX-targetX)<=700 && Math.abs(myY-targetY)<=700) {
      fireOrder=true;
      countDown=0;
    }else{
      fireOrder=false;
    }
  }
  private void firePosition(double x, double y){
    sendLogMessage( "ici: "+Math.PI+Math.atan((y-myY)/(double)(x-myX)));
    if (myX<=x) fire(Math.atan((y-myY)/(double)(x-myX)));
    else fire(Math.PI+Math.atan((y-myY)/(double)(x-myX)));
    return;
  }
  private boolean isSameDirection(double dir1, double dir2){
    return Math.abs(normalizeRadian(dir1)-normalizeRadian(dir2))<ANGLEPRECISION;
  }
  private double normalizeRadian(double angle){
    double result = angle;
    while(result<0) result+=2*Math.PI;
    while(result>=2*Math.PI) result-=2*Math.PI;
    return result;
  }
    private void myMove(){
    isMoving=true;
    move();
  }
  private double myGetHeading(){
    return normalizeRadian(getHeading());
  }
}
