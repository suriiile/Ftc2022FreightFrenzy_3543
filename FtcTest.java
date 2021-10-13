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

package team3543;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import TrcCommonLib.command.CmdDriveMotorsTest;
import TrcCommonLib.command.CmdPidDrive;
import TrcCommonLib.command.CmdPurePursuitDrive;
import TrcCommonLib.command.CmdTimedDrive;

import TrcCommonLib.trclib.TrcElapsedTimer;
import TrcCommonLib.trclib.TrcGameController;
import TrcCommonLib.trclib.TrcPose2D;
import TrcCommonLib.trclib.TrcRobot;
import TrcFtcLib.ftclib.FtcChoiceMenu;
import TrcFtcLib.ftclib.FtcDcMotor;
import TrcFtcLib.ftclib.FtcGamepad;
import TrcFtcLib.ftclib.FtcMenu;
import TrcFtcLib.ftclib.FtcValueMenu;

/**
 * This class contains the Test Mode program.
 */
@TeleOp(name="FtcTest", group="Ftc3543")
public class FtcTest extends FtcTeleOp
{
    private static final String moduleName = "FtcTest";
    private static final boolean debugXPid = true;
    private static final boolean debugYPid = true;
    private static final boolean debugTurnPid = true;

    private enum Test
    {
        SENSORS_TEST,
        SUBSYSTEMS_TEST,
        DRIVE_MOTORS_TEST,
        X_TIMED_DRIVE,
        Y_TIMED_DRIVE,
        PID_DRIVE,
        TUNE_X_PID,
        TUNE_Y_PID,
        TUNE_TURN_PID,
        PURE_PURSUIT_DRIVE
    }   //enum Test

    private TrcElapsedTimer elapsedTimer = null;
    //
    // Made the following menus static so their values will persist across different runs of PID tuning.
    //
    private static FtcValueMenu tuneKpMenu = null;
    private static FtcValueMenu tuneKiMenu = null;
    private static FtcValueMenu tuneKdMenu = null;
    private static FtcValueMenu tuneKfMenu = null;
    //
    // Menu choices.
    //
    private Test test = Test.SENSORS_TEST;
    private double xTarget = 0.0;
    private double yTarget = 0.0;
    private double turnTarget = 0.0;
    private double driveTime = 0.0;
    private double drivePower = 0.0;

    private TrcRobot.RobotCommand testCommand = null;

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
        if (Robot.Preferences.useLoopPerformanceMonitor)
        {
            elapsedTimer = new TrcElapsedTimer("TestLoopMonitor", 2.0);
        }
        //
        // Test menus.
        //
        doTestMenus();

