package pl.pollub.myrecommendation.fragments;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;

import pl.pollub.myrecommendation.InterestSelection;
import pl.pollub.myrecommendation.MainActivity;
import pl.pollub.myrecommendation.R;
import pl.pollub.myrecommendation.models.Category;

/**
 * A simple {@link Fragment} subclass.
 */
public class ProfileFragment extends Fragment {

    private TextView tvName, tvEmail, tvSex;
    private ImageView ivProfilePicture;

    private String profileUserId;
    private FirebaseFirestore mFireStore;
    private FirebaseAuth mAuth;
    private FirebaseUser firebaseUser;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @SuppressLint("ValidFragment")
    public ProfileFragment(String userId){
        this.profileUserId = userId;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view =  inflater.inflate(R.layout.fragment_profile, container, false);
        mFireStore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        firebaseUser = mAuth.getCurrentUser();

        loadUserInfo();
        getCurrentUserRecommendation();
        tvName = view.findViewById(R.id.tvProfileName);
        tvEmail = view.findViewById(R.id.tvProfileEmail);
        tvSex = view.findViewById(R.id.tvProfileSex);
        ivProfilePicture = view.findViewById(R.id.ivProfilePicture);

        // Inflate the layout for this fragment
        return view;
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    private void getCurrentUserRecommendation(){
        Fragment childFragment = new HomeFragment(this.profileUserId);
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.replace(R.id.constraintProfileRecommendation, childFragment).commit();
    }

    private void loadUserInfo() {
        mFireStore.collection("Users").document(this.profileUserId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.getResult().exists()){
                    DocumentSnapshot data = task.getResult();
                    tvName.setText(data.get("name").toString());
                    tvEmail.setText(firebaseUser.getEmail().toString());
                    String sex = "Male";
                    if(data.get("sex").equals("F")) sex = "Female";
                    tvSex.setText(sex);
                    RequestOptions placeholderOption = new RequestOptions().placeholder(R.drawable.ic_launcher_background);
                    if(getActivity() != null){
                        Glide.with(getActivity()).applyDefaultRequestOptions(placeholderOption)
                                .load(Uri.parse(data.get("profile_picture").toString())).into(ivProfilePicture);

                    }

                }
            }
        });
    }


}
