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
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package team492;

import java.util.Arrays;

import TrcCommonLib.trclib.TrcRobot;
import TrcCommonLib.trclib.TrcDriveBase.DriveOrientation;
import TrcCommonLib.trclib.TrcRobot.RunMode;
import TrcFrcLib.frclib.FrcJoystick;
import TrcFrcLib.frclib.FrcXboxController;

/**
 * This class implements the code to run in TeleOp Mode.
 */
public class FrcTeleOp implements TrcRobot.RobotMode
{
    private static final String moduleName = FrcTeleOp.class.getSimpleName();
    private static final boolean traceButtonEvents = true;
    //
    // Global objects.
    //
    protected final Robot robot;
    private boolean controlsEnabled = false;
    private double driveSpeedScale = RobotParams.DRIVE_NORMAL_SCALE;
    private double turnSpeedScale = RobotParams.TURN_NORMAL_SCALE;
    private double[] prevDriveInputs = null;
    private static final double shooterMaxVel = RobotParams.Shooter.shooterMaxVelocity; // in rps.
    private static final double shooterMinInc = 1.0;    // in rps.
    private static final double shooterMaxInc = 10.0;   // in rps.
    private Double prevShooterVel = null;
    private double presetShooterVel = 0.0;
    private double presetShooterInc = 10.0;
    private Double prevTiltPower = null;
    private boolean manualOverride = false;

    /**
     * Constructor: Create an instance of the object.
     *
     * @param robot specifies the robot object to access all robot hardware and subsystems.
     */
    public FrcTeleOp(Robot robot)
    {
        //
        // Create and initialize global object.
        //
        this.robot = robot;
    }   //FrcTeleOp

    //
    // Implements TrcRobot.RunMode interface.
    //

    /**
     * This method is called when the teleop mode is about to start. Typically, you put code that will prepare
     * the robot for start of teleop here such as creating and configuring joysticks and other subsystems.
     *
     * @param prevMode specifies the previous RunMode it is coming from.
     * @param nextMode specifies the next RunMode it is going into.
     */
    @Override
    public void startMode(RunMode prevMode, RunMode nextMode)
    {
        //
        // Enabling joysticks.
        //
        setControlsEnabled(true);
        //
        // Initialize subsystems for TeleOp mode if necessary.
        //
        if (robot.robotDrive != null)
        {
            robot.robotDrive.driveBase.setDriveOrientation(DriveOrientation.FIELD, true);
        }

        if (RobotParams.Preferences.hybridMode)
        {
            // This makes sure that the autonomous stops running when
            // teleop starts running. If you want the autonomous to
            // continue until interrupted by another command, remove
            // this line or comment it out.
            if (robot.m_autonomousCommand != null)
            {
                robot.m_autonomousCommand.cancel();
            }
        }
    }   //startMode

    /**
     * This method is called when teleop mode is about to end. Typically, you put code that will do clean
     * up here such as disabling joysticks and other subsystems.
     *
     * @param prevMode specifies the previous RunMode it is coming from.
     * @param nextMode specifies the next RunMode it is going into.
     */
    @Override
    public void stopMode(RunMode prevMode, RunMode nextMode)
    {
        //
        // Disabling joysticks.
        //
        setControlsEnabled(false);
        //
        // Disable subsystems before exiting if necessary.
        //
        releaseAutoAssistAndSubsystems();
    }   //stopMode

