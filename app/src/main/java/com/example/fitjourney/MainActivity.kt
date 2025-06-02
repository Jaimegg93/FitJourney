package com.example.fitjourney

import BordeDiaVacio
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
import androidx.compose.foundation.border
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
import DiaVacioOscuro
import FondoDiaVacio
import FondoPantalla
import TextoPrimario
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.FitnessCenter
import java.time.format.DateTimeFormatter
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Delete
import com.example.fitjourney.ui.theme.FitJourneyTheme


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

                    // Asignar color si es nuevo
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


        val rutinasState = remember { mutableStateOf(emptyList<DBHelper.Routine>()) }
        fun actualizarRutinas() {
            rutinasState.value = db.getRoutines(plantillas = true)
            cargarEntrenos()
        }

    val rutinaAddLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            actualizarRutinas()
            selectedTab = "calendario"
            selectedTab = "rutinas"
        }
    }
        val addLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK && emptyDate != null) {
                val db = DBHelper(context)
                val fecha = emptyDate!!.toString()
                val nuevasRutinas = db.getRoutinesForDate(fecha)
                cargarEntrenos()

                if (nuevasRutinas.isNotEmpty()) {
                    routineDate = LocalDate.parse(fecha)
                    routinesForDay = nuevasRutinas
                    emptyDate = null
                }
            }
        }
        LaunchedEffect(Unit) {
            cargarEntrenos()
        }
        val resetVista = {
            routineDate = null
            emptyDate = null
            routinesForDay = emptyList()
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
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
            floatingActionButton = {
                when (selectedTab) {
                    "rutinas" -> FloatingActionButton(onClick = {
                        val intent = Intent(context, AddRoutineActivity::class.java)
                        rutinaAddLauncher.launch(intent)
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Nueva Rutina")
                    }
                }
            },
            bottomBar = {
                NavegacionInferior(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    clearSelection = resetVista
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                when {
                    plantillaFecha != null -> {
                        val db = DBHelper(context)
                        val plantillas = remember { db.getRoutines(plantillas = true) }
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
                    selectedTab == "calendario" && emptyDate != null -> {
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
                        selectedTab == "calendario" && routineDate != null && routinesForDay.isNotEmpty() -> {
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
                                        routinesForDay = db.getRoutinesForDate(routineDate.toString()).map { it.copy() }
                                        cargarEntrenos()
                                    },
                                    onRoutineEdited = {
                                        routinesForDay = db.getRoutinesForDate(routineDate.toString()).map { it.copy() }
                                        cargarEntrenos()
                                    },
                                    onUsarPlantilla = { plantillaFecha = it },
                                    onFinalizarAccion = {
                                        routineDate = null
                                        routinesForDay = emptyList()
                                        selectedTab = "calendario"
                                    }
                                )
                            }

                        }
                    selectedTab == "estadisticas" -> {
                        PantallaEstadisticas(db = DBHelper(context))
                    }

                    selectedTab == "calendario" -> {
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
                                    val rutinasDelDia = db.getRoutinesForDate(fechaIso)
                                    Log.d(
                                        "DEBUG",
                                        "Se encontraron ${rutinasDelDia.size} rutinas para ese día"
                                    )

                                    if (rutinasDelDia.isNotEmpty()) {
                                        routineDate = LocalDate.parse(fechaIso)
                                        routinesForDay = rutinasDelDia
                                        emptyDate = null
                                    } else {
                                        emptyDate = LocalDate.parse(fechaIso)
                                        routineDate = null
                                        routinesForDay = emptyList()
                                    }
                                }
                            }
                        )
                    }


                    selectedTab == "rutinas" -> {
                        fun actualizarRutinas() {
                            rutinasState.value = db.getRoutines(plantillas = true)
                            cargarEntrenos()

                            selectedTab = "calendario"
                            selectedTab = "rutinas"
                        }

                        val rutinaEditLauncher = rememberLauncherForActivityResult(
                            ActivityResultContracts.StartActivityForResult()
                        ) { result ->
                            if (result.resultCode == RESULT_OK) {
                                actualizarRutinas()
                                // Forzar recarga cambiando de pestaña
                                selectedTab = "calendario"
                                selectedTab = "rutinas"
                            }
                        }

                        LaunchedEffect(Unit) {
                            actualizarRutinas()
                        }

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
                                items(rutinasState.value) { rutina ->
                                    val ejercicios = db.getExercisesForRoutine(rutina.id)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarioPersonalizado(
    entrenosPorFecha: Map<String, List<String>>,
    coloresRutina: Map<String, Color>,
    mesMostrado: LocalDate,
    onMesCambiado: (LocalDate) -> Unit,
    onDayClick: (String) -> Unit
) {
    val nombreMes = mesMostrado.month
        .getDisplayName(TextStyle.FULL, Locale.getDefault())
        .replaceFirstChar { it.uppercase() }

    var totalDragX by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(mesMostrado) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        totalDragX += dragAmount.x
                        change.consumePositionChange()
                    },
                    onDragEnd = {
                        if (totalDragX > 100f) onMesCambiado(mesMostrado.minusMonths(1))
                        else if (totalDragX < -100f) onMesCambiado(mesMostrado.plusMonths(1))
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

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { onMesCambiado(mesMostrado.minusMonths(1)) }) {
                    Text("< Anterior")
                }
                Text(
                    text = "$nombreMes ${mesMostrado.year}",
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(onClick = { onMesCambiado(mesMostrado.plusMonths(1)) }) {
                    Text("Siguiente >")
                }
            }

            AnimatedContent(
                targetState = mesMostrado,
                label = "CambioDeMes",
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it } + fadeIn() togetherWith
                                slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith
                                slideOutHorizontally { it } + fadeOut()
                    }
                }
            ) { mesActual ->
                val diasEnMes = mesActual.lengthOfMonth()
                val offset = mesActual.dayOfWeek.value - 1
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
                                        .background(if (esRelleno) FondoPantalla else Color.Transparent)
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
                                        Text(
                                            text = dia,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        if (!esRelleno) {
                                            val rutinas = entrenosPorFecha[clave].orEmpty()
                                            Row(
                                                modifier = Modifier.align(Alignment.CenterHorizontally),
                                                horizontalArrangement = Arrangement.Center
                                            ) {
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

@Composable
fun NavegacionInferior(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    clearSelection: () -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 0.dp
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Filled.ShowChart, contentDescription = "Estadísticas") },
            selected = selectedTab == "estadisticas",
            onClick = {
                onTabSelected("estadisticas")
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
        NavigationBarItem(
            icon = {
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
        NavigationBarItem(
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

@Composable
fun EmptyDayScreen(
    date: LocalDate,
    onAddRoutine: () -> Unit,
    onUsarPlantilla: () -> Unit
) {
    val locale = Locale("es")
    val dayName = date.dayOfWeek
        .getDisplayName(TextStyle.FULL, locale)
        .replaceFirstChar { it.uppercaseChar() }
    val dateStr = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoPantalla)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = dayName,
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = dateStr,
            style = MaterialTheme.typography.bodyMedium,
            color = TextoPrimario
        )

        Spacer(Modifier.height(32.dp))

        Icon(
            imageVector = Icons.Default.CalendarToday,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "No hay ninguna rutina guardada o programada",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))

        Button(onClick = onAddRoutine) {
            Text("Crear rutina desde cero")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onUsarPlantilla) {
            Text("Usar rutina existente")
        }
    }
}


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
    val context = LocalContext.current
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("es"))
        .replaceFirstChar { it.uppercaseChar() }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val nuevasRutinas = db.getRoutinesForDate(date.toString())
            routines.value = nuevasRutinas.map { it.copy(nombre = it.nombre) }
            onRoutineEdited()
            onFinalizarAccion()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "$dayName ${date.format(formatter)}",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(Modifier.height(16.dp))

        routines.value.forEach { rutina ->
            var expanded by remember { mutableStateOf(true) }
            val ejercicios = db.getExercisesForRoutine(rutina.id)
            var showConfirmDialog by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { expanded = !expanded }
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
                            Text(rutina.nombre, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "${ejercicios.size} ejercicios",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextoPrimario
                            )
                        }

                        if (expanded) {
                            IconButton(onClick = { showConfirmDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Eliminar rutina",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = expanded) {
                        Column {
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

                if (showConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = { showConfirmDialog = false },
                        confirmButton = {
                            TextButton(onClick = {
                                db.deleteRoutine(rutina.id)
                                Toast.makeText(context, "Rutina eliminada", Toast.LENGTH_SHORT).show()
                                showConfirmDialog = false
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

        Column (
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            )
         {
            Button(onClick = {
                val intent = Intent(context, AddRoutineActivity::class.java)
                intent.putExtra("fecha", date.toString())
                launcher.launch(intent)
            }) {
                Text("Crear rutina desde 0")
            }
             Spacer(modifier = Modifier.height(5.dp))

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

@Composable
fun PlantillaSelectorScreen(
    fecha: LocalDate,
    plantillas: List<DBHelper.Routine>,
    onUsarPlantilla: (DBHelper.Routine) -> Unit,
    onVolver: () -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val fechaStr = fecha.format(formatter)
    val context = LocalContext.current
    val db = remember { DBHelper(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Selecciona una rutina para el $fechaStr",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(plantillas) { rutina ->
                var expanded by remember { mutableStateOf(false) }
                val ejercicios = remember(rutina.id) { db.getExercisesForRoutine(rutina.id) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { expanded = !expanded }
                        .animateContentSize(),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            rutina.nombre,
                            style = MaterialTheme.typography.titleMedium
                        )

                        AnimatedVisibility(visible = expanded) {
                            Column {
                                Spacer(modifier = Modifier.height(12.dp)
                                    .fillMaxWidth()
                                )

                                ejercicios.forEach { ejercicio ->
                                    Text(
                                        text = "${ejercicio.nombre}: ${ejercicio.series}x${ejercicio.repeticiones} - ${ejercicio.peso} kg",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))

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

        Button(
            onClick = onVolver,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Volver")
        }
    }
}

@Composable
fun PantallaEstadisticas(db: DBHelper) {
    val resumen = remember {
        mutableStateOf(Triple(0, 0, ""))
    }
    val rutinas = remember {
        mutableStateOf<List<DBHelper.RutinaEstadistica>>(emptyList())
    }

    // Cargar estadísticas al iniciar
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
            .verticalScroll(rememberScrollState())
    ) {
        Text("Resumen de Entrenamientos", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

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

