package comp5216.sydney.edu.au.project_flume;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ResetPasswordActivity extends AppCompatActivity {

    EditText email;
    Button resetPassword_Btn;
    FirebaseAuth firebaseAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        email = findViewById(R.id.email_reset);
        resetPassword_Btn = findViewById(R.id.resetPasswordBtnreset);
        firebaseAuth = FirebaseAuth.getInstance();
        resetPassword_Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email_str = email.getText().toString();
                if(email_str.equals("")){
                    Toast.makeText(ResetPasswordActivity.this, "Email Address cannot be" +
                                    " blank",
                            Toast.LENGTH_SHORT).show();
                }else{
                    firebaseAuth.sendPasswordResetEmail(email_str).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()){
                                Toast.makeText(ResetPasswordActivity.this, "Email sent! Check your email",
                                        Toast.LENGTH_SHORT).show();
                                startActivity( new Intent(ResetPasswordActivity.this, MainActivity.class));
                            }else{
                                Toast.makeText(ResetPasswordActivity.this, "Email sent! Check your email",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });

    }
}
