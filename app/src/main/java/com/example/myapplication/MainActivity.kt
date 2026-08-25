package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.data.StudyDatabase
import com.example.myapplication.data.StudyLibraryViewModel
import com.example.myapplication.data.StudyLibraryViewModelFactory
import com.example.myapplication.repository.StudyRepository
import com.example.myapplication.ui.ContextAiRoute
import com.example.myapplication.ui.library.StudyDetailScreen
import com.example.myapplication.ui.library.StudyLibraryScreen
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val database = StudyDatabase.getDatabase(this)
        val repository = StudyRepository(database.studyItemDao())
        
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        ContextAiRoute(
                            onOpenLibrary = { navController.navigate("library") },
                            repository = repository
                        )
                    }
                    composable("library") {
                        val libViewModel: StudyLibraryViewModel = viewModel(
                            factory = StudyLibraryViewModelFactory(repository)
                        )
                        StudyLibraryScreen(
                            viewModel = libViewModel,
                            onBack = { navController.popBackStack() },
                            onItemClick = { item -> 
                                navController.navigate("detail/${item.id}")
                            }
                        )
                    }
                    composable("detail/{itemId}") { backStackEntry ->
                        val itemId = backStackEntry.arguments?.getString("itemId")?.toLongOrNull()
                        val libViewModel: StudyLibraryViewModel = viewModel(
                            factory = StudyLibraryViewModelFactory(repository)
                        )
                        val items by libViewModel.studyItems.collectAsState()
                        val item = items.find { it.id == itemId }
                        
                        if (item != null) {
                            StudyDetailScreen(
                                item = item,
                                onBack = { navController.popBackStack() },
                                onFavoriteToggle = { libViewModel.toggleFavorite(item) },
                                onQuizScoreUpdate = { score -> 
                                    libViewModel.updateQuizScore(item, score)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