        switch (test)
        {
            case DRIVE_MOTORS_TEST:
                if (!Robot.Preferences.visionOnly)
                {
                    testCommand = new CmdDriveMotorsTest(
                        new FtcDcMotor[] {robot.leftFrontWheel, robot.rightFrontWheel,
                                          robot.leftBackWheel, robot.rightBackWheel},
                        5.0, 0.5);
                }
                break;

            case X_TIMED_DRIVE:
                if (!Robot.Preferences.visionOnly)
                {
                    testCommand = new CmdTimedDrive(
                        robot.driveBase, 0.0, driveTime, drivePower, 0.0, 0.0);
                }
                break;

            case Y_TIMED_DRIVE:
                if (!Robot.Preferences.visionOnly)
                {
                    testCommand = new CmdTimedDrive(
                        robot.driveBase, 0.0, driveTime, 0.0, drivePower, 0.0);
                }
                break;

            case PID_DRIVE:
                if (!Robot.Preferences.visionOnly)
                {
                    testCommand = new CmdPidDrive(
                        robot.driveBase, robot.pidDrive, 0.0, drivePower, null,
                        new TrcPose2D(xTarget*12.0, yTarget*12.0, turnTarget));
                }
                break;

            case TUNE_X_PID:
                if (!Robot.Preferences.visionOnly)
                {
                    testCommand = new CmdPidDrive(
                        robot.driveBase, robot.pidDrive, 0.0, drivePower, robot.tunePidCoeff,
                        new TrcPose2D(xTarget*12.0, 0.0, 0.0));
                }
                break;

            case TUNE_Y_PID:
                if (!Robot.Preferences.visionOnly)
                {
                    testCommand = new CmdPidDrive(
                        robot.driveBase, robot.pidDrive, 0.0, drivePower, robot.tunePidCoeff,
                        new TrcPose2D(0.0, yTarget*12.0, 0.0));
                }
                break;

            case TUNE_TURN_PID:
                if (!Robot.Preferences.visionOnly)
                {
                    testCommand = new CmdPidDrive(
                        robot.driveBase, robot.pidDrive, 0.0, drivePower, robot.tunePidCoeff,
                        new TrcPose2D(0.0, 0.0, turnTarget));
                }
                break;

            case PURE_PURSUIT_DRIVE:
                if (!Robot.Preferences.visionOnly)
                {
                    testCommand = new CmdPurePursuitDrive(
                        robot.driveBase, robot.xPosPidCoeff, robot.yPosPidCoeff, robot.turnPidCoeff, robot.velPidCoeff);
                }
                break;
        }
        //
        // Only SENSORS_TEST and SUBSYSTEMS_TEST need TensorFlow, shut it down for all other tests.
        //
        if (robot.tensorFlowVision != null && test != Test.SENSORS_TEST && test != Test.SUBSYSTEMS_TEST)
        {
            robot.globalTracer.traceInfo("TestInit", "Shutting down TensorFlow.");
            robot.tensorFlowVision.shutdown();
            robot.tensorFlowVision = null;
        }
    }   //initRobot

    //
    // Overrides TrcRobot.RobotMode methods.
    //

    @Override
    public void startMode(TrcRobot.RunMode prevMode, TrcRobot.RunMode nextMode)
    {
        super.startMode(prevMode, nextMode);

        if (test == Test.SENSORS_TEST || test == Test.SUBSYSTEMS_TEST)
        {
            robot.setFlashLightOn(true);
        }
        else if (test == Test.PURE_PURSUIT_DRIVE)
        {
            //
            // Doing an infinity.
            //
            // Set the current position as the absolute field origin so the path can be an absolute path.
            robot.driveBase.setFieldPosition(new TrcPose2D(0.0, 0.0, 0.0));
            ((CmdPurePursuitDrive)testCommand).start(
                robot.driveBase.getFieldPosition(), false,
                new TrcPose2D(-24.0, 0, 45.0),
                new TrcPose2D(-24.0, 48.0, 135.0),
                new TrcPose2D(24.0, 48.0, 225.0),
                new TrcPose2D(0.0, 46.0, 270.0),
                new TrcPose2D(0.0, 0.0, 0.0),
                new TrcPose2D(-23.0, 47.0, 225.0),
                new TrcPose2D(0.0, 0.0, 0.0));
        }
    }   //startMode

    @Override
    public void stopMode(TrcRobot.RunMode prevMode, TrcRobot.RunMode nextMode)
    {
        if (testCommand != null)
        {
            testCommand.cancel();
        }

        if (test == Test.SENSORS_TEST || test == Test.SUBSYSTEMS_TEST)
        {
            robot.setFlashLightOn(false);
        }

        super.stopMode(prevMode, nextMode);
    }   //stopMode

    @Override
    public void runPeriodic(double elapsedTime)
    {
        if (shouldDoTeleOp())
        {
            super.runPeriodic(elapsedTime);
        }

        switch (test)
        {
            case SENSORS_TEST:
            case SUBSYSTEMS_TEST:
                //
                // Allow TeleOp to run so we can control the robot in sensors test mode.
                //
                doSensorsTest();
                doVisionTest();
                break;
        }
    }   //runPeriodic

    @Override
    public void runContinuous(double elapsedTime)
    {
        if (testCommand != null)
        {
            testCommand.cmdPeriodic(elapsedTime);

            if (robot.pidDrive.isActive() && (debugXPid || debugYPid || debugTurnPid))
            {
                if (robot.battery != null)
                {
                    robot.globalTracer.traceInfo("Battery", "Voltage=%5.2fV (%5.2fV)",
                                                 robot.battery.getVoltage(), robot.battery.getLowestVoltage());
                }

                robot.globalTracer.traceInfo(moduleName, "RobotPose: %s", robot.driveBase.getFieldPosition());

                if (debugXPid && robot.encoderXPidCtrl != null)
                {
                    robot.encoderXPidCtrl.printPidInfo(robot.globalTracer);
                }

                if (debugYPid && robot.encoderYPidCtrl != null)
                {
                    robot.encoderYPidCtrl.printPidInfo(robot.globalTracer);
                }

                if (debugTurnPid && robot.gyroPidCtrl != null)
                {
                    robot.gyroPidCtrl.printPidInfo(robot.globalTracer);
                }
            }
        }

        switch (test)
        {
            case X_TIMED_DRIVE:
            case Y_TIMED_DRIVE:
                if (!Robot.Preferences.visionOnly)
                {
                    robot.dashboard.displayPrintf(9, "Timed Drive: %.0f sec", driveTime);
                    robot.dashboard.displayPrintf(
                        10, "xPos=%.1f,yPos=%.1f,heading=%.1f",
                        robot.driveBase.getXPosition(), robot.driveBase.getYPosition(), robot.driveBase.getHeading());
                    robot.dashboard.displayPrintf(
                        11, "raw=lf:%.0f,rf:%.0f,lb:%.0f,rb:%.0f",
                        robot.leftFrontWheel.getPosition(), robot.rightFrontWheel.getPosition(),
                        robot.leftBackWheel.getPosition(), robot.rightBackWheel.getPosition());
                }
                break;

            case PID_DRIVE:
            case TUNE_X_PID:
            case TUNE_Y_PID:
            case TUNE_TURN_PID:
                if (!Robot.Preferences.visionOnly)
                {
                    robot.dashboard.displayPrintf(
                        9, "xPos=%.1f,yPos=%.1f,heading=%.1f,raw=lf:%.0f,rf:%.0f,lb:%.0f,rb:%.0f",
                        robot.driveBase.getXPosition(), robot.driveBase.getYPosition(), robot.driveBase.getHeading(),
                        robot.leftFrontWheel.getPosition(), robot.rightFrontWheel.getPosition(),
                        robot.leftBackWheel.getPosition(), robot.rightBackWheel.getPosition());
                    if (robot.encoderXPidCtrl != null)
                    {
                        robot.encoderXPidCtrl.displayPidInfo(10);
                    }
                    robot.encoderYPidCtrl.displayPidInfo(12);
                    robot.gyroPidCtrl.displayPidInfo(14);
                }
                break;

            case PURE_PURSUIT_DRIVE:
                if (!Robot.Preferences.visionOnly)
                {
                    robot.dashboard.displayPrintf(9, "Pure Pursuit Drive: %.0f sec", driveTime);
                    robot.dashboard.displayPrintf(
                        10, "xPos=%.1f,yPos=%.1f,heading=%.1f",
                        robot.driveBase.getXPosition(), robot.driveBase.getYPosition(), robot.driveBase.getHeading());
                }
                break;
        }

        if (elapsedTimer != null)
        {
            elapsedTimer.recordPeriodTime();
            robot.dashboard.displayPrintf(
                6, "Period: %.3f(%.3f/%.3f)",
                elapsedTimer.getAverageElapsedTime(), elapsedTimer.getMinElapsedTime(),
                elapsedTimer.getMaxElapsedTime());
        }
    }   //runContinuous

    //
    // Overrides TrcGameController.ButtonHandler in TeleOp.
    //

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
                7, "%s: %04x->%s", gamepad.toString(), button, pressed ? "Pressed" : "Released");
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
                7, "%s: %04x->%s", gamepad.toString(), button, pressed ? "Pressed" : "Released");
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

    private void doTestMenus()
    {
        //
        // Create menus.
        //
        FtcChoiceMenu<Test> testMenu = new FtcChoiceMenu<>("Tests:", null);
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

        if (tuneKpMenu == null)
        {
            tuneKpMenu = new FtcValueMenu(
                "Kp:", testMenu, 0.0, 1.0, 0.001, robot.tunePidCoeff.kP, " %f");
        }

        if (tuneKiMenu == null)
        {
            tuneKiMenu = new FtcValueMenu(
                "Ki:", testMenu, 0.0, 1.0, 0.0001, robot.tunePidCoeff.kI, " %f");
        }

        if (tuneKdMenu == null)
        {
            tuneKdMenu = new FtcValueMenu(
                "Kd:", testMenu, 0.0, 1.0, 0.0001, robot.tunePidCoeff.kD, " %f");
        }

        if (tuneKfMenu == null)
        {
            tuneKfMenu = new FtcValueMenu(
                "Kf:", testMenu, 0.0, 1.0, 0.001, robot.tunePidCoeff.kF, " %f");
        }

        //
        // Populate menus.
        //
        testMenu.addChoice("Sensors test", Test.SENSORS_TEST, true);
        testMenu.addChoice("Subsystems test", Test.SUBSYSTEMS_TEST, false);
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
        test = testMenu.getCurrentChoiceObject();
        xTarget = xTargetMenu.getCurrentValue();
        yTarget = yTargetMenu.getCurrentValue();
        turnTarget = turnTargetMenu.getCurrentValue();
        driveTime = driveTimeMenu.getCurrentValue();
        drivePower = drivePowerMenu.getCurrentValue();
        //
        // Show choices.
        //
        robot.dashboard.displayPrintf(0, "Test: %s", testMenu.getCurrentChoiceText());
    }   //doTestMenus

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
        if (!Robot.Preferences.visionOnly)
        {
            robot.dashboard.displayPrintf(
                9, LABEL_WIDTH, "Enc: ", "lf=%.0f,rf=%.0f,lb=%.0f,rb=%.0f",
                robot.leftFrontWheel.getPosition(), robot.rightFrontWheel.getPosition(),
                robot.leftBackWheel.getPosition(), robot.rightBackWheel.getPosition());
        }

        if (robot.gyro != null)
        {
            robot.dashboard.displayPrintf(
                10, LABEL_WIDTH, "Gyro: ", "Rate=%.3f,Heading=%.1f",
                robot.gyro.getZRotationRate().value, robot.gyro.getZHeading().value);
        }
    }   //doSensorsTest

    private void doVisionTest()
    {
        if (robot.vuforiaVision != null)
        {
            TrcPose2D robotPose = robot.vuforiaVision.getRobotPose(null, false);
            robot.dashboard.displayPrintf(12, "RobotLocation %s: %s",
                                          robot.vuforiaVision.getLastSeenImageName(), robotPose);
        }

        if (robot.tensorFlowVision != null)
        {
            TensorFlowVision.TargetInfo[] targetInfo = robot.tensorFlowVision.getDetectedTargetsInfo(null);
            if (targetInfo != null && targetInfo.length > 0)
            {
                for (int i = 0; i < targetInfo.length; i++)
                {
                    robot.dashboard.displayPrintf(
                        12 + i, "%s", targetInfo[i]);
                }
            }
        }
    }   //doVisionTest

    private boolean shouldDoTeleOp()
    {
        return !Robot.Preferences.visionOnly && test == Test.SUBSYSTEMS_TEST;
    }   //shouldDoTeleOp

}   //class FtcTest
