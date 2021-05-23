package com.examples.myapplication;

import android.Manifest;
import android.app.Activity;
import android.app.PictureInPictureParams;
import android.content.Context;
        import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.camera2.CameraAccessException;
        import android.hardware.camera2.CameraCaptureSession;
        import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraConstrainedHighSpeedCaptureSession;
import android.hardware.camera2.CameraDevice;
        import android.hardware.camera2.CameraManager;
        import android.hardware.camera2.CameraMetadata;
        import android.hardware.camera2.CaptureRequest;
        import android.hardware.camera2.TotalCaptureResult;
        import android.hardware.camera2.params.StreamConfigurationMap;
        import android.media.Image;
        import android.media.ImageReader;
        import android.os.Build;
        import android.os.Bundle;
        import android.os.Environment;
        import android.os.Handler;
        import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
        import android.util.Size;
        import android.util.SparseIntArray;
        import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
        import android.view.View;
        import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.common.FirebaseVisionPoint;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceContour;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
        import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStream;
        import java.nio.ByteBuffer;
import java.security.Policy;
import java.util.ArrayList;
        import java.util.Arrays;
        import java.util.List;
public class AndroidCamera2API extends AppCompatActivity implements Runnable {
    private static final String TAG = "AndroidCameraApi";
    private Button takePictureButton;
    private TextureView textureView;
    private SurfaceView surfaceView;

    public AndroidCamera2API() {
    }


    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private String cameraId;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest captureRequest;
    protected CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private ImageReader imageReader;
    private File file;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private boolean mFlashSupported;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private Context ctx;
    private com.examples.myapplication.textureView img;
    private Looper looper;
    ArrayList  arrList = new ArrayList ();
    ArrayList<PointF>  contourPointList = new ArrayList<PointF>();
    private List<PointF> userPath = new ArrayList<PointF>();
    final float[] arr = new float[userPath.size()];
    int prevRotation = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_android_camera2_a_p_i);
        textureView = (TextureView) findViewById(R.id.texture);
        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);
        takePictureButton = (Button) findViewById(R.id.btn_takepicture);
        assert takePictureButton != null;
        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();
            }
        });
        surfaceView = (SurfaceView) findViewById(R.id.surface);
        surfaceView.setZOrderOnTop(true);

    }


    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        private final SparseIntArray  ORIENTATIONS_SENSOR =  new SparseIntArray();
        {
            ORIENTATIONS.append(Surface.ROTATION_0, 90);
            ORIENTATIONS.append(Surface.ROTATION_90, 0);
            ORIENTATIONS.append(Surface.ROTATION_180, 270);
            ORIENTATIONS.append(Surface.ROTATION_270, 180);
        }
        private final SparseIntArray  ORIENTATIONS_REAL = new SparseIntArray();
        {
            ORIENTATIONS_REAL.append(Surface.ROTATION_0, 0);
            ORIENTATIONS_REAL.append(Surface.ROTATION_90, 90);
            ORIENTATIONS_REAL.append(Surface.ROTATION_180, 180);
            ORIENTATIONS_REAL.append(Surface.ROTATION_270, 270);
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            width=textureView.getWidth();
            height=textureView.getHeight();
            transformImage(textureView.getWidth(),textureView.getHeight());
            //open your camera here
            openCamera(width,height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width and height
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            Size imageDimensions = null;
            Matrix transformMatrix = new Matrix();
            RectF previewRectF = null;
            RectF cameraRectF = null;
            int sensorOrientation = 0;
            if (rotation != prevRotation) {
                prevRotation = rotation;
                cameraRectF = new RectF(0, 0, imageDimension.getWidth(), imageDimension.getHeight());
                RectF textureRectF = new RectF(0, 0, imageDimension.getHeight(), imageDimension.getWidth());
                float centerX = cameraRectF.centerX();
                float centerY = cameraRectF.centerY();
                if (rotation != Surface.ROTATION_0) {
                    textureRectF.offset(centerX - textureRectF.centerX(), centerY - textureRectF.centerY());
                    transformMatrix.setRectToRect(cameraRectF, textureRectF, Matrix.ScaleToFit.FILL);
                    float scale = Math.max(
                            (float) textureRectF.right / cameraRectF.right,
                            (float) textureRectF.bottom / cameraRectF.bottom);
                    transformMatrix.postScale(scale, scale, centerX, centerY);
                    if (rotation != Surface.ROTATION_180) {
                        int angle = (ORIENTATIONS_REAL.get(rotation) + sensorOrientation +
                                ORIENTATIONS_SENSOR.get(sensorOrientation)) % 360;
                        transformMatrix.postRotate(angle, centerX, centerY);
                    } else {
                        transformMatrix.postRotate(180, centerX, centerY);
                    }
                } else {
                    transformMatrix.reset();
                }
            }
            textureView.setRotation(90);

        }
    };
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            //This is called when the camera is open
            Log.e(TAG, "onOpened");
            cameraDevice = camera;
            createCameraPreview();
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };
    final CameraCaptureSession.CaptureCallback captureCallbackListener = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            Toast.makeText(AndroidCamera2API.this, "Saved:" + file, Toast.LENGTH_SHORT).show();
            createCameraPreview();
        }
    };

    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private int getRotationCompensation(String cameraId, Activity activity, Context context)
            throws CameraAccessException {
        // Get the device's current rotation relative to its "native" orientation.
        // Then, from the ORIENTATIONS table, look up the angle the image must be
        // rotated to compensate for the device's rotation.
        int deviceRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int rotationCompensation = ORIENTATIONS.get(deviceRotation);

        // On most devices, the sensor orientation is 90 degrees, but for some
        // devices it is 270 degrees. For devices with a sensor orientation of
        // 270, rotate the image an additional 180 ((270 + 270) % 360) degrees.
        CameraManager cameraManager = (CameraManager) context.getSystemService(CAMERA_SERVICE);
        int sensorOrientation = cameraManager
                .getCameraCharacteristics(cameraId)
                .get(CameraCharacteristics.SENSOR_ORIENTATION);
        rotationCompensation = (rotationCompensation + sensorOrientation + 270) % 360;

        // Return the corresponding FirebaseVisionImageMetadata rotation value.
        int result;
        switch (rotationCompensation) {
            case 0:
                result = FirebaseVisionImageMetadata.ROTATION_0;
                break;
            case 90:
                result = FirebaseVisionImageMetadata.ROTATION_90;
                break;
            case 180:
                result = FirebaseVisionImageMetadata.ROTATION_180;
                break;
            case 270:
                result = FirebaseVisionImageMetadata.ROTATION_270;
                break;
            default:
                result = FirebaseVisionImageMetadata.ROTATION_0;
                Log.e(TAG, "Bad rotation value: " + rotationCompensation);
        }
        return result;
    }

    protected void takePicture() {
        if (null == cameraDevice) {
            Log.e(TAG, "cameraDevice is null");
            return;
        }
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;
            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            }
            final float preWidth= 1080 ;
            final float preHeight= 870;
            int width = 1080;
            int height = 870;
            if (jpegSizes != null && 0 < jpegSizes.length) {
               width = jpegSizes[4].getWidth();
               height = jpegSizes[4].getHeight();
            }
            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            // Orientation
