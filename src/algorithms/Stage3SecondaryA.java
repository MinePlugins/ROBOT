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

public class Stage3SecondaryA extends Brain {
  //---PARAMETERS---//
  private static final double HEADINGPRECISION = 0.001;
  private static final double ANGLEPRECISION = 0.09;

  private static final int ROCKY = 0x1EADDA;
  private static final int MARIO = 0x5EC0;
  private static final int UNDEFINED = 0xBADC0DE0;

  private static final int TOURNESASK = 1;
  private static final int MOVESASK = 2;
  private static final int TURNEASTTASK = 3;
  private static final int MOVEEASTTASK = 4;
  private static final int TOURNEASK = 5;
  private static final int UTURNAGAINTASK = 6;
  private static final int SINK = 0xBADC0DE1;

  //---VARIABLES---//
  private int state;
  private double myX,myY;
  private boolean isMoving;
  private int whoAmI;
  private double width;
  private int tourne;

  //---CONSTRUCTORS---//
  public Stage3SecondaryA() { super(); }

  //---ABSTRACT-METHODS-IMPLEMENTATION---//
  public void activate() {
    //ODOMETRY CODE
    whoAmI = ROCKY;
    for (IRadarResult o: detectRadar())
      if (isSameDirection(o.getObjectDirection(),Parameters.NORTH)) whoAmI=ROCKY;
    if (whoAmI == ROCKY){
      myX=Parameters.teamASecondaryBot2InitX;
      myY=Parameters.teamASecondaryBot2InitY;
    } else {
      myX=0;
      myY=0;
    }

    //INIT
    state=(whoAmI==ROCKY)?TOURNESASK:SINK;
    isMoving=false;
    tourne=0;
  }
  public void step() {
    //ODOMETRY CODE
    if (isMoving && whoAmI == ROCKY){
      myX+=Parameters.teamASecondaryBotSpeed*Math.cos(myGetHeading());
      myY+=Parameters.teamASecondaryBotSpeed*Math.sin(myGetHeading());
      isMoving=false;
    }
    //AUTOMATON
    if (state==TOURNESASK && !(isSameDirection(myGetHeading(),Parameters.SOUTH))) {
      stepTurn(Parameters.Direction.RIGHT);
      return;
    }
    if (state==TOURNESASK && isSameDirection(myGetHeading(),Parameters.SOUTH)) {
      state=MOVESASK;
      myMove();
      return;
    }
    if (state==MOVESASK && detectFront().getObjectType()!=IFrontSensorResult.Types.WALL) {
      myMove();
      return;
    }
    if (state==MOVESASK && detectFront().getObjectType()==IFrontSensorResult.Types.WALL) {
      state=TURNEASTTASK;
      stepTurn(Parameters.Direction.LEFT);
      return;
    }
    if (state==TURNEASTTASK && !(isSameDirection(myGetHeading(),Parameters.EAST))) {
      stepTurn(Parameters.Direction.LEFT);
      return;
    }
    if (state==TURNEASTTASK && isSameDirection(myGetHeading(),Parameters.EAST)) {
      state=MOVEEASTTASK;
      myMove();
      return;
    }
    if (state==MOVEEASTTASK && detectFront().getObjectType()!=IFrontSensorResult.Types.WALL && myX<1000 && tourne==0) {
      myMove(); 
      return;
    }
    if (state==MOVEEASTTASK && detectFront().getObjectType()!=IFrontSensorResult.Types.WALL && myX>=1000 && tourne==0) {
      tourne=1;
      state=TOURNEASK;
      stepTurn(Parameters.Direction.RIGHT);
      return;
    }
    if (state==MOVEEASTTASK && detectFront().getObjectType()!=IFrontSensorResult.Types.WALL && myX<1500 && tourne==1) {
      myMove(); 
      return;
    }
    if (state==MOVEEASTTASK && detectFront().getObjectType()!=IFrontSensorResult.Types.WALL && myX>=1500 && tourne==1) {
      tourne=2;
      state=TOURNEASK;
      stepTurn(Parameters.Direction.RIGHT);
      return;
    }
    if (state==MOVEEASTTASK && detectFront().getObjectType()!=IFrontSensorResult.Types.WALL && myX<2000 && tourne==2) {
      myMove(); 
      return;
    }
    if (state==MOVEEASTTASK && detectFront().getObjectType()!=IFrontSensorResult.Types.WALL && myX>=2000 && tourne==2) {
      tourne=3;
      state=TOURNEASK;
      stepTurn(Parameters.Direction.RIGHT);
      return;
    }
    if (state==MOVEEASTTASK && detectFront().getObjectType()!=IFrontSensorResult.Types.WALL && tourne>0 && tourne<4) {
      myMove(); 
      return;
    }
    if (state==MOVEEASTTASK && detectFront().getObjectType()==IFrontSensorResult.Types.WALL) {
      state=SINK;
      return;
    }
    if (state==TOURNEASK && !(isSameDirection(myGetHeading(),Parameters.WEST))) {
      stepTurn(Parameters.Direction.RIGHT);
      return;
    }
    if (state==TOURNEASK && isSameDirection(myGetHeading(),Parameters.WEST)) {
      state=UTURNAGAINTASK;
      stepTurn(Parameters.Direction.RIGHT);
      return;
    }
    if (state==UTURNAGAINTASK && !(isSameDirection(myGetHeading(),Parameters.EAST))) {
      stepTurn(Parameters.Direction.RIGHT);
      return;
    }
    if (state==UTURNAGAINTASK && isSameDirection(myGetHeading(),Parameters.EAST)) {
      state=MOVEEASTTASK;
      myMove();
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
