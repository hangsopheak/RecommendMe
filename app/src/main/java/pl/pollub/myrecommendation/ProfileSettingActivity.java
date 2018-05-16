package pl.pollub.myrecommendation;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import pl.pollub.myrecommendation.models.User;
import pl.pollub.myrecommendation.utils.MyUtil;

public class ProfileSettingActivity extends AppCompatActivity implements View.OnClickListener, RadioGroup.OnCheckedChangeListener{

    private EditText etName;
    private RadioGroup rgSexGroup;
    private RadioButton rbMale, rbFemal;
    private CircleImageView ivProfilePicture;
    private Button btnSave;
    private Uri imageURI;
    private ProgressBar progressBar;


    private FirebaseFirestore mFirestore;
    private FirebaseAuth mAuth;
    private StorageReference mStorageReference;

    private boolean isProfilePictureChanged = false;
    private String userId;
    private String sex = "M";
    private List<String> interestedCategories = new ArrayList<>();
    private boolean isFromMain = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_setting);

        isFromMain = getIntent().getBooleanExtra("from_main", false);

        if(isFromMain){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        etName = findViewById(R.id.etName);
        rgSexGroup = findViewById(R.id.radioGroupSex);
        rbMale = findViewById(R.id.radioMale);
        rbFemal = findViewById(R.id.radioFemale);
        ivProfilePicture = findViewById(R.id.profileSettingImage);
        btnSave = findViewById(R.id.btnProfileSettingSave);
        progressBar = findViewById(R.id.progressBarProfileSetting);

        mFirestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        mStorageReference = FirebaseStorage.getInstance().getReference();

        userId = mAuth.getCurrentUser().getUid();

        ivProfilePicture.setOnClickListener(this);
        btnSave.setOnClickListener(this);
        rgSexGroup.setOnCheckedChangeListener(this);
        rgSexGroup.check(R.id.radioMale);

        // display account info
        loadProfile();


    }

    private void loadProfile(){
        progressBar.setVisibility(View.VISIBLE);
        mFirestore.collection("Users").document(userId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()){
                    if(task.getResult().exists()){
                        // Toast.makeText(SetupActivity.this, "Data exist", Toast.LENGTH_SHORT).show();
                        String name = task.getResult().getString("name");
                        String profilePicture = task.getResult().getString("profile_picture");
                        interestedCategories = (List<String>) task.getResult().get("interested_categories");
                        imageURI = Uri.parse(profilePicture);
                        etName.setText(name);
                        RequestOptions placeholderRequest = new RequestOptions();
                        placeholderRequest.placeholder(R.drawable.profile);
                        Glide.with(ProfileSettingActivity.this).setDefaultRequestOptions(placeholderRequest).load(imageURI).into(ivProfilePicture);
                    }else{
                        Toast.makeText(ProfileSettingActivity.this, "Data does not exist", Toast.LENGTH_SHORT).show();
                    }
                }else{
                    String error = task.getException().getMessage();
                    Toast.makeText(ProfileSettingActivity.this, "Storage Error: " + error, Toast.LENGTH_LONG).show();
                }
                progressBar.setVisibility(View.INVISIBLE);
            }
        });
    }


    private void bringPicturePicker() {
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setAspectRatio(1,1)
                .setMinCropResultSize(100,100)
                .setMaxCropResultSize(2500,2500)
                .start(ProfileSettingActivity.this);
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.profileSettingImage:
                pickImage();
                break;
            case R.id.btnProfileSettingSave:
                save();
                break;
        }
    }

    private void save() {
        final String name = etName.getText().toString();
        if(!TextUtils.isEmpty(name) && sex != null && imageURI != null){
            progressBar.setVisibility(View.VISIBLE);
            if(isProfilePictureChanged){
                byte[] thumbData = MyUtil.getCompressedImage(ProfileSettingActivity.this,
                        imageURI, 200, 200,30);

                UploadTask uploadTask = mStorageReference.child("profile_pictures").
                        child(userId + ".jpg").putBytes(thumbData);

                uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if(task.isSuccessful()){
                            storeFireStore(task, name);
                        }else{
                            String error = task.getException().getMessage();
                            Toast.makeText(ProfileSettingActivity.this, "Storage Error: " + error, Toast.LENGTH_LONG).show();
                        }
                        progressBar.setVisibility(View.INVISIBLE);
                    }
                });

            }else{
                storeFireStore(null, name);
                progressBar.setVisibility(View.INVISIBLE);
            }
        }else{
            Toast.makeText(this, "Please fill in all fields!", Toast.LENGTH_SHORT).show();
        }
    }

    private void storeFireStore(Task<UploadTask.TaskSnapshot> task, String userName) {
        Uri downloadUri = null;
        if(task == null){
            downloadUri = imageURI;
        }else{
            downloadUri = task.getResult().getDownloadUrl();
        }

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", userName);
        userMap.put("profile_picture", downloadUri.toString());
        userMap.put("sex", sex);
        userMap.put("interested_categories", interestedCategories);

        mFirestore.collection("Users").document(userId)
                .set(userMap)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            if(isFromMain){
                                toMain();
                            }else{
                                toInterestSelection();
                            }
                        }else{
                            String error = task.getException().getMessage();
                            Toast.makeText(ProfileSettingActivity.this, "FireStore Error: " + error, Toast.LENGTH_LONG).show();
                        }
                    }
                }) ;
    }


    private void pickImage() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(ContextCompat.checkSelfPermission(ProfileSettingActivity.this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(ProfileSettingActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);
            }else{
                bringPicturePicker();
            }
        }else{
            bringPicturePicker();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                isProfilePictureChanged = true;
                imageURI = result.getUri();
                ivProfilePicture.setImageURI(imageURI);
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
                Toast.makeText(this, "Error image picker: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    protected void toMain(){
        Intent mainIntent = new Intent(ProfileSettingActivity.this, MainActivity.class);
        startActivity(mainIntent);
        finish();
    }

    protected void toInterestSelection(){
        Intent interestIntent = new Intent(ProfileSettingActivity.this, InterestSelection.class);
        startActivity(interestIntent);
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);
    }

    private void updateUI(FirebaseUser currentUser) {
        if(currentUser == null){
            Intent loginIntent = new Intent(ProfileSettingActivity.this, LoginActivity.class);
            startActivity(loginIntent);
            finish();
        }
    }


    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int i) {
        rgSexGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch(checkedId){
                    case R.id.radioMale:
                        sex = User.SEX_MALE;
                        break;
                    case R.id.radioFemale:
                        sex = User.SEX_FEMALE;
                        break;
                }
            }
        });
    }
}
