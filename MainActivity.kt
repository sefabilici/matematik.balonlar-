package com.example.matematikbalonlari

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.delay
import kotlin.random.Random

// --- VERİ SINIFLARI ---
data class MathQuestion(val text: String, val answer: Int)

data class Balloon(
    val id: Long = System.currentTimeMillis() + Random.nextLong(),
    val question: MathQuestion,
    val color: Color,
    val xPosition: Float,
    val yPosition: Float = -0.2f
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AppNavigation()
            }
        }
    }
}

// --- NAVİGASYON (Süre bilgisini taşıyacak şekilde güncellendi) ---
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "menu") {
        composable("menu") { MainMenuScreen(navController) }

        // "game" rotasına "minutes" adında bir parametre ekledik
        composable(
            route = "game/{minutes}",
            arguments = listOf(navArgument("minutes") { type = NavType.IntType })
        ) { backStackEntry ->
            // Gelen dakikayı alıyoruz (Varsayılan 3 dk)
            val minutes = backStackEntry.arguments?.getInt("minutes") ?: 3
            GameScreen(navController, minutes)
        }
    }
}

// --- ANA MENÜ (Zorluk Seviyeleri Eklendi) ---
@Composable
fun MainMenuScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3E5F5)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Matematik\nBalonları",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF6200EE),
            lineHeight = 45.sp,
            modifier = Modifier.padding(bottom = 40.dp)
        )

        // Zorluk Butonları
        DifficultyButton("Kolay (3 dk)", Color(0xFF4CAF50)) { navController.navigate("game/3") }
        Spacer(modifier = Modifier.height(16.dp))

        DifficultyButton("Orta (5 dk)", Color(0xFFFF9800)) { navController.navigate("game/5") }
        Spacer(modifier = Modifier.height(16.dp))

        DifficultyButton("Zor (8 dk)", Color(0xFFF44336)) { navController.navigate("game/8") }
    }
}

@Composable
fun DifficultyButton(text: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(50),
        modifier = Modifier
            .fillMaxWidth(0.7f)
            .height(60.dp)
    ) {
        Text(text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

// --- OYUN EKRANI (Zamanlayıcı ve Durdurma Eklendi) ---
@Composable
fun GameScreen(navController: NavController, startMinutes: Int) {
    var score by remember { mutableStateOf(0) }
    var lives by remember { mutableStateOf(3) }
    var balloons by remember { mutableStateOf(listOf<Balloon>()) }
    var userInput by remember { mutableStateOf("") }

    // Oyun Durumu
    var isGameOver by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) } // Duraklatma durumu

    // Zamanlayıcı (Saniye cinsinden)
    var timeRemaining by remember { mutableStateOf(startMinutes * 60) }

    // 1. OYUN FİZİĞİ DÖNGÜSÜ
    LaunchedEffect(isGameOver, isPaused) {
        if (!isGameOver && !isPaused) {
            while (true) {
                delay(50) // Oyun hızı

                // Balonları hareket ettir
                val newBalloons = balloons.map { it.copy(yPosition = it.yPosition + 0.005f) }

                // Kaçanları kontrol et
                val missed = newBalloons.filter { it.yPosition > 1.1f }
                val survived = newBalloons.filter { it.yPosition <= 1.1f }

                if (missed.isNotEmpty()) {
                    lives -= missed.size
                    if (lives <= 0) isGameOver = true
                }

                balloons = survived

                // Yeni balon üret
                if (Random.nextInt(100) < 3) {
                    balloons = balloons + generateRandomBalloon()
                }
            }
        }
    }

    // 2. ZAMANLAYICI DÖNGÜSÜ
    LaunchedEffect(isGameOver, isPaused) {
        if (!isGameOver && !isPaused) {
            while (timeRemaining > 0) {
                delay(1000L) // 1 saniye bekle
                timeRemaining--
            }
            if (timeRemaining == 0) {
                isGameOver = true // Süre biterse oyun biter
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // Balonları Çiz
        balloons.forEach { BalloonView(it) }

        // --- ÜST BİLGİ PANELİ ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(12.dp))
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Can
            Text("❤️ $lives", fontSize = 20.sp, color = Color.Red, fontWeight = FontWeight.Bold)

            // Süre (Dakika:Saniye formatı)
            val minutes = timeRemaining / 60
            val seconds = timeRemaining % 60
            Text(
                text = String.format("%02d:%02d", minutes, seconds),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = if (timeRemaining < 30) Color.Red else Color.Black
            )

            // Durdurma Butonu
            IconButton(onClick = { isPaused = true }) {
                Icon(Icons.Default.Pause, contentDescription = "Durdur", tint = Color(0xFF6200EE), modifier = Modifier.size(32.dp))
            }

            // Puan
            Text("$score Puan", fontSize = 20.sp, color = Color(0xFF6200EE), fontWeight = FontWeight.Bold)
        }

        // --- ALT KISIM (Klavye) ---
        Column(modifier = Modifier.align(Alignment.BottomCenter)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(Color(0xFFEEEEEE), RoundedCornerShape(16.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (userInput.isEmpty()) "?" else userInput,
                    fontSize = 32.sp,
                    color = Color(0xFF6200EE),
                    fontWeight = FontWeight.Bold
                )
            }

            NumberPad(
                onNumberClick = { userInput += it },
                onDelete = { if (userInput.isNotEmpty()) userInput = userInput.dropLast(1) },
                onEnter = {
                    val answer = userInput.toIntOrNull()
                    if (answer != null) {
                        val hit = balloons.find { it.question.answer == answer }
                        if (hit != null) {
                            score += 10
                            balloons = balloons - hit
                            userInput = ""
                        } else {
                            userInput = ""
                        }
                    }
                }
            )
        }

        // --- DURAKLATMA MENÜSÜ (Overlay) ---
        if (isPaused && !isGameOver) {
            AlertDialog(
                onDismissRequest = { /* Kapatılamaz, butona basılmalı */ },
                title = { Text("Oyun Durduruldu") },
                text = { Text("Biraz nefes al!") },
                confirmButton = {
                    Button(onClick = { isPaused = false }) {
                        Text("Devam Et")
                    }
                },
                dismissButton = {
                    Button(onClick = { navController.popBackStack() }) {
                        Text("Menüye Dön")
                    }
                }
            )
        }

        // --- OYUN BİTTİ EKRANI ---
        if (isGameOver) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text(if (lives <= 0) "Canın Bitti!" else "Süre Doldu!") },
                text = {
                    Column {
                        Text("Toplam Puanın: $score")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(if (lives <= 0) "Tüm balonları kaçırdın." else "Zaman bitti.")
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        // Oyunu Sıfırla
                        score = 0
                        lives = 3
                        balloons = emptyList()
                        userInput = ""
                        timeRemaining = startMinutes * 60 // Süreyi tekrar başlat
                        isGameOver = false
                        isPaused = false
                    }) {
                        Text("Tekrar Oyna")
                    }
                },
                dismissButton = {
                    Button(onClick = { navController.popBackStack() }) {
                        Text("Menüye Dön")
                    }
                }
            )
        }
    }
}

