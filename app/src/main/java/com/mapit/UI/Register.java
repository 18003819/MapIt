package com.mapit.UI;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.mapit.R;

public class Register extends AppCompatActivity {
    //declaring components to be used
    Button signUp_Button, back_Button;
    Intent signup_Intent, login_Intent;
    EditText email_Textbox, password_Textbox;
    ProgressBar register_ProgressBar;

    //declaring firebase authentication
    private FirebaseAuth mAuth;

    //declaring Firebase Database
    FirebaseDatabase database = FirebaseDatabase.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        //setting components to their respected ID
        email_Textbox = findViewById(R.id.R_email_txb);
        password_Textbox = findViewById(R.id.R_password_txb);
        signUp_Button = findViewById(R.id.signUp_Btn);
        back_Button = findViewById(R.id.backToLogin_Btn);
        register_ProgressBar = findViewById(R.id.register_Pb);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        //declaring Intents to load other activities
        signup_Intent = new Intent(this, Login.class);
        login_Intent = new Intent(this, Login.class);

        //methods for button clicks
        signUp_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RegisterUser();
            }
        });

        back_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(login_Intent);
            }
        });

    }

    private void RegisterUser() {
        //method used to validate user input and register new user
        final String email = email_Textbox.getText().toString().trim();
        final String password = password_Textbox.getText().toString().trim();

        if (email.isEmpty()) {
            email_Textbox.setError("Email is Required");
            email_Textbox.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            email_Textbox.setError("Please Enter a Valid Email");
            email_Textbox.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            password_Textbox.setError("Password is Required");
            password_Textbox.requestFocus();
            return;
        }
        if (password.length() < 6) {
            password_Textbox.setError("Minimum length of password should be 6");
            password_Textbox.requestFocus();
            return;
        }

        register_ProgressBar.setVisibility(View.VISIBLE);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        register_ProgressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Toast.makeText(Register.this, "User Registered Successfully", Toast.LENGTH_LONG).show();

                            //adding to RealTime Database
                            DatabaseReference myRef = database.getReference("User");
                            myRef.child(EncodeString(email)).child("Email").setValue(email);
                            myRef.child(EncodeString(email)).child("Password").setValue(password);
                            myRef.child(EncodeString(email)).child("MeasurementUnit").setValue("Metric");
                            myRef.child(EncodeString(email)).child("TransportMode").setValue("Car");

                            startActivity(signup_Intent);
                        } else {
                            if (task.getException() instanceof FirebaseAuthUserCollisionException)
                                Toast.makeText(Register.this, "User with this email Already Exists", Toast.LENGTH_LONG).show();
                            else
                                Toast.makeText(Register.this, "An Error Occured. Please Try Again!", Toast.LENGTH_LONG).show();
                        }

                    }
                });

    }

    public static String EncodeString(String string) {
        return string.replace(".", "_");
    }
}

