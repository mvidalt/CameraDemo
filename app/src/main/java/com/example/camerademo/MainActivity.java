package com.example.camerademo;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback{

    SurfaceView surfaceView;
    Button button;

    Camera camera;
    SurfaceHolder surfaceHolder;

    private Bitmap capturedBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        surfaceView = findViewById(R.id.surfaceView);
        button = findViewById(R.id.button);

        // Solicitar permisos de cámara y escritura externa en tiempo de ejecución
        String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (!hasPermissions(permissions)) {
            ActivityCompat.requestPermissions(this, permissions, 100);
        }

        // Inicializar la SurfaceView y configurar el SurfaceHolder
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);

        button.setOnClickListener(this::onClick);
    }


    private boolean hasPermissions(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            Bitmap bitmap = (Bitmap) Objects.requireNonNull(data.getExtras()).get("data");
            // imageView.setImageBitmap(bitmap); // Elimina esta línea si no es necesaria

            saveImageToExternalStorage(bitmap);

            // Oculta la SurfaceView después de tomar la foto
            surfaceView.setVisibility(View.GONE);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Verificar si se otorgaron todos los permisos
        // Permiso otorgado, puedes continuar con la operación de la cámara
        // Permiso denegado, manejar en consecuencia (por ejemplo, mostrar un mensaje o deshabilitar la funcionalidad de la cámara)
    }

    private void saveImageToExternalStorage(Bitmap bitmap) {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File myDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);


            if (!myDir.exists()) {
                if (myDir.mkdirs()) {
                    saveImageToFile(bitmap, myDir);
                }

            } else {
                saveImageToFile(bitmap, myDir);
            }
        }

    }

    private void saveImageToFile(Bitmap bitmap, File directory) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "IMG_" + timeStamp + ".jpg";
        File file = new File(directory, fileName);

        try {
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();

            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            mediaScanIntent.setData(Uri.fromFile(file));
            sendBroadcast(mediaScanIntent);

            Toast.makeText(this,"Image Saves Succesfully" ,Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }




    private void onClick(View v) {
        // Llama a un método para capturar la imagen directamente desde la vista previa de la cámara
        captureImage();

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // Abre la cámara cuando la SurfaceView está creada
        camera = Camera.open();

        if (camera != null) {
            try {
                Camera.Parameters parameters = camera.getParameters();

                // Determina la orientación actual del dispositivo
                int rotation = getWindowManager().getDefaultDisplay().getRotation();
                int degrees = 0;

                switch (rotation) {
                    case Surface.ROTATION_0:
                        degrees = 0;
                        break;
                    case Surface.ROTATION_90:
                        degrees = 90;
                        break;
                    case Surface.ROTATION_180:
                        degrees = 180;
                        break;
                    case Surface.ROTATION_270:
                        degrees = 270;
                        break;
                }

                // Ajusta la orientación de la cámara
                Camera.CameraInfo info = new Camera.CameraInfo();
                Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, info);
                int result = (info.orientation - degrees + 360) % 360;
                camera.setDisplayOrientation(result);

                // Configura la vista previa de la cámara en la SurfaceView
                camera.setPreviewDisplay(holder);
                camera.startPreview();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // Libera la cámara cuando la SurfaceView está destruida
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }

    private void captureImage() {
        if (camera != null) {
            camera.takePicture(null, null, this::onPictureTaken);
        }
    }

    private void onPictureTaken(byte[] data, Camera camera) {
        Bitmap originalBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

        // Obtener la orientación actual del dispositivo
        int rotation = getWindowManager().getDefaultDisplay().getRotation();

        // Ajustar la orientación de la imagen capturada
        int rotationDegree = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                rotationDegree = 90;
                break;
            case Surface.ROTATION_90:
                rotationDegree = 0;
                break;
            case Surface.ROTATION_180:
                rotationDegree = 270;
                break;
            case Surface.ROTATION_270:
                rotationDegree = 180;
                break;
        }

        // Rotar la imagen antes de guardarla
        capturedBitmap = rotateBitmap(originalBitmap, rotationDegree);

        // Redimensionar el Bitmap a un tamaño más manejable
        int targetWidth = 800; // Puedes ajustar este valor según tus necesidades
        int targetHeight = (int) (capturedBitmap.getWidth() * (targetWidth / (float) capturedBitmap.getHeight()));
        capturedBitmap = Bitmap.createScaledBitmap(capturedBitmap, targetWidth, targetHeight, true);

        // Guardar la imagen en la galería o en tu lógica de almacenamiento
        saveImageToExternalStorage(capturedBitmap);

        // Actualizar el SurfaceView con la imagen capturada
        updateSurfaceView();
    }


    private void updateSurfaceView() {
        // Muestra la imagen capturada en el SurfaceView
        if (capturedBitmap != null) {
            Canvas canvas = surfaceHolder.lockCanvas();
            if (canvas != null) {
                canvas.drawBitmap(capturedBitmap, 0, 0, null);
                surfaceHolder.unlockCanvasAndPost(canvas);
            }
        }
    }

    private Bitmap rotateBitmap(Bitmap source, int rotation) {
        Matrix matrix = new Matrix();
        matrix.postRotate(rotation);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }


}
