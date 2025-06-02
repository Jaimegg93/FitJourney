import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.LocalDate

class DBHelper(context: Context) : SQLiteOpenHelper(context, "fitjourney.db", null, 8) {

    // Data classes utilizadas en la app
    data class Routine(val id: Long, val nombre: String, val esPlantilla: Boolean = false)
    data class RoutineExercise(
        val id: Long,
        val routineId: Long,
        val nombre: String,
        val series: Int,
        val repeticiones: Int,
        val peso: Double
    )
    data class RutinaEstadistica(
        val id: Long,
        val nombre: String,
        val veces: Int,
        val ultimaFecha: String?
    )

    override fun onCreate(db: SQLiteDatabase) {

        db.execSQL("""
            CREATE TABLE routines (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL,
                es_plantilla INTEGER DEFAULT 1
            )
        """.trimIndent())

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

        db.execSQL("""
            CREATE TABLE routine_log (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                fecha TEXT NOT NULL,
                routine_id INTEGER NOT NULL,
                FOREIGN KEY(routine_id) REFERENCES routines(id)
            )
        """.trimIndent())

        // Plantillas con sentido
        db.execSQL("INSERT INTO routines (nombre, es_plantilla) VALUES ('Pecho y Bíceps', 1)") // id 1
        db.execSQL("INSERT INTO routines (nombre, es_plantilla) VALUES ('Espalda y Tríceps', 1)") // id 2
        db.execSQL("INSERT INTO routines (nombre, es_plantilla) VALUES ('Piernas y Core', 1)") // id 3
        db.execSQL("INSERT INTO routines (nombre, es_plantilla) VALUES ('Full Body', 1)") // id 4

        // Ejercicios con sentido para cada rutina
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

        // Logs de uso (como NO son plantillas, hay que insertar una rutina real clonada de las plantillas)
        // Clonamos rutina 1 para fecha 2025-05-10
        db.execSQL("INSERT INTO routines (nombre, es_plantilla) VALUES ('Pecho y Bíceps', 0)") // id 5
        db.execSQL("INSERT INTO routine_log (fecha, routine_id) VALUES ('2025-05-10', 5)")
        db.execSQL("""
            INSERT INTO routine_exercises (routine_id, nombre, series, repeticiones, peso)
            SELECT 5, nombre, series, repeticiones, peso FROM routine_exercises WHERE routine_id = 1
        """)

        // Clonamos rutina 2 para fecha 2025-05-12
        db.execSQL("INSERT INTO routines (nombre, es_plantilla) VALUES ('Espalda y Tríceps', 0)") // id 6
        db.execSQL("INSERT INTO routine_log (fecha, routine_id) VALUES ('2025-05-12', 6)")
        db.execSQL("""
            INSERT INTO routine_exercises (routine_id, nombre, series, repeticiones, peso)
            SELECT 6, nombre, series, repeticiones, peso FROM routine_exercises WHERE routine_id = 2
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS routine_log")
        db.execSQL("DROP TABLE IF EXISTS routine_exercises")
        db.execSQL("DROP TABLE IF EXISTS routines")
        db.execSQL("DROP TABLE IF EXISTS entrenamientos")
        onCreate(db)
    }

    // Inserciones
    fun insertRoutine(nombre: String, esPlantilla: Boolean = true): Long {
        val cv = ContentValues().apply {
            put("nombre", nombre)
            put("es_plantilla", if (esPlantilla) 1 else 0)
        }
        return writableDatabase.insert("routines", null, cv)
    }

    fun duplicarRutina(rutinaId: Long): Long {
        val rutinaOriginal = getRoutines().find { it.id == rutinaId } ?: return -1L
        val nuevoId = insertRoutine(rutinaOriginal.nombre, esPlantilla = false)
        val ejercicios = getExercisesForRoutine(rutinaId)
        ejercicios.forEach {
            insertRoutineExercise(nuevoId, it.nombre, it.series, it.repeticiones, it.peso)
        }
        return nuevoId
    }

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



    fun logRoutine(fecha: String, routineId: Long): Long {
        val cv = ContentValues().apply {
            put("fecha", fecha)
            put("routine_id", routineId)
        }
        return writableDatabase.insert("routine_log", null, cv)
    }

    // Consultas
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

    fun getRoutinesForDate(fecha: String): List<Routine> {
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


    fun getExercisesForRoutine(routineId: Long): List<RoutineExercise> {
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

    fun getTotalDiasEntrenados(): Int {
        val cursor = readableDatabase.rawQuery("SELECT COUNT(DISTINCT fecha) FROM routine_log", null)
        cursor.use { return if (it.moveToFirst()) it.getInt(0) else 0 }
    }

    fun getRutinaMasUsada(): Routine? {
        val query = """
            SELECT r.id, r.nombre, COUNT(*) as uso
            FROM routine_log rl
            JOIN routines r ON rl.routine_id = r.id
            GROUP BY rl.routine_id
            ORDER BY uso DESC
            LIMIT 1
        """.trimIndent()

        val cursor = readableDatabase.rawQuery(query, null)
        cursor.use {
            return if (it.moveToFirst()) {
                Routine(
                    id = it.getLong(it.getColumnIndexOrThrow("id")),
                    nombre = it.getString(it.getColumnIndexOrThrow("nombre")),
                    esPlantilla = false
                )
            } else null
        }
    }

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

    // Updates y deletes
    fun updateRoutine(id: Long, nuevoNombre: String): Int {
        val cv = ContentValues().apply { put("nombre", nuevoNombre) }
        return writableDatabase.update("routines", cv, "id=?", arrayOf(id.toString()))
    }

    fun updateRoutineExercise(exerciseId: Long, nombre: String, series: Int?, repeticiones: Int?, peso: Double?): Int {
        val cv = ContentValues().apply {
            put("nombre", nombre)
            put("series", series ?: 0)
            put("repeticiones", repeticiones ?: 0)
            put("peso", peso ?: 0.0)
        }
        return writableDatabase.update("routine_exercises", cv, "id=?", arrayOf(exerciseId.toString()))
    }

    fun deleteRoutine(routineId: Long): Int {
        writableDatabase.delete("routine_exercises", "routine_id=?", arrayOf(routineId.toString()))
        writableDatabase.delete("routine_log", "routine_id=?", arrayOf(routineId.toString()))
        return writableDatabase.delete("routines", "id=?", arrayOf(routineId.toString()))
    }

    fun deleteRoutineExercise(routineId: Long): Int {
        return writableDatabase.delete("routine_exercises", "routine_id=?", arrayOf(routineId.toString()))
    }


    fun updateRoutineName(id: Long, nuevoNombre: String): Int {
        val values = ContentValues().apply {
            put("nombre", nuevoNombre)
        }
        return writableDatabase.update("routines", values, "id=?", arrayOf(id.toString()))
    }


}

