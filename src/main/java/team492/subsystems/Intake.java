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

import TrcCommonLib.trclib.TrcUtil;
import TrcCommonLib.trclib.TrcPidConveyor;
import TrcFrcLib.frclib.FrcCANFalcon;
import TrcFrcLib.frclib.FrcDigitalInput;
import team492.RobotParams;

public class Intake
{
    private static final String moduleName = Intake.class.getSimpleName();

    private final FrcCANFalcon conveyorMotor;
    private final FrcDigitalInput entrySensor;
    private final FrcDigitalInput exitSensor;
    private final TrcPidConveyor conveyor;

    public Intake()
    {
        conveyorMotor = new FrcCANFalcon(moduleName + ".motor", RobotParams.Intake.candId);
        conveyorMotor.resetFactoryDefault();
        conveyorMotor.setMotorInverted(RobotParams.Intake.motorInverted);
        conveyorMotor.setBrakeModeEnabled(true);
        conveyorMotor.setVoltageCompensationEnabled(TrcUtil.BATTERY_NOMINAL_VOLTAGE);
        conveyorMotor.setCurrentLimit(20.0, 40.0, 0.5);

        entrySensor = new FrcDigitalInput(moduleName + ".entrySensor", RobotParams.Intake.entrySensorChannel);
        entrySensor.setInverted(RobotParams.Intake.entrySensorInverted);

        exitSensor = new FrcDigitalInput(moduleName + ".exitSensor", RobotParams.Intake.exitSensorChannel);
        exitSensor.setInverted(RobotParams.Intake.exitSensorInverted);

        conveyor = new TrcPidConveyor(moduleName, conveyorMotor, entrySensor, exitSensor, RobotParams.Intake.params);
    }   //Intake

    /**
     * This method returns the state of the Arm in a string.
     */
    @Override
    public String toString()
    {
        return moduleName +
               ": numObj=" + conveyor.getNumObjects() +
               ", entry=" + conveyor.isEntrySensorActive() +
               ", exit=" + conveyor.isExitSensorActive();
    }   //toString

    /**
     * This method returns the TrcPidConveyor object created.
     *
     * @return TrcPidConveyor object.
     */
    public TrcPidConveyor getPidConveyor()
    {
        return conveyor;
    }   //getPidConveyor

}   //class Intake