package pl.pollub.myrecommendation;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pl.pollub.myrecommendation.adapters.CategoryRecyclerAdapter;
import pl.pollub.myrecommendation.models.Category;

public class InterestSelection extends AppCompatActivity implements View.OnClickListener {

    private FirebaseFirestore mFireStore;
    private FirebaseAuth mAuth;

    private RecyclerView categoryRecyclerView;
    private CategoryRecyclerAdapter categoryRecyclerAdapter;
    private Button btnDone;
    private ProgressBar progressBar;
    private TextView tvWelcome;

    private ArrayList<Category> categoryList;
    private String[] interestedCategoriesArray;
    private boolean isFromMain = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interest_selection);

        interestedCategoriesArray = getIntent().getStringArrayExtra("interested_categories");
        isFromMain = getIntent().getBooleanExtra("from_main", false);

        mAuth = FirebaseAuth.getInstance();
        mFireStore = FirebaseFirestore.getInstance();

        categoryList = new ArrayList<>();
        categoryRecyclerView = findViewById(R.id.recInterestCategory);
        categoryRecyclerAdapter = new CategoryRecyclerAdapter(categoryList);
        categoryRecyclerView.setLayoutManager(new GridLayoutManager(this, 4));
        categoryRecyclerView.setAdapter(categoryRecyclerAdapter);

        btnDone = findViewById(R.id.btnInterestDone);
        progressBar = findViewById(R.id.progressBarInterestSelection);
        tvWelcome = findViewById(R.id.tvWelcome);

        btnDone.setOnClickListener(this);

        String currentUserId = mAuth.getCurrentUser().getUid();
        mFireStore.collection("Users").document(currentUserId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.getResult().exists()){
                    String name = task.getResult().get("name").toString();
                    tvWelcome.setText("Welcome " + name + "!");
                }
            }
        });


        mFireStore.collection("categories").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                List<DocumentSnapshot> data = task.getResult().getDocuments();
                List<String> interestedCategoriesArrayList = Arrays.asList(interestedCategoriesArray);
                for(DocumentSnapshot doc: data){
                    Category category = new Category();
                    category.setId(doc.getId());
                    category.setName(doc.get("name").toString());
                    category.setIcon(doc.get("icon").toString());
                    if(isFromMain && interestedCategoriesArrayList.contains(doc.getId())){
                        category.setSelected(true);
                    }
                    categoryList.add(category);
                }

                categoryRecyclerAdapter.setCountSelectedCategory(0);
                categoryRecyclerAdapter.notifyDataSetChanged();
            }
        });

        categoryRecyclerAdapter.setOnItemClickListener(new CategoryRecyclerAdapter.OnItemClickListener() {
            @Override
            public void onIconClick(View itemView, int position) {
                categoryList.get(position).toggleSelection();
                categoryRecyclerAdapter.setCountSelectedCategory(0);
                categoryRecyclerAdapter.notifyDataSetChanged();
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser == null){
            toLogin();
        }
    }

    private void toLogin() {
        Intent loginIntent = new Intent(InterestSelection.this, LoginActivity.class);
        startActivity(loginIntent);
        finish();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnInterestDone:
                save();
                break;
        }
    }

    private void save() {
        if(categoryRecyclerAdapter.getCountSelectedCategory() >= 3){
            progressBar.setVisibility(View.VISIBLE);
            final String currentUserId = mAuth.getCurrentUser().getUid();
            mFireStore.collection("Users").document(currentUserId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {

                    Set<String> categorySet = categoryRecyclerAdapter.getSelectedCategories();
                    List<String> categoryList = new ArrayList<>();
                    categoryList.addAll(categorySet);

                    mFireStore.collection("Users").document(currentUserId).update("interested_categories", categoryList)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()){
                                toMain();
                            }else{
                                String error = task.getException().getMessage();
                                Toast.makeText(InterestSelection.this, "FireStore Error: " + error, Toast.LENGTH_LONG).show();
                            }
                            progressBar.setVisibility(View.INVISIBLE);
                        }
                    }) ;
                }
            });

        }else{
            Toast.makeText(this, "Please select at least 3 categories!", Toast.LENGTH_LONG).show();
        }
    }

    private void toMain() {
        Intent mainIntent = new Intent(InterestSelection.this, MainActivity.class);
        startActivity(mainIntent);
        finish();
    }
}
