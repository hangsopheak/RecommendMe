package pl.pollub.myrecommendation.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import pl.pollub.myrecommendation.R;
import pl.pollub.myrecommendation.models.Category;

public class CategoryRecyclerAdapter extends RecyclerView.Adapter<CategoryRecyclerAdapter.ViewHolder> {

    public List<Category> categoryList;
    private Context mContext;
    private FirebaseAuth mAuth;
    private FirebaseFirestore mFireStore;
    private int countSelectedCategory = 0;
    private Set<String> selectedCategories = new HashSet<>();
    private OnItemClickListener listener;

    public CategoryRecyclerAdapter(List<Category> categoryList){
        this.categoryList = categoryList;
    }

    @NonNull
    @Override
    public CategoryRecyclerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        mContext = parent.getContext();
        View view = LayoutInflater.from(mContext).inflate(R.layout.category_item, parent, false);
        return new ViewHolder(view);
    }

    // Define listener member variable
    public interface OnItemClickListener{
        //void onItemClick(View itemView, int position);
        void onIconClick(View itemView, int position);
    }

    // Define the method that allows the parent activity or fragment to define the listener
    public void setOnItemClickListener(OnItemClickListener listener){
        this.listener = listener;
    }


    @Override
    public void onBindViewHolder(@NonNull final CategoryRecyclerAdapter.ViewHolder holder, final int position) {
        holder.setIsRecyclable(false);
        String id = categoryList.get(position).getId();
        String name = categoryList.get(position).getName();
        String icon = categoryList.get(position).getIcon();
        final Boolean isSelected = categoryList.get(position).isSelected();

        holder.setName(name);
        holder.setIcon(icon,isSelected, id);

        mFireStore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

    }

    @Override
    public int getItemCount() {
        return this.categoryList.size();
    }

    public int getCountSelectedCategory(){
        return this.countSelectedCategory;
    }

    public void setCountSelectedCategory(int count){
        this.countSelectedCategory = count;
    }

    public Set<String> getSelectedCategories(){
        return this.selectedCategories;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private TextView tvName;
        private ImageView ivIcon;
        private View mView;

        public ViewHolder(final View itemView) {
            super(itemView);
            mView = itemView;
            ivIcon = mView.findViewById(R.id.ivCategoryIcon);

            ivIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(listener != null){
                        int position = getAdapterPosition();
                        if(position != RecyclerView.NO_POSITION){
                            listener.onIconClick(itemView, position);
                        }
                    }
                }
            });

        }

        public void setName(String name){
            tvName = mView.findViewById(R.id.tvInterestCategoryName);
            tvName.setText(name);
        }

        public void setIcon(String iconURI, boolean isSelected, String categoryId){
            RequestOptions placeholderOption = new RequestOptions().placeholder(R.drawable.ic_launcher_background);
            Glide.with(mContext).applyDefaultRequestOptions(placeholderOption)
                    .load(iconURI).into(ivIcon);

            Drawable circleDrawable = (Drawable) mContext.getDrawable(R.drawable.circle_image);
            Drawable selectedCircleDrawable = (Drawable) mContext.getDrawable(R.drawable.circle_image_selected);
            if(isSelected){
                countSelectedCategory++;
                selectedCategories.add(categoryId);
                ivIcon.setBackground(selectedCircleDrawable);
            }else{
                selectedCategories.remove(categoryId);
                ivIcon.setBackground(circleDrawable);
            }
        }
    }


}
