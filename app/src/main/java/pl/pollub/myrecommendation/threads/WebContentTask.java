package pl.pollub.myrecommendation.threads;

import android.net.Uri;

import com.google.firebase.firestore.DocumentSnapshot;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import pl.pollub.myrecommendation.models.WebContent;

public class WebContentTask implements Callable<WebContent> {

    protected String url;

    public WebContentTask(String url){
        if(!url.contains("http://") && !url.contains("https://")){
            url = "http://" + url;
        }
        this.url = url;
    }

    @Override
    public WebContent call() throws Exception {
        Document document = null;
        try {
            document = Jsoup.connect(this.url).get();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String title = this.getTitle(document);
        String description = this.getDescriptions(document);
        String imageUrl = this.getImageUrl(document);



        WebContent webContent = new WebContent();
        webContent.setTitle(title);
        webContent.setDescription(description);
        webContent.setImageUrl(imageUrl);

        return webContent;

    }

    private String getDescriptions(Document document){
        List<String> descriptionSelectors = new ArrayList<>();
        descriptionSelectors.add("meta[property=og-description]");
        descriptionSelectors.add("meta[name=twitter:description]");
        descriptionSelectors.add("meta[name=description]");
        for(String selector:descriptionSelectors){
            Element descriptionElement = document.select(selector).first();
            if(descriptionElement == null) continue;
            else return descriptionElement.attr("content");
        }

        return null;
    }

    private String getTitle(Document document){
        List<String> selectors = new ArrayList<>();
        selectors.add("meta[property=og-title]");
        selectors.add("meta[name=twitter:title]");
        String title = null;
        for(String selector:selectors){
            Element element = document.select(selector).first();
            if(element == null) continue;
            else title = element.attr("content");
        }

        if(title == null) title = document.title();
        return title;
    }

    private String getImageUrl(Document document){
        List<String> selectors = new ArrayList<>();
        selectors.add("meta[property=og-image]");
        selectors.add("meta[name=twitter:image]");
        String url = null;
        for(String selector:selectors){
            Element element = document.select(selector).first();
            if(element == null) continue;
            else url = element.attr("content");
        }

        if(url == null){
            Elements images = document.select("img");
            List<Uri> imageUri = new ArrayList<>();
            for(Element image:images){
                url = image.absUrl("src");
                break;
            }
        }

        return url;
    }
}
