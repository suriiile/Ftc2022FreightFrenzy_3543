/*
 * Copyright (c) 2021 Titan Robotics Club (http://www.titanrobotics.com)
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

package Ftc2022FreightFrenzy_3543;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import java.util.Locale;

import TrcCommonLib.command.CmdDriveMotorsTest;
import TrcCommonLib.command.CmdPidDrive;
import TrcCommonLib.command.CmdPurePursuitDrive;
import TrcCommonLib.command.CmdTimedDrive;

import TrcCommonLib.trclib.TrcElapsedTimer;
import TrcCommonLib.trclib.TrcGameController;
import TrcCommonLib.trclib.TrcPidController;
import TrcCommonLib.trclib.TrcPose2D;
import TrcCommonLib.trclib.TrcRobot;
import TrcCommonLib.trclib.TrcUtil;
import TrcFtcLib.ftclib.FtcChoiceMenu;
import TrcFtcLib.ftclib.FtcDcMotor;
import TrcFtcLib.ftclib.FtcGamepad;
import TrcFtcLib.ftclib.FtcMenu;
import TrcFtcLib.ftclib.FtcPidCoeffCache;
import TrcFtcLib.ftclib.FtcValueMenu;

/**
 * This class contains the Test Mode program.
 */
@TeleOp(name="FtcTest", group="Ftc3543")
public class FtcTest extends FtcTeleOp
{
    private static final boolean logEvents = true;
    private static final boolean debugPid = true;

    private enum Test
    {
        SENSORS_TEST,
        SUBSYSTEMS_TEST,
        DRIVE_SPEED_TEST,
        DRIVE_MOTORS_TEST,
        X_TIMED_DRIVE,
        Y_TIMED_DRIVE,
        PID_DRIVE,
        TUNE_X_PID,
        TUNE_Y_PID,
        TUNE_TURN_PID,
        PURE_PURSUIT_DRIVE
    }   //enum Test

    /**
     * This class stores the test menu choices.
     */
    private static class TestChoices
    {
        Test test = Test.SENSORS_TEST;
        double xTarget = 0.0;
        double yTarget = 0.0;
        double turnTarget = 0.0;
        double driveTime = 0.0;
        double drivePower = 0.0;
        TrcPidController.PidCoefficients tunePidCoeff = null;

        @Override
        public String toString()
        {
            return String.format(
                Locale.US,
                "test=\"%s\" " +
                "xTarget=%.1f " +
                "yTarget=%.1f " +
                "turnTarget=%1f " +
                "driveTime=%.1f " +
                "drivePower=%.1f " +
                "tunePidCoeff=%s",
                test, xTarget, yTarget, turnTarget, driveTime, drivePower, tunePidCoeff);
        }   //toString

    }   //class TestChoices

    private final FtcPidCoeffCache pidCoeffCache =
        new FtcPidCoeffCache("PIDTuning", RobotParams.LOG_PATH_FOLDER);
    private final TestChoices testChoices = new TestChoices();
    private TrcElapsedTimer elapsedTimer = null;
    private FtcChoiceMenu<Test> testMenu = null;

    private TrcRobot.RobotCommand testCommand = null;
    private double maxDriveVelocity = 0.0;
    private double maxDriveAcceleration = 0.0;
    private double prevTime = 0.0;
    private double prevVelocity = 0.0;

    //
    // Overrides FtcOpMode abstract method.
    //

