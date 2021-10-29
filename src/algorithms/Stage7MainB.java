/* ******************************************************
 * Simovies - Eurobot 2015 Robomovies Simulator.
 * Copyright (C) 2014 <Binh-Minh.Bui-Xuan@ens-lyon.org>.
 * GPL version>=3 <http://www.gnu.org/licenses/>.
 * $Id: algorithms/Stage1.java 2014-10-18 buixuan.
 * ******************************************************/
package algorithms;

import robotsimulator.Brain;
import characteristics.Parameters;
import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;

import java.util.ArrayList;

public class Stage7MainB extends Brain {
  //---PARAMETERS---//
  private static final double ANGLEPRECISION = 0.01;
  private static final double FIREANGLEPRECISION = Math.PI/(double)6;

  private static final int ALPHA = 0x1EADDA;
  private static final int BETA = 0x5EC0;
  private static final int GAMMA = 0x333;
  private static final int TEAM = 0xBADDAD;
  private static final int UNDEFINED = 0xBADC0DE0;
  
  private static final int FIRE = 0xB52;
  private static final int FALLBACK = 0xFA11BAC;
  private static final int ROGER = 0x0C0C0C0C;
  private static final int OVER = 0xC00010FF;

  private static final int TURNSOUTHTASK = 1;
  private static final int MOVETASK = 2;
  private static final int TURNLEFTTASK = 3;
  private static final int TURNRIGHTTASK = 4;

  private static final int SINK = 0xBADC0DE1;

  //---VARIABLES---//
  private int state;
  private double oldAngle;
  private double myX,myY;
  private boolean isMoving;
  private int whoAmI;
  private int fireRythm,rythm,counter;
  private int countDown;
  private double targetX,targetY;
  private boolean fireOrder;
  private boolean freeze;
  private boolean friendlyFire;
  private double[] firePosition_Temp = {0,0}; // x, y
  private double firePositionX_Temp = 0;
  private double firePositionY_Temp = 0;

  //---CONSTRUCTORS---//
  public Stage7MainB() { super(
  ); }

