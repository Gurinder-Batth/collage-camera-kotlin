package com.fennecstero;


import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ImageManipulationHelper {

    private Context context;

    public ImageManipulationHelper(Context context) {
        this.context = context;
    }

    public Bitmap loadBitmapFromUri(Uri uri) {
        try {
            ContentResolver contentResolver = context.getContentResolver();
            return BitmapFactory.decodeStream(contentResolver.openInputStream(uri));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }



    private Uri saveBitmapToFile(Bitmap bitmap, String filename) {
        try {
            File imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            if (!imagesDir.exists()) {
                imagesDir.mkdirs();
            }
            // Reduce the quality to 80 (you can adjust this as needed)
            int quality = 50;
            File imageFile = new File(imagesDir, filename);
            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, quality, fos);
            fos.flush();
            fos.close();
            notifySystemAboutNewImage(imageFile.getPath());


            return Uri.fromFile(imageFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void notifySystemAboutNewImage(String imagePath) {

        // Array of paths you want to scan for media
        String[] paths = {imagePath};

        // Array of MIME types to be scanned
        String[] mimeTypes = {"image/*"};

        // Callback invoked when scanning is complete
        MediaScannerConnection.OnScanCompletedListener callback = new MediaScannerConnection.OnScanCompletedListener() {
            @Override
            public void onScanCompleted(String path, Uri uri) {
                // Scan completed, you can perform any additional actions here
            }
        };

        // Request media scanning
        MediaScannerConnection.scanFile(context, paths, mimeTypes, callback);
    }


//    public Uri createAndSaveCollage(Uri imageUri1, Uri imageUri2, String collageFilename, Boolean borderCheckBox) {
//        Bitmap bitmap1 = loadBitmapFromUri(imageUri1);
//        Bitmap bitmap2 = loadBitmapFromUri(imageUri2);
//
//        if (bitmap1 == null || bitmap2 == null) {
//            return null;
//        }
//
//        if (borderCheckBox) {
//            // Apply border to images
//            bitmap1 = applyBorder(bitmap1);
//            bitmap2 = applyBorder(bitmap2);
//        }
//
//        // Calculate the target width and height for the collage
//        int targetWidth = bitmap1.getWidth() + bitmap2.getWidth();
//        int targetHeight = (targetWidth * 3) / 5; // 4:5 aspect ratio
//
//        // Create a new bitmap with the target dimensions
//        Bitmap collageBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
//
//        Canvas canvas = new Canvas(collageBitmap);
//        canvas.drawColor(Color.WHITE);  // Background color
//
//        // Draw the first image onto the collage
//        canvas.drawBitmap(bitmap1, 0, 0, null);
//
//        // Draw the second image next to the first image
//        canvas.drawBitmap(bitmap2, bitmap1.getWidth(), 0, null);
//
//        // Save the collage bitmap to a file
//        return saveBitmapToFile(collageBitmap, collageFilename);
//    }

    public Uri createAndSaveCollage(Uri imageUri1, Uri imageUri2, String collageFilename, boolean applyBorder) {
        Bitmap bitmap1 = loadScaledBitmapFromUri(imageUri1, 800, 1000);  // Example max size
        Bitmap bitmap2 = loadScaledBitmapFromUri(imageUri2, 800, 1000);  // Example max size

        if (bitmap1 == null || bitmap2 == null) {
            return null;
        }

        // Crop the bitmaps to a 4:5 aspect ratio
        bitmap1 = cropToAspectRatio(bitmap1, 4, 5);
        bitmap2 = cropToAspectRatio(bitmap2, 4, 5);

        if (applyBorder) {
            // Apply border to images
            bitmap1 = applyBorder(bitmap1, 20, 20, 20, 10);
            bitmap2 = applyBorder(bitmap2, 20, 20, 10, 20);
        }

        // Calculate the target dimensions for the collage to maintain a 4:5 aspect ratio
        int targetHeight = Math.max(bitmap1.getHeight(), bitmap2.getHeight());
        int targetWidth = (int) (targetHeight * 4.0 / 5.0);

        // Ensure targetWidth fits both images
        if (bitmap1.getWidth() + bitmap2.getWidth() > targetWidth) {
            // Scale down the images to fit the target width
            float scale = (float) targetWidth / (bitmap1.getWidth() + bitmap2.getWidth());
            bitmap1 = Bitmap.createScaledBitmap(bitmap1, (int) (bitmap1.getWidth() * scale), (int) (bitmap1.getHeight() * scale), true);
            bitmap2 = Bitmap.createScaledBitmap(bitmap2, (int) (bitmap2.getWidth() * scale), (int) (bitmap2.getHeight() * scale), true);
        }

        // Create a new bitmap with the target dimensions
        Bitmap collageBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(collageBitmap);
        canvas.drawColor(Color.WHITE);  // Background color

        // Calculate positions to center the images
        int left1 = (targetWidth / 2) - bitmap1.getWidth();
        int top1 = (targetHeight - bitmap1.getHeight()) / 2;
        int left2 = targetWidth / 2;
        int top2 = (targetHeight - bitmap2.getHeight()) / 2;

        // Draw the first image onto the collage
        canvas.drawBitmap(bitmap1, left1, top1, null);

        // Draw the second image next to the first image
        canvas.drawBitmap(bitmap2, left2, top2, null);

        // Save the collage bitmap to a file
        return saveBitmapToFile(collageBitmap, collageFilename);
    }

    private Bitmap loadScaledBitmapFromUri(Uri uri, int maxWidth, int maxHeight) {
        // Load a scaled-down version of the image to save memory
        try {
            InputStream input = context.getContentResolver().openInputStream(uri);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(input, null, options);
            input.close();

            int originalWidth = options.outWidth;
            int originalHeight = options.outHeight;
            int scaleFactor = Math.min(originalWidth / maxWidth, originalHeight / maxHeight);

            options.inJustDecodeBounds = false;
            options.inSampleSize = scaleFactor;
            input = context.getContentResolver().openInputStream(uri);
            Bitmap scaledBitmap = BitmapFactory.decodeStream(input, null, options);
            input.close();
            return scaledBitmap;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Bitmap cropToAspectRatio(Bitmap bitmap, int aspectX, int aspectY) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int newWidth = width;
        int newHeight = height;

        if (width * aspectY > height * aspectX) {
            newWidth = height * aspectX / aspectY;
        } else {
            newHeight = width * aspectY / aspectX;
        }

        int xOffset = (width - newWidth) / 2;
        int yOffset = (height - newHeight) / 2;

        return Bitmap.createBitmap(bitmap, xOffset, yOffset, newWidth, newHeight);
    }


//    working code
//    public  Uri createAndSaveCollage(Uri imageUri1, Uri imageUri2, String collageFilename, boolean applyBorder) {
//        Bitmap bitmap1 = loadBitmapFromUri(imageUri1);
//        Bitmap bitmap2 = loadBitmapFromUri(imageUri2);
//
//        if (bitmap1 == null || bitmap2 == null) {
//            return null;
//        }
//
//        if (applyBorder) {
//            // Apply border to images
//            bitmap1 = applyBorder(bitmap1, 20, 20, 20, 10);
//            bitmap2 = applyBorder(bitmap2,20, 20, 10, 20);
//        }
//
//        // Calculate the target width and height for the collage
//        int targetWidth = bitmap1.getWidth() + bitmap2.getWidth();
//        int targetHeight = Math.max(bitmap1.getHeight(), bitmap2.getHeight());
//
//        // Create a new bitmap with the target dimensions
//        Bitmap collageBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
//
//        Canvas canvas = new Canvas(collageBitmap);
//        canvas.drawColor(Color.WHITE);  // Background color
//
//        // Draw the first image onto the collage
//        canvas.drawBitmap(bitmap1, 0, 0, null);
//
//        // Draw the second image next to the first image
//        canvas.drawBitmap(bitmap2, bitmap1.getWidth(), 0, null);
//
//        // Save the collage bitmap to a file
//        return saveBitmapToFile(collageBitmap, collageFilename);
//    }


    public void createAndSaveCollageAsync(Uri imageUri1, Uri imageUri2, String collageFilename, Boolean borderCheckBox) {
//        new Thread(() -> {
            // Load and manipulate images
        Bitmap bitmap1 = loadBitmapFromUri(imageUri1);
        Bitmap bitmap2 = loadBitmapFromUri(imageUri2);

        if (bitmap1 == null || bitmap2 == null) {
            return;
        }



// Calculate collage dimensions
        float aspectRatio = (float) bitmap1.getWidth() / bitmap1.getHeight();
        int targetWidth = bitmap1.getWidth() + bitmap2.getWidth();
        int targetHeight = (int) (targetWidth / aspectRatio);

// Create a new bitmap with the target dimensions
        Bitmap collageBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(collageBitmap);
        canvas.drawColor(Color.WHITE);  // Background color

// Calculate the scale factor for resizing the images
        float scaleFactor1 = (float) targetHeight / bitmap1.getHeight();
        float scaleFactor2 = (float) targetHeight / bitmap2.getHeight();

// Calculate the scaled width for each image
        int scaledWidth1 = (int) (bitmap1.getWidth() * scaleFactor1);
        int scaledWidth2 = (int) (bitmap2.getWidth() * scaleFactor2);

// Calculate the center coordinates for cropping
        int centerX1 = (scaledWidth1 - bitmap1.getWidth()) / 2;
        int centerX2 = (scaledWidth2 - bitmap2.getWidth()) / 2;

// Resize and crop the first image
        Bitmap scaledBitmap1 = Bitmap.createScaledBitmap(bitmap1, scaledWidth1, targetHeight, true);
        Bitmap croppedBitmap1 = Bitmap.createBitmap(scaledBitmap1, centerX1, 0, bitmap1.getWidth(), targetHeight);

// Resize and crop the second image
        Bitmap scaledBitmap2 = Bitmap.createScaledBitmap(bitmap2, scaledWidth2, targetHeight, true);
        Bitmap croppedBitmap2 = Bitmap.createBitmap(scaledBitmap2, centerX2, 0, bitmap2.getWidth(), targetHeight);


        if (borderCheckBox) {
            // Draw the first image onto the collage
            canvas.drawBitmap(croppedBitmap1, 0, 0, null);
            // Draw a border around the first image
            int borderWidth = 20; // Adjust border width as needed
            Paint borderPaint = new Paint();
            borderPaint.setColor(Color.WHITE);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(borderWidth);
            canvas.drawRect(0, 0, croppedBitmap1.getWidth(), targetHeight, borderPaint);
            canvas.drawBitmap(croppedBitmap2, croppedBitmap1.getWidth(), 0, null);
            canvas.drawRect(croppedBitmap1.getWidth(), 0, targetWidth, targetHeight, borderPaint);

        }
        else {
            canvas.drawBitmap(croppedBitmap1, 0, 0, null);
            canvas.drawBitmap(croppedBitmap2, croppedBitmap1.getWidth(), 0, null);
        }

// Save the collage bitmap to a file
        Uri collageUri = saveBitmapToFile(collageBitmap, collageFilename);


        // Update UI or perform any necessary actions with collageUri (e.g., display the saved collage)
//        }).start();
    }


    private void drawBorder(Canvas canvas, int width, int height) {
        int borderWidth = 10; // Set the border width as needed
        Paint borderPaint = new Paint();
        borderPaint.setColor(Color.RED);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(borderWidth);

        // Draw the border
        canvas.drawRect(borderWidth / 2, borderWidth / 2, width - borderWidth / 2, height - borderWidth / 2, borderPaint);
    }

    private Bitmap applyBorder(Bitmap bitmap, int topBorderWidth, int bottomBorderWidth, int leftBorderWidth, int rightBorderWidth) {
        int borderedWidth = bitmap.getWidth() + leftBorderWidth + rightBorderWidth;
        int borderedHeight = bitmap.getHeight() + topBorderWidth + bottomBorderWidth;

        Bitmap borderedBitmap = Bitmap.createBitmap(borderedWidth, borderedHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(borderedBitmap);
        canvas.drawColor(Color.WHITE);  // Background color

        Paint borderPaint = new Paint();
        borderPaint.setColor(Color.WHITE); // Border color
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setAntiAlias(false); // Disable anti-aliasing

        // Draw the borders with different widths
        // Top border
        canvas.drawRect(leftBorderWidth / 2f, topBorderWidth / 2f,
                borderedWidth - rightBorderWidth / 2f, topBorderWidth / 2f + topBorderWidth, borderPaint);
        // Bottom border
        canvas.drawRect(leftBorderWidth / 2f, borderedHeight - bottomBorderWidth / 2f - bottomBorderWidth,
                borderedWidth - rightBorderWidth / 2f, borderedHeight - bottomBorderWidth / 2f, borderPaint);
        // Left border
        canvas.drawRect(leftBorderWidth / 2f, topBorderWidth / 2f + topBorderWidth,
                leftBorderWidth / 2f + leftBorderWidth, borderedHeight - bottomBorderWidth / 2f - bottomBorderWidth, borderPaint);
        // Right border
        canvas.drawRect(borderedWidth - rightBorderWidth / 2f - rightBorderWidth, topBorderWidth / 2f + topBorderWidth,
                borderedWidth - rightBorderWidth / 2f, borderedHeight - bottomBorderWidth / 2f - bottomBorderWidth, borderPaint);

        // Draw the original image on top of the borders
        canvas.drawBitmap(bitmap, leftBorderWidth, topBorderWidth, null);

        return borderedBitmap;
    }



}

