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

public class Stage6MainA extends Brain {
  //---PARAMETERS---//
  private static final double HEADINGPRECISION = 0.001;
  private static final double ANGLEPRECISION = 0.1;

  private static final int ALPHA = 0x1EADDA;
  private static final int BETA = 0x5EC0;
  private static final int GAMMA = 0x333;
  private static final int TEAM = 0xBADDAD;
  private static final int UNDEFINED = 0xBADC0DE0;
  
  private static final int FALLBACK = 0xFA11BAC;
  private static final int ROGER = 0x0C0C0C0C;
  private static final int OVER = 0xC00010FF;

  private static final int TURNSOUTHTASK = 1;
  private static final int MOVESOUTHTASK = 2;
  private static final int MOVEBACKSOUTHTASK = 21;
  private static final int MOVESOUTHBISTASK = 22;
  private static final int MOVEBACKSOUTHBISTASK = 23;
  private static final int MOVESOUTHTERTASK = 24;
  private static final int PAUSETASK = 25;
  private static final int TURNEASTTASK = 3;
  private static final int MOVEEASTTASK = 4;
  private static final int TURNNORTHTASK = 5;
  private static final int TURNSOUTHBISTASK = 51;
  private static final int SWINGTASK = 6;
  private static final int FREEZE = -1;
  private static final int SINK = 0xBADC0DE1;

  //---VARIABLES---//
  private int state;
  private double myX,myY,oldY;
  private boolean isMoving,isMovingBack;
  private int whoAmI;
  private int fireRythm,rythm,counter;
  private boolean fallbackOrder;

  //---CONSTRUCTORS---//
  public Stage6MainA() { super(); }

