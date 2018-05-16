package pl.pollub.myrecommendation.fragments;


import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import pl.pollub.myrecommendation.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class NewRecommendationFragment extends Fragment {


    public NewRecommendationFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_new_recommendation, container, false);
    }

}
