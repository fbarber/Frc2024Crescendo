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

package team492.autotasks;

import TrcCommonLib.trclib.TrcAutoTask;
import TrcCommonLib.trclib.TrcDbgTrace;
import TrcCommonLib.trclib.TrcEvent;
import TrcCommonLib.trclib.TrcOwnershipMgr;
import TrcCommonLib.trclib.TrcRobot;
import TrcCommonLib.trclib.TrcTaskMgr;
import TrcCommonLib.trclib.TrcUtil;
import TrcFrcLib.frclib.FrcPhotonVision;
import team492.Robot;
import team492.vision.PhotonVision.PipelineType;

/**
 * This class implements auto-assist task.
 */
public class TaskAutoPickupFromGround extends TrcAutoTask<TaskAutoPickupFromGround.State>
{
    private static final String moduleName = TaskAutoPickupFromGround.class.getSimpleName();

    public enum State
    {
        START,
        DETECT_NOTE, 
        PREP_TO_DRIVE,
        DRIVE_TO_NOTE,
        DONE
    }   //enum State

    private static class TaskParams
    {
        boolean inAuto;
        TaskParams(boolean inAuto)
        {
            this.inAuto = inAuto;
        }   //TaskParams
    }   //class TaskParams

    private final String ownerName;
    private final Robot robot;
    private final TrcEvent event;


    private final TrcDbgTrace msgTracer;
    private String currOwner = null;
    private String driveOwner = null;

    /**
     * Constructor: Create an instance of the object.
     *
     * @param ownerName specifies the owner name to take subsystem ownership, can be null if no ownership required.
     * @param robot specifies the robot object that contains all the necessary subsystems.
     */
    public TaskAutoPickupFromGround(String ownerName, Robot robot, TrcDbgTrace msgTracer)
    {
        super(moduleName, ownerName, TrcTaskMgr.TaskType.POST_PERIODIC_TASK);
        this.ownerName = ownerName;
        this.robot = robot;
        this.msgTracer = msgTracer;
        this.event = new TrcEvent(moduleName + ".event");
    }   //TaskAutoPickupFromGround

    /**
     * This method starts the auto-assist operation.
     *
     * @param completionEvent specifies the event to signal when done, can be null if none provided.
     */
    public void autoAssistPickup(TrcEvent completionEvent, boolean inAuto)
    {
        tracer.traceInfo(
            moduleName, 
            "event=" + completionEvent + 
            ", inAuto=" + inAuto);
        startAutoTask(State.START, new TaskParams(inAuto), completionEvent);
    }   //autoAssistPickup

    /**
     * This method cancels an in progress auto-assist operation if any.
     */
    public void autoAssistCancel()
    {
        final String funcName = "autoAssistCancel";

        if(msgTracer != null){
            tracer.traceInfo(moduleName, "Canceling auto-assist.");
        }
        stopAutoTask(false);
    }   //autoAssistCancel

    //
    // Implement TrcAutoTask abstract methods.
    //

    /**
     * This method is called by the super class to acquire ownership of all subsystems involved in the auto-assist
     * operation. This is typically done before starting an auto-assist operation.
     *
     * @return true if acquired all subsystems ownership, false otherwise. It releases all ownership if any acquire
     *         failed.
     */
    @Override
    protected boolean acquireSubsystemsOwnership()
    {
        // TODO: Figure out subsystem owenership aquisition
        boolean success = ownerName == null ||
                          (robot.robotDrive.driveBase.acquireExclusiveAccess(ownerName));

        if (success)
        {
            currOwner = ownerName;
            tracer.traceInfo(moduleName, "Successfully acquired subsystem ownerships.");
        }
        else
        {
            TrcOwnershipMgr ownershipMgr = TrcOwnershipMgr.getInstance();
            tracer.traceWarn(
                moduleName,
                "Failed to acquire subsystem ownership (currOwner=" + currOwner +
                ", robotDrive=" + ownershipMgr.getOwner(robot.robotDrive.driveBase) + ").");
            releaseSubsystemsOwnership();
        }

        return success;
    }   //acquireSubsystemsOwnership

    /**
     * This method is called by the super class to release ownership of all subsystems involved in the auto-assist
     * operation. This is typically done if the auto-assist operation is completed or canceled.
     */
    @Override
    protected void releaseSubsystemsOwnership()
    {
        // TODO: Figure out subsystem ownership release
        if (ownerName != null)
        {
            TrcOwnershipMgr ownershipMgr = TrcOwnershipMgr.getInstance();
            tracer.traceInfo(
                moduleName,
                "Releasing subsystem ownership (currOwner=" + currOwner +
                ", robotDrive=" + ownershipMgr.getOwner(robot.robotDrive.driveBase) + ").");
            robot.robotDrive.driveBase.releaseExclusiveAccess(currOwner);
            currOwner = null;
        }

        if (driveOwner != null)
        {
            robot.robotDrive.driveBase.releaseExclusiveAccess(driveOwner);
            driveOwner = null;
        }


        robot.intake.releaseExclusiveAccess(currOwner);
        currOwner = null;
    }   //releaseSubsystemsOwnership

    /**
     * This method is called by the super class to stop all the subsystems.
     */
    @Override
    protected void stopSubsystems()
    {
        tracer.traceInfo(moduleName, "Stopping subsystems.");
        robot.robotDrive.cancel(currOwner);
    }   //stopSubsystems

    /**
     * This methods is called periodically to run the auto-assist task.
     *
     * @param params specifies the task parameters.
     * @param state specifies the current state of the task.
     * @param taskType specifies the type of task being run.
     * @param runMode specifies the competition mode (e.g. Autonomous, TeleOp, Test).
     * @param slowPeriodicLoop specifies true if it is running the slow periodic loop on the main robot thread,
     *        false if running the fast loop on the main robot thread.
     */
    @Override
    protected void runTaskState(
        Object params, State state, TrcTaskMgr.TaskType taskType, TrcRobot.RunMode runMode, boolean slowPeriodicLoop)
    {
        TaskParams taskParams = (TaskParams) params;

        switch (state)
        {
            case START:
                // We are checking if the back camera is working, and if so, moving to the DETECT_NOTE state

                if(robot.photonVisionBack != null){
                    tracer.traceInfo(moduleName, "Using Vision to find Note.");
                    sm.setState(State.DETECT_NOTE);
                } else{
                    // Back camera is not working, moving to DONE State
                    tracer.traceInfo(moduleName, "Error in Camera, moving to DONE State");
                    sm.setState(State.DONE);
                }

                break;

            case DETECT_NOTE:
                    
                FrcPhotonVision.DetectedObject object = robot.photonVisionBack.getBestDetectedObject();
                
                if(object != null){
                // TODO: Write Logic
                // TODO: Logic needs review
                    

                    // double targetDist = TrcUtil.magnitude(object., object.targetPose.y);
                    // double targetAngle = object.targetPose.


                    
                }
                // TODO: Write State
                break;

            case PREP_TO_DRIVE:
                // TODO: Write State
                // Will tuck in shooter, and prep subsystems to pickup object
                break;

            case DRIVE_TO_NOTE:
                // TODO: Write State
                // Will implement driving code
                break;


            default:
            case DONE:
                // Stop task.
                stopAutoTask(true);
                break;
        }
    }   //runTaskState
 
}   //class TaskAutoPickupFromGround