//            final int rotation = getWindowManager().getDefaultDisplay().getRotation();
//            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));

//            final File file = new File(Environment.getExternalStorageDirectory() + "/pic.jpg");


            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    try {
                        image = reader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        final byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        final Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
//                        save(bytes);
//                        save(bytes);
//                        BitmapDrawable drawable=(BitmapDrawable) imageView.getDrawable();
//                        Bitmap bitmap=drawable.getBitmap();
//                        File filepath=Environment.getExternalStorageDirectory();
//                        File dir =new File(filepath.getAbsolutePath()+"/Demo/");
//                        dir.mkdir();
//                        File file=new File(dir,System.currentTimeMillis()+".jpg");
//try{
//    FileOutputStream outputStream = new FileOutputStream(file);
//}catch (FileNotFoundException e){
//    e.printStackTrace();
//}

                        FirebaseApp.initializeApp(AndroidCamera2API.this);
                        FirebaseVisionFaceDetectorOptions options =
                                new FirebaseVisionFaceDetectorOptions.Builder()
                                        .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                                        .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                                        .setContourMode(FirebaseVisionFaceDetectorOptions.ALL_CONTOURS)
                                        .enableTracking()
                                        .build();
                        FirebaseVisionFaceDetector detector = FirebaseVision.getInstance().getVisionFaceDetector(options);
                         final FirebaseVisionImage firebaseImage = FirebaseVisionImage.fromBitmap(bitmap);
                        Task<List<FirebaseVisionFace>> result = detector
                                .detectInImage(firebaseImage)
                                .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionFace>>() {
                                    @Override
                                    public void onSuccess(List<FirebaseVisionFace> faces) {
                                        for (FirebaseVisionFace face : faces) {

                                            Rect bounds = face.getBoundingBox();
                                            float rotY = face.getHeadEulerAngleY();  // Head is rotated to the right rotY degrees
                                            float rotZ = face.getHeadEulerAngleZ();  // Head is tilted sideways rotZ degrees

                                            List<FirebaseVisionPoint> faceContours = face.getContour(FirebaseVisionFaceContour.FACE).getPoints();

                                            // If landmark detection was enabled (mouth, ears, eyes, cheeks, and
                                            // nose available):

                                            FirebaseVisionFaceLandmark MOUTH_BOTTOM = face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_BOTTOM);
                                            FirebaseVisionFaceLandmark LEFT_CHEEK=face.getLandmark(FirebaseVisionFaceLandmark.LEFT_CHEEK);
                                            FirebaseVisionFaceLandmark leftEar = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EAR);
                                            FirebaseVisionFaceLandmark leftEye = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EYE);
                                            FirebaseVisionFaceLandmark MOUTH_LEFT = face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_LEFT);
                                            FirebaseVisionFaceLandmark NOSE_BASE = face.getLandmark(FirebaseVisionFaceLandmark.NOSE_BASE);
                                            FirebaseVisionFaceLandmark RIGHT_CHEEK=face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_CHEEK);
                                            FirebaseVisionFaceLandmark rightEar = face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EAR);
                                            FirebaseVisionFaceLandmark rightEye = face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EYE);
                                            FirebaseVisionFaceLandmark MOUTH_RIGHT=face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_RIGHT);




                                            if (leftEar != null && leftEye != null) {
                                                FirebaseVisionPoint leftEarPos = leftEar.getPosition();
                                                FirebaseVisionPoint leftEyePos = leftEye.getPosition();

                                            }

                                            // If contour detection was enabled:
                                            List<FirebaseVisionPoint> leftEyeContour=
                                                    face.getContour(FirebaseVisionFaceContour.ALL_POINTS).getPoints();
                                            for (int i = 0; i < leftEyeContour.size(); i++)
                                            {
                                                contourPointList.add(
                                                        new PointF(
                                                                (float)leftEyeContour.get(i).getX(),
                                                                (float)leftEyeContour.get(i).getY()
                                                        )
                                                );

                                            }

                                            arrList.add(leftEyeContour);

