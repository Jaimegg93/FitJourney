// Clase que extiende SQLiteOpenHelper y gestiona toda la lógica de la base de datos local
// para rutinas, ejercicios y logs de uso en la aplicación FitJourney

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper


/**
 * Clase DBHelper que crea/actualiza la base de datos "fitjourney.db" y proporciona
 * métodos CRUD (crear, leer, actualizar, eliminar) para rutinas, ejercicios y logs.
 *
 * @param context Contexto de la aplicación para acceder al sistema de archivos interno.
 */
class DBHelper(context: Context) : SQLiteOpenHelper(context, "fitjourney.db", null, 8) {

    // Data classes para representar entidades de la base de datos
    // Representa una rutina, ya sea plantilla (esPlantilla = true) o rutina real (esPlantilla = false)
    data class Routine(val id: Long, val nombre: String, val esPlantilla: Boolean = false)

    // Representa un ejercicio asociado a una rutina
    data class RoutineExercise(
        val id: Long,
        val routineId: Long,
        val nombre: String,
        val series: Int,
        val repeticiones: Int,
        val peso: Double
    )
    // Representa estadísticas resumidas por cada rutina (solo para rutinas ya ejecutadas, no plantillas)
    data class RutinaEstadistica(
        val id: Long,
        val nombre: String,
        val veces: Int,
        val ultimaFecha: String?
    )

    // onCreate: Se llama al crear la base de datos por primera vez
    override fun onCreate(db: SQLiteDatabase) {
        // Crear tabla "routines": contiene ID, nombre y flag es_plantilla
        db.execSQL("""
            CREATE TABLE routines (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL,
                es_plantilla INTEGER DEFAULT 1
            )
        """.trimIndent())
        // Crear tabla "routine_exercises": asocia ejercicios a una rutina
        db.execSQL("""
            CREATE TABLE routine_exercises (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                routine_id INTEGER NOT NULL,
                nombre TEXT NOT NULL,
                series INTEGER,
                repeticiones INTEGER,
                peso REAL,
                FOREIGN KEY(routine_id) REFERENCES routines(id)
            )
        """.trimIndent())
        // Crear tabla "routine_log": registra fecha y rutina usada en ese día
        db.execSQL("""
            CREATE TABLE routine_log (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                fecha TEXT NOT NULL,
                routine_id INTEGER NOT NULL,
                FOREIGN KEY(routine_id) REFERENCES routines(id)
            )
        """.trimIndent())

        // Insertar plantillas iniciales
        db.execSQL("INSERT INTO routines (nombre, es_plantilla) VALUES ('Pecho y Bíceps', 1)") // id 1
        db.execSQL("INSERT INTO routines (nombre, es_plantilla) VALUES ('Espalda y Tríceps', 1)") // id 2
        db.execSQL("INSERT INTO routines (nombre, es_plantilla) VALUES ('Piernas y Core', 1)") // id 3
        db.execSQL("INSERT INTO routines (nombre, es_plantilla) VALUES ('Full Body', 1)") // id 4

        // Insertar ejercicios para plantillas
        db.execSQL("""
            INSERT INTO routine_exercises (routine_id, nombre, series, repeticiones, peso)
            VALUES
                (1, 'Press banca', 4, 10, 60.0),
                (1, 'Aperturas inclinadas', 3, 12, 12.5),
                (1, 'Curl bíceps barra', 4, 10, 30.0),
                (2, 'Dominadas', 4, 8, 0.0),
                (2, 'Remo con barra', 4, 10, 60.0),
                (2, 'Extensiones de tríceps', 3, 12, 20.0),
                (3, 'Sentadillas', 4, 10, 70.0),
                (3, 'Zancadas', 3, 12, 15.0),
                (3, 'Plancha', 3, 60, 0.0),
                (4, 'Burpees', 3, 12, 0.0),
                (4, 'Peso muerto', 4, 8, 80.0),
                (4, 'Press militar', 4, 10, 35.0)
        """.trimIndent())

        // Insertar algunos logs de uso reales (no plantillas)
        db.execSQL("INSERT INTO routines (nombre, es_plantilla) VALUES ('Pecho y Bíceps', 0)") // id 5
        db.execSQL("INSERT INTO routine_log (fecha, routine_id) VALUES ('2025-06-10', 5)")
        db.execSQL("""
            INSERT INTO routine_exercises (routine_id, nombre, series, repeticiones, peso)
            SELECT 5, nombre, series, repeticiones, peso FROM routine_exercises WHERE routine_id = 1
        """)

        // Clonamos rutina 2 para fecha 2025-05-12
        db.execSQL("INSERT INTO routines (nombre, es_plantilla) VALUES ('Espalda y Tríceps', 0)") // id 6
        db.execSQL("INSERT INTO routine_log (fecha, routine_id) VALUES ('2025-06-12', 6)")
        db.execSQL("""
            INSERT INTO routine_exercises (routine_id, nombre, series, repeticiones, peso)
            SELECT 6, nombre, series, repeticiones, peso FROM routine_exercises WHERE routine_id = 2
        """)
    }

