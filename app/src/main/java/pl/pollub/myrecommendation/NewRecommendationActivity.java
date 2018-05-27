package pl.pollub.myrecommendation;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlacePhotoMetadata;
import com.google.android.gms.location.places.PlacePhotoMetadataBuffer;
import com.google.android.gms.location.places.PlacePhotoMetadataResponse;
import com.google.android.gms.location.places.PlacePhotoResponse;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.ServerTimestamp;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import pl.pollub.myrecommendation.models.Category;
import pl.pollub.myrecommendation.models.LinkRecommendation;
import pl.pollub.myrecommendation.models.Location;
import pl.pollub.myrecommendation.models.LocationRecommendation;
import pl.pollub.myrecommendation.models.Recommendation;
import pl.pollub.myrecommendation.models.User;
import pl.pollub.myrecommendation.models.WebContent;
import pl.pollub.myrecommendation.utils.MyUtil;

public class NewRecommendationActivity extends AppCompatActivity implements
        RadioGroup.OnCheckedChangeListener, View.OnClickListener{

    private EditText etLink, etTitle, etDescription, etIdea;
    private ImageView ivImage, ivPin;
    private RadioGroup rgPostType;
    private RadioButton rbByWebsite, rbByLocation;
    private Spinner spCategory;
    private TextView tvLblWebsiteURL, tvLblLocation, tvLocationName;
    private Button btnPost;
    private ProgressBar progressBar;

    private Timer timer = new Timer();
    private final long DELAY = 1000; // in ms

    private GoogleApiClient mGoogleApiClient;
    private int PLACE_PICKER_REQUEST = 10;

    private GeoDataClient mGeoDataClient;

    private Handler handler;
    private Place place;

    private FirebaseFirestore mFirestore;
    private FirebaseAuth mAuth;
    private StorageReference mStorageReference;

    private String userId;
    private Uri imageUri;
    private int recommendationType = Recommendation.TYPE_LINK;
    private Location selectedLocation = new Location();
    private Recommendation recommendation = new Recommendation();
    private Category selectedCategory;
    private String downloadUrl;
    private List<String> interestedCategories;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_recommendation);




        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        etLink = findViewById(R.id.etNewRecLink);
        etDescription = findViewById(R.id.etNewRecDescription);
        etTitle = findViewById(R.id.etNewRecTitle);
        etIdea = findViewById(R.id.etNewRecIdea);
        ivImage = findViewById(R.id.ivNewRecImage);
        ivPin = findViewById(R.id.ivNewRecPickLocation);
        rgPostType = findViewById(R.id.rgNewRecPostType);
        rbByWebsite = findViewById(R.id.rbNewRecByLink);
        rbByLocation = findViewById(R.id.rbNewRecByLocation);
        spCategory = findViewById(R.id.spNewRecCategory);
        tvLblWebsiteURL = findViewById(R.id.tvNewRecLblWebsiteUrl);
        tvLblLocation = findViewById(R.id.tvNewRecLblLocation);
        tvLocationName = findViewById(R.id.tvNewRecLocationName);
        btnPost = findViewById(R.id.btnNewRecPost);
        progressBar = findViewById(R.id.pbNewRec);

        rgPostType.check(R.id.rbNewRecByLink);
        etLink.requestFocus();
        handler = new Handler();

        rgPostType.setOnCheckedChangeListener(this);
        ivImage.setOnClickListener(this);
        ivPin.setOnClickListener(this);
        btnPost.setOnClickListener(this);


        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .build();

        mGeoDataClient = Places.getGeoDataClient(this,null);
        mFirestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        mStorageReference = FirebaseStorage.getInstance().getReference();


        userId = mAuth.getCurrentUser().getUid();

        etLink.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(timer != null)
                    timer.cancel();
            }

            @Override
            public void afterTextChanged(Editable editable) {
                //avoid triggering event when text is too short
                if (editable.length() >= 3) {
                    timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            String link = etLink.getText().toString();
                            final WebContent webContent = MyUtil.scrapWebContent(link);
                            // to run on UI thread
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    etTitle.setText(webContent.getTitle());
                                    etDescription.setText(webContent.getDescription());
                                    if(webContent.getImageUrl()!= null){
                                        imageUri = Uri.parse(webContent.getImageUrl());
                                        Glide.with(NewRecommendationActivity.this)
                                                .load(Uri.parse(webContent.getImageUrl())).into(ivImage);
                                    }
                                }
                            });
                        }
                    }, DELAY);
                }
            }
        });
        loadInterestedCategory();
        updateRecommendationTypeUI();


        spCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                selectedCategory = (Category) parent.getSelectedItem();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        Intent intent = getIntent();
        String intentAction = intent.getAction();
        if(intentAction != null){
            if (intentAction.equals(Intent.ACTION_SEND)) {
                String url = MyUtil.getUrlFromIntentFilter(intent.getStringExtra(Intent.EXTRA_TEXT));
                etLink.setText(url);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                Place place = PlacePicker.getPlace(data, this);
                selectedLocation.setName(place.getName().toString());
                selectedLocation.setAddress(place.getAddress().toString());
                selectedLocation.setLatitude(place.getLatLng().latitude);
                selectedLocation.setLongitude(place.getLatLng().longitude);
                selectedLocation.setLocationId(place.getId());
                if(place.getPhoneNumber() != null) selectedLocation.setPhoneNumber(place.getPhoneNumber().toString());
                if(place.getWebsiteUri() != null) selectedLocation.setUrl(place.getWebsiteUri().toString());

                etTitle.setText(selectedLocation.getName());
                etDescription.setText(selectedLocation.getAddress());
                getPhotoMetadata(place.getId());

            }
        }else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                imageUri = result.getUri();
                ivImage.setImageURI(imageUri);
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
                Toast.makeText(this, "Error image picker: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    protected void getPhotoMetadata(String placeId) {
        final Task<PlacePhotoMetadataResponse> photoMetadataResponse = mGeoDataClient.getPlacePhotos(placeId);
        photoMetadataResponse.addOnCompleteListener(new OnCompleteListener<PlacePhotoMetadataResponse>() {
            @Override
            public void onComplete(@NonNull Task<PlacePhotoMetadataResponse> task) {
                // Get the list of photos.
                PlacePhotoMetadataResponse photos = task.getResult();
                // Get the PlacePhotoMetadataBuffer (metadata for all of the photos).
                PlacePhotoMetadataBuffer photoMetadataBuffer = photos.getPhotoMetadata();
                if(photoMetadataBuffer.getCount() > 0){
                    // Get the first photo in the list.
                    PlacePhotoMetadata photoMetadata = photoMetadataBuffer.get(0);
                    // Get the attribution text.
                    CharSequence attribution = photoMetadata.getAttributions();
                    // Get a full-size bitmap for the photo.
                    Task<PlacePhotoResponse> photoResponse = mGeoDataClient.getPhoto(photoMetadata);
                    photoResponse.addOnCompleteListener(new OnCompleteListener<PlacePhotoResponse>() {
                        @Override
                        public void onComplete(@NonNull Task<PlacePhotoResponse> task) {
                            PlacePhotoResponse photo = task.getResult();
                            Bitmap bitmap = photo.getBitmap();
                            ivImage.invalidate();
                            ivImage.setImageBitmap(bitmap);
                            imageUri = MyUtil.getImageUri(NewRecommendationActivity.this, bitmap);
                            selectedLocation.setImageUri(imageUri);
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int i) {
        switch(i){
            case R.id.rbNewRecByLink:
                recommendationType = Recommendation.TYPE_LINK;
                updateRecommendationTypeUI();
                break;
            case R.id.rbNewRecByLocation:
                recommendationType = Recommendation.TYPE_LOCATION;
                updateRecommendationTypeUI();
                break;
        }
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.ivNewRecImage:
                MyUtil.pickImage(NewRecommendationActivity.this, 6, 4);
                break;
            case R.id.ivNewRecPickLocation:
                pickLocation();
                break;
            case R.id.btnNewRecPost:
                save();
                break;
        }
    }

    private void save() {
        recommendation.setUserId(userId);
        recommendation.setCategoryId(selectedCategory.getId());
        recommendation.setCategory(selectedCategory);
        recommendation.setDescription(etDescription.getText().toString());
        recommendation.setTitle(etTitle.getText().toString());
        recommendation.setIdea(etIdea.getText().toString());
        recommendation.setImageUri(imageUri);
        if(TextUtils.isEmpty(recommendation.getCategoryId()) ||
           recommendation.getCategoryId().equals("0") ||
           TextUtils.isEmpty(recommendation.getDescription()) ||
           TextUtils.isEmpty(recommendation.getTitle()) ||
           recommendation.getImageUri() == null){
            Toast.makeText(this, "Please enter all require fields!", Toast.LENGTH_SHORT).show();
            return;
        }

        if(!URLUtil.isHttpUrl(imageUri.toString()) && !URLUtil.isHttpsUrl(imageUri.toString())){
            byte[] thumbData = MyUtil.getCompressedImage(NewRecommendationActivity.this,
                    imageUri, 400, 400,40);

            Long currentTimestamp = System.currentTimeMillis() / 1000L;
            progressBar.setVisibility(View.VISIBLE);
            UploadTask uploadTask = mStorageReference.child("recommendation_images").
                    child(currentTimestamp.toString() + ".jpg").putBytes(thumbData);

            uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                    if(task.isSuccessful()){
                        downloadUrl = task.getResult().getDownloadUrl().toString();
                        if(recommendationType == Recommendation.TYPE_LINK){
                            saveLinkRecommendation();
                        }else if(recommendationType == Recommendation.TYPE_LOCATION){
                            saveLocationRecommendation();
                        }
                    }else{

                        String error = task.getException().getMessage();
                        Toast.makeText(NewRecommendationActivity.this, "Storage Error: " + error, Toast.LENGTH_LONG).show();
                        progressBar.setVisibility(View.INVISIBLE);
                    }

                }
            });
        }else{
            downloadUrl = imageUri.toString();
            if(recommendationType == Recommendation.TYPE_LINK){
                saveLinkRecommendation();
            }else if(recommendationType == Recommendation.TYPE_LOCATION){
                saveLocationRecommendation();
            }
        }



    }

    private void saveLocationRecommendation() {

        if(TextUtils.isEmpty(selectedLocation.getLocationId())){
            Toast.makeText(this, "Please pick the location!", Toast.LENGTH_SHORT).show();
            return;
        }

        final Map<String, Object> locationMap = getLocationMap();
        mFirestore.collection("Locations").document(selectedLocation.getLocationId())
                .set(locationMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    Map<String, Object> recommendationMap = getCommonRecommendationMap();
                    recommendationMap.put("location", locationMap);
                    Long currentTimestamp = System.currentTimeMillis() / 1000L;
                    mFirestore.collection("Recommendation").document(currentTimestamp.toString())
                            .set(recommendationMap)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if(task.isSuccessful()){
                                        progressBar.setVisibility(View.INVISIBLE);
                                        toMain();
                                    }else{
                                        String error = task.getException().getMessage();
                                        Toast.makeText(NewRecommendationActivity.this, "FireStore Error: " + error, Toast.LENGTH_LONG).show();
                                        progressBar.setVisibility(View.INVISIBLE);
                                    }
                                }
                            }) ;
                }else{
                    String error = task.getException().getMessage();
                    Toast.makeText(NewRecommendationActivity.this, "FireStore Error:  " + error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    private void saveLinkRecommendation() {
        String websiteUrl = etLink.getText().toString();
        websiteUrl = MyUtil.addUrlProtocol(websiteUrl);
        if(TextUtils.isEmpty(websiteUrl) || (!URLUtil.isHttpUrl(websiteUrl) && !URLUtil.isHttpsUrl(websiteUrl))){
            Toast.makeText(this, "Empty website URL or invalid URL!", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> recommendationMap = getCommonRecommendationMap();
        recommendationMap.put("website_url", websiteUrl);
        Long currentTimestamp = System.currentTimeMillis() / 1000L;
        mFirestore.collection("Recommendation").document(currentTimestamp.toString())
                .set(recommendationMap)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            progressBar.setVisibility(View.INVISIBLE);
                            toMain();
                        }else{
                            String error = task.getException().getMessage();
                            Toast.makeText(NewRecommendationActivity.this, "FireStore Error: " + error, Toast.LENGTH_LONG).show();
                            progressBar.setVisibility(View.INVISIBLE);
                        }
                    }
                }) ;
    }


    private Map<String, Object> getLocationMap(){
        Map<String, Object> locationMap = new HashMap<>();
        locationMap.put("location_id", selectedLocation.getLocationId());
        locationMap.put("name", selectedLocation.getName());
        locationMap.put("address", selectedLocation.getAddress());
        locationMap.put("image_uri", downloadUrl);
        locationMap.put("longitude", selectedLocation.getLongitude());
        locationMap.put("latitude", selectedLocation.getLatitude());
        locationMap.put("url", selectedLocation.getUrl());
        locationMap.put("phone_number", selectedLocation.getPhoneNumber());
        return locationMap;
    }

    private Map<String, Object> getCommonRecommendationMap(){
        Map<String, Object> recommendationMap = new HashMap<>();
        recommendationMap.put("user_id", recommendation.getUserId());
        recommendationMap.put("category_id", recommendation.getCategoryId());
        recommendationMap.put("recommendation_type", recommendationType);
        recommendationMap.put("title", recommendation.getTitle());
        recommendationMap.put("image_url", downloadUrl);
        recommendationMap.put("description", recommendation.getDescription());
        recommendationMap.put("idea", recommendation.getIdea());
        recommendationMap.put("timestamp", FieldValue.serverTimestamp());
        return recommendationMap;
    }

    protected void pickLocation(){
        PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
        try {
            startActivityForResult(builder.build(NewRecommendationActivity.this), PLACE_PICKER_REQUEST);
        } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }
    }

    protected void updateRecommendationTypeUI(){
        if(recommendationType == Recommendation.TYPE_LINK){
            tvLblWebsiteURL.setVisibility(View.VISIBLE);
            etLink.setVisibility(View.VISIBLE);
            ivPin.setVisibility(View.GONE);
            tvLblLocation.setVisibility(View.GONE);
            tvLocationName.setVisibility(View.GONE);
        }else{
            tvLblWebsiteURL.setVisibility(View.GONE);
            etLink.setVisibility(View.GONE);
            ivPin.setVisibility(View.VISIBLE);
            tvLblLocation.setVisibility(View.VISIBLE);
            tvLocationName.setVisibility(View.VISIBLE);
        }
    }

    protected void loadInterestedCategory(){
        mFirestore.collection("Users").document(userId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.getResult().exists()){
                    DocumentSnapshot data = task.getResult();
                    interestedCategories = (List<String>) data.get("interested_categories");

                    if(interestedCategories == null){
                        Intent interestedCategoryIntent = new Intent(NewRecommendationActivity.this, InterestSelection.class);
                        startActivity(interestedCategoryIntent);
                        finish();
                    }

                    loadCategory();
                }
            }
        });
    }
    protected void loadCategory(){

        mFirestore.collection("categories").orderBy("name", Query.Direction.ASCENDING)
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()){
                    List<DocumentSnapshot> data = task.getResult().getDocuments();
                    ArrayList<Category> categoryArrayList = new ArrayList<>();
                    Category defaultEmptyCategory = new Category();
                    defaultEmptyCategory.setId("0");
                    defaultEmptyCategory.setName(getString(R.string.please_select_category));
                    categoryArrayList.add(defaultEmptyCategory);
                    for(DocumentSnapshot doc: data){
                        if(!interestedCategories.contains(doc.getId())) continue;
                        Category category = new Category();
                        category.setId(doc.getId());
                        category.setName(doc.get("name").toString());
                        category.setIcon(doc.get("icon").toString());
                        categoryArrayList.add(category);
                    }
                    //fill data in spinner
                    ArrayAdapter<Category> adapter = new ArrayAdapter<Category>(NewRecommendationActivity.this,
                            android.R.layout.simple_spinner_dropdown_item, categoryArrayList);
                    spCategory.setAdapter(adapter);
                    //spCategory.setSelection(adapter.getPosition(myItem));

                }else{
                    String error = task.getException().getMessage();
                    Toast.makeText(NewRecommendationActivity.this, "Error loading category: " + error, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void toMain(){
        Intent intent = new Intent(NewRecommendationActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

}
