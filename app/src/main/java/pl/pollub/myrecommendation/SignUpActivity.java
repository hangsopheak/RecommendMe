package pl.pollub.myrecommendation;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class SignUpActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText etEmail, etPassword, etPasswordConfirmation;
    private ProgressBar progressBarSignUp;
    private Button btnSignUp, btnToLogin;

    private FirebaseAuth mAuth;
    private FirebaseFirestore mFireStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // bind view
        etEmail = findViewById(R.id.etSignUpEmail);
        etPassword = findViewById(R.id.etSignUpPassword);
        etPasswordConfirmation = findViewById(R.id.etSignUpConfirmPassword);
        progressBarSignUp = findViewById(R.id.progressBarSignUp);

        btnSignUp = findViewById(R.id.btnSignUp);
        btnToLogin = findViewById(R.id.btnToLogin);

        btnSignUp.setOnClickListener(this);
        btnToLogin.setOnClickListener(this);

        mAuth = FirebaseAuth.getInstance();
        mFireStore = FirebaseFirestore.getInstance();

    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.btnSignUp:
                register();
                break;
            case R.id.btnToLogin:
                toLogin();
                break;
        }
    }

    public void toLogin(){
        Intent loginIntent = new Intent(SignUpActivity.this, LoginActivity.class);
        startActivity(loginIntent);
    }

    public void toProfileSetting(){
        Intent profileSettingIntent = new Intent(SignUpActivity.this, ProfileSettingActivity.class);
        startActivity(profileSettingIntent);
        finish();
    }

    public void register(){

        String email = etEmail.getText().toString();
        String password = etPassword.getText().toString();
        String confirmPassword = etPasswordConfirmation.getText().toString();

        if(!TextUtils.isEmpty(email) && !TextUtils.isEmpty(password) && !TextUtils.isEmpty(confirmPassword)){
            if(password.equals(confirmPassword)){
                progressBarSignUp.setVisibility(View.VISIBLE);
                mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){
                            Toast.makeText(SignUpActivity.this, "Sign Up Successfully!", Toast.LENGTH_SHORT).show();
                            toProfileSetting();
                        }else{
                            String errorMessage = task.getException().getMessage();
                            Toast.makeText(SignUpActivity.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                        progressBarSignUp.setVisibility(View.INVISIBLE);

                    }
                });
            }else{
                Toast.makeText(SignUpActivity.this, "Password does not match", Toast.LENGTH_LONG).show();
            }

        }else{
            Toast.makeText(this, "Please enter all required fields!", Toast.LENGTH_SHORT).show();
        }
    }
}