    // onUpgrade: Se llama cuando la versión de BD cambia
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Eliminamos tablas previas para recrerar desde cero
        // Esta asi para realizar pruebas
        db.execSQL("DROP TABLE IF EXISTS routine_log")
        db.execSQL("DROP TABLE IF EXISTS routine_exercises")
        db.execSQL("DROP TABLE IF EXISTS routines")
        db.execSQL("DROP TABLE IF EXISTS entrenamientos")
        onCreate(db)
    }


    /**
     * Inserta una nueva rutina en la tabla "routines".
     *
     * @param nombre      Nombre de la rutina a insertar.
     * @param esPlantilla Boolean que indica si la rutina es plantilla (true) o rutina real (false).
     * @return ID de la nueva fila insertada (Long) o -1 si hubo un error.
     */
    fun insertRoutine(nombre: String, esPlantilla: Boolean = true): Long {
        val cv = ContentValues().apply {
            put("nombre", nombre)
            put("es_plantilla", if (esPlantilla) 1 else 0)
        }
        return writableDatabase.insert("routines", null, cv)
    }


    /**
     * Duplica una rutina existente (con ID rutinaId) copiando su nombre y todos sus ejercicios.
     * El duplicado se marca como rutina real (es_plantilla = false).
     *
     * @param rutinaId ID de la rutina original que se va a duplicar como rutina real.
     * @return ID de la nueva rutina duplicada (Long), o -1 si no se encontró la original.
     */
    fun duplicarRutina(rutinaId: Long): Long {
        val rutinaOriginal = getRoutines().find { it.id == rutinaId } ?: return -1L
        val nuevoId = insertRoutine(rutinaOriginal.nombre, esPlantilla = false)
        val ejercicios = getEjerciciosPorRutina(rutinaId)
        ejercicios.forEach {
            insertRoutineExercise(nuevoId, it.nombre, it.series, it.repeticiones, it.peso)
        }
        return nuevoId
    }

    /**
     * Inserta un ejercicio asociado a una rutina dada.
     *
     * @param routineId ID de la rutina a la que pertenece el ejercicio.
     * @param nombre    Nombre del ejercicio.
     * @param series    Número de series (Int).
     * @param repeticiones Número de repeticiones (Int).
     * @param peso      Peso utilizado (Double).
     * @return ID de la nueva fila en "routine_exercises", o -1 si hubo error.
     */
    fun insertRoutineExercise(routineId: Long, nombre: String, series: Int?, repeticiones: Int?, peso: Double?): Long {
        val cv = ContentValues().apply {
            put("routine_id", routineId)
            put("nombre", nombre)
            put("series", series ?: 0)
            put("repeticiones", repeticiones ?: 0)
            put("peso", peso ?: 0.0)
        }
        return writableDatabase.insert("routine_exercises", null, cv)
    }

    /**
     * Registra en "routine_log" que una rutina se ejecutó en cierta fecha.
     *
     * @param fecha     Fecha en formato "YYYY-MM-DD".
     * @param routineId ID de la rutina que se ejecutó (debe existir en "routines").
     * @return ID de la nueva fila en "routine_log", o -1 si hubo error.
     */
    fun logRoutine(fecha: String, routineId: Long): Long {
        val cv = ContentValues().apply {
            put("fecha", fecha)
            put("routine_id", routineId)
        }
        return writableDatabase.insert("routine_log", null, cv)
    }

    /**
     * Obtiene la lista de rutinas filtradas por si son plantillas o rutinas reales.
     *
     * @param plantillas Boolean que indica si queremos solo plantillas (true) o solo rutinas reales (false).
     * @return Lista de objetos Routine correspondientes al filtro.
     */
    fun getRoutines(plantillas: Boolean = true): List<Routine> {
        val list = mutableListOf<Routine>()
        val cursor = readableDatabase.query(
            "routines",
            arrayOf("id", "nombre", "es_plantilla"),
            "es_plantilla = ?",
            arrayOf(if (plantillas) "1" else "0"),
            null, null, null
        )
        cursor.use {
            while (it.moveToNext()) {
                list += Routine(
                    id = it.getLong(it.getColumnIndexOrThrow("id")),
                    nombre = it.getString(it.getColumnIndexOrThrow("nombre")),
                    esPlantilla = it.getInt(it.getColumnIndexOrThrow("es_plantilla")) == 1
                )
            }
        }
        return list
    }

    /**
     * Obtiene todas las rutinas programadas (no plantillas) para una fecha dada.
     *
     * @param fecha Fecha en formato "YYYY-MM-DD".
     * @return Lista de objetos Routine correspondientes a esa fecha.
     */
    fun getRoutinasPorFecha(fecha: String): List<Routine> {
        val list = mutableListOf<Routine>()
        val query = """
        SELECT r.id, r.nombre, r.es_plantilla 
        FROM routine_log rl
        JOIN routines r ON rl.routine_id = r.id
        WHERE rl.fecha = ?
    """.trimIndent()

        val cursor = readableDatabase.rawQuery(query, arrayOf(fecha))
        cursor.use {
            while (it.moveToNext()) {
                list += Routine(
                    id = it.getLong(it.getColumnIndexOrThrow("id")),
                    nombre = it.getString(it.getColumnIndexOrThrow("nombre")),
                    esPlantilla = it.getInt(it.getColumnIndexOrThrow("es_plantilla")) == 1
                )
            }
        }
        return list
    }

    /**
     * Obtiene los ejercicios asociados a una rutina específica.
     *
     * @param routineId ID de la rutina de la que queremos los ejercicios.
     * @return Lista de objetos RoutineExercise con los datos de cada ejercicio.
     */
    fun getEjerciciosPorRutina(routineId: Long): List<RoutineExercise> {
        val list = mutableListOf<RoutineExercise>()
        readableDatabase.query(
            "routine_exercises",
            arrayOf("id", "routine_id", "nombre", "series", "repeticiones", "peso"),
            "routine_id=?",
            arrayOf(routineId.toString()), null, null, null
        ).use { c ->
            while (c.moveToNext()) {
                list += RoutineExercise(
                    id = c.getLong(c.getColumnIndexOrThrow("id")),
                    routineId = c.getLong(c.getColumnIndexOrThrow("routine_id")),
                    nombre = c.getString(c.getColumnIndexOrThrow("nombre")),
                    series = c.getInt(c.getColumnIndexOrThrow("series")),
                    repeticiones = c.getInt(c.getColumnIndexOrThrow("repeticiones")),
                    peso = c.getDouble(c.getColumnIndexOrThrow("peso"))
                )
            }
        }
        return list
    }

    /**
     * Devuelve el número total de días distintos en los que se ha registrado al menos una rutina.
     *
     * @return Entero con la cantidad de días distintos en "routine_log".
     */
    fun getTotalDiasEntrenados(): Int {
        val cursor = readableDatabase.rawQuery("SELECT COUNT(DISTINCT fecha) FROM routine_log", null)
        cursor.use { return if (it.moveToFirst()) it.getInt(0) else 0 }
    }

    /**
     * Obtiene estadísticas de uso agrupadas por nombre de rutina para rutinas reales (es_plantilla = 0).
     *
     * @return Lista de RutinaEstadistica con nombre, cantidad de usos y fecha de última vez.
     */
    fun getEstadisticasPorRutinaPlantilla(): List<RutinaEstadistica> {
        val query = """
        SELECT r.nombre, COUNT(rl.id) AS usos, MAX(rl.fecha) AS ultima_fecha
        FROM routines r
        JOIN routine_log rl ON rl.routine_id = r.id
        WHERE r.es_plantilla = 0
        GROUP BY r.nombre
        ORDER BY usos DESC
    """.trimIndent()

        val lista = mutableListOf<RutinaEstadistica>()
        val cursor = readableDatabase.rawQuery(query, null)
        cursor.use {
            var idFicticio = 1L
            while (it.moveToNext()) {
                lista.add(
                    RutinaEstadistica(
                        id = idFicticio++,
                        nombre = it.getString(it.getColumnIndexOrThrow("nombre")),
                        veces = it.getInt(it.getColumnIndexOrThrow("usos")),
                        ultimaFecha = it.getString(it.getColumnIndexOrThrow("ultima_fecha"))
                    )
                )
            }
        }
        return lista
    }

    /**
     * Elimina una rutina completa (junto con sus ejercicios y logs) de la base de datos.
     *
     * @param routineId ID de la rutina a eliminar.
     * @return Número de filas eliminadas de la tabla "routines" (debería ser 1 si se eliminó correcto).
     */
    fun deleteRoutine(routineId: Long): Int {
        writableDatabase.delete("routine_exercises", "routine_id=?", arrayOf(routineId.toString()))
        writableDatabase.delete("routine_log", "routine_id=?", arrayOf(routineId.toString()))
        return writableDatabase.delete("routines", "id=?", arrayOf(routineId.toString()))
    }

    /**
     * Elimina todos los ejercicios asociados a una rutina específica.
     *
     * @param routineId ID de la rutina cuyos ejercicios se eliminarán.
     * @return Número de filas eliminadas en "routine_exercises".
     */
    fun deleteRoutineExercise(routineId: Long): Int {
        return writableDatabase.delete("routine_exercises", "routine_id=?", arrayOf(routineId.toString()))
    }

    /**
     * Actualiza solo el nombre de una rutina existente.
     *
     * @param id         ID de la rutina.
     * @param nuevoNombre Nuevo nombre para la rutina.
     * @return Número de filas afectadas.
     */
    fun updateRoutineName(id: Long, nuevoNombre: String): Int {
        val values = ContentValues().apply {
            put("nombre", nuevoNombre)
        }
        return writableDatabase.update("routines", values, "id=?", arrayOf(id.toString()))
    }


}

