package com.roboagv

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.roboagv.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: RobotViewModel by viewModels()
    private lateinit var voice: VoiceController

    private var imageCapture: ImageCapture? = null
    private var mode = Mode.NAVIGATE

    enum class Mode { NAVIGATE, RECORD_ROOM }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.all { it.value }) startCamera()
        else Toast.makeText(this, "Camera and microphone permissions are required", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        voice = VoiceController(this)

        setupTabs()
        setupButtons()
        observeState()
        switchMode(Mode.NAVIGATE)
        checkPermissions()
    }

    // ── UI setup ─────────────────────────────────────────────────────────────

    private fun setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Navigate"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Record Room"))

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                switchMode(if (tab.position == 0) Mode.NAVIGATE else Mode.RECORD_ROOM)
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun setupButtons() {
        binding.btnVoice.setOnClickListener { startVoiceCommand() }
        binding.btnCapture.setOnClickListener { captureAndAddRoomImage() }
        binding.btnSaveRoom.setOnClickListener { viewModel.submitRoomRecording() }
        binding.btnSettings.setOnClickListener { showSettingsDialog() }
    }

    private fun switchMode(newMode: Mode) {
        mode = newMode
        viewModel.resetState()

        when (newMode) {
            Mode.NAVIGATE -> {
                binding.navigatePanel.visibility = View.VISIBLE
                binding.recordPanel.visibility = View.GONE
                binding.robotView.visibility = View.VISIBLE
            }
            Mode.RECORD_ROOM -> {
                binding.navigatePanel.visibility = View.GONE
                binding.recordPanel.visibility = View.VISIBLE
                binding.robotView.visibility = View.GONE
                binding.imageCountText.text = "0 photos"
            }
        }
    }

    // ── Voice command ─────────────────────────────────────────────────────────

    private fun startVoiceCommand() {
        setControlsEnabled(false)
        viewModel.setListening()

        voice.startListening(
            onResult = { command ->
                runOnUiThread {
                    setStatus("Command: \"$command\"\nCapturing frame...")
                    captureFrame { bitmap ->
                        viewModel.navigate(command, bitmap)
                    }
                }
            },
            onError = { error ->
                runOnUiThread {
                    setStatus("Voice error: $error")
                    setControlsEnabled(true)
                }
            }
        )
    }

    // ── Camera capture ────────────────────────────────────────────────────────

    private fun captureAndAddRoomImage() {
        captureFrame { bitmap ->
            viewModel.addRoomImage(bitmap)
        }
    }

    private fun captureFrame(onCapture: (Bitmap) -> Unit) {
        imageCapture?.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = image.toBitmap()
                    image.close()
                    onCapture(bitmap)
                }
                override fun onError(exc: ImageCaptureException) {
                    Log.e("Camera", "Capture failed: ${exc.message}")
                    Toast.makeText(this@MainActivity, "Camera capture failed", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // ── State observation ─────────────────────────────────────────────────────

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                when (state) {
                    is AppState.Idle -> {
                        setStatus("Ready")
                        setControlsEnabled(true)
                    }
                    is AppState.Listening -> {
                        setStatus("Listening... speak your command")
                        setControlsEnabled(false)
                    }
                    is AppState.Processing -> {
                        setStatus("Processing with AI...")
                        setControlsEnabled(false)
                    }
                    is AppState.Capturing -> {
                        binding.imageCountText.text = "${state.count} photos"
                        setStatus("${state.count} photo(s) captured. Capture more angles or Save Room.")
                        setControlsEnabled(true)
                    }
                    is AppState.NavigationResult -> {
                        val arrow = when (state.direction) {
                            "forward" -> "▲"
                            "back"    -> "▼"
                            "left"    -> "◀"
                            "right"   -> "▶"
                            else      -> "■"
                        }
                        setStatus("$arrow ${state.direction.uppercase()}\n\"${state.speechText}\"")
                        binding.robotView.setDirection(state.direction)
                        voice.speak(state.speechText)
                        setControlsEnabled(true)
                    }
                    is AppState.RoomRecorded -> {
                        setStatus("Room saved! ${state.count} scene(s) stored in AI memory.")
                        voice.speak("Room recorded. ${state.count} scenes stored in memory.")
                        setControlsEnabled(true)
                    }
                    is AppState.Error -> {
                        setStatus("Error: ${state.message}")
                        setControlsEnabled(true)
                        Toast.makeText(this@MainActivity, state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun setStatus(text: String) {
        binding.statusText.text = text
    }

    private fun setControlsEnabled(enabled: Boolean) {
        binding.btnVoice.isEnabled = enabled
        binding.btnCapture.isEnabled = enabled
        binding.btnSaveRoom.isEnabled = enabled
        if (enabled) {
            binding.btnVoice.text = "Hold to Speak"
        }
    }

    // ── Settings ──────────────────────────────────────────────────────────────

    private fun showSettingsDialog() {
        val editText = EditText(this).apply {
            hint = "http://192.168.1.100:8000"
            setText(ApiClient.baseUrl.trimEnd('/'))
            setPadding(40, 20, 40, 20)
        }

        AlertDialog.Builder(this)
            .setTitle("Backend Server URL")
            .setMessage("Enter the IP and port of your Python server:")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val url = editText.text.toString().trim()
                if (url.isNotEmpty()) {
                    ApiClient.setBaseUrl(url)
                    Toast.makeText(this, "Server URL updated", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── Camera ────────────────────────────────────────────────────────────────

    private fun checkPermissions() {
        val needed = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            .filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }

        if (needed.isEmpty()) startCamera()
        else permissionLauncher.launch(needed.toTypedArray())
    }

    private fun startCamera() {
        ProcessCameraProvider.getInstance(this).addListener({
            val provider = ProcessCameraProvider.getInstance(this).get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            try {
                provider.unbindAll()
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
            } catch (e: Exception) {
                Log.e("Camera", "Bind failed: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onDestroy() {
        super.onDestroy()
        voice.destroy()
    }
}
