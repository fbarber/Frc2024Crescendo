/*
 * Copyright (c) 2024 Titan Robotics Club (http://www.titanrobotics.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


package team492.subsystems;

import TrcCommonLib.trclib.TrcEvent;
import TrcCommonLib.trclib.TrcUtil;
import TrcFrcLib.frclib.FrcCANFalcon;
import team492.Robot;
import team492.RobotParams;
// single motor turning the spool (lets telescope out) (slack = taller) 
// (max/min height)
// lower limit switch (limit in rotations)


public class Climber
{
    private static final String moduleName = Climber.class.getSimpleName();

    public final FrcCANFalcon climberMotor;

    public Climber()
    {
        climberMotor = new FrcCANFalcon(moduleName + ".motor", RobotParams.Climber.climberCandId);
        climberMotor.resetFactoryDefault();
        climberMotor.setMotorInverted(RobotParams.Climber.motorInverted);
        climberMotor.setBrakeModeEnabled(true);
        climberMotor.setVoltageCompensationEnabled(TrcUtil.BATTERY_NOMINAL_VOLTAGE);
        climberMotor.setPositionSensorScaleAndOffset(RobotParams.Climber.climberPosScale, 0.0);
        climberMotor.setPositionPidCoefficients(RobotParams.Climber.climberPosPidCoeff);

        climberMotor.enableLowerLimitSwitch(true);

    }   //Climber

    /**
     * This method returns the state of the Climber in a string.
     */
    @Override
    public String toString()
    {
        return moduleName +
               ", lowerLimit=" + climberMotor.isLowerLimitSwitchActive();
    }   //toString

    public FrcCANFalcon getClimberMotor()
    {
        return climberMotor;
    }   //get climberMotor

    public double getPosition()
    {
        return climberMotor.getPosition();
    }
    
    public void setPosition(double position)
    {
        climberMotor.setPosition(position);
    }

    public void setPosition(double position, TrcEvent completionEvent, double timeout)
    {
        climberMotor.setPosition(0.0, position, true, 1.0, completionEvent, timeout);
    }

}   //class Climber