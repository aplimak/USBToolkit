package ir.aeliux.usbtoolkit;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.Arrays;

import ir.aeliux.usbtoolkit.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding bindings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        bindings = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(bindings.getRoot());
        setSupportActionBar(bindings.topAppBar);
        int[] paddings = {
                bindings.main.getPaddingLeft(),
                bindings.main.getPaddingTop(),
                bindings.main.getPaddingRight(),
                bindings.main.getPaddingBottom()
        };
        ViewCompat.setOnApplyWindowInsetsListener(bindings.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    paddings[0] + systemBars.left,
                    paddings[1] + systemBars.top,
                    paddings[2] + systemBars.right,
                    paddings[3] + systemBars.bottom
            );
            return insets;
        });
    }
}