@Composable
fun BalloonView(balloon: Balloon) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    val xOffset = (screenWidth * balloon.xPosition) - 40.dp
    val yOffset = screenHeight * balloon.yPosition

    Box(
        modifier = Modifier
            .offset(x = xOffset, y = yOffset)
            .size(80.dp)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(balloon.color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = balloon.question.text,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
    }
}

@Composable
fun NumberPad(onNumberClick: (String) -> Unit, onDelete: () -> Unit, onEnter: () -> Unit) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("sil", "0", "gir")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF3E5F5))
            .padding(10.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(2.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { key ->
                    Button(
                        onClick = {
                            when (key) {
                                "sil" -> onDelete()
                                "gir" -> onEnter()
                                else -> onNumberClick(key)
                            }
                        },
                        modifier = Modifier.size(75.dp).padding(2.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (key == "gir") Color(0xFF6200EE) else Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(4.dp)
                    ) {
                        val textColor = if (key == "gir") Color.White else Color.Black
                        if (key == "sil") Text("⌫", color = textColor, fontSize = 20.sp)
                        else if (key == "gir") Text("✓", color = textColor, fontSize = 24.sp)
                        else Text(key, color = textColor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

fun generateRandomBalloon(): Balloon {
    val op = Random.nextInt(3)
    val q: MathQuestion
    if (op == 0) {
        val a = Random.nextInt(1, 20)
        val b = Random.nextInt(1, 20)
        q = MathQuestion("$a + $b", a + b)
    } else if (op == 1) {
        val a = Random.nextInt(10, 30)
        val b = Random.nextInt(1, 10)
        q = MathQuestion("$a - $b", a - b)
    } else {
        val num = Random.nextInt(2, 10)
        q = MathQuestion("√${num * num}", num)
    }

    val colors = listOf(Color(0xFFFF5252), Color(0xFF2196F3), Color(0xFFFFC107), Color(0xFF4CAF50))
    return Balloon(
        question = q,
        color = colors.random(),
        xPosition = Random.nextFloat().coerceIn(0.1f, 0.9f)
    )
}
