package com.queenshack.vstickapp;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.Image;
import android.media.ImageReader;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.clarifai.api.ClarifaiClient;
import com.clarifai.api.RecognitionRequest;
import com.clarifai.api.RecognitionResult;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by Md Islam on 4/23/2016.
 */
public class MainActivity extends Activity {


    private SurfaceView surface_view;
    private Camera mCamera;
    SurfaceHolder.Callback sh_ob = null;
    SurfaceHolder surface_holder = null;
    SurfaceHolder.Callback sh_callback = null;

    private File mFile;

    private TextToSpeech tts;


    private Camera.ShutterCallback shutterCallback;
    private Camera.PictureCallback raw;
    private Camera.PictureCallback jpeg;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);


        mFile = new File(getExternalFilesDir(null), "pic.jpg");

        getWindow().setFormat(PixelFormat.TRANSLUCENT);

        surface_view = new SurfaceView(getApplicationContext());
        addContentView(surface_view, new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.FILL_PARENT));

        if (surface_holder == null) {
            surface_holder = surface_view.getHolder();
        }

        sh_callback = my_callback();
        surface_holder.addCallback(sh_callback);

        surface_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCommand();
            }
        });


    }



    Camera.ShutterCallback myShutterCallback = new Camera.ShutterCallback(){

        public void onShutter() {
            // TODO Auto-generated method stub
        }};

    Camera.PictureCallback myPictureCallback_RAW = new Camera.PictureCallback(){

        public void onPictureTaken(byte[] arg0, Camera arg1) {
            // TODO Auto-generated method stub
        }};


    Camera.PictureCallback myPictureCallback_JPG = new Camera.PictureCallback() {

        public void onPictureTaken(byte[] arg0, Camera arg1) {
            // TODO Auto-generated method stub
            Bitmap bitmapPicture = BitmapFactory.decodeByteArray(arg0, 0, arg0.length);

            Bitmap bmp = Bitmap.createBitmap(bitmapPicture, 0, 0, bitmapPicture.getWidth(), bitmapPicture.getHeight(), null, true);

            ImageSaver imageSaver = new ImageSaver(bmp, mFile);
            imageSaver.run();

        }
    };




    private void getCommand(){
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "en-US");

        try {
            startActivityForResult(intent, 1);
        } catch (ActivityNotFoundException a) {
            Toast t = Toast.makeText(getApplicationContext(),
                    "Opps! Your device doesn't support Speech to Text",
                    Toast.LENGTH_SHORT);
            t.show();
        }
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case 1: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> text = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                    String commands = text.get(0);
                    for(String command : commands.split("[\\s']")){
                        if(command.equalsIgnoreCase("what")){

                            mCamera.takePicture(myShutterCallback, myPictureCallback_RAW, myPictureCallback_JPG);
                            mCamera.stopPreview();

                            mCamera.startPreview();


                            ImageRecognition imageRecognition = new ImageRecognition();
                            imageRecognition.execute(mFile);

                        } else if(command.equalsIgnoreCase("who")){

                        }
                    }


                }
                break;
            }

        }
    }



    private class ImageRecognition extends AsyncTask<File, Void, String> {

        @Override
        protected String doInBackground(File... params) {
            ClarifaiClient clarifai = new ClarifaiClient("PMoeJh2lnczEKodRSLB6n0tjuwIZ0yu-iT8hfLMq" , "NiIca6AZ5yiNxV1VTt-6OIfv5843mpxZWuS60DzS");
            List<RecognitionResult> results = clarifai.recognize(new RecognitionRequest(params[0]));

            String result = results.get(0).getTags().get(0).getName();

            Log.d("Crash", result);

            return result;

//            for (Tag tag : results.get(0).getTags()) {
//                Log.d("Crash", tag.getName() + ": " + tag.getProbability());
//            }
//
//            return null;
        }


        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            sayResult(s);
        }



    }




    private void sayResult(final String text){

        tts = new TextToSpeech(MainActivity.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = tts.setLanguage(Locale.US);
                    if (result == TextToSpeech.LANG_MISSING_DATA ||
                            result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("error", "This Language is not supported");
                    } else {
                        ConvertTextToSpeech(text);
                    }
                }
            }});

        tts.setLanguage(Locale.US);
        tts.speak("Text to say aloud", TextToSpeech.QUEUE_ADD, null);

    }


    private void ConvertTextToSpeech(String text) {
        // TODO Auto-generated method stub
        if(text==null || "".equals(text))
        {
            text = "Content not available";
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }else
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }





    private class ImageSaver implements Runnable {

        /**
         * The JPEG image
         */
        private final Bitmap mImage;
        /**
         * The file we save the image into.
         */
        private final File mFile;

        public ImageSaver(Bitmap image, File file) {
            mImage = image;
            mFile = file;
        }

        @Override
        public void run() {

            FileOutputStream out = null;
            try {
                out = new FileOutputStream(mFile);
                mImage.compress(Bitmap.CompressFormat.JPEG, 100, out); // bmp is your Bitmap instance
                // PNG is a lossless format, the compression factor (100) is ignored
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }



//            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
//            byte[] bytes = new byte[buffer.remaining()];
//            buffer.get(bytes);
//            FileOutputStream output = null;
//            try {
//                output = new FileOutputStream(mFile);
//                output.write(bytes);
//            } catch (IOException e) {
//                e.printStackTrace();
//            } finally {
//                mImage.close();
//                if (null != output) {
//                    try {
//                        output.close();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }


        }

    }


    SurfaceHolder.Callback my_callback() {
        SurfaceHolder.Callback ob1 = new SurfaceHolder.Callback() {

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                mCamera = Camera.open();

                try {
                    mCamera.setPreviewDisplay(holder);
                } catch (IOException exception) {
                    mCamera.release();
                    mCamera = null;
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                mCamera.startPreview();
            }

        };

        return ob1;
    }


}
