package cl.duocuc.aulaviva.data.model

data class Clase(
    val id: String = "",
    val nombre: String = "",
    val descripcion: String = "",  // Descripción de las actividades de la clase
    val fecha: String = "",
    val archivoPdfUrl: String = "",  // URL del PDF en Supabase Storage (si existe)
    val archivoPdfNombre: String = "",  // Nombre del archivo PDF
    val creador: String = "", // UID del docente
    val asignaturaId: String = "" // FK a asignaturas (OBLIGATORIO)
)