    /**
     * This method is called to initialize the robot. In FTC, this is called when the "Init" button on the Driver
     * Station is pressed.
     */
    @Override
    public void initRobot()
    {
        //
        // TeleOp initialization.
        //
        super.initRobot();
        if (RobotParams.Preferences.useLoopPerformanceMonitor)
        {
            elapsedTimer = new TrcElapsedTimer("TestLoopMonitor", 2.0);
        }
        //
        // Test menus.
        //
        doTestMenus();
        //
        // Create the robot command for the tests that need one.
        //
        switch (testChoices.test)
        {
            case DRIVE_MOTORS_TEST:
                if (!RobotParams.Preferences.visionOnly)
                {
                    testCommand = new CmdDriveMotorsTest(
                        new FtcDcMotor[] {robot.robotDrive.leftFrontWheel, robot.robotDrive.rightFrontWheel,
                                          robot.robotDrive.leftBackWheel, robot.robotDrive.rightBackWheel},
                        5.0, 0.5);
                }
                break;

            case X_TIMED_DRIVE:
                if (!RobotParams.Preferences.visionOnly)
                {
                    testCommand = new CmdTimedDrive(
                        robot.robotDrive.driveBase, 0.0, testChoices.driveTime,
                        testChoices.drivePower, 0.0, 0.0);
                }
                break;

            case Y_TIMED_DRIVE:
                if (!RobotParams.Preferences.visionOnly)
                {
                    testCommand = new CmdTimedDrive(
                        robot.robotDrive.driveBase, 0.0, testChoices.driveTime,
                        0.0, testChoices.drivePower, 0.0);
                }
                break;

            case PID_DRIVE:
                if (!RobotParams.Preferences.visionOnly)
                {
                    testCommand = new CmdPidDrive(
                        robot.robotDrive.driveBase, robot.robotDrive.pidDrive, 0.0, testChoices.drivePower, null,
                        new TrcPose2D(testChoices.xTarget*12.0, testChoices.yTarget*12.0, testChoices.turnTarget));
                }
                break;

            case TUNE_X_PID:
                if (!RobotParams.Preferences.visionOnly)
                {
                    testCommand = new CmdPidDrive(
                        robot.robotDrive.driveBase, robot.robotDrive.pidDrive, 0.0, testChoices.drivePower,
                        testChoices.tunePidCoeff, new TrcPose2D(testChoices.xTarget*12.0, 0.0, 0.0));
                }
                break;

            case TUNE_Y_PID:
                if (!RobotParams.Preferences.visionOnly)
                {
                    testCommand = new CmdPidDrive(
                        robot.robotDrive.driveBase, robot.robotDrive.pidDrive, 0.0, testChoices.drivePower,
                        testChoices.tunePidCoeff, new TrcPose2D(0.0, testChoices.yTarget*12.0, 0.0));
                }
                break;

            case TUNE_TURN_PID:
                if (!RobotParams.Preferences.visionOnly)
                {
                    testCommand = new CmdPidDrive(
                        robot.robotDrive.driveBase, robot.robotDrive.pidDrive, 0.0, testChoices.drivePower,
                        testChoices.tunePidCoeff, new TrcPose2D(0.0, 0.0, testChoices.turnTarget));
                }
                break;

            case PURE_PURSUIT_DRIVE:
                if (!RobotParams.Preferences.visionOnly)
                {
                    testCommand = new CmdPurePursuitDrive(
                        robot.robotDrive.driveBase, robot.robotDrive.xPosPidCoeff, robot.robotDrive.yPosPidCoeff,
                        robot.robotDrive.turnPidCoeff, robot.robotDrive.velPidCoeff);
                }
                break;
        }
        //
        // Only SENSORS_TEST and SUBSYSTEMS_TEST need TensorFlow, shut it down for all other tests.
        //
        if (robot.vision != null && robot.vision.isTensorFlowVisionInitialized() &&
            testChoices.test != Test.SENSORS_TEST && testChoices.test != Test.SUBSYSTEMS_TEST)
        {
            robot.globalTracer.traceInfo("TestInit", "Shutting down TensorFlow.");
            robot.vision.tensorFlowShutdown();
        }
    }   //initRobot

    //
    // Overrides TrcRobot.RobotMode methods.
    //

