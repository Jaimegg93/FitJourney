package com.example.fitjourney

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import com.example.fitjourney.ui.theme.FitJourneyTheme
import DBHelper
import android.content.Intent
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
class AddRoutineActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            val db = DBHelper(this)
            val fechaAsignada = intent.getStringExtra("fecha")
            val routineId = intent.getLongExtra("routine_id", -1L)

        val rutinaExistente = if (routineId != -1L)
            db.getRoutines(plantillas = true).plus(db.getRoutines(plantillas = false)).find { it.id == routineId }
        else null
        val ejerciciosExistentes = if (routineId != -1L) db.getExercisesForRoutine(routineId) else emptyList()

        setContent {
            FitJourneyTheme {
                AddRoutineScreen(
                    nombreInicial = rutinaExistente?.nombre ?: "",
                    ejerciciosIniciales = ejerciciosExistentes.map {
                        ExerciseEntry(
                            nombre = it.nombre,
                            series = it.series.toString(),
                            reps = it.repeticiones.toString(),
                            peso = it.peso.toString()
                        )
                    },
                    modoEdicion = routineId != -1L,
                    onSave = { nombreRutina, ejercicios ->

                        if (ejercicios.isEmpty()) {
                            Toast.makeText(this, "Añade al menos un ejercicio", Toast.LENGTH_SHORT).show()
                            return@AddRoutineScreen
                        }

                        val esPlantilla = fechaAsignada == null
                        val idFinal: Long

                        if (routineId == -1L) {
                            idFinal = db.insertRoutine(nombreRutina, esPlantilla = esPlantilla)
                        } else {
                            db.updateRoutineName(routineId, nombreRutina)
                            db.deleteRoutineExercise(routineId)
                            idFinal = routineId
                        }

                        ejercicios.forEach { entry ->
                            val series = entry.series.toIntOrNull() ?: 0
                            val reps = entry.reps.toIntOrNull() ?: 0
                            val peso = entry.peso.toDoubleOrNull() ?: 0.0
                            db.insertRoutineExercise(idFinal, entry.nombre, series, reps, peso)
                        }

                        if (routineId == -1L && fechaAsignada != null) {
                            db.logRoutine(fechaAsignada, idFinal)
                        }

                        Toast.makeText(
                            this,
                            if (routineId == -1L) "Rutina guardada" else "Rutina actualizada",
                            Toast.LENGTH_SHORT
                        ).show()
                        val volverACalendario = intent.getBooleanExtra("volverACalendario", false)
                        val resultIntent = Intent()
                        resultIntent.putExtra("forzarCalendario", volverACalendario)
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    }
                )
            }
        }

        }

    }

class ExerciseEntry(
    nombre: String = "",
    series: String = "",
    reps: String = "",
    peso: String = ""
) {
    var nombre by mutableStateOf(nombre)
    var series by mutableStateOf(series)
    var reps by mutableStateOf(reps)
    var peso by mutableStateOf(peso)

    fun copy(): ExerciseEntry = ExerciseEntry(nombre, series, reps, peso)
}

@Composable
fun AddRoutineScreen(
    onSave: (String, List<ExerciseEntry>) -> Unit,
    nombreInicial: String = "",
    ejerciciosIniciales: List<ExerciseEntry> = listOf(ExerciseEntry()),
    modoEdicion: Boolean = false
) {
    var nombreRutina by remember { mutableStateOf(nombreInicial) }


    val ejercicios = remember { mutableStateListOf<ExerciseEntry>() }

    LaunchedEffect(ejerciciosIniciales) {
        ejercicios.clear()
        ejercicios.addAll(ejerciciosIniciales.map { it.copy() })
    }
    LaunchedEffect(nombreInicial) {
        nombreRutina = nombreInicial
    }
    val context = LocalContext.current



    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = if (modoEdicion) "Editar rutina" else "Crear rutina",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.headlineSmall
        )

        OutlinedTextField(
            value = nombreRutina,
            onValueChange = { nombreRutina = it },
            label = { Text("Nombre rutina") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                cursorColor = MaterialTheme.colorScheme.primary
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        ejercicios.forEachIndexed { idx, entry ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Ejercicio ${idx + 1}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                IconButton(onClick = { ejercicios.removeAt(idx) }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar ejercicio",
                        tint = MaterialTheme.colorScheme.primary // o Color(0xFFFF9800) para naranja directo
                    )
                }
            }

            OutlinedTextField(
                value = entry.nombre,
                onValueChange = { entry.nombre = it },
                label = { Text("Nombre ejercicio") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )
            OutlinedTextField(
                value = entry.series,
                onValueChange = { entry.series = it },
                label = { Text("Series") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )
            OutlinedTextField(
                value = entry.reps,
                onValueChange = { entry.reps = it },
                label = { Text("Repeticiones") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )
            OutlinedTextField(
                value = entry.peso,
                onValueChange = { entry.peso = it },
                label = { Text("Peso") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Button(
            onClick = { ejercicios.add(ExerciseEntry()) },
            modifier = Modifier.align(Alignment.CenterHorizontally),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("Añadir ejercicio", color = MaterialTheme.colorScheme.onPrimary)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (nombreRutina.isNotBlank() && ejercicios.all { it.nombre.isNotBlank() }) {
                    onSave(nombreRutina, ejercicios)
                } else {
                    Toast.makeText(context, "Completa el nombre y todos los ejercicios", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.align(Alignment.End),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                if (modoEdicion) "Guardar cambios" else "Guardar rutina",
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