    /**
     * This method is called periodically on the main robot thread. Typically, you put TeleOp control code here that
     * doesn't require frequent update For example, TeleOp joystick code or status display code can be put here since
     * human responses are considered slow.
     *
     * @param elapsedTime specifies the elapsed time since the mode started.
     * @param slowPeriodicLoop specifies true if it is running the slow periodic loop on the main robot thread,
     *        false otherwise.
     */
    @Override
    public void periodic(double elapsedTime, boolean slowPeriodicLoop)
    {
        int lineNum = 1;

        if (slowPeriodicLoop)
        {
            if (controlsEnabled)
            {
                //
                // DriveBase operation.
                //
                if (robot.robotDrive != null)
                {
                    double[] driveInputs = robot.robotDrive.getDriveInputs(
                        RobotParams.ROBOT_DRIVE_MODE, true, driveSpeedScale, turnSpeedScale);
                    if (!Arrays.equals(driveInputs, prevDriveInputs))
                    {
                        if (robot.robotDrive.driveBase.supportsHolonomicDrive())
                        {
                            robot.robotDrive.driveBase.holonomicDrive(
                                null, driveInputs[0], driveInputs[1], driveInputs[2],
                                robot.robotDrive.driveBase.getDriveGyroAngle());
                            robot.dashboard.displayPrintf(
                                lineNum++, "Holonomic: x=%.3f, y=%.3f, rot=%.3f",
                                driveInputs[0], driveInputs[1], driveInputs[2]);
                        }
                        else if (RobotParams.Preferences.useTankDrive)
                        {
                            robot.robotDrive.driveBase.tankDrive(driveInputs[0], driveInputs[1]);
                            robot.dashboard.displayPrintf(
                                lineNum++, "Tank: left=%.3f, right=%.3f, rot=%.3f",
                                driveInputs[0], driveInputs[1], driveInputs[2]);
                        }
                        else
                        {
                            robot.robotDrive.driveBase.arcadeDrive(driveInputs[1], driveInputs[2]);
                            robot.dashboard.displayPrintf(
                                lineNum++, "Arcade: x=%.3f, y=%.3f, rot=%.3f",
                                driveInputs[0], driveInputs[1], driveInputs[2]);
                        }
                    }
                    prevDriveInputs = driveInputs;
                }
                //
                // Analog control of subsystem is done here if necessary.
                //
                if (RobotParams.Preferences.useSubsystems)
                {
                    if (robot.shooter != null)
                    {
                        double shooterVel =
                            (robot.operatorController.getRightTriggerAxis() -
                             robot.operatorController.getLeftTriggerAxis()) * shooterMaxVel;
                        if (presetShooterVel != 0.0)
                        {
                            if (shooterVel == 0.0)
                            {
                                // We have a button preset velocity and joystick is not overriding, set shooter to
                                // preset velocity.
                                shooterVel = presetShooterVel;
                            }
                            else
                            {
                                // We have a button preset velocity but joystick is overriding, set shooter to
                                // joystick value and clear button preset velocity.
                                presetShooterVel = 0.0;
                            }
                        }

                        if (prevShooterVel == null || prevShooterVel != shooterVel)
                        {
                            // Only set shooter velocity if it is different from previous velocity.
                            if (shooterVel == 0.0)
                            {
                                // Don't abruptly stop the shooter, gently spin down.
                                robot.shooter.stopShooter();
                            }
                            else
                            {
                                robot.shooter.setShooterVelocity(shooterVel);
                            }
                            prevShooterVel = shooterVel;
                        }
                        robot.dashboard.displayPrintf(
                            lineNum++, "Shooter: vel=%.0f/%.0f, preset=%.0f, inc=%.0f",
                            shooterVel, robot.shooter.getShooterVelocity(), presetShooterVel, presetShooterInc);

                        // Controlling tilt angle.
                        double tiltPower = robot.operatorController.getLeftYWithDeadband(true);
                        if (prevTiltPower == null || prevTiltPower != tiltPower)
                        {
                            robot.shooter.setTiltPower(tiltPower);
                        }
                        robot.dashboard.displayPrintf(
                            lineNum++, "Tilt: power=%.2f/%.2f, angle=%.2f/%f, limits=%s/%s",
                            tiltPower, robot.shooter.getTiltPower(),
                            robot.shooter.getTiltAngle(),
                            robot.shooter.tiltMotor.getMotorPosition(),
                            robot.shooter.tiltLowerLimitSwitchActive(),
                            robot.shooter.tiltUpperLimitSwitchActive());
                    }
                }
            }
            //
            // Update robot status.
            //
            if (RobotParams.Preferences.doStatusUpdate)
            {
                robot.updateStatus();
            }
        }
    }   //periodic

    /**
     * This method enables/disables joystick controls.
     *
     * @param enabled specifies true to enable joystick control, false to disable.
     */
    protected void setControlsEnabled(boolean enabled)
    {
        controlsEnabled = enabled;

        // if (!RobotParams.Preferences.hybridMode)
        // {
        if (RobotParams.Preferences.useDriverXboxController)
        {
            robot.driverController.setButtonHandler(enabled? this::driverControllerButtonEvent: null);
        }
        else
        {
            robot.leftDriveStick.setButtonHandler(enabled? this::leftDriveStickButtonEvent: null);
            robot.rightDriveStick.setButtonHandler(enabled? this::rightDriveStickButtonEvent: null);
        }

        if (RobotParams.Preferences.useOperatorXboxController)
        {
            robot.operatorController.setButtonHandler(enabled? this::operatorControllerButtonEvent: null);
        }
        else
        {
            robot.operatorStick.setButtonHandler(enabled? this::operatorStickButtonEvent: null);
        }
        // }

        if (RobotParams.Preferences.useButtonPanels)
        {
            robot.buttonPanel.setButtonHandler(enabled? this::buttonPanelButtonEvent: null);
            robot.switchPanel.setButtonHandler(enabled? this::switchPanelButtonEvent: null);
        }
    }   //setControlsEnabled