    /**
     * This method is called before test mode is about to start so it can initialize appropriate subsystems for the
     * test.
     *
     * @param prevMode specifies the previous RunMode it is coming from (always null for FTC).
     * @param nextMode specifies the next RunMode it is going into.
     */
    @Override
    public void startMode(TrcRobot.RunMode prevMode, TrcRobot.RunMode nextMode)
    {
        final String funcName = "startMode";

        super.startMode(prevMode, nextMode);
        switch (testChoices.test)
        {
            case SENSORS_TEST:
            case SUBSYSTEMS_TEST:
                if (robot.vision != null)
                {
                    //
                    // Vision generally will impact performance, so we only enable it if it's needed.
                    //
                    if (robot.vision.isVuforiaVisionInitialized())
                    {
                        robot.globalTracer.traceInfo(funcName, "Enabling Vuforia.");
                        robot.vision.setVuforiaEnabled(true);
                    }

                    if (robot.vision.isTensorFlowVisionInitialized())
                    {
                        robot.globalTracer.traceInfo(funcName, "Enabling TensorFlow.");
                        robot.vision.setTensorFlowEnabled(true);
                    }
                }
                break;

            case PID_DRIVE:
            case TUNE_X_PID:
            case TUNE_Y_PID:
            case TUNE_TURN_PID:
                robot.robotDrive.pidDrive.setMsgTracer(robot.globalTracer, logEvents, debugPid);
                break;

            case PURE_PURSUIT_DRIVE:
                robot.robotDrive.purePursuitDrive.setMsgTracer(robot.globalTracer, logEvents, debugPid);
                //
                // Doing a 48x48-inch square box with robot heading always pointing to the center of the box.
                //
                // Set the current position as the absolute field origin so the path can be an absolute path.
                robot.robotDrive.driveBase.setFieldPosition(new TrcPose2D(0.0, 0.0, 0.0));
                ((CmdPurePursuitDrive)testCommand).start(
                    robot.robotDrive.driveBase.getFieldPosition(), false,
                    new TrcPose2D(-24.0, 0, 45.0),
                    new TrcPose2D(-24.0, 48.0, 135.0),
                    new TrcPose2D(24.0, 48.0, 225.0),
                    new TrcPose2D(24.0, 0.0, 315.0),
                    new TrcPose2D(0.0, 0.0, 0.0));
                break;
        }
    }   //startMode

    /**
     * This method is called before test mode is about to exit so it can do appropriate cleanup.
     *
     * @param prevMode specifies the previous RunMode it is coming from.
     * @param nextMode specifies the next RunMode it is going into (always null for FTC).
     */
    @Override
    public void stopMode(TrcRobot.RunMode prevMode, TrcRobot.RunMode nextMode)
    {
        final String funcName = "stopMode";

        if (testCommand != null)
        {
            testCommand.cancel();
        }

        if (robot.vision != null)
        {
            //
            // Vision generally will impact performance, so we only enable it if it's needed.
            //
            if (robot.vision.isVuforiaVisionInitialized())
            {
                robot.globalTracer.traceInfo(funcName, "Disabling Vuforia.");
                robot.vision.setVuforiaEnabled(false);
            }

            if (robot.vision.isTensorFlowVisionInitialized())
            {
                robot.globalTracer.traceInfo(funcName, "Shutting down TensorFlow.");
                robot.vision.tensorFlowShutdown();
            }
        }

        super.stopMode(prevMode, nextMode);
    }   //stopMode

    /**
     * This method is called periodically during test mode to perform low frequency tasks such as teleop control or
     * displaying status or test results.
     *
     * @param elapsedTime specifies the elapsed time since the mode started.
     */
    @Override
    public void runPeriodic(double elapsedTime)
    {
        if (shouldDoTeleOp())
        {
            //
            // Allow TeleOp to run so we can control the robot in subsystem test or drive speed test modes.
            //
            super.runPeriodic(elapsedTime);
        }

        switch (testChoices.test)
        {
            case SENSORS_TEST:
            case SUBSYSTEMS_TEST:
                doSensorsTest();
                doVisionTest();
                break;
        }
    }   //runPeriodic

