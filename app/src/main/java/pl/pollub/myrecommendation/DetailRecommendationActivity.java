package pl.pollub.myrecommendation;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.http.SslError;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.okhttp.internal.Internal;

import java.util.List;

import pl.pollub.myrecommendation.models.Category;
import pl.pollub.myrecommendation.models.Recommendation;
import pl.pollub.myrecommendation.models.User;
import pl.pollub.myrecommendation.utils.MyUtil;

public class DetailRecommendationActivity extends AppCompatActivity implements View.OnClickListener {

    final int REQUEST_CALL = 22;
    private TextView tvTitle, tvDescription, tvIdea,
            tvLink, tvPhone, tvAddress, tvLocationLink,
            tvUserName, tvTimeAgo, tvCategory;

    private ImageView ivImage, ivLink, ivPin, ivPhone, ivLocationLink, ivProfilePicture;

    private String recommendationId;
    private FirebaseAuth mAuth;
    private FirebaseFirestore mFireStore;
    private User currentUser = new User();

    private String title, description, idea, imageUrl,
            websiteUrl, locationLink, locationId, phoneNumber, address;
    private int recommendationType;
    private double longitude, latitude;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_recommendation);

        Intent intent = getIntent();
        recommendationId = intent.getStringExtra("recommendation_id");

        mAuth = FirebaseAuth.getInstance();
        mFireStore = FirebaseFirestore.getInstance();

        currentUser.setId(mAuth.getUid());

        tvTitle = findViewById(R.id.tvDetailRecTitle);
        tvDescription = findViewById(R.id.tvDetailRecDescription);
        tvIdea = findViewById(R.id.tvDetailRecIdea);
        tvLink = findViewById(R.id.tvDetailRecWebsite);
        tvPhone = findViewById(R.id.tvDetailRecPhone);
        tvAddress = findViewById(R.id.tvDetailRecAddress);
        tvLocationLink = findViewById(R.id.tvDetailRecLocationLink);
        tvUserName = findViewById(R.id.tvDetailRecUserName);
        tvCategory = findViewById(R.id.tvDetailRecCategory);

        ivImage = findViewById(R.id.ivDetailRecImage);
        ivLink = findViewById(R.id.ivDetailRecLink);
        ivPin = findViewById(R.id.ivDetailRecPin);
        ivPhone = findViewById(R.id.ivDetailRecPhone);
        ivLocationLink = findViewById(R.id.ivDetailRecLocationLink);
        ivProfilePicture = findViewById(R.id.ivDetailRecProfilePicture);

        tvLocationLink.setOnClickListener(this);
        tvLink.setOnClickListener(this);
        tvPhone.setOnClickListener(this);
        tvAddress.setOnClickListener(this);

        loadUserInfo();
        loadRecommendation();

    }

    private void loadRecommendation() {
        mFireStore.collection("Recommendation").document(recommendationId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.getResult().exists()) {
                    DocumentSnapshot data = task.getResult();
                    title = MyUtil.getNullOrString(data.get("title"));
                    description = MyUtil.getNullOrString(data.get("description"));
                    idea = MyUtil.getNullOrString(data.get("idea"));
                    imageUrl = MyUtil.getNullOrString(data.get("image_url"));
                    recommendationType = Integer.parseInt(MyUtil.getNullOrString(data.get("recommendation_type")));

                    tvTitle.setText(title);
                    tvDescription.setText(description);
                    if (!TextUtils.isEmpty(idea)) {
                        tvIdea.setText(idea);
                    } else {
                        tvIdea.setVisibility(View.GONE);
                    }

                    RequestOptions placeholderOption = new RequestOptions().placeholder(R.color.gray);
                    Glide.with(DetailRecommendationActivity.this).applyDefaultRequestOptions(placeholderOption)
                            .load(Uri.parse(imageUrl)).into(ivImage);

                    if (recommendationType == Recommendation.TYPE_LINK) {
                        websiteUrl = MyUtil.getNullOrString(data.get("website_url"));
                        ivPin.setVisibility(View.GONE);
                        ivPhone.setVisibility(View.GONE);
                        ivLocationLink.setVisibility(View.GONE);
                        tvAddress.setVisibility(View.GONE);
                        tvPhone.setVisibility(View.GONE);
                        tvLocationLink.setVisibility(View.GONE);
                        String lblWebsiteUrl = websiteUrl;
                        if (websiteUrl.length() > 81)
                            lblWebsiteUrl = websiteUrl.subSequence(0, 80) + "...";
                        tvLink.setText(lblWebsiteUrl);
                    } else {
                        address = MyUtil.getNullOrString(data.get("location.address"));
                        locationLink = MyUtil.getNullOrString(data.get("location.url"));
                        phoneNumber = MyUtil.getNullOrString(data.get("location.phone_number"));
                        locationId = MyUtil.getNullOrString(data.get("location.location_id"));
                        longitude = Double.parseDouble(MyUtil.getNullOrString(data.get("location.longitude")));
                        latitude = Double.parseDouble(MyUtil.getNullOrString(data.get("location.latitude")));
                        ivLink.setVisibility(View.GONE);
                        tvLink.setVisibility(View.GONE);
                        tvAddress.setText(address);
                        if (TextUtils.isEmpty(phoneNumber)) {
                            tvPhone.setVisibility(View.GONE);
                            ivPhone.setVisibility(View.GONE);
                        } else {
                            tvPhone.setText(phoneNumber);
                        }

                        if (TextUtils.isEmpty(locationLink)) {
                            tvLocationLink.setVisibility(View.GONE);
                            ivLocationLink.setVisibility(View.GONE);
                        } else {
                            String lblLocationLink = locationLink;
                            if (locationLink.length() > 81)
                                lblLocationLink = locationLink.subSequence(0, 80) + "...";
                            tvLocationLink.setText(lblLocationLink);
                        }
                    }

                    loadRecommendationUser(data.get("user_id").toString());
                    loadCategory(data.get("category_id").toString());
                }
            }
        });
    }

    private void loadUserInfo() {

        mFireStore.collection("Users").document(currentUser.getId()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.getResult().exists()) {
                    DocumentSnapshot data = task.getResult();
                    currentUser.setName(data.get("name").toString());
                    currentUser.setProfilePicture(data.get("profile_picture").toString());
                    currentUser.setSex(data.get("sex").toString());
                    currentUser.setInterestedCategories((List<String>) data.get("interested_categories"));

                }
            }
        });
    }

    private void loadRecommendationUser(String userId) {
        mFireStore.collection("Users").document(userId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.getResult().exists()) {
                    DocumentSnapshot data = task.getResult();
                    tvUserName.setText(data.get("name").toString());

                    RequestOptions placeholderOption = new RequestOptions().placeholder(R.color.gray);
                    Glide.with(DetailRecommendationActivity.this).applyDefaultRequestOptions(placeholderOption)
                            .load(Uri.parse(data.get("profile_picture").toString())).into(ivProfilePicture);
                }
            }
        });
    }

    private void loadCategory(String categoryId) {
        mFireStore.collection("categories").document(categoryId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.getResult().exists()) {
                    DocumentSnapshot data = task.getResult();
                    tvCategory.setText(data.get("name").toString());
                }
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tvDetailRecWebsite:
                toWebView(websiteUrl);
                break;
            case R.id.tvDetailRecLocationLink:
                toWebView(locationLink);
                break;
            case R.id.tvDetailRecAddress:
                toMap();
                break;
            case R.id.tvDetailRecPhone:
                makePhoneCall();
                break;
        }
    }

    private void toWebView(String url) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        startActivity(i);

    }

    private void toMap() {
        Intent intent = new Intent(DetailRecommendationActivity.this, MapActivity.class);
        intent.putExtra("longitude", longitude);
        intent.putExtra("latitude", latitude);
        intent.putExtra("title", title);
        startActivity(intent);
    }


    private void makePhoneCall() {
        if (phoneNumber.trim().length() > 0) {

            if (ContextCompat.checkSelfPermission(DetailRecommendationActivity.this,
                    android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(DetailRecommendationActivity.this,
                        new String[]{android.Manifest.permission.CALL_PHONE}, REQUEST_CALL);
            } else {
                String dial = "tel:" + phoneNumber;
                startActivity(new Intent(Intent.ACTION_CALL, Uri.parse(dial)));
            }

        } else {
            Toast.makeText(DetailRecommendationActivity.this, "Enter Phone Number", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CALL) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                makePhoneCall();
            } else {
                Toast.makeText(this, "Permission DENIED", Toast.LENGTH_SHORT).show();
            }
        }
    }

}
