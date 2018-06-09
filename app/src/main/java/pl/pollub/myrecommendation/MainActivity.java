package pl.pollub.myrecommendation;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.aurelhubert.ahbottomnavigation.AHBottomNavigation;
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationAdapter;
import com.aurelhubert.ahbottomnavigation.notification.AHNotification;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import pl.pollub.myrecommendation.adapters.HomeCategoryRecyclerAdapter;
import pl.pollub.myrecommendation.adapters.RecommendationRecyclerAdapter;
import pl.pollub.myrecommendation.fragments.HomeFragment;
import pl.pollub.myrecommendation.fragments.NotificationFragment;
import pl.pollub.myrecommendation.fragments.ProfileFragment;
import pl.pollub.myrecommendation.fragments.SavedFragment;
import pl.pollub.myrecommendation.models.Category;
import pl.pollub.myrecommendation.models.User;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,
        HomeFragment.OnFragmentInteractionListener,
        NotificationFragment.OnFragmentInteractionListener,
        ProfileFragment.OnFragmentInteractionListener,
        SavedFragment.OnFragmentInteractionListener,
        AHBottomNavigation.OnTabSelectedListener
{
    public static final int FRAGMENT_TYPE_HOME = 1;
    public static final int FRAGMENT_TYPE_SAVED = 2;
    public static final int FRAGMENT_TYPE_PROFILE = 3;

    private DrawerLayout mDrawer;
    private ActionBarDrawerToggle mToggle;
    private NavigationView navigationView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore mFireStore;

    private TextView tvNavName, tvNavEmail;
    private CircleImageView ivNavProfilePicture;
    private FloatingActionButton btnNewRecommendation;
    private AHBottomNavigation bottomNavigation;
    private AHBottomNavigationAdapter bottomNavigationAdapter;
    private RecyclerView rvHomeCategory;
    private HomeCategoryRecyclerAdapter homeCategoryRecyclerAdapter;

    private User currentUser;
    private FirebaseUser firebaseCurrentUser;
    private String selectedCategoryId = "0";

    private HomeFragment homeFragment;
    private SavedFragment savedFragment;
    private NotificationFragment notificationFragment;
    private ProfileFragment profileFragment;
    private int fragmentType = FRAGMENT_TYPE_HOME;

    private List<Category> selectedCategoryList = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFireStore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
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
        rvHomeCategory = findViewById(R.id.rvHomeCategory);
        LinearLayoutManager linearLayoutManager
                = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        homeCategoryRecyclerAdapter = new HomeCategoryRecyclerAdapter(selectedCategoryList);
        homeCategoryRecyclerAdapter.setSelectedCategoryId(selectedCategoryId);
        rvHomeCategory.setLayoutManager(linearLayoutManager);
        rvHomeCategory.setAdapter(homeCategoryRecyclerAdapter);

        btnNewRecommendation.setOnClickListener(this);

        bottomNavigation = (AHBottomNavigation) findViewById(R.id.bottom_navigation);
        bottomNavigationAdapter = new AHBottomNavigationAdapter(this, R.menu.bottom_menu);
        bottomNavigationAdapter.setupWithBottomNavigation(bottomNavigation);
        bottomNavigation.setColoredModeColors(Color.parseColor("#F63D2B"), Color.parseColor("#747474"));
        bottomNavigation.setSelected(true);
        bottomNavigation.setTitleState(AHBottomNavigation.TitleState.ALWAYS_SHOW);
        bottomNavigation.setTranslucentNavigationEnabled(true);
        bottomNavigation.setOnTabSelectedListener(this);

        mDrawer = (DrawerLayout) findViewById(R.id.drawer);
        mToggle = new ActionBarDrawerToggle(this, mDrawer, R.string.drawer_open, R.string.drawer_close);
        mDrawer.addDrawerListener(mToggle);
        navigationView.bringToFront();
        mToggle.syncState();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        homeFragment = new HomeFragment();
        savedFragment = new SavedFragment();
        notificationFragment = new NotificationFragment();
        profileFragment = new ProfileFragment(firebaseCurrentUser.getUid());

        currentUser = new User();
        currentUser.setId(firebaseCurrentUser.getUid());
        currentUser.setEmail(firebaseCurrentUser.getEmail());
        loadUserInfo();

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                setNavigationContent(item);
                return true;
            }
        });

        homeCategoryRecyclerAdapter.setOnItemClickListener(new HomeCategoryRecyclerAdapter.OnItemClickListener() {
            @Override
            public void onClick(View itemView, int position) {
                selectedCategoryId = selectedCategoryList.get(position).getId();
                homeCategoryRecyclerAdapter.setSelectedCategoryId(selectedCategoryId);
                homeCategoryRecyclerAdapter.notifyDataSetChanged();
                if(fragmentType == FRAGMENT_TYPE_HOME) {
                    getSupportFragmentManager().beginTransaction().detach(homeFragment).attach(homeFragment).commit();
                } else if(fragmentType == FRAGMENT_TYPE_PROFILE) {
                    getSupportFragmentManager().beginTransaction().detach(profileFragment).attach(profileFragment).commit();
                }else{
                    getSupportFragmentManager().beginTransaction().detach(savedFragment).attach(savedFragment).commit();
                }


            }
        });

        replaceFragment(homeFragment);

    }
    public String getSelectedCategoryId(){
        return this.selectedCategoryId;
    }

    public String getCurrentUserId(){
        return this.currentUser.getId();
    }

    public int getFragmentType(){
        return this.fragmentType;
    }

    private void loadUserInfo() {
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
                    setNotification();

                    mFireStore.collection("categories")
                            .orderBy("name", Query.Direction.ASCENDING).get()
                            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            List<DocumentSnapshot> data = task.getResult().getDocuments();
                            Category allCategory = new Category();
                            allCategory.setId("0");
                            allCategory.setName("All");
                            selectedCategoryList.add(allCategory);
                            for(DocumentSnapshot doc: data){
                                if(!currentUser.getInterestedCategories().contains(doc.getId())) continue;
                                Category category = new Category();
                                category.setId(doc.getId());
                                category.setName(doc.get("name").toString());
                                category.setIcon(doc.get("icon").toString());

                                selectedCategoryList.add(category);
                            }
                            homeCategoryRecyclerAdapter.notifyDataSetChanged();
                        }
                    });
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
        if(currentUser.getInterestedCategories() == null) return;
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
        if(currentUser.getInterestedCategories() == null) return;
        Intent intent = new Intent(MainActivity.this, NewRecommendationActivity.class);
        String[] data = new String[currentUser.getInterestedCategories().size()];
        data= currentUser.getInterestedCategories().toArray(data);
        intent.putExtra("interested_categories", data);
        startActivity(intent);
    }

    private void replaceFragment(Fragment fragment){
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.main_container, fragment);
        fragmentTransaction.commit();
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }


    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    public void setNotification(){
        mFireStore.collection("Users/" + currentUser.getId() +"/notification")
                .whereEqualTo("unseen", true).addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
                if(documentSnapshots != null){
                    String notificationNumber = null;
                    if(documentSnapshots.size() > 0) notificationNumber = Integer.toString(documentSnapshots.size());
                    AHNotification notification = new AHNotification.Builder()
                            .setText(notificationNumber)
                            .setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.colorRed_A400))
                            .setTextColor(ContextCompat.getColor(MainActivity.this, R.color.colorWhite))
                            .build();
                    bottomNavigation.setNotification(notification, 2);
                }
            }
        });
    }

    @Override
    public boolean onTabSelected(int position, boolean wasSelected) {
        switch (position){
            case 0:
                this.fragmentType = FRAGMENT_TYPE_HOME;
                replaceFragment(homeFragment);
                break;
            case 1:
                this.fragmentType = FRAGMENT_TYPE_SAVED;
                replaceFragment(savedFragment);
                break;
            case 2:
                replaceFragment(notificationFragment);
                break;
            case 3:
                this.fragmentType = FRAGMENT_TYPE_PROFILE;
                replaceFragment(profileFragment);
                break;
        }
        return true;

    }
}