    //
    // Implements FrcButtonHandler.
    //

    /**
     * This method is called when a driver controller button event is detected.
     *
     * @param button specifies the button ID that generates the event.
     * @param pressed specifies true if the button is pressed, false otherwise.
     */
    private void driverControllerButtonEvent(int button, boolean pressed)
    {
        if (traceButtonEvents)
        {
            robot.globalTracer.traceInfo(moduleName, ">>>>> button=%d, pressed=%s", button, pressed);
        }

        robot.dashboard.displayPrintf(
            8, "DriverController: button=0x%04x %s", button, pressed ? "pressed" : "released");

        switch (button)
        {
            case FrcXboxController.BUTTON_A:
                // Toggle between field or robot oriented driving.
                if (robot.robotDrive != null && pressed)
                {
                    if (robot.robotDrive.driveBase.getDriveOrientation() != DriveOrientation.FIELD)
                    {
                        robot.robotDrive.driveBase.setDriveOrientation(DriveOrientation.FIELD, true);
                    }
                    else
                    {
                        robot.robotDrive.driveBase.setDriveOrientation(DriveOrientation.ROBOT, false);
                    }
                }
                break;

            case FrcXboxController.BUTTON_B:
                break;

            case FrcXboxController.LEFT_BUMPER:
                if (pressed)
                {
                    driveSpeedScale = RobotParams.DRIVE_SLOW_SCALE;
                    turnSpeedScale = RobotParams.TURN_SLOW_SCALE;
                }
                else
                {
                    driveSpeedScale = RobotParams.DRIVE_NORMAL_SCALE;
                    turnSpeedScale = RobotParams.TURN_NORMAL_SCALE;
                }
                break;

            case FrcXboxController.RIGHT_BUMPER:
                if (robot.robotDrive != null &&
                    robot.robotDrive.driveBase.getDriveOrientation() == DriveOrientation.ROBOT)
                {
                    // Inverted drive only makes sense for robot oriented driving.
                    robot.robotDrive.driveBase.setDriveOrientation(
                        pressed? DriveOrientation.INVERTED: DriveOrientation.ROBOT, false);
                }
                break;

            case FrcXboxController.BACK:
                break;

            case FrcXboxController.START:
                break;

            case FrcXboxController.LEFT_STICK_BUTTON:
                break;

            case FrcXboxController.RIGHT_STICK_BUTTON:
                break;
        }
    }   //driverControllerButtonEvent

    /**
     * This method is called when an operator controller button event is detected.
     *
     * @param button specifies the button ID that generates the event.
     * @param pressed specifies true if the button is pressed, false otherwise.
     */
    private void operatorControllerButtonEvent(int button, boolean pressed)
    {
        if (traceButtonEvents)
        {
            robot.globalTracer.traceInfo(moduleName, ">>>>> button=%d, pressed=%s", button, pressed);
        }

        robot.dashboard.displayPrintf(
            8, "OperatorController: button=0x%04x %s", button, pressed ? "pressed" : "released");

        switch (button)
        {
            case FrcXboxController.BUTTON_A:
                if (robot.intake != null)
                {
                    if (pressed)
                    {
                        robot.intake.autoIntake(RobotParams.Intake.intakePower, 0.0, 0.0);
                    }
                    else
                    {
                        robot.intake.cancel();
                    }
                }
                break;

            case FrcXboxController.BUTTON_B:
                if (robot.intake != null)
                {
                    if (pressed)
                    {
                        if (manualOverride)
                        {
                            // AKA: spit!
                            robot.intake.autoEjectReverse(RobotParams.Intake.ejectReversePower, 0.0);
                        }
                        else
                        {
                            // AKA: shoot!
                            robot.intake.autoEjectForward(RobotParams.Intake.ejectForwardPower, 0.0);
                        }
                    }
                    else
                    {
                        robot.intake.cancel();
                    }
                }
                break;

            case FrcXboxController.BUTTON_X:
                if (pressed && robot.climber != null)
                {
                    robot.climber.extend();
                }
                break;

            case FrcXboxController.BUTTON_Y:
                if (pressed && robot.climber != null)
                {
                    robot.climber.retract();
                }
                break;

            case FrcXboxController.LEFT_BUMPER:
                manualOverride = pressed;
                if (robot.shooter != null)
                {
                    robot.shooter.setManualOverrideEnabled(manualOverride);
                }
                break;

            case FrcXboxController.RIGHT_BUMPER:
                break;

            case FrcXboxController.DPAD_UP:
                if (pressed)
                {
                    if (presetShooterVel + presetShooterInc <= shooterMaxVel)
                    {
                        presetShooterVel += presetShooterInc;
                    }
                }
                break;

            case FrcXboxController.DPAD_DOWN:
                if (pressed)
                {
                    if (presetShooterVel - presetShooterInc >= -shooterMaxVel)
                    {
                        presetShooterVel -= presetShooterInc;
                    }
                }
                break;

            case FrcXboxController.DPAD_LEFT:
                if (pressed)
                {
                    if (presetShooterInc * 10.0 <= shooterMaxInc)
                    {
                        presetShooterInc *= 10.0;
                    }
                }
                break;

            case FrcXboxController.DPAD_RIGHT:
                if (pressed)
                {
                    if (presetShooterInc / 10.0 >= shooterMinInc)
                    {
                        presetShooterInc /= 10.0;
                    }
                }
                break;

            case FrcXboxController.BACK:
                break;

            case FrcXboxController.START:
                break;

            case FrcXboxController.LEFT_STICK_BUTTON:
                break;

            case FrcXboxController.RIGHT_STICK_BUTTON:
                break;
        }
    }   //operatorControllerButtonEvent