    /**
     * This method is called continuously during test mode to execute the test command.
     *
     * @param elapsedTime specifies the elapsed time since the mode started.
     */
    @Override
    public void runContinuous(double elapsedTime)
    {
        //
        // Run the testCommand if any.
        //
        if (testCommand != null)
        {
            testCommand.cmdPeriodic(elapsedTime);
        }
        //
        // Display test status.
        //
        switch (testChoices.test)
        {
            case DRIVE_SPEED_TEST:
                if (!RobotParams.Preferences.visionOnly)
                {
                    double currTime = TrcUtil.getCurrentTime();
                    TrcPose2D velPose = robot.robotDrive.driveBase.getFieldVelocity();
                    double velocity = TrcUtil.magnitude(velPose.x, velPose.y);
                    double acceleration = 0.0;

                    if (prevTime != 0.0)
                    {
                        acceleration = (velocity - prevVelocity)/(currTime - prevTime);
                    }

                    if (velocity > maxDriveVelocity)
                    {
                        maxDriveVelocity = velocity;
                    }

                    if (acceleration > maxDriveAcceleration)
                    {
                        maxDriveAcceleration = acceleration;
                    }

                    prevTime = currTime;
                    prevVelocity = velocity;

                    robot.dashboard.displayPrintf(8, "Drive Vel: (%.1f/%.1f)", velocity, maxDriveVelocity);
                    robot.dashboard.displayPrintf(9, "Drive Accel: (%.1f/%.1f)", acceleration, maxDriveAcceleration);
                }
                break;

            case X_TIMED_DRIVE:
            case Y_TIMED_DRIVE:
                if (!RobotParams.Preferences.visionOnly)
                {
                    robot.dashboard.displayPrintf(8, "Timed Drive: %.0f sec", testChoices.driveTime);
                    robot.dashboard.displayPrintf(
                        9, "xPos=%.1f,yPos=%.1f,heading=%.1f",
                        robot.robotDrive.driveBase.getXPosition(), robot.robotDrive.driveBase.getYPosition(),
                        robot.robotDrive.driveBase.getHeading());
                    robot.dashboard.displayPrintf(
                        10, "raw=lf:%.0f,rf:%.0f,lb:%.0f,rb:%.0f",
                        robot.robotDrive.leftFrontWheel.getPosition(), robot.robotDrive.rightFrontWheel.getPosition(),
                        robot.robotDrive.leftBackWheel.getPosition(), robot.robotDrive.rightBackWheel.getPosition());
                }
                break;

            case PID_DRIVE:
            case TUNE_X_PID:
            case TUNE_Y_PID:
            case TUNE_TURN_PID:
                if (!RobotParams.Preferences.visionOnly)
                {
                    robot.dashboard.displayPrintf(
                        8, "xPos=%.1f,yPos=%.1f,heading=%.1f,raw=lf:%.0f,rf:%.0f,lb:%.0f,rb:%.0f",
                        robot.robotDrive.driveBase.getXPosition(), robot.robotDrive.driveBase.getYPosition(),
                        robot.robotDrive.driveBase.getHeading(),
                        robot.robotDrive.leftFrontWheel.getPosition(), robot.robotDrive.rightFrontWheel.getPosition(),
                        robot.robotDrive.leftBackWheel.getPosition(), robot.robotDrive.rightBackWheel.getPosition());
                    if (robot.robotDrive.encoderXPidCtrl != null)
                    {
                        robot.robotDrive.encoderXPidCtrl.displayPidInfo(9);
                    }
                    if (robot.robotDrive.encoderYPidCtrl != null)
                    {
                        robot.robotDrive.encoderYPidCtrl.displayPidInfo(11);
                    }
                    if (robot.robotDrive.gyroPidCtrl != null)
                    {
                        robot.robotDrive.gyroPidCtrl.displayPidInfo(13);
                    }
                }
                break;

            case PURE_PURSUIT_DRIVE:
                if (!RobotParams.Preferences.visionOnly)
                {
                    robot.dashboard.displayPrintf(
                        8, "xPos=%.1f,yPos=%.1f,heading=%.1f,rawEnc=lf:%.0f,rf:%.0f,rb:%.0f",
                        robot.robotDrive.driveBase.getXPosition(), robot.robotDrive.driveBase.getYPosition(),
                        robot.robotDrive.driveBase.getHeading(),
                        robot.robotDrive.leftFrontWheel.getPosition(), robot.robotDrive.rightFrontWheel.getPosition(),
                        robot.robotDrive.rightBackWheel.getPosition());
                }
                break;
        }

        if (elapsedTimer != null)
        {
            elapsedTimer.recordPeriodTime();
            robot.dashboard.displayPrintf(
                15, "Period: %.3f(%.3f/%.3f)",
                elapsedTimer.getAverageElapsedTime(), elapsedTimer.getMinElapsedTime(),
                elapsedTimer.getMaxElapsedTime());
        }
    }   //runContinuous

    //
    // Overrides TrcGameController.ButtonHandler in TeleOp.
    //

