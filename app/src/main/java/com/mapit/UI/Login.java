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
import com.mapit.R;

public class Login extends AppCompatActivity
{
    FirebaseAuth mAuth;

    EditText email_Textbox, password_Textbox;
    Button login_Button, register_Button;// ForceLogin;
    ProgressBar login_Progressbar;
    Intent login_Intent, register_Intent;

    public static String G_email;



    @Override
    protected void onCreate(Bundle savedInstanceState)
    {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mAuth = FirebaseAuth.getInstance();

        email_Textbox = findViewById(R.id.L_email_txb);
        password_Textbox = findViewById(R.id.L_password_txb);
        login_Progressbar = findViewById(R.id.login_Pb);
        //ForceLogin = findViewById(R.id.forceLogin);

        login_Button = findViewById(R.id.login_Btn);
        login_Intent = new Intent(this, MainActivity.class);
        login_Button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                UserLogin();
            }
        });

        register_Button = findViewById(R.id.register_Btn);
        register_Intent = new Intent(this, Register.class);
        register_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { startActivity(register_Intent);
            }
        });

/*
       ForceLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                email_Textbox.setText("mattsean.talbot@gmail.com");
                password_Textbox.setText("gateway5");
            }
        });
*/

    }

    private void UserLogin(){
        //Simplified Coding

        final String email = email_Textbox.getText().toString().trim();
        String password = password_Textbox.getText().toString().trim();

        if(email.isEmpty())
        {
            email_Textbox.setError("Email is Required");
            email_Textbox.requestFocus();
            return;
        }

        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches())
        {
            email_Textbox.setError("Please Enter a Valid Email");
            email_Textbox.requestFocus();
            return;
        }

        if(password.isEmpty())
        {
            password_Textbox.setError("Password is Required");
            password_Textbox.requestFocus();
            return;
        }
        if(password.length() < 6){
            password_Textbox.setError("Minimum length of password should be 6");
            password_Textbox.requestFocus();
            return;
        }

        login_Progressbar.setVisibility(View.VISIBLE);

        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {

                if(task.isSuccessful()){
                    login_Progressbar.setVisibility(View.GONE);
                    login_Intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    G_email = email;
                    startActivity(login_Intent);
                }else{
                    login_Progressbar.setVisibility(View.GONE);
                    Toast.makeText(Login.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

    }
}
