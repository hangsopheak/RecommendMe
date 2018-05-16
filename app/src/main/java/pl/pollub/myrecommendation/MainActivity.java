package pl.pollub.myrecommendation;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import pl.pollub.myrecommendation.models.User;
import pl.pollub.myrecommendation.models.WebContent;
import pl.pollub.myrecommendation.utils.MyUtil;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private DrawerLayout mDrawer;
    private ActionBarDrawerToggle mToggle;
    private NavigationView navigationView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore mFireStore;

    private TextView tvNavName, tvNavEmail;
    private CircleImageView ivNavProfilePicture;
    private FloatingActionButton btnNewRecommendation;

    private User currentUser;
    private FirebaseUser firebaseCurrentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        mFireStore = FirebaseFirestore.getInstance();
        firebaseCurrentUser = mAuth.getCurrentUser();
        if(firebaseCurrentUser == null){
            sendToLogin();
            return;
        }

        navigationView = findViewById(R.id.navigationView);
        tvNavName = navigationView.getHeaderView(0).findViewById(R.id.tvNavUserName);
        tvNavEmail = navigationView.getHeaderView(0).findViewById(R.id.tvNavEmail);
        ivNavProfilePicture = navigationView.getHeaderView(0).findViewById(R.id.ivNavProfilePicture);
        btnNewRecommendation = findViewById(R.id.btnNewRecommendation);

        btnNewRecommendation.setOnClickListener(this);

        mDrawer = (DrawerLayout) findViewById(R.id.drawer);
        mToggle = new ActionBarDrawerToggle(this, mDrawer, R.string.drawer_open, R.string.drawer_close);
        mDrawer.addDrawerListener(mToggle);
        navigationView.bringToFront();
        mToggle.syncState();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                setNavigationContent(item);
                return true;
            }
        });

        currentUser = new User();
        currentUser.setId(firebaseCurrentUser.getUid());
        currentUser.setEmail(firebaseCurrentUser.getEmail());
        mFireStore.collection("Users").document(currentUser.getId()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.getResult().exists()){
                    DocumentSnapshot data = task.getResult();
                    currentUser.setName(data.get("name").toString());
                    currentUser.setProfilePicture(data.get("profile_picture").toString());
                    currentUser.setSex(data.get("sex").toString());
                    currentUser.setInterestedCategories((List<String>) data.get("interested_categories"));

                    if(currentUser.getInterestedCategories() == null){
                        Intent interestedCategoryIntent = new Intent(MainActivity.this, InterestSelection.class);
                        startActivity(interestedCategoryIntent);
                        finish();
                    }
                    bindNavHeader();
                }
            }
        });
    }

    private void bindNavHeader(){
        tvNavName.setText(currentUser.getName());
        tvNavEmail.setText(currentUser.getEmail());
        RequestOptions placeholderOption = new RequestOptions().placeholder(R.drawable.ic_launcher_background);
        Glide.with(MainActivity.this).applyDefaultRequestOptions(placeholderOption)
                .load(currentUser.getProfilePicture()).into(ivNavProfilePicture);

    }

    private void setNavigationContent(MenuItem item) {
        int id = item.getItemId();
        switch (id){
            case R.id.left_logout:
                logout();
                break;
            case R.id.left_profile_setting:
                sendToProfileSetting();
                break;
            case R.id.left_interest:
                sendToInterestSelection();
                break;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateUI();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(mToggle.onOptionsItemSelected(item)){
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void updateUI(){
        if(firebaseCurrentUser == null) {
            sendToLogin();
        }else{
            String userId = firebaseCurrentUser.getUid();
            mFireStore.collection("Users").document(userId)
                    .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if(!task.getResult().exists()){
                        Intent profileSettingIntent = new Intent(MainActivity.this,
                                ProfileSettingActivity.class);
                        startActivity(profileSettingIntent);
                        finish();
                    }
                }
            });
        }
    }

    private void sendToLogin(){
        Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(loginIntent);
        finish();
    }

    private void logout() {
        mAuth.signOut();
        sendToLogin();
    }

    private void sendToProfileSetting(){
        Intent profileSettingIntent = new Intent(MainActivity.this, ProfileSettingActivity.class);
        profileSettingIntent.putExtra("from_main", true);
        startActivity(profileSettingIntent);
    }

    private void sendToInterestSelection(){
        Intent interestSeletionIntent = new Intent(MainActivity.this, InterestSelection.class);
        interestSeletionIntent.putExtra("from_main", true);
        String[] data = new String[currentUser.getInterestedCategories().size()];
        data= currentUser.getInterestedCategories().toArray(data);
        interestSeletionIntent.putExtra("interested_categories", data);
        startActivity(interestSeletionIntent);
    }


    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.btnNewRecommendation:
                toNewRecommendation();
                break;
        }
    }

    private void toNewRecommendation() {
        Intent intent = new Intent(MainActivity.this, NewRecommendationActivity.class);
        startActivity(intent);
    }
}