  //---ABSTRACT-METHODS-IMPLEMENTATION---//
  public void activate() {
    //ODOMETRY CODE
    //Parameters.teamBMainBotFrontalDetectionRange=3000;
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

    //INIT
    state=TURNLEFTTASK;
    isMoving=false;
    fireOrder=false;
    fireRythm=150;
    oldAngle=myGetHeading();
    targetX=1500;
    targetY=1000;
  }
  public void step() {
    //ODOMETRY CODE
    if (isMoving){
      myX+=Parameters.teamAMainBotSpeed*Math.cos(myGetHeading());
      myY+=Parameters.teamAMainBotSpeed*Math.sin(myGetHeading());
      isMoving=false;
    }
    //DEBUG MESSAGE
    boolean debug=true;
    if (debug && whoAmI == ALPHA && state!=SINK) {
      sendLogMessage("#ALPHA *thinks* (x,y)= ("+(int)myX+", "+(int)myY+") theta= "+(int)(myGetHeading()*180/(double)Math.PI)+"Â°. #State= "+state);
    }
    if (debug && whoAmI == BETA && state!=SINK) {
      sendLogMessage("#BETA *thinks* (x,y)= ("+(int)myX+", "+(int)myY+") theta= "+(int)(myGetHeading()*180/(double)Math.PI)+"Â°. #State= "+state);
    }
    if (debug && whoAmI == GAMMA && state!=SINK) {
      sendLogMessage("#GAMMA *thinks* (x,y)= ("+(int)myX+", "+(int)myY+") theta= "+(int)(myGetHeading()*180/(double)Math.PI)+"Â°. #State= "+state);
    }
    if (debug && fireOrder) sendLogMessage("Firing enemy!!");

    //COMMUNICATION
    firePositionX_Temp = 0;
    ArrayList<String> messages=fetchAllMessages();
    for (String m: messages) if (Integer.parseInt(m.split(":")[1])==whoAmI || Integer.parseInt(m.split(":")[1])==TEAM) process(m);
    
    //RADAR DETECTION
    freeze=false;
    friendlyFire=true;
    
    for (IRadarResult o: detectRadar()){
      if (o.getObjectType()==IRadarResult.Types.OpponentMainBot || o.getObjectType()==IRadarResult.Types.OpponentSecondaryBot) {
        double enemyX=myX+o.getObjectDistance()*Math.cos(o.getObjectDirection());
        double enemyY=myY+o.getObjectDistance()*Math.sin(o.getObjectDirection());
        broadcast(whoAmI+":"+TEAM+":"+FIRE+":"+enemyX+":"+enemyY+":"+OVER);
      }
      if (o.getObjectDistance()<=100 && !isRoughlySameDirection(o.getObjectDirection(),getHeading())) {
        freeze=false;
      }
      if (o.getObjectType()==IRadarResult.Types.TeamMainBot || o.getObjectType()==IRadarResult.Types.TeamSecondaryBot /*|| o.getObjectType()==IRadarResult.Types.Wreck*/) {
        // System.out.println(onTheWay(o.getObjectDirection()));
        if (fireOrder && onTheWay(o.getObjectDirection())) {
          friendlyFire=false;
          fireOrder=true;
        }
      }
    }
    if (freeze) return;

    //AUTOMATON
    if (fireOrder) countDown++;
    if (countDown>=10000) fireOrder=false;
    if (fireOrder && friendlyFire) {
      // ne prend que 1 des 2 ordres
      fireRythm*=1000;
      // firePosition(targetX+10,targetY+10);
      firePosition(firePositionX_Temp,firePositionY_Temp);

      
      return;
    }
    fireRythm*=15000;
    //if (fireRythm>=Parameters.bulletFiringLatency) fireRythm=150;
    if (state==TURNSOUTHTASK && !(isSameDirection(getHeading(),Parameters.NORTH))) {
      stepTurn(Parameters.Direction.RIGHT);
      return;
    }
    /*
    if (state==TURNSOUTHTASK && isSameDirection(getHeading(),Parameters.NORTH)) {
      state=MOVETASK;
      myMove();
      return;
    }
    if (state==MOVETASK && detectFront().getObjectType()!=IFrontSensorResult.Types.WALL) {
      myMove();
      return;
    }
    if (state==MOVETASK && detectFront().getObjectType()==IFrontSensorResult.Types.WALL) {
      state=TURNLEFTTASK;
      oldAngle=myGetHeading();
      stepTurn(Parameters.Direction.LEFT);
      return;
    }*/
    if (state==TURNLEFTTASK && !(isSameDirection(getHeading(),oldAngle+Parameters.LEFTTURNFULLANGLE))) {
      stepTurn(Parameters.Direction.LEFT);
      System.out.println(getHeading() + " | " + oldAngle + " | " + Parameters.LEFTTURNFULLANGLE);

      return;
    }
    if (state==TURNLEFTTASK && isSameDirection(getHeading(),oldAngle+Parameters.LEFTTURNFULLANGLE) && myY <= 1850 ) {
      // state=TURNRIGHTTASK;
      
      myMove();
      return;
    }
    if (state==TURNRIGHTTASK && !(isSameDirection(getHeading(),oldAngle+Parameters.LEFTTURNFULLANGLE)) && myX <= 2860 && whoAmI==ALPHA) {
      stepTurn(Parameters.Direction.LEFT);
      return;
    }

    if (state==SINK) {
      myMove();
      return;
    }

    if (true) {
      return;
    }
  }
  private void myMove(){
    isMoving=true;
    move();
  }
  private double myGetHeading(){
    return normalizeRadian(getHeading());
  }
  private double normalizeRadian(double angle){
    double result = angle;
    while(result<0) result+=2*Math.PI;
    while(result>=2*Math.PI) result-=2*Math.PI;
    return result;
  }
  private boolean isSameDirection(double dir1, double dir2){
    return Math.abs(normalizeRadian(dir1)-normalizeRadian(dir2))<ANGLEPRECISION;
  }
  private boolean isRoughlySameDirection(double dir1, double dir2){
    return Math.abs(normalizeRadian(dir1)-normalizeRadian(dir2))<FIREANGLEPRECISION;
  }
  private void process(String message){
    targetX = Double.parseDouble(message.split(":")[3]);
    targetY = Double.parseDouble(message.split(":")[4]);
    // System.out.println("targetX : " + targetX);
    // System.out.println("targetY : " + targetY);
    // System.out.println("Position X : " + firePositionX_Temp);
    // permet de déterminer sur quel robot tirer, soit le plus proche
    if(targetX >= firePositionX_Temp && targetX >= 1500 && targetY >= 250){
      firePositionX_Temp = targetX;
      firePositionY_Temp = targetY;
      // System.out.println(firePositionX_Temp + " | " + firePositionY_Temp);
      if (Integer.parseInt(message.split(":")[2])==FIRE /*&& Math.abs(myX-targetX)<=700 && Math.abs(myY-targetY)<=700*/) {
        fireOrder=true;
        countDown=0;
      }else{
      fireOrder=false;
      }
    }
  }
  private void firePosition(double x, double y){
    sendLogMessage( ""+Math.PI+Math.atan((y-myY)/(double)(x-myX)));
    if (myX<=x) fire(Math.atan((y-myY)/(double)(x-myX)));
    else fire(Math.PI+Math.atan((y-myY)/(double)(x-myX)));
    return;
  }
  private boolean onTheWay(double angle){
    if (myX<=targetX) return isRoughlySameDirection(angle,Math.atan((targetY-myY)/(double)(targetX-myX)));
    else return isRoughlySameDirection(angle,Math.PI+Math.atan((targetY-myY)/(double)(targetX-myX)));
  }
}