// MainActivity.kt
// Actividad principal de la app, donde se define la interfaz y la navegación entre las pestañas.

package com.example.fitjourney

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.platform.LocalContext
import DBHelper
import FondoPantalla
import TextoPrimario
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.CalendarToday
import java.time.format.DateTimeFormatter
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Delete
import com.example.fitjourney.ui.theme.FitJourneyTheme

// Extiende ComponentActivity para usar Jetpack Compose.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FitJourneyTheme {
                PantallaPrincipal()
            }
        }
    }
}

/**
 * Composable que gestiona la pantalla principal de la app FitJourney.
 *
 * Se encarga de:
 *  - Mantener el estado de la pestaña seleccionada ("estadisticas", "calendario", "rutinas").
 *  - Controlar fechas y selección de días (emptyDate, routineDate, plantillaFecha).
 *  - Cargar y mapear las rutinas programadas para mostrarlas en el calendario.
 *  - Definir los launchers para crear/editar rutinas (plantillas y rutinas en fecha específica).
 *  - Pintar el Scaffold con TopAppBar, FloatingActionButton (solo en "rutinas") y barra de navegación inferior.
 *  - En función del estado, mostrar:
 *      • PlantillaSelectorScreen si el usuario seleccionó una plantilla para una fecha.
 *      • EmptyDayScreen si el día del calendario está vacío.
 *      • RoutineDayScreen si hay rutinas programadas en el día.
 *      • PantallaEstadisticas en la pestaña de estadísticas.
 *      • CalendarioPersonalizado en la pestaña de calendario.
 *      • Lista de plantillas (con opciones de edición/borrado) en la pestaña de rutinas.
 *
 */