//                                            userPath.add(arrList.get(FirebaseVision),arrList.get());

                                            List<FirebaseVisionPoint> upperLipBottomContour =
                                                    face.getContour(FirebaseVisionFaceContour.UPPER_LIP_BOTTOM).getPoints();
                                            arrList.add(upperLipBottomContour);

                                            // If classification was enabled:
                                            if (face.getSmilingProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                                                float smileProb = face.getSmilingProbability();
                                                arrList.add(smileProb);
                                            }
                                            if (face.getRightEyeOpenProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                                                float rightEyeOpenProb = face.getRightEyeOpenProbability();
                                                arrList.add(rightEyeOpenProb);

                                            }
                                            // If face tracking was enabled:
                                            if (face.getTrackingId() != FirebaseVisionFace.INVALID_ID) {
                                                int id = face.getTrackingId();
                                                arrList.add(id);
                                                FirebaseVisionFaceLandmark landmark = face.getLandmark(id);
                                                if (landmark != null) {
                                                    FirebaseVisionPoint point = landmark.getPosition();
//                                                    canvas.drawCircle(point.getX(), point.getY(), 10f, myPaint);
                                                }
                                            }
                                            int w= bitmap.getWidth();
                                            int h = bitmap.getHeight();
                                            Matrix matrix = new Matrix();
                                            SurfaceHolder mHolder = surfaceView.getHolder();
                                            mHolder.setFormat(PixelFormat.TRANSPARENT);
                                            Surface surface = mHolder.getSurface();
                                            Canvas canvas = surface.lockCanvas(null);
                                            float canvasHeight = canvas.getHeight();
                                            float canvasWidth =canvas.getWidth();
                                            Paint myPaint = new Paint();
                                            myPaint.setColor(Color.YELLOW);
                                            myPaint.setStrokeWidth(4);
                                            myPaint.setStyle(Paint.Style.STROKE);
                                            Paint myPaint1 = new Paint();
                                            myPaint1.setColor(Color.RED);
                                            myPaint1.setStrokeWidth(3);
                                            myPaint1.setStyle(Paint.Style.STROKE);
                                            Paint myPaint2 = new Paint();
                                            myPaint2.setColor(Color.BLUE);
                                            myPaint2.setStrokeWidth(6);
                                                  myPaint2.setStyle(Paint.Style.STROKE);
