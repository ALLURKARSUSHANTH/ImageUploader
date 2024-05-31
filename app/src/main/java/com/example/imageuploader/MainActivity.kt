package com.example.imageuploader

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.imageuploader.ui.theme.ImageUploaderTheme
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
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
    var selectedImageUri by remember { mutableStateOf<Uri?>(null)
    }
    val context = LocalContext.current

    val getContent = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        selectedImageUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .clip(shape = CircleShape),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (selectedImageUri != null) {
            val bitmap: Bitmap? =
                MediaStore.Images.Media.getBitmap(context.contentResolver, selectedImageUri)
            bitmap?.asImageBitmap()?.let { imageBitmap ->
                Image(
                    bitmap = imageBitmap,
                    contentDescription = null,
                    modifier = Modifier.size(200.dp)
                        .clip(shape = CircleShape)
                )
            }
        }
            else {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(200.dp)
                    .clip(shape = CircleShape )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            // Launch the image picker
            getContent.launch("image/*")
        }) {
            Text("Select Image")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedImageUri != null) {
            Button(onClick = {
                uploadImageToFirebase(selectedImageUri!!)
            }) {
                Text("Upload Image")
            }
        }
    }
}

private fun uploadImageToFirebase(imageUri: Uri) {
    val storageRef = Firebase.storage.reference
    val imagesRef = storageRef.child("images/${UUID.randomUUID()}")
    imagesRef.putFile(imageUri)
        .addOnSuccessListener {
            Log.d("Success: ","Image Uploaded Succesfully")
        }
        .addOnFailureListener {
            Log.e("Failure: ","Error while uploading Image")
        }
}

@Preview(showBackground = true)
@Composable
fun ImageUploadScreenPreview() {
    Surface {
        ImageUploadFunction()
    }
}