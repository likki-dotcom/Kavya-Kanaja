package com.example.kavyakanaja1

import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar

// 1. DATA MODELS (Updated with Vocab)
data class VocabWord(
    val word: String,
    val definition: String
)

data class Poem(
    val id: Int,
    val title: String,
    val poet: String,
    val poet_bio: String,
    val content: String,
    val meaning: String,
    val audio_res: String,
    val vocab: List<VocabWord> // Now handles the dynamic word meanings
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val allPoems = try {
            loadPoemsFromJson()
        } catch (exception: Exception) {
            emptyList()
        }
        setContent {
            val navController = rememberNavController()
            KavyaAppNavigation(navController, allPoems)
        }
    }

    private fun loadPoemsFromJson(): List<Poem> {
        val jsonString = resources.openRawResource(R.raw.poems).bufferedReader().use { it.readText() }
        val listType = object : TypeToken<List<Poem>>() {}.type
        return Gson().fromJson(jsonString, listType)
    }
}

@Composable
fun KavyaAppNavigation(navController: NavHostController, poems: List<Poem>) {
    var selectedPoem by remember { mutableStateOf<Poem?>(null) }

    NavHost(navController = navController, startDestination = "daily") {
        composable("daily") {
            val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
            val dailyPoem = if (poems.isNotEmpty()) poems[dayOfYear % poems.size] else null
            dailyPoem?.let {
                PoemDetailScreen(it, titleLabel = "POEM OF THE DAY", onOpenLibrary = { navController.navigate("library") })
            }
        }
        composable("library") {
            LibraryScreen(poems, onPoemClick = { poem ->
                selectedPoem = poem
                navController.navigate("detail")
            })
        }
        composable("detail") {
            selectedPoem?.let {
                PoemDetailScreen(it, titleLabel = "POEM DETAILS", onOpenLibrary = { navController.popBackStack() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(poems: List<Poem>, onPoemClick: (Poem) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kavya Library") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1B5E20),
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(poems) { poem ->
                ListItem(
                    headlineContent = { Text(poem.title, fontWeight = FontWeight.Bold) },
                    supportingContent = { Text(poem.poet) },
                    modifier = Modifier.clickable { onPoemClick(poem) }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun PoemDetailScreen(poem: Poem, titleLabel: String, onOpenLibrary: () -> Unit) {
    val context = LocalContext.current
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var showBioDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    if (showBioDialog) {
        AlertDialog(
            onDismissRequest = { showBioDialog = false },
            title = { Text(text = "Poet's Corner: ${poem.poet}", fontWeight = FontWeight.Bold) },
            text = { Text(text = poem.poet_bio) },
            confirmButton = {
                TextButton(onClick = { showBioDialog = false }) {
                    Text("Close", color = Color(0xFF1B5E20))
                }
            }
        )
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFFDF5E6)) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = titleLabel, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1B5E20))
                IconButton(onClick = {
                    mediaPlayer?.let { if (it.isPlaying) it.stop(); it.release() }
                    onOpenLibrary()
                }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.List, contentDescription = "Library", tint = Color(0xFF1B5E20))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(text = poem.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3E2723))
                    Text(
                        text = "Poet: ${poem.poet} (View Bio ⓘ)",
                        fontSize = 14.sp,
                        color = Color(0xFF1B5E20),
                        modifier = Modifier.clickable { showBioDialog = true }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                    Text(text = poem.content, fontSize = 18.sp, lineHeight = 28.sp, modifier = Modifier.padding(vertical = 8.dp))

                    Spacer(modifier = Modifier.height(20.dp))

                    // 2. DYNAMIC VOCABULARY SECTION
                    Text(text = "Vocabulary (Tap for meaning):", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState)
                            .padding(vertical = 8.dp)
                    ) {
                        poem.vocab.forEach { item ->
                            SuggestionChip(
                                onClick = { Toast.makeText(context, "${item.word}: ${item.definition}", Toast.LENGTH_SHORT).show() },
                                label = { Text(item.word) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Bhavartha (Explanation):", fontWeight = FontWeight.Bold)
                    Text(text = poem.meaning, fontSize = 14.sp, color = Color.DarkGray)

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            val resId = context.resources.getIdentifier(poem.audio_res, "raw", context.packageName)
                            if (resId != 0) {
                                mediaPlayer?.let { if (it.isPlaying) it.stop(); it.release() }
                                mediaPlayer = MediaPlayer.create(context, resId)
                                mediaPlayer?.start()
                                mediaPlayer?.setOnCompletionListener { it.release(); mediaPlayer = null }
                            } else {
                                mediaPlayer?.let { if (it.isPlaying) it.stop(); it.release() }
                                mediaPlayer = null
                                Toast.makeText(context, "Audio recitation coming soon!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
                    ) {
                        Text("Listen & Learn", color = Color.White)
                    }
                }
            }
        }
    }
}