    /**
     * This method is called when a driver gamepad button event occurs.
     *
     * @param gamepad specifies the game controller object that generated the event.
     * @param button specifies the button ID that generates the event
     * @param pressed specifies true if the button is pressed, false otherwise.
     */
    @Override
    public void driverButtonEvent(TrcGameController gamepad, int button, boolean pressed)
    {
        if (shouldDoTeleOp())
        {
            boolean processed = false;
            //
            // In addition to or instead of the gamepad controls handled by FtcTeleOp, we can add to or override the
            // FtcTeleOp gamepad actions.
            //
            robot.dashboard.displayPrintf(
                7, "%s: %04x->%s", gamepad, button, pressed ? "Pressed" : "Released");
            switch (button)
            {
                case FtcGamepad.GAMEPAD_DPAD_UP:
                    break;

                case FtcGamepad.GAMEPAD_DPAD_DOWN:
                    break;

                case FtcGamepad.GAMEPAD_DPAD_LEFT:
                    break;

                case FtcGamepad.GAMEPAD_DPAD_RIGHT:
                    break;
            }
            //
            // If the control was not processed by this method, pass it back to TeleOp.
            //
            if (!processed)
            {
                super.driverButtonEvent(gamepad, button, pressed);
            }
        }
    }   //driverButtonEvent

    /**
     * This method is called when an operator gamepad button event occurs.
     *
     * @param gamepad specifies the game controller object that generated the event.
     * @param button specifies the button ID that generates the event
     * @param pressed specifies true if the button is pressed, false otherwise.
     */
    @Override
    public void operatorButtonEvent(TrcGameController gamepad, int button, boolean pressed)
    {
        if (shouldDoTeleOp())
        {
            boolean processed = false;
            //
            // In addition to or instead of the gamepad controls handled by FtcTeleOp, we can add to or override the
            // FtcTeleOp gamepad actions.
            //
            robot.dashboard.displayPrintf(
                7, "%s: %04x->%s", gamepad, button, pressed ? "Pressed" : "Released");
            switch (button)
            {
                case FtcGamepad.GAMEPAD_DPAD_UP:
                    break;

                case FtcGamepad.GAMEPAD_DPAD_DOWN:
                    break;

                case FtcGamepad.GAMEPAD_DPAD_LEFT:
                    break;

                case FtcGamepad.GAMEPAD_DPAD_RIGHT:
                    break;
            }
            //
            // If the control was not processed by this method, pass it back to TeleOp.
            //
            if (!processed)
            {
                super.operatorButtonEvent(gamepad, button, pressed);
            }
        }
    }   //operatorButtonEvent

