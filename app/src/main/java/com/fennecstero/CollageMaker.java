package com.fennecstero;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.appcompat.app.AppCompatActivity;

import java.io.OutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class CollageMaker {

    private Context context;

    public CollageMaker(Context context) {
        this.context = context;
    }

    public void createAndSaveCollage(Uri imageUri1, Uri imageUri2, String outputFileName, boolean borderCheck, boolean isHor) throws IOException {
        // Load images from URIs
        List<Bitmap> images = Arrays.asList(
                MediaStore.Images.Media.getBitmap(context.getContentResolver(), imageUri1),
                MediaStore.Images.Media.getBitmap(context.getContentResolver(), imageUri2)
        );

        // Dimensions for a 4:5 aspect ratio
        int width = 1200;  // Width of the collage (4:5 aspect ratio)
        int height = 1500;  // Height of the collage (4:5 aspect ratio)


        // Create a new Bitmap for the collage with the specified dimensions
        Bitmap collage = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(collage);

        // Fill the background with white color
        canvas.drawColor(Color.WHITE);

        // Calculate the size for each image to fit within the collage dimensions
        int rows = isHor ? 2 : 1;  // 2 rows if horizontal, else 1 row
        int cols = isHor ? 1 : images.size();  // 1 column if horizontal, else number of images

        int cellWidth = width / cols;
        int cellHeight = height / rows;

        Paint borderPaint = new Paint();
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStyle(Paint.Style.STROKE);
        if (borderCheck) {
            borderPaint.setStrokeWidth(0);
        }

        // Place each image within the grid cells
        for (int i = 0; i < images.size(); i++) {
            Bitmap image = images.get(i);

            if (isHor) {
                // Rotate images if horizontal flag is true
                image = rotateBitmap(image, 90);
            }

            // Scale and crop the image to fit within the cell while maintaining aspect ratio
            Bitmap scaledImage = scaleAndCropBitmap(image, cellWidth, cellHeight);

            // Calculate the position for the image
            int x = isHor ? 0 : i * cellWidth;
            int y = isHor ? i * cellHeight : 0;  // Only one row for non-horizontal case

            // Draw the scaled image onto the collage
            canvas.drawBitmap(scaledImage, x, y, null);

            // Optionally draw a border around the image
            if (borderCheck) {
                canvas.drawRect(x, y, x + cellWidth, y + cellHeight, borderPaint);
            }
        }

        // Save the collage to the gallery
        saveImageToGallery(context, collage, outputFileName);
    }

    private Bitmap rotateBitmap(Bitmap bitmap, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private Bitmap scaleAndCropBitmap(Bitmap bitmap, int width, int height) {
        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();

        // Calculate scaling factors
        float scale = Math.max((float) width / originalWidth, (float) height / originalHeight);

        // Calculate new dimensions
        int scaledWidth = Math.round(scale * originalWidth);
        int scaledHeight = Math.round(scale * originalHeight);

        // Scale the bitmap
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true);

        // Create a new bitmap for the cropped image
        Bitmap croppedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(croppedBitmap);

        // Draw the scaled bitmap onto the canvas, centered
        int left = (scaledWidth - width) / 2;
        int top = (scaledHeight - height) / 2;
        canvas.drawBitmap(scaledBitmap, -left, -top, null);

        return croppedBitmap;
    }

    public void saveImageToGallery(Context context, Bitmap bitmap, String fileName) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

        Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri != null) {
            try (OutputStream out = context.getContentResolver().openOutputStream(uri)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