    /**
     * This method is called when a right driver stick button event is detected.
     *
     * @param button specifies the button ID that generates the event
     * @param pressed specifies true if the button is pressed, false otherwise.
     */
    private void leftDriveStickButtonEvent(int button, boolean pressed)
    {
        if (traceButtonEvents)
        {
            robot.globalTracer.traceInfo(moduleName, ">>>>> button=%d, pressed=%s", button, pressed);
        }

        robot.dashboard.displayPrintf(
            8, "LeftDriveStick: button=0x%04x %s", button, pressed ? "pressed" : "released");

        switch (button)
        {
            case FrcJoystick.LOGITECH_TRIGGER:
                break;

            case FrcJoystick.LOGITECH_BUTTON2:
                break;

            case FrcJoystick.LOGITECH_BUTTON3:
                break;

            case FrcJoystick.LOGITECH_BUTTON4:
                break;

            case FrcJoystick.LOGITECH_BUTTON5:
                break;

            case FrcJoystick.LOGITECH_BUTTON6:
                break;

            case FrcJoystick.LOGITECH_BUTTON7:
                break;

            case FrcJoystick.LOGITECH_BUTTON8:
                break;

            case FrcJoystick.LOGITECH_BUTTON9:
                break;

            case FrcJoystick.LOGITECH_BUTTON10:
                break;

            case FrcJoystick.LOGITECH_BUTTON11:
                break;

            case FrcJoystick.LOGITECH_BUTTON12:
                break;
        }
    }   //leftDriveStickButtonEvent

    /**
     * This method is called when a right driver stick button event is detected.
     *
     * @param button specifies the button ID that generates the event
     * @param pressed specifies true if the button is pressed, false otherwise.
     */
    private void rightDriveStickButtonEvent(int button, boolean pressed)
    {
        if (traceButtonEvents)
        {
            robot.globalTracer.traceInfo(moduleName, ">>>>> button=%d, pressed=%s", button, pressed);
        }

        robot.dashboard.displayPrintf(
            8, "RightDriveStick: button=0x%04x %s", button, pressed ? "pressed" : "released");

        switch (button)
        {
            case FrcJoystick.SIDEWINDER_TRIGGER:
                // Toggle between field or robot oriented driving.
                if (robot.robotDrive != null && pressed)
                {
                    if (robot.robotDrive.driveBase.getDriveOrientation() != DriveOrientation.FIELD)
                    {
                        robot.robotDrive.driveBase.setDriveOrientation(DriveOrientation.FIELD, true);
                    }
                    else
                    {
                        robot.robotDrive.driveBase.setDriveOrientation(DriveOrientation.ROBOT, false);
                    }
                }
                break;

            case FrcJoystick.LOGITECH_BUTTON3:
                // Inverted drive only makes sense for robot oriented driving.
                if (robot.robotDrive != null &&
                    robot.robotDrive.driveBase.getDriveOrientation() == DriveOrientation.ROBOT)
                {
                    robot.robotDrive.driveBase.setDriveOrientation(
                        pressed? DriveOrientation.INVERTED: DriveOrientation.ROBOT, false);
                }
                break;
        }
    }   //rightDriveStickButtonEvent

