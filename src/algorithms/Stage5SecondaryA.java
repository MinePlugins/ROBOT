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

public class Stage5SecondaryA extends Brain {
  //---PARAMETERS---//
  private static final double HEADINGPRECISION = 0.001;
  private static final double ANGLEPRECISION = 0.1;

  private static final int ROCKY = 0x1EADDA;
  private static final int MARIO = 0x5EC0;
  private static final int UNDEFINED = 0xBADC0DE0;

  private static final int TURNSOUTHTASK = 1;
  private static final int MOVESOUTHTASK = 2;
  private static final int TURNEASTTASK = 3;
  private static final int MOVEEASTTASK = 4;
  private static final int TURNNORTHTASK = 5;
  private static final int TURNSOUTHBISTASK = 51;
  private static final int SWINGTASK = 6;
  private static final int FREEZE = -1;
  private static final int SINK = 0xBADC0DE1;

  //---VARIABLES---//
  private int state;
  private double myX,myY;
  private boolean isMoving;
  private int whoAmI;
  private int rythm;

  //---CONSTRUCTORS---//
  public Stage5SecondaryA() { super(); }

  //---ABSTRACT-METHODS-IMPLEMENTATION---//
  public void activate() {
    //ODOMETRY CODE
    whoAmI = ROCKY;
    for (IRadarResult o: detectRadar())
      if (isSameDirection(o.getObjectDirection(),Parameters.NORTH)) whoAmI=MARIO;
    if (whoAmI == ROCKY){
      myX=Parameters.teamASecondaryBot1InitX;
      myY=Parameters.teamASecondaryBot1InitY;
    } else {
      myX=Parameters.teamASecondaryBot2InitX;
      myY=Parameters.teamASecondaryBot2InitY;
    }

    //INIT
    state=TURNSOUTHTASK;
    isMoving=false;
  }
  public void step() {
    //ODOMETRY CODE
    if (isMoving && whoAmI == ROCKY){
      myX+=Parameters.teamASecondaryBotSpeed*Math.cos(myGetHeading());
      myY+=Parameters.teamASecondaryBotSpeed*Math.sin(myGetHeading());
      isMoving=false;
    }
    if (isMoving && whoAmI == MARIO){
      myX+=Parameters.teamASecondaryBotSpeed*Math.cos(myGetHeading());
      myY+=Parameters.teamASecondaryBotSpeed*Math.sin(myGetHeading());
      isMoving=false;
    }
    //DEBUG MESSAGE
    if (whoAmI == ROCKY && state!=SINK) {
      sendLogMessage("*thinks* (x,y)= ("+(int)myX+", "+(int)myY+") and theta= "+(int)(myGetHeading()*180/(double)Math.PI)+"°. State= "+state);
    }
    if (whoAmI == MARIO && state!=SINK) {
      sendLogMessage("*thinks* (x,y)= ("+(int)myX+", "+(int)myY+") and theta= "+(int)(myGetHeading()*180/(double)Math.PI)+"°. State= "+state);
    }

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
    if (state==MOVESOUTHTASK && detectFront().getObjectType()!=IFrontSensorResult.Types.WALL) {
      myMove(); //And what to do when blind blocked?
      //sendLogMessage("Moving a head. Waza!");
      return;
    }
    if (state==MOVESOUTHTASK && detectFront().getObjectType()==IFrontSensorResult.Types.WALL) {
      state=TURNEASTTASK;
      stepTurn(Parameters.Direction.LEFT);
      //sendLogMessage("Iceberg at 12 o'clock. Heading 9!");
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
    if (state==MOVEEASTTASK && detectFront().getObjectType()!=IFrontSensorResult.Types.WALL && myX<2200 && whoAmI==MARIO) {
      myMove(); //And what to do when blind blocked?
      //sendLogMessage("Moving a head. Waza!");
      return;
    }
    if (state==MOVEEASTTASK && detectFront().getObjectType()!=IFrontSensorResult.Types.WALL && myX>=2200 && whoAmI==MARIO) {
      state=TURNNORTHTASK;
      stepTurn(Parameters.Direction.LEFT);
      //sendLogMessage("Turn North.");
      return;
    }
    if (state==MOVEEASTTASK && detectFront().getObjectType()!=IFrontSensorResult.Types.WALL && myX<1800 && whoAmI==ROCKY) {
      myMove(); //And what to do when blind blocked?
      //sendLogMessage("Moving a head. Waza!");
      return;
    }
    if (state==MOVEEASTTASK && detectFront().getObjectType()!=IFrontSensorResult.Types.WALL && myX>=1800 && whoAmI==ROCKY) {
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
      myMove();
      return;
    }
    if (state==SWINGTASK){
      if (rythm==0) stepTurn(Parameters.Direction.LEFT); else myMove();
      rythm++;
      rythm=rythm%3;
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
  private double myGetHeading(){
    double result = getHeading();
    while(result<0) result+=2*Math.PI;
    while(result>2*Math.PI) result-=2*Math.PI;
    return result;
  }
  private boolean isSameDirection(double dir1, double dir2){
    return Math.abs(dir1-dir2)<ANGLEPRECISION;
  }
}