//                                            canvas.rotate(-90, canvasWidth /2, canvasHeight /2);
                                            Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                                            Canvas canvas1 =new Canvas(bitmap);
                                            canvas1.setBitmap(bitmap);
                                            float hei = textureView.getHeight();
                                            float widt = textureView.getWidth();
                                           float widthRatio= preWidth/widt;
                                            float heightRatio= preHeight/hei;
                                            for (int i = 0; i <contourPointList.size() ; i++) {
                                                canvas1.drawCircle(contourPointList.get(i).x,contourPointList.get(i).y,3f,myPaint );
                                            }

//                                                canvas1.drawLine(contourPointList.get(i).x,contourPointList.get(i).y,contourPointList.get(i+1).x,contourPointList.get(i+1).y,myPaint1 );
////                                            canvas1.drawLine(leftEye.getPosition().getX(),leftEye.getPosition().getY(),rightEye.getPosition().getX(),rightEye.getPosition().getY(),myPaint);
//                                            canvas1.drawLine(leftEye.getPosition().getX(),leftEye.getPosition().getY(),NOSE_BASE.getPosition().getX(),NOSE_BASE.getPosition().getY(),myPaint);
//                                            canvas1.drawLine(NOSE_BASE.getPosition().getX(),NOSE_BASE.getPosition().getY(),rightEye.getPosition().getX(),rightEye.getPosition().getY(),myPaint);
////                                            canvas1.drawLine(NOSE_BASE.getPosition().getX(),NOSE_BASE.getPosition().getY(),MOUTH_BOTTOM.getPosition().getX(),MOUTH_BOTTOM.getPosition().getY(),myPaint);
////                                            canvas1.drawLine(leftEye.getPosition().getX(),leftEye.getPosition().getY(),LEFT_CHEEK.getPosition().getX(),LEFT_CHEEK.getPosition().getY(),myPaint);
////                                            canvas1.drawLine(rightEye.getPosition().getX(),rightEye.getPosition().getY(),RIGHT_CHEEK.getPosition().getX(),RIGHT_CHEEK.getPosition().getY(),myPaint);
////                                            canvas1.drawLine(LEFT_CHEEK.getPosition().getX(),LEFT_CHEEK.getPosition().getY(),MOUTH_LEFT.getPosition().getX(),MOUTH_LEFT.getPosition().getY(),myPaint);
//                                            canvas1.drawLine(MOUTH_LEFT.getPosition().getX(),MOUTH_LEFT.getPosition().getY(),MOUTH_BOTTOM.getPosition().getX(),MOUTH_BOTTOM.getPosition().getY(),myPaint);
////                                            canvas1.drawLine(RIGHT_CHEEK.getPosition().getX(),RIGHT_CHEEK.getPosition().getY(),MOUTH_RIGHT.getPosition().getX(),MOUTH_RIGHT.getPosition().getY(),myPaint);
//                                            canvas1.drawLine(MOUTH_RIGHT.getPosition().getX(),MOUTH_RIGHT.getPosition().getY(),MOUTH_BOTTOM.getPosition().getX(),MOUTH_BOTTOM.getPosition().getY(),myPaint);
//
//                                            canvas1.drawCircle((MOUTH_BOTTOM.getPosition().getX()),(MOUTH_BOTTOM.getPosition().getY()),10f,myPaint2);
//                                            canvas1.drawCircle((LEFT_CHEEK.getPosition().getX()),(LEFT_CHEEK.getPosition().getY()),10f,myPaint2);
////                                            canvas.drawCircle(leftEar.getPosition().getX(),leftEar.getPosition().getY(),10f,myPaint1);
//                                            canvas1.drawCircle((leftEye.getPosition().getX()),(leftEye.getPosition().getY()),10f,myPaint2);
//                                            canvas1.drawCircle((MOUTH_LEFT.getPosition().getX()),(MOUTH_LEFT.getPosition().getY()),10f,myPaint2);
//                                            canvas1.drawCircle((NOSE_BASE.getPosition().getX()),(NOSE_BASE.getPosition().getY()),10f,myPaint2);
//                                            canvas1.drawCircle((RIGHT_CHEEK.getPosition().getX()),(RIGHT_CHEEK.getPosition().getY()),10f,myPaint2);
////                                            canvas.drawCircle(rightEar.getPosition().getX(),rightEar.getPosition().getY(),10f,myPaint1);
//                                            canvas1.drawCircle((rightEye.getPosition().getX()),(rightEye.getPosition().getY()),10f,myPaint2);
//                                            canvas1.drawCircle((MOUTH_RIGHT.getPosition().getX()),(MOUTH_RIGHT.getPosition().getY()),10f,myPaint2);
                                            canvas.drawBitmap(bitmap,matrix , myPaint2);
                                            surface.unlockCanvasAndPost(canvas);
                                            int heig = textureView.getHeight();
                                            int widt1 = textureView.getWidth();
                                            int hi= surfaceView.getHeight();
                                            int wid=surfaceView.getWidth();


