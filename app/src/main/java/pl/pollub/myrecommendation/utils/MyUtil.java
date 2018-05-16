package pl.pollub.myrecommendation.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;

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
import pl.pollub.myrecommendation.models.WebContent;
import pl.pollub.myrecommendation.threads.WebContentTask;

public class MyUtil {

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
}
