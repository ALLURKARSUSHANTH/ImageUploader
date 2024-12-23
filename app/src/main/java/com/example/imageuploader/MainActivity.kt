package com.example.imageuploader

import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.imageuploader.ui.theme.ImageUploaderTheme
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.common.InputImage
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ImageUploaderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ImageUploadFunction()
                }
            }
        }
    }
}

@Composable
fun ImageUploadFunction() {
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var detectedFaces by remember { mutableStateOf<List<Rect>>(emptyList()) }
    val context = LocalContext.current

    val getContent = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        selectedImageUri = uri
        uri?.let {
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            selectedBitmap = bitmap // Cache the bitmap
        }
    }

    // Detect faces when the user clicks the "Detect Faces" button
    fun detectFaces() {
        selectedBitmap?.let { bitmap ->
            processFaceDetection(bitmap, { faces ->
                detectedFaces = faces.map { it.boundingBox }
                Log.d("Face Detection", "Detected ${faces.size} faces.")
            })
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Display the selected image
        selectedBitmap?.asImageBitmap()?.let { imageBitmap ->
            Box(
                modifier = Modifier
                    .size(300.dp) // Fix the image size
                    .clip(CircleShape) // Ensure the image stays circular
            ) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize() // Fill the Box but not the whole screen
                )

                // Draw rectangles around detected faces
                detectedFaces.forEach { faceRect ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = faceRect.left.dp, y = faceRect.top.dp)
                            .size(faceRect.width().dp, faceRect.height().dp)
                            .border(2.dp, Color.Red)
                    )
                }
            }
        } ?: run {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(200.dp).clip(CircleShape)
            )
        }

        // Display the number of faces detected
        Text(
            text = "Faces Detected: ${detectedFaces.size}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Button to select image
        Button(onClick = {
            // Launch the image picker
            getContent.launch("image/*")
        }) {
            Text("Select Image")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Button to detect faces in the selected image
        Button(onClick = {
            detectFaces() // Detect faces when clicked
        }) {
            Text("Detect Faces")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Button to upload image to Firebase
        if (selectedImageUri != null) {
            Button(onClick = {
                selectedImageUri?.let { uploadImageToFirebase(it) }
            }) {
                Text("Upload Image")
            }
        }
    }
}

// Function to process face detection
private fun processFaceDetection(bitmap: Bitmap, onFacesDetected: (List<Face>) -> Unit) {
    val image = InputImage.fromBitmap(bitmap, 0)

    // Get the default face detector
    val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
        .build()

    val detector = FaceDetection.getClient(options)

    // Process the image
    detector.process(image)
        .addOnSuccessListener { faces ->
            onFacesDetected(faces)
        }
        .addOnFailureListener { e ->
            Log.e("Face Detection", "Error detecting faces", e)
        }
}

private fun uploadImageToFirebase(imageUri: Uri) {
    val storageRef = Firebase.storage.reference
    val imagesRef = storageRef.child("images/${UUID.randomUUID()}")
    imagesRef.putFile(imageUri)
        .addOnSuccessListener {
            Log.d("Success:", "Image Uploaded Successfully")
        }
        .addOnFailureListener {
            Log.e("Failure:", "Error while uploading Image")
        }
}

@Preview(showBackground = true)
@Composable
fun ImageUploadScreenPreview() {
    Surface {
        ImageUploadFunction()
    }
}
