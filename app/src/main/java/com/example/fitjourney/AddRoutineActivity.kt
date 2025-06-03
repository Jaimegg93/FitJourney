// AddRoutineActivity.kt
// Actividad para crear o editar rutinas de entrenamiento
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

/*
 * - Si recibimos un extra "fecha", crearemos una rutina nueva para esa fecha (no plantilla).
 * - Si recibimos un extra "routine_id", estamos en modo edición de plantilla o rutina existente.
 */
class AddRoutineActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = DBHelper(this)
        val fechaAsignada = intent.getStringExtra("fecha")
        val routineId = intent.getLongExtra("routine_id", -1L)

        // Si recibimos un ID válido, buscamos la rutina existente (sea plantilla o no)
        val rutinaExistente = if (routineId != -1L)
            db.getRoutines(plantillas = true).plus(db.getRoutines(plantillas = false)).find { it.id == routineId }
        else null
        // Obtenemos sus ejercicios si existe
        val ejerciciosExistentes = if (routineId != -1L) db.getEjerciciosPorRutina(routineId) else emptyList()

        setContent {
            FitJourneyTheme {
                AddRoutineScreen(
                    // Llamamos al Composable que dibuja la pantalla de formulario
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
                        // Tiene que haber al menos un ejercicio creado, con un nombre
                        if (ejercicios.isEmpty()) {
                            Toast.makeText(this, "Añade al menos un ejercicio", Toast.LENGTH_SHORT).show()
                            return@AddRoutineScreen
                        }

                        // Determinamos si guardamos como plantilla (solo si no hay fecha)
                        val esPlantilla = fechaAsignada == null
                        val idFinal: Long

                        if (routineId == -1L) {
                            // Creamos nueva rutina (plantilla o asignada a un día)
                            idFinal = db.insertRoutine(nombreRutina, esPlantilla = esPlantilla)
                        } else {
                            // Actualizamos nombre de rutina existente
                            db.updateRoutineName(routineId, nombreRutina)
                            // Borramos ejercicios anteriores para insertar los nuevos
                            db.deleteRoutineExercise(routineId)
                            idFinal = routineId
                        }
                        // Insertamos cada ejercicio con su configuración
                        ejercicios.forEach { entry ->
                            val series = entry.series.toIntOrNull() ?: 0
                            val reps = entry.reps.toIntOrNull() ?: 0
                            val peso = entry.peso.toDoubleOrNull() ?: 0.0
                            db.insertRoutineExercise(idFinal, entry.nombre, series, reps, peso)
                        }

                        // Si es nueva rutina asociada a fecha, registramos en log de uso
                        if (routineId == -1L && fechaAsignada != null) {
                            db.logRoutine(fechaAsignada, idFinal)
                        }

                        // Avisamos al usuario con toast y cerramos Activity con RESULT_OK
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

// Clase auxiliar para manejar los campos de cada ejercicio en el formulario
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

/**
 * Pantalla para crear o editar una rutina.
 *
 * @param onSave             Lambda que se invoca al pulsar "Guardar", recibe:
 *                           - nombreRutina: String con el nombre ingresado.
 *                           - ejercicios: List<ExerciseEntry> con todos los ejercicios añadidos.
 * @param nombreInicial      Nombre de rutina si estamos editando; cadena vacía si estamos creando.
 * @param ejerciciosIniciales Lista de ExerciseEntry preexistentes (en modo edición) o lista con un elemento vacía por defecto.
 * @param modoEdicion        Boolean que indica si venimos a editar (true) o a crear (false).
 */
@Composable
fun AddRoutineScreen(
    onSave: (String, List<ExerciseEntry>) -> Unit,
    nombreInicial: String = "",
    ejerciciosIniciales: List<ExerciseEntry> = listOf(ExerciseEntry()),
    modoEdicion: Boolean = false
) {
    // Estado para el texto del nombre de la rutina
    var nombreRutina by remember { mutableStateOf(nombreInicial) }
    // Lista mutable de ejercicios en el formulario
    val ejercicios = remember { mutableStateListOf<ExerciseEntry>() }

    // Inicializamos ejercicios si venimos de edición, para mostrar toda la información ya existente
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
            .verticalScroll(rememberScrollState()) // Permite scroll si hay muchos ejercicios
            .padding(16.dp)
    ) {
        Text(
            text = if (modoEdicion) "Editar rutina" else "Crear rutina",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.headlineSmall
        )
        // Campo de texto para el nombre de la rutina
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
        // Para cada ejercicio en la lista, mostramos los campos y botón de eliminar
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
            // Campo para nombre de ejercicio
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
            // Campo para series
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
            // Campo para repeticiones
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
            // Campo para peso
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
        // Botón para añadir un nuevo ejercicio al formulario
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

        // Botón final para guardar; solo habilita onSave si nombre y ejercicios válidos
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

