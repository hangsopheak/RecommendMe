package pl.pollub.myrecommendation.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;

import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import id.zelory.compressor.Compressor;
import pl.pollub.myrecommendation.ProfileSettingActivity;
import pl.pollub.myrecommendation.models.WebContent;
import pl.pollub.myrecommendation.threads.WebContentTask;

public class MyUtil {
    private static final int SECOND_MILLIS = 1000;
    private static final int MINUTE_MILLIS = 60 * SECOND_MILLIS;
    private static final int HOUR_MILLIS = 60 * MINUTE_MILLIS;
    private static final int DAY_MILLIS = 24 * HOUR_MILLIS;

    public static byte[] getCompressedImage(Context context, Uri uri, int maxWidth, int maxHeight, int quality){
        Bitmap compressedImageFile = null;
        File imageFile = new File(uri.getPath());
        try {
            compressedImageFile = new Compressor(context)
                    .setMaxHeight(maxHeight)
                    .setMaxWidth(maxWidth)
                    .setQuality(quality)
                    .compressToBitmap(imageFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        compressedImageFile.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        return baos.toByteArray();
    }

    public static WebContent scrapWebContent(String url){
        ExecutorService service = Executors.newFixedThreadPool(1);
        url = addUrlProtocol(url);
        Future<WebContent> webContentFuture =service.submit(new WebContentTask(url));
        service.shutdown();
        WebContent webContent = new WebContent();
        try {
            webContent = webContentFuture.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return webContent;
    }

    public static void pickImage(Activity activity, int ratioX, int ratioY) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(ContextCompat.checkSelfPermission(activity,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},2);
            }else{
                bringPicturePicker(activity, ratioX, ratioY);
            }
        }else{
            bringPicturePicker(activity, ratioX, ratioY);
        }
    }

    private static void bringPicturePicker(Activity activity, int ratioX, int ratioY) {
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setAspectRatio(ratioX,ratioY)
                .setMinCropResultSize(100,100)
                .setMaxCropResultSize(5000,5000)
                .start(activity);
    }

    public static Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        Uri uri = Uri.parse(path);
        return Uri.parse(getRealPathFromUri(inContext, uri));
    }

    public static String getRealPathFromUri(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public static String addUrlProtocol(String url){
        if(!url.contains("http://") && !url.contains("https://")){
            url = "http://" + url;
        }
        return url;
    }

    public static String getUrlFromIntentFilter(String textData){
        String url = null;
        int pos = 0;
        if(!TextUtils.isEmpty(textData)){
            if(textData.contains("https")){
                pos = textData.indexOf("https");
            }else if(textData.contains("http")){
                pos = textData.indexOf("http");
            }else{
                return null;
            }
            url = textData.substring(pos);
        }
        return url;
    }

    public static String getNullOrString(Object string){
        if(string == null) return null;
        else return string.toString();
    }




    public static String getTimeAgo(long time, Context ctx) {
        if (time < 1000000000000L) {
            // if timestamp given in seconds, convert to millis
            time *= 1000;
        }

        long now = System.currentTimeMillis();
        if (time > now || time <= 0) {
            return null;
        }

        // TODO: localize
        final long diff = now - time;
        if (diff < MINUTE_MILLIS) {
            return "just now";
        } else if (diff < 2 * MINUTE_MILLIS) {
            return "a minute ago";
        } else if (diff < 50 * MINUTE_MILLIS) {
            return diff / MINUTE_MILLIS + " minutes ago";
        } else if (diff < 90 * MINUTE_MILLIS) {
            return "an hour ago";
        } else if (diff < 24 * HOUR_MILLIS) {
            return diff / HOUR_MILLIS + " hours ago";
        } else if (diff < 48 * HOUR_MILLIS) {
            return "yesterday";
        } else {
            return diff / DAY_MILLIS + " days ago";
        }
    }
}