@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPrincipal() {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf("calendario") }
    var emptyDate by remember { mutableStateOf<LocalDate?>(null) }
    var plantillaFecha by remember { mutableStateOf<LocalDate?>(null) }
    var mesMostrado by remember { mutableStateOf(LocalDate.now().withDayOfMonth(1)) }
    var routineDate by remember { mutableStateOf<LocalDate?>(null) }
    var routinesForDay by remember { mutableStateOf<List<DBHelper.Routine>>(emptyList()) }
    val entrenosPorFechaState = remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
    val db = remember { DBHelper(context) }

    val coloresRutina = remember { mutableStateMapOf<String, Color>() }
    val listaColores = listOf(
        Color(0xFFE57373), // Rojo suave
        Color(0xFF81C784), // Verde suave
        Color(0xFF64B5F6), // Azul claro
        Color(0xFFBA68C8), // Lila pastel
        Color(0xFF4DB6AC), // Verde azulado claro
        Color(0xFFA1887F), // Marrón grisáceo
        Color(0xFFFFB74D), // Naranja pastel
        Color(0xFFCDE02F)  // Amarillo
    )

    // Función para cargar del log de la base de datos todos los entrenos y llenar el mapa
    fun cargarEntrenos() {
        val mapa = mutableMapOf<String, List<String>>()
        val query = """
        SELECT rl.fecha, r.nombre 
        FROM routine_log rl
        JOIN routines r ON rl.routine_id = r.id
    """.trimIndent()
        val cursor = db.readableDatabase.rawQuery(query, null)
        cursor.use {
            while (it.moveToNext()) {
                val fecha = it.getString(0)
                val nombre = it.getString(1)

                // Si el nombre de la rutina aún no tiene color asignado, lo agregamos
                if (nombre !in coloresRutina) {
                    val index = coloresRutina.size % listaColores.size
                    coloresRutina[nombre] = listaColores[index]
                }

                // Añadir rutina a esa fecha
                mapa[fecha] = mapa.getOrDefault(fecha, emptyList()) + nombre
            }
        }
        entrenosPorFechaState.value = mapa
    }

    // Estado y función para mantener la lista de plantillas (routines con esPlantilla = true)
    val rutinasState = remember { mutableStateOf(emptyList<DBHelper.Routine>()) }
    fun actualizarRutinas() {
        rutinasState.value = db.getRoutines(plantillas = true)
        cargarEntrenos()
    }

    // Lanzadores para iniciar AddRoutineActivity:
    // - rutinaAddLauncher abre pantalla para crear una nueva plantilla (botón + en "Rutinas")
    val rutinaAddLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            actualizarRutinas()
            selectedTab = "calendario"
            selectedTab = "rutinas"
        }
    }

    // - addLauncher abre pantalla para agregar una rutina a un día concreto del calendario
    val addLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && emptyDate != null) {
            val db = DBHelper(context)
            val fecha = emptyDate!!.toString()
            val nuevasRutinas = db.getRoutinasPorFecha(fecha)
            cargarEntrenos()

            if (nuevasRutinas.isNotEmpty()) {
                routineDate = LocalDate.parse(fecha)
                routinesForDay = nuevasRutinas
                emptyDate = null
            }
        }
    }
    // Se ejecuta al componer por primera vez, carga todos los entrenos
    LaunchedEffect(Unit) {
        cargarEntrenos()
    }
    // Función para resetear las selecciones de día y rutina en la vista principal
    val resetVista = {
        routineDate = null
        emptyDate = null
        routinesForDay = emptyList()
    }
    // Estructura Scaffold
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // El título cambia según la pestaña seleccionada
                    Text(
                        when (selectedTab) {
                            "estadisticas" -> "Estadísticas"
                            "calendario" -> "Calendario"
                            "rutinas" -> "Rutinas"
                            else -> "FitJourney"
                        }
                    )
                }
            )
        },
        //Boton para crear nueva plantilla
        floatingActionButton = {
            // Solo mostramos el FAB cuando estamos en la pestaña "rutinas"
            when (selectedTab) {
                "rutinas" -> FloatingActionButton(onClick = {
                    val intent = Intent(context, AddRoutineActivity::class.java)
                    rutinaAddLauncher.launch(intent)
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Nueva Rutina")
                }
            }
        },
        // Barra de navegación inferior con tres iconos: Estadísticas, Calendario y Rutinas
        bottomBar = {
            NavegacionInferior(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                clearSelection = resetVista
            )
        }
    ) { padding ->
        // Contenedor principal dentro de Scaffold
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Lógica principal de la vista dependiendo de:
            // 1) Si se seleccionó una plantilla (plantillaFecha != null)
            // 2) Si estamos en pestaña "calendario" y día sin rutinas (emptyDate != null)
            // 3) Si estamos en "calendario" y hay rutinas para el día (routineDate != null && routinesForDay no vacío)
            // 4) Si estamos en pestaña "estadisticas"
            // 5) Si estamos en pestaña "calendario" (pintar calendario)
            // 6) Si estamos en pestaña "rutinas" (lista de plantillas)
            when {
                // 1) Mostrar selector de plantillas para la fecha indicaba
                plantillaFecha != null -> {
                    val db = DBHelper(context)
                    val plantillas = remember { db.getRoutines(plantillas = true) }
                    // Si el usuario pulsa atrás, cancelamos selección de plantilla
                    // Sin esto al dar hacia atras en un dia del calendario se sale de la app
                    BackHandler { plantillaFecha = null }
                    PlantillaSelectorScreen(
                        fecha = plantillaFecha!!,
                        plantillas = plantillas,
                        onUsarPlantilla = { rutina ->
                            val copiaId = db.duplicarRutina(rutina.id)
                            if (copiaId != -1L) {
                                db.logRoutine(plantillaFecha.toString(), copiaId)
                                cargarEntrenos()
                                Toast.makeText(
                                    context,
                                    "Rutina añadida a $plantillaFecha",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(context, "Error al duplicar la rutina", Toast.LENGTH_SHORT).show()
                            }
                            plantillaFecha = null
                            emptyDate = null
                        },
                        onVolver = {
                            plantillaFecha = null
                        }
                    )
                }
                // 2) Si estamos en calendario y day vacío (no hay rutinas), mostramos pantalla para crear o usar plantilla
                selectedTab == "calendario" && emptyDate != null -> {
                    // Manejo del botón atrás para volver a vista de calendario sin detalle
                    // Sin esto al dar hacia atras en un dia del calendario se sale de la app
                    BackHandler { emptyDate = null }
                    EmptyDayScreen(
                        date = emptyDate!!,
                        onAddRoutine = {
                            val intent = Intent(context, AddRoutineActivity::class.java)
                            intent.putExtra("fecha", emptyDate!!.toString())
                            addLauncher.launch(intent)
                        },
                        onUsarPlantilla = {
                            plantillaFecha = emptyDate
                        }
                    )
                }
                // 3) Si estamos en calendario y ya hay rutinas para ese día seleccionado
                selectedTab == "calendario" && routineDate != null && routinesForDay.isNotEmpty() -> {
                    // Manejo del botón atrás para volver a vista de calendario sin detalle
                    // Sin esto al dar hacia atras en un dia del calendario se sale de la app
                    BackHandler {
                        routineDate = null
                        routinesForDay = emptyList()
                    }
                    key(routineDate, routinesForDay) {
                        RoutineDayScreen(
                            date = routineDate!!,
                            routines = mutableStateOf(routinesForDay),
                            db = db,
                            onRoutineDeleted = {
                                // Si borran una rutina, recargamos lista de rutinas para esa fecha
                                routinesForDay = db.getRoutinasPorFecha(routineDate.toString()).map { it.copy() }
                                cargarEntrenos()
                            },
                            onRoutineEdited = {
                                routinesForDay = db.getRoutinasPorFecha(routineDate.toString()).map { it.copy() }
                                cargarEntrenos()
                            },
                            onUsarPlantilla = { plantillaFecha = it },
                            onFinalizarAccion = {
                                // Al finalizar (borrar o editar), volvemos a vista de calendario
                                routineDate = null
                                routinesForDay = emptyList()
                                selectedTab = "calendario"
                            }
                        )
                    }

                }
                // 4) Pestaña "Estadísticas": mostramos pantalla de estadísticas generales
                selectedTab == "estadisticas" -> {
                    PantallaEstadisticas(db = DBHelper(context))
                }
                // 5) Pestaña "Calendario": pintamos el calendario con eventos (puntos de colores)
                selectedTab == "calendario" -> {
                    // Asegurarnos de recargar datos antes de pintar
                    cargarEntrenos()
                    CalendarioPersonalizado(
                        entrenosPorFecha = entrenosPorFechaState.value,
                        coloresRutina = coloresRutina,
                        mesMostrado = mesMostrado,
                        onMesCambiado = { mesMostrado = it },
                        onDayClick = { fechaIso ->
                            Log.d("DEBUG", "Día pulsado: $fechaIso")

                            if (fechaIso.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                                val db = DBHelper(context)
                                val rutinasDelDia = db.getRoutinasPorFecha(fechaIso)
                                Log.d(
                                    "DEBUG",
                                    "Se encontraron ${rutinasDelDia.size} rutinas para ese día"
                                )
                                // Hay rutinas: mostramos detalle
                                if (rutinasDelDia.isNotEmpty()) {
                                    routineDate = LocalDate.parse(fechaIso)
                                    routinesForDay = rutinasDelDia
                                    emptyDate = null
                                }
                                // No hay rutinas: marcamos día vacío
                                else {
                                    emptyDate = LocalDate.parse(fechaIso)
                                    routineDate = null
                                    routinesForDay = emptyList()
                                }
                            }
                        }
                    )
                }
                // 6) Pestaña "Rutinas": mostramos lista de plantillas
                selectedTab == "rutinas" -> {
                    fun actualizarRutinas() {
                        rutinasState.value = db.getRoutines(plantillas = true)
                        cargarEntrenos()
                        // Forzamos pequeño cambio de pestaña para recomponer
                        //No lograba que funcionase de otra manera
                        selectedTab = "calendario"
                        selectedTab = "rutinas"
                    }
                    // Launcher para cuando editamos una plantilla (pasamos el ID de rutina)
                    val rutinaEditLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        if (result.resultCode == RESULT_OK) {
                            actualizarRutinas()
                            // Forzamos pequeño cambio de pestaña para recomponer
                            selectedTab = "calendario"
                            selectedTab = "rutinas"
                        }
                    }
                    // LaunchedEffect para cargar plantillas al entrar por primera vez
                    LaunchedEffect(Unit) {
                        actualizarRutinas()
                    }
                    // Si no hay plantillas, mostramos mensaje
                    if (rutinasState.value.isEmpty()) {
                        Text(
                            "No tienes rutinas guardadas",
                            modifier = Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            //Mostramos las plantillas en cards
                            items(rutinasState.value) { rutina ->
                                val ejercicios = db.getEjerciciosPorRutina(rutina.id)
                                var showConfirmDialog by remember { mutableStateOf(false) }
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                        .animateContentSize(),
                                    elevation = CardDefaults.cardElevation(4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Column {
                                                Text(
                                                    rutina.nombre,
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                                Text(
                                                    "${ejercicios.size} ejercicios",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextoPrimario
                                                )
                                            }

                                            IconButton(onClick = { showConfirmDialog = true }) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Eliminar",
                                                    tint = Color(0xFFFF9800)
                                                )
                                            }
                                        }

                                        Spacer(Modifier.height(8.dp))

                                        ejercicios.forEach {
                                            Text("- ${it.nombre}: ${it.series}x${it.repeticiones}, ${it.peso} kg")
                                        }

                                        Spacer(Modifier.height(16.dp))

                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Button(
                                                onClick = {
                                                    val intent = Intent(context, AddRoutineActivity::class.java)
                                                    intent.putExtra("routine_id", rutina.id)
                                                    rutinaEditLauncher.launch(intent)
                                                },
                                                modifier = Modifier.defaultMinSize(minHeight = 32.dp),
                                                contentPadding = PaddingValues(
                                                    horizontal = 20.dp,
                                                    vertical = 8.dp
                                                )
                                            ) {
                                                Text(
                                                    "Editar",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                    }
                                }
                                //Modal para confirmar si se quiere borrar la plantila
                                if (showConfirmDialog) {
                                    AlertDialog(
                                        onDismissRequest = { showConfirmDialog = false },
                                        title = { Text("¿Eliminar rutina?") },
                                        text = { Text("Esta acción no se puede deshacer.") },
                                        confirmButton = {
                                            TextButton(onClick = {
                                                db.deleteRoutine(rutina.id)
                                                Toast.makeText(context, "Rutina eliminada", Toast.LENGTH_SHORT).show()
                                                showConfirmDialog = false
                                                actualizarRutinas()
                                            }) {
                                                Text("Sí, eliminar")
                                            }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { showConfirmDialog = false }) {
                                                Text("Cancelar")
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


/**
 * Dibuja un calendario mensual personalizado mostrando puntos de color para los días con rutinas.
 *
 * @param entrenosPorFecha Mapa cuyas claves son fechas en formato "YYYY-MM-DD" y cuyos valores son listas de nombres de rutinas programadas para esa fecha.
 * @param coloresRutina    Mapa que asocia cada nombre de rutina a un Color, utilizado para pintar los puntos representativos en el calendario.
 * @param mesMostrado      Fecha (primer día del mes) que indica el mes actualmente visible en el calendario.
 * @param onMesCambiado    Callback que se invoca con un LocalDate (primer día del mes anterior o siguiente) cuando el usuario cambia de mes (botones o gesto de arrastre).
 * @param onDayClick       Callback que se invoca con una String (fecha ISO "YYYY-MM-DD") cuando el usuario pulsa sobre un día válido del calendario.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarioPersonalizado(
    entrenosPorFecha: Map<String, List<String>>,
    coloresRutina: Map<String, Color>,
    mesMostrado: LocalDate,
    onMesCambiado: (LocalDate) -> Unit,
    onDayClick: (String) -> Unit
) {
    // Obtenemos el nombre completo del mes y ponemos a mayuscula la primera letra
    val nombreMes = mesMostrado.month
        .getDisplayName(TextStyle.FULL, Locale.getDefault())
        .replaceFirstChar { it.uppercase() }

    // Variable para llevar el arrastre horizontal, para poder cambiar de mes arrastrando y no solo con los botones
    var totalDragX by remember { mutableStateOf(0f) }

    // Caja que cubre el ancho completo para detectar gestos de arrastre
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(mesMostrado) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        // Sumamos la distancia arrastrada en X
                        totalDragX += dragAmount.x
                        change.consumePositionChange()
                    },
                    onDragEnd = {
                        // Si arrastramos hacia la derecha, vamos al mes anterior
                        if (totalDragX > 100f) onMesCambiado(mesMostrado.minusMonths(1))
                        // Si arrastramos hacia la izquierda, vamos al mes siguiente
                        else if (totalDragX < -100f) onMesCambiado(mesMostrado.plusMonths(1))
                        // Reiniciamos el valor
                        totalDragX = 0f
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            // Cabecera: botón mes anterior, nombre del mes y botón mes siguiente
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón "Anterior"
                TextButton(onClick = { onMesCambiado(mesMostrado.minusMonths(1)) }) {
                    Text("< Anterior")
                }
                // Texto con el nombre del mes y año
                Text(
                    text = "$nombreMes ${mesMostrado.year}",
                    style = MaterialTheme.typography.titleMedium
                )
                // Botón "Siguiente"
                TextButton(onClick = { onMesCambiado(mesMostrado.plusMonths(1)) }) {
                    Text("Siguiente >")
                }
            }

            // AnimatedContent: animar cambio de mes
            AnimatedContent(
                targetState = mesMostrado,
                label = "CambioDeMes",
                transitionSpec = {
                    // Mes siguiente: entramos desde la derecha y salimos a la izquierda
                    if (targetState > initialState) {
                        slideInHorizontally { it } + fadeIn() togetherWith
                                slideOutHorizontally { -it } + fadeOut()
                    }
                    // Mes anterior: entramos desde la izquierda y salimos a la derecha
                    else {
                        slideInHorizontally { -it } + fadeIn() togetherWith
                                slideOutHorizontally { it } + fadeOut()
                    }
                }
            ) { mesActual ->
                // Calculamos cuántos días tiene el mes
                val diasEnMes = mesActual.lengthOfMonth()
                // Offset para el primer día (valor de 0 a 6)
                val offset = mesActual.dayOfWeek.value - 1
                // Lista de strings con celdas vacías (“”) y luego los números 1..diasEnMes
                val dias = buildList<String> {
                    repeat(offset) { add("") }
                    for (d in 1..diasEnMes) add(d.toString())
                }

                Column(Modifier.fillMaxWidth()) {
                    // Cabecera de la semana
                    Row(Modifier.fillMaxWidth()) {
                        listOf(
                            "LUN",
                            "MAR",
                            "MIÉ",
                            "JUE",
                            "VIE",
                            "SÁB",
                            "DOM"
                        ).forEach { diaSemana ->
                            Text(
                                text = diaSemana,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    // Filas de días
                    dias.chunked(7).forEach { fila ->
                        Row(Modifier.fillMaxWidth()) {
                            fila.forEach { dia ->
                                // Si el string está vacío, es celda de relleno
                                val esRelleno = dia.isBlank()
                                // Definimos la clave ISO solo si no es relleno
                                val clave = if (!esRelleno) {
                                    "%04d-%02d-%02d".format(
                                        mesActual.year,
                                        mesActual.monthValue,
                                        dia.toInt()
                                    )
                                } else ""
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(72.dp)
                                        // Fondo oscuro si relleno, transparente si día válido
                                        .background(if (esRelleno) FondoPantalla else Color.Transparent)
                                        // Solo clicable si no es relleno
                                        .clickable(enabled = !esRelleno) {
                                            if (clave.isNotBlank()) {
                                                onDayClick(clave)
                                            }
                                        },
                                    contentAlignment = Alignment.TopStart
                                ) {
                                    Column(
                                        Modifier
                                            .fillMaxSize()
                                            .padding(4.dp)
                                    ) {
                                        // Número del día centrado arriba
                                        Text(
                                            text = dia,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        if (!esRelleno) {
                                            // Obtenemos lista de rutinas programadas para esa fecha
                                            val rutinas = entrenosPorFecha[clave].orEmpty()
                                            Row(
                                                modifier = Modifier.align(Alignment.CenterHorizontally),
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                // Dibujamos hasta 3 puntos de color por cada rutina
                                                rutinas.take(3).forEach { nombre ->
                                                    Box(
                                                        modifier = Modifier
                                                            .size(14.dp)
                                                            .padding(2.dp)
                                                            .background(
                                                                color = coloresRutina[nombre]
                                                                    ?: FondoPantalla,
                                                                shape = CircleShape
                                                            )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            // Rellenar celdas faltantes
                            repeat(7 - fila.size) {
                                Spacer(
                                    Modifier
                                        .weight(1f)
                                        .height(72.dp)
                                        .background(FondoPantalla)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


/**
 * Barra de navegación inferior con iconos para cambiar entre pestañas de la aplicación.
 *
 * @param selectedTab   Cadena que indica la pestaña actualmente seleccionada ("estadisticas", "calendario" o "rutinas").
 * @param onTabSelected Lambda que recibe el identificador de la pestaña seleccionada al pulsar un icono y actualiza la vista.
 * @param clearSelection Lambda que se invoca para reiniciar cualquier selección de día o rutina cuando se cambia de pestaña.
 */
@Composable
fun NavegacionInferior(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    clearSelection: () -> Unit
) {
    // Contenedor de la barra de navegación inferior
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 0.dp
    ) {
        // Ítem para la pestaña "Estadísticas"
        NavigationBarItem(
            // Icono de gráfico de barras
            icon = { Icon(Icons.Filled.ShowChart, contentDescription = "Estadísticas") },
            // Marca si está seleccionado (para colorear el ícono)
            selected = selectedTab == "estadisticas",
            onClick = {
                // Al pulsar, cambiamos a la pestaña "estadisticas"
                onTabSelected("estadisticas")
                // Limpiamos cualquier selección de día o rutina pendiente
                clearSelection()
            },
            // Colores para el ítem (icono seleccionado vs. no seleccionado)
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurface,
                indicatorColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            // No mostramos texto en la etiqueta
            label = {},
            alwaysShowLabel = false

        )
        // Ítem para la pestaña "Calendario"
        NavigationBarItem(
            icon = {
                // Icono de calendario; si está seleccionado, se pinta con color primario
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = if (selectedTab == "calendario")
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurface,
                indicatorColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            selected = selectedTab == "calendario",
            onClick = {
                onTabSelected("calendario")
                clearSelection()
            },
            label = {},
            alwaysShowLabel = false
        )
        // Ítem para la pestaña "Rutinas"
        NavigationBarItem(
            // Icono de pesas
            icon = { Icon(Icons.Filled.FitnessCenter, contentDescription = "Rutinas") },
            selected = selectedTab == "rutinas",
            onClick = {
                onTabSelected("rutinas")
                clearSelection()
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurface,
                indicatorColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            label = {},
            alwaysShowLabel = false
        )
    }

}

/**
 * Pantalla que aparece cuando el usuario selecciona un día sin rutinas programadas.
 *
 * @param date          LocalDate que representa la fecha seleccionada.
 * @param onAddRoutine  Lambda que se invoca cuando el usuario pulsa el botón "Crear rutina desde cero". Se usará para abrir la actividad de creación de rutina.
 * @param onUsarPlantilla Lambda que se invoca cuando el usuario pulsa "Usar rutina existente". Permite seleccionar una plantilla para esa fecha.
 */
@Composable
fun EmptyDayScreen(
    date: LocalDate,
    onAddRoutine: () -> Unit,
    onUsarPlantilla: () -> Unit
) {
    // Locale en español para obtener correctamente el nombre del día de la semana
    val locale = Locale("es")
    // Nombre completo del día con inicial mayúscula
    val dayName = date.dayOfWeek
        .getDisplayName(TextStyle.FULL, locale)
        .replaceFirstChar { it.uppercaseChar() }
    // Formateamos la fecha en "dd/MM/yyyy"
    val dateStr = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

    // column por que la informacion se ordena verticalmente
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoPantalla)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Mostramos el nombre del día
        Text(
            text = dayName,
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(4.dp))
        // Mostramos la fecha
        Text(
            text = dateStr,
            style = MaterialTheme.typography.bodyMedium,
            color = TextoPrimario
        )

        Spacer(Modifier.height(32.dp))
        // Icono de calendario grande para indicar que no hay rutinas
        Icon(
            imageVector = Icons.Default.CalendarToday,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        // Mensaje informando que se esta en un dia vacio
        Text(
            text = "No hay ninguna rutina guardada o programada",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))

        // Botón para crear una rutina desde cero
        Button(onClick = onAddRoutine) {
            Text("Crear rutina desde cero")
        }
        Spacer(Modifier.height(12.dp))
        // Botón para usar una plantilla existente
        OutlinedButton(onClick = onUsarPlantilla) {
            Text("Usar rutina existente")
        }
    }
}

/**
 * Pantalla que muestra las rutinas programadas para un día específico y permite editar o eliminar.
 *
 * @param date             LocalDate con la fecha seleccionada.
 * @param routines         Estado mutable (MutableState) con la lista de rutinas (DBHelper.Routine) para ese día.
 * @param db               Instancia de DBHelper para operaciones en base de datos.
 * @param onRoutineDeleted Lambda que se llama cuando se elimina una rutina, para actualizar la lista y el calendario.
 * @param onRoutineEdited  Lambda que se llama cuando se edita una rutina, para recargar la lista y actualizar el calendario.
 * @param onUsarPlantilla  Lambda que recibe una LocalDate: si se quiere usar una plantilla para esa misma fecha, se invoca con la fecha.
 * @param onFinalizarAccion Lambda que se invoca al terminar una acción (editar/eliminar) para regresar a la vista de calendario.
 */
@Composable
fun RoutineDayScreen(
    date: LocalDate,
    routines: MutableState<List<DBHelper.Routine>>,
    db: DBHelper,
    onRoutineDeleted: () -> Unit,
    onRoutineEdited: () -> Unit,
    onUsarPlantilla: (LocalDate) -> Unit,
    onFinalizarAccion: () -> Unit,
) {
    // Contexto de la Activity para lanzar Intents y mostrar Toasts
    val context = LocalContext.current
    // Formateador para mostrar la fecha en "dd/MM/yyyy"
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    // Nombre completo del día de la semana en español
    val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("es"))
        .replaceFirstChar { it.uppercaseChar() }

    // Launcher para abrir AddRoutineActivity y esperar resultado (editar rutina existente)
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Si se editó, recargamos las rutinas para esa fecha
            val nuevasRutinas = db.getRoutinasPorFecha(date.toString())
            routines.value = nuevasRutinas.map { it.copy(nombre = it.nombre) }
            // Informamos que se editó y regresamos a calendario
            onRoutineEdited()
            onFinalizarAccion()
        }
    }
    // Columna principal que contiene título y lista de rutinas
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Título con "Día NombreDD/MM/YYYY"
        Text(
            "$dayName ${date.format(formatter)}",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(16.dp))

        // Para cada rutina en el estado "routines"
        routines.value.forEach { rutina ->
            // Estado local que controla si el Card está expandido para mostrar ejercicios, en true por defecto
            var expanded by remember { mutableStateOf(true) }
            // Obtenemos la lista de ejercicios para esta rutina (desde BD)
            val ejercicios = db.getEjerciciosPorRutina(rutina.id)
            // Estado local para diálogo de confirmación al eliminar rutina, oculto por defecto
            var showConfirmDialog by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { expanded = !expanded }// Al pulsar el Card, alternamos expandido/plegado
                    .animateContentSize(),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Fila con nombre de rutina y botón de eliminar (solo visible si está expandido)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            // Nombre de la rutina
                            Text(rutina.nombre, style = MaterialTheme.typography.titleMedium)
                            // Cantidad de ejercicios (texto más pequeño)
                            Text(
                                text = "${ejercicios.size} ejercicios",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextoPrimario
                            )
                        }

                        if (expanded) {
                            // Botón de eliminar, solo si el Card está expandido, hace aparecer el modal
                            IconButton(onClick = { showConfirmDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Eliminar rutina",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    // Sección expandible: muestra lista de ejercicios y botón "Editar"
                    AnimatedVisibility(visible = expanded) {
                        Column {
                            Spacer(Modifier.height(8.dp))
                            // Para cada ejercicio de la rutina, mostramos: nombre, series, repeticiones y peso"
                            ejercicios.forEach {
                                Text("- ${it.nombre}: ${it.series}x${it.repeticiones}, ${it.peso} kg")
                            }
                            Spacer(Modifier.height(16.dp))

                            // Botón para editar rutina existente
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Button(
                                    onClick = {
                                        // Lanzamos AddRoutineActivity en modo edición pasando el ID de rutina
                                        val intent = Intent(context, AddRoutineActivity::class.java)
                                        intent.putExtra("routine_id", rutina.id)
                                        launcher.launch(intent)
                                    },
                                    modifier = Modifier.defaultMinSize(minHeight = 32.dp),
                                    contentPadding = PaddingValues(
                                        horizontal = 20.dp,
                                        vertical = 8.dp
                                    )
                                ) {
                                    Text("Editar", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
                // Modal de confirmación para eliminar rutina
                if (showConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = { showConfirmDialog = false },
                        confirmButton = {
                            TextButton(onClick = {
                                // Si confirma, eliminamos rutina en BD y mostramos Toast
                                db.deleteRoutine(rutina.id)
                                Toast.makeText(context, "Rutina eliminada", Toast.LENGTH_SHORT).show()
                                showConfirmDialog = false
                                // Informamos que se eliminó para actualizar lista y calendario
                                onRoutineDeleted()
                            }) {
                                Text("Sí, eliminar")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showConfirmDialog = false }) {
                                Text("Cancelar")
                            }
                        },
                        title = { Text("¿Eliminar rutina?") },
                        text = { Text("Esta acción no se puede deshacer.") }
                    )
                }
            }


        }
        Spacer(modifier = Modifier.height(24.dp))

        // Botón para crear nueva rutina desde cero o usando una plantilla
        Column (
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            )
         {
             // Botón para crear nueva rutina desde cero
             // Al pulsar, lanzamos AddRoutineActivity sin ID (es nueva) pasando la fecha
             Button(onClick = {
                val intent = Intent(context, AddRoutineActivity::class.java)
                intent.putExtra("fecha", date.toString())
                launcher.launch(intent)
            }) {
                Text("Crear rutina desde 0")
            }
             Spacer(modifier = Modifier.height(5.dp))

             // Botón para usar plantilla existente en ese día
             // Volvemos a la vista de calendario
             OutlinedButton(
                 onClick = { onUsarPlantilla(date)
                     onFinalizarAccion()
                     },
             ) {
                 Text("Usar rutina existente")
             }
        }
    }
}


/**
 * Pantalla para seleccionar una rutina plantilla y aplicarla a una fecha dada.
 *
 * @param fecha           LocalDate que representa el día al que se aplicará la plantilla.
 * @param plantillas      Lista de rutinas (DBHelper.Routine) marcadas como plantillas (esPlantilla = true).
 * @param onUsarPlantilla Lambda que recibe la rutina seleccionada; se invoca para duplicar esa plantilla en la fecha indicada.
 * @param onVolver        Lambda que se invoca cuando el usuario pulsa el botón “Volver” para descartar la selección de plantilla.
 */
@Composable
fun PlantillaSelectorScreen(
    fecha: LocalDate,
    plantillas: List<DBHelper.Routine>,
    onUsarPlantilla: (DBHelper.Routine) -> Unit,
    onVolver: () -> Unit
) {
    // Formateador de fecha para mostrar día/mes/año
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val fechaStr = fecha.format(formatter)
    val context = LocalContext.current
    val db = remember { DBHelper(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Título con instrucción y fecha
        Text(
            text = "Selecciona una rutina para el $fechaStr",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Lista para mostrar todas las plantillas disponibles
        LazyColumn {
            items(plantillas) { rutina ->
                // Estado local para expandir/plegar la tarjeta al pulsar, falso de inicio por si hay mucha
                var expanded by remember { mutableStateOf(false) }
                // Obtenemos los ejercicios de esta plantilla solo una vez (remember con rutina.id)
                val ejercicios = remember(rutina.id) { db.getEjerciciosPorRutina(rutina.id) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { expanded = !expanded } // Al hacer clic, alternamos expandido
                        .animateContentSize(),  // Animación de cambio de tamaño
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Nombre de la plantilla
                        Text(
                            rutina.nombre,
                            style = MaterialTheme.typography.titleMedium
                        )
                        // Sección expandible: si expanded == true, mostramos ejercicios y botón “Usar”
                        AnimatedVisibility(visible = expanded) {
                            Column {
                                Spacer(modifier = Modifier.height(12.dp)
                                    .fillMaxWidth()
                                )
                                // Listamos cada ejercicio de la plantilla
                                ejercicios.forEach { ejercicio ->
                                    Text(
                                        text = "${ejercicio.nombre}: ${ejercicio.series}x${ejercicio.repeticiones} - ${ejercicio.peso} kg",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                // Botón centrado para aplicar esta plantilla a la fecha
                                Button(
                                    onClick = { onUsarPlantilla(rutina) },
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                ) {
                                    Text("Usar")
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        // Botón para volver sin seleccionar ninguna plantilla
        Button(
            onClick = onVolver,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Volver")
        }
    }
}

/**
 * Pantalla de estadísticas que muestra un resumen de entrenamientos y uso de plantillas.
 *
 * @param db Instancia de DBHelper para consultar datos de entrenamientos y plantillas desde la base de datos.
 */
@Composable
fun PantallaEstadisticas(db: DBHelper) {
    // Estado para almacenar el resumen: Triple(totalDiasEntrenados, totalRutinas, nombreRutinaMasUsada)
    val resumen = remember {
        mutableStateOf(Triple(0, 0, ""))
    }
    // Estado para almacenar la lista de estadísticas por cada plantilla (DBHelper.RutinaEstadistica)
    val rutinas = remember {
        mutableStateOf<List<DBHelper.RutinaEstadistica>>(emptyList())
    }

    // Al iniciar la composición, cargamos los datos de la base de datos
    LaunchedEffect(Unit) {
        val totalDias = db.getTotalDiasEntrenados()
        val lista = db.getEstadisticasPorRutinaPlantilla()

        val totalRutinas = lista.sumOf { it.veces }
        val masUsada = lista.maxByOrNull { it.veces }?.nombre ?: "Ninguna"

        resumen.value = Triple(totalDias, totalRutinas, masUsada)
        rutinas.value = lista
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())  // Permite desplazar si hay muchas plantillas
    ) {
        // Título principal de la pantalla
        Text("Resumen de Entrenamientos", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        // Card que muestra los valores del resumen
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Días entrenados: ${resumen.value.first}")
                Text("Rutinas realizadas: ${resumen.value.second}")
                Text("Rutina más usada: ${resumen.value.third}")
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Uso de Plantillas", style = MaterialTheme.typography.titleMedium)

        Spacer(Modifier.height(8.dp))

        // Para cada elemento en rutinas.value, mostramos una tarjeta con detalles
        rutinas.value.forEach {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(it.nombre, style = MaterialTheme.typography.titleMedium)
                    Text("Veces usada: ${it.veces}")
                    Text("Última vez: ${it.ultimaFecha ?: "Nunca"}")
                }
            }
        }
    }
}

