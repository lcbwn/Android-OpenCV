package com.example.lukasz.opencvtest;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.support.annotation.IntRange;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.PortraitCameraView;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.security.Policy;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2
{

    // Used to load the 'native-lib' library on application startup.
    static
    {
        System.loadLibrary("native-lib");
    }

    private static final String TAG = "MainActivity";
    //camera manager to light
    private boolean hasFlash;

    //OpenCV load
    static
    {
        if (!OpenCVLoader.initDebug())
        {
            Log.d(TAG, "OpenCV not loaded");
        } else
        {
            Log.d(TAG, "OpenCV loaded");
        }
    }

    //OpenCV camera
    JavaCameraView javaCameraView;

    //Matrixes
    Mat imgRGBA, imageGray, imageCanny, imageinRange, imageTMP;

    //Interface
    LinearLayout linearLayout;
    Button b1, b2, b3;
    TextView tv1, tvSeekBar1, tvSeekBar2;
    SeekBar seekBar1, seekBar2;

    //variables
    int counter = 0;
    int tresh1 = 50;
    int tresh2 = 150;
    int erosion_size = 5;
    int dilatation_size = 5;
    boolean isLight = false;
    boolean isb2 = false;

    List<MatOfPoint> contours = new ArrayList<>();
    Mat hierarchy = new Mat();
    List<RotatedRect> minRect = new ArrayList<>(contours.size());
    List<RotatedRect> minEllipse = new ArrayList<>(contours.size());

    BaseLoaderCallback loaderCallback = new BaseLoaderCallback(this)
    {
        @Override
        public void onManagerConnected(int status)
        {
            switch (status)
            {
                case BaseLoaderCallback.SUCCESS:
                {
                    javaCameraView.enableView();
                    break;
                }
                default:
                {
                    super.onManagerConnected(status);
                    break;
                }
            }
            super.onManagerConnected(status);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        javaCameraView = (JavaCameraView) findViewById(R.id.cameraView);
        javaCameraView.setVisibility(View.VISIBLE);
        javaCameraView.setCvCameraViewListener(this);

        //initializing interface elements
        b1 = (Button) findViewById(R.id.button);
        b2 = (Button) findViewById(R.id.button2);
        b3 = (Button) findViewById(R.id.button3);
        tv1 = (TextView) findViewById(R.id.textView);
        tvSeekBar1 = (TextView) findViewById(R.id.textView2);
        tvSeekBar2 = (TextView) findViewById(R.id.textView3);
        seekBar1 = (SeekBar) findViewById(R.id.seekBar);
        seekBar2 = (SeekBar) findViewById(R.id.seekBar2);
        linearLayout = (LinearLayout) findViewById(R.id.linearLayout);


        b2.setVisibility(View.INVISIBLE);
        tv1.setVisibility(View.INVISIBLE);
        linearLayout.setVisibility(View.INVISIBLE);


        hasFlash = getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        if (!hasFlash)
        {
            // device doesn't support flash
            // Show alert message and close the application
            AlertDialog alert = new AlertDialog.Builder(MainActivity.this)
                    .create();
            alert.setTitle("Error");
            alert.setMessage("Sorry, your device doesn't support flash light!");
            alert.setButton("OK", new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int which)
                {
                    // closing the application
                    finish();
                }
            });
            alert.show();
            return;
        }

        seekBar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
                                            {
                                                @Override
                                                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
                                                {
                                                    tresh1 = progress;
                                                    tvSeekBar1.setText("Treshhold 1: " + String.valueOf(tresh1));
                                                }

                                                @Override
                                                public void onStartTrackingTouch(SeekBar seekBar)
                                                {

                                                }

                                                @Override
                                                public void onStopTrackingTouch(SeekBar seekBar)
                                                {

                                                }
                                            }
        );
        seekBar2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
                                            {
                                                @Override
                                                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
                                                {
                                                    tresh2 = progress;
                                                    tvSeekBar2.setText("Treshhold 2: " + String.valueOf(tresh2));

                                                }

                                                @Override
                                                public void onStartTrackingTouch(SeekBar seekBar)
                                                {

                                                }

                                                @Override
                                                public void onStopTrackingTouch(SeekBar seekBar)
                                                {

                                                }
                                            }
        );
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    protected void onPause()
    {
        super.onPause();
        if (javaCameraView != null)
        {
            javaCameraView.disableView();
        }
        if (isLight)
        {
            turnOffFlashLight();
        }
        imgRGBA.release();
        imageGray.release();
        imageCanny.release();
        imageinRange.release();
        imageTMP.release();
    }

    protected void onDestroy()
    {
        super.onDestroy();
        if (javaCameraView != null)
        {
            javaCameraView.disableView();
        }
        if (isLight)
        {
            turnOffFlashLight();
        }
        imgRGBA.release();
        imageGray.release();
        imageCanny.release();
        imageinRange.release();
        imageTMP.release();
    }

    protected void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug())
        {
            Log.d(TAG, "OpenCV not loaded");
            loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        } else
        {
            Log.d(TAG, "OpenCV loaded");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, loaderCallback);
            tv1.setText("OpenCV OK!");
        }
        if (isLight)
        {
            turnOnFlashLight();
        }
    }

    public void onCameraViewStarted(int width, int height)
    {
        imgRGBA = new Mat(height, width, CvType.CV_8UC4);
        imageGray = new Mat(imgRGBA.size(), CvType.CV_8UC1);
        imageCanny = new Mat(imgRGBA.size(), CvType.CV_8UC1);
        imageinRange = new Mat(imgRGBA.size(),CvType.CV_8UC1);
        imageTMP = new Mat(imgRGBA.size(),CvType.CV_8UC1);
    }

    @Override
    public void onCameraViewStopped()
    {
        imgRGBA.release();
        imageGray.release();
        imageCanny.release();
        imageinRange.release();
        imageTMP.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame)
    {
        imgRGBA = inputFrame.rgba();
        Imgproc.cvtColor(imgRGBA, imageGray, Imgproc.COLOR_RGBA2GRAY);
        Imgproc.Canny(imageGray, imageCanny, tresh1,tresh2);
        Core.inRange(imageGray, new Scalar(tresh1, tresh1, tresh1), new Scalar(tresh2, tresh2, tresh2),imageinRange);
        Mat element1 = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2*erosion_size + 1, 2*erosion_size+1));
        Imgproc.erode(imageinRange,imageTMP,element1);
        Mat element2 = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2*dilatation_size + 1, 2*dilatation_size+1));
        Imgproc.erode(imageTMP,imageTMP,element2);
        Imgproc.findContours(imageTMP,contours,hierarchy,Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        //for (int i = 0; i < contours.size(); i++)
        //{
       //     if (Imgproc.contourArea(contours(> 100))
       //     minRect.set(i, Imgproc.minAreaRect((contours.get(i))));
      //  }
        switch (counter)
        {
            case 0:
                return imgRGBA;
            case 1:
                return imageGray;
            case 2:
                return imageCanny;
            case 3:
                return imageinRange;
            case 4:
                return imageTMP;
            default:
                return imgRGBA;
        }
    }


    public void onClick(View view)
    {
        switch (counter)
        {
            case 0:
                counter = 1;
                b1.setText("Change to Canny");
                b2.setVisibility(View.INVISIBLE);
                linearLayout.setVisibility(View.INVISIBLE);
                break;
            case 1:
                counter = 2;
                b1.setText("Change to inRange");
                b2.setVisibility(View.VISIBLE);
                b2.setText("Show tresholds");
                isb2 = false;
                linearLayout.setVisibility(View.INVISIBLE);
                break;
            case 2:
                counter = 3;
                b1.setText("Change to opened");
                b2.setVisibility(View.VISIBLE);
                b2.setText("Show scalars");
                isb2 = false;
                linearLayout.setVisibility(View.INVISIBLE);
                break;
            case 3:
                counter = 4;
                b2.setVisibility(View.INVISIBLE);
                linearLayout.setVisibility(View.INVISIBLE);
                b1.setText("Change RGB");
                break;
            case 4:
                counter = 0;
                b1.setText("Change to gray");
                b2.setVisibility(View.INVISIBLE);
                linearLayout.setVisibility(View.INVISIBLE);
                break;
        }
    }

    public void onClick2(View view)
    {
        switch (counter)
        {
            case 2:

                if (isb2 == false)
                {
                    b2.setText("Hide tresholds");
                    linearLayout.setVisibility(View.VISIBLE);
                    isb2 = true;
                } else
                {
                    b2.setText("Show tresholds");
                    linearLayout.setVisibility(View.INVISIBLE);
                    isb2 = false;

                }
                break;
            case 3:
                if (isb2 == false)
                {
                    b2.setText("Hide scalars");
                    linearLayout.setVisibility(View.VISIBLE);
                    isb2 = true;
                } else
                {
                    b2.setText("Show scalars");
                    linearLayout.setVisibility(View.INVISIBLE);
                    isb2 = false;

                }

        }
    }

    public void OnLight(View view)
    {
        if (isLight == false)
        {
            turnOnFlashLight();
        } else turnOffFlashLight();
    }

    public void turnOnFlashLight()
    {
        javaCameraView.turnOnTheFlash();
        b3.setBackgroundResource(R.drawable.light_on);
        isLight = true;
    }


    public void turnOffFlashLight()
    {
        javaCameraView.turnOffTheFlash();
        b3.setBackgroundResource(R.drawable.light_off);
        isLight = false;
    }

}