//                                            userPath.add(
//                                                    new PointF(leftEar.getPosition().getX(),
//                                                            leftEar.getPosition().getY())
//                                            );
                                            userPath.add(
                                                    new PointF(leftEye.getPosition().getX(),
                                                            leftEye.getPosition().getY())
                                            );

                                            //draw on surface view canvas


                                            mHolder.addCallback(new SurfaceHolder.Callback() {
                                                @Override
                                                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

                                                }
                                                @Override
                                                public void surfaceCreated(SurfaceHolder holder) {
                                                }
                                                @Override
                                                public void surfaceDestroyed(SurfaceHolder holder) {

                                                }
                                            });
                                        }
                                    }

                                    private void withIndex() {
                                    }

                                    private void rotate(int i, float x, float y) {
                                        return;
                                    }

                                    private float translateY(Float y) {
                                        return y;
                                    }

                                    private float translateX(Float x) {
                                        return x;
                                    }

                                    private float rotate (float degrees){
                                        return degrees;
                                    }

                                    protected void onDraw(Canvas canvas) {
                                        canvas.drawColor(Color.BLACK);
                                        canvas.drawPoints(arr, 10, 10, new Paint());
                                    }

                                });



                        if (bitmap == null)
                            Log.e(TAG, "bitmap is null");

                    } finally {
                        if (image != null) {
                            image.close();
                        }
                    }
                }



                public void drawPoints(Canvas canvas){

                }

                private void save(byte[] bytes) throws IOException {
                    OutputStream output = null;
                    try {
                        output = new FileOutputStream(file);
                        output.write(bytes);
                    } finally {
                        if (null != output) {
                            output.close();
                        }
                    }
                }
            };


                reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Toast.makeText(AndroidCamera2API.this, "Saved:" + file, Toast.LENGTH_SHORT).show();
                    createCameraPreview();
                }
            };
            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                private void transformImage(int width, int height)
                {


                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    protected void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
//            texture.setDefaultBufferSize(textureView.getWidth(), textureView.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(AndroidCamera2API.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCamera(int width,int height) {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        setUpCameraOutputs(width, height);
        configureTransform(width, height);
        Log.e(TAG, "is camera open");   width=textureView.getWidth();
        height=textureView.getHeight();
        try {
            cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(AndroidCamera2API.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "openCamera X");
    }

    private void configureTransform(int width, int height) {
    }

    private void setUpCameraOutputs(int width, int height) {
    }

    protected void updatePreview() {
        if (null == cameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                Toast.makeText(AndroidCamera2API.this, "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
        startBackgroundThread();
        if (textureView.isAvailable()) {
            transformImage(textureView.getWidth(),textureView.getHeight());
            openCamera(textureView.getWidth(),textureView.getHeight());
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }

    private void transformImage(int width,int height) {

        if (textureView == null) {

            return;
        } else try {
            {
                Matrix matrix = new Matrix();
                int rotation = getWindowManager().getDefaultDisplay().getRotation();
                RectF textureRectF = new RectF(0, 0, width, height);
                RectF previewRectF = new RectF(0, 0, textureView.getHeight(), textureView.getWidth());
                float centerX = textureRectF.centerX();
                float centerY = textureRectF.centerY();
                if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
                    previewRectF.offset(centerX - previewRectF.centerX(), centerY - previewRectF.centerY());
                    matrix.setRectToRect(textureRectF, previewRectF, Matrix.ScaleToFit.FILL);
                    float scale = Math.max((float) width / width, (float) height / width);
                    matrix.postScale(scale, scale, centerX, centerY);
                    matrix.postRotate(90 * (rotation - 2), centerX, centerY);
                }
                textureView.setTransform(matrix);            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        Log.e(TAG, "onPause");
        //closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    public void FaceDetector(Context ctx, textureView img, Looper looper) {
        this.ctx = ctx;
        this.img = img;
        this.looper = looper;
    }

    @Override
    public void run() {

    }
}