    /**
     * This method is called when an operator stick button event is detected.
     *
     * @param button specifies the button ID that generates the event
     * @param pressed specifies true if the button is pressed, false otherwise.
     */
    private void operatorStickButtonEvent(int button, boolean pressed)
    {
        if (traceButtonEvents)
        {
            robot.globalTracer.traceInfo(moduleName, ">>>>> button=%d, pressed=%s", button, pressed);
        }

        robot.dashboard.displayPrintf(
            8, "OperatorStick: button=0x%04x %s", button, pressed ? "pressed" : "released");

        switch (button)
        {
            case FrcJoystick.LOGITECH_TRIGGER:
                break;

            case FrcJoystick.LOGITECH_BUTTON2:
                break;

            case FrcJoystick.LOGITECH_BUTTON3:
                break;

            case FrcJoystick.LOGITECH_BUTTON4:
                break;

            case FrcJoystick.LOGITECH_BUTTON5:
                break;

            case FrcJoystick.LOGITECH_BUTTON6:
                break;

            case FrcJoystick.LOGITECH_BUTTON7:
                break;

            case FrcJoystick.LOGITECH_BUTTON8:
                break;

            case FrcJoystick.LOGITECH_BUTTON9:
                break;

            case FrcJoystick.LOGITECH_BUTTON10:
                break;

            case FrcJoystick.LOGITECH_BUTTON11:
                break;

            case FrcJoystick.LOGITECH_BUTTON12:
                break;
        }
    }   //operatorStickButtonEvent

    /**
     * This method is called when a button panel button event is detected.
     *
     * @param button specifies the button ID that generates the event
     * @param pressed specifies true if the button is pressed, false otherwise.
     */
    private void buttonPanelButtonEvent(int button, boolean pressed)
    {
        if (traceButtonEvents)
        {
            robot.globalTracer.traceInfo(moduleName, ">>>>> button=%d, pressed=%s", button, pressed);
        }

        robot.dashboard.displayPrintf(
            8, "ButtonPanel: button=0x%04x %s", button, pressed ? "pressed" : "released");

        switch (button)
        {
            case FrcJoystick.PANEL_BUTTON_RED1:
                break;

            case FrcJoystick.PANEL_BUTTON_GREEN1:
                break;

            case FrcJoystick.PANEL_BUTTON_BLUE1:
                break;

            case FrcJoystick.PANEL_BUTTON_YELLOW1:
                break;

            case FrcJoystick.PANEL_BUTTON_WHITE1:
                break;

            case FrcJoystick.PANEL_BUTTON_RED2:
                break;

            case FrcJoystick.PANEL_BUTTON_GREEN2:
                break;

            case FrcJoystick.PANEL_BUTTON_BLUE2:
                break;

            case FrcJoystick.PANEL_BUTTON_YELLOW2:
                break;

            case FrcJoystick.PANEL_BUTTON_WHITE2:
                break;
        }
    }   //buttonPanelButtonEvent

    /**
     * This method is called when a switch panel button event is detected.
     *
     * @param button specifies the button ID that generates the event
     * @param pressed specifies true if the button is pressed, false otherwise.
     */
    private void switchPanelButtonEvent(int button, boolean pressed)
    {
        if (traceButtonEvents)
        {
            robot.globalTracer.traceInfo(moduleName, ">>>>> button=%d, pressed=%s", button, pressed);
        }

        robot.dashboard.displayPrintf(
            8, "SwitchPanel: button=0x%04x %s", button, pressed ? "pressed" : "released");

        switch (button)
        {
            case FrcJoystick.PANEL_SWITCH_WHITE1:
                break;

            case FrcJoystick.PANEL_SWITCH_RED1:
                break;

            case FrcJoystick.PANEL_SWITCH_GREEN1:
                break;

            case FrcJoystick.PANEL_SWITCH_BLUE1:
                break;

            case FrcJoystick.PANEL_SWITCH_YELLOW1:
                break;

            case FrcJoystick.PANEL_SWITCH_WHITE2:
                break;

            case FrcJoystick.PANEL_SWITCH_RED2:
                break;

            case FrcJoystick.PANEL_SWITCH_GREEN2:
                break;

            case FrcJoystick.PANEL_SWITCH_BLUE2:
                break;

            case FrcJoystick.PANEL_SWITCH_YELLOW2:
                break;
        }
    }   //switchPanelButtonEvent

    /**
     * This method is called to cancel all pending auto-assist operations and release the ownership of all subsystems.
     */
    private void releaseAutoAssistAndSubsystems()
    {
    }   //releaseAutoAssistAndSubsystems

}   //class FrcTeleOp