    /**
     * This method creates and displays the test menus and record the selected choices.
     */
    private void doTestMenus()
    {
        //
        // Create menus.
        //
        testMenu = new FtcChoiceMenu<>("Tests:", null);
        FtcValueMenu xTargetMenu = new FtcValueMenu(
            "xTarget:", testMenu, -10.0, 10.0, 0.5, 0.0, " %.1f ft");
        FtcValueMenu yTargetMenu = new FtcValueMenu(
            "yTarget:", testMenu, -10.0, 10.0, 0.5, 0.0, " %.1f ft");
        FtcValueMenu turnTargetMenu = new FtcValueMenu(
            "turnTarget:", testMenu, -180.0, 180.0, 5.0, 0.0, " %.0f deg");
        FtcValueMenu driveTimeMenu = new FtcValueMenu(
            "Drive time:", testMenu, 1.0, 10.0, 1.0, 4.0, " %.0f sec");
        FtcValueMenu drivePowerMenu = new FtcValueMenu(
            "Drive power:", testMenu, -1.0, 1.0, 0.1, 0.5, " %.1f");
        //
        // PID Tuning menus.
        //
        FtcValueMenu tuneKpMenu = new FtcValueMenu(
            "Kp:", testMenu, 0.0, 1.0, 0.001, this::getTuneKp, " %f");
        FtcValueMenu tuneKiMenu = new FtcValueMenu(
            "Ki:", testMenu, 0.0, 1.0, 0.0001, this::getTuneKi, " %f");
        FtcValueMenu tuneKdMenu = new FtcValueMenu(
            "Kd:", testMenu, 0.0, 1.0, 0.0001, this::getTuneKd, " %f");
        FtcValueMenu tuneKfMenu = new FtcValueMenu(
            "Kf:", testMenu, 0.0, 1.0, 0.001, this::getTuneKf, " %f");
        //
        // Populate menus.
        //
        testMenu.addChoice("Sensors test", Test.SENSORS_TEST, true);
        testMenu.addChoice("Subsystems test", Test.SUBSYSTEMS_TEST, false);
        testMenu.addChoice("Drive speed test", Test.DRIVE_SPEED_TEST, false);
        testMenu.addChoice("Motors test", Test.DRIVE_MOTORS_TEST, false);
        testMenu.addChoice("X Timed drive", Test.X_TIMED_DRIVE, false, driveTimeMenu);
        testMenu.addChoice("Y Timed drive", Test.Y_TIMED_DRIVE, false, driveTimeMenu);
        testMenu.addChoice("PID drive", Test.PID_DRIVE, false, xTargetMenu);
        testMenu.addChoice("Tune X PID", Test.TUNE_X_PID, false, tuneKpMenu);
        testMenu.addChoice("Tune Y PID", Test.TUNE_Y_PID, false, tuneKpMenu);
        testMenu.addChoice("Tune Turn PID", Test.TUNE_TURN_PID, false, tuneKpMenu);
        testMenu.addChoice("Pure Pursuit Drive", Test.PURE_PURSUIT_DRIVE, false);

        xTargetMenu.setChildMenu(yTargetMenu);
        yTargetMenu.setChildMenu(turnTargetMenu);
        turnTargetMenu.setChildMenu(drivePowerMenu);
        driveTimeMenu.setChildMenu(drivePowerMenu);
        tuneKpMenu.setChildMenu(tuneKiMenu);
        tuneKiMenu.setChildMenu(tuneKdMenu);
        tuneKdMenu.setChildMenu(tuneKfMenu);
        //
        // Traverse menus.
        //
        FtcMenu.walkMenuTree(testMenu);
        //
        // Fetch choices.
        //
        testChoices.test = testMenu.getCurrentChoiceObject();
        testChoices.xTarget = xTargetMenu.getCurrentValue();
        testChoices.yTarget = yTargetMenu.getCurrentValue();
        testChoices.turnTarget = turnTargetMenu.getCurrentValue();
        testChoices.driveTime = driveTimeMenu.getCurrentValue();
        testChoices.drivePower = drivePowerMenu.getCurrentValue();
        testChoices.tunePidCoeff = new TrcPidController.PidCoefficients(
            tuneKpMenu.getCurrentValue(), tuneKiMenu.getCurrentValue(),
            tuneKdMenu.getCurrentValue(),tuneKfMenu.getCurrentValue());

        TrcPidController tunePidCtrl = getTunePidController(testChoices.test);
        if (tunePidCtrl != null)
        {
            //
            // Write the user input PID coefficients to a cache file so tune PID menu can read them as start value
            // next time.
            //
            pidCoeffCache.writeCachedPidCoeff(tunePidCtrl, testChoices.tunePidCoeff);
        }
        //
        // Show choices.
        //
        robot.dashboard.displayPrintf(0, "Test Choices: %s", testChoices);
    }   //doTestMenus

    /**
     * This method returns the PID controller for the tune test.
     *
     * @param test specifies the selected test.
     * @return tune PID controller.
     */
    private TrcPidController getTunePidController(Test test)
    {
        TrcPidController pidCtrl;

        switch (test)
        {
            case TUNE_X_PID:
                pidCtrl = robot.robotDrive.encoderXPidCtrl;
                break;

            case TUNE_Y_PID:
                pidCtrl = robot.robotDrive.encoderYPidCtrl;
                break;

            case TUNE_TURN_PID:
                pidCtrl = robot.robotDrive.gyroPidCtrl;
                break;

            default:
                pidCtrl = null;
        }

        return pidCtrl;
    }   //getTunePidController

    /**
     * This method is called by the tuneKpMenu to get the start value to be displayed as the current value of the menu.
     *
     * @return start Kp value of the PID controller being tuned.
     */
    private double getTuneKp()
    {
        double value = 0.0;
        TrcPidController tunePidCtrl = getTunePidController(testMenu.getCurrentChoiceObject());

        if (tunePidCtrl != null)
        {
            value = pidCoeffCache.getCachedPidCoeff(tunePidCtrl).kP;
        }

        return value;
    }   //getTuneKp

