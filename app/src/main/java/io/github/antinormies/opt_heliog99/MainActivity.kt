package io.github.antinormies.opt_heliog99

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import io.github.antinormies.opt_heliog99.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Example of a call to a native method
        binding.sampleText.text = stringFromJNI()
    }

    /**
     * A native method that is implemented by the 'opt_heliog99' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {
        // Used to load the 'opt_heliog99' library on application startup.
        init {
            System.loadLibrary("opt_heliog99")
        }
    }
}