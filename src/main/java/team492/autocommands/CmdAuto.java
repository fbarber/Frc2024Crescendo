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

package team492.autocommands;

import TrcCommonLib.trclib.TrcEvent;
import TrcCommonLib.trclib.TrcRobot;
import TrcCommonLib.trclib.TrcStateMachine;
import TrcCommonLib.trclib.TrcTimer;
import team492.Robot;
import team492.FrcAuto.AutoChoices;

/**
 * This class implements an autonomous strategy.
 */
public class CmdAuto implements TrcRobot.RobotCommand
{
    private static final String moduleName = CmdAuto.class.getSimpleName();

    private enum State
    {
        START,
        DONE
    }   //enum State

    private final Robot robot;
    private final AutoChoices autoChoices;
    private final TrcTimer timer;
    private final TrcEvent event;
    private final TrcStateMachine<State> sm;

    /**
     * Constructor: Create an instance of the object.
     *
     * @param robot specifies the robot object for providing access to various global objects.
     * @param autoChoices specifies the autoChoices object.
     */
    public CmdAuto(Robot robot, AutoChoices autoChoices)
    {
        this.robot = robot;
        this.autoChoices = autoChoices;

        timer = new TrcTimer(moduleName);
        event = new TrcEvent(moduleName);
        sm = new TrcStateMachine<>(moduleName);
        sm.start(State.START);
    }   //CmdAuto

    //
    // Implements the TrcRobot.RobotCommand interface.
    //

    /**
     * This method checks if the current RobotCommand  is running.
     *
     * @return true if the command is running, false otherwise.
     */
    @Override
    public boolean isActive()
    {
        return sm.isEnabled();
    }   //isActive

    /**
     * This method cancels the command if it is active.
     */
    @Override
    public void cancel()
    {
        timer.cancel();
        sm.stop();
    }   //cancel

    /**
     * This method must be called periodically by the caller to drive the command sequence forward.
     *
     * @param elapsedTime specifies the elapsed time in seconds since the start of the robot mode.
     * @return true if the command sequence is completed, false otherwise.
     */
    @Override
    public boolean cmdPeriodic(double elapsedTime)
    {
        State state = sm.checkReadyAndGetState();

        if (state == null)
        {
            robot.dashboard.displayPrintf(8, "State: disabled or waiting (nextState=" + sm.getNextState() + ")...");
        }
        else
        {
            robot.dashboard.displayPrintf(8, "State: " + state);

            /*
             * START:
             *      - Set up robot starting location according to autoChoices.
             *      - Call autoScoreNote to score pre-load: targetType=Speaker/Amp, useVision, relocalize, shootInPlace only for Speaker.
             *      - goto DO_DELAY.
             * DO_DELAY:
             *      - if there is delay
             *      -   do delay and goto DRIVE_TO_WING_NOTE.
             *      - else
             *      -   goto DRIVE_TO_WING_NOTE.
             * DRIVE_TO_WING_NOTE:
             *      - if autoChoices said yes to score wing note
             *      -   determine which wing note to pick up and drive there then goto PICKUP_WING_NOTE.
             *      - else
             *      -   goto DRIVE_TO_END_ACTION.
             * PICKUP_WING_NOTE:
             *      - Call autoPickupFromGround: useVision or blind???
             *      - goto DRIVE_TO_SCORE_POINT.
             * DRIVE_TO_SCORE_POINT:
             *      - Determine the ScorePoint and drive there.
             *      - goto SCORE_WING_NOTE.
             * SCORE_WING_NOTE:
             *      - Call autoScoreNote to score wing note: targetType=Speaker/Amp, useVision, relocalize, shootInPlace only for Speaker.
             *      - goto DRIVE_TO_END_ACTION.
             * DRIVE_TO_END_ACTION:
             *      - Plan a path to the appropriate location according to End Action and drive there.
             *      - goto PERFORM_END_ACTION.
             * PERFORM_END_ACTION:
             *      - if EndAction is PARK_STARTING_ZONE or PARK_WING_ZONE
             *      -   goto DONE
             *      - else
             *      -   Call autoPickupFromGround to pick up center-line Note: useVision or blind???
             *      -   goto DONE
             * DONE:
             *      - Quit.
             */
            switch (state)
            {
                case START:
                    double startDelay = autoChoices.getStartDelay();
                    if (startDelay > 0.0)
                    {
                        timer.set(startDelay, event);
                        sm.waitForSingleEvent(event, State.DONE);
                    }
                    else
                    {
                        sm.setState(State.DONE);
                    }
                    break;

                default:
                case DONE:
                    // We are done.
                    cancel();
                    break;
            }

            robot.globalTracer.traceStateInfo(
                sm.toString(), state, robot.robotDrive.driveBase, robot.robotDrive.pidDrive,
                robot.robotDrive.purePursuitDrive, null);
        }

        return !sm.isEnabled();
    }   //cmdPeriodic

}   //class CmdAuto