    /**
     * This method is called by the tuneKiMenu to get the start value to be displayed as the current value of the menu.
     *
     * @return start Ki value of the PID controller being tuned.
     */
    private double getTuneKi()
    {
        double value = 0.0;
        TrcPidController tunePidCtrl = getTunePidController(testMenu.getCurrentChoiceObject());

        if (tunePidCtrl != null)
        {
            value = pidCoeffCache.getCachedPidCoeff(tunePidCtrl).kI;
        }

        return value;
    }   //getTuneKi

    /**
     * This method is called by the tuneKdMenu to get the start value to be displayed as the current value of the menu.
     *
     * @return start Kd value of the PID controller being tuned.
     */
    private double getTuneKd()
    {
        double value = 0.0;
        TrcPidController tunePidCtrl = getTunePidController(testMenu.getCurrentChoiceObject());

        if (tunePidCtrl != null)
        {
            value = pidCoeffCache.getCachedPidCoeff(tunePidCtrl).kD;
        }

        return value;
    }   //getTuneKd

    /**
     * This method is called by the tuneKfMenu to get the start value to be displayed as the current value of the menu.
     *
     * @return start Kf value of the PID controller being tuned.
     */
    double getTuneKf()
    {
        double value = 0.0;
        TrcPidController tunePidCtrl = getTunePidController(testMenu.getCurrentChoiceObject());

        if (tunePidCtrl != null)
        {
            value = pidCoeffCache.getCachedPidCoeff(tunePidCtrl).kF;
        }

        return value;
    }   //getTuneKF

    /**
     * This method reads all sensors and prints out their values. This is a very useful diagnostic tool to check
     * if all sensors are working properly. For encoders, since test sensor mode is also teleop mode, you can
     * operate the gamepads to turn the motors and check the corresponding encoder counts.
     */
    private void doSensorsTest()
    {
        final int LABEL_WIDTH = 100;
        //
        // Read all sensors and display on the dashboard.
        // Drive the robot around to sample different locations of the field.
        //
        if (!RobotParams.Preferences.visionOnly)
        {
            robot.dashboard.displayPrintf(
                8, LABEL_WIDTH, "Enc: ", "lf=%.0f,rf=%.0f,lb=%.0f,rb=%.0f",
                robot.robotDrive.leftFrontWheel.getPosition(), robot.robotDrive.rightFrontWheel.getPosition(),
                robot.robotDrive.leftBackWheel.getPosition(), robot.robotDrive.rightBackWheel.getPosition());
        }

        if (robot.robotDrive.gyro != null)
        {
            robot.dashboard.displayPrintf(
                9, LABEL_WIDTH, "Gyro: ", "Rate=%.3f,Heading=%.1f",
                robot.robotDrive.gyro.getZRotationRate().value, robot.robotDrive.gyro.getZHeading().value);
        }

        if (robot.arm != null)
        {
            robot.dashboard.displayPrintf(
                10, LABEL_WIDTH, "Arm: ", "pos=%.0f,lowLimit=%s,upLimit=%s",
                robot.arm.getPosition(), robot.arm.isLowerLimitSwitchActive(), robot.arm.isUpperLimitSwitchActive());
        }
    }   //doSensorsTest

    /**
     * This method calls vision code to detect target objects and display their info.
     */
    private void doVisionTest()
    {
        if (robot.vision != null)
        {
            if (robot.vision.isVuforiaVisionInitialized())
            {
                TrcPose2D robotPose = robot.vision.getRobotPose(null, false);
                robot.dashboard.displayPrintf(11, "RobotLocation %s: %s",
                                              robot.vision.getLastSeenVuforiaImageName(), robotPose);
            }

            if (robot.vision.isTensorFlowVisionInitialized())
            {
                robot.vision.getCurrentDuckPositions();
            }
        }
    }   //doVisionTest

    /**
     * This method is called to determine if Test mode is allowed to do teleop control of the robot.
     *
     * @return true to allow and false otherwise.
     */
    private boolean shouldDoTeleOp()
    {
        return !RobotParams.Preferences.visionOnly &&
               (testChoices.test == Test.SUBSYSTEMS_TEST || testChoices.test == Test.DRIVE_SPEED_TEST);
    }   //shouldDoTeleOp

}   //class FtcTest