  //---ABSTRACT-METHODS-IMPLEMENTATION---//
  public void activate() {
    //ODOMETRY CODE
    whoAmI = GAMMA;
    for (IRadarResult o: detectRadar())
      if (isSameDirection(o.getObjectDirection(),Parameters.NORTH)) whoAmI=ALPHA;
    for (IRadarResult o: detectRadar())
      if (isSameDirection(o.getObjectDirection(),Parameters.SOUTH) && whoAmI!=GAMMA) whoAmI=BETA;
    if (whoAmI == GAMMA){
      myX=Parameters.teamAMainBot1InitX;
      myY=Parameters.teamAMainBot1InitY;
    } else {
      myX=Parameters.teamAMainBot2InitX;
      myY=Parameters.teamAMainBot2InitY;
    }
    if (whoAmI == ALPHA){
      myX=Parameters.teamAMainBot3InitX;
      myY=Parameters.teamAMainBot3InitY;
    }

    //INIT
    state=TURNSOUTHTASK;
    isMoving=false;
    isMovingBack=false;
    fallbackOrder=false;
  }
  public void step() {
    //ODOMETRY CODE
    if (isMoving){
      myX+=Parameters.teamAMainBotSpeed*Math.cos(myGetHeading());
      myY+=Parameters.teamAMainBotSpeed*Math.sin(myGetHeading());
      isMoving=false;
    }
    if (isMovingBack){
      myX-=Parameters.teamAMainBotSpeed*Math.cos(myGetHeading());
      myY-=Parameters.teamAMainBotSpeed*Math.sin(myGetHeading());
      isMovingBack=false;
    }
    //DEBUG MESSAGE
    boolean debug=true;
    if (debug && whoAmI == ALPHA && state!=SINK) {
      sendLogMessage("#ALPHA *thinks* (x,y)= ("+(int)myX+", "+(int)myY+") theta= "+(int)(myGetHeading()*180/(double)Math.PI)+"°. #State= "+state);
    }
    if (debug && whoAmI == BETA && state!=SINK) {
      sendLogMessage("#BETA *thinks* (x,y)= ("+(int)myX+", "+(int)myY+") theta= "+(int)(myGetHeading()*180/(double)Math.PI)+"°. #State= "+state);
    }
    if (debug && whoAmI == GAMMA && state!=SINK) {
      sendLogMessage("#GAMMA *thinks* (x,y)= ("+(int)myX+", "+(int)myY+") theta= "+(int)(myGetHeading()*180/(double)Math.PI)+"°. #State= "+state);
    }

    //COMMUNICATION
    ArrayList<String> messages=fetchAllMessages();
    for (String m: messages) if (Integer.parseInt(m.split(":")[1])==whoAmI || Integer.parseInt(m.split(":")[1])==TEAM) process(m);

    //AUTOMATON
    if (state==TURNSOUTHTASK && !(isSameDirection(myGetHeading(),Parameters.SOUTH))) {
      stepTurn(Parameters.Direction.RIGHT);
      //sendLogMessage("Initial TeamA Secondary Bot2 position. Heading South!");
      return;
    }
    if (state==TURNSOUTHTASK && isSameDirection(myGetHeading(),Parameters.SOUTH)) {
      state=MOVESOUTHTASK;
      myMove();
      //sendLogMessage("Moving a head. Waza!");
      return;
    }
    if (state==MOVESOUTHTASK && detectFront().getObjectType()!=IFrontSensorResult.Types.WALL && !fallbackOrder) {
      myMove();
      return;
    }
    if (state==MOVESOUTHTASK && detectFront().getObjectType()==IFrontSensorResult.Types.WALL) {
      state=MOVEBACKSOUTHTASK;
      oldY=myY;
      broadcast(whoAmI+":"+TEAM+":"+FALLBACK+":"+OVER);
      myMoveBack();
      return;
    }
    if (state==MOVESOUTHTASK && fallbackOrder) {
      state=MOVEBACKSOUTHTASK;
      oldY=myY;
      broadcast(whoAmI+":"+TEAM+":"+ROGER+":"+OVER);
      myMoveBack();
      return;
    }
    if (state==MOVEBACKSOUTHTASK && !(myY<=oldY-200)) {
      myMoveBack();
      return;
    }
    if (state==MOVEBACKSOUTHTASK && myY<=oldY-200) {
      if (whoAmI==ALPHA) {
        state=TURNEASTTASK;
        stepTurn(Parameters.Direction.LEFT);
      } else {
        state=MOVESOUTHBISTASK;
        fallbackOrder=false;
        myMove();
      }
      return;
    }
    if (state==MOVESOUTHBISTASK && detectFront().getObjectType()!=IFrontSensorResult.Types.WALL && !fallbackOrder) {
      myMove();
      return;
    }
    if (state==MOVESOUTHBISTASK && detectFront().getObjectType()==IFrontSensorResult.Types.WALL) {
      state=MOVEBACKSOUTHBISTASK;
      oldY=myY;
      broadcast(whoAmI+":"+GAMMA+":"+FALLBACK+":"+OVER);
      myMoveBack();
      return;
    }
    if (state==MOVESOUTHBISTASK && fallbackOrder) {
      state=MOVEBACKSOUTHBISTASK;
      oldY=myY;
      broadcast(whoAmI+":"+TEAM+":"+ROGER+":"+OVER);
      myMoveBack();
      return;
    }
    if (state==MOVEBACKSOUTHBISTASK && !(myY<=oldY-200)) {
      //sendLogMessage("Bot "+whoAmI+" moving back bis with oldY= "+oldY);
      myMoveBack();
      return;
    }
    if (state==MOVEBACKSOUTHBISTASK && myY<=oldY-200) {
      if (whoAmI==BETA) {
        state=TURNEASTTASK;
        stepTurn(Parameters.Direction.LEFT);
      } else {
        oldY=myY;
        state=MOVESOUTHTERTASK;
        myMove();
      }
      return;
    }
    if (state==MOVESOUTHTERTASK && !(myY>oldY+200)) {
      myMove();
      return;
    }
    if (state==MOVESOUTHTERTASK && myY>oldY+200) {
      state=PAUSETASK;
      counter=0;
      return;
    }
    if (state==PAUSETASK && counter<=200) {
      counter++;
      return;
    }
    if (state==PAUSETASK && counter>200) {
      state=TURNEASTTASK;
      stepTurn(Parameters.Direction.LEFT);
      return;
    }
    if (state==TURNEASTTASK && !(isSameDirection(myGetHeading(),Parameters.EAST))) {
      stepTurn(Parameters.Direction.LEFT);
      //sendLogMessage("Iceberg at 12 o'clock. Heading 9!");
      return;
    }
    if (state==TURNEASTTASK && isSameDirection(myGetHeading(),Parameters.EAST)) {
      state=MOVEEASTTASK;
      myMove();
      //sendLogMessage("Moving a head. Waza!");
      return;
    }
    if (state==MOVEEASTTASK && detectFront().getObjectType()!=IFrontSensorResult.Types.WALL && myX<1100 && whoAmI==ALPHA) {
      myMove(); //And what to do when blind blocked?
      //sendLogMessage("Moving a head. Waza!");
      return;
    }
    if (state==MOVEEASTTASK && detectFront().getObjectType()!=IFrontSensorResult.Types.WALL && myX>=1100 && whoAmI==ALPHA) {
      state=TURNNORTHTASK;
      stepTurn(Parameters.Direction.LEFT);
      //sendLogMessage("Turn North.");
      return;
    }
    if (state==MOVEEASTTASK && detectFront().getObjectType()!=IFrontSensorResult.Types.WALL && myX<300 && whoAmI==BETA) {
      myMove(); //And what to do when blind blocked?
      //sendLogMessage("Moving a head. Waza!");
      return;
    }
    if (state==MOVEEASTTASK && detectFront().getObjectType()!=IFrontSensorResult.Types.WALL && myX>=300 && whoAmI==BETA) {
      state=TURNSOUTHBISTASK;
      stepTurn(Parameters.Direction.LEFT);
      //sendLogMessage("Turn North.");
      return;
    }
    if (state==MOVEEASTTASK && detectFront().getObjectType()!=IFrontSensorResult.Types.WALL && myX<300 && whoAmI==GAMMA) {
      myMove(); //And what to do when blind blocked?
      //sendLogMessage("Moving a head. Waza!");
      return;
    }
    if (state==MOVEEASTTASK && detectFront().getObjectType()!=IFrontSensorResult.Types.WALL && myX>=300 && whoAmI==GAMMA) {
      state=TURNSOUTHBISTASK;
      stepTurn(Parameters.Direction.LEFT);
      //sendLogMessage("Turn North.");
      return;
    }
    if (state==TURNNORTHTASK && !(isSameDirection(myGetHeading(),Parameters.NORTH+2*Math.PI))) {
      stepTurn(Parameters.Direction.LEFT);
      //sendLogMessage("Initial TeamA Secondary Bot2 position. Heading South!");
      return;
    }
    if (state==TURNNORTHTASK && isSameDirection(myGetHeading(),Parameters.NORTH+2*Math.PI)) {
      state=SWINGTASK;
      rythm=0;
      myMove();
      //sendLogMessage("Moving a head. Waza!");
      return;
    }
    if (state==TURNSOUTHBISTASK && !(isSameDirection(myGetHeading(),Parameters.SOUTH))) {
      stepTurn(Parameters.Direction.RIGHT);
      return;
    }
    if (state==TURNSOUTHBISTASK && isSameDirection(myGetHeading(),Parameters.SOUTH)) {
      state=SWINGTASK;
      rythm=0;
      fireRythm=0;
      myMove();
      return;
    }
    if (state==SWINGTASK){
      if (fireRythm==0) {
        firePosition(700,1500);
        fireRythm++;
        return;
      }
      fireRythm++;
      if (fireRythm==Parameters.bulletFiringLatency) fireRythm=0;
      if (rythm==0) stepTurn(Parameters.Direction.LEFT); else myMove();
      rythm++;
      if (rythm==14) rythm=0;
      return;
    }

    if (state==FREEZE) {
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
  private void myMoveBack(){
    isMovingBack=true;
    moveBack();
  }
  private double myGetHeading(){
    double result = getHeading();
    while(result<0) result+=2*Math.PI;
    while(result>2*Math.PI) result-=2*Math.PI;
    return result;
  }
  private boolean isSameDirection(double dir1, double dir2){
    return Math.abs(dir1-dir2)<ANGLEPRECISION;
  }
  private void process(String message){
    if (Integer.parseInt(message.split(":")[2])==FALLBACK) fallbackOrder=true;
  }
  private void firePosition(int x, int y){
    if (myX<=x) fire(Math.atan((y-myY)/(double)(x-myX)));
    else fire(Math.PI+Math.atan((y-myY)/(double)(x-myX)));
    return;
  }
}
